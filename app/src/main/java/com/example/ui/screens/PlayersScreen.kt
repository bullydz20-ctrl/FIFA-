package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PlayerLevel
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.FifaPlayerCard
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentUiState

@Composable
fun PlayersScreen(
    uiState: TournamentUiState,
    onOpenRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val statsById = uiState.playerStats.associateBy { it.player.id }

    val filteredPlayers = remember(uiState.approvedPlayers, searchQuery) {
        if (searchQuery.isBlank()) uiState.approvedPlayers
        else {
            val q = searchQuery.trim().lowercase()
            uiState.approvedPlayers.filter {
                it.name.lowercase().contains(q) ||
                    it.elysiumNetId.lowercase().contains(q) ||
                    it.clubName.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "قائمة المشاركين",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "${uiState.approvedPlayers.size} / ${uiState.tournament?.maxParticipants ?: 16} لاعب معتمد",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberSky
                )
            }

            if (uiState.isRegistrationOpenNow) {
                Button(
                    onClick = onOpenRegister,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("players_register_button")
                ) {
                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسجيل")
                }
            } else {
                Surface(
                    color = ElevatedNavy,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = ChampionshipGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("التسجيل مغلق", style = MaterialTheme.typography.labelMedium, color = ChampionshipGold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث بالاسم أو معرف ElysiumNet...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberSky) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("players_search_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredPlayers.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Default.Groups,
                title = if (uiState.approvedPlayers.isEmpty()) "لا يوجد مشاركون معتمدون حتى الآن" else "لا توجد نتائج مطابقة للبحث",
                subtitle = if (uiState.approvedPlayers.isEmpty()) {
                    "يمكن للاعبين إرسال طلبات التسجيل، وستظهر بطاقاتهم الاحترافية هنا فور اعتمادها من المشرف."
                } else {
                    "جرّب البحث باسم آخر أو بمعرف ElysiumNet."
                },
                actionLabel = if (uiState.isRegistrationOpenNow && uiState.approvedPlayers.isEmpty()) "تسجيل أول لاعب" else null,
                onAction = if (uiState.isRegistrationOpenNow && uiState.approvedPlayers.isEmpty()) onOpenRegister else null
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 168.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPlayers, key = { it.id }) { player ->
                    FifaPlayerCard(
                        player = player,
                        stats = statsById[player.id],
                        isChampion = uiState.championPlayer?.id == player.id
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterPlayerDialog(
    onDismiss: () -> Unit,
    onImportAvatar: (android.net.Uri, (String) -> Unit) -> Unit,
    onSubmit: (name: String, elysiumId: String, avatarUri: String?, level: PlayerLevel, club: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var elysiumId by remember { mutableStateOf("") }
    var clubName by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf(PlayerLevel.INTERMEDIATE) }
    var avatarUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportAvatar(uri) { savedPath -> avatarUri = savedPath }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = {
            Text(
                text = "التسجيل في بطولة FIFA 22",
                style = MaterialTheme.typography.titleLarge,
                color = ChampionshipGold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlayerAvatar(
                        avatarUri = avatarUri,
                        playerName = name.ifBlank { "P" },
                        size = 58.dp,
                        borderColor = ElectricCobalt
                    )
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("register_pick_avatar")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (avatarUri == null) "صورة اختيارية" else "تغيير الصورة")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم اللاعب *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_name_input")
                )

                OutlinedTextField(
                    value = elysiumId,
                    onValueChange = { elysiumId = it },
                    label = { Text("معرف اللاعب في ElysiumNet *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_elysium_input")
                )

                OutlinedTextField(
                    value = clubName,
                    onValueChange = { clubName = it },
                    label = { Text("الفريق المفضل (اختياري)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_club_input")
                )

                Text(
                    text = "المستوى المبدئي (قابل للتعديل من المشرف)",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { selectedLevel = lvl },
                            label = { Text(lvl.labelAr, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCobalt,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(name, elysiumId, avatarUri, selectedLevel, clubName) },
                enabled = name.isNotBlank() && elysiumId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                modifier = Modifier.testTag("register_submit_button")
            ) {
                Text("إرسال طلب التسجيل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}
