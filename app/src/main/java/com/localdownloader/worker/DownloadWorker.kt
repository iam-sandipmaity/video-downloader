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
import com.localdownloader.downloader.CommandResult
import com.localdownloader.downloader.DownloadEngine
import com.localdownloader.ffmpeg.FfmpegExecutor
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import kotlinx.coroutines.CancellationException

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadEngine: DownloadEngine,
    private val downloadTaskStore: DownloadTaskStore,
    private val ffmpegExecutor: FfmpegExecutor,
    private val fileUtils: FileUtils,
    private val logger: Logger,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(WorkerKeys.TASK_ID) ?: id.toString()
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
            extractorArgs = inputData.getString(WorkerKeys.EXTRACTOR_ARGS).orEmpty().ifBlank { null },
            fallbackExtractorArgs = inputData.getString(WorkerKeys.FALLBACK_EXTRACTOR_ARGS).orEmpty().ifBlank { null },
            youtubeAuthEnabled = inputData.getBoolean(WorkerKeys.YOUTUBE_AUTH_ENABLED, false),
            youtubeCookiesPath = inputData.getString(WorkerKeys.YOUTUBE_COOKIES_PATH).orEmpty().ifBlank { null },
            youtubePoToken = inputData.getString(WorkerKeys.YOUTUBE_PO_TOKEN).orEmpty().ifBlank { null },
            youtubePoTokenClientHint = inputData.getString(WorkerKeys.YOUTUBE_PO_TOKEN_CLIENT_HINT) ?: "web.gvs",
            mergeOutputFormat = inputData.getString(WorkerKeys.MERGE_OUTPUT_FORMAT).orEmpty().ifBlank { null },
            isPlaylistEnabled = inputData.getBoolean(WorkerKeys.PLAYLIST_ENABLED, false),
            shouldDownloadSubtitles = inputData.getBoolean(WorkerKeys.DOWNLOAD_SUBTITLES, false),
            shouldEmbedMetadata = inputData.getBoolean(WorkerKeys.EMBED_METADATA, true),
            shouldEmbedThumbnail = inputData.getBoolean(WorkerKeys.EMBED_THUMBNAIL, false),
            shouldWriteThumbnail = inputData.getBoolean(WorkerKeys.WRITE_THUMBNAIL, false),
            extractAudio = inputData.getBoolean(WorkerKeys.EXTRACT_AUDIO, false),
            audioFormat = inputData.getString(WorkerKeys.AUDIO_FORMAT).orEmpty().ifBlank { null },
            audioBitrateKbps = inputData.getInt(WorkerKeys.AUDIO_BITRATE, -1).takeIf { it > 0 },
            playlistItemIndex = inputData.getInt(WorkerKeys.PLAYLIST_ITEM_INDEX, -1).takeIf { it > 0 },
            playlistFolderName = inputData.getString(WorkerKeys.PLAYLIST_FOLDER_NAME).orEmpty().ifBlank { null },
        )
        logger.d("DownloadWorker", "DownloadOptions: $options")
        val shouldContinuePlaylistQueue = options.isPlaylistEnabled && options.playlistItemIndex != null

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
        suspend fun runDownloadAttempt(attemptOptions: DownloadOptions): CommandResult {
            return downloadEngine.runDownload(
                options = attemptOptions,
                outputTemplate = attemptOptions.outputTemplate,
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
        }

        suspend fun runDownloadWithExtractorFallbacks(
            attemptOptions: DownloadOptions,
            stageLabel: String,
        ): CommandResult {
            var stageResult = runDownloadAttempt(attemptOptions)
            if (!stageResult.isSuccess && shouldRetryWithFallbackExtractor(attemptOptions, stageResult.stderr)) {
                appendDebugTrace(taskId, "$stageLabel retry: switching to analyzed YouTube extractor args")
                stageResult = runDownloadAttempt(
                    attemptOptions.copy(extractorArgs = attemptOptions.fallbackExtractorArgs),
                )
            }
            return stageResult
        }

        var result = try {
            if (shouldTryExplicitSplitDownload(options)) {
                appendDebugTrace(taskId, "Trying explicit video/audio split download before combined fallback")
                val splitResult = tryExplicitSplitDownload(
                    options = options,
                    originalOutputTemplate = outputTemplate,
                    taskId = taskId,
                    onRunDownload = ::runDownloadWithExtractorFallbacks,
                )
                if (splitResult.isSuccess) {
                    outputPath = splitResult.outputPath
                    CommandResult(exitCode = 0, stdout = "split download succeeded", stderr = "")
                } else {
                    appendDebugTrace(
                        taskId,
                        "Split download fallback failed: ${splitResult.errorMessage ?: "unknown error"}",
                    )
                    runDownloadAttempt(options)
                }
            } else {
                runDownloadAttempt(options)
            }
        } catch (cancelled: CancellationException) {
            appendDebugTrace(taskId, "Worker cancelled while download was in progress")
            throw cancelled
        } catch (throwable: Throwable) {
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
            return finishFailureResult(
                shouldContinuePlaylistQueue = shouldContinuePlaylistQueue,
                failureMessage = failureMessage,
            )
        }

        if (!result.isSuccess && shouldRetryWithFallbackExtractor(options, result.stderr)) {
            appendDebugTrace(taskId, "Retrying with analyzed YouTube extractor args after initial failure")
            val fallbackOptions = options.copy(extractorArgs = options.fallbackExtractorArgs)
            result = runDownloadAttempt(fallbackOptions)
        }

        if (!result.isSuccess && shouldRetryWithMp4Fallback(options, result.stderr)) {
            appendDebugTrace(taskId, "Retrying with mp4 fallback after YouTube format/access failure")
            val fallbackOptions = options.copy(
                formatId = buildMp4FallbackSelector(),
                mergeOutputFormat = "mp4",
            )
            result = runDownloadAttempt(fallbackOptions)
        }

        if (!result.isSuccess && shouldRetryWithYoutubeAuth(options, result.stderr)) {
            val authAttempts = youtubeAuthAttempts(options.youtubePoTokenClientHint)
            authAttempts.forEachIndexed { index, attempt ->
                if (result.isSuccess) return@forEachIndexed
                appendDebugTrace(
                    taskId,
                    "Retrying with YouTube auth mode ${index + 1}/${authAttempts.size}: ${attempt.label}",
                )
                result = runDownloadAttempt(
                    options.copy(
                        formatId = attempt.selector,
                        extractorArgs = buildYoutubeAuthExtractorArgs(
                            clientSpec = attempt.clientSpec.orEmpty(),
                            poToken = options.youtubePoToken,
                        ),
                        fallbackExtractorArgs = null,
                    ),
                )
            }
        }

        if (!result.isSuccess && shouldRetryWithYoutubeSafeMode(options, result.stderr)) {
            val safeModeAttempts = youtubeSafeModeAttempts()
            safeModeAttempts.forEachIndexed { index, attempt ->
                if (result.isSuccess) return@forEachIndexed
                appendDebugTrace(
                    taskId,
                    "Retrying with YouTube safe mode ${index + 1}/${safeModeAttempts.size}: ${attempt.label}",
                )
                result = runDownloadAttempt(
                    options.copy(
                        formatId = attempt.selector,
                        extractorArgs = null,
                        mergeOutputFormat = null,
                    ),
                )
            }
        }

        if (!result.isSuccess && shouldRetryTransientFailure(result.stderr) && runAttemptCount < MAX_TRANSIENT_RETRY_ATTEMPTS) {
            appendDebugTrace(
                taskId,
                "Transient failure detected; scheduling WorkManager retry ${runAttemptCount + 1}/$MAX_TRANSIENT_RETRY_ATTEMPTS",
            )
            return Result.retry()
        }

        if (result.isSuccess) {
            logger.i("DownloadWorker", "Task completed taskId=$taskId outputPath=$outputPath")
            appendDebugTrace(taskId, "Task completed successfully")

            var publicPath: String? = null
            outputPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    fileUtils.copyToPublicDownloads(
                        sourceFile = file,
                        playlistFolderName = options.playlistFolderName,
                    )?.let { destPath ->
                        publicPath = destPath
                        appendDebugTrace(taskId, "Copied to public Downloads: $destPath")
                        if (destPath != file.absolutePath && file.delete()) {
                            appendDebugTrace(taskId, "Removed private staging copy after public export")
                        }
                    }
                }
                appendDebugTrace(taskId, "Saved file: $path")
            }
            val finalPath = publicPath ?: outputPath
            val finalSizeLabel = finalPath
                ?.let(::File)
                ?.takeIf { it.exists() }
                ?.length()
                ?.toReadableSize()

            downloadTaskStore.update(taskId) { task ->
                task.copy(
                    status = DownloadStatus.COMPLETED,
                    progressPercent = 100,
                    outputPath = finalPath,
                    downloadedStr = finalSizeLabel ?: task.downloadedStr.takeMeaningfulSizeLabel(),
                    totalSizeStr = finalSizeLabel ?: task.totalSizeStr.takeMeaningfulSizeLabel(),
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
        return finishFailureResult(
            shouldContinuePlaylistQueue = shouldContinuePlaylistQueue,
            failureMessage = failureMessage,
        )
    }

    private fun shouldRetryWithMp4Fallback(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio) return false
        val lower = stderr.lowercase()
        return lower.contains("http error 403") ||
            lower.contains("403: forbidden") ||
            lower.contains("requested format is not available")
    }

    private fun shouldRetryWithFallbackExtractor(options: DownloadOptions, stderr: String): Boolean {
        val fallbackArgs = options.fallbackExtractorArgs?.trim().orEmpty()
        if (fallbackArgs.isBlank() || fallbackArgs == options.extractorArgs) return false

        val lower = stderr.lowercase()
        return lower.contains("http error 403") ||
            lower.contains("403: forbidden") ||
            lower.contains("requested format is not available")
    }

    private fun buildMp4FallbackSelector(): String {
        return "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best"
    }

    private fun shouldRetryWithYoutubeSafeMode(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false

        val lower = stderr.lowercase()
        return lower.contains("http error 403") ||
            lower.contains("403: forbidden") ||
            lower.contains("requested format is not available")
    }

    private fun youtubeSafeModeAttempts(): List<SafeModeAttempt> {
        return listOf(
            SafeModeAttempt(
                label = "classic mp4 progressive",
                selector = "18/22/best[height<=360][vcodec!=none][acodec!=none][ext=mp4]/best[height<=360][vcodec!=none][acodec!=none]",
            ),
            SafeModeAttempt(
                label = "best muxed under 480p",
                selector = "best[height<=480][vcodec!=none][acodec!=none]/best[height<=360]/best[vcodec!=none][acodec!=none]",
            ),
        )
    }

    private fun shouldRetryWithYoutubeAuth(options: DownloadOptions, stderr: String): Boolean {
        if (!options.youtubeAuthEnabled) return false
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false
        if (options.youtubeCookiesPath.isNullOrBlank()) return false
        if (!File(options.youtubeCookiesPath).exists()) return false
        if (options.youtubePoToken.isNullOrBlank()) return false

        val lower = stderr.lowercase()
        return lower.contains("http error 403") ||
            lower.contains("403: forbidden") ||
            lower.contains("requested format is not available")
    }

    private fun youtubeAuthAttempts(preferredHint: String): List<SafeModeAttempt> {
        val attempts = listOf(
            SafeModeAttempt(
                label = "mweb authenticated best",
                selector = "bestvideo*+bestaudio/best",
                clientSpec = "default,mweb",
            ),
            SafeModeAttempt(
                label = "web authenticated best",
                selector = "bestvideo*+bestaudio/best",
                clientSpec = "default,web",
            ),
            SafeModeAttempt(
                label = "mweb authenticated muxed safe mode",
                selector = "best[height<=480][vcodec!=none][acodec!=none]/best[height<=360]/best",
                clientSpec = "default,mweb",
            ),
        )
        val normalizedHint = preferredHint.trim().lowercase()
        return attempts.sortedByDescending { attempt ->
            when {
                normalizedHint == "mweb.gvs" && attempt.clientSpec?.contains("mweb") == true -> 1
                normalizedHint == "web.gvs" && attempt.clientSpec?.contains("web") == true && attempt.clientSpec?.contains("mweb") != true -> 1
                else -> 0
            }
        }
    }

    private fun buildYoutubeAuthExtractorArgs(clientSpec: String, poToken: String?): String {
        val trimmedToken = poToken.orEmpty().trim()
        val poTokenPrefix = when {
            clientSpec.contains("mweb") -> "mweb.gvs+"
            clientSpec.contains("web") -> "web.gvs+"
            else -> "mweb.gvs+"
        }
        return "youtube:player_client=$clientSpec;po_token=$poTokenPrefix$trimmedToken"
    }

    private fun shouldTryExplicitSplitDownload(options: DownloadOptions): Boolean {
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false
        if (options.formatId.contains("/")) return false
        return options.formatId.contains("+")
    }

    private fun isYoutubeUrl(url: String): Boolean {
        val normalized = url.lowercase()
        return normalized.contains("youtube.com") || normalized.contains("youtu.be")
    }

    private suspend fun tryExplicitSplitDownload(
        options: DownloadOptions,
        originalOutputTemplate: String,
        taskId: String,
        onRunDownload: suspend (DownloadOptions, String) -> CommandResult,
    ): SplitDownloadResult {
        val selectors = splitFormatSelector(options.formatId) ?: return SplitDownloadResult.failure("Format is not splittable")
        val videoTemplate = buildSplitOutputTemplate(originalOutputTemplate, ".video")
        val audioTemplate = buildSplitOutputTemplate(originalOutputTemplate, ".audio")

        appendDebugTrace(taskId, "Split video selector=${selectors.videoSelector}")
        val videoPath = downloadSinglePart(
            taskId = taskId,
            stageLabel = "Video stream",
            outputTemplate = videoTemplate,
            options = options.copy(
                formatId = selectors.videoSelector,
                mergeOutputFormat = null,
                shouldDownloadSubtitles = false,
                shouldEmbedMetadata = false,
                shouldEmbedThumbnail = false,
                shouldWriteThumbnail = false,
            ),
            onRunDownload = onRunDownload,
        ) ?: return SplitDownloadResult.failure("Video stream download failed")

        appendDebugTrace(taskId, "Split audio selector=${selectors.audioSelector}")
        val audioPath = downloadSinglePart(
            taskId = taskId,
            stageLabel = "Audio stream",
            outputTemplate = audioTemplate,
            options = options.copy(
                formatId = selectors.audioSelector,
                mergeOutputFormat = null,
                shouldDownloadSubtitles = false,
                shouldEmbedMetadata = false,
                shouldEmbedThumbnail = false,
                shouldWriteThumbnail = false,
            ),
            onRunDownload = onRunDownload,
        ) ?: return SplitDownloadResult.failure("Audio stream download failed")

        val mergedOutputPath = buildMergedOutputPath(
            videoPath = videoPath,
            preferredExtension = options.mergeOutputFormat ?: File(videoPath).extension.ifBlank { "mp4" },
        )
        appendDebugTrace(taskId, "Merging split streams into $mergedOutputPath")

        val mergeResult = ffmpegExecutor.execute(
            args = buildList {
                add("-i")
                add(videoPath)
                add("-i")
                add(audioPath)
                add("-map")
                add("0:v:0")
                add("-map")
                add("1:a:0")
                add("-c")
                add("copy")
                if (mergedOutputPath.endsWith(".mp4", ignoreCase = true) || mergedOutputPath.endsWith(".mov", ignoreCase = true)) {
                    add("-movflags")
                    add("+faststart")
                }
                add("-y")
                add(mergedOutputPath)
            },
            onStderrLine = { line ->
                appendDebugTrace(taskId, "ffmpeg: ${line.take(MAX_OUTPUT_TRACE_LINE_LENGTH)}")
            },
        )

        if (!mergeResult.isSuccess) {
            safeDelete(videoPath)
            safeDelete(audioPath)
            return SplitDownloadResult.failure(mergeResult.stderr.ifBlank { "FFmpeg merge failed" })
        }

        safeDelete(videoPath)
        safeDelete(audioPath)
        return SplitDownloadResult.success(mergedOutputPath)
    }

    private suspend fun downloadSinglePart(
        taskId: String,
        stageLabel: String,
        outputTemplate: String,
        options: DownloadOptions,
        onRunDownload: suspend (DownloadOptions, String) -> CommandResult,
    ): String? {
        appendDebugTrace(taskId, "$stageLabel start format=${options.formatId}")
        val result = onRunDownload(options.copy(outputTemplate = outputTemplate), stageLabel)
        if (!result.isSuccess) {
            appendDebugTrace(
                taskId,
                "$stageLabel failed: ${result.stderr.take(MAX_OUTPUT_TRACE_LINE_LENGTH).ifBlank { "unknown error" }}",
            )
            return null
        }

        val path = inferDownloadedPath(outputTemplate) ?: return null
        appendDebugTrace(taskId, "$stageLabel saved to $path")
        return path
    }

    private fun inferDownloadedPath(outputTemplate: String): String? {
        val templateFile = File(outputTemplate)
        val dir = templateFile.parentFile ?: return null
        val stem = templateFile.name.substringBefore(".%(ext)s")
        val matches = dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(stem) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        return matches.firstOrNull()?.absolutePath
    }

    private fun buildSplitOutputTemplate(outputTemplate: String, suffix: String): String {
        val extToken = ".%(ext)s"
        return if (outputTemplate.contains(extToken)) {
            outputTemplate.replace(extToken, "$suffix$extToken")
        } else {
            "$outputTemplate$suffix.%(ext)s"
        }
    }

    private fun buildMergedOutputPath(videoPath: String, preferredExtension: String): String {
        val videoFile = File(videoPath)
        val ext = preferredExtension.ifBlank { videoFile.extension.ifBlank { "mp4" } }
        val baseName = videoFile.nameWithoutExtension.removeSuffix(".video")
        return File(videoFile.parentFile, "$baseName.$ext").absolutePath
    }

    private fun splitFormatSelector(selector: String): SplitSelectors? {
        val separatorIndex = selector.indexOf('+')
        if (separatorIndex <= 0 || separatorIndex >= selector.lastIndex) return null
        val videoSelector = selector.substring(0, separatorIndex).trim()
        val audioSelector = selector.substring(separatorIndex + 1).trim()
        if (videoSelector.isBlank() || audioSelector.isBlank()) return null
        return SplitSelectors(
            videoSelector = videoSelector,
            audioSelector = audioSelector,
        )
    }

    private fun safeDelete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
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
                activeWorkId = id.toString(),
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
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo(title = title, progress = progress))
            logger.i("DownloadWorker", "Foreground notification started for taskId=$taskId")
            return true
        } catch (error: Throwable) {
            logger.e(
                "DownloadWorker",
                "Failed to start foreground notification; continuing download taskId=$taskId permissionGranted=${canPostNotifications()}",
                error,
            )
            return false
        }
    }

    private fun updateForegroundIfPossible(title: String, progress: Int, taskId: String) {
        runCatching {
            setForegroundAsync(createForegroundInfo(title = title, progress = progress))
        }.onFailure { error ->
            logger.e(
                "DownloadWorker",
                "Failed to update foreground notification taskId=$taskId progress=$progress permissionGranted=${canPostNotifications()}",
                error,
            )
        }
    }

    private fun shouldRetryTransientFailure(stderr: String): Boolean {
        val lower = stderr.lowercase()
        return lower.contains("unable to download api json") ||
            lower.contains("no address associated with hostname") ||
            lower.contains("temporary failure in name resolution") ||
            lower.contains("failed to resolve") ||
            lower.contains("network is unreachable") ||
            lower.contains("connection reset") ||
            lower.contains("connection aborted") ||
            lower.contains("connection timed out") ||
            lower.contains("read timed out") ||
            lower.contains("timed out") ||
            lower.contains("i/o timeout") ||
            lower.contains("transport endpoint is not connected")
    }

    private fun finishFailureResult(
        shouldContinuePlaylistQueue: Boolean,
        failureMessage: String,
    ): Result {
        val outputData = workDataOf(
            WorkerKeys.ERROR_MESSAGE to failureMessage,
            WorkerKeys.TERMINAL_STATUS to DownloadStatus.FAILED.name,
        )
        if (!shouldContinuePlaylistQueue) {
            return Result.failure(outputData)
        }
        appendDebugTrace(
            id.toString(),
            "Playlist item failed but worker is returning success so the remaining queue can continue",
        )
        return Result.success(outputData)
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
        const val MAX_TRANSIENT_RETRY_ATTEMPTS = 3
    }

    private fun Long.toReadableSize(): String {
        if (this <= 0L) return ""
        val kib = 1024.0
        val mib = kib * 1024.0
        val gib = mib * 1024.0
        return when {
            this >= gib -> String.format("%.1f GB", this / gib)
            this >= mib -> String.format("%.1f MB", this / mib)
            this >= kib -> String.format("%.1f KB", this / kib)
            else -> "$this B"
        }
    }

    private fun String?.takeMeaningfulSizeLabel(): String? {
        val normalized = this?.trim().orEmpty()
        if (normalized.isBlank()) return null
        if (normalized.equals("na", ignoreCase = true)) return null
        if (normalized.equals("n/a", ignoreCase = true)) return null
        if (normalized.equals("unknown", ignoreCase = true)) return null
        return normalized
    }

    private data class SplitSelectors(
        val videoSelector: String,
        val audioSelector: String,
    )

    private data class SplitDownloadResult(
        val isSuccess: Boolean,
        val outputPath: String?,
        val errorMessage: String?,
    ) {
        companion object {
            fun success(outputPath: String) = SplitDownloadResult(
                isSuccess = true,
                outputPath = outputPath,
                errorMessage = null,
            )

            fun failure(errorMessage: String) = SplitDownloadResult(
                isSuccess = false,
                outputPath = null,
                errorMessage = errorMessage,
            )
        }
    }

    private data class SafeModeAttempt(
        val label: String,
        val selector: String,
        val clientSpec: String? = null,
    )
}
