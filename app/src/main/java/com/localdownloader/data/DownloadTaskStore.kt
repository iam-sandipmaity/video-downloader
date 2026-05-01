package com.localdownloader.data

import com.localdownloader.data.persistence.DownloadTaskDao
import com.localdownloader.data.persistence.DownloadTaskEntity
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTaskStore @Inject constructor(
    private val dao: DownloadTaskDao,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** In-memory cache synced from Room for fast non-suspending access. */
    private val tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())

    init {
        scope.launch {
            dao.observeAll().collect { entities ->
                val map = entities.mapNotNull { it.toDomainTask() }.associateBy { it.id }
                tasks.update { map }
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
        tasks.update { taskMap -> taskMap + (task.id to task) }
        scope.launch { dao.upsert(task.toEntity(optionsJson = optionsJson)) }
    }

    fun update(taskId: String, reducer: (DownloadTask) -> DownloadTask) {
        tasks.update { taskMap ->
            val current = taskMap[taskId] ?: return@update taskMap
            val updated = reducer(current)
            scope.launch {
                val existingOptionsJson = dao.getById(taskId)?.optionsJson
                dao.upsert(updated.toEntity(optionsJson = existingOptionsJson))
            }
            taskMap + (taskId to updated)
        }
    }

    fun countByUrl(url: String): Int = tasks.value.values.count { it.url == url }

    fun cacheOptions(taskId: String, optionsJson: String) {
        scope.launch {
            dao.getById(taskId)?.let { entity ->
                dao.upsert(entity.copy(optionsJson = optionsJson))
            }
        }
    }

    fun remove(taskId: String) {
        tasks.update { taskMap -> taskMap - taskId }
        scope.launch { dao.deleteById(taskId) }
    }

    fun removeMany(taskIds: Collection<String>) {
        if (taskIds.isEmpty()) return
        val ids = taskIds.toSet()
        tasks.update { taskMap -> taskMap - ids }
        scope.launch {
            ids.forEach { dao.deleteById(it) }
        }
    }

    fun clearAll() {
        tasks.update { emptyMap() }
        scope.launch { dao.deleteAll() }
    }

    suspend fun getCachedOptions(taskId: String): String? {
        return dao.getById(taskId)?.optionsJson
    }
}

private fun DownloadTask.toEntity(optionsJson: String? = null): DownloadTaskEntity {
    return DownloadTaskEntity(
        id = id,
        url = url,
        title = title,
        status = status.name,
        progressPercent = progressPercent,
        speed = speed,
        eta = eta,
        outputPath = outputPath,
        downloadedStr = downloadedStr,
        totalSizeStr = totalSizeStr,
        errorMessage = errorMessage,
        debugTrace = debugTrace,
        optionsJson = optionsJson,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
    )
}

private fun DownloadTaskEntity.toDomainTask(): DownloadTask? {
    return runCatching {
        DownloadTask(
            id = id,
            url = url,
            title = title,
            status = DownloadStatus.valueOf(status),
            progressPercent = progressPercent,
            speed = speed,
            eta = eta,
            outputPath = outputPath,
            downloadedStr = downloadedStr,
            totalSizeStr = totalSizeStr,
            errorMessage = errorMessage,
            debugTrace = debugTrace,
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
        )
    }.getOrNull()
}
