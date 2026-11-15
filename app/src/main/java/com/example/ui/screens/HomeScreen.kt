package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchEntity
import com.example.domain.PlayerStats
import com.example.security.AdminPermission
import com.example.security.hasPermission
import com.example.ui.components.ChampionShowcaseCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MatchResultsTableCard
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.ScheduledRemindersCard
import com.example.ui.components.TournamentExportShareCard
import com.example.ui.components.TournamentHeroBanner
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.ScoreDisplayStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentUiState

@Composable
fun HomeScreen(
    uiState: TournamentUiState,
    onOpenRegister: () -> Unit,
    onEditMatch: (MatchEntity) -> Unit,
    onQuickUpdateGoals: (MatchEntity, Int, Int) -> Unit = { _, _, _ -> },
    onOpenExportShare: () -> Unit = {},
    onOpenReminder: (MatchEntity) -> Unit = {},
    onCancelReminder: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tournament = uiState.tournament
    val playersById = uiState.players.associateBy { it.id }
    val remindersByMatchId = uiState.matchReminders.associateBy { it.matchId }
    val canEditScores = uiState.currentAdmin?.hasPermission(AdminPermission.EDIT_SCORES) == true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (tournament != null) {
            item {
                TournamentHeroBanner(
                    tournament = tournament,
                    approvedCount = uiState.approvedPlayers.size,
                    pendingCount = uiState.pendingPlayers.size,
                    isRegistrationOpen = uiState.isRegistrationOpenNow,
                    onRegisterClick = onOpenRegister
                )
            }
        }

        if (uiState.championPlayer != null) {
            item {
                ChampionShowcaseCard(champion = uiState.championPlayer)
            }
        }

        // Tournament rules card
        if (tournament != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "قوانين ونظام البطولة",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionshipGold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RulePill(
                                icon = Icons.Default.Timer,
                                title = "مدة المباراة",
                                value = "${tournament.matchDurationMinutes} دقيقة",
                                modifier = Modifier.weight(1f)
                            )
                            RulePill(
                                icon = Icons.Default.SportsScore,
                                title = "الأهداف واللعب",
                                value = tournament.targetGoalsRule,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RulePill(
                                icon = Icons.Default.EmojiEvents,
                                title = "حسم الإقصائيات",
                                value = tournament.knockoutTieRule.shortAr,
                                modifier = Modifier.weight(1f)
                            )
                            RulePill(
                                icon = Icons.Default.MilitaryTech,
                                title = "التأهل",
                                value = "${tournament.qualifiersPerGroup} من كل مجموعة",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Tournament Statistics & Top Players
        item {
            SectionTitle(title = "إحصائيات البطولة وأفضل اللاعبين")
        }

        item {
            val stats = uiState.overviewStats
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatHighlightCard(
                        icon = Icons.Default.SportsScore,
                        label = "إجمالي الأهداف",
                        value = stats.totalGoals.toString(),
                        subtext = "في ${stats.completedMatches} / ${stats.totalMatches} مباراة",
                        accent = CyberSky,
                        modifier = Modifier.weight(1f)
                    )
                    StatHighlightCard(
                        icon = Icons.Default.Star,
                        label = "هداف البطولة",
                        value = stats.topScorer?.player?.name ?: "—",
                        subtext = stats.topScorer?.let { "${it.goalsScored} هدف" } ?: "بانتظار المباريات",
                        accent = ChampionshipGold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatHighlightCard(
                        icon = Icons.Default.Shield,
                        label = "أقوى دفاع",
                        value = stats.bestDefense?.player?.name ?: "—",
                        subtext = stats.bestDefense?.let { "استقبل ${it.goalsConceded} • شباك نظيفة ${it.cleanSheets}" }
                            ?: "بانتظار المباريات",
                        accent = EmeraldWin,
                        modifier = Modifier.weight(1f)
                    )
                    StatHighlightCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "أعلى نسبة فوز",
                        value = stats.highestWinRate?.player?.name ?: "—",
                        subtext = stats.highestWinRate?.let { "${it.winRatePercent}% (${it.wins} فوز)" }
                            ?: "بانتظار المباريات",
                        accent = RoyalViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val topPlayers = uiState.playerStats.filter { it.matchesPlayed > 0 }.take(5)
        if (topPlayers.isNotEmpty()) {
            items(topPlayers, key = { "top_${it.player.id}" }) { stat ->
                TopPlayerRow(stat = stat)
            }
        }

        // Live & Recent Matches
        item {
            SectionTitle(title = "جدول المباريات والنتائج المباشرة")
        }

        if (uiState.allMatches.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Default.SportsScore,
                    title = "لم تبدأ القرعة بعد",
                    subtitle = "عند اعتماد اللاعبين وإجراء القرعة من لوحة المشرف، ستظهر جميع المباريات والنتائج المباشرة هنا."
                )
            }
        } else {
            val orderedMatches = uiState.allMatches.sortedWith(
                compareBy<MatchEntity> { it.isCompleted }
                    .thenByDescending { it.stage.order }
                    .thenBy { it.roundNumber }
            )
            item {
                MatchResultsTableCard(
                    matches = orderedMatches,
                    playersById = playersById,
                    canEdit = canEditScores,
                    onQuickUpdateGoals = onQuickUpdateGoals,
                    onOpenDetailedEditor = onEditMatch,
                    remindersByMatchId = remindersByMatchId,
                    onOpenReminder = onOpenReminder,
                    showFilters = true,
                    maxVisibleRows = 12
                )
            }
        }

        if (uiState.allMatches.isNotEmpty() || uiState.matchReminders.isNotEmpty()) {
            item {
                ScheduledRemindersCard(
                    reminders = uiState.matchReminders,
                    onCancelReminder = onCancelReminder
                )
            }
        }

        item {
            TournamentExportShareCard(
                onOpenExportShare = onOpenExportShare,
                tag = "home_export_share_card"
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = TextPrimary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun RulePill(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ElevatedNavy,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = CyberSky, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatHighlightCard(
    icon: ImageVector,
    label: String,
    value: String,
    subtext: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TopPlayerRow(stat: PlayerStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                PlayerAvatar(
                    avatarUri = stat.player.avatarUri,
                    playerName = stat.player.name,
                    size = 40.dp,
                    borderColor = ElectricCobalt
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stat.player.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stat.wins} فوز • ${stat.draws} تعادل • ${stat.losses} خسارة",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${stat.goalsScored}",
                    style = ScoreDisplayStyle,
                    color = ChampionshipGold
                )
                Text(
                    text = "هدف",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
