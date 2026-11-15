package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AdminAccountEntity
import com.example.data.local.AppDatabase
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.SessionPreferences
import com.example.data.model.AdminRole
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import com.example.data.repository.RepoResult
import com.example.data.repository.TournamentRepository
import com.example.ui.screens.BracketScreen
import com.example.ui.theme.FifaTournamentTheme
import com.example.ui.viewmodel.TournamentUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BracketInteractiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: TournamentRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TournamentRepository(context, db, SessionPreferences(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun repositoryAdvancesWinnersFromRoundOf16ToFinalAndCascadesResets() = runTest {
        repo.ensureDefaultTournamentShellIfEmpty()
        var tournament = repo.latestTournament.first()!!
        repo.saveTournamentSettings(
            tournament.copy(
                maxParticipants = 16,
                format = TournamentFormat.DIRECT_KNOCKOUT
            )
        )
        tournament = repo.latestTournament.first()!!

        for (i in 1..16) {
            assertTrue(
                repo.registerPlayer(
                    tournamentId = tournament.id,
                    name = "بطل $i",
                    elysiumNetId = "EN_$i",
                    avatarUri = null,
                    level = PlayerLevel.PRO,
                    clubName = "Club $i"
                ) is RepoResult.Success
            )
        }
        repo.players.first().forEach { p ->
            assertTrue(repo.updatePlayerStatus(p, PlayerStatus.APPROVED) is RepoResult.Success)
        }

        // Generate 16-player bracket (R16 -> QF -> SF -> Final)
        assertTrue(repo.runDrawAndGenerateSchedule() is RepoResult.Success)
        val initialMatches = repo.matches.first()
        assertEquals(8, initialMatches.count { it.stage == MatchStage.ROUND_OF_16 })
        assertEquals(4, initialMatches.count { it.stage == MatchStage.QUARTER_FINAL })
        assertEquals(2, initialMatches.count { it.stage == MatchStage.SEMI_FINAL })
        assertEquals(1, initialMatches.count { it.stage == MatchStage.FINAL })

        // Pick winners for first two R16 matches feeding into QF #0
        val r16Match0 = initialMatches.first { it.stage == MatchStage.ROUND_OF_16 && it.bracketIndex == 0 }
        val r16Match1 = initialMatches.first { it.stage == MatchStage.ROUND_OF_16 && it.bracketIndex == 1 }
        assertTrue(repo.updateKnockoutMatchWinner(r16Match0, r16Match0.player1Id) is RepoResult.Success)
        assertTrue(repo.updateKnockoutMatchWinner(r16Match1, r16Match1.player2Id) is RepoResult.Success)

        val qf0 = repo.matches.first().first { it.stage == MatchStage.QUARTER_FINAL && it.bracketIndex == 0 }
        assertEquals(r16Match0.player1Id, qf0.player1Id)
        assertEquals(r16Match1.player2Id, qf0.player2Id)

        // Advance QF #0 winner to SF #0
        assertTrue(repo.updateKnockoutMatchWinner(qf0, qf0.player1Id) is RepoResult.Success)
        val sf0AfterAdvance = repo.matches.first().first { it.stage == MatchStage.SEMI_FINAL && it.bracketIndex == 0 }
        assertEquals(r16Match0.player1Id, sf0AfterAdvance.player1Id)

        // Change R16 #0 winner -> QF #0 must reset winner & SF #0 slot cleared!
        val latestR16Match0 = repo.matches.first().first { it.id == r16Match0.id }
        assertTrue(repo.updateKnockoutMatchWinner(latestR16Match0, latestR16Match0.player2Id) is RepoResult.Success)

        val qf0AfterOverride = repo.matches.first().first { it.stage == MatchStage.QUARTER_FINAL && it.bracketIndex == 0 }
        assertEquals(latestR16Match0.player2Id, qf0AfterOverride.player1Id)
        assertNull(qf0AfterOverride.winnerId)

        val sf0AfterReset = repo.matches.first().first { it.stage == MatchStage.SEMI_FINAL && it.bracketIndex == 0 }
        assertNull(sf0AfterReset.player1Id)
    }

    @Test
    fun bracketUiDisplaysStagesFromRoundOf16ToFinalAndUpdatesWinner() {
        val samplePlayers = (1L..16L).map { id ->
            PlayerEntity(
                id = id,
                tournamentId = 1L,
                name = "لاعب $id",
                elysiumNetId = "EN_$id",
                clubName = "Team $id",
                level = PlayerLevel.PRO,
                ovrRating = 88,
                status = PlayerStatus.APPROVED
            )
        }

        val matches = listOf(
            // Round of 16 (first pair)
            MatchEntity(
                id = 10L, tournamentId = 1L, stage = MatchStage.ROUND_OF_16,
                roundNumber = 2, bracketIndex = 0, player1Id = 1L, player2Id = 2L,
                nextMatchId = 20L, nextMatchSlot = 1
            ),
            MatchEntity(
                id = 11L, tournamentId = 1L, stage = MatchStage.ROUND_OF_16,
                roundNumber = 2, bracketIndex = 1, player1Id = 3L, player2Id = 4L,
                nextMatchId = 20L, nextMatchSlot = 2
            ),
            // Quarter Final
            MatchEntity(
                id = 20L, tournamentId = 1L, stage = MatchStage.QUARTER_FINAL,
                roundNumber = 3, bracketIndex = 0, player1Id = 1L, player2Id = 3L,
                isCompleted = true, winnerId = 1L, nextMatchId = 30L, nextMatchSlot = 1
            ),
            // Semi Final
            MatchEntity(
                id = 30L, tournamentId = 1L, stage = MatchStage.SEMI_FINAL,
                roundNumber = 4, bracketIndex = 0, player1Id = 1L, player2Id = 5L,
                nextMatchId = 40L, nextMatchSlot = 1
            ),
            // Final
            MatchEntity(
                id = 40L, tournamentId = 1L, stage = MatchStage.FINAL,
                roundNumber = 5, bracketIndex = 0, player1Id = 1L, player2Id = 9L
            )
        )

        val admin = AdminAccountEntity(
            id = 99L,
            username = "Djilali",
            displayName = "Djilali",
            role = AdminRole.HEAD_ADMIN,
            pinHash = "h",
            pinSalt = "s",
            canEditScores = true
        )

        var selectedMatchId: Long? = null
        var selectedWinnerId: Long? = -1L
        var scoreEditorMatchId: Long? = null

        composeRule.setContent {
            FifaTournamentTheme {
                BracketScreen(
                    uiState = TournamentUiState(
                        isLoaded = true,
                        players = samplePlayers,
                        approvedPlayers = samplePlayers,
                        knockoutMatches = matches,
                        allMatches = matches,
                        currentAdmin = admin
                    ),
                    onEditMatch = { scoreEditorMatchId = it.id },
                    onSelectWinner = { m, wId ->
                        selectedMatchId = m.id
                        selectedWinnerId = wId
                    }
                )
            }
        }
        composeRule.waitForIdle()

        // All 4 roadmap stage filters + full tree + champion pedestal exist
        composeRule.onNodeWithTag("bracket_stage_filter_all").assertIsDisplayed()
        composeRule.onNodeWithTag("bracket_stage_filter_ROUND_OF_16").assertIsDisplayed()
        composeRule.onNodeWithTag("bracket_stage_filter_QUARTER_FINAL").assertIsDisplayed()
        composeRule.onNodeWithTag("bracket_stage_filter_SEMI_FINAL").assertIsDisplayed()
        composeRule.onNodeWithTag("bracket_stage_filter_FINAL").assertIsDisplayed()
        composeRule.onNodeWithTag("bracket_full_tree").assertIsDisplayed()

        // Filter to Round of 16 and pick Player 1 as winner of match 10
        composeRule.onNodeWithTag("bracket_stage_filter_ROUND_OF_16").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bracket_focused_stage_ROUND_OF_16").assertIsDisplayed()

        composeRule.onNodeWithTag("bracket_match_10").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bracket_pick_winner_p1_10").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(10L, selectedMatchId)
        assertEquals(1L, selectedWinnerId)

        // Filter to Quarter Final (which already has winnerId = 1L) and clear winner
        composeRule.onNodeWithTag("bracket_stage_filter_QUARTER_FINAL").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bracket_match_20").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bracket_clear_winner_20").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(20L, selectedMatchId)
        assertNull(selectedWinnerId)

        // Filter to Final and open detailed score editor
        composeRule.onNodeWithTag("bracket_stage_filter_FINAL").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bracket_champion_pedestal").assertIsDisplayed()
        composeRule.onNodeWithTag("bracket_match_40").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("bracket_open_score_dialog_40").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(40L, scoreEditorMatchId)
    }

    @Test
    fun visitorModeShowsBracketReadOnlyWithoutWinnerDialog() {
        val p1 = PlayerEntity(
            id = 1L, tournamentId = 1L, name = "زائر 1", elysiumNetId = "EN1",
            level = PlayerLevel.PRO, ovrRating = 88, status = PlayerStatus.APPROVED
        )
        val p2 = PlayerEntity(
            id = 2L, tournamentId = 1L, name = "زائر 2", elysiumNetId = "EN2",
            level = PlayerLevel.PRO, ovrRating = 87, status = PlayerStatus.APPROVED
        )
        val finalMatch = MatchEntity(
            id = 50L, tournamentId = 1L, stage = MatchStage.FINAL,
            roundNumber = 5, bracketIndex = 0, player1Id = 1L, player2Id = 2L
        )

        composeRule.setContent {
            FifaTournamentTheme {
                BracketScreen(
                    uiState = TournamentUiState(
                        isLoaded = true,
                        players = listOf(p1, p2),
                        approvedPlayers = listOf(p1, p2),
                        knockoutMatches = listOf(finalMatch),
                        currentAdmin = null
                    ),
                    onEditMatch = {},
                    onSelectWinner = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bracket_match_50").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("تحديث الفائز — المباراة النهائية").assertDoesNotExist()
    }
}
