package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object TournamentShareHelper {

    fun createShareTextIntent(title: String, text: String): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(sendIntent, "مشاركة نتائج البطولة عبر")
    }

    fun createShareFileIntent(
        fileUri: Uri,
        title: String,
        captionText: String
    ): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, captionText)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "مشاركة ملف نتائج البطولة")
    }

    fun writeSummaryToUri(
        context: Context,
        uri: Uri,
        content: String
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun writeCacheSummaryFileUri(
        context: Context,
        fileName: String,
        content: String
    ): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val file = File(exportDir, safeName)
            file.writeText(content, Charsets.UTF_8)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            null
        }
    }
}
