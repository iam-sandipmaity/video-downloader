package com.localdownloader.updates

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YtDlpUpdateIntegrityTest {

    @Test
    fun parseSha256Checksums_supportsStandardChecksumLines() {
        val payload = """
            aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa  yt-dlp
            bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb *yt-dlp.exe
        """.trimIndent()

        val parsed = parseSha256Checksums(payload)

        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", parsed["yt-dlp"])
        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", parsed["yt-dlp.exe"])
    }

    @Test
    fun findExpectedSha256_returnsNullWhenAssetIsMissing() {
        val payload = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa  yt-dlp"

        assertNull(findExpectedSha256(payload, "yt-dlp.exe"))
    }

    @Test
    fun sha256Hex_hashesFileContent() {
        val tempFile = Files.createTempFile("ytdlp-digest", ".txt").toFile().apply {
            writeText("hello world")
            deleteOnExit()
        }

        assertEquals(
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            sha256Hex(tempFile),
        )
    }
}
