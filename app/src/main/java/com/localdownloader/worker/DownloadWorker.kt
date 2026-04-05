package com.localdownloader.worker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import java.io.File
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.localdownloader.data.DownloadTaskStore
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.downloader.DownloadEngine
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadEngine: DownloadEngine,
    private val downloadTaskStore: DownloadTaskStore,
    private val fileUtils: FileUtils,
    private val logger: Logger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = id.toString()
        logger.i("DownloadWorker", "doWork started taskId=$taskId")
        val url = inputData.getString(WorkerKeys.URL)
            ?: return Result.failure(workDataOf(WorkerKeys.ERROR_MESSAGE to "Missing URL"))
        val formatId = inputData.getString(WorkerKeys.FORMAT_ID)
            ?: return Result.failure(workDataOf(WorkerKeys.ERROR_MESSAGE to "Missing format id"))
        val outputTemplate = inputData.getString(WorkerKeys.OUTPUT_TEMPLATE)
            ?: return Result.failure(workDataOf(WorkerKeys.ERROR_MESSAGE to "Missing output template"))
        val titleHint = inputData.getString(WorkerKeys.TITLE_HINT).orEmpty()
        logger.i(
            "DownloadWorker",
            "Input parsed taskId=$taskId url=$url format=$formatId outputTemplate=$outputTemplate title='$titleHint'",
        )

        val options = DownloadOptions(
            url = url,
            formatId = formatId,
            outputTemplate = outputTemplate,
            mergeOutputFormat = inputData.getString(WorkerKeys.MERGE_OUTPUT_FORMAT).orEmpty().ifBlank { null },
            isPlaylistEnabled = inputData.getBoolean(WorkerKeys.PLAYLIST_ENABLED, false),
            shouldDownloadSubtitles = inputData.getBoolean(WorkerKeys.DOWNLOAD_SUBTITLES, false),
            shouldEmbedMetadata = inputData.getBoolean(WorkerKeys.EMBED_METADATA, true),
            shouldEmbedThumbnail = inputData.getBoolean(WorkerKeys.EMBED_THUMBNAIL, false),
            shouldWriteThumbnail = inputData.getBoolean(WorkerKeys.WRITE_THUMBNAIL, false),
            extractAudio = inputData.getBoolean(WorkerKeys.EXTRACT_AUDIO, false),
            audioFormat = inputData.getString(WorkerKeys.AUDIO_FORMAT).orEmpty().ifBlank { null },
            audioBitrateKbps = inputData.getInt(WorkerKeys.AUDIO_BITRATE, -1).takeIf { it > 0 },
        )
        logger.d("DownloadWorker", "DownloadOptions: $options")

        val runningTitle = titleHint.ifBlank { "Downloading..." }
        ensureTaskRunning(
            taskId = taskId,
            url = url,
            title = runningTitle,
        )
        appendDebugTrace(taskId, "Worker started")
        appendDebugTrace(taskId, "Input accepted: format=$formatId")
        appendDebugTrace(taskId, "Output template: $outputTemplate")

        val foregroundStarted = startForegroundIfPossible(
            title = titleHint.ifBlank { "Downloading media" },
            progress = 0,
            taskId = taskId,
        )
        appendDebugTrace(
            taskId,
            if (foregroundStarted) {
                "Foreground notification active"
            } else {
                "Foreground notification unavailable (permission denied or start failed)"
            },
        )

        var outputPath: String? = null
        var lastLoggedProgress = -1
        val result = runCatching {
            downloadEngine.runDownload(
                options = options,
                outputTemplate = outputTemplate,
                onProgress = { progress ->
                    val normalizedProgress = progress.percent?.coerceIn(0, 100)
                    logger.d(
                        "DownloadWorker/progress",
                        "taskId=$taskId percent=${normalizedProgress ?: "null"} speed=${progress.speed} eta=${progress.eta}",
                    )
                    if (normalizedProgress != null) {
                        updateForegroundIfPossible(
                            title = titleHint.ifBlank { "Downloading media" },
                            progress = normalizedProgress,
                            taskId = taskId,
                        )
                    }
                    if (normalizedProgress != null &&
                        (normalizedProgress == 0 || normalizedProgress == 100 || normalizedProgress >= lastLoggedProgress + 5)
                    ) {
                        lastLoggedProgress = normalizedProgress
                        appendDebugTrace(
                            taskId,
                            "Progress ${normalizedProgress}% speed=${progress.speed ?: "-"} eta=${progress.eta ?: "-"}",
                        )
                    }
                    downloadTaskStore.update(taskId) { task ->
                        task.copy(
                            status = DownloadStatus.RUNNING,
                            progressPercent = normalizedProgress ?: task.progressPercent,
                            speed = progress.speed ?: task.speed,
                            eta = progress.eta ?: task.eta,
                            downloadedStr = progress.downloadedStr ?: task.downloadedStr,
                            totalSizeStr = progress.totalStr ?: task.totalSizeStr,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                },
                onOutputLine = { line ->
                    logger.d("DownloadWorker/output", "taskId=$taskId line=$line")
                    appendDebugTrace(taskId, "yt-dlp: ${line.take(MAX_OUTPUT_TRACE_LINE_LENGTH)}")
                    parseOutputPath(line)?.let { parsed -> outputPath = parsed }
                },
            )
        }.getOrElse { throwable ->
            val failureMessage = buildFailureMessage(
                throwable = throwable,
                stderr = null,
            )
            logger.e("DownloadWorker", "yt-dlp command crashed", throwable)
            appendDebugTrace(taskId, "Task failed: $failureMessage")
            downloadTaskStore.update(taskId) { task ->
                task.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = failureMessage,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
            logger.e("DownloadWorker", "Task failed due to exception taskId=$taskId message=${throwable.message}")
            return Result.failure(workDataOf(WorkerKeys.ERROR_MESSAGE to failureMessage))
        }

        if (result.isSuccess) {
            logger.i("DownloadWorker", "Task completed taskId=$taskId outputPath=$outputPath")
            appendDebugTrace(taskId, "Task completed successfully")

            var publicPath: String? = null
            outputPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    fileUtils.copyToPublicDownloads(file)?.let { destPath ->
                        publicPath = destPath
                        appendDebugTrace(taskId, "Copied to public Downloads: $destPath")
                    }
                }
                appendDebugTrace(taskId, "Saved file: $path")
            }
            val finalPath = publicPath ?: outputPath

            downloadTaskStore.update(taskId) { task ->
                task.copy(
                    status = DownloadStatus.COMPLETED,
                    progressPercent = 100,
                    outputPath = finalPath,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
            return Result.success(workDataOf(WorkerKeys.OUTPUT_PATH to finalPath))
        }

        logger.w(
            "DownloadWorker",
            "Task failed taskId=$taskId exitCode=${result.exitCode} stderr=${result.stderr.take(1000)}",
        )
        val failureMessage = buildFailureMessage(
            throwable = null,
            stderr = result.stderr,
        )
        appendDebugTrace(taskId, "Task failed: $failureMessage")
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.FAILED,
                errorMessage = failureMessage,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
        return Result.failure(workDataOf(WorkerKeys.ERROR_MESSAGE to failureMessage))
    }

    private fun parseOutputPath(line: String): String? {
        val destinationPrefix = "Destination: "
        if (line.contains(destinationPrefix)) {
            return line.substringAfter(destinationPrefix).trim().removeSurrounding("\"")
                .also { logger.i("DownloadWorker", "Detected destination output path: $it") }
        }

        val mergePrefix = "Merging formats into \""
        if (line.contains(mergePrefix)) {
            return line.substringAfter(mergePrefix).substringBeforeLast("\"")
                .also { logger.i("DownloadWorker", "Detected merged output path: $it") }
        }

        return null
    }

    private fun ensureTaskRunning(taskId: String, url: String, title: String) {
        val existing = downloadTaskStore.getTask(taskId)
        if (existing != null) {
            downloadTaskStore.update(taskId) { task ->
                task.copy(
                    url = url,
                    title = title,
                    status = DownloadStatus.RUNNING,
                    errorMessage = null,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
            return
        }

        downloadTaskStore.upsert(
            DownloadTask(
                id = taskId,
                url = url,
                title = title,
                status = DownloadStatus.RUNNING,
                progressPercent = 0,
                debugTrace = "Task created by worker",
            ),
        )
    }

    private fun appendDebugTrace(taskId: String, message: String) {
        val normalized = message.trim().replace("\n", " ")
        if (normalized.isBlank()) return

        val entry = "${System.currentTimeMillis()}: $normalized"
        downloadTaskStore.update(taskId) { task ->
            val merged = if (task.debugTrace.isNullOrBlank()) {
                entry
            } else {
                task.debugTrace + "\n" + entry
            }
            task.copy(
                debugTrace = merged.takeLast(MAX_DEBUG_TRACE_CHARS),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    private fun buildFailureMessage(throwable: Throwable?, stderr: String?): String {
        val raw = sequenceOf(
            throwable?.message?.trim(),
            stderr?.trim(),
        ).filterNotNull()
            .firstOrNull { it.isNotBlank() }
            ?: "Download failed"

        val shortRaw = raw.take(MAX_ERROR_MESSAGE_CHARS)
        return if (isStorageDenied(throwable, shortRaw)) {
            "Storage denied: $shortRaw"
        } else {
            shortRaw
        }
    }

    private fun isStorageDenied(throwable: Throwable?, detail: String): Boolean {
        if (throwable is SecurityException) return true

        val normalized = detail.lowercase()
        return normalized.contains("permission denied") ||
            normalized.contains("eacces") ||
            normalized.contains("errno 13") ||
            normalized.contains("operation not permitted") ||
            normalized.contains("read-only file system")
    }

    private suspend fun startForegroundIfPossible(title: String, progress: Int, taskId: String): Boolean {
        if (!canPostNotifications()) {
            logger.w(
                "DownloadWorker",
                "Notification permission denied; continuing without foreground notification. taskId=$taskId",
            )
            return false
        }
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo(title = title, progress = progress))
            logger.i("DownloadWorker", "Foreground notification started for taskId=$taskId")
            return true
        } catch (error: Throwable) {
            logger.e(
                "DownloadWorker",
                "Failed to start foreground notification; continuing download taskId=$taskId",
                error,
            )
            return false
        }
    }

    private fun updateForegroundIfPossible(title: String, progress: Int, taskId: String) {
        if (!canPostNotifications()) return
        runCatching {
            setForegroundAsync(createForegroundInfo(title = title, progress = progress))
        }.onFailure { error ->
            logger.e(
                "DownloadWorker",
                "Failed to update foreground notification taskId=$taskId progress=$progress",
                error,
            )
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification: Notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Progress: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Downloader jobs",
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "download_worker_channel"
        const val MAX_DEBUG_TRACE_CHARS = 10_000
        const val MAX_ERROR_MESSAGE_CHARS = 800
        const val MAX_OUTPUT_TRACE_LINE_LENGTH = 240
    }
}
