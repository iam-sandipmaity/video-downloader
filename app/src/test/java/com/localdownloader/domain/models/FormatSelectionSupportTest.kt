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

    @Test
    fun availableOutputTransforms_offersDerivedChoicesForMuxedOnlySources() {
        val muxedChoices = listOf(
            FormatChoice(
                selector = "best[ext=mp4]",
                label = "720p mp4 muxed",
                streamType = StreamType.VIDEO_AUDIO,
                container = "mp4",
                height = 720,
                isMerged = false,
                isImageLike = false,
            ),
        )

        assertEquals(
            listOf(
                OutputTransform.NONE,
                OutputTransform.EXTRACT_AUDIO,
                OutputTransform.REMOVE_AUDIO,
            ),
            availableOutputTransforms(
                sourceStreamType = StreamType.VIDEO_AUDIO,
                videoAudioChoices = muxedChoices,
                videoOnlyChoices = emptyList(),
                audioOnlyChoices = emptyList(),
            ),
        )
    }

    @Test
    fun resolveAvailableOutputTransform_resetsUnsupportedDerivedChoice() {
        val muxedChoices = listOf(
            FormatChoice(
                selector = "best[ext=mp4]",
                label = "720p mp4 muxed",
                streamType = StreamType.VIDEO_AUDIO,
                container = "mp4",
                height = 720,
                isMerged = false,
                isImageLike = false,
            ),
        )
        val nativeAudioChoices = listOf(
            FormatChoice(
                selector = "ba[ext=m4a]",
                label = "audio m4a 320kbps",
                streamType = StreamType.AUDIO_ONLY,
                container = "m4a",
                height = null,
                isMerged = false,
                isImageLike = false,
            ),
        )

        assertEquals(
            OutputTransform.NONE,
            resolveAvailableOutputTransform(
                preferredTransform = OutputTransform.EXTRACT_AUDIO,
                sourceStreamType = StreamType.VIDEO_AUDIO,
                videoAudioChoices = muxedChoices,
                videoOnlyChoices = emptyList(),
                audioOnlyChoices = nativeAudioChoices,
            ),
        )
    }

    @Test
    fun effectiveOutputStreamType_mapsDerivedOutputsToTheirRealResultType() {
        assertEquals(
            StreamType.AUDIO_ONLY,
            effectiveOutputStreamType(StreamType.VIDEO_AUDIO, OutputTransform.EXTRACT_AUDIO),
        )
        assertEquals(
            StreamType.VIDEO_ONLY,
            effectiveOutputStreamType(StreamType.VIDEO_AUDIO, OutputTransform.REMOVE_AUDIO),
        )
        assertEquals(
            StreamType.VIDEO_AUDIO,
            effectiveOutputStreamType(StreamType.VIDEO_AUDIO, OutputTransform.NONE),
        )
    }

    @Test
    fun findBestMatchingFormatChoice_matchesExactHeightAndContainer() {
        val choices = listOf(
            FormatChoice(selector = "1080_mp4", label = "1080p MP4", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 1080, isMerged = false, isImageLike = false),
            FormatChoice(selector = "1080_webm", label = "1080p WebM", streamType = StreamType.VIDEO_AUDIO, container = "webm", height = 1080, isMerged = false, isImageLike = false),
            FormatChoice(selector = "720_mp4", label = "720p MP4", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 720, isMerged = false, isImageLike = false),
        )

        val result = findBestMatchingFormatChoice(choices, targetHeight = 1080, preferredContainer = "mp4")
        assertEquals("1080_mp4", result?.selector)
    }

    @Test
    fun findBestMatchingFormatChoice_fallsBackToClosestResolutionUnderTarget() {
        val choices = listOf(
            FormatChoice(selector = "4k", label = "4K", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 2160, isMerged = false, isImageLike = false),
            FormatChoice(selector = "720p", label = "720p", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 720, isMerged = false, isImageLike = false),
            FormatChoice(selector = "480p", label = "480p", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 480, isMerged = false, isImageLike = false),
        )

        // User wanted 1080p, but only 4K and 720p/480p exist. Closest under 1080p is 720p!
        val result = findBestMatchingFormatChoice(choices, targetHeight = 1080, preferredContainer = "mp4")
        assertEquals("720p", result?.selector)
    }

    @Test
    fun findBestMatchingFormatChoice_selectsHighestForBestQuality() {
        val choices = listOf(
            FormatChoice(selector = "720p", label = "720p", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 720, isMerged = false, isImageLike = false),
            FormatChoice(selector = "4k", label = "4K", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 2160, isMerged = false, isImageLike = false),
            FormatChoice(selector = "1080p", label = "1080p", streamType = StreamType.VIDEO_AUDIO, container = "mp4", height = 1080, isMerged = false, isImageLike = false),
        )

        val result = findBestMatchingFormatChoice(choices, targetHeight = null, preferredContainer = "auto")
        assertEquals("4k", result?.selector)
    }
}

