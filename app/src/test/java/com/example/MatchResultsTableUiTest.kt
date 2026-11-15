package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.ui.components.MatchResultsTableCard
import com.example.ui.theme.FifaTournamentTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchResultsTableUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val p1 = PlayerEntity(
        id = 1L,
        tournamentId = 10L,
        name = "ياسين",
        elysiumNetId = "EN-Yassin",
        clubName = "Real Madrid",
        level = PlayerLevel.PRO,
        ovrRating = 90,
        status = PlayerStatus.APPROVED
    )
    private val p2 = PlayerEntity(
        id = 2L,
        tournamentId = 10L,
        name = "أمين",
        elysiumNetId = "EN-Amine",
        clubName = "PSG",
        level = PlayerLevel.PRO,
        ovrRating = 88,
        status = PlayerStatus.APPROVED
    )
    private val p3 = PlayerEntity(
        id = 3L,
        tournamentId = 10L,
        name = "كريم",
        elysiumNetId = "EN-Karim",
        clubName = "Man City",
        level = PlayerLevel.INTERMEDIATE,
        ovrRating = 82,
        status = PlayerStatus.APPROVED
    )

    private val playersById = mapOf(1L to p1, 2L to p2, 3L to p3)

    @Test
    fun readOnlyVisitorSeesTableScoresWithoutGoalEditControls() {
        val completedMatch = MatchEntity(
            id = 101L,
            tournamentId = 10L,
            stage = MatchStage.GROUP,
            roundNumber = 1,
            player1Id = 1L,
            player2Id = 2L,
            leg1Score1 = 3,
            leg1Score2 = 1,
            isCompleted = true,
            winnerId = 1L,
            scheduledLabel = "المجموعة A"
        )

        composeRule.setContent {
            FifaTournamentTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MatchResultsTableCard(
                        matches = listOf(completedMatch),
                        playersById = playersById,
                        canEdit = false,
                        onQuickUpdateGoals = { _, _, _ -> },
                        onOpenDetailedEditor = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("match_results_table_card").assertIsDisplayed()
        composeRule.onNodeWithText("ياسين").assertIsDisplayed()
        composeRule.onNodeWithText("أمين").assertIsDisplayed()
        composeRule.onNodeWithText("3 : 1").assertIsDisplayed()
        composeRule.onNodeWithTag("match_101_p1_plus").assertDoesNotExist()
        composeRule.onNodeWithTag("match_101_detail_edit").assertDoesNotExist()
    }

    @Test
    fun adminCanIncrementAndDecrementPerPlayerGoalsAndSaveResult() {
        val pendingMatch = MatchEntity(
            id = 202L,
            tournamentId = 10L,
            stage = MatchStage.GROUP,
            roundNumber = 1,
            player1Id = 1L,
            player2Id = 2L,
            isCompleted = false,
            scheduledLabel = "الجولة 1"
        )

        var savedMatch: MatchEntity? = null
        var savedP1 = -1
        var savedP2 = -1

        composeRule.setContent {
            FifaTournamentTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MatchResultsTableCard(
                        matches = listOf(pendingMatch),
                        playersById = playersById,
                        canEdit = true,
                        onQuickUpdateGoals = { m, g1, g2 ->
                            savedMatch = m
                            savedP1 = g1
                            savedP2 = g2
                        },
                        onOpenDetailedEditor = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Increment Player 1 goals to 3, then decrement once to 2
        repeat(3) {
            composeRule.onNodeWithTag("match_202_p1_plus").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("match_202_p1_minus").performScrollTo().performClick()

        // Increment Player 2 goals to 1
        composeRule.onNodeWithTag("match_202_p2_plus").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("match_202_p1_goals").assertTextEquals("2")
        composeRule.onNodeWithTag("match_202_p2_goals").assertTextEquals("1")
        composeRule.onNodeWithTag("match_202_save_goals").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(202L, savedMatch?.id)
        assertEquals(2, savedP1)
        assertEquals(1, savedP2)
    }

    @Test
    fun filterChipsFilterMatchesCorrectly() {
        val groupDone = MatchEntity(
            id = 1L,
            tournamentId = 10L,
            stage = MatchStage.GROUP,
            roundNumber = 1,
            player1Id = 1L,
            player2Id = 2L,
            leg1Score1 = 2,
            leg1Score2 = 0,
            isCompleted = true,
            winnerId = 1L,
            scheduledLabel = "المجموعة A"
        )
        val knockoutPending = MatchEntity(
            id = 2L,
            tournamentId = 10L,
            stage = MatchStage.FINAL,
            roundNumber = 1,
            player1Id = 1L,
            player2Id = 3L,
            isCompleted = false,
            scheduledLabel = "النهائي"
        )

        composeRule.setContent {
            FifaTournamentTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MatchResultsTableCard(
                        matches = listOf(groupDone, knockoutPending),
                        playersById = playersById,
                        canEdit = true,
                        onQuickUpdateGoals = { _, _, _ -> },
                        onOpenDetailedEditor = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Filter to pending only -> row 2 visible, row 1 hidden
        composeRule.onNodeWithTag("match_filter_pending").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("match_row_2").assertIsDisplayed()
        composeRule.onNodeWithTag("match_row_1").assertDoesNotExist()

        // Filter to completed only -> row 1 visible, row 2 hidden
        composeRule.onNodeWithTag("match_filter_completed").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("match_row_1").assertIsDisplayed()
        composeRule.onNodeWithTag("match_row_2").assertDoesNotExist()

        // Filter to knockout only -> row 2 visible, row 1 hidden
        composeRule.onNodeWithTag("match_filter_knockout").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("match_row_2").assertIsDisplayed()
        composeRule.onNodeWithTag("match_row_1").assertDoesNotExist()
    }

    @Test
    fun tiedKnockoutScorePromptsDetailedPenaltyEditorInsteadOfQuickSave() {
        val knockoutMatch = MatchEntity(
            id = 303L,
            tournamentId = 10L,
            stage = MatchStage.SEMI_FINAL,
            roundNumber = 1,
            player1Id = 1L,
            player2Id = 2L,
            isTwoLegged = false,
            isCompleted = false,
            scheduledLabel = "نصف النهائي"
        )

        var quickSaved: MatchEntity? = null
        var detailedOpenedFor: MatchEntity? = null

        composeRule.setContent {
            FifaTournamentTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MatchResultsTableCard(
                        matches = listOf(knockoutMatch),
                        playersById = playersById,
                        canEdit = true,
                        onQuickUpdateGoals = { m, _, _ -> quickSaved = m },
                        onOpenDetailedEditor = { detailedOpenedFor = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Make score 1 - 1 tie in knockout single-leg match
        composeRule.onNodeWithTag("match_303_p1_plus").performScrollTo().performClick()
        composeRule.onNodeWithTag("match_303_p2_plus").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("إدخال ركلات الترجيح").assertIsDisplayed()
        composeRule.onNodeWithTag("match_303_save_goals").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertNull(quickSaved)
        assertEquals(303L, detailedOpenedFor?.id)
    }
}
