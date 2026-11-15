package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.local.AdminAccountEntity
import com.example.data.local.LockoutState
import com.example.data.model.AdminRole
import com.example.ui.screens.AdminAuthGate
import com.example.ui.theme.FifaTournamentTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminLoginUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headAdminFirstTimeSetupRequiresSixDigitPinAndSubmits() {
        var setupUser = ""
        var setupDisplay = ""
        var setupPin = ""
        var setupKeep = false

        composeRule.setContent {
            FifaTournamentTheme {
                AdminAuthGate(
                    hasHeadAdmin = false,
                    admins = emptyList(),
                    lockoutState = LockoutState(),
                    onSetupHeadAdmin = { u, d, p, keep ->
                        setupUser = u
                        setupDisplay = d
                        setupPin = p
                        setupKeep = keep
                    },
                    onLogin = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        // Submit disabled with fewer than 6 digits
        composeRule.onNodeWithTag("setup_admin_pin").performScrollTo().performTextInput("1234")
        composeRule.onNodeWithTag("setup_admin_confirm_pin").performScrollTo().performTextInput("1234")
        composeRule.onNodeWithTag("setup_admin_submit").performScrollTo().assertIsNotEnabled()

        // Complete to 6 digits
        composeRule.onNodeWithTag("setup_admin_pin").performScrollTo().performTextInput("89")
        composeRule.onNodeWithTag("setup_admin_confirm_pin").performScrollTo().performTextInput("89")
        composeRule.onNodeWithTag("setup_admin_submit").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals("admin", setupUser)
        assertEquals("المشرف الرئيسي", setupDisplay)
        assertEquals("123489", setupPin)
        assertTrue(setupKeep)
    }

    @Test
    fun assistantAndHeadAdminCanSelectChipEnterSixDigitPinAndToggleDataStoreSession() {
        val head = AdminAccountEntity(
            id = 1L,
            username = "chief",
            displayName = "الرئيسي",
            role = AdminRole.HEAD_ADMIN,
            pinHash = "h1",
            pinSalt = "s1"
        )
        val sub = AdminAccountEntity(
            id = 2L,
            username = "mod1",
            displayName = "المساعد الأول",
            role = AdminRole.SUB_ADMIN,
            pinHash = "h2",
            pinSalt = "s2"
        )

        var loginUser = ""
        var loginPin = ""
        var loginKeep = true

        composeRule.setContent {
            FifaTournamentTheme {
                AdminAuthGate(
                    hasHeadAdmin = true,
                    admins = listOf(head, sub),
                    lockoutState = LockoutState(),
                    onSetupHeadAdmin = { _, _, _, _ -> },
                    onLogin = { u, p, keep ->
                        loginUser = u
                        loginPin = p
                        loginKeep = keep
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // Select sub-admin chip
        composeRule.onNodeWithTag("login_account_chip_mod1").performScrollTo().performClick()

        // Partial 5-digit PIN keeps login button disabled
        composeRule.onNodeWithTag("login_admin_pin").performScrollTo().performTextInput("74185")
        composeRule.onNodeWithTag("login_admin_submit").performScrollTo().assertIsNotEnabled()

        // Enter 6th digit + toggle keep session off
        composeRule.onNodeWithTag("login_admin_pin").performScrollTo().performTextInput("2")
        composeRule.onNodeWithTag("login_keep_session_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("login_admin_submit").performScrollTo().assertIsEnabled().performClick()
        composeRule.waitForIdle()

        assertEquals("mod1", loginUser)
        assertEquals("741852", loginPin)
        assertFalse(loginKeep)
    }

    @Test
    fun lockoutBannerDisplaysAndDisablesLoginWhenLocked() {
        val head = AdminAccountEntity(
            id = 1L,
            username = "chief",
            displayName = "الرئيسي",
            role = AdminRole.HEAD_ADMIN,
            pinHash = "h1",
            pinSalt = "s1"
        )

        composeRule.setContent {
            FifaTournamentTheme {
                AdminAuthGate(
                    hasHeadAdmin = true,
                    admins = listOf(head),
                    lockoutState = LockoutState(
                        failedAttempts = 0,
                        lockedUntilMillis = System.currentTimeMillis() + 25_000L
                    ),
                    onSetupHeadAdmin = { _, _, _, _ -> },
                    onLogin = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("login_lockout_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("login_admin_submit").assertIsNotEnabled()
    }

    @Test
    fun typingSingleDigitsAddsExactlyOneDigitPerKeystroke() {
        val djilali = AdminAccountEntity(
            id = 1L,
            username = "Djilali",
            displayName = "Djilali",
            role = AdminRole.HEAD_ADMIN,
            pinHash = "h1",
            pinSalt = "s1"
        )
        var capturedPin = ""

        composeRule.setContent {
            FifaTournamentTheme {
                AdminAuthGate(
                    hasHeadAdmin = true,
                    admins = listOf(djilali),
                    lockoutState = LockoutState(),
                    onSetupHeadAdmin = { _, _, _, _ -> },
                    onLogin = { _, pin, _ -> capturedPin = pin }
                )
            }
        }
        composeRule.waitForIdle()

        val pinNode = composeRule.onNodeWithTag("login_admin_pin").performScrollTo()
        val digits = listOf("2", "0", "2", "2", "2", "6")
        digits.forEachIndexed { index, d ->
            pinNode.performTextInput(d)
            composeRule.waitForIdle()
            val expectedCount = "${index + 1}/6"
            composeRule.onNodeWithTag("login_admin_submit").let { submit ->
                if (index < 5) submit.assertIsNotEnabled() else submit.assertIsEnabled()
            }
            // Verify the counter reflects strictly 1 added digit per keystroke
            composeRule.onNodeWithText(expectedCount).assertIsDisplayed()
        }

        composeRule.onNodeWithTag("login_admin_submit").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("202226", capturedPin)
    }
}
