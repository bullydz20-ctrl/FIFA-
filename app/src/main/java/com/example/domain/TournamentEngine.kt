package com.example.domain

import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentGroupEntity
import com.example.data.model.KnockoutTieRule
import com.example.data.model.MatchStage
import kotlin.math.max

data class GroupStandingRow(
    val player: PlayerEntity,
    val position: Int,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int,
    val isQualified: Boolean
)

data class PlayerStats(
    val player: PlayerEntity,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsScored: Int,
    val goalsConceded: Int,
    val cleanSheets: Int,
    val winRatePercent: Int
)

data class TournamentOverviewStats(
    val totalMatches: Int,
    val completedMatches: Int,
    val totalGoals: Int,
    val topScorer: PlayerStats?,
    val bestDefense: PlayerStats?,
    val highestWinRate: PlayerStats?
)

sealed class ScoreResolution {
    data class Valid(
        val isCompleted: Boolean,
        val winnerId: Long?
    ) : ScoreResolution()

    data class Error(val messageAr: String) : ScoreResolution()
}

object TournamentEngine {

    private val arabicGroupLetters = listOf(
        "المجموعة أ", "المجموعة ب", "المجموعة ج", "المجموعة د",
        "المجموعة هـ", "المجموعة و", "المجموعة ز", "المجموعة ح"
    )

    fun groupDisplayName(index: Int): String {
        return arabicGroupLetters.getOrElse(index) { "المجموعة ${index + 1}" }
    }

    /**
     * Generates Round-Robin fixtures for a given list of players using the circle algorithm.
     */
    fun generateRoundRobinMatches(
        tournamentId: Long,
        stage: MatchStage,
        groupId: Long?,
        playerIds: List<Long>,
        doubleRoundRobin: Boolean = false
    ): List<MatchEntity> {
        if (playerIds.size < 2) return emptyList()

        val rotated = playerIds.map<Long, Long?> { it }.toMutableList()
        if (rotated.size % 2 != 0) {
            rotated.add(null) // BYE
        }

        val n = rotated.size
        val numRounds = n - 1
        val half = n / 2
        val result = mutableListOf<MatchEntity>()

        for (round in 0 until numRounds) {
            for (i in 0 until half) {
                val p1 = rotated[i]
                val p2 = rotated[n - 1 - i]
                if (p1 != null && p2 != null) {
                    val home = if (round % 2 == 0) p1 else p2
                    val away = if (round % 2 == 0) p2 else p1
                    result.add(
                        MatchEntity(
                            tournamentId = tournamentId,
                            stage = stage,
                            groupId = groupId,
                            roundNumber = round + 1,
                            bracketIndex = result.size,
                            player1Id = home,
                            player2Id = away,
                            isTwoLegged = false,
                            scheduledLabel = "الجولة ${round + 1}"
                        )
                    )
                }
            }
            // Rotate all except index 0
            val last = rotated.removeAt(n - 1)
            rotated.add(1, last)
        }

        if (doubleRoundRobin) {
            val firstLegCount = result.size
            for (i in 0 until firstLegCount) {
                val m = result[i]
                result.add(
                    m.copy(
                        roundNumber = m.roundNumber + numRounds,
                        bracketIndex = result.size,
                        player1Id = m.player2Id,
                        player2Id = m.player1Id,
                        scheduledLabel = "الجولة ${m.roundNumber + numRounds}"
                    )
                )
            }
        }

        return result
    }

    /**
     * Distributes players across groups evenly (or preserves manual group assignments).
     */
    fun distributePlayersIntoGroups(
        players: List<PlayerEntity>,
        groups: List<TournamentGroupEntity>,
        shuffle: Boolean
    ): List<PlayerEntity> {
        if (groups.isEmpty() || players.isEmpty()) return players
        val orderedPlayers = if (shuffle) players.shuffled() else players
        return orderedPlayers.mapIndexed { idx, player ->
            val targetGroup = groups[idx % groups.size]
            player.copy(groupId = targetGroup.id, seedOrder = idx + 1)
        }
    }

