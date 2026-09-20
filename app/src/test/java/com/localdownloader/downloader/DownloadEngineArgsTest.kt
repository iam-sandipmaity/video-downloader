package com.localdownloader.downloader

import com.localdownloader.domain.models.DownloadOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadEngineArgsTest {
    @Test
    fun shouldPassMergeOutputFormat_skipsMergeFlagForAudioExtraction() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "best",
            extractAudio = true,
            audioFormat = "m4a",
            audioBitrateKbps = 320,
            mergeOutputFormat = "m4a",
        )

        assertFalse(shouldPassMergeOutputFormat(options))
    }

    @Test
    fun shouldPassMergeOutputFormat_rejectsAudioContainersEvenWhenExtractAudioIsFalse() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "320",
            extractAudio = false,
            mergeOutputFormat = "m4a",
        )

        assertFalse(shouldPassMergeOutputFormat(options))
    }

    @Test
    fun shouldPassMergeOutputFormat_rejectsInvalidContainers() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "best",
            extractAudio = false,
            mergeOutputFormat = "mp3",
        )

        assertFalse(shouldPassMergeOutputFormat(options))
    }

    @Test
    fun shouldPassMergeOutputFormat_keepsMergeFlagForVideoDownloads() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "bestvideo+bestaudio/best",
            mergeOutputFormat = "mp4",
        )

        assertTrue(shouldPassMergeOutputFormat(options))
    }

    @Test
    fun shouldPassMergeOutputFormat_acceptsValidContainers() {
        listOf("mp4", "mkv", "webm", "mov", "avi", "flv").forEach { container ->
            val options = DownloadOptions(
                url = "https://example.com/watch",
                formatId = "bestvideo+bestaudio/best",
                mergeOutputFormat = container,
            )
            assertTrue("Expected container $container to be valid", shouldPassMergeOutputFormat(options))
        }
    }

    @Test
    fun shouldPassAudioQuality_skipsBitrateForWavExtraction() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "bestaudio",
            extractAudio = true,
            audioFormat = "wav",
            audioBitrateKbps = 192,
        )

        assertFalse(shouldPassAudioQuality(options))
    }

    @Test
    fun shouldPassAudioQuality_keepsBitrateForMp3Extraction() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "bestaudio",
            extractAudio = true,
            audioFormat = "mp3",
            audioBitrateKbps = 192,
        )

        assertTrue(shouldPassAudioQuality(options))
    }
}
