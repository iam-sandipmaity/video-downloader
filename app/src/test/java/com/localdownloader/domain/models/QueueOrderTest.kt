package com.localdownloader.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

class QueueOrderTest {

    @Test
    fun queueOrderSignature_ignoresProgressAndKeepsIdentityStable() {
        val first = listOf(
            task("a", createdAt = 2, progress = 10),
            task("b", createdAt = 1, progress = 20),
        )
        val progressed = listOf(
            task("a", createdAt = 2, progress = 90),
            task("b", createdAt = 1, progress = 40),
        )

        assertEquals(queueOrderSignature(first), queueOrderSignature(progressed))
    }

    @Test
    fun applyStableTaskOrder_reusesCachedIdsWhileRefreshingTaskSnapshots() {
        val original = listOf(
            task("new", createdAt = 20, progress = 0),
            task("old", createdAt = 10, progress = 0),
        )
        val orderedIds = orderedTaskIdsByCreatedAt(original, oldestFirst = false)
        val progressed = listOf(
            task("old", createdAt = 10, progress = 55),
            task("new", createdAt = 20, progress = 12),
        )

        val ordered = applyStableTaskOrder(orderedIds, progressed)

        assertEquals(listOf("new", "old"), ordered.map { it.id })
        assertEquals(12, ordered[0].progressPercent)
        assertEquals(55, ordered[1].progressPercent)
    }

    private fun task(id: String, createdAt: Long, progress: Int): DownloadTask {
        return DownloadTask(
            id = id,
            url = "https://example.com/$id",
            title = id,
            status = DownloadStatus.RUNNING,
            progressPercent = progress,
            createdAtEpochMs = createdAt,
        )
    }
}
