package com.localdownloader.data

import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.localdownloader.worker.DownloadWorker
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.PlaylistEntry
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.downloader.FormatExtractor
import com.localdownloader.ffmpeg.Compressor
import com.localdownloader.ffmpeg.FormatConverter
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import com.localdownloader.worker.WorkerKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val formatExtractor: FormatExtractor,
    private val downloadTaskStore: DownloadTaskStore,
    private val formatConverter: FormatConverter,
    private val compressor: Compressor,
    private val settingsStore: SettingsStore,
    private val workManager: WorkManager,
    private val fileUtils: FileUtils,
    private val logger: Logger,
    private val json: Json,
) : DownloaderRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workObserverJobs = ConcurrentHashMap.newKeySet<String>()

    override suspend fun analyzeUrl(url: String): Result<VideoInfo> {
        logger.i("DownloadRepository", "analyzeUrl called for: $url")
        val result = formatExtractor.analyze(url)
        result.onSuccess { info ->
            logger.i(
                "DownloadRepository",
                "analyzeUrl success: title='${info.title}', formats=${info.formats.size}, playlist=${info.isPlaylist}",
            )
        }.onFailure { error ->
            logger.e("DownloadRepository", "analyzeUrl failed for: $url", error)
        }
        return result
    }

    override suspend fun enqueueDownload(options: DownloadOptions, titleHint: String): Result<String> {
        return runCatching {
            logger.i(
                "DownloadRepository",
                "enqueueDownload called url=${options.url}, format=${options.formatId}, titleHint='$titleHint'",
            )
            val outputTemplate = if (File(options.outputTemplate).isAbsolute) {
                options.outputTemplate
            } else {
                val sameUrlCount = downloadTaskStore.countByUrl(options.url)
                val templateBase = if (sameUrlCount > 0) {
                    fileUtils.appendCounterToTemplate(options.outputTemplate, sameUrlCount)
                } else {
                    options.outputTemplate
                }
                fileUtils.createOutputTemplateWithDirectory(templateBase)
            }
            val prepared = prepareDownload(
                options = options.copy(outputTemplate = outputTemplate),
                titleHint = titleHint,
            )
            workManager.enqueue(prepared.request)
            observeWorkState(taskId = prepared.taskId, workId = prepared.request.id)
            logger.i("DownloadRepository", "Queued download ${prepared.taskId} with outputTemplate=$outputTemplate")
            prepared.taskId
        }.onFailure { error ->
            logger.e("DownloadRepository", "enqueueDownload failed", error)
        }
    }

    override suspend fun enqueuePlaylistDownload(
        options: DownloadOptions,
        playlistTitle: String,
        entries: List<PlaylistEntry>,
    ): Result<List<String>> {
        return runCatching {
            require(entries.isNotEmpty()) { "This playlist does not contain any downloadable items." }

            val playlistDirectory = fileUtils.createUniquePlaylistDirectory(playlistTitle)
            val preparedDownloads = entries.map { entry ->
                val itemOutputTemplate = fileUtils.buildPlaylistItemOutputTemplate(
                    playlistDirectory = playlistDirectory,
                    baseTemplate = options.outputTemplate,
                    playlistItemIndex = entry.playlistItemIndex,
                )
                prepareDownload(
                    options = options.copy(
                        outputTemplate = itemOutputTemplate,
                        isPlaylistEnabled = true,
                        playlistItemIndex = entry.playlistItemIndex,
                        playlistFolderName = playlistDirectory.name,
                    ),
                    titleHint = entry.title,
                )
            }

            enqueueSequential(preparedDownloads.map { it.request })
            preparedDownloads.forEach { prepared ->
                observeWorkState(taskId = prepared.taskId, workId = prepared.request.id)
            }
            preparedDownloads.map { it.taskId }
        }.onFailure { error ->
            logger.e("DownloadRepository", "enqueuePlaylistDownload failed", error)
        }
    }

    override suspend fun pauseDownload(taskId: String) {
        logger.i("DownloadRepository", "pauseDownload taskId=$taskId")
        workManager.cancelWorkById(UUID.fromString(taskId))
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.PAUSED,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun resumeDownload(taskId: String): Result<String> {
        logger.i("DownloadRepository", "resumeDownload taskId=$taskId")

        val task = downloadTaskStore.getTask(taskId)
            ?: return Result.failure(IllegalStateException("No task found with ID $taskId"))

        val optionsJson = downloadTaskStore.getCachedOptions(taskId)
            ?: return Result.failure(IllegalStateException("No cached download options for task $taskId"))

        val options = runCatching { json.decodeFromString<DownloadOptions>(optionsJson) }
            .getOrElse { return Result.failure(it) }

        return enqueueDownload(options = options, titleHint = task.title)
    }

    override suspend fun cancelDownload(taskId: String) {
        logger.i("DownloadRepository", "cancelDownload taskId=$taskId")
        workManager.cancelWorkById(UUID.fromString(taskId))
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.CANCELED,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun renameDownloadedFile(taskId: String, newName: String): Result<Unit> {
        return runCatching {
            val task = downloadTaskStore.getTask(taskId)
                ?: error("No task found with ID $taskId")
            val outputPath = task.outputPath?.takeIf { it.isNotBlank() }
                ?: error("This task does not have a saved output file.")
            val sourceFile = File(outputPath)
            if (!sourceFile.exists()) {
                error("The saved file could not be found.")
            }

            val normalizedName = buildRenamedFileName(
                rawName = newName,
                currentName = sourceFile.name,
            )
            val targetFile = File(sourceFile.parentFile, normalizedName)
            require(sourceFile.absolutePath != targetFile.absolutePath) {
                "Choose a different name."
            }
            require(!targetFile.exists()) {
                "A file with that name already exists."
            }
            require(sourceFile.renameTo(targetFile)) {
                "Unable to rename the saved file."
            }

            downloadTaskStore.update(taskId) { current ->
                current.copy(
                    title = targetFile.name,
                    outputPath = targetFile.absolutePath,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "renameDownloadedFile failed taskId=$taskId", error)
        }
    }

    override suspend fun deleteDownloadedFile(taskId: String): Result<Unit> {
        return runCatching {
            val task = downloadTaskStore.getTask(taskId)
                ?: error("No task found with ID $taskId")
            val outputPath = task.outputPath?.takeIf { it.isNotBlank() }
            if (outputPath != null) {
                val file = File(outputPath)
                if (file.exists() && !file.delete()) {
                    error("Unable to delete the saved file.")
                }
            }
            downloadTaskStore.remove(taskId)
        }.onFailure { error ->
            logger.e("DownloadRepository", "deleteDownloadedFile failed taskId=$taskId", error)
        }
    }

    override fun observeDownloadQueue(): Flow<List<DownloadTask>> = downloadTaskStore.observeAll()

    override suspend fun convertMedia(
        request: ConversionRequest,
        onProgress: ((Float) -> Unit)?,
    ): Result<String> = formatConverter.convert(request, onProgress)

    override suspend fun compressMedia(
        request: CompressionRequest,
        onProgress: ((Float) -> Unit)?,
    ): Result<String> = compressor.compress(request, onProgress)

    override fun observeSettings(): Flow<AppSettings> = settingsStore.observeSettings()

    override suspend fun updateSettings(settings: AppSettings) {
        settingsStore.updateSettings(settings)
    }

    private fun prepareDownload(
        options: DownloadOptions,
        titleHint: String,
    ): PreparedDownload {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    WorkerKeys.URL to options.url,
                    WorkerKeys.FORMAT_ID to options.formatId,
                    WorkerKeys.OUTPUT_TEMPLATE to options.outputTemplate,
                    WorkerKeys.EXTRACTOR_ARGS to (options.extractorArgs ?: ""),
                    WorkerKeys.FALLBACK_EXTRACTOR_ARGS to (options.fallbackExtractorArgs ?: ""),
                    WorkerKeys.YOUTUBE_AUTH_ENABLED to options.youtubeAuthEnabled,
                    WorkerKeys.YOUTUBE_COOKIES_PATH to (options.youtubeCookiesPath ?: ""),
                    WorkerKeys.YOUTUBE_PO_TOKEN to (options.youtubePoToken ?: ""),
                    WorkerKeys.YOUTUBE_PO_TOKEN_CLIENT_HINT to options.youtubePoTokenClientHint,
                    WorkerKeys.MERGE_OUTPUT_FORMAT to (options.mergeOutputFormat ?: ""),
                    WorkerKeys.PLAYLIST_ENABLED to options.isPlaylistEnabled,
                    WorkerKeys.DOWNLOAD_SUBTITLES to options.shouldDownloadSubtitles,
                    WorkerKeys.EMBED_METADATA to options.shouldEmbedMetadata,
                    WorkerKeys.EMBED_THUMBNAIL to options.shouldEmbedThumbnail,
                    WorkerKeys.WRITE_THUMBNAIL to options.shouldWriteThumbnail,
                    WorkerKeys.EXTRACT_AUDIO to options.extractAudio,
                    WorkerKeys.AUDIO_FORMAT to (options.audioFormat ?: ""),
                    WorkerKeys.AUDIO_BITRATE to (options.audioBitrateKbps ?: -1),
                    WorkerKeys.TITLE_HINT to titleHint,
                    WorkerKeys.PLAYLIST_ITEM_INDEX to (options.playlistItemIndex ?: -1),
                    WorkerKeys.PLAYLIST_FOLDER_NAME to (options.playlistFolderName ?: ""),
                ),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        val taskId = request.id.toString()
        val queuedTask = DownloadTask(
            id = taskId,
            url = options.url,
            title = titleHint.ifBlank { "Queued download" },
            status = DownloadStatus.QUEUED,
            debugTrace = "Queued: waiting for worker start",
        )

        downloadTaskStore.upsert(
            task = queuedTask,
            optionsJson = json.encodeToString(options),
        )

        return PreparedDownload(
            taskId = taskId,
            request = request,
        )
    }

    private fun enqueueSequential(requests: List<OneTimeWorkRequest>) {
        if (requests.isEmpty()) return
        var continuation = workManager.beginWith(requests.first())
        requests.drop(1).forEach { request ->
            continuation = continuation.then(request)
        }
        continuation.enqueue()
    }

    private fun observeWorkState(taskId: String, workId: UUID) {
        repositoryScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { info ->
                if (info == null) {
                    appendTaskDebug(taskId, "WorkManager state unavailable")
                    return@collect
                }
                syncTaskFromWorkState(taskId, info)
            }
        }.also { /* no need to store reference, WorkManager handles persistence */ }
    }

    private fun syncTaskFromWorkState(taskId: String, info: WorkInfo) {
        when (info.state) {
            WorkInfo.State.ENQUEUED -> {
                downloadTaskStore.update(taskId) { task ->
                    if (task.status.isTerminal) task
                    else task.copy(
                        status = DownloadStatus.QUEUED,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.RUNNING -> {
                downloadTaskStore.update(taskId) { task ->
                    if (task.status.isTerminal) task
                    else task.copy(
                        status = DownloadStatus.RUNNING,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.SUCCEEDED -> {
                val outputPath = info.outputData.getString(WorkerKeys.OUTPUT_PATH)
                downloadTaskStore.update(taskId) { task ->
                    if (task.status == DownloadStatus.COMPLETED) task
                    else task.copy(
                        status = DownloadStatus.COMPLETED,
                        progressPercent = task.progressPercent.coerceAtLeast(100),
                        outputPath = outputPath ?: task.outputPath,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.FAILED -> {
                val failureMessage = info.outputData.getString(WorkerKeys.ERROR_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Download failed before worker returned an explicit error"
                downloadTaskStore.update(taskId) { task ->
                    task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = failureMessage,
                        debugTrace = appendDebugLine(task.debugTrace, "WorkManager failed: $failureMessage"),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.CANCELLED -> {
                downloadTaskStore.update(taskId) { task ->
                    task.copy(
                        status = DownloadStatus.CANCELED,
                        debugTrace = appendDebugLine(task.debugTrace, "WorkManager cancelled"),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.BLOCKED -> {
                appendTaskDebug(taskId, "WorkManager blocked: waiting for constraints")
            }
        }
    }

    private fun appendTaskDebug(taskId: String, line: String) {
        val cleaned = line.trim().replace("\n", " ")
        if (cleaned.isBlank()) return
        val entry = "${System.currentTimeMillis()}: $cleaned"
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                debugTrace = appendDebugLine(task.debugTrace, entry),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    private fun appendDebugLine(existing: String?, line: String): String {
        val combined = if (existing.isNullOrBlank()) line else "$existing\n$line"
        return combined.takeLast(MAX_DEBUG_TRACE_CHARS)
    }

    private fun buildRenamedFileName(rawName: String, currentName: String): String {
        val currentExtension = currentName.substringAfterLast('.', "")
        val sanitized = fileUtils.sanitizeFileName(rawName.trim())
        if (sanitized.contains('.') || currentExtension.isBlank()) {
            return sanitized
        }
        return "$sanitized.$currentExtension"
    }

    private companion object {
        const val MAX_DEBUG_TRACE_CHARS = 10_000
    }

    private data class PreparedDownload(
        val taskId: String,
        val request: OneTimeWorkRequest,
    )
}

private val DownloadStatus.isTerminal: Boolean
    get() = this in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELED)
