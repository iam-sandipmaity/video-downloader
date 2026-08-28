package com.localdownloader.ui.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TroubleshootingReportTest {

    @Test
    fun formatByteCount_usesBinaryUnits() {
        assertEquals("512 B", formatByteCount(512))
        assertEquals("1.0 KB", formatByteCount(1024))
        assertEquals("1.5 MB", formatByteCount((1024L * 1024L * 3L) / 2L))
    }

    @Test
    fun formatJvmMemoryLines_includesUsedTotalAndMax() {
        val lines = formatJvmMemoryLines(
            maxBytes = 256L * 1024L * 1024L,
            totalBytes = 64L * 1024L * 1024L,
            freeBytes = 16L * 1024L * 1024L,
        )

        assertTrue(lines.any { it.contains("JVM heap used") && it.contains("48.0 MB") })
        assertTrue(lines.any { it.contains("JVM heap max") && it.contains("256.0 MB") })
    }

    @Test
    fun formatStorageLines_includesInternalAndSharedCategories() {
        val lines = formatStorageLines(
            internalTotalBytes = 8L * 1024L * 1024L * 1024L,
            internalAvailableBytes = 2L * 1024L * 1024L * 1024L,
            sharedTotalBytes = 32L * 1024L * 1024L * 1024L,
            sharedAvailableBytes = 10L * 1024L * 1024L * 1024L,
        )

        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("App internal storage"))
        assertTrue(lines[1].contains("Shared storage"))
    }
}
