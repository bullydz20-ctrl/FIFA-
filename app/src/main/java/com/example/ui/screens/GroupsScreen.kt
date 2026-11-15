package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchEntity
import com.example.data.model.TournamentFormat
import com.example.domain.GroupStandingRow
import com.example.security.AdminPermission
import com.example.security.hasPermission
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MatchScoreCard
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentUiState

@Composable
fun GroupsScreen(
    uiState: TournamentUiState,
    onEditMatch: (MatchEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val tournament = uiState.tournament
    val playersById = uiState.players.associateBy { it.id }
    val canEditScores = uiState.currentAdmin?.hasPermission(AdminPermission.EDIT_SCORES) == true
    var selectedGroupIdx by remember { mutableIntStateOf(0) }

    if (tournament?.format == TournamentFormat.DIRECT_KNOCKOUT) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            EmptyStatePanel(
                icon = Icons.Default.TableChart,
                title = "البطولة بنظام الخروج المباشر",
                subtitle = "هذا النظام لا يتضمن دور مجموعات؛ يمكنك متابعة جميع المواجهات مباشرة من تبويب الإقصائيات."
            )
        }
        return
    }

    if (tournament?.format == TournamentFormat.LEAGUE_KNOCKOUT) {
        if (uiState.leagueStandings.isEmpty() || uiState.leagueMatches.isEmpty()) {
            Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
                EmptyStatePanel(
                    icon = Icons.Default.TableChart,
                    title = "لم يتم إنشاء جدول الدوري بعد",
                    subtitle = "بانتظار قيام المشرف بإجراء القرعة وتوليد مباريات الدوري."
                )
            }
            return
        }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StandingsTableCard(
                    title = "جدول ترتيب الدوري العام",
                    rows = uiState.leagueStandings
                )
            }
            item {
                Text(
                    text = "مباريات الدوري",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            items(uiState.leagueMatches, key = { "lg_${it.id}" }) { match ->
                MatchScoreCard(
                    match = match,
                    player1 = match.player1Id?.let { playersById[it] },
                    player2 = match.player2Id?.let { playersById[it] },
                    canEdit = canEditScores,
                    onEditClick = { onEditMatch(match) }
                )
            }
        }
        return
    }

    // GROUPS_KNOCKOUT mode
    val groups = uiState.groupsWithStandings
    if (groups.isEmpty()) {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
            EmptyStatePanel(
                icon = Icons.Default.TableChart,
                title = "لم تُجرَ قرعة المجموعات بعد",
                subtitle = "بعد اعتماد اللاعبين وتشغيل القرعة من لوحة المشرف، ستظهر المجموعات والترتيب التلقائي وجدول المباريات هنا."
            )
        }
        return
    }

    val safeIndex = selectedGroupIdx.coerceIn(0, groups.lastIndex)
    val activeGroup = groups[safeIndex]

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups.size) { idx ->
                    val grp = groups[idx]
                    FilterChip(
                        selected = idx == safeIndex,
                        onClick = { selectedGroupIdx = idx },
                        label = { Text(grp.group.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCobalt,
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }
        }

        item {
            StandingsTableCard(
                title = "ترتيب ${activeGroup.group.name}",
                rows = activeGroup.standings
            )
        }

        item {
            Text(
                text = "مباريات ${activeGroup.group.name}",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        items(activeGroup.matches, key = { "gm_${it.id}" }) { match ->
            MatchScoreCard(
                match = match,
                player1 = match.player1Id?.let { playersById[it] },
                player2 = match.player2Id?.let { playersById[it] },
                canEdit = canEditScores,
                onEditClick = { onEditMatch(match) }
            )
        }
    }
}

@Composable
fun StandingsTableCard(
    title: String,
    rows: List<GroupStandingRow>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChampionshipGold,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = ChampionshipGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "الذهبي = متأهل",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampionshipGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedNavy)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(22.dp), style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
                Text("اللاعب", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                StatColHeader("ل")
                StatColHeader("ف")
                StatColHeader("ت")
                StatColHeader("خ")
                StatColHeader("له")
                StatColHeader("ع")
                StatColHeader("+/-")
                Text("نقاط", modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelSmall, color = ChampionshipGold, textAlign = TextAlign.Center)
            }

            rows.forEachIndexed { idx, row ->
                if (idx > 0) {
                    HorizontalDivider(color = CardBorder.copy(alpha = 0.4f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (row.isQualified) ChampionshipGold.copy(alpha = 0.06f) else ObsidianVoid.copy(alpha = 0f)
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.position.toString(),
                        modifier = Modifier.width(22.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (row.isQualified) ChampionshipGold else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerAvatar(
                            avatarUri = row.player.avatarUri,
                            playerName = row.player.name,
                            size = 24.dp,
                            borderColor = if (row.isQualified) ChampionshipGold else CardBorder
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = row.player.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.isQualified) TextPrimary else TextSecondary,
                            fontWeight = if (row.isQualified) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatColVal(row.played.toString())
                    StatColVal(row.wins.toString())
                    StatColVal(row.draws.toString())
                    StatColVal(row.losses.toString())
                    StatColVal(row.goalsFor.toString())
                    StatColVal(row.goalsAgainst.toString())
                    val gdStr = if (row.goalDifference > 0) "+${row.goalDifference}" else row.goalDifference.toString()
                    StatColVal(gdStr, color = if (row.goalDifference >= 0) CyberSky else TextSecondary)
                    Text(
                        text = row.points.toString(),
                        modifier = Modifier.width(34.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = ChampionshipGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColHeader(label: String) {
    Text(
        text = label,
        modifier = Modifier.width(22.dp),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StatColVal(value: String, color: androidx.compose.ui.graphics.Color = TextPrimary) {
    Text(
        text = value,
        modifier = Modifier.width(22.dp),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        textAlign = TextAlign.Center
    )
}
