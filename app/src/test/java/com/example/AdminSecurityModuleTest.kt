package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.AdminRole
import com.example.data.repository.RepoResult
import com.example.security.AdminPermission
import com.example.security.AdminSecurityModule
import com.example.security.PinSecurityManager
import com.example.security.PinStrength
import com.example.security.hasPermission
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminSecurityModuleTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var sessionPrefs: SessionPreferences
    private lateinit var securityModule: AdminSecurityModule

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionPrefs = SessionPreferences(context)
        securityModule = AdminSecurityModule(db.adminDao(), sessionPrefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun pbkdf2HashingRequiresSixDigitsUsesUniqueSaltAndEvaluatesStrength() {
        val salt1 = PinSecurityManager.generateSalt()
        val salt2 = PinSecurityManager.generateSalt()
        assertNotEquals(salt1, salt2)

        val hash1 = PinSecurityManager.hashPin("739201", salt1)
        val hash2 = PinSecurityManager.hashPin("739201", salt2)
        assertTrue(hash1.startsWith("pbkdf2_v1:"))
        assertNotEquals(hash1, hash2)
        assertTrue(PinSecurityManager.verifyPin("739201", salt1, hash1))
        assertFalse(PinSecurityManager.verifyPin("739202", salt1, hash1))

        assertFalse(PinSecurityManager.isValidPinFormat("1234"))
        assertFalse(PinSecurityManager.isValidPinFormat("12345"))
        assertTrue(PinSecurityManager.isValidPinFormat("739201"))

        assertEquals(PinStrength.INVALID, PinSecurityManager.evaluateStrength("1234"))
        assertEquals(PinStrength.WEAK, PinSecurityManager.evaluateStrength("111111"))
        assertEquals(PinStrength.WEAK, PinSecurityManager.evaluateStrength("123456"))
        assertEquals(PinStrength.GOOD, PinSecurityManager.evaluateStrength("112233"))
        assertEquals(PinStrength.STRONG, PinSecurityManager.evaluateStrength("739201"))
    }

    @Test
    fun headAdminChangePinAndSubAdminPermissionsAndDataStoreSession() = runTest {
        // Reject < 6 digit PIN on setup
        assertTrue(securityModule.setupHeadAdmin("chief", "الرئيسي", "4829") is RepoResult.Error)

        val head = (securityModule.setupHeadAdmin("chief", "الرئيسي", "482910") as RepoResult.Success).data
        assertTrue(head.hasPermission(AdminPermission.MANAGE_TOURNAMENT))
        assertTrue(head.hasPermission(AdminPermission.CROWN_CHAMPION))

        // Verify DataStore session metadata saved
        val headSession = sessionPrefs.getSessionSnapshot()
        assertEquals(head.id, headSession.loggedInAdminId)
        assertEquals("chief", headSession.loggedInUsername)
        assertEquals(AdminRole.HEAD_ADMIN.name, headSession.loggedInRole)
        assertTrue(headSession.keepSignedIn)

        // Change Head Admin PIN
        assertTrue(securityModule.changeOwnPin(head.id, "000000", "918273") is RepoResult.Error)
        assertTrue(securityModule.changeOwnPin(head.id, "482910", "918273") is RepoResult.Success)
        assertTrue(securityModule.loginAdmin("chief", "482910") is RepoResult.Error)
        assertTrue(securityModule.loginAdmin("chief", "918273") is RepoResult.Success)

        // Create Sub-Admin with limited permissions
        assertTrue(
            securityModule.createSubAdmin(
                actingAdminId = head.id,
                username = "scorer1",
                displayName = "مسجل النتائج",
                pin = "567190",
                canManageTournament = false,
                canManagePlayers = false,
                canRunDraw = false,
                canEditScores = true,
                canCrownChampion = false
            ) is RepoResult.Success
        )

        val sub = securityModule.admins.first().first { it.username == "scorer1" }
        assertTrue(sub.hasPermission(AdminPermission.EDIT_SCORES))
        assertFalse(sub.hasPermission(AdminPermission.RUN_DRAW))
        assertTrue(securityModule.requirePermission(sub.id, AdminPermission.EDIT_SCORES) is RepoResult.Success)
        assertTrue(securityModule.requirePermission(sub.id, AdminPermission.RUN_DRAW) is RepoResult.Error)

        // Non-head-admin cannot create sub-admins
        assertTrue(
            securityModule.createSubAdmin(
                actingAdminId = sub.id,
                username = "unauth",
                displayName = "غير مصرح",
                pin = "555511",
                canManageTournament = false,
                canManagePlayers = false,
                canRunDraw = false,
                canEditScores = true,
                canCrownChampion = false
            ) is RepoResult.Error
        )

        // Login with ephemeral session (keepSignedIn = false)
        assertTrue(securityModule.loginAdmin("scorer1", "567190", keepSignedIn = false) is RepoResult.Success)
        val subSession = sessionPrefs.getSessionSnapshot()
        assertEquals(sub.id, subSession.loggedInAdminId)
        assertEquals(AdminRole.SUB_ADMIN.name, subSession.loggedInRole)
        assertFalse(subSession.keepSignedIn)

        // Ephemeral session clears on simulated launch
        securityModule.clearEphemeralSessionOnLaunch()
        assertNull(sessionPrefs.getLoggedInAdminIdOnce())

        // Login with persistent session then suspend sub-admin -> clears session
        assertTrue(securityModule.loginAdmin("scorer1", "567190", keepSignedIn = true) is RepoResult.Success)
        assertTrue(
            securityModule.updateSubAdmin(
                actingAdminId = head.id,
                target = sub,
                displayName = "مسجل ومشرف البطولة",
                newPin = null,
                isActive = false,
                canManageTournament = true,
                canManagePlayers = false,
                canRunDraw = false,
                canEditScores = false,
                canCrownChampion = false
            ) is RepoResult.Success
        )
        assertNull(sessionPrefs.getLoggedInAdminIdOnce())
        assertTrue(securityModule.loginAdmin("scorer1", "567190") is RepoResult.Error)

        val updatedSub = securityModule.admins.first().first { it.id == sub.id }
        assertEquals("مسجل ومشرف البطولة", updatedSub.displayName)
        assertFalse(updatedSub.isActive)
        assertTrue(updatedSub.canManageTournament)
        assertFalse(updatedSub.canEditScores)

        // Reactivate & verify new permissions take effect
        assertTrue(
            securityModule.updateSubAdmin(
                actingAdminId = head.id,
                target = updatedSub,
                newPin = null,
                isActive = true,
                canManageTournament = true,
                canManagePlayers = false,
                canRunDraw = false,
                canEditScores = false,
                canCrownChampion = false
            ) is RepoResult.Success
        )
        val reactivated = securityModule.admins.first().first { it.id == sub.id }
        assertTrue(reactivated.hasPermission(AdminPermission.MANAGE_TOURNAMENT))
        assertFalse(reactivated.hasPermission(AdminPermission.EDIT_SCORES))
        assertTrue(securityModule.requirePermission(reactivated.id, AdminPermission.MANAGE_TOURNAMENT) is RepoResult.Success)
        assertTrue(securityModule.requirePermission(reactivated.id, AdminPermission.EDIT_SCORES) is RepoResult.Error)

        // Delete Sub-Admin
        assertTrue(securityModule.deleteSubAdmin(head.id, reactivated) is RepoResult.Success)
        assertTrue(securityModule.admins.first().none { it.id == sub.id })
    }

    @Test
    fun bruteForceLockoutTriggersAfterFiveWrongAttemptsAndUnlocksAfterCooldown() = runTest {
        securityModule.setupHeadAdmin("chief", "الرئيسي", "739204")
        val baseTime = 1_000_000L

        for (attempt in 1..4) {
            val res = securityModule.loginAdmin("chief", "000000", nowMillis = baseTime + attempt * 100L)
            assertTrue(res is RepoResult.Error)
        }

        // 5th wrong attempt triggers lockout
        val fifth = securityModule.loginAdmin("chief", "000000", nowMillis = baseTime + 500L)
        assertTrue(fifth is RepoResult.Error)

        // Even right PIN fails while locked
        val duringLock = securityModule.loginAdmin("chief", "739204", nowMillis = baseTime + 10_000L)
        assertTrue(duringLock is RepoResult.Error)

        // Succeeds after 30s cooldown
        val afterLock = securityModule.loginAdmin(
            "chief",
            "739204",
            nowMillis = baseTime + 500L + PinSecurityManager.LOCKOUT_DURATION_MS + 100L
        )
        assertTrue(afterLock is RepoResult.Success)
    }

    @Test
    fun userEntityPermissionsIntegrityDetectsUnauthorizedTampering() = runTest {
        val head = (securityModule.setupHeadAdmin("chief", "الرئيسي", "482910") as RepoResult.Success).data
        assertTrue(
            securityModule.createSubAdmin(
                actingAdminId = head.id,
                username = "scorer_safe",
                displayName = "مسجل موثق",
                pin = "567190",
                canManageTournament = false,
                canManagePlayers = false,
                canRunDraw = false,
                canEditScores = true,
                canCrownChampion = false
            ) is RepoResult.Success
        )

        val storedUser = db.userDao().getAdminByUsername("scorer_safe")!!
        assertTrue(storedUser.permissionsHash.startsWith("perm_v1:"))
        assertTrue(securityModule.requirePermission(storedUser.id, AdminPermission.EDIT_SCORES) is RepoResult.Success)

        // Simulate raw SQLite tampering of permission flags without updating permissionsHash
        db.userDao().updateAdmin(storedUser.copy(canManageTournament = true, canCrownChampion = true))

        val tamperedRes = securityModule.requirePermission(storedUser.id, AdminPermission.MANAGE_TOURNAMENT)
        assertTrue(tamperedRes is RepoResult.Error)
        assertFalse(db.userDao().getAdminById(storedUser.id)!!.hasPermission(AdminPermission.EDIT_SCORES))
    }

    @Test
    fun defaultHeadAdminDjilaliAuthenticatesWithExpectedPin() = runTest {
        securityModule.ensureDefaultHeadAdminIfEmpty()
        val djilali = db.userDao().getAdminByUsername("Djilali")!!
        assertEquals(AdminRole.HEAD_ADMIN, djilali.role)
        assertEquals("Djilali", djilali.displayName)
        assertTrue(djilali.hasPermission(AdminPermission.MANAGE_TOURNAMENT))
        assertTrue(djilali.hasPermission(AdminPermission.CROWN_CHAMPION))

        assertTrue(securityModule.loginAdmin("Djilali", "000000") is RepoResult.Error)
        val loginRes = securityModule.loginAdmin("Djilali", "202226")
        assertTrue(loginRes is RepoResult.Success)
    }
}
