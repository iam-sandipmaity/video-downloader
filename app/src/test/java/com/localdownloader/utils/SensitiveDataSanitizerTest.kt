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

    @Test
    fun sanitize_redactsCredentialsAndHeaders() {
        val sanitized = SensitiveDataSanitizer.sanitize(
            "cmd: yt-dlp --password mySecretPass -u myUser --add-header Authorization:Bearer_token123 " +
                "Bearer secret_jwt_token_456 api_key=key_789 pin=1234 pinHash=abcdef",
        )

        assertTrue(sanitized.contains("--password <redacted>"))
        assertTrue(sanitized.contains("-u <redacted>"))
        assertTrue(sanitized.contains("--add-header Authorization:<redacted>"))
        assertTrue(sanitized.contains("Bearer <redacted>"))
        assertTrue(sanitized.contains("token=<redacted>"))
        assertTrue(sanitized.contains("password=<redacted>"))
        assertFalse(sanitized.contains("mySecretPass"))
        assertFalse(sanitized.contains("secret_jwt_token_456"))
        assertFalse(sanitized.contains("key_789"))
        assertFalse(sanitized.contains("1234"))
        assertFalse(sanitized.contains("abcdef"))
    }
}
