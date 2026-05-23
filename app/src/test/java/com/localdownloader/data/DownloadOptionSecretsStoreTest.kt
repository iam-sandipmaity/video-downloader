package com.localdownloader.data

import com.localdownloader.domain.models.DownloadOptions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    fun downloadOptions_serializesCookiesPathUsingLegacyKey() {
        val options = DownloadOptions(
            url = "https://example.com/video",
            formatId = "best",
            cookiesPath = "/tmp/cookies.txt",
        )

        val serialized = Json.encodeToString(options)

        assertTrue(serialized.contains("\"youtubeCookiesPath\":\"/tmp/cookies.txt\""))
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
            cookiesPath = "/tmp/cookies.txt",
            youtubePoToken = "secret-token",
            youtubePoTokenClientHint = "web.creator",
            youtubeDataSyncId = "sync-id",
        )

        val redacted = options.redactedForPersistence()

        assertNull(redacted.extractorArgs)
        assertNull(redacted.fallbackExtractorArgs)
        assertNull(redacted.loadInfoJsonPath)
        assertNull(redacted.userAgentHeader)
        assertNull(redacted.cookiesPath)
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
            cookiesPath = "/tmp/cookies.txt",
            youtubePoToken = "secret-token",
            youtubeDataSyncId = "sync-id",
        )

        val hydrated = baseOptions.applyPersistedSecrets(secrets)

        assertEquals("youtube:player-client=web", hydrated.extractorArgs)
        assertEquals("fallback", hydrated.fallbackExtractorArgs)
        assertEquals("/tmp/info.json", hydrated.loadInfoJsonPath)
        assertEquals("UnitTest/1.0", hydrated.userAgentHeader)
        assertEquals("/tmp/cookies.txt", hydrated.cookiesPath)
        assertEquals("secret-token", hydrated.youtubePoToken)
        assertEquals("sync-id", hydrated.youtubeDataSyncId)
    }

    @Test
    fun applyPersistedSecrets_usesMaterializedCookiesPathOverride() {
        val baseOptions = DownloadOptions(
            url = "https://example.com/video",
            formatId = "best",
            youtubeAuthEnabled = true,
        )
        val secrets = PersistedDownloadOptionSecrets(
            cookiesPath = "/tmp/original-cookies.txt",
            cookiesText = "# Netscape HTTP Cookie File",
        )

        val hydrated = baseOptions.applyPersistedSecrets(
            secrets = secrets,
            cookiesPath = "/tmp/materialized-cookies.txt",
        )

        assertEquals("/tmp/materialized-cookies.txt", hydrated.cookiesPath)
    }

    @Test
    fun persistedSecrets_serializeCookiesFieldsUsingLegacyKeys() {
        val secrets = PersistedDownloadOptionSecrets(
            cookiesPath = "/tmp/original-cookies.txt",
            cookiesText = "# Netscape HTTP Cookie File",
        )

        val serialized = Json.encodeToString(secrets)

        assertTrue(serialized.contains("\"youtubeCookiesPath\":\"/tmp/original-cookies.txt\""))
        assertTrue(serialized.contains("\"youtubeCookiesText\":\"# Netscape HTTP Cookie File\""))
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
