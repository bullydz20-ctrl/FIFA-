package com.example.domain

import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.MatchStage
import com.example.ui.viewmodel.TournamentUiState

object TournamentExportFormatter {

    fun buildSuggestedFileName(uiState: TournamentUiState): String {
        val raw = uiState.tournament?.name ?: "fifa22_tournament"
        val asciiSlug = raw
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(28)
        return if (asciiSlug.isNotBlank()) {
            "${asciiSlug}_results.txt"
        } else {
            "fifa22_tournament_results.txt"
        }
    }

    fun buildFullSummaryText(uiState: TournamentUiState): String {
        val playersById = uiState.players.associateBy { it.id }
        val t = uiState.tournament
        val stats = uiState.overviewStats
        val finalMatch = uiState.knockoutMatches.find { it.stage == MatchStage.FINAL }
        val resolvedChampion = uiState.championPlayer
            ?: finalMatch?.winnerId?.let { playersById[it] }

        return buildString {
            appendLine("════════════════════════════════════════")
            appendLine("🏆 ملخص نتائج البطولة الرسمية")
            appendLine("════════════════════════════════════════")
            appendLine("البطولة: ${t?.name ?: "بطولة FIFA 22"}")
            appendLine("المجتمع: COMMUNITY PS4 JAILBREAK ONLINE")
            if (t != null) {
                appendLine("النظام: ${t.format.labelAr}")
                appendLine("الحالة: ${t.status.labelAr}")
                appendLine("المشاركون المعتمدون: ${uiState.approvedPlayers.size} / ${t.maxParticipants}")
            }
            appendLine("تقدم المباريات: ${stats.completedMatches} / ${stats.totalMatches} مكتملة")
            appendLine("إجمالي الأهداف المسجلة: ${stats.totalGoals}")
            appendLine()

            if (resolvedChampion != null) {
                appendLine("🥇 بطل البطولة: ${resolvedChampion.name} (${resolvedChampion.elysiumNetId})")
                if (resolvedChampion.clubName.isNotBlank()) {
                    appendLine("   النادي: ${resolvedChampion.clubName}")
                }
                appendLine()
            }

            appendLine("── أبرز الجوائز والإحصائيات ──")
            appendLine(
                "• هداف البطولة: " + (stats.topScorer?.let { "${it.player.name} (${it.goalsScored} هدف)" } ?: "لم يتحدد بعد")
            )
            appendLine(
                "• أقوى دفاع: " + (stats.bestDefense?.let { "${it.player.name} (استقبل ${it.goalsConceded} | شباك نظيفة ${it.cleanSheets})" } ?: "لم يتحدد بعد")
            )
            appendLine(
                "• أعلى نسبة فوز: " + (stats.highestWinRate?.let { "${it.player.name} (${it.winRatePercent}% - ${it.wins} فوز)" } ?: "لم يتحدد بعد")
            )

            if (uiState.groupsWithStandings.isNotEmpty()) {
                appendLine()
                appendLine("── ترتيب المجموعات ──")
                uiState.groupsWithStandings.forEach { groupData ->
                    appendLine("[${groupData.group.name}]")
                    groupData.standings.forEach { row ->
                        val qualBadge = if (row.isQualified) " ✓ متأهل" else ""
                        val diffSign = if (row.goalDifference > 0) "+${row.goalDifference}" else "${row.goalDifference}"
                        appendLine("  ${row.position}. ${row.player.name} — ${row.points} ن (لعب ${row.played} | ف ${row.wins} | ت ${row.draws} | خ ${row.losses} | ف.أ $diffSign)$qualBadge")
                    }
                }
            } else if (uiState.leagueStandings.isNotEmpty()) {
                appendLine()
                appendLine("── ترتيب جدول الدوري ──")
                uiState.leagueStandings.forEach { row ->
                    val qualBadge = if (row.isQualified) " ✓ متأهل" else ""
                    val diffSign = if (row.goalDifference > 0) "+${row.goalDifference}" else "${row.goalDifference}"
                    appendLine("  ${row.position}. ${row.player.name} — ${row.points} ن (لعب ${row.played} | ف ${row.wins} | ت ${row.draws} | خ ${row.losses} | ف.أ $diffSign)$qualBadge")
                }
            }

            if (uiState.knockoutMatches.isNotEmpty()) {
                appendLine()
                appendLine("── نتائج الأدوار الإقصائية ──")
                val groupedKnockout = uiState.knockoutMatches
                    .groupBy { it.stage }
                    .toList()
                    .sortedBy { it.first.order }
                groupedKnockout.forEach { (stage, matches) ->
                    appendLine("▶ ${stage.labelAr}")
                    matches.sortedBy { it.bracketIndex }.forEach { match ->
                        appendLine("  • ${formatMatchLine(match, playersById)}")
                    }
                }
            }

            val groupOrLeagueMatches = uiState.allMatches.filter {
                it.stage == MatchStage.GROUP || it.stage == MatchStage.LEAGUE
            }
            if (groupOrLeagueMatches.isNotEmpty()) {
                appendLine()
                appendLine("── نتائج مباريات المجموعات / الدوري ──")
                groupOrLeagueMatches.forEach { match ->
                    appendLine("  • ${formatMatchLine(match, playersById)}")
                }
            }

            appendLine()
            appendLine("تم الإنشاء بواسطة تطبيق إدارة بطولات PS4 Jailbreak FIFA 22")
        }.trim()
    }

