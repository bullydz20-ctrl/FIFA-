package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.AdminAccountEntity
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.model.AdminRole
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.ui.components.MatchResultsTableCard
import com.example.ui.components.PlayerAvatar
import com.example.ui.components.TournamentExportShareCard
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentUiState

@Composable
fun AdminPlayersTab(
    uiState: TournamentUiState,
    onStatusChange: (PlayerEntity, PlayerStatus) -> Unit,
    onUpdateDetails: (PlayerEntity) -> Unit,
    onDeletePlayer: (PlayerEntity) -> Unit
) {
    var editingPlayer by remember { mutableStateOf<PlayerEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Pending queue
        Text(
            text = "طلبات التسجيل الجديدة (${uiState.pendingPlayers.size})",
            style = MaterialTheme.typography.titleMedium,
            color = ChampionshipGold
        )

        if (uiState.pendingPlayers.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                border = BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "لا توجد طلبات بانتظار المراجعة حالياً.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            uiState.pendingPlayers.forEach { player ->
                AdminPlayerRow(
                    player = player,
                    onApprove = { onStatusChange(player, PlayerStatus.APPROVED) },
                    onReject = { onStatusChange(player, PlayerStatus.REJECTED) },
                    onEdit = { editingPlayer = player },
                    onDelete = { onDeletePlayer(player) }
                )
            }
        }

        // Approved players
        Text(
            text = "اللاعبون المعتمدون (${uiState.approvedPlayers.size})",
            style = MaterialTheme.typography.titleMedium,
            color = EmeraldWin
        )
        uiState.approvedPlayers.forEach { player ->
            AdminPlayerRow(
                player = player,
                onApprove = null,
                onReject = { onStatusChange(player, PlayerStatus.REJECTED) },
                onEdit = { editingPlayer = player },
                onDelete = { onDeletePlayer(player) }
            )
        }

        if (uiState.rejectedPlayers.isNotEmpty()) {
            Text(
                text = "المرفوضون (${uiState.rejectedPlayers.size})",
                style = MaterialTheme.typography.titleMedium,
                color = CrimsonLoss
            )
            uiState.rejectedPlayers.forEach { player ->
                AdminPlayerRow(
                    player = player,
                    onApprove = { onStatusChange(player, PlayerStatus.APPROVED) },
                    onReject = null,
                    onEdit = { editingPlayer = player },
                    onDelete = { onDeletePlayer(player) }
                )
            }
        }
    }

    editingPlayer?.let { target ->
        EditPlayerDialog(
            player = target,
            onDismiss = { editingPlayer = null },
            onSave = { updated ->
                onUpdateDetails(updated)
                editingPlayer = null
            }
        )
    }
}

