package com.localdownloader.data

import com.localdownloader.data.persistence.DownloadTaskDao
import com.localdownloader.data.persistence.DownloadTaskEntity
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTaskStore @Inject constructor(
    private val dao: DownloadTaskDao,
    private val json: Json,
) {

    fun observeAll(): Flow<List<DownloadTask>> {
        return dao.observeAll().map { entities ->
            entities.mapNotNull { it.toDomainTask() }
        }
    }

    suspend fun getTask(taskId: String): DownloadTask? {
        return dao.getById(taskId)?.toDomainTask()
    }

    suspend fun upsert(task: DownloadTask) {
        dao.upsert(task.toEntity())
    }

    suspend fun update(taskId: String, reducer: (DownloadTask) -> DownloadTask) {
        val current = dao.getById(taskId)?.toDomainTask() ?: return
        val updated = reducer(current)
        dao.upsert(updated.toEntity())
    }

    suspend fun countByUrl(url: String): Int = dao.countByUrl(url)

    /** Caches download options as JSON for resume capability. */
    suspend fun cacheOptions(taskId: String, optionsJson: String) {
        dao.getById(taskId)?.let { entity ->
            dao.upsert(entity.copy(optionsJson = optionsJson))
        }
    }

    /** Retrieves cached download options JSON for resume capability. */
    suspend fun getCachedOptions(taskId: String): String? {
        return dao.getById(taskId)?.optionsJson
    }
}

private fun DownloadTask.toEntity(): DownloadTaskEntity {
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
