package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.MatchStage
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

val KNOCKOUT_ROADMAP_STAGES = listOf(
    MatchStage.ROUND_OF_16,
    MatchStage.QUARTER_FINAL,
    MatchStage.SEMI_FINAL,
    MatchStage.FINAL
)

@Composable
fun BracketStagePillsBar(
    selectedStage: MatchStage?,
    matchesByStage: Map<MatchStage, List<MatchEntity>>,
    onSelectStage: (MatchStage?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedStage == null,
                onClick = { onSelectStage(null) },
                label = { Text("الشجرة الكاملة") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChampionshipGold,
                    selectedLabelColor = MidnightSurface
                ),
                modifier = Modifier.testTag("bracket_stage_filter_all")
            )
        }

        items(KNOCKOUT_ROADMAP_STAGES, key = { it.name }) { stage ->
            val stageMatches = matchesByStage[stage].orEmpty()
            val completed = stageMatches.count { it.isCompleted }
            val total = stageMatches.size
            val isPresent = total > 0
            val badgeText = if (isPresent) "${stage.labelAr} ($completed/$total)" else "${stage.labelAr} (—)"

            FilterChip(
                selected = selectedStage == stage,
                enabled = isPresent,
                onClick = { onSelectStage(stage) },
                label = { Text(badgeText) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCobalt,
                    selectedLabelColor = TextPrimary
                ),
                modifier = Modifier.testTag("bracket_stage_filter_${stage.name}")
            )
        }
    }
}

