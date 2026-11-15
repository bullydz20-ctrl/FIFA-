package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.TournamentExportFormatter
import com.example.ui.theme.CardBorder
import com.example.ui.theme.ChampionshipGold
import com.example.ui.theme.CyberSky
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TournamentUiState

enum class ExportSummaryMode(val labelAr: String, val tag: String) {
    FULL("التقرير الكامل (.txt)", "export_mode_full"),
    SOCIAL("ملخص النشر السريع", "export_mode_social")
}

@Composable
fun TournamentExportShareCard(
    onOpenExportShare: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "home_export_share_card"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurface),
        border = BorderStroke(1.dp, CyberSky.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = ChampionshipGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تصدير ومشاركة نتائج البطولة",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampionshipGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "احفظ التقرير النهائي كملف نصي (.txt) أو شاركه مباشرة عبر Discord وواتساب وتطبيقات التواصل.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = onOpenExportShare,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tag}_button")
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تصدير التقرير / مشاركة النتائج")
            }
        }
    }
}

@Composable
fun ExportShareDialog(
    uiState: TournamentUiState,
    onDismiss: () -> Unit,
    onExportTxtFile: (suggestedFileName: String, content: String) -> Unit,
    onShareSocialText: (title: String, content: String) -> Unit,
    onShareTxtAttachment: (fileName: String, title: String, content: String) -> Unit,
    onCopySummary: (content: String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ExportSummaryMode.FULL) }
    val previewText = remember(uiState, selectedMode) {
        when (selectedMode) {
            ExportSummaryMode.FULL -> TournamentExportFormatter.buildFullSummaryText(uiState)
            ExportSummaryMode.SOCIAL -> TournamentExportFormatter.buildSocialPostText(uiState)
        }
    }
    val tournamentTitle = uiState.tournament?.name ?: "ملخص نتائج بطولة FIFA 22"
    val suggestedFileName = remember(uiState.tournament?.name) {
        TournamentExportFormatter.buildSuggestedFileName(uiState)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = ChampionshipGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تصدير ومشاركة نتائج البطولة",
                    style = MaterialTheme.typography.titleMedium,
                    color = ChampionshipGold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportSummaryMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode.labelAr, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCobalt,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(mode.tag)
                        )
                    }
                }

                // Live Preview Box
                Surface(
                    color = ObsidianVoid.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 210.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.testTag("export_preview_text")
                        )
                    }
                }

                // Primary Intent Actions
                Button(
                    onClick = { onShareSocialText(tournamentTitle, previewText) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldWin),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_share_social_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = ObsidianVoid,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة مباشرة عبر تطبيقات التواصل", color = ObsidianVoid, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onExportTxtFile(suggestedFileName, previewText) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCobalt),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_save_txt_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ كملف نصي (.txt) على الجهاز")
                }

                OutlinedButton(
                    onClick = { onShareTxtAttachment(suggestedFileName, tournamentTitle, previewText) },
                    border = BorderStroke(1.dp, ChampionshipGold.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_share_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = ChampionshipGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة التقرير كملف .txt مرفق", color = ChampionshipGold)
                }

                OutlinedButton(
                    onClick = { onCopySummary(previewText) },
                    border = BorderStroke(1.dp, CyberSky.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_copy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = CyberSky,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نسخ الملخص إلى الحافظة", color = CyberSky)
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
