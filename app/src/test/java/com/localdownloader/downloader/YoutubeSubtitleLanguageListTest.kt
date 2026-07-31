package com.localdownloader.downloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class YoutubeSubtitleLanguageListTest {

    @Test
    fun includesPreferredAudioLanguageFirst() {
        val list = buildYoutubeSubtitleLanguageList(
            preferredAudioLanguage = "hi",
            locale = Locale("en", "US"),
        )
        val parts = list.split(",")
        assertEquals("hi", parts.first())
        // English and the final `all` fallback must always be present.
        assertTrue("expected 'en' in $list", parts.contains("en"))
        assertTrue("expected 'en-orig' in $list", parts.contains("en-orig"))
        assertTrue("expected 'all' fallback in $list", parts.contains("all"))
    }

    @Test
    fun alwaysAppendsAllFallbackForNonEnglishVideos() {
        val list = buildYoutubeSubtitleLanguageList(
            preferredAudioLanguage = null,
            locale = Locale("en"),
        )
        assertTrue(
            "non-English videos must still yield *some* subtitle via the 'all' fallback: $list",
            list.split(",").contains("all"),
        )
    }

    @Test
    fun deduplicatesLocaleLanguageAndTag() {
        val list = buildYoutubeSubtitleLanguageList(
            preferredAudioLanguage = null,
            locale = Locale("es", "ES"),
        )
        val parts = list.split(",")
        // es-ES tag and es language should both appear without duplicate es entries beyond those.
        assertTrue("expected 'es-ES' in $list", parts.contains("es-ES"))
        assertTrue("expected 'es' in $list", parts.contains("es"))
        assertEquals(parts.indexOf("es"), parts.lastIndexOf("es"))
    }

    @Test
    fun ignoresUndefinedPreferredLanguage() {
        val list = buildYoutubeSubtitleLanguageList(
            preferredAudioLanguage = "und",
            locale = Locale("en"),
        )
        assertTrue("und must not leak into the list: $list", !list.split(",").contains("und"))
    }
}
