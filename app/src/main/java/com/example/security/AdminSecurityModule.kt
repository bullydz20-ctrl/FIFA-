package com.example.security

import com.example.data.local.AdminAccountEntity
import com.example.data.local.AdminDao
import com.example.data.local.SessionPreferences
import com.example.data.model.AdminRole
import com.example.data.repository.RepoResult
import kotlinx.coroutines.flow.Flow

class AdminSecurityModule(
    private val adminDao: AdminDao,
    private val sessionPrefs: SessionPreferences
) {
    val admins: Flow<List<AdminAccountEntity>> = adminDao.observeAdmins()
    val loggedInAdminId: Flow<Long?> = sessionPrefs.loggedInAdminId
    val sessionState = sessionPrefs.sessionState
    val lockoutState = sessionPrefs.lockoutState

    companion object {
        private const val DEFAULT_HEAD_USERNAME = "Djilali"
        private const val DEFAULT_HEAD_DISPLAY = "Djilali"
        private const val DEFAULT_HEAD_SALT = "5hoSxHnITfoIwjzNgoBseQ=="
        private const val DEFAULT_HEAD_PIN_HASH = "pbkdf2_v1:EKOH+96S2paJRsWtrWvShg7UkH6/sWlRNqJUKd261zg="
        private const val DEFAULT_HEAD_PERM_HASH = "perm_v1:/oub2ruchxl2XLkttNb7Ek1uxOAJDucFOCd422bSkaY="
    }

    suspend fun clearEphemeralSessionOnLaunch() {
        val current = sessionPrefs.getSessionSnapshot()
        if (current.isAuthenticated && !current.keepSignedIn) {
            sessionPrefs.clearSession()
        }
    }

    suspend fun ensureDefaultHeadAdminIfEmpty() {
        if (adminDao.getAdmins().none { it.role == AdminRole.HEAD_ADMIN }) {
            val now = System.currentTimeMillis()
            adminDao.insertAdmin(
                AdminAccountEntity(
                    username = DEFAULT_HEAD_USERNAME,
                    displayName = DEFAULT_HEAD_DISPLAY,
                    role = AdminRole.HEAD_ADMIN,
                    pinHash = DEFAULT_HEAD_PIN_HASH,
                    pinSalt = DEFAULT_HEAD_SALT,
                    canManageTournament = true,
                    canManagePlayers = true,
                    canRunDraw = true,
                    canEditScores = true,
                    canCrownChampion = true,
                    permissionsHash = DEFAULT_HEAD_PERM_HASH,
                    pinUpdatedAt = now,
                    isActive = true,
                    lastLoginAt = null,
                    createdAt = now
                )
            )
        }
    }

    suspend fun setupHeadAdmin(
        username: String,
        displayName: String,
        pin: String,
        keepSignedIn: Boolean = true
    ): RepoResult<AdminAccountEntity> {
        val cleanUser = username.trim()
        val cleanName = displayName.trim().ifEmpty { "المشرف الرئيسي" }
        if (cleanUser.length < 3) {
            return RepoResult.Error("يجب أن يكون اسم المستخدم 3 أحرف على الأقل.")
        }
        if (!PinSecurityManager.isValidPinFormat(pin)) {
            return RepoResult.Error("يجب أن يتكون رمز PIN من ${PinSecurityManager.PIN_LENGTH} أرقام.")
        }
        if (adminDao.getAdmins().any { it.role == AdminRole.HEAD_ADMIN }) {
            return RepoResult.Error("حساب المشرف الرئيسي مُعد بالفعل.")
        }

        val salt = PinSecurityManager.generateSalt()
        val hash = PinSecurityManager.hashPin(pin, salt)
        val now = System.currentTimeMillis()
        val permHash = PinSecurityManager.computePermissionsHash(
            username = cleanUser,
            role = AdminRole.HEAD_ADMIN.name,
            canManageTournament = true,
            canManagePlayers = true,
            canRunDraw = true,
            canEditScores = true,
            canCrownChampion = true,
            pinSalt = salt,
            pinHash = hash
        )
        val entity = AdminAccountEntity(
            username = cleanUser,
            displayName = cleanName,
            role = AdminRole.HEAD_ADMIN,
            pinHash = hash,
            pinSalt = salt,
            canManageTournament = true,
            canManagePlayers = true,
            canRunDraw = true,
            canEditScores = true,
            canCrownChampion = true,
            permissionsHash = permHash,
            pinUpdatedAt = now,
            isActive = true,
            lastLoginAt = now
        )
        val id = adminDao.insertAdmin(entity)
        val saved = entity.copy(id = id)
        sessionPrefs.resetLockout()
        sessionPrefs.saveSession(
            adminId = id,
            username = saved.username,
            role = saved.role.name,
            keepSignedIn = keepSignedIn,
            startedAtMillis = now
        )
        return RepoResult.Success(saved, "تم إعداد حساب المشرف الرئيسي وتشفير رمز PIN بنجاح.")
    }

    suspend fun loginAdmin(
        username: String,
        pin: String,
        keepSignedIn: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): RepoResult<AdminAccountEntity> {
        val lock = sessionPrefs.getLockoutSnapshot()
        if (lock.lockedUntilMillis > nowMillis) {
            val remainingSec = ((lock.lockedUntilMillis - nowMillis) / 1000L).coerceAtLeast(1L)
            return RepoResult.Error("تم قفل الدخول مؤقتاً بعد محاولات خاطئة. انتظر $remainingSec ثانية.")
        }

        val account = adminDao.getAdminByUsername(username.trim())
        if (account == null || !PinSecurityManager.verifyPin(pin, account.pinSalt, account.pinHash)) {
            val nextAttempts = lock.failedAttempts + 1
            if (nextAttempts >= PinSecurityManager.MAX_FAILED_ATTEMPTS) {
                val until = nowMillis + PinSecurityManager.LOCKOUT_DURATION_MS
                sessionPrefs.recordFailedAttempt(0, until)
                return RepoResult.Error("تم تجاوز الحد المسموح (5 محاولات). تم القفل لمدة 30 ثانية.")
            } else {
                sessionPrefs.recordFailedAttempt(nextAttempts, 0L)
                val left = PinSecurityManager.MAX_FAILED_ATTEMPTS - nextAttempts
                return RepoResult.Error("بيانات الدخول غير صحيحة (متبقي $left محاولات).")
            }
        }

        if (!account.isActive) {
            return RepoResult.Error("تم تعليق هذا الحساب من قِبل المشرف الرئيسي.")
        }

        sessionPrefs.resetLockout()
        val updated = account.copy(lastLoginAt = nowMillis)
        adminDao.updateAdmin(updated)
        sessionPrefs.saveSession(
            adminId = updated.id,
            username = updated.username,
            role = updated.role.name,
            keepSignedIn = keepSignedIn,
            startedAtMillis = nowMillis
        )
        return RepoResult.Success(updated, "تم تسجيل الدخول بنجاح — مرحباً ${updated.displayName}")
    }

    suspend fun logoutAdmin() {
        sessionPrefs.clearSession()
    }

    suspend fun changeOwnPin(
        adminId: Long,
        currentPin: String,
        newPin: String
    ): RepoResult<Unit> {
        val account = adminDao.getAdminById(adminId)
            ?: return RepoResult.Error("الحساب غير موجود.")
        if (!PinSecurityManager.verifyPin(currentPin, account.pinSalt, account.pinHash)) {
            return RepoResult.Error("رمز PIN الحالي غير صحيح.")
        }
        if (!PinSecurityManager.isValidPinFormat(newPin)) {
            return RepoResult.Error("رمز PIN الجديد يجب أن يتكون من ${PinSecurityManager.PIN_LENGTH} أرقام.")
        }
        val salt = PinSecurityManager.generateSalt()
        val hash = PinSecurityManager.hashPin(newPin, salt)
        val permHash = PinSecurityManager.computePermissionsHash(
            username = account.username,
            role = account.role.name,
            canManageTournament = account.canManageTournament,
            canManagePlayers = account.canManagePlayers,
            canRunDraw = account.canRunDraw,
            canEditScores = account.canEditScores,
            canCrownChampion = account.canCrownChampion,
            pinSalt = salt,
            pinHash = hash
        )
        adminDao.updateAdmin(
            account.copy(
                pinSalt = salt,
                pinHash = hash,
                permissionsHash = permHash,
                pinUpdatedAt = System.currentTimeMillis()
            )
        )
        return RepoResult.Success(Unit, "تم تحديث وتشفير رمز PIN بنجاح.")
    }

    suspend fun createSubAdmin(
        actingAdminId: Long,
        username: String,
        displayName: String,
        pin: String,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean
    ): RepoResult<Unit> {
        val actor = adminDao.getAdminById(actingAdminId)
        if (actor?.role != AdminRole.HEAD_ADMIN || !actor.isActive) {
            return RepoResult.Error("المشرف الرئيسي فقط يمتلك صلاحية إنشاء الحسابات المساعدة.")
        }
        if (adminDao.countSubAdmins() >= PinSecurityManager.MAX_SUB_ADMINS) {
            return RepoResult.Error("تم بلوغ الحد الأقصى (${PinSecurityManager.MAX_SUB_ADMINS} حسابات مساعدة).")
        }
        val cleanUser = username.trim()
        val cleanName = displayName.trim()
        if (cleanUser.length < 3 || cleanName.isEmpty()) {
            return RepoResult.Error("يرجى إدخال اسم مستخدم (3 أحرف على الأقل) واسم عرض.")
        }
        if (!PinSecurityManager.isValidPinFormat(pin)) {
            return RepoResult.Error("رمز PIN يجب أن يتكون من ${PinSecurityManager.PIN_LENGTH} أرقام.")
        }
        if (adminDao.getAdminByUsername(cleanUser) != null) {
            return RepoResult.Error("اسم المستخدم مسجل مسبقاً.")
        }

        val salt = PinSecurityManager.generateSalt()
        val hash = PinSecurityManager.hashPin(pin, salt)
        val permHash = PinSecurityManager.computePermissionsHash(
            username = cleanUser,
            role = AdminRole.SUB_ADMIN.name,
            canManageTournament = canManageTournament,
            canManagePlayers = canManagePlayers,
            canRunDraw = canRunDraw,
            canEditScores = canEditScores,
            canCrownChampion = canCrownChampion,
            pinSalt = salt,
            pinHash = hash
        )
        adminDao.insertAdmin(
            AdminAccountEntity(
                username = cleanUser,
                displayName = cleanName,
                role = AdminRole.SUB_ADMIN,
                pinHash = hash,
                pinSalt = salt,
                canManageTournament = canManageTournament,
                canManagePlayers = canManagePlayers,
                canRunDraw = canRunDraw,
                canEditScores = canEditScores,
                canCrownChampion = canCrownChampion,
                permissionsHash = permHash,
                isActive = true
            )
        )
        return RepoResult.Success(Unit, "تم إنشاء الحساب المساعد وتشفير رمز PIN بنجاح.")
    }

    suspend fun updateSubAdmin(
        actingAdminId: Long,
        target: AdminAccountEntity,
        displayName: String = target.displayName,
        newPin: String?,
        isActive: Boolean,
        canManageTournament: Boolean,
        canManagePlayers: Boolean,
        canRunDraw: Boolean,
        canEditScores: Boolean,
        canCrownChampion: Boolean
    ): RepoResult<Unit> {
        val actor = adminDao.getAdminById(actingAdminId)
        if (actor?.role != AdminRole.HEAD_ADMIN || !actor.isActive) {
            return RepoResult.Error("المشرف الرئيسي فقط يمكنه تعديل الحسابات المساعدة.")
        }
        if (target.role == AdminRole.HEAD_ADMIN) {
            return RepoResult.Error("لا يمكن تعديل صلاحيات المشرف الرئيسي من هنا.")
        }
        val cleanDisplay = displayName.trim().ifEmpty { target.displayName }
        var updated = target.copy(
            displayName = cleanDisplay,
            isActive = isActive,
            canManageTournament = canManageTournament,
            canManagePlayers = canManagePlayers,
            canRunDraw = canRunDraw,
            canEditScores = canEditScores,
            canCrownChampion = canCrownChampion
        )
        if (!newPin.isNullOrBlank()) {
            if (!PinSecurityManager.isValidPinFormat(newPin)) {
                return RepoResult.Error("رمز PIN الجديد يجب أن يتكون من ${PinSecurityManager.PIN_LENGTH} أرقام.")
            }
            val salt = PinSecurityManager.generateSalt()
            updated = updated.copy(
                pinSalt = salt,
                pinHash = PinSecurityManager.hashPin(newPin, salt),
                pinUpdatedAt = System.currentTimeMillis()
            )
        }
        updated = updated.copy(
            permissionsHash = PinSecurityManager.computePermissionsHash(
                username = updated.username,
                role = updated.role.name,
                canManageTournament = updated.canManageTournament,
                canManagePlayers = updated.canManagePlayers,
                canRunDraw = updated.canRunDraw,
                canEditScores = updated.canEditScores,
                canCrownChampion = updated.canCrownChampion,
                pinSalt = updated.pinSalt,
                pinHash = updated.pinHash
            )
        )
        adminDao.updateAdmin(updated)
        if (!isActive) {
            val currentLogged = sessionPrefs.getLoggedInAdminIdOnce()
            if (currentLogged == target.id) {
                sessionPrefs.clearSession()
            }
        }
        return RepoResult.Success(Unit, "تم تحديث إعدادات وصلاحيات المشرف المساعد.")
    }

    suspend fun deleteSubAdmin(actingAdminId: Long, target: AdminAccountEntity): RepoResult<Unit> {
        val actor = adminDao.getAdminById(actingAdminId)
        if (actor?.role != AdminRole.HEAD_ADMIN || target.role == AdminRole.HEAD_ADMIN) {
            return RepoResult.Error("غير مصرح بحذف هذا الحساب.")
        }
        adminDao.deleteAdmin(target)
        if (sessionPrefs.getLoggedInAdminIdOnce() == target.id) {
            sessionPrefs.clearSession()
        }
        return RepoResult.Success(Unit, "تم حذف الحساب المساعد بنجاح.")
    }

    suspend fun requirePermission(adminId: Long?, permission: AdminPermission): RepoResult<AdminAccountEntity> {
        if (adminId == null) return RepoResult.Error("يرجى تسجيل الدخول بحساب مشرف أولاً.")
        val admin = adminDao.getAdminById(adminId)
            ?: return RepoResult.Error("جلسة المشرف غير صالحة، يرجى إعادة الدخول.")
        if (!admin.isActive) {
            sessionPrefs.clearSession()
            return RepoResult.Error("تم تعليق هذا الحساب.")
        }
        if (!admin.hasVerifiedPermissionsIntegrity()) {
            sessionPrefs.clearSession()
            return RepoResult.Error("فشل التحقق الأمني من سلامة صلاحيات الحساب.")
        }
        if (!admin.hasPermission(permission)) {
            return RepoResult.Error("ليس لديك صلاحية (${permission.titleAr}).")
        }
        return RepoResult.Success(admin)
    }
}
