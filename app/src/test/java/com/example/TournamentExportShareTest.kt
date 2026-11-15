package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.TournamentGroupEntity
import com.example.data.model.MatchStage
import com.example.data.model.PlayerLevel
import com.example.data.model.PlayerStatus
import com.example.data.model.TournamentFormat
import com.example.data.model.TournamentStatus
import com.example.domain.GroupStandingRow
import com.example.domain.PlayerStats
import com.example.domain.TournamentExportFormatter
import com.example.domain.TournamentOverviewStats
import com.example.export.TournamentShareHelper
import com.example.ui.components.ExportShareDialog
import com.example.ui.theme.FifaTournamentTheme
import com.example.ui.viewmodel.GroupWithStandings
import com.example.ui.viewmodel.TournamentUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TournamentExportShareTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun sampleUiState(): TournamentUiState {
        val p1 = PlayerEntity(
            id = 1L, tournamentId = 10L, name = "ياسين", elysiumNetId = "EN-Yassin",
            clubName = "Real Madrid", level = PlayerLevel.PRO, ovrRating = 90, status = PlayerStatus.APPROVED
        )
        val p2 = PlayerEntity(
            id = 2L, tournamentId = 10L, name = "أمين", elysiumNetId = "EN-Amine",
            clubName = "PSG", level = PlayerLevel.PRO, ovrRating = 88, status = PlayerStatus.APPROVED
        )
        val finalMatch = MatchEntity(
            id = 50L, tournamentId = 10L, stage = MatchStage.FINAL,
            roundNumber = 5, bracketIndex = 0, player1Id = 1L, player2Id = 2L,
            leg1Score1 = 4, leg1Score2 = 2, isCompleted = true, winnerId = 1L
        )
        val group = TournamentGroupEntity(id = 5L, tournamentId = 10L, name = "المجموعة A", displayOrder = 0)
        val standings = listOf(
            GroupStandingRow(p1, 1, 1, 1, 0, 0, 4, 2, 2, 3, true),
            GroupStandingRow(p2, 2, 1, 0, 0, 1, 2, 4, -2, 0, false)
        )
        val p1Stats = PlayerStats(p1, 1, 1, 0, 0, 4, 2, 0, 100)
        val p2Stats = PlayerStats(p2, 1, 0, 0, 1, 2, 4, 0, 0)

        return TournamentUiState(
            isLoaded = true,
            tournament = TournamentEntity(
                id = 10L,
                name = "بطولة النخبة FIFA 22",
                description = "بطولة مجتمع PS4",
                startDateTime = "الجمعة 21:00",
                format = TournamentFormat.GROUPS_KNOCKOUT,
                status = TournamentStatus.COMPLETED,
                maxParticipants = 16,
                championPlayerId = 1L
            ),
            players = listOf(p1, p2),
            approvedPlayers = listOf(p1, p2),
            groupsWithStandings = listOf(GroupWithStandings(group, standings, emptyList())),
            knockoutMatches = listOf(finalMatch),
            allMatches = listOf(finalMatch),
            playerStats = listOf(p1Stats, p2Stats),
            overviewStats = TournamentOverviewStats(
                totalMatches = 1,
                completedMatches = 1,
                totalGoals = 6,
                topScorer = p1Stats,
                bestDefense = p1Stats,
                highestWinRate = p1Stats
            ),
            championPlayer = p1
        )
    }

    @Test
    fun formatterGeneratesCompleteSummaryAndSocialPost() {
        val state = sampleUiState()
        val full = TournamentExportFormatter.buildFullSummaryText(state)
        assertTrue(full.contains("بطولة النخبة FIFA 22"))
        assertTrue(full.contains("بطل البطولة: ياسين (EN-Yassin)"))
        assertTrue(full.contains("هداف البطولة: ياسين (4 هدف)"))
        assertTrue(full.contains("المجموعة A"))
        assertTrue(full.contains("المباراة النهائية"))
        assertTrue(full.contains("ياسين (4 - 2) أمين ★ الفائز: ياسين"))

        val social = TournamentExportFormatter.buildSocialPostText(state)
        assertTrue(social.contains("البطل المتوج: ياسين"))
        assertTrue(social.contains("#FIFA22"))
        assertTrue(social.contains("4 - 2"))

        val fileName = TournamentExportFormatter.buildSuggestedFileName(state)
        assertTrue(fileName.endsWith(".txt"))
    }

    @Test
    fun shareHelperCreatesIntentsAndWritesTextFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val textIntent = TournamentShareHelper.createShareTextIntent("عنوان", "ملخص النتائج")
        assertEquals(Intent.ACTION_CHOOSER, textIntent.action)
        @Suppress("DEPRECATION")
        val wrapped = textIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(wrapped)
        assertEquals(Intent.ACTION_SEND, wrapped?.action)
        assertEquals("text/plain", wrapped?.type)
        assertEquals("ملخص النتائج", wrapped?.getStringExtra(Intent.EXTRA_TEXT))

        // Write via FileProvider & verify file URI share intent
        val providerUri = TournamentShareHelper.writeCacheSummaryFileUri(
            context = context,
            fileName = "summary_test.txt",
            content = "نتيجة النهائي: 4 - 2"
        )
        assertNotNull(providerUri)
        val fileIntent = TournamentShareHelper.createShareFileIntent(providerUri!!, "ملف", "نص")
        assertEquals(Intent.ACTION_CHOOSER, fileIntent.action)
        @Suppress("DEPRECATION")
        val wrappedFile = fileIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertEquals(Intent.ACTION_SEND, wrappedFile?.action)
        assertTrue((wrappedFile?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        // Write via Uri output stream (SAF style)
        val targetFile = File(context.cacheDir, "saf_output_test.txt")
        val ok = TournamentShareHelper.writeSummaryToUri(context, Uri.fromFile(targetFile), "تجربة الحفظ النصي")
        assertTrue(ok)
        assertEquals("تجربة الحفظ النصي", targetFile.readText(Charsets.UTF_8))
    }

    @Test
    fun exportShareDialogTogglesModesAndTriggersAllExportActions() {
        val state = sampleUiState()
        var exportedName = ""
        var exportedBody = ""
        var sharedTextBody = ""
        var sharedFileName = ""
        var copiedBody = ""

        composeRule.setContent {
            FifaTournamentTheme {
                ExportShareDialog(
                    uiState = state,
                    onDismiss = {},
                    onExportTxtFile = { name, body ->
                        exportedName = name
                        exportedBody = body
                    },
                    onShareSocialText = { _, body ->
                        sharedTextBody = body
                    },
                    onShareTxtAttachment = { name, _, _ ->
                        sharedFileName = name
                    },
                    onCopySummary = { body ->
                        copiedBody = body
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("export_preview_text").assertIsDisplayed()

        // Switch to Social mode & trigger social Intent share
        composeRule.onNodeWithTag("export_mode_social").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("export_share_social_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(sharedTextBody.contains("#FIFA22"))

        // Switch back to Full mode & trigger .txt export, file share, and copy
        composeRule.onNodeWithTag("export_mode_full").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("export_save_txt_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(exportedName.endsWith(".txt"))
        assertTrue(exportedBody.contains("ترتيب المجموعات"))

        composeRule.onNodeWithTag("export_share_file_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(sharedFileName.endsWith(".txt"))

        composeRule.onNodeWithTag("export_copy_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(copiedBody.contains("بطل البطولة: ياسين"))
    }
}
