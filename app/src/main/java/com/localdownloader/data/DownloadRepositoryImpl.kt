package com.localdownloader.data

import android.content.Context
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
import com.localdownloader.domain.models.PlaylistDownloadRequest
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.downloader.FormatExtractor
import com.localdownloader.notifications.AppNotifications
import com.localdownloader.ffmpeg.Compressor
import com.localdownloader.ffmpeg.FormatConverter
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import com.localdownloader.utils.SensitiveDataSanitizer
import com.localdownloader.worker.WorkerKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val formatExtractor: FormatExtractor,
    private val downloadTaskStore: DownloadTaskStore,
    private val formatConverter: FormatConverter,
    private val compressor: Compressor,
    private val settingsStore: SettingsStore,
    private val workManager: WorkManager,
    private val fileUtils: FileUtils,
    private val downloadOptionSecretsStore: DownloadOptionSecretsStore,
    private val logger: Logger,
) : DownloaderRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workObserverJobs = ConcurrentHashMap<String, Job>()
    private val pauseExpiryJobs = ConcurrentHashMap<String, Job>()
    private val pauseExpiryDeadlines = ConcurrentHashMap<String, Long>()
    private val schedulingMutex = Mutex()
    private val historyCleanupMutex = Mutex()

    init {
        repositoryScope.launch {
            downloadTaskStore.observeAll().collect { tasks ->
                syncPauseExpiryTimers(tasks)
            }
        }
        repositoryScope.launch {
            downloadTaskStore.awaitInitialLoad()
            while (true) {
                delay(FAILED_HISTORY_PRUNE_INTERVAL_MS)
                runCatching {
                    pruneFailedAndCanceledHistory(
                        settingsStore.observeSettings().first().downloadHistoryRetentionDays,
                    )
                }.onFailure { error ->
                    logger.w("DownloadRepository", "Periodic failed history prune failed", error)
                }
            }
        }
        repositoryScope.launch {
            downloadTaskStore.awaitInitialLoad()
            migratePersistedTaskSecrets()
            pruneFailedAndCanceledHistory(settingsStore.observeSettings().first().downloadHistoryRetentionDays)
            refillQueuedDownloads()
        }
    }

    override suspend fun analyzeUrl(
        url: String,
        cookiesPath: String?,
        userAgent: String?,
    ): Result<VideoInfo> {
        logger.i("DownloadRepository", "analyzeUrl called for: $url")
        val result = formatExtractor.analyze(
            url = url,
            cookiesPath = cookiesPath,
            userAgent = userAgent,
        )
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
            scheduleDownloadStart(
                taskId = taskId,
                options = options.copy(outputTemplate = outputTemplate),
                titleHint = titleHint,
            )
            logger.i("DownloadRepository", "Queued download $taskId with outputTemplate=$outputTemplate")
            taskId
        }.onFailure { error ->
            logger.e("DownloadRepository", "enqueueDownload failed", error)
        }
    }

    override suspend fun enqueuePlaylistDownload(
        playlistTitle: String,
        requests: List<PlaylistDownloadRequest>,
    ): Result<List<String>> {
        return runCatching {
            require(requests.isNotEmpty()) { "Select at least one playlist item to queue." }

            val playlistDirectory = fileUtils.createUniquePlaylistDirectory(playlistTitle)
            schedulingMutex.withLock {
                val settings = settingsStore.observeSettings().first()
                val requiredNetworkType = requiredNetworkTypeFor(settings)
                val queuedDownloads = requests.map { request ->
                    val entry = request.entry
                    val itemOutputTemplate = fileUtils.buildPlaylistItemOutputTemplate(
                        playlistDirectory = playlistDirectory,
                        baseTemplate = request.options.outputTemplate,
                        playlistItemIndex = entry.playlistItemIndex,
                    )
                    prepareDownload(
                        taskId = UUID.randomUUID().toString(),
                        options = request.options.copy(
                            outputTemplate = itemOutputTemplate,
                            thumbnailUrl = entry.thumbnailUrl ?: request.options.thumbnailUrl,
                            isPlaylistEnabled = true,
                            playlistItemIndex = entry.playlistItemIndex,
                            playlistFolderName = playlistDirectory.name,
                        ),
                        titleHint = request.titleHint,
                        requiredNetworkType = requiredNetworkType,
                        assignWorkId = false,
                    )
                }
                fillAvailableDownloadSlotsLocked(settings)
                queuedDownloads.map { it.taskId }
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "enqueuePlaylistDownload failed", error)
        }
    }

    override suspend fun pauseDownload(taskId: String) {
        logger.i("DownloadRepository", "pauseDownload taskId=$taskId")
        val task = downloadTaskStore.getTask(taskId) ?: return
        val pauseExpiresAt = System.currentTimeMillis() + PAUSE_RESUME_WINDOW_MS

        downloadTaskStore.update(task.id) { current ->
            if (current.status.isTerminal || current.status == DownloadStatus.PAUSED) {
                current
            } else {
                current.copy(
                    status = DownloadStatus.PAUSED,
                    pauseExpiresAtEpochMs = pauseExpiresAt,
                    errorMessage = null,
                    debugTrace = appendDebugLine(
                        current.debugTrace,
                        "Paused by user. Resume available for ${PAUSE_RESUME_WINDOW_MS / 60_000} minutes before cleanup.",
                    ),
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
        }
        task.activeWorkId?.let { workManager.cancelWorkById(UUID.fromString(it)) }
    }

    override suspend fun resumeDownload(taskId: String): Result<String> {
        logger.i("DownloadRepository", "resumeDownload taskId=$taskId")

        val task = downloadTaskStore.getTask(taskId)
            ?: return Result.failure(IllegalStateException("No task found with ID $taskId"))

        if (task.status != DownloadStatus.PAUSED) {
            return Result.failure(IllegalStateException("Only paused downloads can be resumed."))
        }
        if (!task.activeWorkId.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Pause is still finishing. Please try resume again in a moment."))
        }

        val taskOptions = loadTaskOptions(taskId)
            ?: return Result.failure(IllegalStateException("No cached download options for task $taskId"))

        if (task.pauseExpiresAtEpochMs?.let { it <= System.currentTimeMillis() } == true) {
            expirePausedDownload(task.id)
            return Result.failure(IllegalStateException("Sorry, this paused download expired after 10 minutes. Its cached data was removed."))
        }

        return runCatching {
            if (taskOptions.isPlaylistEnabled) {
                downloadTaskStore.update(task.id) { current ->
                    current.copy(
                        status = DownloadStatus.QUEUED,
                        activeWorkId = null,
                        pauseExpiresAtEpochMs = null,
                        errorMessage = null,
                        debugTrace = appendDebugLine(current.debugTrace, "Resume requested: returning item to the playlist queue"),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            } else {
                scheduleDownloadStart(
                    taskId = task.id,
                    options = taskOptions,
                    titleHint = task.title,
                    existingTask = task,
                )
            }
            refillQueuedDownloads()
            taskId
        }.onFailure { error ->
            logger.e("DownloadRepository", "resumeDownload failed taskId=$taskId", error)
        }
    }

    override suspend fun retryDownload(taskId: String): Result<String> {
        logger.i("DownloadRepository", "retryDownload taskId=$taskId")

        val task = downloadTaskStore.getTask(taskId)
            ?: return Result.failure(IllegalStateException("No task found with ID $taskId"))

        if (task.status != DownloadStatus.FAILED && task.status != DownloadStatus.CANCELED) {
            return Result.failure(IllegalStateException("Only failed or canceled downloads can be retried."))
        }

        val taskOptions = loadTaskOptions(taskId)
            ?: return Result.failure(IllegalStateException("No cached download options for task $taskId"))

        return runCatching {
            val retryTask = task.copy(
                progressPercent = 0,
                speed = null,
                eta = null,
                outputPath = null,
                downloadedStr = null,
                totalSizeStr = null,
                errorMessage = null,
                pauseExpiresAtEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
            if (taskOptions.isPlaylistEnabled) {
                downloadTaskStore.update(task.id) { current ->
                    current.copy(
                        status = DownloadStatus.QUEUED,
                        activeWorkId = null,
                        progressPercent = 0,
                        speed = null,
                        eta = null,
                        outputPath = null,
                        downloadedStr = null,
                        totalSizeStr = null,
                        errorMessage = null,
                        pauseExpiresAtEpochMs = null,
                        debugTrace = appendDebugLine(current.debugTrace, "Retry requested: returning item to the playlist queue"),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            } else {
                scheduleDownloadStart(
                    taskId = task.id,
                    options = taskOptions,
                    titleHint = task.title,
                    existingTask = retryTask,
                )
            }
            refillQueuedDownloads()
            taskId
        }.onFailure { error ->
            logger.e("DownloadRepository", "retryDownload failed taskId=$taskId", error)
        }
    }

    override suspend fun cancelDownload(taskId: String) {
        logger.i("DownloadRepository", "cancelDownload taskId=$taskId")
        val existingTask = downloadTaskStore.getTask(taskId)
        val activeWorkId = existingTask?.activeWorkId
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.CANCELED,
                activeWorkId = task.activeWorkId,
                pauseExpiresAtEpochMs = null,
                debugTrace = appendDebugLine(task.debugTrace, "Canceled by user"),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
        if (settingsStore.observeSettings().first().notifyCanceledDownloads) {
            existingTask?.title?.takeIf { it.isNotBlank() }?.let { title ->
                AppNotifications.showDownloadCanceled(
                    context = appContext,
                    taskId = taskId,
                    title = title,
                )
            }
        }
        activeWorkId?.let { workManager.cancelWorkById(UUID.fromString(it)) }
        if (activeWorkId == null) {
            refillQueuedDownloads()
        }
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

    override suspend fun removeDownloadedFilesFromLibrary(taskIds: List<String>): Result<Int> {
        return runCatching {
            val ids = taskIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            require(ids.isNotEmpty()) { "Select at least one saved item." }

            removeDownloadedTasks(ids, deleteFromStorage = false)
        }.onFailure { error ->
            logger.e(
                "DownloadRepository",
                "removeDownloadedFilesFromLibrary failed taskIds=${taskIds.size}",
                error,
            )
        }
    }

    override suspend fun permanentlyDeleteDownloadedFiles(taskIds: List<String>): Result<Int> {
        return runCatching {
            val ids = taskIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            require(ids.isNotEmpty()) { "Select at least one saved item." }

            removeDownloadedTasks(ids, deleteFromStorage = true)
        }.onFailure { error ->
            logger.e(
                "DownloadRepository",
                "permanentlyDeleteDownloadedFiles failed taskIds=${taskIds.size}",
                error,
            )
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

    override suspend fun clearFailedAndCanceledHistory(): Result<Int> {
        return runCatching {
            historyCleanupMutex.withLock {
                val historyTaskIds = downloadTaskStore.getAllTasks()
                    .asSequence()
                    .filter { it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELED }
                    .map { it.id }
                    .toList()
                if (historyTaskIds.isEmpty()) return@withLock 0
                downloadOptionSecretsStore.clear(historyTaskIds)
                downloadTaskStore.removeMany(historyTaskIds)
                historyTaskIds.size
            }
        }.onFailure { error ->
            logger.e("DownloadRepository", "clearFailedAndCanceledHistory failed", error)
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
                val missingIds = missingTasks.map { it.id }
                downloadOptionSecretsStore.clear(missingIds)
                downloadTaskStore.removeMany(missingIds)
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
        val previous = settingsStore.observeSettings().first()
        settingsStore.updateSettings(settings)
        if (previous.downloadHistoryRetentionDays != settings.downloadHistoryRetentionDays) {
            pruneFailedAndCanceledHistory(settings.downloadHistoryRetentionDays)
        }
        if (previous.maxConcurrentDownloads != settings.maxConcurrentDownloads ||
            previous.allowMeteredDownloads != settings.allowMeteredDownloads
        ) {
            refreshQueuedScheduling("Queue scheduling refreshed after download settings changed")
        }
    }

    override suspend fun refillQueuedDownloads() {
        schedulingMutex.withLock {
            fillAvailableDownloadSlotsLocked(settingsStore.observeSettings().first())
        }
    }

    private fun prepareDownload(
        taskId: String,
        options: DownloadOptions,
        titleHint: String,
        requiredNetworkType: NetworkType,
        existingTask: DownloadTask? = null,
        assignWorkId: Boolean = true,
    ): PreparedDownload {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    WorkerKeys.TASK_ID to taskId,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(requiredNetworkType)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        val queuedTask = DownloadTask(
            id = taskId,
            url = options.url,
            title = titleHint.ifBlank { existingTask?.title ?: "Queued download" },
            status = DownloadStatus.QUEUED,
            activeWorkId = if (assignWorkId) request.id.toString() else null,
            progressPercent = existingTask?.progressPercent ?: 0,
            speed = existingTask?.speed,
            eta = existingTask?.eta,
            outputPath = existingTask?.outputPath,
            thumbnailUrl = existingTask?.thumbnailUrl ?: options.thumbnailUrl,
            downloadedStr = existingTask?.downloadedStr,
            totalSizeStr = existingTask?.totalSizeStr,
            errorMessage = null,
            debugTrace = appendDebugLine(
                existingTask?.debugTrace,
                if (assignWorkId) {
                    "Queued: waiting for worker start"
                } else {
                    "Queued: waiting for an available slot"
                },
            ),
            pauseExpiresAtEpochMs = null,
            createdAtEpochMs = existingTask?.createdAtEpochMs ?: System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis(),
        )

        val persistedOptionsJson = downloadOptionSecretsStore.persist(taskId, options)
        downloadTaskStore.upsert(
            task = queuedTask,
            optionsJson = persistedOptionsJson,
        )

        return PreparedDownload(
            taskId = taskId,
            request = request,
            options = options,
        )
    }

    private fun enqueuePreparedDownload(prepared: PreparedDownload) {
        workManager.enqueue(prepared.request)
        observeWorkState(taskId = prepared.taskId, workId = prepared.request.id)
    }

    private suspend fun scheduleDownloadStart(
        taskId: String,
        options: DownloadOptions,
        titleHint: String,
        existingTask: DownloadTask? = null,
    ) {
        schedulingMutex.withLock {
            val settings = settingsStore.observeSettings().first()
            val shouldStartNow = countOccupiedSlots() < settings.maxConcurrentDownloads
            val prepared = prepareDownload(
                taskId = taskId,
                options = options,
                titleHint = titleHint,
                requiredNetworkType = requiredNetworkTypeFor(settings),
                existingTask = existingTask,
                assignWorkId = shouldStartNow,
            )
            if (shouldStartNow) {
                enqueuePreparedDownload(prepared)
            }
        }
    }

    private suspend fun refreshQueuedScheduling(reason: String) {
        schedulingMutex.withLock {
            val queuedWithAssignedWork = downloadTaskStore.getAllTasks()
                .filter { task ->
                    task.status == DownloadStatus.QUEUED &&
                        !task.activeWorkId.isNullOrBlank()
                }

            queuedWithAssignedWork.forEach { task ->
                val workId = task.activeWorkId ?: return@forEach
                downloadTaskStore.update(task.id) { current ->
                    current.copy(
                        activeWorkId = null,
                        debugTrace = appendDebugLine(current.debugTrace, reason),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
                runCatching { workManager.cancelWorkById(UUID.fromString(workId)) }
            }

            fillAvailableDownloadSlotsLocked(settingsStore.observeSettings().first())
        }
    }

    private suspend fun fillAvailableDownloadSlotsLocked(settings: AppSettings) {
        var availableSlots = settings.maxConcurrentDownloads - countOccupiedSlots()
        if (availableSlots <= 0) return

        val queuedCandidates = downloadTaskStore.getAllTasks()
            .filter { task ->
                task.status == DownloadStatus.QUEUED &&
                    task.activeWorkId.isNullOrBlank()
            }
            .mapNotNull { task ->
                val options = loadTaskOptions(task.id) ?: return@mapNotNull null
                DownloadTaskWithOptions(task = task, options = options)
            }
            .sortedWith(
                compareBy<DownloadTaskWithOptions> { it.task.createdAtEpochMs }
                    .thenBy { it.options.playlistItemIndex ?: Int.MAX_VALUE }
                    .thenBy { it.task.title.lowercase() },
            )

        queuedCandidates.forEach { queued ->
            if (availableSlots <= 0) return
            val prepared = prepareDownload(
                taskId = queued.task.id,
                options = queued.options,
                titleHint = queued.task.title,
                requiredNetworkType = requiredNetworkTypeFor(settings),
                existingTask = queued.task,
                assignWorkId = true,
            )
            enqueuePreparedDownload(prepared)
            availableSlots -= 1
        }
    }

    private fun requiredNetworkTypeFor(settings: AppSettings): NetworkType {
        return if (settings.allowMeteredDownloads) {
            NetworkType.CONNECTED
        } else {
            NetworkType.UNMETERED
        }
    }

    private fun countOccupiedSlots(): Int {
        return downloadTaskStore.getAllTasks().count { task ->
            !task.status.isTerminal &&
                task.status != DownloadStatus.PAUSED &&
                !task.activeWorkId.isNullOrBlank()
        }
    }

    private fun observeWorkState(taskId: String, workId: UUID) {
        workObserverJobs.remove(taskId)?.cancel()
        val workIdValue = workId.toString()
        lateinit var observerJob: Job
        observerJob = repositoryScope.launch {
            try {
                workManager.getWorkInfoByIdFlow(workId).collect { info ->
                    val trackedTask = downloadTaskStore.getTask(taskId)
                    if (trackedTask?.activeWorkId != workIdValue) {
                        cancel("Task no longer tracks work $workIdValue")
                        return@collect
                    }
                    if (info == null) {
                        appendTaskDebug(taskId, "WorkManager state unavailable")
                        return@collect
                    }
                    syncTaskFromWorkState(taskId, workIdValue, info)
                    if (info.state.isFinished) {
                        cancel("Observed WorkManager terminal state for $workIdValue")
                    }
                }
            } finally {
                workObserverJobs.remove(taskId, observerJob)
            }
        }
        workObserverJobs[taskId] = observerJob
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
                            debugTrace = ensureDebugLine(task.debugTrace, "Task failed: $failureMessage"),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                    triggerQueuedDownloadRefill()
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
                            debugTrace = ensureDebugLine(task.debugTrace, "Task canceled"),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                    triggerQueuedDownloadRefill()
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
                    downloadTaskStore.clearCachedOptions(taskId)
                    downloadOptionSecretsStore.clear(taskId)
                    triggerQueuedDownloadRefill()
                }
            }

            WorkInfo.State.FAILED -> {
                val failureMessage = info.outputData.getString(WorkerKeys.ERROR_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Download failed before worker returned an explicit error"
                var shouldAdvancePlaylist = false
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
                            debugTrace = ensureDebugLine(task.debugTrace, "Task canceled"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        ).also { shouldAdvancePlaylist = true }

                        else -> task.copy(
                            status = DownloadStatus.FAILED,
                            activeWorkId = null,
                            errorMessage = failureMessage,
                            debugTrace = ensureDebugLine(task.debugTrace, "Task failed: $failureMessage"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        ).also { shouldAdvancePlaylist = true }
                    }
                }
                if (shouldAdvancePlaylist) {
                    triggerQueuedDownloadRefill()
                }
            }

            WorkInfo.State.CANCELLED -> {
                var shouldAdvancePlaylist = false
                downloadTaskStore.update(taskId) { task ->
                    when {
                        task.status == DownloadStatus.PAUSED || task.pauseExpiresAtEpochMs != null -> task.copy(
                            activeWorkId = null,
                            debugTrace = appendDebugLine(task.debugTrace, "Worker cancelled after pause request"),
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )

                        task.status == DownloadStatus.CANCELED -> task.copy(
                            activeWorkId = null,
                            debugTrace = ensureDebugLine(task.debugTrace, "Task canceled"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        ).also { shouldAdvancePlaylist = true }

                        else -> task.copy(
                            status = DownloadStatus.CANCELED,
                            activeWorkId = null,
                            debugTrace = ensureDebugLine(task.debugTrace, "Task canceled"),
                            pauseExpiresAtEpochMs = null,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        ).also { shouldAdvancePlaylist = true }
                    }
                }
                if (shouldAdvancePlaylist) {
                    triggerQueuedDownloadRefill()
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
            if (task.status.isTerminal) {
                task
            } else {
                task.copy(
                    debugTrace = appendDebugLine(task.debugTrace, entry),
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            }
        }
    }

    private fun appendDebugLine(existing: String?, line: String): String {
        val sanitized = SensitiveDataSanitizer.sanitize(line)
        val combined = if (existing.isNullOrBlank()) sanitized else "$existing\n$sanitized"
        return combined.takeLast(MAX_DEBUG_TRACE_CHARS)
    }

    private fun ensureDebugLine(existing: String?, line: String): String {
        return if (existing.isNullOrBlank()) {
            appendDebugLine(existing, line)
        } else {
            existing
        }
    }

    private suspend fun loadTaskOptions(taskId: String): DownloadOptions? {
        val optionsJson = downloadTaskStore.getCachedOptions(taskId) ?: return null
        return downloadOptionSecretsStore.hydrate(taskId, optionsJson)
    }

    private fun triggerQueuedDownloadRefill() {
        repositoryScope.launch {
            refillQueuedDownloads()
        }
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

        loadTaskOptions(taskId)
            ?.let { options -> runCatching { fileUtils.deleteDownloadArtifacts(options.outputTemplate) } }

        downloadTaskStore.clearCachedOptions(taskId)
        downloadOptionSecretsStore.clear(taskId)
        downloadTaskStore.update(taskId) { current ->
            current.copy(
                status = DownloadStatus.CANCELED,
                activeWorkId = null,
                pauseExpiresAtEpochMs = null,
                errorMessage = "Paused download expired after 10 minutes. Cached data was cleaned up.",
                debugTrace = appendDebugLine(
                    current.debugTrace,
                    "Paused download expired after 10 minutes. Cached data was cleaned up.",
                ),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
        pauseExpiryJobs.remove(taskId)?.cancel()
        pauseExpiryDeadlines.remove(taskId)
    }

    private suspend fun migratePersistedTaskSecrets() {
        downloadTaskStore.getAllTasks().forEach { task ->
            if (task.status == DownloadStatus.COMPLETED) {
                if (downloadTaskStore.getCachedOptions(task.id) != null) {
                    downloadTaskStore.clearCachedOptions(task.id)
                }
                downloadOptionSecretsStore.clear(task.id)
                return@forEach
            }

            val optionsJson = downloadTaskStore.getCachedOptions(task.id) ?: return@forEach
            val migratedJson = downloadOptionSecretsStore.migratePersistedOptions(task.id, optionsJson)
            if (migratedJson != null && migratedJson != optionsJson) {
                downloadTaskStore.cacheOptions(task.id, migratedJson)
            }
        }
    }

    private suspend fun pruneFailedAndCanceledHistory(retentionDays: Int): Int {
        return historyCleanupMutex.withLock {
            val normalizedDays = retentionDays.coerceIn(MIN_FAILED_HISTORY_RETENTION_DAYS, MAX_FAILED_HISTORY_RETENTION_DAYS)
            val cutoff = System.currentTimeMillis() - normalizedDays * DAY_IN_MILLIS
            val staleTaskIds = downloadTaskStore.getAllTasks()
                .asSequence()
                .filter {
                    (it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELED) &&
                        it.updatedAtEpochMs < cutoff
                }
                .map { it.id }
                .toList()
            if (staleTaskIds.isEmpty()) return@withLock 0
            downloadOptionSecretsStore.clear(staleTaskIds)
            downloadTaskStore.removeMany(staleTaskIds)
            staleTaskIds.size
        }
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

        downloadOptionSecretsStore.clear(removedIds)
        downloadTaskStore.removeMany(removedIds)

        if (failedIds.isNotEmpty()) {
            error("Unable to delete ${failedIds.size} saved file(s) from device storage.")
        }
        return removedIds.size
    }

    private companion object {
        const val MAX_DEBUG_TRACE_CHARS = 250_000
        const val PAUSE_RESUME_WINDOW_MS = 10 * 60 * 1000L
        const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
        const val MIN_FAILED_HISTORY_RETENTION_DAYS = 7
        const val MAX_FAILED_HISTORY_RETENTION_DAYS = 180
        const val FAILED_HISTORY_PRUNE_INTERVAL_MS = 12L * 60L * 60L * 1000L
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
        val options: DownloadOptions,
    )

    private data class DownloadTaskWithOptions(
        val task: DownloadTask,
        val options: DownloadOptions,
    )
}

private val DownloadStatus.isTerminal: Boolean
    get() = this in setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELED)
