package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderEntity
import com.example.data.local.PlayerEntity
import com.example.notifications.MatchReminderScheduler
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CrimsonLoss
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

private enum class KickoffPreset(val labelAr: String, val offsetMinutes: Int, val tag: String) {
    MIN_15("بعد 15 د", 15, "reminder_preset_15m"),
    MIN_30("بعد 30 د", 30, "reminder_preset_30m"),
    HOUR_1("بعد ساعة", 60, "reminder_preset_1h"),
    HOUR_3("بعد 3 ساعات", 180, "reminder_preset_3h")
}

private val LEAD_TIME_OPTIONS = listOf(
    0 to "عند الموعد",
    5 to "قبل 5 د",
    15 to "قبل 15 د",
    30 to "قبل 30 د"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchReminderDialog(
    match: MatchEntity,
    player1: PlayerEntity,
    player2: PlayerEntity,
    existingReminder: MatchReminderEntity?,
    onDismiss: () -> Unit,
    onScheduleReminder: (scheduledAtMillis: Long, leadMinutes: Int) -> Unit,
    onTestNotification: (scheduledAtMillis: Long, leadMinutes: Int) -> Unit,
    onCancelReminder: () -> Unit
) {
    val context = LocalContext.current
    val initialKickoff = remember(match.id, existingReminder) {
        val candidate = existingReminder?.scheduledAtMillis ?: match.scheduledAtMillis
        if (candidate != null && candidate > System.currentTimeMillis()) {
            candidate
        } else {
            System.currentTimeMillis() + 30 * 60_000L
        }
    }

    var scheduledAtMillis by remember(match.id) { mutableLongStateOf(initialKickoff) }
    var leadMinutes by remember(match.id) { mutableIntStateOf(existingReminder?.leadMinutes ?: 15) }

    val remindAtPreview = remember(scheduledAtMillis, leadMinutes) {
        maxOf(scheduledAtMillis - leadMinutes * 60_000L, System.currentTimeMillis() + 2_000L)
    }

    fun openCustomDateTimePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val timeCal = Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, day, hourOfDay, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        scheduledAtMillis = picked.timeInMillis
                    },
                    timeCal.get(Calendar.HOUR_OF_DAY),
                    timeCal.get(Calendar.MINUTE),
                    true
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ChampionshipGold)
                Column {
                    Text(
                        text = "جدولة تنبيه المباراة",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampionshipGold
                    )
                    Text(
                        text = "${player1.name} 🆚 ${player2.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberSky
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر موعد انطلاق المباراة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KickoffPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                scheduledAtMillis = System.currentTimeMillis() + preset.offsetMinutes * 60_000L
                            },
                            label = { Text(preset.labelAr, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ElevatedNavy,
                                labelColor = CyberSky
                            ),
                            modifier = Modifier.testTag(preset.tag)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { openCustomDateTimePicker() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberSky.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_pick_custom_time")
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = CyberSky, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديد التاريخ والساعة يدوياً", color = CyberSky)
                }

                Text(
                    text = "تنبيهني قبل المباراة بـ:",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LEAD_TIME_OPTIONS.forEach { (minutes, label) ->
                        FilterChip(
                            selected = leadMinutes == minutes,
                            onClick = { leadMinutes = minutes },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCobalt,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("reminder_lead_$minutes")
                        )
                    }
                }

                Surface(
                    color = ObsidianVoid.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "موعد المباراة: ${MatchReminderScheduler.formatDateTime(scheduledAtMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "وقت إرسال التنبيه: ${MatchReminderScheduler.formatDateTime(remindAtPreview)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChampionshipGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = { onScheduleReminder(scheduledAtMillis, leadMinutes) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_confirm_button")
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (existingReminder != null) "تحديث التنبيه المجدول" else "حفظ وجدولة التنبيه")
                }

                OutlinedButton(
                    onClick = { onTestNotification(scheduledAtMillis, leadMinutes) },
                    border = BorderStroke(1.dp, EmeraldWin.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_test_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = EmeraldWin,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال إشعار تجريبي الآن", color = EmeraldWin)
                }

                if (existingReminder != null) {
                    OutlinedButton(
                        onClick = onCancelReminder,
                        border = BorderStroke(1.dp, CrimsonLoss.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminder_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = CrimsonLoss,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إلغاء التنبيه المجدول", color = CrimsonLoss)
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
fun ScheduledRemindersCard(
    reminders: List<MatchReminderEntity>,
    onCancelReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scheduled_reminders_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, ElectricCobalt.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = ChampionshipGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تنبيهات المباريات القادمة (${reminders.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampionshipGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (reminders.isEmpty()) {
                Text(
                    text = "لم تجدول أي تنبيهات بعد. اضغط على أيقونة الجرس 🔔 بجانب أي مباراة قادمة لتذكيرك قبل موعدها.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                reminders.forEachIndexed { index, reminder ->
                    if (index > 0) {
                        HorizontalDivider(color = CardBorder.copy(alpha = 0.45f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scheduled_reminder_item_${reminder.matchId}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = reminder.matchTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${reminder.matchStageLabel} • التنبيه: ${MatchReminderScheduler.formatDateTime(reminder.remindAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberSky,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                color = if (reminder.isDelivered) EmeraldWin.copy(alpha = 0.16f) else ChampionshipGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (reminder.isDelivered) "تم التنبيه" else "مجدول",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (reminder.isDelivered) EmeraldWin else ChampionshipGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = { onCancelReminder(reminder.matchId) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("reminder_delete_${reminder.matchId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف تنبيه ${reminder.matchTitle}",
                                    tint = CrimsonLoss,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
