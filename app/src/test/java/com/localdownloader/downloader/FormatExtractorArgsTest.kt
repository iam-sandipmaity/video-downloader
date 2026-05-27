package com.localdownloader.downloader

import java.nio.file.Files
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatExtractorArgsTest {

    @Test
    fun buildAnalyzeArgsForRequest_neverDisablesCertificateValidation() {
        val tempDir = Files.createTempDirectory("format-extractor-args")
        val cookieFile = Files.createTempFile(tempDir, "cookies-", ".txt").toFile().apply {
            writeText("# Netscape HTTP Cookie File")
            deleteOnExit()
        }

        val args = buildAnalyzeArgsForRequest(
            url = "https://example.com/watch",
            extractorArgs = "site:mode=default",
            cookiesPath = cookieFile.absolutePath,
            userAgent = "UnitTestAgent/1.0",
            capturedInfoJsonPath = tempDir.resolve("capture.info.json").toString(),
            tempDirPath = tempDir.toString(),
            useLineJsonMode = false,
        )

        assertEquals("-J", args.first())
        assertTrue(args.contains("--cookies"))
        assertTrue(args.contains("--add-header"))
        assertTrue(args.contains("--extractor-args"))
        assertFalse(args.contains("--no-check-certificates"))
    }

    @Test
    fun buildAnalyzeArgsForRequest_lineJsonModeOnlyChangesJsonFlag() {
        val tempDir = Files.createTempDirectory("format-extractor-line-json")

        val args = buildAnalyzeArgsForRequest(
            url = "https://example.com/watch",
            extractorArgs = null,
            cookiesPath = null,
            userAgent = null,
            capturedInfoJsonPath = tempDir.resolve("capture.info.json").toString(),
            tempDirPath = tempDir.toString(),
            useLineJsonMode = true,
        )

        assertEquals("-j", args.first())
        assertFalse(args.contains("--no-check-certificates"))
        assertEquals("https://example.com/watch", args.last())
    }

    @Test
    fun buildAnalyzeArgsForRequest_usesRequestedSocketTimeout() {
        val tempDir = Files.createTempDirectory("format-extractor-timeout")

        val args = buildAnalyzeArgsForRequest(
            url = "https://example.com/watch",
            extractorArgs = null,
            cookiesPath = null,
            userAgent = null,
            capturedInfoJsonPath = tempDir.resolve("capture.info.json").toString(),
            tempDirPath = tempDir.toString(),
            socketTimeoutSeconds = EXTENDED_ANALYZE_SOCKET_TIMEOUT_SECONDS,
            useLineJsonMode = false,
        )

        val timeoutIndex = args.indexOf("--socket-timeout")
        assertTrue(timeoutIndex >= 0)
        assertEquals(
            EXTENDED_ANALYZE_SOCKET_TIMEOUT_SECONDS.toString(),
            args[timeoutIndex + 1],
        )
        assertFalse(args.contains("--no-check-certificates"))
    }

    @Test
    fun shouldRetryAnalyzeWithExtendedTimeout_onlyForTransientFailures() {
        assertTrue(
            shouldRetryAnalyzeWithExtendedTimeout(
                "[generic] Unable to download webpage: The read operation timed out",
            ),
        )
        assertTrue(
            shouldRetryAnalyzeWithExtendedTimeout(
                "temporary failure in name resolution",
                "unsupported url",
            ),
        )
        assertFalse(
            shouldRetryAnalyzeWithExtendedTimeout(
                "unsupported url",
                "requested format is not available",
            ),
        )
    }

    @Test
    fun shouldContinueAnalyzeCandidateSearch_keepsRetryingForLowQualityOnlyResults() {
        assertTrue(
            shouldContinueAnalyzeCandidateSearch(
                totalFormats = 28,
                videoOnlyFormats = 0,
                maxHeight = 360,
            ),
        )
    }

    @Test
    fun shouldContinueAnalyzeCandidateSearch_stopsOnceHdSelectionIsAvailable() {
        assertFalse(
            shouldContinueAnalyzeCandidateSearch(
                totalFormats = 28,
                videoOnlyFormats = 0,
                maxHeight = 720,
            ),
        )
        assertFalse(
            shouldContinueAnalyzeCandidateSearch(
                totalFormats = 8,
                videoOnlyFormats = 2,
                maxHeight = 1080,
            ),
        )
    }

    @Test
    fun shouldIgnoreFormat_keepsAudioContainersEvenWhenCodecFlagsAreMissing() {
        val item = buildJsonObject {
            put("format_id", "320")
            put("ext", "m4a")
            put("format_note", "audio only")
            put("abr", 320)
        }

        assertFalse(shouldIgnoreFormat(item))
    }

    @Test
    fun parseFormatBitrateKbps_fallsBackToAbrForAudioEntries() {
        val item = buildJsonObject {
            put("format_id", "128")
            put("ext", "m4a")
            put("abr", 128)
        }

        assertEquals(128, parseFormatBitrateKbps(item))
    }

    @Test
    fun youtubeWatchUrlsWithPlaylistContextStillForceSingleVideoAnalyze() {
        val url = "https://www.youtube.com/watch?v=abc123&list=PL_test_123&index=4"

        assertEquals(YoutubeAnalyzeIntent.EXPLICIT_VIDEO, resolveYoutubeAnalyzeIntent(url))
        assertTrue(isExplicitYoutubeVideoUrl(url))
        assertFalse(isLikelyYoutubePlaylistUrl(url))
        assertFalse(shouldUseFastPlaylistAnalyze(url))
        assertTrue(shouldForceNoPlaylistAnalyze(url))
    }

    @Test
    fun youtubeWatchVideosUrlsUseFastListAnalyzeIntent() {
        val url = "https://www.youtube.com/watch_videos?video_ids=abc123,def456"

        assertEquals(YoutubeAnalyzeIntent.WATCH_VIDEOS, resolveYoutubeAnalyzeIntent(url))
        assertTrue(isYoutubeWatchVideosUrl(url))
        assertTrue(shouldUseFastPlaylistAnalyze(url))
        assertFalse(shouldForceNoPlaylistAnalyze(url))
    }

    @Test
    fun youtubeChannelTabUrlsUseFastListAnalyzeIntent() {
        val url = "https://www.youtube.com/@openai/videos"

        assertEquals(YoutubeAnalyzeIntent.CHANNEL_TAB, resolveYoutubeAnalyzeIntent(url))
        assertTrue(isYoutubeChannelTabUrl(url))
        assertTrue(shouldUseFastPlaylistAnalyze(url))
        assertFalse(shouldForceNoPlaylistAnalyze(url))
    }

    @Test
    fun youtubeChannelRootUrlsStayOutOfFastListAnalyzeForNow() {
        val url = "https://www.youtube.com/@openai"

        assertEquals(YoutubeAnalyzeIntent.CHANNEL_ROOT, resolveYoutubeAnalyzeIntent(url))
        assertFalse(shouldUseFastPlaylistAnalyze(url))
        assertFalse(shouldForceNoPlaylistAnalyze(url))
    }

    @Test
    fun buildAnalyzeArgsForRequest_addsNoPlaylistForYoutubeWatchUrlInsidePlaylist() {
        val tempDir = Files.createTempDirectory("format-extractor-watch-list")

        val args = buildAnalyzeArgsForRequest(
            url = "https://www.youtube.com/watch?v=abc123&list=PL_test_123&index=4",
            extractorArgs = null,
            cookiesPath = null,
            userAgent = null,
            capturedInfoJsonPath = tempDir.resolve("capture.info.json").toString(),
            tempDirPath = tempDir.toString(),
            useLineJsonMode = false,
        )

        assertTrue(args.contains("--no-playlist"))
        assertFalse(args.contains("--flat-playlist"))
        assertFalse(args.contains("--lazy-playlist"))
    }
}
