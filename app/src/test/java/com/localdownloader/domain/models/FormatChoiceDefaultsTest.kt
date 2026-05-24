package com.localdownloader.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FormatChoiceDefaultsTest {

    @Test
    fun prefersPlaybackFriendlyMp4OverHigherVp9ForAutoContainer() {
        val vp9Uhd = videoAudioChoice(
            selector = "313+251",
            container = "webm",
            height = 2160,
            videoCodec = "vp9",
            audioCodec = "opus",
        )
        val avcFhd = videoAudioChoice(
            selector = "137+140",
            container = "mp4",
            height = 1080,
            videoCodec = "avc1.640028",
            audioCodec = "mp4a.40.2",
        )

        val selected = preferredDefaultChoiceForStreamType(
            streamType = StreamType.VIDEO_AUDIO,
            requestedContainer = "auto",
            choices = listOf(vp9Uhd, avcFhd),
        )

        assertSame(avcFhd, selected)
    }

    @Test
    fun keepsHighestChoiceForExplicitWebmContainer() {
        val vp9Uhd = videoAudioChoice(
            selector = "313+251",
            container = "webm",
            height = 2160,
            videoCodec = "vp9",
            audioCodec = "opus",
        )
        val vp9Fhd = videoAudioChoice(
            selector = "248+251",
            container = "webm",
            height = 1080,
            videoCodec = "vp9",
            audioCodec = "opus",
        )

        val selected = preferredDefaultChoiceForStreamType(
            streamType = StreamType.VIDEO_AUDIO,
            requestedContainer = "webm",
            choices = listOf(vp9Uhd, vp9Fhd),
        )

        assertSame(vp9Uhd, selected)
    }

    @Test
    fun keepsFirstAudioChoiceForAudioStream() {
        val first = FormatChoice(
            selector = "251",
            label = "audio webm 160kbps",
            streamType = StreamType.AUDIO_ONLY,
            container = "webm",
            height = null,
            isMerged = false,
            isImageLike = false,
            audioCodec = "opus",
        )
        val second = first.copy(selector = "140", container = "m4a", audioCodec = "mp4a.40.2")

        val selected = preferredDefaultChoiceForStreamType(
            streamType = StreamType.AUDIO_ONLY,
            requestedContainer = "auto",
            choices = listOf(first, second),
        )

        assertSame(first, selected)
    }

    private fun videoAudioChoice(
        selector: String,
        container: String,
        height: Int,
        videoCodec: String,
        audioCodec: String,
    ): FormatChoice {
        return FormatChoice(
            selector = selector,
            label = "$height p $container",
            streamType = StreamType.VIDEO_AUDIO,
            container = container,
            height = height,
            isMerged = true,
            isImageLike = false,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
        )
    }
}