@Composable
fun BracketMatchNode(
    match: MatchEntity,
    player1: PlayerEntity?,
    player2: PlayerEntity?,
    isFinal: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canTap = canEdit && player1 != null && player2 != null
    val p1Won = match.isCompleted && match.winnerId != null && match.winnerId == player1?.id
    val p2Won = match.isCompleted && match.winnerId != null && match.winnerId == player2?.id

    val score1Text = formatBracketScore(match, isPlayer1 = true)
    val score2Text = formatBracketScore(match, isPlayer1 = false)

    Card(
        modifier = modifier
            .testTag("bracket_match_${match.id}")
            .then(
                if (canTap) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinal) ElevatedNavy else MidnightSurface
        ),
        border = BorderStroke(
            width = when {
                isFinal -> 1.6.dp
                match.isCompleted -> 1.2.dp
                canTap -> 1.2.dp
                else -> 1.dp
            },
            color = when {
                isFinal && match.winnerId != null -> ChampionshipGold
                isFinal -> ChampionshipGold.copy(alpha = 0.85f)
                match.isCompleted -> EmeraldWin.copy(alpha = 0.85f)
                canTap -> ElectricCobalt
                else -> CardBorder
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianVoid.copy(alpha = 0.55f))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (isFinal) "النهائي الكبير" else "${match.stage.labelAr} #${match.bracketIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFinal) ChampionshipGold else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (match.penaltyScore1 != null && match.penaltyScore2 != null) {
                        Text(
                            text = "ر.ت ${match.penaltyScore1}-${match.penaltyScore2}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampionshipGold
                        )
                    } else if (isFinal && match.winnerId != null) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "حُسم النهائي",
                            tint = ChampionshipGold,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (match.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "مكتملة",
                            tint = EmeraldWin,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (canTap) {
                        Surface(
                            color = ElectricCobalt.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تحديث الفائز أو النتيجة",
                                    tint = CyberSky,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (match.winnerId != null) "تعديل" else "تحديد الفائز",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberSky
                                )
                            }
                        }
                    }
                }
            }

            BracketSlotRow(
                player = player1,
                scoreText = score1Text,
                isWinner = p1Won,
                isEliminated = p2Won,
                modifier = Modifier.weight(1f)
            )
            HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))
            BracketSlotRow(
                player = player2,
                scoreText = score2Text,
                isWinner = p2Won,
                isEliminated = p1Won,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BracketSlotRow(
    player: PlayerEntity?,
    scoreText: String,
    isWinner: Boolean,
    isEliminated: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                when {
                    isWinner -> ChampionshipGold.copy(alpha = 0.14f)
                    isEliminated -> ObsidianVoid.copy(alpha = 0.28f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PlayerAvatar(
                avatarUri = player?.avatarUri,
                playerName = player?.name ?: "؟",
                size = 24.dp,
                borderColor = when {
                    isWinner -> ChampionshipGold
                    player != null -> CardBorder
                    else -> CardBorder.copy(alpha = 0.4f)
                }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = player?.name ?: "بانتظار المتأهل",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isWinner -> ChampionshipGold
                    isEliminated -> TextMuted
                    player != null -> TextPrimary
                    else -> TextMuted
                },
                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isWinner) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "متأهل",
                    tint = ChampionshipGold,
                    modifier = Modifier.size(14.dp)
                )
            }
            Surface(
                color = if (isWinner) ChampionshipGold.copy(alpha = 0.22f) else ElevatedNavy,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = scoreText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isWinner) ChampionshipGold else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun BracketConnectorCanvas(
    matchesInRound: List<MatchEntity>,
    nextMatchCount: Int,
    slotHeight: Dp,
    nextSlotHeight: Dp,
    connectorWidth: Dp,
    totalBracketHeight: Dp,
    headerTopOffset: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .padding(top = headerTopOffset)
            .width(connectorWidth)
            .height(totalBracketHeight)
    ) {
        val w = size.width
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val childX = if (isRtl) w else 0f
        val parentX = if (isRtl) 0f else w
        val midX = w / 2f

        val slotPx = with(density) { slotHeight.toPx() }
        val nextSlotPx = with(density) { nextSlotHeight.toPx() }
        val strokeWidthPx = 2.5.dp.toPx()

        for (pairIdx in 0 until nextMatchCount) {
            val topChildY = slotPx * (pairIdx * 2) + slotPx / 2f
            val bottomChildY = slotPx * (pairIdx * 2 + 1) + slotPx / 2f
            val parentY = nextSlotPx * pairIdx + nextSlotPx / 2f

            val topMatch = matchesInRound.getOrNull(pairIdx * 2)
            val botMatch = matchesInRound.getOrNull(pairIdx * 2 + 1)
            val topHasWinner = topMatch?.winnerId != null
            val botHasWinner = botMatch?.winnerId != null

            // Top branch
            val topPath = Path().apply {
                moveTo(childX, topChildY)
                lineTo(midX, topChildY)
                lineTo(midX, parentY)
                lineTo(parentX, parentY)
            }
            // Bottom branch
            val botPath = Path().apply {
                moveTo(childX, bottomChildY)
                lineTo(midX, bottomChildY)
                lineTo(midX, parentY)
                lineTo(parentX, parentY)
            }

            // Draw inactive paths first so active gold paths render crisply on top
            if (!topHasWinner) {
                drawPath(
                    path = topPath,
                    color = CardBorder,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (!botHasWinner) {
                drawPath(
                    path = botPath,
                    color = CardBorder,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (topHasWinner) {
                drawPath(
                    path = topPath,
                    color = ChampionshipGold,
                    style = Stroke(width = strokeWidthPx * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (botHasWinner) {
                drawPath(
                    path = botPath,
                    color = ChampionshipGold,
                    style = Stroke(width = strokeWidthPx * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun ChampionPedestalNode(
    champion: PlayerEntity?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("bracket_champion_pedestal"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (champion != null) ElevatedNavy else MidnightSurface
        ),
        border = BorderStroke(
            width = if (champion != null) 2.dp else 1.dp,
            color = if (champion != null) ChampionshipGold else CardBorder
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (champion != null) {
                            listOf(
                                ChampionshipGold.copy(alpha = 0.22f),
                                RoyalViolet.copy(alpha = 0.14f),
                                MidnightSurface
                            )
                        } else {
                            listOf(MidnightSurface, ObsidianVoid.copy(alpha = 0.6f))
                        }
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (champion != null) ChampionshipGold.copy(alpha = 0.22f)
                            else ElevatedNavy
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "كأس البطولة",
                        tint = if (champion != null) ChampionshipGold else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "بطل البطولة",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampionshipGold,
                    fontWeight = FontWeight.Bold
                )

                if (champion != null) {
                    PlayerAvatar(
                        avatarUri = champion.avatarUri,
                        playerName = champion.name,
                        size = 40.dp,
                        borderColor = ChampionshipGold
                    )
                    Text(
                        text = champion.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (champion.clubName.isNotBlank()) {
                        Text(
                            text = champion.clubName,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberSky,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "يتحدد من النهائي",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BracketMatchWinnerDialog(
    match: MatchEntity,
    player1: PlayerEntity,
    player2: PlayerEntity,
    onDismiss: () -> Unit,
    onSelectWinner: (winnerPlayerId: Long?) -> Unit,
    onOpenScoreEditor: () -> Unit
) {
    val isFinal = match.stage == MatchStage.FINAL
    val score1 = formatBracketScore(match, isPlayer1 = true)
    val score2 = formatBracketScore(match, isPlayer1 = false)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isFinal) Icons.Default.EmojiEvents else Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = ChampionshipGold
                )
                Column {
                    Text(
                        text = "تحديث الفائز — ${match.stage.labelAr}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampionshipGold
                    )
                    Text(
                        text = if (isFinal) "اختر بطل البطولة أو سجّل النتيجة الكاملة"
                        else "اختر اللاعب المتأهل للدور التالي أو عدّل الأهداف",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WinnerCandidateCard(
                    player = player1,
                    scoreText = score1,
                    isCurrentWinner = match.winnerId == player1.id,
                    actionLabel = if (isFinal) "تتويج بالبطولة" else "تأهيل للدور التالي",
                    onClick = { onSelectWinner(player1.id) },
                    testTag = "bracket_pick_winner_p1_${match.id}"
                )

                WinnerCandidateCard(
                    player = player2,
                    scoreText = score2,
                    isCurrentWinner = match.winnerId == player2.id,
                    actionLabel = if (isFinal) "تتويج بالبطولة" else "تأهيل للدور التالي",
                    onClick = { onSelectWinner(player2.id) },
                    testTag = "bracket_pick_winner_p2_${match.id}"
                )

                HorizontalDivider(color = CardBorder.copy(alpha = 0.6f))

                Button(
                    onClick = onOpenScoreEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bracket_open_score_dialog_${match.id}")
                ) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إدخال النتيجة والأهداف التفصيلية")
                }

                if (match.winnerId != null) {
                    OutlinedButton(
                        onClick = { onSelectWinner(null) },
                        border = BorderStroke(1.dp, CrimsonLoss.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bracket_clear_winner_${match.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = CrimsonLoss,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إلغاء تحديد الفائز وإعادة الفتح", color = CrimsonLoss)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun WinnerCandidateCard(
    player: PlayerEntity,
    scoreText: String,
    isCurrentWinner: Boolean,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentWinner) ChampionshipGold.copy(alpha = 0.14f) else ElevatedNavy
        ),
        border = BorderStroke(
            width = if (isCurrentWinner) 1.5.dp else 1.dp,
            color = if (isCurrentWinner) ChampionshipGold else CardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                PlayerAvatar(
                    avatarUri = player.avatarUri,
                    playerName = player.name,
                    size = 38.dp,
                    borderColor = if (isCurrentWinner) ChampionshipGold else CardBorder
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isCurrentWinner) ChampionshipGold else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(player.elysiumNetId)
                            if (player.clubName.isNotBlank()) append(" • ${player.clubName}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (scoreText != "-") {
                    Text(
                        text = "الأهداف: $scoreText",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberSky
                    )
                }
                Surface(
                    color = if (isCurrentWinner) ChampionshipGold else ElectricCobalt.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isCurrentWinner) "الفائز الحالي ✓" else actionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentWinner) MidnightSurface else CyberSky,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatBracketScore(match: MatchEntity, isPlayer1: Boolean): String {
    val s1 = if (isPlayer1) match.leg1Score1 else match.leg1Score2
    val s2 = if (isPlayer1) match.leg2Score1 else match.leg2Score2
    return if (match.isTwoLegged) {
        if (s1 == null) "-" else (s1 + (s2 ?: 0)).toString()
    } else {
        s1?.toString() ?: "-"
    }
}

fun drawChampionConnectorLine(
    drawScope: androidx.compose.ui.graphics.drawscope.DrawScope,
    hasChampion: Boolean
) {
    val w = drawScope.size.width
    val centerY = drawScope.size.height / 2f
    drawScope.drawLine(
        color = if (hasChampion) ChampionshipGold else CardBorder,
        start = Offset(0f, centerY),
        end = Offset(w, centerY),
        strokeWidth = with(drawScope) { 2.8.dp.toPx() },
        cap = StrokeCap.Round
    )
}
