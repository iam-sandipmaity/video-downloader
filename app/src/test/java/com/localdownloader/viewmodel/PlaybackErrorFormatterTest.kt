package com.localdownloader.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorFormatterTest {

    @Test
    fun rewritesCapabilityFailureIntoFriendlyMessage() {
        val rawMessage = "MediaCodecVideoRenderer error, index=0, format=Format(1, null, video/webm, video/x-vnd.on2.vp9, null, -1, en, [3840, 2160, -1.0, ColorInfo(...)], [-1, -1]), format_supported=NO_EXCEEDS_CAPABILITIES"

        val formatted = friendlyPlaybackErrorMessage(
            rawMessage = rawMessage,
            playablePath = "/tmp/sample.webm",
        )

        assertEquals(
            "This device can't play 3840x2160 VP9 WEBM in the built-in player. Try a 1080p MP4 download or open it in another player.",
            formatted,
        )
    }

    @Test
    fun preservesGenericPlaybackErrors() {
        val formatted = friendlyPlaybackErrorMessage(
            rawMessage = "Source error",
            playablePath = "/tmp/sample.mp4",
        )

        assertTrue(formatted.contains("Source error"))
    }
}
