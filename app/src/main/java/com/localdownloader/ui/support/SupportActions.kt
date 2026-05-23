package com.localdownloader.ui.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.utils.SensitiveDataSanitizer
import java.io.File
import java.util.ArrayList

private const val ISSUE_TRACKER_URL = "https://github.com/iam-sandipmaity/video-downloader/issues/new"
private const val LOGS_DIRECTORY = "logs"
private const val APP_LOG_FILE = "app.log"
private const val CRASH_LOG_FILE = "crash.log"

fun openSupportIssue(
    context: Context,
    task: DownloadTask? = null,
    flaggedAsStuck: Boolean = false,
) {
    val title = buildIssueTitle(task, flaggedAsStuck)
    val body = buildIssueBody(task, flaggedAsStuck)
    val issueUri = Uri.parse(ISSUE_TRACKER_URL).buildUpon()
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

    context.startActivity(Intent.createChooser(shareIntent, "Share log.txt"))
}

private fun availableLogFiles(context: Context): List<File> {
    val logsDir = File(context.filesDir, LOGS_DIRECTORY)
    return listOf(
        File(logsDir, APP_LOG_FILE),
        File(logsDir, CRASH_LOG_FILE),
    ).filter { it.exists() && it.length() > 0L }
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
        appendLine("- Shared log.txt / crash log from the app")
        appendLine("- Short note explaining what you expected and what happened instead")
    }.trim()
}
