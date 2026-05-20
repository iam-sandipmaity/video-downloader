package com.localdownloader.data

import com.localdownloader.data.persistence.DownloadTaskDao
import com.localdownloader.data.persistence.DownloadTaskEntity
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTaskStore @Inject constructor(
    private val dao: DownloadTaskDao,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeOperations = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val pendingTaskWrites = ConcurrentHashMap<String, Long>()
    private val pendingTaskRemovals = ConcurrentHashMap.newKeySet<String>()
    private val pendingOptionWrites = ConcurrentHashMap<String, String>()
    private val pendingOptionClears = ConcurrentHashMap.newKeySet<String>()
    private val initialLoad = CompletableDeferred<Unit>()
    @Volatile
    private var clearAllPending = false

    /** In-memory cache synced from Room for fast non-suspending access. */
    private val tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    private val cachedOptions = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        writerScope.launch {
            for (operation in writeOperations) {
                operation()
            }
        }
        scope.launch {
            dao.observeAll().collect { entities ->
                if (!initialLoad.isCompleted) {
                    initialLoad.complete(Unit)
                }

                if (clearAllPending) {
                    if (entities.isEmpty()) {
                        clearAllPending = false
                    }
                    tasks.update { emptyMap() }
                    cachedOptions.update { emptyMap() }
                    return@collect
                }

                val incomingTasks = entities.mapNotNull { it.toDomainTask(json) }.associateBy { it.id }
                val incomingOptions = entities.mapNotNull { entity ->
                    entity.optionsJson?.let { entity.id to it }
                }.toMap()
                var mergedTasks: Map<String, DownloadTask> = emptyMap()
                tasks.update { current ->
                    mergeTaskSnapshot(current = current, incoming = incomingTasks)
                        .also { mergedTasks = it }
                }
                cachedOptions.update { current ->
                    mergeOptionsSnapshot(
                        current = current,
                        activeTaskIds = mergedTasks.keys,
                        incoming = incomingOptions,
                    )
                }
            }
        }
    }

    fun observeAll(): Flow<List<DownloadTask>> {
        return tasks.map { taskMap ->
            taskMap.values.sortedByDescending { it.createdAtEpochMs }
        }
    }

    fun getAllTasks(): List<DownloadTask> = tasks.value.values.toList()

    fun getTask(taskId: String): DownloadTask? = tasks.value[taskId]

    fun upsert(task: DownloadTask, optionsJson: String? = null) {
        clearAllPending = false
        pendingTaskRemovals.remove(task.id)
        pendingTaskWrites[task.id] = task.updatedAtEpochMs
        tasks.update { taskMap -> taskMap + (task.id to task) }
        if (optionsJson != null) {
            pendingOptionClears.remove(task.id)
            pendingOptionWrites[task.id] = optionsJson
            cachedOptions.update { options -> options + (task.id to optionsJson) }
        }
        enqueueWrite {
            dao.upsert(task.toEntity(json = json, optionsJson = optionsJson))
        }
    }

    fun update(taskId: String, reducer: (DownloadTask) -> DownloadTask) {
        tasks.update { taskMap ->
            val current = taskMap[taskId] ?: return@update taskMap
            val updated = reducer(current)
            clearAllPending = false
            pendingTaskRemovals.remove(taskId)
            pendingTaskWrites[taskId] = updated.updatedAtEpochMs
            enqueueWrite {
                val existingOptionsJson = cachedOptions.value[taskId] ?: dao.getById(taskId)?.optionsJson
                dao.upsert(updated.toEntity(json = json, optionsJson = existingOptionsJson))
            }
            taskMap + (taskId to updated)
        }
    }

    fun countByUrl(url: String): Int = tasks.value.values.count { it.url == url }

    fun cacheOptions(taskId: String, optionsJson: String) {
        pendingTaskRemovals.remove(taskId)
        pendingOptionClears.remove(taskId)
        pendingOptionWrites[taskId] = optionsJson
        cachedOptions.update { options -> options + (taskId to optionsJson) }
        enqueueWrite {
            dao.getById(taskId)?.let { entity ->
                dao.upsert(entity.copy(optionsJson = optionsJson))
            }
        }
    }

    fun clearCachedOptions(taskId: String) {
        pendingOptionWrites.remove(taskId)
        pendingOptionClears.add(taskId)
        cachedOptions.update { options -> options - taskId }
        enqueueWrite {
            dao.getById(taskId)?.let { entity ->
                dao.upsert(entity.copy(optionsJson = null))
            }
        }
    }

    fun remove(taskId: String) {
        pendingTaskWrites.remove(taskId)
        pendingTaskRemovals.add(taskId)
        pendingOptionWrites.remove(taskId)
        pendingOptionClears.remove(taskId)
        tasks.update { taskMap -> taskMap - taskId }
        cachedOptions.update { options -> options - taskId }
        enqueueWrite { dao.deleteById(taskId) }
    }

    fun removeMany(taskIds: Collection<String>) {
        if (taskIds.isEmpty()) return
        val ids = taskIds.toSet()
        ids.forEach { taskId ->
            pendingTaskWrites.remove(taskId)
            pendingTaskRemovals.add(taskId)
            pendingOptionWrites.remove(taskId)
            pendingOptionClears.remove(taskId)
        }
        tasks.update { taskMap -> taskMap - ids }
        cachedOptions.update { options -> options - ids }
        enqueueWrite {
            ids.forEach { dao.deleteById(it) }
        }
    }

    fun clearAll() {
        clearAllPending = true
        pendingTaskWrites.clear()
        pendingTaskRemovals.clear()
        pendingOptionWrites.clear()
        pendingOptionClears.clear()
        tasks.update { emptyMap() }
        cachedOptions.update { emptyMap() }
        enqueueWrite { dao.deleteAll() }
    }

    suspend fun getCachedOptions(taskId: String): String? {
        return cachedOptions.value[taskId] ?: dao.getById(taskId)?.optionsJson
    }

    suspend fun awaitInitialLoad() {
        initialLoad.await()
    }

    private fun enqueueWrite(operation: suspend () -> Unit) {
        check(writeOperations.trySend(operation).isSuccess) {
            "DownloadTaskStore write queue is unavailable"
        }
    }

    private fun mergeTaskSnapshot(
        current: Map<String, DownloadTask>,
        incoming: Map<String, DownloadTask>,
    ): Map<String, DownloadTask> {
        val merged = LinkedHashMap<String, DownloadTask>(maxOf(current.size, incoming.size))

        incoming.forEach { (taskId, incomingTask) ->
            if (taskId in pendingTaskRemovals) {
                return@forEach
            }

            val currentTask = current[taskId]
            val pendingUpdatedAt = pendingTaskWrites[taskId]
            val keepCurrent = when {
                currentTask == null -> false
                pendingUpdatedAt != null && incomingTask.updatedAtEpochMs < pendingUpdatedAt -> true
                currentTask.updatedAtEpochMs > incomingTask.updatedAtEpochMs -> true
                currentTask.updatedAtEpochMs == incomingTask.updatedAtEpochMs &&
                    !currentTask.activeWorkId.isNullOrBlank() &&
                    incomingTask.activeWorkId.isNullOrBlank() -> true
                else -> false
            }

            if (keepCurrent) {
                merged[taskId] = currentTask
            } else {
                merged[taskId] = incomingTask
                if (pendingUpdatedAt != null && incomingTask.updatedAtEpochMs >= pendingUpdatedAt) {
                    pendingTaskWrites.remove(taskId, pendingUpdatedAt)
                }
            }
        }

        current.forEach { (taskId, currentTask) ->
            if (taskId in incoming) return@forEach
            when {
                taskId in pendingTaskRemovals -> Unit
                pendingTaskWrites.containsKey(taskId) -> merged[taskId] = currentTask
                else -> Unit
            }
        }

        pendingTaskRemovals.toList()
            .filter { taskId -> taskId !in incoming && taskId !in current }
            .forEach { taskId -> pendingTaskRemovals.remove(taskId) }

        pendingTaskWrites.keys.toList()
            .filter { taskId -> taskId !in merged }
            .forEach { taskId -> pendingTaskWrites.remove(taskId) }

        return merged
    }

    private fun mergeOptionsSnapshot(
        current: Map<String, String>,
        activeTaskIds: Set<String>,
        incoming: Map<String, String>,
    ): Map<String, String> {
        val merged = LinkedHashMap<String, String>(activeTaskIds.size)

        activeTaskIds.forEach { taskId ->
            if (taskId in pendingTaskRemovals) {
                return@forEach
            }

            if (taskId in pendingOptionClears) {
                if (taskId !in incoming) {
                    pendingOptionClears.remove(taskId)
                }
                return@forEach
            }

            val expectedOption = pendingOptionWrites[taskId]
            val currentOption = current[taskId]
            val incomingOption = incoming[taskId]

            when {
                expectedOption != null -> {
                    if (incomingOption == expectedOption) {
                        pendingOptionWrites.remove(taskId, expectedOption)
                        merged[taskId] = incomingOption
                    } else {
                        merged[taskId] = currentOption ?: expectedOption
                    }
                }

                incomingOption != null -> merged[taskId] = incomingOption
                currentOption != null -> merged[taskId] = currentOption
            }
        }

        pendingOptionWrites.keys.toList()
            .filter { taskId -> taskId !in activeTaskIds }
            .forEach { taskId -> pendingOptionWrites.remove(taskId) }

        pendingOptionClears.toList()
            .filter { taskId -> taskId !in activeTaskIds }
            .forEach { taskId -> pendingOptionClears.remove(taskId) }

        return merged
    }
}

