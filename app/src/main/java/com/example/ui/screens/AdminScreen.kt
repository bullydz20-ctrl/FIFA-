package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminAccountEntity
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.model.AdminRole
import com.example.data.model.PlayerStatus
import com.example.security.AdminPermission
import com.example.security.hasPermission
import com.example.ui.components.EmptyStatePanel
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentUiState

private enum class AdminSection(val titleAr: String, val icon: ImageVector) {
    TOURNAMENT("البطولة", Icons.Default.Settings),
    PLAYERS("المشاركون", Icons.Default.Groups),
    DRAW("القرعة", Icons.Default.Casino),
    MATCHES("المباريات", Icons.Default.SportsScore),
    SUB_ADMINS("المشرفون", Icons.Default.ManageAccounts),
    CHAMPION("التتويج", Icons.Default.EmojiEvents)
}

@Composable
fun AdminScreen(
    uiState: TournamentUiState,
    onSetupHeadAdmin: (String, String, String, Boolean) -> Unit,
    onLogin: (String, String, Boolean) -> Unit,
    onLogout: () -> Unit,
    onImportBanner: (Uri, (String) -> Unit) -> Unit,
    onSaveTournament: (TournamentEntity) -> Unit,
    onCreateFreshTournament: (TournamentEntity) -> Unit,
    onDeleteTournament: () -> Unit,
    onUpdatePlayerStatus: (PlayerEntity, PlayerStatus) -> Unit,
    onUpdatePlayerDetails: (PlayerEntity) -> Unit,
    onDeletePlayer: (PlayerEntity) -> Unit,
    onRunDraw: (Map<Long, Int>, List<Long>) -> Unit,
    onGenerateKnockoutFromStandings: () -> Unit,
    onEditMatch: (MatchEntity) -> Unit,
    onQuickUpdateGoals: (MatchEntity, Int, Int) -> Unit = { _, _, _ -> },
    onChangeOwnPin: (String, String) -> Unit,
    onCreateSubAdmin: (String, String, String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUpdateSubAdmin: (AdminAccountEntity, String, String?, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDeleteSubAdmin: (AdminAccountEntity) -> Unit,
    onCrownChampion: (Long?) -> Unit,
    onOpenExportShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentAdmin = uiState.currentAdmin
    if (currentAdmin == null) {
        AdminAuthGate(
            hasHeadAdmin = uiState.hasHeadAdmin,
            admins = uiState.admins,
            lockoutState = uiState.lockoutState,
            onSetupHeadAdmin = onSetupHeadAdmin,
            onLogin = onLogin,
            modifier = modifier
        )
        return
    }

    val allowedSections = remember(currentAdmin) {
        buildList {
            if (currentAdmin.hasPermission(AdminPermission.MANAGE_TOURNAMENT)) add(AdminSection.TOURNAMENT)
            if (currentAdmin.hasPermission(AdminPermission.MANAGE_PLAYERS)) add(AdminSection.PLAYERS)
            if (currentAdmin.hasPermission(AdminPermission.RUN_DRAW)) add(AdminSection.DRAW)
            if (currentAdmin.hasPermission(AdminPermission.EDIT_SCORES)) add(AdminSection.MATCHES)
            if (currentAdmin.role == AdminRole.HEAD_ADMIN) add(AdminSection.SUB_ADMINS)
            if (currentAdmin.hasPermission(AdminPermission.CROWN_CHAMPION)) add(AdminSection.CHAMPION)
        }
    }

    var selectedSection by remember(allowedSections) {
        mutableStateOf(allowedSections.firstOrNull())
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Logged-in admin bar
        Surface(
            color = MidnightSurface,
            border = BorderStroke(1.dp, CardBorder),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "لوحة تحكم: ${currentAdmin.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ChampionshipGold
                    )
                    val sessionBadge = if (uiState.sessionState.keepSignedIn) "جلسة محفوظة (DataStore)" else "جلسة مؤقتة"
                    Text(
                        text = "${currentAdmin.role.labelAr} • $sessionBadge",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberSky
                    )
                }
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("admin_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "تسجيل الخروج",
                        tint = CrimsonLoss
                    )
                }
            }
        }

        if (allowedSections.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyStatePanel(
                    icon = Icons.Default.ManageAccounts,
                    title = "لا توجد صلاحيات مفعلة",
                    subtitle = "اطلب من المشرف الرئيسي تفعيل الصلاحيات المناسبة لحسابك."
                )
            }
            return
        }

        // Section Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allowedSections) { sec ->
                FilterChip(
                    selected = selectedSection == sec,
                    onClick = { selectedSection = sec },
                    leadingIcon = {
                        Icon(sec.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = {
                        val badge = when (sec) {
                            AdminSection.PLAYERS -> if (uiState.pendingPlayers.isNotEmpty()) " (${uiState.pendingPlayers.size})" else ""
                            AdminSection.SUB_ADMINS -> {
                                val count = uiState.admins.count { it.role == AdminRole.SUB_ADMIN }
                                " ($count/5)"
                            }
                            else -> ""
                        }
                        Text("${sec.titleAr}$badge")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCobalt,
                        selectedLabelColor = TextPrimary,
                        selectedLeadingIconColor = ChampionshipGold
                    ),
                    modifier = Modifier.testTag("admin_tab_${sec.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                when (selectedSection) {
                    AdminSection.TOURNAMENT -> uiState.tournament?.let { t ->
                        AdminTournamentSettingsTab(
                            tournament = t,
                            onImportBanner = onImportBanner,
                            onSaveSettings = onSaveTournament,
                            onCreateFresh = onCreateFreshTournament,
                            onDeleteTournament = onDeleteTournament
                        )
                    }
                    AdminSection.PLAYERS -> AdminPlayersTab(
                        uiState = uiState,
                        onStatusChange = onUpdatePlayerStatus,
                        onUpdateDetails = onUpdatePlayerDetails,
                        onDeletePlayer = onDeletePlayer
                    )
                    AdminSection.DRAW -> AdminDrawTab(
                        uiState = uiState,
                        onRunDraw = onRunDraw,
                        onGenerateKnockoutFromStandings = onGenerateKnockoutFromStandings
                    )
                    AdminSection.MATCHES -> AdminMatchesTab(
                        uiState = uiState,
                        onEditMatch = onEditMatch,
                        onQuickUpdateGoals = onQuickUpdateGoals
                    )
                    AdminSection.SUB_ADMINS -> AdminSubAdminsTab(
                        uiState = uiState,
                        onChangeOwnPin = onChangeOwnPin,
                        onCreateSubAdmin = onCreateSubAdmin,
                        onUpdateSubAdmin = onUpdateSubAdmin,
                        onDeleteSubAdmin = onDeleteSubAdmin
                    )
                    AdminSection.CHAMPION -> AdminProgressAndChampionTab(
                        uiState = uiState,
                        onCrownChampion = onCrownChampion,
                        onOpenExportShare = onOpenExportShare
                    )
                    null -> Spacer(modifier = Modifier)
                }
            }
        }
    }
}
