package com.localdownloader.downloader

import com.localdownloader.domain.models.MediaFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FormatSelectorBuilderTest {
    @Test
    fun buildMergedSelector_keepsExactAndRecoveryFallbacks() {
        val video = mediaFormat(
            formatId = "4000",
            extension = "mp4",
            videoCodec = "avc1",
            audioCodec = "none",
        )
        val audio = mediaFormat(
            formatId = "ba",
            extension = "m4a",
            videoCodec = "none",
            audioCodec = "mp4a",
        )

        val selector = FormatSelectorBuilder.buildMergedSelector(video, audio)

        assertEquals(
            "4000+ba/4000+ba[ext=m4a]/4000+bestaudio/4000/b[ext=mp4]/b/best",
            selector,
        )
    }

    @Test
    fun buildVideoOnlySelector_staysVideoOnly() {
        val selector = FormatSelectorBuilder.buildVideoOnlySelector(
            mediaFormat(
                formatId = "137",
                extension = "mp4",
                videoCodec = "avc1",
                audioCodec = "none",
            ),
        )

        assertEquals("137/bv/bestvideo", selector)
        assertFalse(selector.contains("/b/"))
        assertFalse(selector.endsWith("/b"))
    }

    @Test
    fun buildAudioOnlySelector_staysAudioOnly() {
        val selector = FormatSelectorBuilder.buildAudioOnlySelector(
            mediaFormat(
                formatId = "251",
                extension = "webm",
                videoCodec = "none",
                audioCodec = "opus",
            ),
        )

        assertEquals("251/ba[ext=webm]/ba/bestaudio", selector)
        assertTrue(selector.contains("bestaudio"))
        assertFalse(selector.endsWith("/b"))
    }

    @Test
    fun buildMergedSelector_preservesAudioLanguageFallbacks() {
        val video = mediaFormat(
            formatId = "4000",
            extension = "mp4",
            videoCodec = "avc1",
            audioCodec = "none",
        )
        val audio = mediaFormat(
            formatId = "140-hi",
            extension = "m4a",
            videoCodec = "none",
            audioCodec = "mp4a",
            language = "hi",
        )

        val selector = FormatSelectorBuilder.buildMergedSelector(video, audio)

        assertTrue(selector.contains("4000+ba[ext=m4a][language^=hi]"))
        assertTrue(selector.contains("4000+ba[language^=hi]"))
    }

    private fun mediaFormat(
        formatId: String,
        extension: String,
        videoCodec: String,
        audioCodec: String,
        language: String? = null,
    ): MediaFormat {
        return MediaFormat(
            formatId = formatId,
            extension = extension,
            container = extension,
            resolution = null,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            fileSizeBytes = null,
            bitrateKbps = null,
            fps = null,
            note = null,
            language = language,
        )
    }
}