    fun buildSocialPostText(uiState: TournamentUiState): String {
        val playersById = uiState.players.associateBy { it.id }
        val t = uiState.tournament
        val stats = uiState.overviewStats
        val finalMatch = uiState.knockoutMatches.find { it.stage == MatchStage.FINAL }
        val resolvedChampion = uiState.championPlayer
            ?: finalMatch?.winnerId?.let { playersById[it] }

        return buildString {
            appendLine("🏆 ${t?.name ?: "بطولة FIFA 22"}")
            appendLine("🎮 COMMUNITY PS4 JAILBREAK ONLINE")
            appendLine()
            if (resolvedChampion != null) {
                appendLine("👑 البطل المتوج: ${resolvedChampion.name} (${resolvedChampion.elysiumNetId})")
            } else {
                appendLine("📊 الحالة: ${t?.status?.labelAr ?: "جارية"} (${stats.completedMatches}/${stats.totalMatches} مباراة)")
            }

            if (finalMatch != null && (finalMatch.isCompleted || finalMatch.leg1Score1 != null)) {
                appendLine("🔥 النهائي: ${formatMatchLine(finalMatch, playersById)}")
            }

            val semis = uiState.knockoutMatches
                .filter { it.stage == MatchStage.SEMI_FINAL && it.isCompleted }
                .sortedBy { it.bracketIndex }
            if (semis.isNotEmpty()) {
                appendLine("⚡ نصف النهائي:")
                semis.forEach { m ->
                    appendLine("• ${formatMatchLine(m, playersById)}")
                }
            }

            stats.topScorer?.let { top ->
                appendLine("⚽ الهداف: ${top.player.name} (${top.goalsScored} هدف)")
            }
            appendLine("🥅 إجمالي الأهداف: ${stats.totalGoals}")
            appendLine()
            append("#FIFA22 #PS4_Jailbreak_Online #ElysiumNet")
        }.trim()
    }

    private fun formatMatchLine(
        match: MatchEntity,
        playersById: Map<Long, PlayerEntity>
    ): String {
        val p1 = match.player1Id?.let { playersById[it]?.name } ?: "بانتظار المتأهل"
        val p2 = match.player2Id?.let { playersById[it]?.name } ?: "بانتظار المتأهل"
        val scoreStr = when {
            match.isTwoLegged && match.leg1Score1 != null -> {
                val tot1 = match.leg1Score1 + (match.leg2Score1 ?: 0)
                val tot2 = (match.leg1Score2 ?: 0) + (match.leg2Score2 ?: 0)
                "$tot1 - $tot2 (ذ ${match.leg1Score1}-${match.leg1Score2} / إ ${match.leg2Score1 ?: "-"}-${match.leg2Score2 ?: "-"})"
            }
            match.leg1Score1 != null && match.leg1Score2 != null -> {
                "${match.leg1Score1} - ${match.leg1Score2}"
            }
            match.isCompleted -> "حُسمت"
            else -> "لم تُلعب"
        }
        val penStr = if (match.penaltyScore1 != null && match.penaltyScore2 != null) {
            " [ترجيح ${match.penaltyScore1}-${match.penaltyScore2}]"
        } else ""
        val winnerName = match.winnerId?.let { playersById[it]?.name }
        val winnerSuffix = if (winnerName != null) " ★ الفائز: $winnerName" else ""
        return "$p1 ($scoreStr$penStr) $p2$winnerSuffix"
    }
}
