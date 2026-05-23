package com.localdownloader.downloader

import java.nio.file.Files
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
}