    /**
     * Computes live standings for a group (or league when groupId == null).
     */
    fun computeStandings(
        players: List<PlayerEntity>,
        matches: List<MatchEntity>,
        qualifiersCount: Int
    ): List<GroupStandingRow> {
        if (players.isEmpty()) return emptyList()

        data class Acc(
            var played: Int = 0,
            var wins: Int = 0,
            var draws: Int = 0,
            var losses: Int = 0,
            var gf: Int = 0,
            var ga: Int = 0,
            var pts: Int = 0
        )

        val map = players.associate { it.id to Acc() }

        for (match in matches) {
            if (!match.isCompleted) continue
            val p1 = match.player1Id ?: continue
            val p2 = match.player2Id ?: continue
            val s1 = match.leg1Score1 ?: continue
            val s2 = match.leg1Score2 ?: continue

            val a1 = map[p1]
            val a2 = map[p2]

            if (a1 != null) {
                a1.played += 1
                a1.gf += s1
                a1.ga += s2
                when {
                    s1 > s2 -> { a1.wins += 1; a1.pts += 3 }
                    s1 == s2 -> { a1.draws += 1; a1.pts += 1 }
                    else -> { a1.losses += 1 }
                }
            }
            if (a2 != null) {
                a2.played += 1
                a2.gf += s2
                a2.ga += s1
                when {
                    s2 > s1 -> { a2.wins += 1; a2.pts += 3 }
                    s2 == s1 -> { a2.draws += 1; a2.pts += 1 }
                    else -> { a2.losses += 1 }
                }
            }
        }

        val sorted = players.map { player ->
            val a = map[player.id] ?: Acc()
            GroupStandingRow(
                player = player,
                position = 0,
                played = a.played,
                wins = a.wins,
                draws = a.draws,
                losses = a.losses,
                goalsFor = a.gf,
                goalsAgainst = a.ga,
                goalDifference = a.gf - a.ga,
                points = a.pts,
                isQualified = false
            )
        }.sortedWith(
            compareByDescending<GroupStandingRow> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
                .thenByDescending { it.wins }
                .thenBy { it.player.name }
        )

        return sorted.mapIndexed { index, row ->
            row.copy(
                position = index + 1,
                isQualified = index < qualifiersCount
            )
        }
    }

    /**
     * Cross-seeds qualifiers from groups into knockout order so groupmates only meet in the final.
     * Supports 2, 4, 8, or 16 qualifiers (and pads with null BYEs if count is non-power-of-2).
     */
    fun buildKnockoutSeedPairsFromGroups(
        groupStandings: List<List<GroupStandingRow>>,
        qualifiersPerGroup: Int
    ): List<Long?> {
        val allQualified = mutableListOf<Long>()
        if (groupStandings.size >= 2 && qualifiersPerGroup >= 2) {
            // Pair adjacent groups: 1A vs 2B, 1B vs 2A, 1C vs 2D, 1D vs 2C...
            val firsts = groupStandings.mapNotNull { it.getOrNull(0)?.player?.id }
            val seconds = groupStandings.mapNotNull { it.getOrNull(1)?.player?.id }
            val paired = mutableListOf<Long>()
            var idx = 0
            while (idx < firsts.size) {
                if (idx + 1 < firsts.size && idx + 1 < seconds.size) {
                    paired.add(firsts[idx])
                    paired.add(seconds[idx + 1])
                    paired.add(firsts[idx + 1])
                    paired.add(seconds[idx])
                    idx += 2
                } else {
                    paired.add(firsts[idx])
                    seconds.getOrNull(idx)?.let { paired.add(it) }
                    idx += 1
                }
            }
            // Add any 3rd/4th qualifiers if qualifiersPerGroup > 2
            for (pos in 2 until qualifiersPerGroup) {
                for (group in groupStandings) {
                    group.getOrNull(pos)?.player?.id?.let {
                        if (!paired.contains(it)) paired.add(it)
                    }
                }
            }
            allQualified.addAll(paired)
        } else {
            for (pos in 0 until qualifiersPerGroup) {
                for (group in groupStandings) {
                    group.getOrNull(pos)?.player?.id?.let { allQualified.add(it) }
                }
            }
        }

        val capped = allQualified.take(16)
        val bracketSize = nextPowerOfTwo(max(2, capped.size)).coerceAtMost(16)
        val padded = MutableList<Long?>(bracketSize) { null }
        for (i in capped.indices) {
            padded[i] = capped[i]
        }
        return padded
    }

    /**
     * Generates a linked knockout bracket tree (ordered from Final down to first round for easy DB insertion).
     * Each entry is paired with its parent index in the output list so repositories can assign real IDs.
     */
    data class BracketNodeDraft(
        val tempIndex: Int,
        val parentTempIndex: Int?,
        val slotInParent: Int,
        val entity: MatchEntity
    )

