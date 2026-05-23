package com.localdownloader.downloader

import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeMuxedFormatRoutingTest {
    @Test
    fun resolveYoutubeFormatRouting_reroutesMuxedYoutubeMp4Choice() {
        val routing = resolveYoutubeFormatRouting(
            sourceUrl = "https://www.youtube.com/watch?v=test",
            streamType = StreamType.VIDEO_AUDIO,
            selectedChoice = muxedChoice(container = "mp4", height = 1080),
            requestedContainer = "mp4",
            fallbackSelector = "301/b[ext=mp4]/b/best",
            hasMergedVideoAudioChoice = true,
        )

        assertEquals(
            "bestvideo[height<=1080][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=1080]+bestaudio/best[height<=1080]/best",
            routing.selector,
        )
        assertNotNull(routing.queueNote)
    }

    @Test
    fun resolveYoutubeFormatRouting_keepsMuxedNonYoutubeChoice() {
        val routing = resolveYoutubeFormatRouting(
            sourceUrl = "https://example.com/video",
            streamType = StreamType.VIDEO_AUDIO,
            selectedChoice = muxedChoice(container = "webm", height = 480),
            requestedContainer = "webm",
            fallbackSelector = "244/b[ext=webm]/b/best",
            hasMergedVideoAudioChoice = true,
        )

        assertEquals("244/b[ext=webm]/b/best", routing.selector)
        assertNull(routing.queueNote)
    }

    @Test
    fun resolveYoutubeFormatRouting_keepsAlreadyMergedChoice() {
        val routing = resolveYoutubeFormatRouting(
            sourceUrl = "https://www.youtube.com/watch?v=test",
            streamType = StreamType.VIDEO_AUDIO,
            selectedChoice = muxedChoice(container = "mp4", height = 2160).copy(
                selector = "401+140/401+bestaudio/401/best",
                isMerged = true,
            ),
            requestedContainer = "mp4",
            fallbackSelector = "401+140/401+bestaudio/401/best",
            hasMergedVideoAudioChoice = true,
        )

        assertEquals("401+140/401+bestaudio/401/best", routing.selector)
        assertNull(routing.queueNote)
    }

    private fun muxedChoice(
        container: String,
        height: Int,
    ): FormatChoice {
        return FormatChoice(
            selector = "244/b[ext=$container]/b/best",
            label = "${height}p muxed",
            streamType = StreamType.VIDEO_AUDIO,
            container = container,
            height = height,
            isMerged = false,
            isImageLike = false,
            videoCodec = "vp9",
            audioCodec = "opus",
        )
    }
}
