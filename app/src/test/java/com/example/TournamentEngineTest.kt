package com.example

import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentGroupEntity
import com.example.data.model.KnockoutTieRule
import com.example.data.model.MatchStage
import com.example.data.model.PlayerStatus
import com.example.domain.ScoreResolution
import com.example.domain.TournamentEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TournamentEngineTest {

    private fun fakePlayer(id: Long, name: String, groupId: Long? = null) = PlayerEntity(
        id = id,
        tournamentId = 1L,
        name = name,
        elysiumNetId = "ely_$id",
        status = PlayerStatus.APPROVED,
        groupId = groupId
    )

    @Test
    fun roundRobinGeneratesUniquePairingsForFourPlayers() {
        val matches = TournamentEngine.generateRoundRobinMatches(
            tournamentId = 1L,
            stage = MatchStage.GROUP,
            groupId = 10L,
            playerIds = listOf(1L, 2L, 3L, 4L)
        )
        assertEquals(6, matches.size)
        val pairs = matches.map { setOf(it.player1Id!!, it.player2Id!!) }.toSet()
        assertEquals(6, pairs.size)
    }

    @Test
    fun roundRobinHandlesOddPlayerCountWithoutSelfMatches() {
        val matches = TournamentEngine.generateRoundRobinMatches(
            tournamentId = 1L,
            stage = MatchStage.GROUP,
            groupId = 10L,
            playerIds = listOf(1L, 2L, 3L, 4L, 5L)
        )
        assertEquals(10, matches.size)
        assertTrue(matches.all { it.player1Id != it.player2Id })
    }

    @Test
    fun groupStandingsSortByPointsThenGoalDifferenceAndMarkQualifiers() {
        val players = listOf(
            fakePlayer(1, "أحمد", 1),
            fakePlayer(2, "خالد", 1),
            fakePlayer(3, "ياسين", 1),
            fakePlayer(4, "عمر", 1)
        )
        val matches = listOf(
            MatchEntity(id = 1, tournamentId = 1, stage = MatchStage.GROUP, groupId = 1, player1Id = 1, player2Id = 2, leg1Score1 = 3, leg1Score2 = 1, isCompleted = true, winnerId = 1),
            MatchEntity(id = 2, tournamentId = 1, stage = MatchStage.GROUP, groupId = 1, player1Id = 3, player2Id = 4, leg1Score1 = 1, leg1Score2 = 1, isCompleted = true, winnerId = null),
            MatchEntity(id = 3, tournamentId = 1, stage = MatchStage.GROUP, groupId = 1, player1Id = 1, player2Id = 3, leg1Score1 = 2, leg1Score2 = 0, isCompleted = true, winnerId = 1),
            MatchEntity(id = 4, tournamentId = 1, stage = MatchStage.GROUP, groupId = 1, player1Id = 2, player2Id = 4, leg1Score1 = 4, leg1Score2 = 0, isCompleted = true, winnerId = 2)
        )
        val standings = TournamentEngine.computeStandings(players, matches, qualifiersCount = 2)
        assertEquals(1L, standings[0].player.id) // 6 pts
        assertEquals(2L, standings[1].player.id) // 3 pts, +2 GD
        assertTrue(standings[0].isQualified)
        assertTrue(standings[1].isQualified)
        assertFalse(standings[2].isQualified)
    }

    @Test
    fun knockoutTreeBuildsLinkedRoundsFromR16ToFinal() {
        val seeds = (1L..16L).toList()
        val drafts = TournamentEngine.buildKnockoutTreeDrafts(
            tournamentId = 1L,
            seededSlots = seeds,
            tieRule = KnockoutTieRule.TWO_LEGS_FINAL_SINGLE
        )
        // 1 Final + 2 Semis + 4 Quarters + 8 R16 = 15 matches
        assertEquals(15, drafts.size)
        val finalMatch = drafts.first()
        assertEquals(MatchStage.FINAL, finalMatch.entity.stage)
        assertFalse(finalMatch.entity.isTwoLegged)
        val semis = drafts.filter { it.entity.stage == MatchStage.SEMI_FINAL }
        assertEquals(2, semis.size)
        assertTrue(semis.all { it.entity.isTwoLegged && it.parentTempIndex == 0 })
    }

    @Test
    fun knockoutTieRequiresPenaltiesAndDeterminesWinner() {
        val match = MatchEntity(
            id = 99,
            tournamentId = 1,
            stage = MatchStage.QUARTER_FINAL,
            player1Id = 10L,
            player2Id = 20L,
            isTwoLegged = false
        )
        val missingPen = TournamentEngine.resolveMatchResult(match, 2, 2, null, null, null, null)
        assertTrue(missingPen is ScoreResolution.Error)

        val resolved = TournamentEngine.resolveMatchResult(match, 2, 2, null, null, 5, 4)
        assertTrue(resolved is ScoreResolution.Valid)
        assertEquals(10L, (resolved as ScoreResolution.Valid).winnerId)
    }

    @Test
    fun distributePlayersEvenlyAcrossGroups() {
        val players = (1L..8L).map { fakePlayer(it, "لاعب $it") }
        val groups = listOf(
            TournamentGroupEntity(1, 1, "أ", 0),
            TournamentGroupEntity(2, 1, "ب", 1)
        )
        val distributed = TournamentEngine.distributePlayersIntoGroups(players, groups, shuffle = false)
        assertEquals(4, distributed.count { it.groupId == 1L })
        assertEquals(4, distributed.count { it.groupId == 2L })
    }
}
