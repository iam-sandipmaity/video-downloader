package com.localdownloader.worker

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.os.Build
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
import com.localdownloader.downloader.YoutubeRequestPlanner
import com.localdownloader.ffmpeg.FfmpegExecutor
import com.localdownloader.notifications.AppNotifications
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
            loadInfoJsonPath = inputData.getString(WorkerKeys.LOAD_INFO_JSON_PATH).orEmpty().ifBlank { null },
            userAgentHeader = inputData.getString(WorkerKeys.USER_AGENT_HEADER).orEmpty().ifBlank { null },
            youtubeAuthEnabled = inputData.getBoolean(WorkerKeys.YOUTUBE_AUTH_ENABLED, false),
            youtubeCookiesPath = inputData.getString(WorkerKeys.YOUTUBE_COOKIES_PATH).orEmpty().ifBlank { null },
            youtubePoToken = inputData.getString(WorkerKeys.YOUTUBE_PO_TOKEN).orEmpty().ifBlank { null },
            youtubePoTokenClientHint = inputData.getString(WorkerKeys.YOUTUBE_PO_TOKEN_CLIENT_HINT) ?: "web.gvs",
            youtubeDataSyncId = inputData.getString(WorkerKeys.YOUTUBE_DATA_SYNC_ID).orEmpty().ifBlank { null },
            mergeOutputFormat = inputData.getString(WorkerKeys.MERGE_OUTPUT_FORMAT).orEmpty().ifBlank { null },
            preferredVideoHeight = inputData.getInt(WorkerKeys.PREFERRED_VIDEO_HEIGHT, -1).takeIf { it > 0 },
            downloadVideoOnly = inputData.getBoolean(WorkerKeys.DOWNLOAD_VIDEO_ONLY, false),
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
        cleanupOrphanedManagedArtifacts(outputTemplate = outputTemplate, taskId = taskId)

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
        var lastAttemptOptions = options
        var shouldGenerateThumbnailFallback = false
        suspend fun runDownloadAttempt(attemptOptions: DownloadOptions): CommandResult {
            lastAttemptOptions = attemptOptions
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
                            downloadedStr = progress.downloadedStr,
                            totalStr = progress.totalStr,
                            speed = progress.speed,
                            eta = progress.eta,
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
                    attemptOptions.copy(
                        extractorArgs = attemptOptions.fallbackExtractorArgs,
                        loadInfoJsonPath = null,
                    ),
                )
            }
            return stageResult
        }

        var result = try {
            runDownloadAttempt(options)
        } catch (cancelled: CancellationException) {
            appendDebugTrace(taskId, "Worker cancelled while download was in progress")
            throw cancelled
        } catch (throwable: Throwable) {
            if (shouldKeepPausedState(taskId = taskId, throwable = throwable, stderr = null)) {
                appendDebugTrace(taskId, "Pause request intercepted worker shutdown before failure handling")
                return finishPausedResult()
            }
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
            showFailureNotification(
                taskId = taskId,
                title = runningTitle,
                message = failureMessage,
            )
            logger.e("DownloadWorker", "Task failed due to exception taskId=$taskId message=${throwable.message}")
            return finishFailureResult(
                shouldContinuePlaylistQueue = shouldContinuePlaylistQueue,
                failureMessage = failureMessage,
            )
        }

        if (!result.isSuccess && shouldRetryWithFallbackExtractor(options, result.stderr)) {
            appendDebugTrace(taskId, "Retrying with analyzed YouTube extractor args after initial failure")
            val fallbackOptions = options.copy(
                extractorArgs = options.fallbackExtractorArgs,
                loadInfoJsonPath = null,
            )
            result = runDownloadAttempt(fallbackOptions)
        }

        if (!result.isSuccess && shouldRetryClosedStreamFailure(result.stderr)) {
            appendDebugTrace(taskId, "Retrying once after transient closed-stream yt-dlp failure")
            result = runDownloadAttempt(
                lastAttemptOptions.copy(loadInfoJsonPath = null),
            )
        }

        if (!result.isSuccess && shouldTryExplicitSplitDownload(options, result.stderr)) {
            appendDebugTrace(taskId, "Retrying with explicit split-stream recovery")
            val splitResult = tryExplicitSplitDownload(
                options = options.copy(loadInfoJsonPath = null),
                originalOutputTemplate = outputTemplate,
                taskId = taskId,
                onRunDownload = { attemptOptions, stageLabel ->
                    runDownloadWithExtractorFallbacks(
                        attemptOptions.copy(loadInfoJsonPath = null),
                        stageLabel,
                    )
                },
            )
            if (splitResult.isSuccess) {
                outputPath = splitResult.outputPath
                result = CommandResult(exitCode = 0, stdout = "", stderr = "")
            } else {
                appendDebugTrace(
                    taskId,
                    "Explicit split recovery failed: ${splitResult.errorMessage.orEmpty().take(MAX_OUTPUT_TRACE_LINE_LENGTH).ifBlank { "unknown error" }}",
                )
            }
        }

        if (!result.isSuccess && shouldRetryWithYoutubeSelectorRecovery(options, result.stderr)) {
            val recoveryAttempts = youtubeSameSelectorRecoveryAttempts(options)
            recoveryAttempts.forEachIndexed { index, attempt ->
                if (result.isSuccess) return@forEachIndexed
                appendDebugTrace(
                    taskId,
                    "Retrying with fresh YouTube selector ${index + 1}/${recoveryAttempts.size}: ${attempt.label}",
                )
                result = runDownloadAttempt(
                    options.copy(
                        formatId = attempt.selector,
                        extractorArgs = attempt.extractorArgs,
                        fallbackExtractorArgs = null,
                        loadInfoJsonPath = null,
                    ),
                )
            }
        }

        if (!result.isSuccess && shouldRetryWithYoutubeAuth(options, result.stderr)) {
            val authAttempts = youtubeAuthenticatedSelectorAttempts(
                requestedSelector = options.formatId,
                poToken = options.youtubePoToken,
                preferredHint = options.youtubePoTokenClientHint,
                dataSyncId = options.youtubeDataSyncId,
            )
            val hasPoToken = !options.youtubePoToken.isNullOrBlank()
            authAttempts.forEachIndexed { index, attempt ->
                if (result.isSuccess) return@forEachIndexed
                appendDebugTrace(
                    taskId,
                    if (hasPoToken) {
                        "Retrying with YouTube compatibility mode ${index + 1}/${authAttempts.size}: ${attempt.label}"
                    } else {
                        "Retrying with cookie-backed YouTube mode ${index + 1}/${authAttempts.size}: ${attempt.label}"
                    },
                )
                result = runDownloadAttempt(
                    options.copy(
                        formatId = attempt.selector,
                        extractorArgs = attempt.extractorArgs,
                        fallbackExtractorArgs = null,
                        loadInfoJsonPath = null,
                    ),
                )
            }
        }

        if (!result.isSuccess && shouldRetryWithYoutubeAdaptiveFallback(options, result.stderr)) {
            val adaptiveAttempts = youtubeAdaptiveRecoveryAttempts(options)
            adaptiveAttempts.forEachIndexed { index, attempt ->
                if (result.isSuccess) return@forEachIndexed
                appendDebugTrace(
                    taskId,
                    "Retrying with adaptive YouTube recovery ${index + 1}/${adaptiveAttempts.size}: ${attempt.label}",
                )
                result = runDownloadAttempt(
                    options.copy(
                        formatId = attempt.selector,
                        extractorArgs = attempt.extractorArgs,
                        fallbackExtractorArgs = null,
                        loadInfoJsonPath = null,
                    ),
                )
            }
        }

        if (!result.isSuccess && shouldRetryWithMp4Fallback(options, result.stderr)) {
            if (hasUsableCookies(options) && isYoutubeUrl(options.url) && !options.downloadVideoOnly) {
                val progressiveAttempts = youtubeProgressiveRecoveryAttempts(
                    poToken = options.youtubePoToken,
                    preferredHint = options.youtubePoTokenClientHint,
                    dataSyncId = options.youtubeDataSyncId,
                )
                progressiveAttempts.forEachIndexed { index, attempt ->
                    if (result.isSuccess) return@forEachIndexed
                    appendDebugTrace(
                        taskId,
                        "Retrying with cookie-backed progressive fallback ${index + 1}/${progressiveAttempts.size}: ${attempt.label}",
                    )
                    result = runDownloadAttempt(
                        options.copy(
                            formatId = attempt.selector,
                            extractorArgs = attempt.extractorArgs,
                            fallbackExtractorArgs = null,
                            loadInfoJsonPath = null,
                            mergeOutputFormat = "mp4",
                        ),
                    )
                }
            } else {
                appendDebugTrace(taskId, "Retrying with mp4 fallback after format/access failure")
                val fallbackOptions = options.copy(
                    formatId = buildMp4FallbackSelector(downloadVideoOnly = options.downloadVideoOnly),
                    loadInfoJsonPath = null,
                    mergeOutputFormat = if (options.downloadVideoOnly) null else "mp4",
                )
                result = runDownloadAttempt(fallbackOptions)
            }
        }

        if (!result.isSuccess && shouldRetryWithYoutubeSafeMode(options, result.stderr)) {
            val safeModeAttempts = youtubeSafeModeAttempts(downloadVideoOnly = options.downloadVideoOnly)
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
                        loadInfoJsonPath = null,
                        mergeOutputFormat = null,
                    ),
                )
            }
        }

        if (!result.isSuccess) {
            val recoveredOutputPath = recoverPrimaryMediaAfterThumbnailFailure(
                stderr = result.stderr,
                currentOutputPath = outputPath,
                outputTemplate = outputTemplate,
            )
            if (recoveredOutputPath != null) {
                outputPath = recoveredOutputPath
                shouldGenerateThumbnailFallback = options.shouldWriteThumbnail
                if (!options.shouldWriteThumbnail) {
                    cleanupThumbnailSidecars(recoveredOutputPath)
                }
                appendDebugTrace(
                    taskId,
                    "Thumbnail postprocessing failed after media was saved; completing without embedded thumbnail",
                )
                result = CommandResult(
                    exitCode = 0,
                    stdout = result.stdout,
                    stderr = result.stderr,
                )
            }
        }

        if (!result.isSuccess) {
            val recoveredOutputPath = recoverPrimaryMediaAfterPostprocessingFailure(
                taskId = taskId,
                stderr = result.stderr,
                currentOutputPath = outputPath,
                outputTemplate = outputTemplate,
                preferredExtension = options.mergeOutputFormat,
            )
            if (recoveredOutputPath != null) {
                outputPath = recoveredOutputPath
                if (options.shouldWriteThumbnail) {
                    shouldGenerateThumbnailFallback = true
                } else if (!options.shouldEmbedThumbnail) {
                    cleanupThumbnailSidecars(recoveredOutputPath)
                }
                appendDebugTrace(
                    taskId,
                    "Recovered media after yt-dlp postprocessing failure; finishing without optional postprocess extras",
                )
                result = CommandResult(
                    exitCode = 0,
                    stdout = result.stdout,
                    stderr = result.stderr,
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
            if (outputPath == null) {
                outputPath = inferDownloadedPath(outputTemplate)
            }
            if (outputPath != null && shouldGenerateThumbnailFallback) {
                generateVideoFrameThumbnail(
                    primaryPath = outputPath!!,
                    taskId = taskId,
                )
            }
            if (outputPath != null && options.shouldDownloadSubtitles) {
                val subtitleTemplate = buildOutputTemplateForExistingFile(outputPath!!)
                appendDebugTrace(taskId, "Downloading subtitle sidecars for completed media")
                val subtitleResult = downloadEngine.runSubtitleDownload(
                    options = options.copy(
                        outputTemplate = subtitleTemplate,
                        loadInfoJsonPath = null,
                    ),
                    outputTemplate = subtitleTemplate,
                    onOutputLine = { line ->
                        appendDebugTrace(taskId, "yt-dlp subtitles: ${line.take(MAX_OUTPUT_TRACE_LINE_LENGTH)}")
                    },
                )
                if (!subtitleResult.isSuccess) {
                    appendDebugTrace(
                        taskId,
                        "Subtitle sidecar download skipped after failure: ${subtitleResult.stderr.take(MAX_OUTPUT_TRACE_LINE_LENGTH).ifBlank { "unknown error" }}",
                    )
                }
            }
            logger.i("DownloadWorker", "Task completed taskId=$taskId outputPath=$outputPath")
            appendDebugTrace(taskId, "Task completed successfully")

            val exportedBundle = outputPath?.let { path ->
                exportManagedMediaBundle(
                    primaryPath = path,
                    playlistFolderName = options.playlistFolderName,
                    taskId = taskId,
                )
            } ?: ExportedMediaBundle(
                primaryPath = outputPath,
                subtitlePaths = emptyList(),
            )
            val finalPath = exportedBundle.primaryPath
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
                    subtitlePaths = exportedBundle.subtitlePaths,
                    downloadedStr = finalSizeLabel ?: task.downloadedStr.takeMeaningfulSizeLabel(),
                    totalSizeStr = finalSizeLabel ?: task.totalSizeStr.takeMeaningfulSizeLabel(),
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
            showCompletionNotification(
                taskId = taskId,
                title = runningTitle,
                outputPath = finalPath,
                sizeLabel = finalSizeLabel,
            )
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
        if (shouldKeepPausedState(taskId = taskId, throwable = null, stderr = result.stderr)) {
            appendDebugTrace(taskId, "Pause request preserved paused state after command interruption")
            return finishPausedResult()
        }
        appendDebugTrace(taskId, "Task failed: $failureMessage")
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.FAILED,
                errorMessage = failureMessage,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
        showFailureNotification(
            taskId = taskId,
            title = runningTitle,
            message = failureMessage,
        )
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
            lower.contains("requested format is not available") ||
            isFragmentAccessFailure(lower)
    }

    private fun shouldRetryWithYoutubeSelectorRecovery(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false
        return isYoutubeFormatAccessFailure(stderr)
    }

    private fun shouldRetryWithFallbackExtractor(options: DownloadOptions, stderr: String): Boolean {
        val fallbackArgs = options.fallbackExtractorArgs?.trim().orEmpty()
        if (fallbackArgs.isBlank() || fallbackArgs == options.extractorArgs) return false

        val lower = stderr.lowercase()
        return lower.contains("http error 403") ||
            lower.contains("403: forbidden") ||
            lower.contains("requested format is not available")
    }

    private fun buildMp4FallbackSelector(downloadVideoOnly: Boolean): String {
        return if (downloadVideoOnly) {
            "bestvideo[height<=480][ext=mp4]/bestvideo[height<=360][ext=mp4]/bestvideo[height<=480]/bestvideo[height<=360]/bestvideo"
        } else {
            "22/18/best[ext=mp4][vcodec!=none][acodec!=none]/best[height<=480][vcodec!=none][acodec!=none]/best"
        }
    }

    private fun shouldRetryWithYoutubeAdaptiveFallback(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false
        return isYoutubeFormatAccessFailure(stderr)
    }

    private fun shouldRetryWithYoutubeSafeMode(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false

        return isYoutubeFormatAccessFailure(stderr)
    }

    private fun youtubeSafeModeAttempts(downloadVideoOnly: Boolean): List<SafeModeAttempt> {
        return if (downloadVideoOnly) {
            listOf(
                SafeModeAttempt(
                    label = "best video under 720p mp4",
                    selector = "bestvideo[height<=720][ext=mp4]/bestvideo[height<=480][ext=mp4]/bestvideo[height<=360]",
                ),
                SafeModeAttempt(
                    label = "best video under 480p",
                    selector = "bestvideo[height<=480]/bestvideo[height<=360]/bestvideo",
                ),
            )
        } else {
            listOf(
                SafeModeAttempt(
                    label = "classic mp4 progressive",
                    selector = "22/18/best[height<=720][vcodec!=none][acodec!=none][ext=mp4]/best[height<=360][vcodec!=none][acodec!=none]",
                ),
                SafeModeAttempt(
                    label = "best muxed under 480p",
                    selector = "best[height<=480][vcodec!=none][acodec!=none]/best[height<=360]/best[vcodec!=none][acodec!=none]",
                ),
            )
        }
    }

    private fun shouldRetryWithYoutubeAuth(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio) return false
        if (!isYoutubeUrl(options.url)) return false
        if (!hasUsableCookies(options)) return false

        return isYoutubeFormatAccessFailure(stderr)
    }

    private fun youtubeAuthenticatedSelectorAttempts(
        requestedSelector: String,
        poToken: String?,
        preferredHint: String,
        dataSyncId: String?,
    ): List<SafeModeAttempt> {
        return YoutubeRequestPlanner.authenticatedSameSelectorAttempts(
            requestedSelector = requestedSelector,
            poToken = poToken,
            preferredHint = preferredHint,
            dataSyncId = dataSyncId,
        ).map { attempt ->
            SafeModeAttempt(
                label = attempt.label,
                selector = attempt.selector,
                extractorArgs = attempt.extractorArgs,
            )
        }
    }

    private fun youtubeSameSelectorRecoveryAttempts(options: DownloadOptions): List<SafeModeAttempt> {
        val attempts = buildList {
            if (!options.loadInfoJsonPath.isNullOrBlank()) {
                add(
                    SafeModeAttempt(
                        label = "same selector without cached info-json",
                        selector = options.formatId,
                        extractorArgs = options.extractorArgs,
                    ),
                )
            }
            YoutubeRequestPlanner.recoveryCandidates(
                selectedExtractorArgs = options.extractorArgs,
                cookiesAvailable = hasUsableCookies(options),
            ).forEach { extractorArgs ->
                add(
                    SafeModeAttempt(
                        label = "same selector via alternate extractor",
                        selector = options.formatId,
                        extractorArgs = extractorArgs,
                    ),
                )
            }
        }
        return dedupeRecoveryAttempts(attempts)
    }

    private fun youtubeAdaptiveRecoveryAttempts(options: DownloadOptions): List<SafeModeAttempt> {
        val adaptiveSelector = YoutubeRequestPlanner.buildAdaptiveSelector(
            maxHeight = options.preferredVideoHeight,
            container = options.mergeOutputFormat,
            videoOnly = options.downloadVideoOnly,
        )
        val attempts = buildList {
            if (adaptiveSelector != options.formatId || !options.loadInfoJsonPath.isNullOrBlank()) {
                add(
                    SafeModeAttempt(
                        label = "adaptive selector preserving requested quality",
                        selector = adaptiveSelector,
                        extractorArgs = options.extractorArgs,
                    ),
                )
            }
            YoutubeRequestPlanner.recoveryCandidates(
                selectedExtractorArgs = options.extractorArgs,
                cookiesAvailable = hasUsableCookies(options),
            ).forEach { extractorArgs ->
                add(
                    SafeModeAttempt(
                        label = "adaptive selector via alternate extractor",
                        selector = adaptiveSelector,
                        extractorArgs = extractorArgs,
                    ),
                )
            }
            if (hasUsableCookies(options)) {
                addAll(
                    youtubeAuthenticatedSelectorAttempts(
                        requestedSelector = adaptiveSelector,
                        poToken = options.youtubePoToken,
                        preferredHint = options.youtubePoTokenClientHint,
                        dataSyncId = options.youtubeDataSyncId,
                    ).map { attempt ->
                        attempt.copy(label = "authenticated ${attempt.label}")
                    },
                )
            }
        }
        return dedupeRecoveryAttempts(attempts)
    }

    private fun youtubeProgressiveRecoveryAttempts(
        poToken: String?,
        preferredHint: String,
        dataSyncId: String?,
    ): List<SafeModeAttempt> {
        return YoutubeRequestPlanner.progressiveRecoveryAttempts(
            poToken = poToken,
            preferredHint = preferredHint,
            dataSyncId = dataSyncId,
        ).map { attempt ->
            SafeModeAttempt(
                label = attempt.label,
                selector = attempt.selector,
                extractorArgs = attempt.extractorArgs,
            )
        }
    }

    private fun hasUsableCookies(options: DownloadOptions): Boolean {
        val cookiesPath = options.youtubeCookiesPath ?: return false
        return File(cookiesPath).exists()
    }

    private fun shouldTryExplicitSplitDownload(options: DownloadOptions, stderr: String): Boolean {
        if (options.extractAudio || options.downloadVideoOnly) return false
        if (splitFormatSelector(options.formatId) == null) return false
        return isYoutubeFormatAccessFailure(stderr)
    }

    private fun dedupeRecoveryAttempts(attempts: List<SafeModeAttempt>): List<SafeModeAttempt> {
        return attempts.distinctBy { attempt ->
            attempt.selector + "||" + attempt.extractorArgs.orEmpty()
        }
    }

    private fun isYoutubeFormatAccessFailure(stderr: String): Boolean {
        val lower = stderr.lowercase()
        return lower.contains("http error 403") ||
            lower.contains("403: forbidden") ||
            lower.contains("requested format is not available") ||
            lower.contains("sign in to confirm") ||
            lower.contains("use --cookies") ||
            lower.contains("precondition check failed") ||
            isFragmentAccessFailure(lower)
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

        val mergeResult = mergeMediaWithFallback(
            taskId = taskId,
            videoPath = videoPath,
            audioPath = audioPath,
            targetPath = mergedOutputPath,
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
        return matches.firstOrNull { it.nameWithoutExtension == stem && isLikelyPrimaryMediaFile(it) }?.absolutePath
            ?: matches.firstOrNull(::isLikelyPrimaryMediaFile)?.absolutePath
            ?: matches.firstOrNull { it.nameWithoutExtension == stem && !isKnownSidecarArtifact(it) }?.absolutePath
            ?: matches.firstOrNull { !isKnownSidecarArtifact(it) }?.absolutePath
            ?: matches.firstOrNull()?.absolutePath
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
        val baseName = videoFile.nameWithoutExtension
            .removeSuffix(".video")
            .removeSuffix(".audio")
            .replace(Regex("""\.f[a-z0-9_-]+$""", RegexOption.IGNORE_CASE), "")
        return File(videoFile.parentFile, "$baseName.$ext").absolutePath
    }

    private fun buildOutputTemplateForExistingFile(path: String): String {
        val file = File(path)
        val baseName = file.nameWithoutExtension
        return File(file.parentFile, "$baseName.%(ext)s").absolutePath
    }

    private fun splitFormatSelector(selector: String): SplitSelectors? {
        return selector.split('/')
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .mapNotNull { alternative ->
                val separatorIndex = alternative.indexOf('+')
                if (separatorIndex <= 0 || separatorIndex >= alternative.lastIndex) {
                    return@mapNotNull null
                }
                val videoSelector = alternative.substring(0, separatorIndex).trim()
                val audioSelector = alternative.substring(separatorIndex + 1).trim()
                if (videoSelector.isBlank() || audioSelector.isBlank()) {
                    return@mapNotNull null
                }
                SplitSelectors(
                    videoSelector = videoSelector,
                    audioSelector = audioSelector,
                )
            }
            .firstOrNull()
    }

    private fun safeDelete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun exportManagedMediaBundle(
        primaryPath: String,
        playlistFolderName: String?,
        taskId: String,
    ): ExportedMediaBundle {
        val primaryFile = File(primaryPath)
        if (!primaryFile.exists()) {
            return ExportedMediaBundle(primaryPath = primaryPath, subtitlePaths = emptyList())
        }

        val bundleFiles = fileUtils.resolveManagedMediaBundle(primaryPath)
        if (bundleFiles.isEmpty()) {
            return ExportedMediaBundle(primaryPath = primaryPath, subtitlePaths = emptyList())
        }

        var finalPrimaryPath = primaryPath
        val finalSubtitlePaths = mutableListOf<String>()
        val sidecars = bundleFiles.filter { it.absolutePath != primaryFile.absolutePath }
        val exportedPrimaryPath = fileUtils.copyToPublicDownloads(
            sourceFile = primaryFile,
            playlistFolderName = playlistFolderName,
        )
        if (exportedPrimaryPath != null) {
            appendDebugTrace(taskId, "Copied to public Downloads: $exportedPrimaryPath")
            finalPrimaryPath = exportedPrimaryPath
            if (exportedPrimaryPath != primaryFile.absolutePath && primaryFile.delete()) {
                appendDebugTrace(taskId, "Removed private staging copy after public export: ${primaryFile.name}")
            }
        }

        val sourceStem = primaryFile.nameWithoutExtension
        val exportedPrimaryFile = File(finalPrimaryPath)
        val targetStem = exportedPrimaryFile.nameWithoutExtension
        sidecars.forEach { artifact ->
            val targetFileName = if (exportedPrimaryPath != null) {
                val suffix = artifact.name.removePrefix(sourceStem)
                "$targetStem$suffix"
            } else {
                null
            }
            val exportedPath = fileUtils.copyToPublicDownloads(
                sourceFile = artifact,
                playlistFolderName = playlistFolderName,
                targetFileName = targetFileName,
            )
            if (exportedPath != null) {
                appendDebugTrace(taskId, "Copied to public Downloads: $exportedPath")
                if (isSupportedSubtitlePath(exportedPath)) {
                    finalSubtitlePaths += exportedPath
                }
                if (exportedPath != artifact.absolutePath && artifact.delete()) {
                    appendDebugTrace(taskId, "Removed private staging copy after public export: ${artifact.name}")
                }
            } else if (isSupportedSubtitlePath(artifact.absolutePath)) {
                finalSubtitlePaths += artifact.absolutePath
            }
        }
        appendDebugTrace(taskId, "Saved file: $finalPrimaryPath")
        return ExportedMediaBundle(
            primaryPath = finalPrimaryPath,
            subtitlePaths = finalSubtitlePaths.distinct(),
        )
    }

    private fun isSupportedSubtitlePath(path: String): Boolean {
        return when (File(path).extension.lowercase()) {
            "srt", "vtt", "webvtt", "ass", "ssa", "ttml", "dfxp", "xml" -> true
            else -> false
        }
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

    private fun recoverPrimaryMediaAfterThumbnailFailure(
        stderr: String,
        currentOutputPath: String?,
        outputTemplate: String,
    ): String? {
        if (!isThumbnailPostprocessingFailure(stderr)) return null

        currentOutputPath
            ?.let(::File)
            ?.takeIf { it.exists() && isLikelyPrimaryMediaFile(it) && !isTemporarySplitArtifact(it) }
            ?.let { return it.absolutePath }

        return inferDownloadedPath(outputTemplate)
            ?.takeIf { candidate ->
                File(candidate).exists() && isLikelyPrimaryMediaFile(File(candidate))
            }
    }

    private fun isThumbnailPostprocessingFailure(stderr: String): Boolean {
        val lower = stderr.lowercase()
        return lower.contains("postprocessing: decoder (codec webp) not found") ||
            (lower.contains("thumbnailsconvertor") && lower.contains("decoder (codec webp) not found")) ||
            (lower.contains("embedthumbnail") && lower.contains("decoder (codec webp) not found")) ||
            (lower.contains("thumbnail") && lower.contains("decoder (codec webp) not found"))
    }

    private fun cleanupThumbnailSidecars(primaryPath: String) {
        val primaryFile = File(primaryPath)
        val parent = primaryFile.parentFile ?: return
        val stem = primaryFile.nameWithoutExtension
        parent.listFiles()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.absolutePath != primaryFile.absolutePath &&
                    candidate.nameWithoutExtension == stem &&
                    candidate.extension.lowercase() in THUMBNAIL_SIDECAR_EXTENSIONS
            }
            ?.forEach { candidate ->
                runCatching { candidate.delete() }
            }
    }

    private fun generateVideoFrameThumbnail(
        primaryPath: String,
        taskId: String,
    ): String? {
        val primaryFile = File(primaryPath)
        if (!primaryFile.exists()) return null
        if (primaryFile.extension.lowercase() !in VIDEO_ARTIFACT_EXTENSIONS) {
            appendDebugTrace(taskId, "Skipping poster-frame thumbnail fallback for non-video file")
            return null
        }

        findExistingThumbnailSidecar(primaryPath)?.let { existing ->
            appendDebugTrace(taskId, "Keeping existing thumbnail sidecar after postprocess failure: ${existing.name}")
            return existing.absolutePath
        }

        cleanupThumbnailSidecars(primaryPath)
        val targetFile = File(primaryFile.parentFile, "${primaryFile.nameWithoutExtension}.jpg")
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(primaryPath)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: throw IllegalStateException("No video frame available for thumbnail fallback")
            targetFile.outputStream().use { output ->
                check(frame.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                    "Unable to encode fallback thumbnail"
                }
            }
            runCatching { frame.recycle() }
            if (targetFile.exists() && targetFile.length() > 0L) {
                appendDebugTrace(taskId, "Generated poster-frame thumbnail fallback: ${targetFile.name}")
                targetFile.absolutePath
            } else {
                throw IllegalStateException("Fallback thumbnail file was empty")
            }
        }.onFailure { error ->
            appendDebugTrace(
                taskId,
                "Poster-frame thumbnail fallback skipped: ${error.message.orEmpty().ifBlank { "unknown error" }.take(MAX_OUTPUT_TRACE_LINE_LENGTH)}",
            )
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }

    private fun findExistingThumbnailSidecar(primaryPath: String): File? {
        val primaryFile = File(primaryPath)
        val parent = primaryFile.parentFile ?: return null
        val stem = primaryFile.nameWithoutExtension
        return parent.listFiles()
            ?.firstOrNull { candidate ->
                candidate.isFile &&
                    candidate.absolutePath != primaryFile.absolutePath &&
                    candidate.nameWithoutExtension == stem &&
                    candidate.extension.lowercase() in THUMBNAIL_SIDECAR_EXTENSIONS &&
                    candidate.length() > 0L
            }
    }

    private suspend fun recoverPrimaryMediaAfterPostprocessingFailure(
        taskId: String,
        stderr: String,
        currentOutputPath: String?,
        outputTemplate: String,
        preferredExtension: String?,
    ): String? {
        if (!isRecoverablePostprocessingFailure(stderr)) return null

        currentOutputPath
            ?.let(::File)
            ?.takeIf {
                it.exists() &&
                    isLikelyPrimaryMediaFile(it) &&
                    !isTemporarySplitArtifact(it) &&
                    it.length() > MIN_RECOVERED_MEDIA_BYTES
            }
            ?.let { return it.absolutePath }

        val splitArtifacts = findSplitMediaArtifacts(
            outputTemplate = outputTemplate,
            currentOutputPath = currentOutputPath,
        ) ?: return null
        val targetPath = currentOutputPath
            ?.takeIf { candidate ->
                !isTemporarySplitArtifact(File(candidate))
            }
            ?: buildMergedOutputPath(
                videoPath = splitArtifacts.video.absolutePath,
                preferredExtension = preferredExtension ?: splitArtifacts.video.extension.ifBlank { "mp4" },
            )
        val targetFile = File(targetPath)
        if (
            targetFile.exists() &&
            targetFile.absolutePath != splitArtifacts.video.absolutePath &&
            targetFile.absolutePath != splitArtifacts.audio.absolutePath
        ) {
            runCatching { targetFile.delete() }
        }

        appendDebugTrace(
            taskId,
            "Recovering split media with direct ffmpeg remux after yt-dlp postprocessing failure",
        )
        val remuxResult = mergeMediaWithFallback(
            taskId = taskId,
            videoPath = splitArtifacts.video.absolutePath,
            audioPath = splitArtifacts.audio.absolutePath,
            targetPath = targetPath,
        )
        if (!remuxResult.isSuccess || !targetFile.exists() || targetFile.length() <= MIN_RECOVERED_MEDIA_BYTES) {
            appendDebugTrace(
                taskId,
                "Manual remux recovery failed: ${preferredFailureLine(remuxResult.stderr).orEmpty().ifBlank { "unknown ffmpeg error" }.take(MAX_OUTPUT_TRACE_LINE_LENGTH)}",
            )
            return null
        }

        if (splitArtifacts.video.absolutePath != targetFile.absolutePath) {
            safeDelete(splitArtifacts.video.absolutePath)
        }
        if (splitArtifacts.audio.absolutePath != targetFile.absolutePath) {
            safeDelete(splitArtifacts.audio.absolutePath)
        }
        return targetFile.absolutePath
    }

    private suspend fun mergeMediaWithFallback(
        taskId: String,
        videoPath: String,
        audioPath: String,
        targetPath: String,
    ): CommandResult {
        val copyResult = runMergeAttempt(
            taskId = taskId,
            videoPath = videoPath,
            audioPath = audioPath,
            targetPath = targetPath,
            reencodeAudio = false,
        )
        if (copyResult.isSuccess) {
            return copyResult
        }

        val lower = copyResult.stderr.lowercase()
        if (
            !lower.contains("stream #") &&
            !lower.contains("error") &&
            !lower.contains("invalid") &&
            !lower.contains("could not") &&
            !lower.contains("failed")
        ) {
            return copyResult
        }

        runCatching { File(targetPath).delete() }
        appendDebugTrace(taskId, "Stream-copy merge failed; retrying with AAC audio fallback")
        val fallbackResult = runMergeAttempt(
            taskId = taskId,
            videoPath = videoPath,
            audioPath = audioPath,
            targetPath = targetPath,
            reencodeAudio = true,
        )
        if (fallbackResult.isSuccess) {
            appendDebugTrace(taskId, "AAC audio fallback merge succeeded")
        }
        return fallbackResult
    }

    private suspend fun runMergeAttempt(
        taskId: String,
        videoPath: String,
        audioPath: String,
        targetPath: String,
        reencodeAudio: Boolean,
    ): CommandResult {
        return ffmpegExecutor.execute(
            args = buildList {
                add("-y")
                add("-i")
                add(videoPath)
                add("-i")
                add(audioPath)
                add("-map")
                add("0:v:0")
                add("-map")
                add("1:a:0")
                add("-c:v")
                add("copy")
                if (reencodeAudio) {
                    add("-c:a")
                    add("aac")
                    add("-b:a")
                    add("128k")
                } else {
                    add("-c:a")
                    add("copy")
                }
                if (targetPath.endsWith(".mp4", ignoreCase = true) || targetPath.endsWith(".mov", ignoreCase = true)) {
                    add("-movflags")
                    add("+faststart")
                }
                add(targetPath)
            },
            onStderrLine = { line ->
                appendDebugTrace(taskId, "ffmpeg: ${line.take(MAX_OUTPUT_TRACE_LINE_LENGTH)}")
            },
        )
    }

    private fun isRecoverablePostprocessingFailure(stderr: String): Boolean {
        val lower = stderr.lowercase()
        return lower.contains("postprocessing:") ||
            lower.contains("thumbnailsconvertor") ||
            lower.contains("embedthumbnail") ||
            lower.contains("stream #1:0 -> #0:1 (copy)") ||
            (lower.contains("stream #") && lower.contains("(copy)"))
    }

    private fun findSplitMediaArtifacts(
        outputTemplate: String,
        currentOutputPath: String?,
    ): SplitMediaArtifacts? {
        val currentOutputFile = currentOutputPath?.let(::File)
        val parent = currentOutputFile?.parentFile
            ?: File(outputTemplate).parentFile
            ?: return null
        val stem = currentOutputFile
            ?.nameWithoutExtension
            ?.takeIf { it.isNotBlank() }
            ?: File(outputTemplate).name.substringBefore(".%(ext)s")
        val candidates = parent.listFiles()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.name.startsWith(stem) &&
                    candidate.length() > 0L &&
                    isTemporarySplitArtifact(candidate)
            }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        val video = candidates.firstOrNull(::isPossibleVideoSplitArtifact) ?: return null
        val audio = candidates.firstOrNull(::isPossibleAudioSplitArtifact) ?: return null
        return SplitMediaArtifacts(video = video, audio = audio)
    }

    private fun cleanupOrphanedManagedArtifacts(outputTemplate: String, taskId: String) {
        val templateFile = File(outputTemplate)
        val parent = templateFile.parentFile ?: return
        val stem = templateFile.name.substringBefore(".%(ext)s")
        val primaryExists = parent.listFiles()
            ?.any { candidate ->
                candidate.isFile &&
                    candidate.nameWithoutExtension == stem &&
                    isLikelyPrimaryMediaFile(candidate)
            }
            ?: false
        if (primaryExists) return

        parent.listFiles()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.name.startsWith(stem) &&
                    candidate.nameWithoutExtension != stem &&
                    (
                        isKnownSidecarArtifact(candidate) ||
                            isTemporarySplitArtifact(candidate) ||
                            isTemporaryDownloadArtifactName(candidate.name)
                    )
            }
            ?.forEach { candidate ->
                if (runCatching { candidate.delete() }.getOrDefault(false)) {
                    appendDebugTrace(taskId, "Removed stale artifact before download: ${candidate.name}")
                }
            }
    }

    private fun isLikelyPrimaryMediaFile(file: File): Boolean {
        return file.extension.lowercase() in PRIMARY_MEDIA_EXTENSIONS
    }

    private fun isKnownSidecarArtifact(file: File): Boolean {
        return file.extension.lowercase() in KNOWN_SIDECAR_EXTENSIONS
    }

    private fun isTemporarySplitArtifact(file: File): Boolean {
        val lower = file.name.lowercase()
        return Regex(""".*\.f[a-z0-9_-]+.*\..+""").matches(lower) ||
            lower.contains(".fdash-") ||
            lower.contains(".video.") ||
            lower.contains(".audio.")
    }

    private fun isPossibleVideoSplitArtifact(file: File): Boolean {
        if (!isTemporarySplitArtifact(file)) return false
        return file.extension.lowercase() in VIDEO_ARTIFACT_EXTENSIONS
    }

    private fun isPossibleAudioSplitArtifact(file: File): Boolean {
        if (!isTemporarySplitArtifact(file)) return false
        return file.extension.lowercase() in AUDIO_ARTIFACT_EXTENSIONS
    }

    private fun isTemporaryDownloadArtifactName(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".part") ||
            lower.endsWith(".ytdl") ||
            lower.endsWith(".temp")
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
        val raw = listOfNotNull(
            stderr?.trim()?.takeIf { it.isNotBlank() },
            throwable?.message?.trim()?.takeIf { it.isNotBlank() },
        ).firstOrNull { !isClosedStreamFailure(it.lowercase()) }
            ?: listOfNotNull(
                stderr?.trim()?.takeIf { it.isNotBlank() },
                throwable?.message?.trim()?.takeIf { it.isNotBlank() },
            ).firstOrNull()
            ?: "Download failed"

        val shortRaw = preferredFailureLine(raw)
            ?.take(MAX_ERROR_MESSAGE_CHARS)
            ?: "Download failed"
        if (isClosedStreamFailure(shortRaw.lowercase())) {
            return "Download was interrupted before yt-dlp returned a stable error. Please retry."
        }
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

    private suspend fun startForegroundIfPossible(
        title: String,
        progress: Int,
        taskId: String,
        downloadedStr: String? = null,
        totalStr: String? = null,
        speed: String? = null,
        eta: String? = null,
    ): Boolean {
        try {
            AppNotifications.ensureChannels(applicationContext)
            setForeground(
                createForegroundInfo(
                    title = title,
                    progress = progress,
                    taskId = taskId,
                    downloadedStr = downloadedStr,
                    totalStr = totalStr,
                    speed = speed,
                    eta = eta,
                ),
            )
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

    private fun updateForegroundIfPossible(
        title: String,
        progress: Int,
        taskId: String,
        downloadedStr: String? = null,
        totalStr: String? = null,
        speed: String? = null,
        eta: String? = null,
    ) {
        runCatching {
            setForegroundAsync(
                createForegroundInfo(
                    title = title,
                    progress = progress,
                    taskId = taskId,
                    downloadedStr = downloadedStr,
                    totalStr = totalStr,
                    speed = speed,
                    eta = eta,
                ),
            )
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
            lower.contains("transport endpoint is not connected") ||
            isClosedStreamFailure(lower)
    }

    private fun shouldRetryClosedStreamFailure(stderr: String): Boolean {
        return isClosedStreamFailure(stderr.lowercase())
    }

    private fun preferredFailureLine(detail: String): String? {
        val lines = detail.lineSequence()
            .map { it.trim().removePrefix("ERROR: ") }
            .filter { it.isNotBlank() }
            .toList()
        return lines.firstOrNull { line ->
            val lower = line.lowercase()
            lower.contains("http error") ||
                lower.contains("forbidden") ||
                lower.contains("requested format") ||
                lower.contains("fragment not found") ||
                lower.contains("unable to download video data") ||
                lower.contains("sign in to confirm") ||
                lower.contains("use --cookies") ||
                lower.contains("precondition check failed")
        } ?: lines.firstOrNull()
    }

    private fun isClosedStreamFailure(detail: String): Boolean {
        return detail.contains("interrupted by close") ||
            detail.contains("stream closed") ||
            detail.contains("i/o operation on closed file") ||
            detail.contains("operation on closed file") ||
            detail.contains("broken pipe") ||
            detail == "closed"
    }

    private fun isFragmentAccessFailure(detail: String): Boolean {
        return detail.contains("fragment not found") ||
            detail.contains("unable to download video data") ||
            (detail.contains("giving up after") && detail.contains("fragment"))
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

    private fun finishPausedResult(): Result {
        return Result.success(
            workDataOf(
                WorkerKeys.TERMINAL_STATUS to DownloadStatus.PAUSED.name,
            ),
        )
    }

    private fun shouldKeepPausedState(
        taskId: String,
        throwable: Throwable?,
        stderr: String?,
    ): Boolean {
        val task = downloadTaskStore.getTask(taskId) ?: return false
        if (task.status != DownloadStatus.PAUSED) return false

        val detail = sequenceOf(
            throwable?.message?.trim(),
            stderr?.trim(),
        ).filterNotNull()
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
            .lowercase()

        return isClosedStreamFailure(detail) ||
            detail.contains("cancelled") ||
            detail.contains("canceled") ||
            detail.contains("interrupted")
    }

    private fun canPostNotifications(): Boolean {
        return AppNotifications.canPostUserNotifications(applicationContext)
    }

    private fun createForegroundInfo(
        title: String,
        progress: Int,
        taskId: String,
        downloadedStr: String? = null,
        totalStr: String? = null,
        speed: String? = null,
        eta: String? = null,
    ): ForegroundInfo {
        val notificationId = AppNotifications.progressNotificationId(taskId)
        val notification: Notification = AppNotifications.buildActiveDownloadNotification(
            context = applicationContext,
            taskId = taskId,
            title = title,
            progress = progress,
            downloadedStr = downloadedStr,
            totalStr = totalStr,
            speed = speed,
            eta = eta,
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun showCompletionNotification(
        taskId: String,
        title: String,
        outputPath: String?,
        sizeLabel: String?,
    ) {
        AppNotifications.showDownloadCompleted(
            context = applicationContext,
            taskId = taskId,
            title = title,
            outputPath = outputPath,
            sizeLabel = sizeLabel,
        )
    }

    private fun showFailureNotification(
        taskId: String,
        title: String,
        message: String,
    ) {
        AppNotifications.showDownloadFailed(
            context = applicationContext,
            taskId = taskId,
            title = title,
            errorMessage = message,
        )
    }

    private companion object {
        const val MAX_DEBUG_TRACE_CHARS = 250_000
        const val MAX_ERROR_MESSAGE_CHARS = 800
        const val MAX_OUTPUT_TRACE_LINE_LENGTH = 2_000
        const val MAX_TRANSIENT_RETRY_ATTEMPTS = 3
        const val MIN_RECOVERED_MEDIA_BYTES = 32L * 1024L
        val PRIMARY_MEDIA_EXTENSIONS = setOf(
            "mp4",
            "m4v",
            "mkv",
            "webm",
            "mov",
            "avi",
            "3gp",
            "m4a",
            "mp3",
            "aac",
            "opus",
            "ogg",
            "flac",
            "wav",
        )
        val VIDEO_ARTIFACT_EXTENSIONS = setOf(
            "mp4",
            "m4v",
            "mkv",
            "webm",
            "mov",
            "avi",
            "3gp",
        )
        val AUDIO_ARTIFACT_EXTENSIONS = setOf(
            "m4a",
            "mp3",
            "aac",
            "opus",
            "ogg",
            "flac",
            "wav",
            "webm",
        )
        val THUMBNAIL_SIDECAR_EXTENSIONS = setOf(
            "jpg",
            "jpeg",
            "png",
            "webp",
        )
        val KNOWN_SIDECAR_EXTENSIONS = THUMBNAIL_SIDECAR_EXTENSIONS + setOf(
            "srt",
            "vtt",
            "webvtt",
            "ass",
            "ssa",
            "ttml",
            "dfxp",
            "xml",
            "description",
            "json",
            "infojson",
            "nfo",
        )
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

    private data class ExportedMediaBundle(
        val primaryPath: String?,
        val subtitlePaths: List<String>,
    )

    private data class SplitMediaArtifacts(
        val video: File,
        val audio: File,
    )

    private data class SafeModeAttempt(
        val label: String,
        val selector: String,
        val extractorArgs: String? = null,
    )
}
