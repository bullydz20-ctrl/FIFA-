package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MatchReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val matchId = intent.getLongExtra(MatchReminderScheduler.EXTRA_MATCH_ID, -1L)
        if (matchId <= 0L) return

        val title = intent.getStringExtra(MatchReminderScheduler.EXTRA_TITLE)
            ?: "⏰ تذكير بمباراة قادمة"
        val body = intent.getStringExtra(MatchReminderScheduler.EXTRA_BODY)
            ?: "اقترب موعد مباراتك في البطولة."
        val stage = intent.getStringExtra(MatchReminderScheduler.EXTRA_STAGE)
            ?: "البطولة"

        MatchReminderScheduler.showMatchNotification(
            context = context,
            matchId = matchId,
            title = title,
            body = body,
            stageLabel = stage
        )

        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getInstance(context.applicationContext).matchReminderDao().markDelivered(matchId)
            } catch (_: Exception) {
                // Best-effort delivery flag update
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
