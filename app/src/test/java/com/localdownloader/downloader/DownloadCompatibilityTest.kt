package com.localdownloader.downloader

import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadCompatibilityTest {
    @Test
    fun resolveMergeContainerCompatibility_switchesHighResAv1Mp4ToMkv() {
        val result = resolveMergeContainerCompatibility(
            requestedContainer = "mp4",
            selectedChoice = mergedChoice(
                height = 2160,
                videoCodec = "av01.0.12M.08",
            ),
        )

        assertEquals("mkv", result.resolvedContainer)
        assertNotNull(result.queueNote)
    }

    @Test
    fun resolveMergeContainerCompatibility_switchesLowerResAv1Mp4ToMkvToo() {
        val result = resolveMergeContainerCompatibility(
            requestedContainer = "mp4",
            selectedChoice = mergedChoice(
                height = 1080,
                videoCodec = "av01.0.08M.08",
            ),
        )

        assertEquals("mkv", result.resolvedContainer)
        assertNotNull(result.queueNote)
    }

    @Test
    fun resolveMergeContainerCompatibility_switchesVp9OpusMp4ToWebm() {
        val result = resolveMergeContainerCompatibility(
            requestedContainer = "mp4",
            selectedChoice = FormatChoice(
                selector = "248+251",
                label = "1080p webm merge",
                streamType = StreamType.VIDEO_AUDIO,
                container = "webm",
                height = 1080,
                isMerged = true,
                isImageLike = false,
                videoCodec = "vp9",
                audioCodec = "opus",
            ),
        )

        assertEquals("webm", result.resolvedContainer)
        assertNotNull(result.queueNote)
    }

    @Test
    fun resolveMergeContainerCompatibility_ignoresMuxedChoices() {
        val result = resolveMergeContainerCompatibility(
            requestedContainer = "mp4",
            selectedChoice = FormatChoice(
                selector = "301/b[ext=mp4]/b/best",
                label = "1080p mp4 muxed",
                streamType = StreamType.VIDEO_AUDIO,
                container = "mp4",
                height = 1080,
                isMerged = false,
                isImageLike = false,
                videoCodec = "avc1.640028",
                audioCodec = "mp4a.40.2",
            ),
        )

        assertEquals("mp4", result.resolvedContainer)
        assertNull(result.queueNote)
    }

    private fun mergedChoice(
        height: Int,
        videoCodec: String,
    ): FormatChoice {
        return FormatChoice(
            selector = "401+140/401+bestaudio/401/best",
            label = "${height}p",
            streamType = StreamType.VIDEO_AUDIO,
            container = "mp4",
            height = height,
            isMerged = true,
            isImageLike = false,
            videoCodec = videoCodec,
            audioCodec = "mp4a.40.2",
        )
    }
}
