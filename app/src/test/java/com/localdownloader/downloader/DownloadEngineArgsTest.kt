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

    @Test
    fun buildSubtitleArgs_honorsCustomLanguagesAndSrtConversion() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "best",
            subtitleLanguages = listOf("en", "hi", "es"),
            subtitleConvertFormat = "srt",
            shouldDownloadSubtitles = true,
            shouldEmbedSubtitles = false,
        )

        val args = buildSubtitleArgs(options)
        assertTrue(args.contains("--sub-langs"))
        val subLangsIndex = args.indexOf("--sub-langs")
        org.junit.Assert.assertEquals("en,hi,es", args[subLangsIndex + 1])
        assertTrue(args.contains("--write-subs"))
        assertTrue(args.contains("--convert-subs"))
        val convertIndex = args.indexOf("--convert-subs")
        org.junit.Assert.assertEquals("srt", args[convertIndex + 1])
    }

    @Test
    fun buildSubtitleArgs_handlesEmbeddingAndPreservingSidecars() {
        val options = DownloadOptions(
            url = "https://example.com/watch",
            formatId = "best",
            subtitleLanguages = listOf("en"),
            shouldEmbedSubtitles = true,
            keepSubtitleFiles = true,
            extractAudio = false,
        )

        val args = buildSubtitleArgs(options)
        assertTrue(args.contains("--embed-subs"))
        assertTrue(args.contains("--write-subs"))
    }
}
