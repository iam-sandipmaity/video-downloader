package com.localdownloader.data

import com.localdownloader.domain.models.DownloadTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTaskStore @Inject constructor() {
    private val tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())

    fun observeAll(): Flow<List<DownloadTask>> {
        return tasks.map { taskMap ->
            taskMap.values.sortedByDescending { it.createdAtEpochMs }
        }
    }

    fun getTask(taskId: String): DownloadTask? = tasks.value[taskId]

    fun upsert(task: DownloadTask) {
        tasks.update { taskMap -> taskMap + (task.id to task) }
    }

    fun update(taskId: String, reducer: (DownloadTask) -> DownloadTask) {
        tasks.update { taskMap ->
            val current = taskMap[taskId] ?: return@update taskMap
            taskMap + (taskId to reducer(current))
        }
    }

    /** Returns how many tasks (any status) share this URL in the current session. */
    fun countByUrl(url: String): Int = tasks.value.values.count { it.url == url }
}
