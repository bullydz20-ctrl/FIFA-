package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.AdminAccountEntity
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.model.PlayerLevel
import com.example.data.model.TournamentStatus
import com.example.domain.PlayerStats
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OvrBadgeStyle
import com.example.ui.theme.RoyalViolet
import com.example.ui.theme.ScoreDisplayStyle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CommunityTopHeader(
    currentAdmin: AdminAccountEntity?,
    onOpenAdmin: () -> Unit,
    onOpenExportShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MidnightSurface.copy(alpha = 0.96f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_community_logo),
                    contentDescription = stringResource(R.string.cd_community_logo),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, ChampionshipGold, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.community_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ChampionshipGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.community_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenExportShare,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("header_share_export_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "تصدير ومشاركة نتائج البطولة",
                    tint = CyberSky
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Surface(
                modifier = Modifier
                    .testTag("header_admin_button")
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenAdmin),
                color = if (currentAdmin != null) {
                    ChampionshipGold.copy(alpha = 0.16f)
                } else {
                    ElectricCobalt.copy(alpha = 0.2f)
                },
                border = BorderStroke(
                    1.dp,
                    if (currentAdmin != null) ChampionshipGold else ElectricCobalt
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentAdmin != null) Icons.Default.VerifiedUser else Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (currentAdmin != null) ChampionshipGold else CyberSky,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentAdmin?.displayName ?: stringResource(R.string.action_admin_login),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (currentAdmin != null) ChampionshipGold else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentHeroBanner(
    tournament: TournamentEntity,
    approvedCount: Int,
    pendingCount: Int,
    isRegistrationOpen: Boolean,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                if (!tournament.bannerUri.isNullOrBlank()) {
                    AsyncImage(
                        model = tournament.bannerUri,
                        contentDescription = stringResource(R.string.cd_tournament_banner),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_tournament_banner),
                        contentDescription = stringResource(R.string.cd_tournament_banner),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianVoid.copy(alpha = 0.2f),
                                    ObsidianVoid.copy(alpha = 0.72f),
                                    MidnightSurface
                                )
                            )
                        )
                )

                // Status badge
                StatusPill(
                    text = tournament.status.labelAr,
                    color = when (tournament.status) {
                        TournamentStatus.REGISTRATION_OPEN -> EmeraldWin
                        TournamentStatus.REGISTRATION_CLOSED -> ChampionshipGold
                        TournamentStatus.GROUP_STAGE, TournamentStatus.KNOCKOUT_STAGE -> CyberSky
                        TournamentStatus.COMPLETED -> ChampionshipGold
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tournament.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Capacity & Schedule row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            tint = CyberSky,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المشاركون المعتمدون: $approvedCount / ${tournament.maxParticipants}",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                    if (pendingCount > 0) {
                        Text(
                            text = "$pendingCount قيد المراجعة",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampionshipGold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                val progress = if (tournament.maxParticipants > 0) {
                    (approvedCount.toFloat() / tournament.maxParticipants.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 1f) ChampionshipGold else ElectricCobalt,
                    trackColor = ElevatedNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(
                        icon = Icons.Default.Schedule,
                        label = tournament.startDateTime.ifBlank { "يُحدد لاحقاً" },
                        modifier = Modifier.weight(1f)
                    )
                    InfoChip(
                        icon = Icons.Default.SportsSoccer,
                        label = tournament.format.labelAr,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isRegistrationOpen) {
                    Button(
                        onClick = onRegisterClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hero_register_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCobalt,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.HowToReg, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سجّل في البطولة الآن",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("hero_registration_closed_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CardBorder)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ChampionshipGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (approvedCount >= tournament.maxParticipants) {
                                "التسجيل مغلق لاكتمال العدد"
                            } else {
                                "التسجيل مغلق حالياً"
                            },
                            color = ChampionshipGold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ElevatedNavy,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = CyberSky, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = ObsidianVoid.copy(alpha = 0.85f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun PlayerAvatar(
    avatarUri: String?,
    playerName: String,
    size: Dp = 44.dp,
    borderColor: Color = ElectricCobalt
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(ElectricCobalt.copy(alpha = 0.35f), RoyalViolet.copy(alpha = 0.45f)))
            )
            .border(1.5.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = stringResource(R.string.cd_player_avatar),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = playerName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "؟"
            if (initial == "؟") {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FifaPlayerCard(
    player: PlayerEntity,
    stats: PlayerStats?,
    isChampion: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accent = when {
        isChampion -> ChampionshipGold
        player.level == PlayerLevel.LEGEND -> ChampionshipGold
        player.level == PlayerLevel.PRO -> RoyalViolet
        player.level == PlayerLevel.INTERMEDIATE -> ElectricCobalt
        else -> CyberSky
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.7f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.18f),
                            MidnightSurface,
                            ElevatedNavy.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // OVR & Level column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = player.ovrRating.toString(),
                            style = OvrBadgeStyle,
                            color = accent
                        )
                        Text(
                            text = "OVR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    PlayerAvatar(
                        avatarUri = player.avatarUri,
                        playerName = player.name,
                        size = 56.dp,
                        borderColor = accent
                    )

                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isChampion) "البطل" else player.level.labelAr,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = CyberSky,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ElysiumNet: ${player.elysiumNetId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberSky,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (player.clubName.isNotBlank()) {
                    Text(
                        text = "الفريق: ${player.clubName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianVoid.copy(alpha = 0.55f))
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat("لعب", stats?.matchesPlayed ?: 0)
                    MiniStat("فوز", stats?.wins ?: 0)
                    MiniStat("أهداف", stats?.goalsScored ?: 0)
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun ChampionShowcaseCard(
    champion: PlayerEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, ChampionshipGold),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ChampionshipGold.copy(alpha = 0.25f),
                            RoyalViolet.copy(alpha = 0.22f),
                            ElectricCobalt.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ChampionshipGold.copy(alpha = 0.2f))
                        .border(1.5.dp, ChampionshipGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.cd_champion_trophy),
                        tint = ChampionshipGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "بطل البطولة المتوّج",
                        style = MaterialTheme.typography.labelLarge,
                        color = ChampionshipGold
                    )
                    Text(
                        text = champion.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "ElysiumNet: ${champion.elysiumNetId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberSky
                    )
                }
                PlayerAvatar(
                    avatarUri = champion.avatarUri,
                    playerName = champion.name,
                    size = 58.dp,
                    borderColor = ChampionshipGold
                )
            }
        }
    }
}

@Composable
fun MatchScoreCard(
    match: MatchEntity,
    player1: PlayerEntity?,
    player2: PlayerEntity?,
    canEdit: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isP1Winner = match.isCompleted && match.winnerId != null && match.winnerId == player1?.id
    val isP2Winner = match.isCompleted && match.winnerId != null && match.winnerId == player2?.id

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canEdit && player1 != null && player2 != null) {
                    Modifier.clickable(onClick = onEditClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(
            1.dp,
            if (match.isCompleted) ElectricCobalt.copy(alpha = 0.5f) else CardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.stage.labelAr} • ${match.scheduledLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberSky
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            match.isCompleted -> "مكتملة"
                            match.leg1Score1 != null -> "بانتظار الإياب"
                            else -> "لم تُلعب بعد"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (match.isCompleted) EmeraldWin else TextSecondary
                    )
                    if (canEdit && player1 != null && player2 != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل النتيجة",
                            tint = ChampionshipGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        size = 38.dp,
                        borderColor = if (isP1Winner) ChampionshipGold else CardBorder
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

                // Center Score Box
                Surface(
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (match.isCompleted) ChampionshipGold.copy(alpha = 0.5f) else CardBorder),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val s1 = if (match.isTwoLegged) {
                            if (match.leg1Score1 != null) (match.leg1Score1 + (match.leg2Score1 ?: 0)).toString() else "-"
                        } else {
                            match.leg1Score1?.toString() ?: "-"
                        }
                        val s2 = if (match.isTwoLegged) {
                            if (match.leg1Score2 != null) (match.leg1Score2 + (match.leg2Score2 ?: 0)).toString() else "-"
                        } else {
                            match.leg1Score2?.toString() ?: "-"
                        }
                        Text(
                            text = "$s1 : $s2",
                            style = ScoreDisplayStyle,
                            color = if (match.isCompleted) TextPrimary else TextSecondary
                        )
                        if (match.isTwoLegged && match.leg1Score1 != null) {
                            Text(
                                text = "ذهاب (${match.leg1Score1}-${match.leg1Score2}) • إياب (${match.leg2Score1 ?: "-"}-${match.leg2Score2 ?: "-"})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        if (match.penaltyScore1 != null && match.penaltyScore2 != null) {
                            Text(
                                text = "ترجيح: ${match.penaltyScore1} - ${match.penaltyScore2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampionshipGold
                            )
                        }
                    }
                }

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
                    Spacer(modifier = Modifier.width(8.dp))
                    PlayerAvatar(
                        avatarUri = player2?.avatarUri,
                        playerName = player2?.name ?: "؟",
                        size = 38.dp,
                        borderColor = if (isP2Winner) ChampionshipGold else CardBorder
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricCobalt.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = CyberSky, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