    fun buildKnockoutTreeDrafts(
        tournamentId: Long,
        seededSlots: List<Long?>,
        tieRule: KnockoutTieRule
    ): List<BracketNodeDraft> {
        val bracketSize = nextPowerOfTwo(max(2, seededSlots.size)).coerceIn(2, 16)
        val padded = List(bracketSize) { i -> seededSlots.getOrNull(i) }

        val stages = when (bracketSize) {
            16 -> listOf(
                MatchStage.FINAL,
                MatchStage.SEMI_FINAL,
                MatchStage.QUARTER_FINAL,
                MatchStage.ROUND_OF_16
            )
            8 -> listOf(
                MatchStage.FINAL,
                MatchStage.SEMI_FINAL,
                MatchStage.QUARTER_FINAL
            )
            4 -> listOf(
                MatchStage.FINAL,
                MatchStage.SEMI_FINAL
            )
            else -> listOf(MatchStage.FINAL)
        }

        // Build nodes top-down (Final at tempIndex 0, then Semis, then Quarters, then R16)
        val drafts = mutableListOf<BracketNodeDraft>()
        val roundStartIndices = mutableMapOf<Int, Int>() // roundDepth 0 = Final, 1 = Semi, ...

        for ((depth, stage) in stages.withIndex()) {
            val matchCount = 1 shl depth
            roundStartIndices[depth] = drafts.size
            val isFirstRound = (depth == stages.lastIndex)
            val isFinal = (stage == MatchStage.FINAL)
            val twoLegged = (tieRule == KnockoutTieRule.TWO_LEGS_FINAL_SINGLE && !isFinal)

            for (i in 0 until matchCount) {
                val tempIndex = drafts.size
                val parentDepth = depth - 1
                val parentTempIndex = if (parentDepth >= 0) {
                    roundStartIndices.getValue(parentDepth) + (i / 2)
                } else null
                val slotInParent = if (i % 2 == 0) 1 else 2

                val p1 = if (isFirstRound) padded.getOrNull(i * 2) else null
                val p2 = if (isFirstRound) padded.getOrNull(i * 2 + 1) else null

                val match = MatchEntity(
                    tournamentId = tournamentId,
                    stage = stage,
                    groupId = null,
                    roundNumber = stage.order,
                    bracketIndex = i,
                    player1Id = p1,
                    player2Id = p2,
                    isTwoLegged = twoLegged,
                    isCompleted = false,
                    winnerId = null,
                    nextMatchId = null,
                    nextMatchSlot = slotInParent,
                    scheduledLabel = stage.labelAr
                )
                drafts.add(BracketNodeDraft(tempIndex, parentTempIndex, slotInParent, match))
            }
        }

        return drafts
    }

    /**
     * Evaluates match scores and returns winner or validation error in Arabic.
     */
    fun resolveMatchResult(
        match: MatchEntity,
        leg1S1: Int?,
        leg1S2: Int?,
        leg2S1: Int?,
        leg2S2: Int?,
        pen1: Int?,
        pen2: Int?
    ): ScoreResolution {
        val p1 = match.player1Id
        val p2 = match.player2Id
        if (p1 == null || p2 == null) {
            return ScoreResolution.Error("لا يمكن تسجيل النتيجة قبل تحديد طرفي المباراة.")
        }

        if (leg1S1 == null || leg1S2 == null) {
            return ScoreResolution.Valid(isCompleted = false, winnerId = null)
        }
        if (leg1S1 < 0 || leg1S2 < 0 || (leg2S1 != null && leg2S1 < 0) || (leg2S2 != null && leg2S2 < 0)) {
            return ScoreResolution.Error("عدد الأهداف لا يمكن أن يكون سالباً.")
        }

        val isGroupOrLeague = (match.stage == MatchStage.GROUP || match.stage == MatchStage.LEAGUE)
        if (isGroupOrLeague) {
            val winner = when {
                leg1S1 > leg1S2 -> p1
                leg1S2 > leg1S1 -> p2
                else -> null
            }
            return ScoreResolution.Valid(isCompleted = true, winnerId = winner)
        }

        // Knockout match
        if (match.isTwoLegged) {
            if (leg2S1 == null || leg2S2 == null) {
                // Leg 1 saved, waiting for Leg 2
                return ScoreResolution.Valid(isCompleted = false, winnerId = null)
            }
            val agg1 = leg1S1 + leg2S1
            val agg2 = leg1S2 + leg2S2
            return when {
                agg1 > agg2 -> ScoreResolution.Valid(isCompleted = true, winnerId = p1)
                agg2 > agg1 -> ScoreResolution.Valid(isCompleted = true, winnerId = p2)
                else -> resolvePenalties(p1, p2, pen1, pen2)
            }
        } else {
            return when {
                leg1S1 > leg1S2 -> ScoreResolution.Valid(isCompleted = true, winnerId = p1)
                leg1S2 > leg1S1 -> ScoreResolution.Valid(isCompleted = true, winnerId = p2)
                else -> resolvePenalties(p1, p2, pen1, pen2)
            }
        }
    }

