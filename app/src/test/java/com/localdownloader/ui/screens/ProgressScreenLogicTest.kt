package com.localdownloader.ui.screens

import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressScreenLogicTest {

    @Test
    fun buildDiagnosticEntries_includesSourceErrorAndRecentLogLines() {
        val task = DownloadTask(
            id = "task-1",
            url = "https://www.youtube.com/watch?v=123",
            title = "Sample",
            status = DownloadStatus.FAILED,
            errorMessage = "Sign-in required",
            debugTrace = """
                12:00:00: Preparing request
                12:00:01: Fetching player response
                12:00:02: Falling back to authenticated extractor args
                12:00:03: Sign-in required
            """.trimIndent(),
        )

        val diagnostics = buildDiagnosticEntries(
            task = task,
            currentTimeMs = task.updatedAtEpochMs + 65_000L,
        )

        assertTrue(diagnostics.any { it.label == "Source" && it.value == "youtube.com" })
        assertTrue(diagnostics.any { it.label == "Error" && it.value == "Sign-in required" })
        assertTrue(
            diagnostics.any { entry ->
                entry.label == "Recent log lines" &&
                    entry.value.contains("authenticated extractor args") &&
                    entry.value.contains("Sign-in required")
            },
        )
    }

    @Test
    fun latestDebugMessages_returnsOnlyTrailingNonBlankLines() {
        val messages = latestDebugMessages(
            debugTrace = """
                1: first
                
                2: second
                3: third
                4: fourth
            """.trimIndent(),
            limit = 2,
        )

        assertEquals(listOf("third", "fourth"), messages)
    }
}
