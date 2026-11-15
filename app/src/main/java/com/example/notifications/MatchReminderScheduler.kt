package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.MatchReminderEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MatchReminderScheduler {

    const val CHANNEL_ID = "match_reminders_channel"
    const val EXTRA_MATCH_ID = "extra_match_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_STAGE = "extra_stage"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "تذكير المباريات القادمة",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "تنبيهات محلية لتذكير اللاعبين بمواعيد مبارياتهم القادمة في البطولة"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun buildAlarmIntent(
        context: Context,
        matchId: Long,
        title: String,
        body: String,
        stageLabel: String
    ): Intent {
        return Intent(context, MatchReminderReceiver::class.java).apply {
            action = "com.example.ACTION_MATCH_REMINDER_$matchId"
            putExtra(EXTRA_MATCH_ID, matchId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_STAGE, stageLabel)
        }
    }

    fun scheduleReminder(
        context: Context,
        reminder: MatchReminderEntity
    ) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val body = buildNotificationBody(reminder)
        val intent = buildAlarmIntent(
            context = context,
            matchId = reminder.matchId,
            title = "⏰ موعد مباراتك اقترب: ${reminder.matchTitle}",
            body = body,
            stageLabel = reminder.matchStageLabel
        )
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.matchId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = maxOf(reminder.remindAtMillis, System.currentTimeMillis() + 1_500L)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, matchId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MatchReminderReceiver::class.java).apply {
            action = "com.example.ACTION_MATCH_REMINDER_$matchId"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            matchId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    @SuppressLint("MissingPermission")
    fun showMatchNotification(
        context: Context,
        matchId: Long,
        title: String,
        body: String,
        stageLabel: String
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureNotificationChannel(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            matchId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(stageLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(matchId.toInt(), notification)
        return true
    }

    fun buildNotificationBody(reminder: MatchReminderEntity): String {
        val kickoffText = formatDateTime(reminder.scheduledAtMillis)
        val leadNote = if (reminder.leadMinutes > 0) {
            "تبدأ المباراة خلال ${reminder.leadMinutes} دقيقة ($kickoffText)."
        } else {
            "حان الآن موعد انطلاق المواجهة ($kickoffText)."
        }
        return "${reminder.matchStageLabel} • $leadNote جهّز فريقك على ElysiumNet!"
    }

    fun formatDateTime(millis: Long): String {
        val formatter = SimpleDateFormat("dd/MM • HH:mm", Locale.getDefault())
        return formatter.format(Date(millis))
    }
}
