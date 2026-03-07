package com.localdownloader.data

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.CompressionRequest
import com.localdownloader.domain.models.ConversionRequest
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.downloader.FormatExtractor
import com.localdownloader.ffmpeg.Compressor
import com.localdownloader.ffmpeg.FormatConverter
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import com.localdownloader.worker.DownloadWorker
import com.localdownloader.worker.WorkerKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
) : DownloaderRepository {
    private val optionsByTaskId = ConcurrentHashMap<String, DownloadOptions>()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workObserverJobs = ConcurrentHashMap<String, Job>()

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
            // If the same URL has been queued before in this session, append "(n)" to the
            // filename so repeated downloads don't silently overwrite the previous file.
            val sameUrlCount = downloadTaskStore.countByUrl(options.url)
            val templateBase = if (sameUrlCount > 0) {
                fileUtils.appendCounterToTemplate(options.outputTemplate, sameUrlCount)
            } else {
                options.outputTemplate
            }
            val outputTemplate = fileUtils.createOutputTemplateWithDirectory(templateBase)
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(
                    workDataOf(
                        WorkerKeys.URL to options.url,
                        WorkerKeys.FORMAT_ID to options.formatId,
                        WorkerKeys.OUTPUT_TEMPLATE to outputTemplate,
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
                    ),
                )
                .build()

            val taskId = request.id.toString()
            optionsByTaskId[taskId] = options

            downloadTaskStore.upsert(
                DownloadTask(
                    id = taskId,
                    url = options.url,
                    title = titleHint.ifBlank { "Queued download" },
                    status = DownloadStatus.QUEUED,
                    debugTrace = "Queued: waiting for worker start",
                ),
            )

            workManager.enqueue(request)
            observeWorkState(taskId = taskId, workId = request.id)
            logger.i("DownloadRepository", "Queued download $taskId with outputTemplate=$outputTemplate")
            taskId
        }.onFailure { error ->
            logger.e("DownloadRepository", "enqueueDownload failed", error)
        }
    }

    override suspend fun pauseDownload(taskId: String) {
        logger.i("DownloadRepository", "pauseDownload taskId=$taskId")
        workManager.cancelWorkById(UUID.fromString(taskId))
        stopObserving(taskId)
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.PAUSED,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    override suspend fun resumeDownload(taskId: String): Result<String> {
        logger.i("DownloadRepository", "resumeDownload taskId=$taskId")
        val options = optionsByTaskId[taskId]
            ?: return Result.failure(IllegalStateException("No cached options for task $taskId"))

        val title = downloadTaskStore.getTask(taskId)?.title ?: "Resumed download"
        return enqueueDownload(options = options, titleHint = title)
    }

    override suspend fun cancelDownload(taskId: String) {
        logger.i("DownloadRepository", "cancelDownload taskId=$taskId")
        workManager.cancelWorkById(UUID.fromString(taskId))
        stopObserving(taskId)
        downloadTaskStore.update(taskId) { task ->
            task.copy(
                status = DownloadStatus.CANCELED,
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    override fun observeDownloadQueue(): Flow<List<DownloadTask>> = downloadTaskStore.observeAll()

    override suspend fun convertMedia(request: ConversionRequest, onProgress: ((Float) -> Unit)?): Result<String> {
        return formatConverter.convert(request, onProgress)
    }

    override suspend fun compressMedia(request: CompressionRequest, onProgress: ((Float) -> Unit)?): Result<String> {
        return compressor.compress(request, onProgress)
    }

    override fun observeSettings(): Flow<AppSettings> = settingsStore.observeSettings()

    override suspend fun updateSettings(settings: AppSettings) {
        settingsStore.updateSettings(settings)
    }

    private fun observeWorkState(taskId: String, workId: UUID) {
        stopObserving(taskId)
        val job = repositoryScope.launch {
            var lastState: WorkInfo.State? = null
            workManager.getWorkInfoByIdFlow(workId).collect { info ->
                if (info == null) {
                    appendTaskDebug(taskId, "WorkManager state unavailable")
                    return@collect
                }

                val state = info.state
                if (state != lastState) {
                    logger.i("DownloadRepository", "WorkManager state taskId=$taskId state=$state")
                    appendTaskDebug(
                        taskId,
                        "WorkManager state: $state (attempt=${info.runAttemptCount}, id=${info.id})",
                    )
                    syncTaskFromWorkState(taskId, info)
                    lastState = state
                }
            }
        }
        workObserverJobs[taskId] = job
    }

    private fun syncTaskFromWorkState(taskId: String, info: WorkInfo) {
        when (info.state) {
            WorkInfo.State.ENQUEUED -> {
                downloadTaskStore.update(taskId) { task ->
                    if (task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED || task.status == DownloadStatus.CANCELED) {
                        task
                    } else {
                        task.copy(
                            status = DownloadStatus.QUEUED,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                }
            }

            WorkInfo.State.RUNNING -> {
                downloadTaskStore.update(taskId) { task ->
                    if (task.status == DownloadStatus.COMPLETED || task.status == DownloadStatus.FAILED || task.status == DownloadStatus.CANCELED) {
                        task
                    } else {
                        task.copy(
                            status = DownloadStatus.RUNNING,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    }
                }
            }

            WorkInfo.State.SUCCEEDED -> {
                val outputPath = info.outputData.getString(WorkerKeys.OUTPUT_PATH)
                downloadTaskStore.update(taskId) { task ->
                    if (task.status == DownloadStatus.COMPLETED) {
                        task
                    } else {
                        task.copy(
                            status = DownloadStatus.COMPLETED,
                            progressPercent = if (task.progressPercent > 0) task.progressPercent else 100,
                            outputPath = outputPath ?: task.outputPath,
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
                    task.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = failureMessage,
                        debugTrace = appendDebugLine(
                            existing = task.debugTrace,
                            line = "WorkManager failed: $failureMessage",
                        ),
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }

            WorkInfo.State.CANCELLED -> {
                downloadTaskStore.update(taskId) { task ->
                    task.copy(
                        status = DownloadStatus.CANCELED,
                        debugTrace = appendDebugLine(
                            existing = task.debugTrace,
                            line = "WorkManager cancelled",
                        ),
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
                debugTrace = appendDebugLine(existing = task.debugTrace, line = entry),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    private fun appendDebugLine(existing: String?, line: String): String {
        return if (existing.isNullOrBlank()) {
            line.takeLast(MAX_DEBUG_TRACE_CHARS)
        } else {
            (existing + "\n" + line).takeLast(MAX_DEBUG_TRACE_CHARS)
        }
    }

    private fun stopObserving(taskId: String) {
        workObserverJobs.remove(taskId)?.cancel()
    }

    private companion object {
        const val MAX_DEBUG_TRACE_CHARS = 10_000
    }
}
