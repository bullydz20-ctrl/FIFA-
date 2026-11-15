package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.local.AdminAccountEntity
import com.example.data.model.AdminRole
import com.example.ui.screens.AdminSubAdminsTab
import com.example.ui.theme.FifaTournamentTheme
import com.example.ui.viewmodel.TournamentUiState
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
class SubAdminManagementUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun headAdmin() = AdminAccountEntity(
        id = 1L,
        username = "head_admin",
        displayName = "المشرف الرئيسي",
        role = AdminRole.HEAD_ADMIN,
        pinHash = "hash",
        pinSalt = "salt",
        canManageTournament = true,
        canManagePlayers = true,
        canRunDraw = true,
        canEditScores = true,
        canCrownChampion = true,
        isActive = true
    )

    @Test
    fun rendersFiveSlotsAndCreatesSubAdminWithPresetAndCustomPermission() {
        var capturedUser = ""
        var capturedDisplay = ""
        var capturedPin = ""
        var capturedTournament = false
        var capturedPlayers = false
        var capturedDraw = false
        var capturedScores = false
        var capturedChampion = false

        val uiState = TournamentUiState(
            isLoaded = true,
            admins = listOf(headAdmin()),
            hasHeadAdmin = true,
            currentAdmin = headAdmin()
        )

        composeRule.setContent {
            FifaTournamentTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AdminSubAdminsTab(
                        uiState = uiState,
                        onChangeOwnPin = { _, _ -> },
                        onCreateSubAdmin = { user, display, pin, p1, p2, p3, p4, p5 ->
                            capturedUser = user
                            capturedDisplay = display
                            capturedPin = pin
                            capturedTournament = p1
                            capturedPlayers = p2
                            capturedDraw = p3
                            capturedScores = p4
                            capturedChampion = p5
                        },
                        onUpdateSubAdmin = { _, _, _, _, _, _, _, _, _ -> },
                        onDeleteSubAdmin = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // All 5 assistant slots should be shown as vacant
        for (slot in 1..5) {
            composeRule.onNodeWithTag("empty_sub_admin_slot_$slot")
                .performScrollTo()
                .assertIsDisplayed()
        }

        // Open create dialog
        composeRule.onNodeWithTag("add_sub_admin_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sub_admin_username_input").performTextInput("scorer_ali")
        composeRule.onNodeWithTag("sub_admin_display_input").performTextInput("علي النتائج")
        composeRule.onNodeWithTag("sub_admin_pin_input").performTextInput("482910")

        // Apply scorekeeper preset (only EDIT_SCORES = true), then also toggle tournament management
        composeRule.onNodeWithTag("preset_scorekeeper").performScrollTo().performClick()
        composeRule.onNodeWithTag("perm_tournament").performScrollTo().performClick()
        composeRule.onNodeWithTag("sub_admin_save_button").performClick()
        composeRule.waitForIdle()

        assertEquals("scorer_ali", capturedUser)
        assertEquals("علي النتائج", capturedDisplay)
        assertEquals("482910", capturedPin)
        assertTrue(capturedTournament)
        assertFalse(capturedPlayers)
        assertFalse(capturedDraw)
        assertTrue(capturedScores)
        assertFalse(capturedChampion)
    }

    @Test
    fun editsDisplayNameAndPermissionsAndDeletesSubAdminWithConfirmation() {
        val existingSub = AdminAccountEntity(
            id = 22L,
            username = "mod_omar",
            displayName = "عمر",
            role = AdminRole.SUB_ADMIN,
            pinHash = "hash2",
            pinSalt = "salt2",
            canManageTournament = false,
            canManagePlayers = false,
            canRunDraw = false,
            canEditScores = true,
            canCrownChampion = false,
            isActive = true
        )
        var updatedDisplay = ""
        var updatedPlayers = false
        var deletedId: Long? = null

        val uiState = TournamentUiState(
            isLoaded = true,
            admins = listOf(headAdmin(), existingSub),
            hasHeadAdmin = true,
            currentAdmin = headAdmin()
        )

        composeRule.setContent {
            FifaTournamentTheme {
                AdminSubAdminsTab(
                    uiState = uiState,
                    onChangeOwnPin = { _, _ -> },
                    onCreateSubAdmin = { _, _, _, _, _, _, _, _ -> },
                    onUpdateSubAdmin = { target, display, _, _, _, pPlayers, _, _, _ ->
                        if (target.id == 22L) {
                            updatedDisplay = display
                            updatedPlayers = pPlayers
                        }
                    },
                    onDeleteSubAdmin = { deletedId = it.id }
                )
            }
        }
        composeRule.waitForIdle()

        // Slot #1 occupied, slots #2..#5 vacant
        composeRule.onNodeWithText("حساب #1").assertIsDisplayed()
        composeRule.onNodeWithTag("empty_sub_admin_slot_2").assertIsDisplayed()

        // Edit existing sub-admin
        composeRule.onNodeWithTag("edit_sub_admin_22").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sub_admin_display_input").performTextClearance()
        composeRule.onNodeWithTag("sub_admin_display_input").performTextInput("عمر المنسق")
        composeRule.onNodeWithTag("perm_players").performScrollTo().performClick()
        composeRule.onNodeWithTag("sub_admin_save_button").performClick()
        composeRule.waitForIdle()

        assertEquals("عمر المنسق", updatedDisplay)
        assertTrue(updatedPlayers)

        // Delete existing sub-admin with confirmation dialog
        composeRule.onNodeWithTag("delete_sub_admin_22").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_delete_sub_admin").performClick()
        composeRule.waitForIdle()

        assertEquals(22L, deletedId)
    }
}
