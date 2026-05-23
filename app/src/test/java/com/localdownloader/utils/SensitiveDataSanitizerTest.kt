package com.localdownloader.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveDataSanitizerTest {

    @Test
    fun sanitize_redactsTokensUrlsAndPaths() {
        val sanitized = SensitiveDataSanitizer.sanitize(
            "url=https://example.com/watch?v=secret po_token=abc123 data_sync_id=sync456 " +
                "outputPath=/storage/emulated/0/Download/secret.mp4 --cookies /data/user/0/app/files/cookies.txt",
        )

        assertTrue(sanitized.contains("url=<url:https://example.com (path)>"))
        assertTrue(sanitized.contains("po_token=<redacted>"))
        assertTrue(sanitized.contains("data_sync_id=<redacted>"))
        assertTrue(sanitized.contains("outputPath=<path:secret.mp4>"))
        assertTrue(sanitized.contains("--cookies <path:cookies.txt>"))
        assertFalse(sanitized.contains("abc123"))
        assertFalse(sanitized.contains("sync456"))
    }

    @Test
    fun describeUrl_returnsHostOnlySummary() {
        val summary = SensitiveDataSanitizer.describeUrl("https://www.youtube.com/watch?v=abc123")

        assertTrue(summary.startsWith("https://www.youtube.com"))
        assertTrue(summary.endsWith(" (path)"))
        assertFalse(summary.contains("abc123"))
    }
}
