package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SessionPreferences
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import com.example.data.repository.RepoResult
import com.example.data.repository.TournamentRepository
import com.example.domain.TournamentEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TournamentRepositoryTest {

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
    fun subAdminLimitAndPinAuthWorkCorrectly() = runTest {
        val headRes = repo.setupHeadAdmin("headadmin", "المشرف", "123490")
        assertTrue(headRes is RepoResult.Success)
        val headId = (headRes as RepoResult.Success).data.id

        for (i in 1..5) {
            val subRes = repo.createSubAdmin(
                actingAdminId = headId,
                username = "sub$i",
                displayName = "مساعد $i",
                pin = "20240$i",
                canManageTournament = false,
                canManagePlayers = true,
                canRunDraw = false,
                canEditScores = true,
                canCrownChampion = false
            )
            assertTrue(subRes is RepoResult.Success)
        }

        // 6th sub-admin must be rejected
        val sixth = repo.createSubAdmin(
            actingAdminId = headId,
            username = "sub6",
            displayName = "مساعد 6",
            pin = "999900",
            canManageTournament = false,
            canManagePlayers = true,
            canRunDraw = false,
            canEditScores = true,
            canCrownChampion = false
        )
        assertTrue(sixth is RepoResult.Error)

        // Verify login fails with wrong PIN and succeeds with right PIN
        assertTrue(repo.loginAdmin("sub1", "000000") is RepoResult.Error)
        assertTrue(repo.loginAdmin("sub1", "202401") is RepoResult.Success)
    }

    @Test
    fun fullTournamentLifecycleAutoClosesRegistrationGeneratesKnockoutAndCascadesEdits() = runTest {
        repo.ensureDefaultTournamentShellIfEmpty()
        var tournament = repo.latestTournament.first()!!
        // Configure 4-player, 2-group, 2-qualifiers-per-group tournament
        repo.saveTournamentSettings(
            tournament.copy(
                maxParticipants = 4,
                format = TournamentFormat.GROUPS_KNOCKOUT,
                groupCount = 2,
                qualifiersPerGroup = 1
            )
        )
        tournament = repo.latestTournament.first()!!

        // Register 4 players
        for (i in 1..4) {
            val reg = repo.registerPlayer(
                tournamentId = tournament.id,
                name = "لاعب $i",
                elysiumNetId = "ely_$i",
                avatarUri = null,
                level = PlayerLevel.PRO,
                clubName = "Real Madrid"
            )
            assertTrue(reg is RepoResult.Success)
        }

        // Approve all 4 -> should auto-close registration!
        val registered = repo.players.first()
        registered.forEach { p ->
            assertTrue(repo.updatePlayerStatus(p, PlayerStatus.APPROVED) is RepoResult.Success)
        }
        tournament = repo.latestTournament.first()!!
        assertEquals(TournamentStatus.REGISTRATION_CLOSED, tournament.status)

        // 5th registration must be rejected
        val fifth = repo.registerPlayer(tournament.id, "لاعب 5", "ely_5", null, PlayerLevel.PRO, "")
        assertTrue(fifth is RepoResult.Error)

        // Run Draw
        assertTrue(repo.runDrawAndGenerateSchedule() is RepoResult.Success)
        val groups = repo.groups.first()
        assertEquals(2, groups.size)
        val groupMatches = repo.matches.first()
        assertEquals(2, groupMatches.size) // 2 players per group -> 1 match each

        // Record group scores
        for (m in groupMatches) {
            assertTrue(repo.updateMatchScore(m, 3, 1, null, null, null, null) is RepoResult.Success)
        }

        val approved = repo.players.first().filter { it.status == PlayerStatus.APPROVED }
        val completedMatches = repo.matches.first()
        val groupStandings = groups.map { g ->
            TournamentEngine.computeStandings(
                players = approved.filter { it.groupId == g.id },
                matches = completedMatches.filter { it.groupId == g.id },
                qualifiersCount = 1
            )
        }

        // Generate Knockout (2 qualifiers -> 1 Final)
        assertTrue(repo.generateKnockoutFromCurrentStandings(groupStandings, emptyList()) is RepoResult.Success)
        val finalMatch = repo.matches.first().first { it.stage == MatchStage.FINAL }
        assertNotNull(finalMatch.player1Id)
        assertNotNull(finalMatch.player2Id)

        // Play Final and Crown Champion
        assertTrue(repo.updateMatchScore(finalMatch, 2, 1, null, null, null, null) is RepoResult.Success)
        val updatedFinal = repo.matches.first().first { it.stage == MatchStage.FINAL }
        assertEquals(finalMatch.player1Id, updatedFinal.winnerId)

        assertTrue(repo.crownChampion(updatedFinal.winnerId) is RepoResult.Success)
        assertEquals(TournamentStatus.COMPLETED, repo.latestTournament.first()!!.status)

        // Reopen Final -> Champion cleared automatically
        assertTrue(repo.updateMatchScore(updatedFinal, null, null, null, null, null, null) is RepoResult.Success)
        assertNull(repo.latestTournament.first()!!.championPlayerId)

        // Verify Room @Relation TournamentWithDetails returns participants, groups, and matches
        val details = db.tournamentDao().observeTournamentWithDetails(tournament.id).first()!!
        assertEquals(4, details.participants.size)
        assertEquals(2, details.groups.size)
        assertEquals(3, details.matches.size)

        // Verify ForeignKey CASCADE deletes child rows automatically when tournament is deleted
        db.tournamentDao().deleteTournament(tournament.id)
        assertTrue(db.playerDao().getPlayers(tournament.id).isEmpty())
        assertTrue(db.groupDao().getGroups(tournament.id).isEmpty())
        assertTrue(db.matchDao().getMatches(tournament.id).isEmpty())
    }
}
