package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.SessionPreferences
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.repository.RepoResult
import com.example.data.repository.TournamentRepository
import com.example.notifications.MatchReminderReceiver
import com.example.notifications.MatchReminderScheduler
import com.example.ui.components.MatchReminderDialog
import com.example.ui.components.MatchResultsTableCard
import com.example.ui.components.ScheduledRemindersCard
import com.example.ui.theme.FifaTournamentTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchReminderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Application
    private lateinit var db: AppDatabase
    private lateinit var repo: TournamentRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TournamentRepository(context, db, SessionPreferences(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun schedulingReminderPersistsInRoomSchedulesAlarmAndClearsOnCompletion() = runTest {
        repo.ensureDefaultTournamentShellIfEmpty()
        var tournament = repo.latestTournament.first()!!
        repo.saveTournamentSettings(
            tournament.copy(maxParticipants = 4, format = TournamentFormat.DIRECT_KNOCKOUT)
        )
        tournament = repo.latestTournament.first()!!

        for (i in 1..2) {
            assertTrue(
                repo.registerPlayer(
                    tournamentId = tournament.id,
                    name = "لاعب $i",
                    elysiumNetId = "EN_$i",
                    avatarUri = null,
                    level = PlayerLevel.PRO,
                    clubName = "Team $i"
                ) is RepoResult.Success
            )
        }
        repo.players.first().forEach { p ->
            assertTrue(repo.updatePlayerStatus(p, PlayerStatus.APPROVED) is RepoResult.Success)
        }
        assertTrue(repo.runDrawAndGenerateSchedule() is RepoResult.Success)

        val upcomingMatch = repo.matches.first().first()
        val futureKickoff = System.currentTimeMillis() + 60 * 60_000L // +1 hour

        // Past kickoff should fail cleanly
        val pastRes = repo.scheduleMatchReminder(
            match = upcomingMatch,
            matchTitle = "لاعب 1 ضد لاعب 2",
            scheduledAtMillis = System.currentTimeMillis() - 10_000L,
            leadMinutes = 15
        )
        assertTrue(pastRes is RepoResult.Error)

        // Schedule valid future reminder (15 min before)
        val okRes = repo.scheduleMatchReminder(
            match = upcomingMatch,
            matchTitle = "لاعب 1 ضد لاعب 2",
            scheduledAtMillis = futureKickoff,
            leadMinutes = 15
        )
        assertTrue(okRes is RepoResult.Success)

        // Verify Room stored both match.scheduledAtMillis and match_reminders row
        val refreshedMatch = db.matchDao().getMatchById(upcomingMatch.id)!!
        assertEquals(futureKickoff, refreshedMatch.scheduledAtMillis)
        val storedReminders = repo.matchReminders.first()
        assertEquals(1, storedReminders.size)
        assertEquals(upcomingMatch.id, storedReminders.first().matchId)
        assertEquals(15, storedReminders.first().leadMinutes)

        // Verify AlarmManager received the scheduled alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val scheduled = shadowOf(alarmManager).scheduledAlarms
        assertTrue(scheduled.isNotEmpty())

        // Completing the match removes the scheduled reminder from Room and cancels alarm
        assertTrue(repo.updateMatchScore(refreshedMatch, 3, 1, null, null, null, null) is RepoResult.Success)
        assertTrue(repo.matchReminders.first().isEmpty())
        assertTrue(shadowOf(alarmManager).scheduledAlarms.isEmpty())
    }

    @Test
    fun broadcastReceiverPostsNotificationWhenTriggered() {
        val intent = MatchReminderScheduler.buildAlarmIntent(
            context = context,
            matchId = 77L,
            title = "⏰ تذكير مباراة النخبة",
            body = "ربع النهائي • تبدأ المباراة خلال 15 دقيقة",
            stageLabel = "ربع النهائي"
        )

        MatchReminderReceiver().onReceive(context, intent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(notificationManager).allNotifications
        assertTrue(posted.isNotEmpty())
    }

    @Test
    fun matchTableAndReminderDialogAndScheduledCardUiWorkCorrectly() {
        val p1 = PlayerEntity(
            id = 1L, tournamentId = 1L, name = "ياسين", elysiumNetId = "EN-1",
            level = PlayerLevel.PRO, ovrRating = 90, status = PlayerStatus.APPROVED
        )
        val p2 = PlayerEntity(
            id = 2L, tournamentId = 1L, name = "أمين", elysiumNetId = "EN-2",
            level = PlayerLevel.PRO, ovrRating = 88, status = PlayerStatus.APPROVED
        )
        val upcomingMatch = MatchEntity(
            id = 10L, tournamentId = 1L, stage = MatchStage.GROUP,
            roundNumber = 1, player1Id = 1L, player2Id = 2L, isCompleted = false
        )
        val finishedMatch = MatchEntity(
            id = 11L, tournamentId = 1L, stage = MatchStage.GROUP,
            roundNumber = 1, player1Id = 1L, player2Id = 2L,
            leg1Score1 = 2, leg1Score2 = 0, isCompleted = true, winnerId = 1L
        )
        val activeReminder = MatchReminderEntity(
            id = 1L,
            matchId = 10L,
            tournamentId = 1L,
            matchTitle = "ياسين ضد أمين",
            matchStageLabel = "دور المجموعات",
            scheduledAtMillis = System.currentTimeMillis() + 3_600_000L,
            remindAtMillis = System.currentTimeMillis() + 2_700_000L,
            leadMinutes = 15
        )

        var openedReminderFor: Long? = null
        var deletedReminderFor: Long? = null

        composeRule.setContent {
            FifaTournamentTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MatchResultsTableCard(
                        matches = listOf(upcomingMatch, finishedMatch),
                        playersById = mapOf(1L to p1, 2L to p2),
                        canEdit = false,
                        onQuickUpdateGoals = { _, _, _ -> },
                        onOpenDetailedEditor = {},
                        remindersByMatchId = mapOf(10L to activeReminder),
                        onOpenReminder = { openedReminderFor = it.id }
                    )
                    ScheduledRemindersCard(
                        reminders = listOf(activeReminder),
                        onCancelReminder = { deletedReminderFor = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Upcoming match has reminder button; completed match does not
        composeRule.onNodeWithTag("match_10_reminder_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(10L, openedReminderFor)
        composeRule.onNodeWithTag("match_11_reminder_button").assertDoesNotExist()

        // ScheduledRemindersCard shows reminder and cancels on delete tap
        composeRule.onNodeWithTag("scheduled_reminder_item_10").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("reminder_delete_10").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(10L, deletedReminderFor)
    }

    @Test
    fun matchReminderDialogSchedulesTestsAndCancels() {
        val p1 = PlayerEntity(
            id = 1L, tournamentId = 1L, name = "ياسين", elysiumNetId = "EN-1",
            level = PlayerLevel.PRO, ovrRating = 90, status = PlayerStatus.APPROVED
        )
        val p2 = PlayerEntity(
            id = 2L, tournamentId = 1L, name = "أمين", elysiumNetId = "EN-2",
            level = PlayerLevel.PRO, ovrRating = 88, status = PlayerStatus.APPROVED
        )
        val upcomingMatch = MatchEntity(
            id = 20L, tournamentId = 1L, stage = MatchStage.FINAL,
            roundNumber = 5, player1Id = 1L, player2Id = 2L, isCompleted = false
        )
        val existingReminder = MatchReminderEntity(
            id = 2L, matchId = 20L, tournamentId = 1L, matchTitle = "ياسين ضد أمين",
            matchStageLabel = "المباراة النهائية",
            scheduledAtMillis = System.currentTimeMillis() + 3_600_000L,
            remindAtMillis = System.currentTimeMillis() + 2_700_000L,
            leadMinutes = 15
        )

        var scheduledLead = -1
        var testedNow = false
        var cancelled = false

        composeRule.setContent {
            FifaTournamentTheme {
                MatchReminderDialog(
                    match = upcomingMatch,
                    player1 = p1,
                    player2 = p2,
                    existingReminder = existingReminder,
                    onDismiss = {},
                    onScheduleReminder = { _, lead -> scheduledLead = lead },
                    onTestNotification = { _, _ -> testedNow = true },
                    onCancelReminder = { cancelled = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reminder_preset_1h").performScrollTo().performClick()
        composeRule.onNodeWithTag("reminder_lead_5").performScrollTo().performClick()
        composeRule.onNodeWithTag("reminder_confirm_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(5, scheduledLead)

        composeRule.onNodeWithTag("reminder_test_now_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(testedNow)

        composeRule.onNodeWithTag("reminder_cancel_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(cancelled)
    }
}
