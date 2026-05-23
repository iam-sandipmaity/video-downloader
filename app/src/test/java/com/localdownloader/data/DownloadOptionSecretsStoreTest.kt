package com.localdownloader.data

import com.localdownloader.domain.models.DownloadOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadOptionSecretsStoreTest {

    @Test
    fun containsPersistedSecrets_detectsSensitiveFields() {
        val options = DownloadOptions(
            url = "https://example.com/video",
            formatId = "best",
            extractorArgs = "youtube:player-client=web",
            youtubePoToken = "secret-token",
        )

        assertTrue(options.containsPersistedSecrets())
    }

    @Test
    fun redactedForPersistence_removesSecretsButKeepsNonSecretFlags() {
        val options = DownloadOptions(
            url = "https://example.com/video",
            formatId = "best",
            extractorArgs = "youtube:player-client=web",
            fallbackExtractorArgs = "fallback",
            loadInfoJsonPath = "/tmp/info.json",
            userAgentHeader = "UnitTest/1.0",
            youtubeAuthEnabled = true,
            youtubeCookiesPath = "/tmp/cookies.txt",
            youtubePoToken = "secret-token",
            youtubePoTokenClientHint = "web.creator",
            youtubeDataSyncId = "sync-id",
        )

        val redacted = options.redactedForPersistence()

        assertNull(redacted.extractorArgs)
        assertNull(redacted.fallbackExtractorArgs)
        assertNull(redacted.loadInfoJsonPath)
        assertNull(redacted.userAgentHeader)
        assertNull(redacted.youtubeCookiesPath)
        assertNull(redacted.youtubePoToken)
        assertNull(redacted.youtubeDataSyncId)
        assertTrue(redacted.youtubeAuthEnabled)
        assertEquals("web.creator", redacted.youtubePoTokenClientHint)
    }

    @Test
    fun applyPersistedSecrets_restoresSensitiveFields() {
        val baseOptions = DownloadOptions(
            url = "https://example.com/video",
            formatId = "best",
            youtubeAuthEnabled = true,
        )
        val secrets = PersistedDownloadOptionSecrets(
            extractorArgs = "youtube:player-client=web",
            fallbackExtractorArgs = "fallback",
            loadInfoJsonPath = "/tmp/info.json",
            userAgentHeader = "UnitTest/1.0",
            youtubeCookiesPath = "/tmp/cookies.txt",
            youtubePoToken = "secret-token",
            youtubeDataSyncId = "sync-id",
        )

        val hydrated = baseOptions.applyPersistedSecrets(secrets)

        assertEquals("youtube:player-client=web", hydrated.extractorArgs)
        assertEquals("fallback", hydrated.fallbackExtractorArgs)
        assertEquals("/tmp/info.json", hydrated.loadInfoJsonPath)
        assertEquals("UnitTest/1.0", hydrated.userAgentHeader)
        assertEquals("/tmp/cookies.txt", hydrated.youtubeCookiesPath)
        assertEquals("secret-token", hydrated.youtubePoToken)
        assertEquals("sync-id", hydrated.youtubeDataSyncId)
    }

    @Test
    fun containsPersistedSecrets_ignoresNonSensitiveFlagsAlone() {
        val options = DownloadOptions(
            url = "https://example.com/video",
            formatId = "best",
            youtubeAuthEnabled = true,
            youtubePoTokenClientHint = "web.gvs",
        )

        assertFalse(options.containsPersistedSecrets())
    }
}
