package com.localdownloader.data

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
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
import com.localdownloader.domain.models.MediaSyncResult
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val pauseExpiryJobs = ConcurrentHashMap<String, Job>()
    private val pauseExpiryDeadlines = ConcurrentHashMap<String, Long>()

    init {
        repositoryScope.launch {
            downloadTaskStore.observeAll().collect { tasks ->
                syncPauseExpiryTimers(tasks)
            }
        }
    }

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
            val taskId = UUID.randomUUID().toString()
            val prepared = prepareDownload(
                taskId = taskId,
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
                    taskId = UUID.randomUUID().toString(),
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
        val task = downloadTaskStore.getTask(taskId) ?: return
        val taskOptions = loadTaskOptions(task.id)
        val pauseTargets = resolvePauseTargets(task, taskOptions)
        val pauseExpiresAt = System.currentTimeMillis() + PAUSE_RESUME_WINDOW_MS

        pauseTargets.forEach { target ->
            downloadTaskStore.update(target.id) { current ->
                if (current.status.isTerminal) {
                    current
                } else {
                    current.copy(
                        status = DownloadStatus.PAUSED,
                        pauseExpiresAtEpochMs = pauseExpiresAt,
                        errorMessage = null,
                        debugTrace = appendDebugLine(
                            current.debugTrace,
                            if (pauseTargets.size > 1) {
                                "Paused by user as part of this playlist. Resume available for ${PAUSE_RESUME_WINDOW_MS / 60_000} minutes before cleanup."
                            } else {
                                "Paused by user. Resume available for ${PAUSE_RESUME_WINDOW_MS / 60_000} minutes before cleanup."
                            },
                        ),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }
            target.activeWorkId?.let { workManager.cancelWorkById(UUID.fromString(it)) }
        }
    }

    override suspend fun resumeDownload(taskId: String): Result<String> {
        logger.i("DownloadRepository", "resumeDownload taskId=$taskId")

        val task = downloadTaskStore.getTask(taskId)
            ?: return Result.failure(IllegalStateException("No task found with ID $taskId"))

        if (task.status != DownloadStatus.PAUSED) {
            return Result.failure(IllegalStateException("Only paused downloads can be resumed."))
        }

        val taskOptions = downloadTaskStore.getCachedOptions(taskId)
            ?.let { serialized -> runCatching { json.decodeFromString<DownloadOptions>(serialized) }.getOrNull() }
            ?: return Result.failure(IllegalStateException("No cached download options for task $taskId"))

        val resumeTargets = resolveResumeTargets(task, taskOptions)
        if (resumeTargets.isEmpty()) {
            return Result.failure(IllegalStateException("No paused download is available to resume."))
        }

        val expiredTargets = resumeTargets.filter { target ->
            target.task.pauseExpiresAtEpochMs?.let { it <= System.currentTimeMillis() } == true
        }
        if (expiredTargets.isNotEmpty()) {
            expiredTargets.forEach { expirePausedDownload(it.task.id) }
            return Result.failure(IllegalStateException("Sorry, this paused download expired after 10 minutes. Its cached data was removed."))
        }

        return runCatching {
            val preparedDownloads = resumeTargets.map { target ->
                prepareDownload(
                    taskId = target.task.id,
                    options = target.options,
                    titleHint = target.task.title,
                    existingTask = target.task,
                )
            }
            if (preparedDownloads.size == 1) {
                workManager.enqueue(preparedDownloads.single().request)
            } else {
                enqueueSequential(preparedDownloads.map { it.request })
            }
            preparedDownloads.forEach { prepared ->
                observeWorkState(taskId = prepared.taskId, workId = prepared.request.id)
            }
            taskId
        }.onFailure { error ->
            logger.e("DownloadRepository", "resumeDownload failed taskId=$taskId", error)
        }
    }

    override suspend fun cancelDownload(taskId: String) {
        logger.i("DownloadRepository", "cancelDownload taskId=$taskId")
        val activeWorkId = downloadTaskStore.getTask(taskId)?.activeWorkId
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.CANCELED,
                pauseExpiresAtEpochMs = null,
                debugTrace = appendDebugLine(task.debugTrace, "Cancelled by user"),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
        activeWorkId?.let { workManager.cancelWorkById(UUID.fromString(it)) }
    }

    override suspend fun renameDownloadedFile(taskId: String, newName: String): Result<Unit> {
        return runCatching {
            val task = downloadTaskStore.getTask(taskId)
                ?: error("No task found with ID $taskId")
            val outputPath = task.outputPath?.takeIf { it.isNotBlank() }
                ?: error("This task does not have a saved output file.")
            val sourceFile = File(fileUtils.normalizeLibraryOutputPath(outputPath))
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
            val renamedPath = fileUtils.renameManagedMediaBundle(
                path = sourceFile.absolutePath,
                targetFileName = normalizedName,
            ) ?: error("Unable to rename the saved file.")
            val renamedSubtitlePaths = fileUtils.resolveManagedMediaBundle(renamedPath)
                .map { it.absolutePath }
                .filter { it != renamedPath }
                .filter(::isSupportedSubtitlePath)

            downloadTaskStore.update(taskId) { current ->
                current.copy(
                    title = File(renamedPath).name,
                    outputPath = renamedPath,
                    subtitlePaths = renamedSubtitlePaths,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "renameDownloadedFile failed taskId=$taskId", error)
        }
    }

    override suspend fun deleteDownloadedFile(taskId: String): Result<Unit> {
        return deleteDownloadedFiles(listOf(taskId))
            .map { Unit }
            .onFailure { error ->
                logger.e("DownloadRepository", "deleteDownloadedFile failed taskId=$taskId", error)
            }
    }

    override suspend fun deleteDownloadedFiles(taskIds: List<String>): Result<Int> {
        return runCatching {
            val ids = taskIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            require(ids.isNotEmpty()) { "Select at least one saved item." }

            val deleteFromStorage = settingsStore.observeSettings().first().deleteFromStorageWhenRemovedInApp
            removeDownloadedTasks(ids, deleteFromStorage)
        }.onFailure { error ->
            logger.e("DownloadRepository", "deleteDownloadedFiles failed taskIds=${taskIds.size}", error)
        }
    }

    override suspend fun clearCompletedDownloads(): Result<Int> {
        return runCatching {
            val completedIds = downloadTaskStore.getAllTasks()
                .filter { it.status == DownloadStatus.COMPLETED }
                .map { it.id }

            if (completedIds.isEmpty()) {
                0
            } else {
                val deleteFromStorage = settingsStore.observeSettings().first().deleteFromStorageWhenRemovedInApp
                removeDownloadedTasks(completedIds, deleteFromStorage)
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "clearCompletedDownloads failed", error)
        }
    }

    override suspend fun clearCompletedLibraryEntries(): Result<Int> {
        return runCatching {
            val completedIds = downloadTaskStore.getAllTasks()
                .filter { it.status == DownloadStatus.COMPLETED }
                .map { it.id }

            if (completedIds.isEmpty()) {
                0
            } else {
                removeDownloadedTasks(completedIds, deleteFromStorage = false)
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "clearCompletedLibraryEntries failed", error)
        }
    }

    override suspend fun deleteAllCompletedMedia(): Result<Int> {
        return runCatching {
            val completedIds = downloadTaskStore.getAllTasks()
                .filter { it.status == DownloadStatus.COMPLETED }
                .map { it.id }

            if (completedIds.isEmpty()) {
                0
            } else {
                removeDownloadedTasks(completedIds, deleteFromStorage = true)
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "deleteAllCompletedMedia failed", error)
        }
    }

    override suspend fun syncDownloadedMedia(removeMissingEntries: Boolean?): Result<MediaSyncResult> {
        return runCatching {
            val settings = settingsStore.observeSettings().first()
            val shouldRemoveMissing = removeMissingEntries ?: settings.autoRemoveMissingFilesFromLibrary
            val completedTasks = downloadTaskStore.getAllTasks()
                .filter { it.status == DownloadStatus.COMPLETED }

            completedTasks.forEach { task ->
                val outputPath = task.outputPath?.takeIf { it.isNotBlank() } ?: return@forEach
                val normalizedPath = fileUtils.normalizeLibraryOutputPath(outputPath)
                val normalizedSubtitlePaths = task.subtitlePaths
                    .map(fileUtils::normalizeLibraryOutputPath)
                    .distinct()
                if (normalizedPath != outputPath || normalizedSubtitlePaths != task.subtitlePaths) {
                    downloadTaskStore.update(task.id) { current ->
                        current.copy(
                            outputPath = normalizedPath,
                            subtitlePaths = normalizedSubtitlePaths,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                }
            }

            val missingTasks = completedTasks.filter { task ->
                val outputPath = task.outputPath?.takeIf { it.isNotBlank() } ?: return@filter true
                !File(fileUtils.normalizeLibraryOutputPath(outputPath)).exists()
            }

            if (shouldRemoveMissing && missingTasks.isNotEmpty()) {
                downloadTaskStore.removeMany(missingTasks.map { it.id })
            }

            MediaSyncResult(
                checkedItems = completedTasks.size,
                missingItems = missingTasks.size,
                removedEntries = if (shouldRemoveMissing) missingTasks.size else 0,
            )
        }.onFailure { error ->
            logger.e("DownloadRepository", "syncDownloadedMedia failed", error)
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
        taskId: String,
        options: DownloadOptions,
        titleHint: String,
        existingTask: DownloadTask? = null,
    ): PreparedDownload {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    WorkerKeys.TASK_ID to taskId,
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
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        val queuedTask = DownloadTask(
            id = taskId,
            url = options.url,
            title = titleHint.ifBlank { existingTask?.title ?: "Queued download" },
            status = DownloadStatus.QUEUED,
            activeWorkId = request.id.toString(),
            progressPercent = existingTask?.progressPercent ?: 0,
            speed = existingTask?.speed,
            eta = existingTask?.eta,
            outputPath = existingTask?.outputPath,
            downloadedStr = existingTask?.downloadedStr,
            totalSizeStr = existingTask?.totalSizeStr,
            errorMessage = null,
            debugTrace = appendDebugLine(existingTask?.debugTrace, "Queued: waiting for worker start"),
            pauseExpiresAtEpochMs = null,
            createdAtEpochMs = existingTask?.createdAtEpochMs ?: System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis(),
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
                syncTaskFromWorkState(taskId, workId.toString(), info)
            }
        }.also { /* no need to store reference, WorkManager handles persistence */ }
    }

    private fun syncTaskFromWorkState(taskId: String, workId: String, info: WorkInfo) {
        val currentTask = downloadTaskStore.getTask(taskId) ?: return
        if (currentTask.activeWorkId != workId) return
        when (info.state) {
            WorkInfo.State.ENQUEUED -> {
                downloadTaskStore.update(taskId) { task ->
                    if (task.status.isTerminal) task
                    else task.copy(
                        status = DownloadStatus.QUEUED,
                        pauseExpiresAtEpochMs = null,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.RUNNING -> {
                downloadTaskStore.update(taskId) { task ->
                    if (task.status.isTerminal) task
                    else task.copy(
                        status = DownloadStatus.RUNNING,
                        pauseExpiresAtEpochMs = null,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.SUCCEEDED -> {
                val terminalStatus = info.outputData.getString(WorkerKeys.TERMINAL_STATUS)
                val outputPath = info.outputData.getString(WorkerKeys.OUTPUT_PATH)
                if (terminalStatus == DownloadStatus.FAILED.name) {
                    val failureMessage = info.outputData.getString(WorkerKeys.ERROR_MESSAGE)
                        ?.takeIf { it.isNotBlank() }
                        ?: "Playlist item failed"
                    downloadTaskStore.update(taskId) { task ->
                        task.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = failureMessage,
                            debugTrace = appendDebugLine(
                                task.debugTrace,
                                "Worker finished with logical failure so playlist queue could continue: $failureMessage",
                            ),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                } else if (terminalStatus == DownloadStatus.PAUSED.name) {
                    downloadTaskStore.update(taskId) { task ->
                        task.copy(
                            status = DownloadStatus.PAUSED,
                            activeWorkId = null,
                            debugTrace = appendDebugLine(task.debugTrace, "Worker stopped cleanly for pause request"),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                } else if (terminalStatus == DownloadStatus.CANCELED.name) {
                    downloadTaskStore.update(taskId) { task ->
                        task.copy(
                            status = DownloadStatus.CANCELED,
                            activeWorkId = null,
                            pauseExpiresAtEpochMs = null,
                            debugTrace = appendDebugLine(task.debugTrace, "Worker stopped cleanly after cancel request"),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                } else {
                    downloadTaskStore.update(taskId) { task ->
                        if (task.status == DownloadStatus.COMPLETED) task
                        else task.copy(
                            status = DownloadStatus.COMPLETED,
                            activeWorkId = null,
                            progressPercent = task.progressPercent.coerceAtLeast(100),
                            outputPath = outputPath ?: task.outputPath,
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                }
            }

            WorkInfo.State.FAILED -> {
                val failureMessage = info.outputData.getString(WorkerKeys.ERROR_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Download failed before worker returned an explicit error"
                downloadTaskStore.update(taskId) { task ->
                    when {
                        task.status == DownloadStatus.PAUSED || task.pauseExpiresAtEpochMs != null -> task.copy(
                            activeWorkId = null,
                            debugTrace = appendDebugLine(
                                task.debugTrace,
                                "Worker reported a failure while pause was active; keeping paused state: $failureMessage",
                            ),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )

                        task.status == DownloadStatus.CANCELED -> task.copy(
                            activeWorkId = null,
                            debugTrace = appendDebugLine(task.debugTrace, "Worker stopped after cancel request"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )

                        else -> task.copy(
                            status = DownloadStatus.FAILED,
                            activeWorkId = null,
                            errorMessage = failureMessage,
                            debugTrace = appendDebugLine(task.debugTrace, "WorkManager failed: $failureMessage"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                }
            }

            WorkInfo.State.CANCELLED -> {
                downloadTaskStore.update(taskId) { task ->
                    when {
                        task.status == DownloadStatus.PAUSED || task.pauseExpiresAtEpochMs != null -> task.copy(
                            activeWorkId = null,
                            debugTrace = appendDebugLine(task.debugTrace, "Worker cancelled after pause request"),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )

                        task.status == DownloadStatus.CANCELED -> task.copy(
                            activeWorkId = null,
                            debugTrace = appendDebugLine(task.debugTrace, "WorkManager cancelled"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )

                        else -> task.copy(
                            status = DownloadStatus.CANCELED,
                            activeWorkId = null,
                            debugTrace = appendDebugLine(task.debugTrace, "WorkManager cancelled"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
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

    private suspend fun loadTaskOptions(taskId: String): DownloadOptions? {
        val optionsJson = downloadTaskStore.getCachedOptions(taskId) ?: return null
        return runCatching { json.decodeFromString<DownloadOptions>(optionsJson) }.getOrNull()
    }

    private suspend fun resolvePauseTargets(task: DownloadTask, taskOptions: DownloadOptions?): List<DownloadTask> {
        return resolvePlaylistGroup(task, taskOptions)
            ?.map { it.task }
            ?.filterNot { it.status.isTerminal }
            ?.ifEmpty { listOf(task) }
            ?: listOf(task)
    }

    private suspend fun resolveResumeTargets(task: DownloadTask, taskOptions: DownloadOptions): List<DownloadTaskWithOptions> {
        val playlistGroup = resolvePlaylistGroup(task, taskOptions)
        val targets = (playlistGroup ?: listOf(DownloadTaskWithOptions(task = task, options = taskOptions)))
            .filter { it.task.status == DownloadStatus.PAUSED }
        return targets.sortedBy { it.options.playlistItemIndex ?: Int.MAX_VALUE }
    }

    private suspend fun resolvePlaylistGroup(
        task: DownloadTask,
        taskOptions: DownloadOptions?,
    ): List<DownloadTaskWithOptions>? {
        if (taskOptions?.isPlaylistEnabled != true) return null
        val playlistFolderName = taskOptions.playlistFolderName ?: return null
        val matches = mutableListOf<DownloadTaskWithOptions>()

        downloadTaskStore.getAllTasks().forEach { queuedTask ->
            val queuedTaskOptions = loadTaskOptions(queuedTask.id) ?: return@forEach
            if (!queuedTaskOptions.isPlaylistEnabled) return@forEach
            if (queuedTaskOptions.url != taskOptions.url) return@forEach
            if (queuedTaskOptions.playlistFolderName != playlistFolderName) return@forEach
            matches += DownloadTaskWithOptions(
                task = queuedTask,
                options = queuedTaskOptions,
            )
        }

        return matches
            .sortedBy { it.options.playlistItemIndex ?: Int.MAX_VALUE }
            .takeIf { it.isNotEmpty() }
    }

    private fun syncPauseExpiryTimers(tasks: List<DownloadTask>) {
        val now = System.currentTimeMillis()
        val pausedTaskIds = mutableSetOf<String>()

        tasks.forEach { task ->
            val pauseExpiresAt = task.pauseExpiresAtEpochMs
            if (task.status != DownloadStatus.PAUSED || pauseExpiresAt == null) return@forEach
            pausedTaskIds += task.id

            if (pauseExpiresAt <= now) {
                if (pauseExpiryJobs[task.id]?.isActive != true) {
                    pauseExpiryJobs.remove(task.id)
                    pauseExpiryDeadlines.remove(task.id)
                    repositoryScope.launch { expirePausedDownload(task.id) }
                }
                return@forEach
            }

            val knownDeadline = pauseExpiryDeadlines[task.id]
            if (knownDeadline == pauseExpiresAt && pauseExpiryJobs[task.id]?.isActive == true) {
                return@forEach
            }

            pauseExpiryJobs.remove(task.id)?.cancel()
            pauseExpiryDeadlines[task.id] = pauseExpiresAt
            pauseExpiryJobs[task.id] = repositoryScope.launch {
                delay((pauseExpiresAt - System.currentTimeMillis()).coerceAtLeast(0L))
                expirePausedDownload(task.id)
            }
        }

        pauseExpiryJobs.keys
            .filter { it !in pausedTaskIds }
            .forEach { taskId ->
                pauseExpiryJobs.remove(taskId)?.cancel()
                pauseExpiryDeadlines.remove(taskId)
            }
    }

    private suspend fun expirePausedDownload(taskId: String) {
        val task = downloadTaskStore.getTask(taskId) ?: return
        val pauseExpiresAt = task.pauseExpiresAtEpochMs ?: return
        if (task.status != DownloadStatus.PAUSED || pauseExpiresAt > System.currentTimeMillis()) return

        val optionsJson = downloadTaskStore.getCachedOptions(taskId)
        val cleanedFileCount = optionsJson
            ?.let { serialized -> runCatching { json.decodeFromString<DownloadOptions>(serialized) }.getOrNull() }
            ?.let { options -> runCatching { fileUtils.deleteDownloadArtifacts(options.outputTemplate) }.getOrDefault(0) }
            ?: 0

        downloadTaskStore.clearCachedOptions(taskId)
        downloadTaskStore.update(taskId) { current ->
            current.copy(
                status = DownloadStatus.CANCELED,
                activeWorkId = null,
                pauseExpiresAtEpochMs = null,
                errorMessage = "Paused download expired after 10 minutes. Cached data was cleaned up.",
                debugTrace = appendDebugLine(
                    current.debugTrace,
                    "Pause expired after 10 minutes. Removed $cleanedFileCount cached download artifact(s).",
                ),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
        pauseExpiryJobs.remove(taskId)?.cancel()
        pauseExpiryDeadlines.remove(taskId)
    }

    private fun buildRenamedFileName(rawName: String, currentName: String): String {
        val currentExtension = currentName.substringAfterLast('.', "")
        val sanitized = fileUtils.sanitizeFileName(rawName.trim())
        if (sanitized.contains('.') || currentExtension.isBlank()) {
            return sanitized
        }
        return "$sanitized.$currentExtension"
    }

    private fun removeDownloadedTasks(taskIds: List<String>, deleteFromStorage: Boolean): Int {
        val removedIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()

        taskIds.forEach { taskId ->
            val task = downloadTaskStore.getTask(taskId) ?: return@forEach
            val outputPath = task.outputPath?.takeIf { it.isNotBlank() }
            val normalizedPath = outputPath?.let(fileUtils::normalizeLibraryOutputPath)
            val deletedOrMissing = when {
                !deleteFromStorage -> true
                normalizedPath == null -> true
                else -> {
                    val deletedPrimary = fileUtils.deleteManagedMediaBundle(normalizedPath)
                    val deletedLegacyPrivateCopy = if (outputPath != normalizedPath && outputPath != null) {
                        fileUtils.deleteManagedMediaBundle(outputPath)
                    } else {
                        true
                    }
                    deletedPrimary && deletedLegacyPrivateCopy
                }
            }
            if (deletedOrMissing) {
                removedIds += taskId
            } else {
                failedIds += taskId
            }
        }

        downloadTaskStore.removeMany(removedIds)

        if (failedIds.isNotEmpty()) {
            error("Unable to delete ${failedIds.size} saved file(s) from device storage.")
        }
        return removedIds.size
    }

    private companion object {
        const val MAX_DEBUG_TRACE_CHARS = 10_000
        const val PAUSE_RESUME_WINDOW_MS = 10 * 60 * 1000L
    }

    private fun isSupportedSubtitlePath(path: String): Boolean {
        return when (File(path).extension.lowercase()) {
            "srt", "vtt", "webvtt", "ass", "ssa", "ttml", "dfxp", "xml" -> true
            else -> false
        }
    }

    private data class PreparedDownload(
        val taskId: String,
        val request: OneTimeWorkRequest,
    )

    private data class DownloadTaskWithOptions(
        val task: DownloadTask,
        val options: DownloadOptions,
    )
}

private val DownloadStatus.isTerminal: Boolean
    get() = this in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELED)
