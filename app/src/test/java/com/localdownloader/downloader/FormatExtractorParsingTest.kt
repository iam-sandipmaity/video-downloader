package com.localdownloader.downloader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FormatExtractorParsingTest {

    @Test
    fun inferParsedCodecs_keepsAudioOnlyContainerWhenCodecMetadataMissing() {
        val codecs = inferParsedCodecs(
            extension = "mp3",
            resolution = null,
            rawVideoCodec = null,
            rawAudioCodec = null,
            note = "audio only",
        )

        assertEquals("none", codecs.videoCodec)
        assertEquals("unknown", codecs.audioCodec)
        assertFalse(
            shouldIgnoreParsedFormat(
                formatId = "best",
                extension = "mp3",
                note = "audio only",
                resolvedVideoCodec = codecs.videoCodec,
                resolvedAudioCodec = codecs.audioCodec,
            ),
        )
    }

    @Test
    fun inferParsedCodecs_keepsVideoOnlyEntryWhenResolutionExistsButCodecMetadataMissing() {
        val codecs = inferParsedCodecs(
            extension = "mp4",
            resolution = "720p",
            rawVideoCodec = null,
            rawAudioCodec = null,
            note = "video only",
        )

        assertEquals("unknown", codecs.videoCodec)
        assertEquals("none", codecs.audioCodec)
        assertFalse(
            shouldIgnoreParsedFormat(
                formatId = "video-720",
                extension = "mp4",
                note = "video only",
                resolvedVideoCodec = codecs.videoCodec,
                resolvedAudioCodec = codecs.audioCodec,
            ),
        )
    }

    @Test
    fun analyzeProcessTimeoutMillis_usesLongerDefaultTimeout() {
        assertEquals(DEFAULT_ANALYZE_PROCESS_TIMEOUT_MILLIS, analyzeProcessTimeoutMillis(5))
        assertEquals(EXTENDED_ANALYZE_PROCESS_TIMEOUT_MILLIS, analyzeProcessTimeoutMillis(15))
    }
}
