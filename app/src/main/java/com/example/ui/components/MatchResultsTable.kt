package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.MatchStage
import com.example.notifications.MatchReminderScheduler
import com.example.ui.theme.AmberDraw
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ScoreDisplayStyle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class MatchTableFilter(val labelAr: String, val tag: String) {
    ALL("الكل", "match_filter_all"),
    PENDING("بانتظار النتيجة", "match_filter_pending"),
    COMPLETED("مكتملة", "match_filter_completed"),
    KNOCKOUT("الإقصائيات", "match_filter_knockout")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchResultsTableCard(
    matches: List<MatchEntity>,
    playersById: Map<Long, PlayerEntity>,
    canEdit: Boolean,
    onQuickUpdateGoals: (match: MatchEntity, player1Goals: Int, player2Goals: Int) -> Unit,
    onOpenDetailedEditor: (MatchEntity) -> Unit,
    remindersByMatchId: Map<Long, MatchReminderEntity> = emptyMap(),
    onOpenReminder: ((MatchEntity) -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String = "جدول نتائج المباريات الحالية",
    showFilters: Boolean = true,
    maxVisibleRows: Int? = null
) {
    var selectedFilter by remember { mutableStateOf(MatchTableFilter.ALL) }

    val filteredMatches = remember(matches, selectedFilter) {
        when (selectedFilter) {
            MatchTableFilter.ALL -> matches
            MatchTableFilter.PENDING -> matches.filter { !it.isCompleted && it.player1Id != null && it.player2Id != null }
            MatchTableFilter.COMPLETED -> matches.filter { it.isCompleted }
            MatchTableFilter.KNOCKOUT -> matches.filter { it.stage != MatchStage.GROUP && it.stage != MatchStage.LEAGUE }
        }
    }

    val visibleMatches = if (maxVisibleRows != null) {
        filteredMatches.take(maxVisibleRows)
    } else {
        filteredMatches
    }

    val completedCount = matches.count { it.isCompleted }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_results_table_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsScore,
                        contentDescription = null,
                        tint = ChampionshipGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Bold
                        )
                        if (canEdit) {
                            Text(
                                text = "يمكنك تعديل أهداف كل لاعب مباشرة عبر (+) و (-) ثم الحفظ",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberSky
                            )
                        }
                    }
                }

                Surface(
                    color = ElectricCobalt.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElectricCobalt.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "$completedCount / ${matches.size} مكتملة",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberSky,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (showFilters && matches.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MatchTableFilter.entries.forEach { filter ->
                        val countLabel = if (filter == MatchTableFilter.ALL) " (${matches.size})" else ""
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.labelAr}$countLabel") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCobalt,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag(filter.tag)
                        )
                    }
                }
            }

            if (visibleMatches.isEmpty()) {
                Text(
                    text = "لا توجد مباريات مطابقة للعرض حالياً.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                // Table Column Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedNavy)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اللاعب الأول",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "الأهداف / النتيجة",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampionshipGold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(96.dp)
                    )
                    Text(
                        text = "اللاعب الثاني",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visibleMatches.forEachIndexed { index, match ->
                        if (index > 0) {
                            HorizontalDivider(color = CardBorder.copy(alpha = 0.45f))
                        }
                        MatchResultTableRow(
                            match = match,
                            player1 = match.player1Id?.let { playersById[it] },
                            player2 = match.player2Id?.let { playersById[it] },
                            canEdit = canEdit,
                            activeReminder = remindersByMatchId[match.id],
                            onQuickUpdateGoals = { g1, g2 -> onQuickUpdateGoals(match, g1, g2) },
                            onOpenDetailedEditor = { onOpenDetailedEditor(match) },
                            onOpenReminder = if (onOpenReminder != null) {
                                { onOpenReminder(match) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchResultTableRow(
    match: MatchEntity,
    player1: PlayerEntity?,
    player2: PlayerEntity?,
    canEdit: Boolean,
    activeReminder: MatchReminderEntity? = null,
    onQuickUpdateGoals: (player1Goals: Int, player2Goals: Int) -> Unit,
    onOpenDetailedEditor: () -> Unit,
    onOpenReminder: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bothPlayersReady = player1 != null && player2 != null
    val isKnockout = match.stage != MatchStage.GROUP && match.stage != MatchStage.LEAGUE
    val isP1Winner = match.isCompleted && match.winnerId != null && match.winnerId == player1?.id
    val isP2Winner = match.isCompleted && match.winnerId != null && match.winnerId == player2?.id

    // Draft goal state synced when stored match scores change
    var draftP1 by remember(match.id, match.leg1Score1, match.leg1Score2) {
        mutableStateOf(match.leg1Score1)
    }
    var draftP2 by remember(match.id, match.leg1Score1, match.leg1Score2) {
        mutableStateOf(match.leg1Score2)
    }

    val hasDraftChanges = (draftP1 != match.leg1Score1 || draftP2 != match.leg1Score2) &&
        draftP1 != null && draftP2 != null

    val statusText = when {
        match.isCompleted -> "مكتملة"
        match.leg1Score1 != null -> "بانتظار الإياب"
        else -> "لم تُلعب بعد"
    }
    val statusColor = when {
        match.isCompleted -> EmeraldWin
        match.leg1Score1 != null -> AmberDraw
        else -> CyberSky
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianVoid.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = if (hasDraftChanges) ChampionshipGold.copy(alpha = 0.7f) else CardBorder.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(10.dp)
            .testTag("match_row_${match.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row Top Metadata Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (match.scheduledLabel.isNotBlank()) {
                    "${match.stage.labelAr} • ${match.scheduledLabel}"
                } else {
                    match.stage.labelAr
                },
                style = MaterialTheme.typography.labelSmall,
                color = CyberSky,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (activeReminder != null && !match.isCompleted) {
                    Surface(
                        color = ChampionshipGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "تنبيه ${MatchReminderScheduler.formatDateTime(activeReminder.remindAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampionshipGold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (onOpenReminder != null && !match.isCompleted && bothPlayersReady) {
                    IconButton(
                        onClick = onOpenReminder,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("match_${match.id}_reminder_button")
                    ) {
                        Icon(
                            imageVector = if (activeReminder != null) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "جدولة تنبيه لموعد المباراة",
                            tint = if (activeReminder != null) ChampionshipGold else CyberSky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (canEdit && bothPlayersReady) {
                    IconButton(
                        onClick = onOpenDetailedEditor,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("match_${match.id}_detail_edit")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل النتيجة التفصيلية",
                            tint = ChampionshipGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Players + Center Score / Inline Steppers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player 1
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerAvatar(
                    avatarUri = player1?.avatarUri,
                    playerName = player1?.name ?: "؟",
                    size = 34.dp,
                    borderColor = if (isP1Winner) ChampionshipGold else CardBorder
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = player1?.name ?: "بانتظار المتأهل",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isP1Winner) ChampionshipGold else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player1 != null) {
                        Text(
                            text = player1.elysiumNetId,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Center Scoreboard (always aligned with table header, reflects live draft goals)
            Surface(
                color = ElevatedNavy,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    when {
                        hasDraftChanges -> ChampionshipGold
                        match.isCompleted -> ChampionshipGold.copy(alpha = 0.55f)
                        else -> CardBorder
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val s1 = if (match.isTwoLegged) {
                        if (match.leg1Score1 != null) (match.leg1Score1 + (match.leg2Score1 ?: 0)).toString() else "-"
                    } else {
                        draftP1?.toString() ?: "-"
                    }
                    val s2 = if (match.isTwoLegged) {
                        if (match.leg1Score2 != null) (match.leg1Score2 + (match.leg2Score2 ?: 0)).toString() else "-"
                    } else {
                        draftP2?.toString() ?: "-"
                    }
                    Text(
                        text = "$s1 : $s2",
                        style = ScoreDisplayStyle,
                        color = if (match.isCompleted || hasDraftChanges) TextPrimary else TextSecondary
                    )
                    if (match.isTwoLegged && match.leg1Score1 != null) {
                        Text(
                            text = "ذ (${match.leg1Score1}-${match.leg1Score2}) • إ (${match.leg2Score1 ?: "-"}-${match.leg2Score2 ?: "-"})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    if (match.penaltyScore1 != null && match.penaltyScore2 != null) {
                        Text(
                            text = "ترجيح ${match.penaltyScore1}-${match.penaltyScore2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampionshipGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Player 2
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = player2?.name ?: "بانتظار المتأهل",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isP2Winner) ChampionshipGold else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                    if (player2 != null) {
                        Text(
                            text = player2.elysiumNetId,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                PlayerAvatar(
                    avatarUri = player2?.avatarUri,
                    playerName = player2?.name ?: "؟",
                    size = 34.dp,
                    borderColor = if (isP2Winner) ChampionshipGold else CardBorder
                )
            }
        }

        // Inline Per-Player Goal Steppers Bar (for authorized admins on single-leg ready matches)
        if (canEdit && bothPlayersReady && !match.isTwoLegged) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerGoalStepper(
                    goals = draftP1,
                    minusTag = "match_${match.id}_p1_minus",
                    valueTag = "match_${match.id}_p1_goals",
                    plusTag = "match_${match.id}_p1_plus",
                    playerName = player1?.name ?: "اللاعب 1",
                    onIncrement = {
                        val current1 = draftP1 ?: 0
                        draftP1 = (current1 + 1).coerceAtMost(99)
                        if (draftP2 == null) draftP2 = 0
                    },
                    onDecrement = {
                        val current1 = draftP1 ?: 0
                        draftP1 = (current1 - 1).coerceAtLeast(0)
                        if (draftP2 == null) draftP2 = 0
                    },
                    modifier = Modifier.weight(1f)
                )

                PlayerGoalStepper(
                    goals = draftP2,
                    minusTag = "match_${match.id}_p2_minus",
                    valueTag = "match_${match.id}_p2_goals",
                    plusTag = "match_${match.id}_p2_plus",
                    playerName = player2?.name ?: "اللاعب 2",
                    onIncrement = {
                        val current2 = draftP2 ?: 0
                        draftP2 = (current2 + 1).coerceAtMost(99)
                        if (draftP1 == null) draftP1 = 0
                    },
                    onDecrement = {
                        val current2 = draftP2 ?: 0
                        draftP2 = (current2 - 1).coerceAtLeast(0)
                        if (draftP1 == null) draftP1 = 0
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Inline Save / Reset Bar when goals are modified
        AnimatedVisibility(visible = canEdit && hasDraftChanges) {
            val g1 = draftP1 ?: 0
            val g2 = draftP2 ?: 0
            val needsPenalties = isKnockout && !match.isTwoLegged && g1 == g2

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (needsPenalties) {
                            onOpenDetailedEditor()
                        } else {
                            onQuickUpdateGoals(g1, g2)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (needsPenalties) ChampionshipGold else ElectricCobalt
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("match_${match.id}_save_goals")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (needsPenalties) MidnightSurface else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (needsPenalties) "إدخال ركلات الترجيح" else "حفظ النتيجة ($g1 - $g2)",
                        color = if (needsPenalties) MidnightSurface else TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = {
                        draftP1 = match.leg1Score1
                        draftP2 = match.leg1Score2
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("match_${match.id}_reset_goals")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إلغاء التعديل",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerGoalStepper(
    goals: Int?,
    minusTag: String,
    valueTag: String,
    plusTag: String,
    playerName: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ElevatedNavy.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (goals != null) ChampionshipGold.copy(alpha = 0.4f) else CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = (goals ?: 0) > 0,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(minusTag)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MidnightSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "إنقاص أهداف $playerName",
                        tint = if ((goals ?: 0) > 0) TextPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = goals?.toString() ?: "-",
                style = ScoreDisplayStyle.copy(fontSize = 18.sp),
                color = if (goals != null) ChampionshipGold else TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(valueTag)
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(plusTag)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(ElectricCobalt.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "زيادة أهداف $playerName",
                        tint = CyberSky,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
