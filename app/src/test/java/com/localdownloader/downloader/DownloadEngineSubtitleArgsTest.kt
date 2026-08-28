package com.localdownloader.downloader

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadEngineSubtitleArgsTest {

    @Test
    fun subtitleDownloadArgs_neverEmbedsInTheMainCommand() {
        val args = subtitleDownloadArgs("https://www.youtube.com/watch?v=abc")

        assertFalse(args.contains("--embed-subs"))
        assertTrue(args.contains("--write-subs"))
        assertTrue(args.contains("--write-auto-subs"))
    }

    @Test
    fun preferredYoutubeSubtitleLanguages_includesAudioLanguageLocaleEnglishAndAll() {
        val langs = preferredYoutubeSubtitleLanguages(
            preferredAudioLanguage = "hi-IN",
            locale = Locale.US,
        )

        assertEquals("hi-IN,hi,en-US,en,en-orig,all", langs)
    }

    @Test
    fun subtitleDownloadArgs_usesAllLanguagesForNonYoutube() {
        val args = subtitleDownloadArgs("https://vimeo.com/123")
        val langsIndex = args.indexOf("--sub-langs")

        assertTrue(langsIndex >= 0)
        assertEquals("all,-live_chat", args[langsIndex + 1])
    }
}
