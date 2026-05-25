package com.localdownloader.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.utils.SensitiveDataSanitizer
import java.io.File
import java.util.ArrayList

private const val ISSUE_TRACKER_URL = "https://github.com/iam-sandipmaity/video-downloader/issues/new"
private const val LOGS_DIRECTORY = "logs"
private const val APP_LOG_FILE = "app.log"
private const val CRASH_LOG_FILE = "crash.log"
private const val MAX_SHARED_LOG_FILES = 4

fun openSupportIssue(
    context: Context,
    task: DownloadTask? = null,
    flaggedAsStuck: Boolean = false,
) {
    val title = buildIssueTitle(task, flaggedAsStuck)
    val body = buildIssueBody(task, flaggedAsStuck)
    val issueUri = ISSUE_TRACKER_URL.toUri().buildUpon()
        .appendQueryParameter("title", title)
        .appendQueryParameter("body", body)
        .build()
    context.startActivity(Intent(Intent.ACTION_VIEW, issueUri))
}

fun shareAppLogs(
    context: Context,
    task: DownloadTask? = null,
    flaggedAsStuck: Boolean = false,
) {
    val logFiles = availableLogFiles(context)
    if (logFiles.isEmpty()) {
        Toast.makeText(context, "No log files are available yet.", Toast.LENGTH_SHORT).show()
        return
    }

    val uris = ArrayList<Uri>(logFiles.size)
    logFiles.forEach { file ->
        uris += FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    val body = buildIssueBody(task, flaggedAsStuck)
    val shareIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uris.first())
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share app logs"))
}

private fun availableLogFiles(context: Context): List<File> {
    val logsDir = File(context.filesDir, LOGS_DIRECTORY)
    return logsDir.listFiles()
        ?.filter(::isShareableLogFile)
        ?.sortedByDescending { it.lastModified() }
        ?.filter(::hasMeaningfulLogContent)
        ?.take(MAX_SHARED_LOG_FILES)
        .orEmpty()
}

private fun isShareableLogFile(file: File): Boolean {
    if (!file.isFile || !file.exists()) return false
    return when {
        file.name == APP_LOG_FILE -> true
        file.name == "$APP_LOG_FILE.1" -> true
        file.name.startsWith("app-") && file.name.endsWith(".log") -> true
        file.name == CRASH_LOG_FILE -> true
        file.name == "$CRASH_LOG_FILE.1" -> true
        file.name.startsWith("crash-") && file.name.endsWith(".log") -> true
        else -> false
    }
}

private fun hasMeaningfulLogContent(file: File): Boolean {
    if (file.length() <= 0L) return false
    return runCatching {
        file.bufferedReader().useLines { lines ->
            lines.any { it.isNotBlank() }
        }
    }.getOrDefault(false)
}

private fun buildIssueTitle(
    task: DownloadTask?,
    flaggedAsStuck: Boolean,
): String {
    val prefix = when {
        flaggedAsStuck -> "Download stuck"
        task?.status != null -> "Download ${task.status?.name?.lowercase().orEmpty()}"
        else -> "Download issue"
    }
    val taskTitle = task?.title?.trim().orEmpty().ifBlank { "report" }
    return "$prefix: $taskTitle".take(80)
}

private fun buildIssueBody(
    task: DownloadTask?,
    flaggedAsStuck: Boolean,
): String {
    val problemLine = when {
        flaggedAsStuck -> "The download looks stuck and is no longer updating."
        !task?.errorMessage.isNullOrBlank() -> task?.errorMessage.orEmpty()
        else -> "Please describe what happened."
    }
    return buildString {
        appendLine("## What happened")
        appendLine(problemLine)
        appendLine()
        appendLine("## Task details")
        appendLine("- Title: ${task?.title?.ifBlank { "Unknown" } ?: "Unknown"}")
        appendLine("- URL: ${SensitiveDataSanitizer.describeUrl(task?.url)}")
        appendLine("- Status: ${task?.status?.name ?: "Unknown"}")
        appendLine("- Progress: ${task?.progressPercent ?: 0}%")
        task?.speed?.takeIf { it.isNotBlank() }?.let { appendLine("- Speed: $it") }
        task?.eta?.takeIf { it.isNotBlank() }?.let { appendLine("- ETA: $it") }
        appendLine()
        appendLine("## What to attach")
        appendLine("- Screenshot of the queue or error state")
        appendLine("- Shared app logs from the app")
        appendLine("- Short note explaining what you expected and what happened instead")
    }.trim()
}