    private fun resolvePenalties(
        p1: Long,
        p2: Long,
        pen1: Int?,
        pen2: Int?
    ): ScoreResolution {
        if (pen1 == null || pen2 == null) {
            return ScoreResolution.Error("انتهى اللقاء بالتعادل — يرجى إدخال نتيجة ركلات الترجيح لتحديد المتأهل.")
        }
        if (pen1 < 0 || pen2 < 0) {
            return ScoreResolution.Error("نتيجة ركلات الترجيح غير صالحة.")
        }
        if (pen1 == pen2) {
            return ScoreResolution.Error("لا يمكن أن تنتهي ركلات الترجيح بالتعادل.")
        }
        val winner = if (pen1 > pen2) p1 else p2
        return ScoreResolution.Valid(isCompleted = true, winnerId = winner)
    }

    /**
     * Computes per-player and tournament-wide statistics across all completed matches.
     */
    fun computeTournamentStats(
        players: List<PlayerEntity>,
        matches: List<MatchEntity>
    ): Pair<List<PlayerStats>, TournamentOverviewStats> {
        data class Acc(
            var played: Int = 0,
            var wins: Int = 0,
            var draws: Int = 0,
            var losses: Int = 0,
            var gs: Int = 0,
            var gc: Int = 0,
            var cs: Int = 0
        )

        val statsMap = players.associate { it.id to Acc() }
        var totalGoals = 0
        var completedCount = 0

        for (match in matches) {
            if (match.isCompleted) {
                completedCount += 1
            }
            val p1 = match.player1Id
            val p2 = match.player2Id
            if (p1 == null || p2 == null) continue

            val s1 = match.leg1Score1
            val s2 = match.leg1Score2
            if (s1 != null && s2 != null && match.isCompleted) {
                val leg2G1 = if (match.isTwoLegged) (match.leg2Score1 ?: 0) else 0
                val leg2G2 = if (match.isTwoLegged) (match.leg2Score2 ?: 0) else 0
                val tot1 = s1 + leg2G1
                val tot2 = s2 + leg2G2
                totalGoals += (tot1 + tot2)

                val a1 = statsMap[p1]
                val a2 = statsMap[p2]

                if (a1 != null) {
                    a1.played += 1
                    a1.gs += tot1
                    a1.gc += tot2
                    if (s2 == 0 && (!match.isTwoLegged || leg2G2 == 0)) a1.cs += 1
                    when {
                        match.winnerId == p1 -> a1.wins += 1
                        match.winnerId == p2 -> a1.losses += 1
                        tot1 > tot2 -> a1.wins += 1
                        tot1 == tot2 -> a1.draws += 1
                        else -> a1.losses += 1
                    }
                }
                if (a2 != null) {
                    a2.played += 1
                    a2.gs += tot2
                    a2.gc += tot1
                    if (s1 == 0 && (!match.isTwoLegged || leg2G1 == 0)) a2.cs += 1
                    when {
                        match.winnerId == p2 -> a2.wins += 1
                        match.winnerId == p1 -> a2.losses += 1
                        tot2 > tot1 -> a2.wins += 1
                        tot2 == tot1 -> a2.draws += 1
                        else -> a2.losses += 1
                    }
                }
            }
        }

        val playerStatsList = players.map { p ->
            val a = statsMap[p.id] ?: Acc()
            val winRate = if (a.played > 0) ((a.wins * 100.0) / a.played).toInt() else 0
            PlayerStats(
                player = p,
                matchesPlayed = a.played,
                wins = a.wins,
                draws = a.draws,
                losses = a.losses,
                goalsScored = a.gs,
                goalsConceded = a.gc,
                cleanSheets = a.cs,
                winRatePercent = winRate
            )
        }.sortedWith(
            compareByDescending<PlayerStats> { it.wins }
                .thenByDescending { it.goalsScored }
                .thenBy { it.goalsConceded }
        )

        val activePlayers = playerStatsList.filter { it.matchesPlayed > 0 }
        val topScorer = activePlayers.maxWithOrNull(
            compareBy<PlayerStats> { it.goalsScored }.thenByDescending { it.matchesPlayed }
        )
        val bestDefense = activePlayers.minWithOrNull(
            compareBy<PlayerStats> { it.goalsConceded.toDouble() / it.matchesPlayed }
                .thenByDescending { it.cleanSheets }
        )
        val highestWinRate = activePlayers.maxWithOrNull(
            compareBy<PlayerStats> { it.winRatePercent }.thenBy { it.wins }
        )

        val overview = TournamentOverviewStats(
            totalMatches = matches.size,
            completedMatches = completedCount,
            totalGoals = totalGoals,
            topScorer = topScorer,
            bestDefense = bestDefense,
            highestWinRate = highestWinRate
        )

        return playerStatsList to overview
    }

    fun nextPowerOfTwo(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }
}
