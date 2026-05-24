package com.localdownloader.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSelectionSupportTest {
    @Test
    fun shouldTreatAsAudioOnlyChoice_acceptsAudioContainerFormats() {
        val format = MediaFormat(
            formatId = "320",
            extension = "m4a",
            container = "m4a_dash",
            resolution = null,
            videoCodec = "unknown",
            audioCodec = "mp4a.40.2",
            fileSizeBytes = 15_500_000,
            bitrateKbps = 320,
            fps = null,
            note = "audio only",
        )

        assertTrue(format.shouldTreatAsAudioOnlyChoice())
    }

    @Test
    fun shouldTreatAsAudioOnlyChoice_keepsRealMuxedVideoOutOfAudioBucket() {
        val format = MediaFormat(
            formatId = "18",
            extension = "mp4",
            container = "mp4",
            resolution = "360p",
            videoCodec = "avc1.42001E",
            audioCodec = "mp4a.40.2",
            fileSizeBytes = 8_000_000,
            bitrateKbps = 512,
            fps = 30.0,
            note = null,
        )

        assertFalse(format.shouldTreatAsAudioOnlyChoice())
    }

    @Test
    fun resolveAvailableStreamType_prefersAudioOnlyWhenThatIsAllWeHave() {
        val audioChoices = listOf(
            FormatChoice(
                selector = "320/ba[ext=m4a]/ba/bestaudio",
                label = "audio m4a 320kbps",
                streamType = StreamType.AUDIO_ONLY,
                container = "m4a",
                height = null,
                isMerged = false,
                isImageLike = false,
            ),
        )

        assertEquals(
            StreamType.AUDIO_ONLY,
            resolveAvailableStreamType(
                preferredStreamType = StreamType.VIDEO_AUDIO,
                videoAudioChoices = emptyList(),
                videoOnlyChoices = emptyList(),
                audioOnlyChoices = audioChoices,
            ),
        )
    }

    @Test
    fun choicesForStreamType_doesNotFallbackVideoAudioToOtherBuckets() {
        val audioChoices = listOf(
            FormatChoice(
                selector = "128/ba[ext=m4a]/ba/bestaudio",
                label = "audio m4a 128kbps",
                streamType = StreamType.AUDIO_ONLY,
                container = "m4a",
                height = null,
                isMerged = false,
                isImageLike = false,
            ),
        )

        assertTrue(
            choicesForStreamType(
                streamType = StreamType.VIDEO_AUDIO,
                videoAudioChoices = emptyList(),
                videoOnlyChoices = emptyList(),
                audioOnlyChoices = audioChoices,
            ).isEmpty(),
        )
    }
}
