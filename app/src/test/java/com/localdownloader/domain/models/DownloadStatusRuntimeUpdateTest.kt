package com.localdownloader.domain.models

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadStatusRuntimeUpdateTest {

    @Test
    fun blocksRuntimeUpdates_matchesActiveAndResumableQueueStates() {
        assertTrue(DownloadStatus.QUEUED.blocksRuntimeUpdates())
        assertTrue(DownloadStatus.RUNNING.blocksRuntimeUpdates())
        assertTrue(DownloadStatus.PAUSED.blocksRuntimeUpdates())
    }

    @Test
    fun blocksRuntimeUpdates_ignores_terminalStates() {
        assertFalse(DownloadStatus.COMPLETED.blocksRuntimeUpdates())
        assertFalse(DownloadStatus.FAILED.blocksRuntimeUpdates())
        assertFalse(DownloadStatus.CANCELED.blocksRuntimeUpdates())
    }
}
