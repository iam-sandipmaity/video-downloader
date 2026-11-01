package com.localdownloader.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class YtDlpExecutorArgsTest {

    private val fakeRuntime = YtDlpRuntime(
        pythonBinary = File("/fake/bin/python"),
        quickJsBinary = File("/fake/lib/libqjs.so"),
        ffmpegBinary = File("/fake/bin/ffmpeg"),
        ytDlpScript = File("/fake/bin/yt-dlp"),
        environment = emptyMap(),
    )

    @Test
    fun normalizeYtDlpArgs_withPrecedingDashDash_placesAllOptionsBeforeDashDash() {
        val input = listOf("-o", "output.mp4", "--", "https://www.youtube.com/watch?v=abc123xyz")
        val result = normalizeYtDlpArgs(input, fakeRuntime)

        val dashDashIndex = result.indexOf("--")
        assertTrue("Expected '--' to be present", dashDashIndex >= 0)
        assertEquals("URL must immediately follow '--'", "https://www.youtube.com/watch?v=abc123xyz", result[dashDashIndex + 1])
        assertEquals("URL must be the last argument", result.size - 1, dashDashIndex + 1)

        val noCacheDirIndex = result.indexOf("--no-cache-dir")
        val jsRuntimesIndex = result.indexOf("--js-runtimes")
        val ffmpegLocationIndex = result.indexOf("--ffmpeg-location")

        assertTrue("--no-cache-dir must be present", noCacheDirIndex >= 0)
        assertTrue("--js-runtimes must be present", jsRuntimesIndex >= 0)
        assertTrue("--ffmpeg-location must be present", ffmpegLocationIndex >= 0)

        // All options must be strictly BEFORE '--'
        assertTrue("--no-cache-dir must precede '--'", noCacheDirIndex < dashDashIndex)
        assertTrue("--js-runtimes must precede '--'", jsRuntimesIndex < dashDashIndex)
        assertTrue("--ffmpeg-location must precede '--'", ffmpegLocationIndex < dashDashIndex)

        // Positional slice after '--' should ONLY contain the URL
        val positionalArgs = result.subList(dashDashIndex + 1, result.size)
        assertEquals(listOf("https://www.youtube.com/watch?v=abc123xyz"), positionalArgs)
    }

    @Test
    fun normalizeYtDlpArgs_withoutDashDash_insertsDashDashBeforeUrl() {
        val input = listOf("-o", "output.mp4", "https://www.youtube.com/watch?v=abc123xyz")
        val result = normalizeYtDlpArgs(input, fakeRuntime)

        val dashDashIndex = result.indexOf("--")
        assertTrue("Expected '--' to be inserted", dashDashIndex >= 0)
        assertEquals("URL must immediately follow '--'", "https://www.youtube.com/watch?v=abc123xyz", result[dashDashIndex + 1])

        val ffmpegLocationIndex = result.indexOf("--ffmpeg-location")
        assertTrue("--ffmpeg-location must precede '--'", ffmpegLocationIndex < dashDashIndex)
    }

    @Test
    fun normalizeYtDlpArgs_nonUrlCommand_doesNotAddDashDash() {
        val input = listOf("--version")
        val result = normalizeYtDlpArgs(input, fakeRuntime)

        assertFalse("Command without URL must not contain '--'", result.contains("--"))
        assertTrue(result.contains("--version"))
        assertTrue(result.contains("--ffmpeg-location"))
    }

    @Test
    fun normalizeYtDlpArgs_existingOptionsAreNotDuplicated() {
        val input = listOf(
            "--cache-dir", "/tmp/custom_cache",
            "--ffmpeg-location", "/custom/ffmpeg",
            "--",
            "https://www.youtube.com/watch?v=abc123xyz",
        )
        val result = normalizeYtDlpArgs(input, fakeRuntime)

        // --no-cache-dir must NOT be added if --cache-dir is present
        assertFalse(result.contains("--no-cache-dir"))

        // --ffmpeg-location must appear exactly once
        val ffmpegOccurrences = result.count { it == "--ffmpeg-location" }
        assertEquals(1, ffmpegOccurrences)
        assertEquals("/custom/ffmpeg", result[result.indexOf("--ffmpeg-location") + 1])

        // --js-runtimes must be added before '--'
        val dashDashIndex = result.indexOf("--")
        val jsRuntimesIndex = result.indexOf("--js-runtimes")
        assertTrue(jsRuntimesIndex in 0 until dashDashIndex)
    }
}