private fun DownloadTask.toEntity(json: Json, optionsJson: String? = null): DownloadTaskEntity {
    return DownloadTaskEntity(
        id = id,
        url = url,
        title = title,
        status = status.name,
        activeWorkId = activeWorkId,
        progressPercent = progressPercent,
        speed = speed,
        eta = eta,
        outputPath = outputPath,
        thumbnailUrl = thumbnailUrl,
        subtitlePathsJson = json.encodeToString(subtitlePaths),
        downloadedStr = downloadedStr,
        totalSizeStr = totalSizeStr,
        errorMessage = errorMessage,
        debugTrace = debugTrace,
        optionsJson = optionsJson,
        pauseExpiresAtEpochMs = pauseExpiresAtEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}

private fun DownloadTaskEntity.toDomainTask(json: Json): DownloadTask? {
    return runCatching {
        DownloadTask(
            id = id,
            url = url,
            title = title,
            status = DownloadStatus.valueOf(status),
            activeWorkId = activeWorkId,
            progressPercent = progressPercent,
            speed = speed,
            eta = eta,
            outputPath = outputPath,
            thumbnailUrl = thumbnailUrl,
            subtitlePaths = subtitlePathsJson
                ?.takeIf { it.isNotBlank() }
                ?.let { json.decodeFromString<List<String>>(it) }
                .orEmpty(),
            downloadedStr = downloadedStr,
            totalSizeStr = totalSizeStr,
            errorMessage = errorMessage,
            debugTrace = debugTrace,
            pauseExpiresAtEpochMs = pauseExpiresAtEpochMs,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )
    }.getOrNull()
}