@Composable
private fun AdminPlayerRow(
    player: PlayerEntity,
    onApprove: (() -> Unit)?,
    onReject: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                PlayerAvatar(avatarUri = player.avatarUri, playerName = player.name, size = 40.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(player.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        text = "${player.elysiumNetId} • ${player.level.labelAr} (${player.ovrRating})",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberSky
                    )
                }
            }
            Row {
                if (onApprove != null) {
                    IconButton(onClick = onApprove, modifier = Modifier.testTag("approve_player_${player.id}")) {
                        Icon(Icons.Default.Check, contentDescription = "قبول", tint = EmeraldWin)
                    }
                }
                if (onReject != null) {
                    IconButton(onClick = onReject, modifier = Modifier.testTag("reject_player_${player.id}")) {
                        Icon(Icons.Default.Close, contentDescription = "رفض", tint = CrimsonLoss)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = ChampionshipGold)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun EditPlayerDialog(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onSave: (PlayerEntity) -> Unit
) {
    var name by remember(player.id) { mutableStateOf(player.name) }
    var elysiumId by remember(player.id) { mutableStateOf(player.elysiumNetId) }
    var club by remember(player.id) { mutableStateOf(player.clubName) }
    var level by remember(player.id) { mutableStateOf(player.level) }
    var ovr by remember(player.id) { mutableStateOf(player.ovrRating.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = { Text("تعديل بيانات اللاعب", color = ChampionshipGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = elysiumId, onValueChange = { elysiumId = it }, label = { Text("معرف ElysiumNet") }, singleLine = true)
                OutlinedTextField(value = club, onValueChange = { club = it }, label = { Text("النادي") }, singleLine = true)
                OutlinedTextField(
                    value = ovr,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) ovr = it },
                    label = { Text("التقييم (60-99)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PlayerLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = level == lvl,
                            onClick = {
                                level = lvl
                                ovr = lvl.defaultOvr.toString()
                            },
                            label = { Text(lvl.labelAr, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        player.copy(
                            name = name.trim(),
                            elysiumNetId = elysiumId.trim(),
                            clubName = club.trim(),
                            level = level,
                            ovrRating = (ovr.toIntOrNull() ?: level.defaultOvr).coerceIn(60, 99)
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt)
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AdminMatchesTab(
    uiState: TournamentUiState,
    onEditMatch: (MatchEntity) -> Unit,
    onQuickUpdateGoals: (MatchEntity, Int, Int) -> Unit = { _, _, _ -> }
) {
    val playersById = uiState.players.associateBy { it.id }

    MatchResultsTableCard(
        matches = uiState.allMatches,
        playersById = playersById,
        canEdit = true,
        onQuickUpdateGoals = onQuickUpdateGoals,
        onOpenDetailedEditor = onEditMatch,
        title = "جدول إدارة نتائج المباريات",
        showFilters = true
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSubAdminsTab(
    uiState: TournamentUiState,
    onChangeOwnPin: (currentPin: String, newPin: String) -> Unit,
    onCreateSubAdmin: (String, String, String, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUpdateSubAdmin: (AdminAccountEntity, String, String?, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onDeleteSubAdmin: (AdminAccountEntity) -> Unit
) {
    val subAdmins = uiState.admins.filter { it.role == AdminRole.SUB_ADMIN }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingSubAdmin by remember { mutableStateOf<AdminAccountEntity?>(null) }
    var deletingSubAdmin by remember { mutableStateOf<AdminAccountEntity?>(null) }
    var showChangeHeadPinDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Head Admin Security Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حماية المشرف الرئيسي والتشفير",
                            style = MaterialTheme.typography.titleSmall,
                            color = ChampionshipGold
                        )
                        Text(
                            text = "التحقق محمي بتشفير PBKDF2-SHA256 وقفل تلقائي بعد 5 محاولات خاطئة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { showChangeHeadPinDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("change_head_pin_button")
                    ) {
                        Text("تغيير PIN")
                    }
                }
            }
        }

        // 5 Sub-Admin Slots Header + Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "الحسابات المساعدة (${subAdmins.size} / ${com.example.security.PinSecurityManager.MAX_SUB_ADMINS})",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChampionshipGold
                )
                Text(
                    text = "أنشئ وعدّل واحذف حتى 5 حسابات مساعدة مع صلاحيات مخصصة لكل حساب.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { showCreateDialog = true },
                enabled = subAdmins.size < com.example.security.PinSecurityManager.MAX_SUB_ADMINS,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                modifier = Modifier.testTag("add_sub_admin_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة")
            }
        }

        LinearProgressIndicator(
            progress = { (subAdmins.size.toFloat() / com.example.security.PinSecurityManager.MAX_SUB_ADMINS.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = CyberSky
        )

        // 5 explicit slots
        for (slotIndex in 0 until com.example.security.PinSecurityManager.MAX_SUB_ADMINS) {
            val acc = subAdmins.getOrNull(slotIndex)
            if (acc != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface),
                    border = BorderStroke(1.dp, if (acc.isActive) ElectricCobalt.copy(alpha = 0.55f) else CrimsonLoss.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = ElectricCobalt.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "حساب #${slotIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberSky,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${acc.displayName} (@${acc.username})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (acc.isActive) "الحالة: نشط ومسموح بالدخول" else "الحالة: موقوف مؤقتاً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (acc.isActive) EmeraldWin else CrimsonLoss
                                )
                            }
                            Row {
                                IconButton(
                                    onClick = { editingSubAdmin = acc },
                                    modifier = Modifier.testTag("edit_sub_admin_${acc.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب والصلاحيات", tint = ChampionshipGold)
                                }
                                IconButton(
                                    onClick = { deletingSubAdmin = acc },
                                    modifier = Modifier.testTag("delete_sub_admin_${acc.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف الحساب المساعد", tint = CrimsonLoss)
                                }
                            }
                        }

                        // Permission Badges
                        val granted = com.example.security.AdminPermission.entries.filter {
                            when (it) {
                                com.example.security.AdminPermission.MANAGE_TOURNAMENT -> acc.canManageTournament
                                com.example.security.AdminPermission.MANAGE_PLAYERS -> acc.canManagePlayers
                                com.example.security.AdminPermission.RUN_DRAW -> acc.canRunDraw
                                com.example.security.AdminPermission.EDIT_SCORES -> acc.canEditScores
                                com.example.security.AdminPermission.CROWN_CHAMPION -> acc.canCrownChampion
                            }
                        }
                        if (granted.isEmpty()) {
                            Text("بدون صلاحيات مفعلة", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                granted.forEach { perm ->
                                    Surface(
                                        color = com.example.ui.theme.ElevatedNavy,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, CyberSky.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = perm.titleAr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = CyberSky,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty Slot Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateDialog = true }
                        .testTag("empty_sub_admin_slot_${slotIndex + 1}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightSurface.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, CardBorder.copy(alpha = 0.7f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "خانة مساعد #${slotIndex + 1} — شاغرة",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "اضغط لإنشاء حساب مساعد وتحديد صلاحياته",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberSky
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إنشاء حساب مساعد",
                            tint = CyberSky
                        )
                    }
                }
            }
        }
    }

    if (showChangeHeadPinDialog) {
        ChangeOwnPinDialog(
            onDismiss = { showChangeHeadPinDialog = false },
            onConfirm = { oldPin, newPin ->
                onChangeOwnPin(oldPin, newPin)
                showChangeHeadPinDialog = false
            }
        )
    }

    if (showCreateDialog) {
        SubAdminFormDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
            onSave = { user, display, pin, _, p1, p2, p3, p4, p5 ->
                onCreateSubAdmin(user, display, pin, p1, p2, p3, p4, p5)
                showCreateDialog = false
            }
        )
    }

    editingSubAdmin?.let { target ->
        SubAdminFormDialog(
            initial = target,
            onDismiss = { editingSubAdmin = null },
            onSave = { _, display, pin, isActive, p1, p2, p3, p4, p5 ->
                onUpdateSubAdmin(target, display, pin.ifBlank { null }, isActive, p1, p2, p3, p4, p5)
                editingSubAdmin = null
            }
        )
    }

    deletingSubAdmin?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSubAdmin = null },
            containerColor = MidnightSurface,
            title = { Text("تأكيد حذف الحساب المساعد", color = CrimsonLoss) },
            text = {
                Text(
                    text = "هل تريد حذف حساب ${target.displayName} (@${target.username}) نهائياً؟ سيتم إنهاء أي جلسة مفتوحة له.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubAdmin(target)
                        deletingSubAdmin = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonLoss),
                    modifier = Modifier.testTag("confirm_delete_sub_admin")
                ) { Text("حذف نهائي") }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubAdmin = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun ChangeOwnPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPin: String, newPin: String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val strength = com.example.security.PinSecurityManager.evaluateStrength(newPin)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = { Text("تغيير رمز PIN للمشرف الرئيسي", color = ChampionshipGold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                com.example.ui.components.SixDigitPinInputField(
                    value = currentPin,
                    onValueChange = { currentPin = it },
                    label = "رمز PIN الحالي (6 أرقام)",
                    testTag = "change_pin_current"
                )
                com.example.ui.components.SixDigitPinInputField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = "رمز PIN الجديد (6 أرقام)",
                    testTag = "change_pin_new",
                    supportingText = if (newPin.isNotEmpty()) "قوة الرمز: ${strength.labelAr}" else null
                )
                com.example.ui.components.SixDigitPinInputField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = "تأكيد رمز PIN الجديد",
                    testTag = "change_pin_confirm",
                    isError = confirmPin.isNotEmpty() && confirmPin != newPin,
                    supportingText = if (confirmPin.isNotEmpty() && confirmPin != newPin) "رمز التأكيد غير مطابق" else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPin, newPin) },
                enabled = com.example.security.PinSecurityManager.isValidPinFormat(currentPin) &&
                    com.example.security.PinSecurityManager.isValidPinFormat(newPin) &&
                    newPin == confirmPin,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                modifier = Modifier.testTag("change_pin_save")
            ) { Text("تحديث الرمز") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubAdminFormDialog(
    initial: AdminAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var displayName by remember { mutableStateOf(initial?.displayName ?: "") }
    var pin by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }
    var pTournament by remember { mutableStateOf(initial?.canManageTournament ?: false) }
    var pPlayers by remember { mutableStateOf(initial?.canManagePlayers ?: true) }
    var pDraw by remember { mutableStateOf(initial?.canRunDraw ?: false) }
    var pScores by remember { mutableStateOf(initial?.canEditScores ?: true) }
    var pChampion by remember { mutableStateOf(initial?.canCrownChampion ?: false) }

    val pinValidForSave = if (initial == null) {
        com.example.security.PinSecurityManager.isValidPinFormat(pin)
    } else {
        pin.isBlank() || com.example.security.PinSecurityManager.isValidPinFormat(pin)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = { Text(if (initial == null) "إضافة مشرف مساعد" else "تعديل حساب ${initial.displayName}", color = ChampionshipGold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initial == null) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم للدخول *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("sub_admin_username_input")
                    )
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("الاسم الظاهر *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sub_admin_display_input")
                )
                if (initial != null) {
                    PermissionToggleRow("حساب نشط (مسموح بالدخول)", isActive) { isActive = it }
                }
                com.example.ui.components.SixDigitPinInputField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = if (initial == null) "رمز PIN (6 أرقام) *" else "رمز PIN جديد (اختياري - 6 أرقام)",
                    testTag = "sub_admin_pin_input",
                    supportingText = if (pin.isNotEmpty()) {
                        "قوة الرمز: ${com.example.security.PinSecurityManager.evaluateStrength(pin).labelAr}"
                    } else null
                )
                Text("قوالب صلاحيات جاهزة:", style = MaterialTheme.typography.labelSmall, color = CyberSky)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    com.example.security.SubAdminPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                pTournament = com.example.security.AdminPermission.MANAGE_TOURNAMENT in preset.permissions
                                pPlayers = com.example.security.AdminPermission.MANAGE_PLAYERS in preset.permissions
                                pDraw = com.example.security.AdminPermission.RUN_DRAW in preset.permissions
                                pScores = com.example.security.AdminPermission.EDIT_SCORES in preset.permissions
                                pChampion = com.example.security.AdminPermission.CROWN_CHAMPION in preset.permissions
                            },
                            label = { Text(preset.labelAr, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("preset_${preset.name.lowercase()}")
                        )
                    }
                }
                Text("تحديد الصلاحيات بدقة:", style = MaterialTheme.typography.labelSmall, color = ChampionshipGold)
                PermissionDetailToggleRow(
                    title = com.example.security.AdminPermission.MANAGE_TOURNAMENT.titleAr,
                    subtitle = com.example.security.AdminPermission.MANAGE_TOURNAMENT.descriptionAr,
                    checked = pTournament,
                    tag = "perm_tournament"
                ) { pTournament = it }
                PermissionDetailToggleRow(
                    title = com.example.security.AdminPermission.MANAGE_PLAYERS.titleAr,
                    subtitle = com.example.security.AdminPermission.MANAGE_PLAYERS.descriptionAr,
                    checked = pPlayers,
                    tag = "perm_players"
                ) { pPlayers = it }
                PermissionDetailToggleRow(
                    title = com.example.security.AdminPermission.RUN_DRAW.titleAr,
                    subtitle = com.example.security.AdminPermission.RUN_DRAW.descriptionAr,
                    checked = pDraw,
                    tag = "perm_draw"
                ) { pDraw = it }
                PermissionDetailToggleRow(
                    title = com.example.security.AdminPermission.EDIT_SCORES.titleAr,
                    subtitle = com.example.security.AdminPermission.EDIT_SCORES.descriptionAr,
                    checked = pScores,
                    tag = "perm_scores"
                ) { pScores = it }
                PermissionDetailToggleRow(
                    title = com.example.security.AdminPermission.CROWN_CHAMPION.titleAr,
                    subtitle = com.example.security.AdminPermission.CROWN_CHAMPION.descriptionAr,
                    checked = pChampion,
                    tag = "perm_champion"
                ) { pChampion = it }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, displayName, pin, isActive, pTournament, pPlayers, pDraw, pScores, pChampion) },
                enabled = (initial != null || username.trim().length >= 3) && displayName.isNotBlank() && pinValidForSave,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                modifier = Modifier.testTag("sub_admin_save_button")
            ) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun PermissionDetailToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun PermissionToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminProgressAndChampionTab(
    uiState: TournamentUiState,
    onCrownChampion: (Long?) -> Unit,
    onOpenExportShare: () -> Unit = {}
) {
    val overview = uiState.overviewStats
    val progress = if (overview.totalMatches > 0) {
        overview.completedMatches.toFloat() / overview.totalMatches.toFloat()
    } else 0f

    val finalMatch = uiState.knockoutMatches.find { it.stage == MatchStage.FINAL }
    val finalWinner = finalMatch?.winnerId?.let { wId -> uiState.approvedPlayers.find { it.id == wId } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("متابعة تقدم البطولة", style = MaterialTheme.typography.titleMedium, color = CyberSky)
                Text(
                    text = "المباريات المكتملة: ${overview.completedMatches} من ${overview.totalMatches}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = ChampionshipGold
                )
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurface),
            border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("اختيار بطل البطولة وإعلان النتائج النهائية", style = MaterialTheme.typography.titleMedium, color = ChampionshipGold)

                if (finalWinner != null) {
                    Button(
                        onClick = { onCrownChampion(finalWinner.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = ChampionshipGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MidnightSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تتويج الفائز بالنهائي: ${finalWinner.name}", color = MidnightSurface)
                    }
                }

                Text("أو اختر البطل يدوياً:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.approvedPlayers.forEach { player ->
                        FilterChip(
                            selected = uiState.championPlayer?.id == player.id,
                            onClick = { onCrownChampion(player.id) },
                            label = { Text(player.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampionshipGold,
                                selectedLabelColor = MidnightSurface
                            )
                        )
                    }
                }

                if (uiState.championPlayer != null) {
                    OutlinedButton(
                        onClick = { onCrownChampion(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("إلغاء اعتماد البطل الحالي", color = CrimsonLoss)
                    }
                }
            }
        }

        TournamentExportShareCard(
            onOpenExportShare = onOpenExportShare,
            tag = "admin_export_share_card"
        )
    }
}
