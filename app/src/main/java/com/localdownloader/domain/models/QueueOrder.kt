package com.localdownloader.domain.models

internal fun queueOrderSignature(tasks: List<DownloadTask>): String {
    if (tasks.isEmpty()) return ""
    return buildString(tasks.size * 24) {
        for (task in tasks) {
            append(task.id)
            append(':')
            append(task.createdAtEpochMs)
            append('|')
        }
    }
}

internal fun orderedTaskIdsByCreatedAt(
    tasks: List<DownloadTask>,
    oldestFirst: Boolean,
): List<String> {
    val sorted = if (oldestFirst) {
        tasks.sortedBy { it.createdAtEpochMs }
    } else {
        tasks.sortedByDescending { it.createdAtEpochMs }
    }
    return sorted.map { it.id }
}

internal fun applyStableTaskOrder(
    orderedIds: List<String>,
    tasks: List<DownloadTask>,
): List<DownloadTask> {
    if (orderedIds.isEmpty() || tasks.isEmpty()) return tasks
    val byId = LinkedHashMap<String, DownloadTask>(tasks.size)
    for (task in tasks) {
        byId[task.id] = task
    }
    val ordered = ArrayList<DownloadTask>(tasks.size)
    for (id in orderedIds) {
        byId.remove(id)?.let(ordered::add)
    }
    if (byId.isNotEmpty()) {
        ordered.addAll(byId.values)
    }
    return ordered
}
