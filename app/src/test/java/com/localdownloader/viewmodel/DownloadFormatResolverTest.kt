package com.localdownloader.viewmodel

import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadFormatResolverTest {
    @Test
    fun `uses muxed stream without merge when requested quality is available as combined format`() {
        val videoInfo = youtubeVideoInfo(
            formats = listOf(
                muxedFormat(formatId = "18", height = 360, extension = "mp4", bitrateKbps = 600),
                videoOnlyFormat(formatId = "137", height = 1080, extension = "mp4", bitrateKbps = 2500),
                audioOnlyFormat(formatId = "140", extension = "m4a", bitrateKbps = 128),
            ),
        )

        val resolved = DownloadFormatResolver.resolve(
            videoInfo = videoInfo,
            quality = VideoQuality.SD_360,
            streamType = StreamType.VIDEO_AUDIO,
            container = "mp4",
        )

        assertEquals("18", resolved.formatSelector)
        assertNull(resolved.mergeOutputFormat)
    }

    @Test
    fun `uses split streams and requests merge when higher quality is video only`() {
        val videoInfo = youtubeVideoInfo(
            formats = listOf(
                muxedFormat(formatId = "18", height = 360, extension = "mp4", bitrateKbps = 600),
                videoOnlyFormat(formatId = "137", height = 1080, extension = "mp4", bitrateKbps = 2500),
                audioOnlyFormat(formatId = "140", extension = "m4a", bitrateKbps = 128),
            ),
        )

        val resolved = DownloadFormatResolver.resolve(
            videoInfo = videoInfo,
            quality = VideoQuality.FHD_1080,
            streamType = StreamType.VIDEO_AUDIO,
            container = "mp4",
        )

        assertEquals("137+140", resolved.formatSelector)
        assertEquals("mp4", resolved.mergeOutputFormat)
    }

    @Test
    fun `falls back to legacy selector for non youtube urls`() {
        val videoInfo = youtubeVideoInfo(
            webpageUrl = "https://example.com/watch?v=123",
            formats = emptyList(),
        )

        val resolved = DownloadFormatResolver.resolve(
            videoInfo = videoInfo,
            quality = VideoQuality.HD_720,
            streamType = StreamType.VIDEO_AUDIO,
            container = "mp4",
        )

        assertEquals(
            "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=720]+bestaudio/best[height<=720]/best",
            resolved.formatSelector,
        )
        assertEquals("mp4", resolved.mergeOutputFormat)
    }

    private fun youtubeVideoInfo(
        webpageUrl: String = "https://www.youtube.com/watch?v=test",
        formats: List<MediaFormat>,
    ): VideoInfo {
        return VideoInfo(
            id = "test",
            title = "Test video",
            uploader = "Uploader",
            durationSeconds = 60,
            thumbnailUrl = null,
            webpageUrl = webpageUrl,
            formats = formats,
            isPlaylist = false,
            playlistCount = null,
        )
    }

    private fun muxedFormat(
        formatId: String,
        height: Int,
        extension: String,
        bitrateKbps: Int,
    ): MediaFormat {
        return MediaFormat(
            formatId = formatId,
            extension = extension,
            container = extension,
            resolution = "${height}p",
            videoCodec = "avc1",
            audioCodec = "mp4a",
            fileSizeBytes = null,
            bitrateKbps = bitrateKbps,
            fps = 30.0,
            note = null,
        )
    }

    private fun videoOnlyFormat(
        formatId: String,
        height: Int,
        extension: String,
        bitrateKbps: Int,
    ): MediaFormat {
        return MediaFormat(
            formatId = formatId,
            extension = extension,
            container = extension,
            resolution = "${height}p",
            videoCodec = "avc1",
            audioCodec = "none",
            fileSizeBytes = null,
            bitrateKbps = bitrateKbps,
            fps = 30.0,
            note = null,
        )
    }

    private fun audioOnlyFormat(
        formatId: String,
        extension: String,
        bitrateKbps: Int,
    ): MediaFormat {
        return MediaFormat(
            formatId = formatId,
            extension = extension,
            container = extension,
            resolution = null,
            videoCodec = "none",
            audioCodec = "mp4a",
            fileSizeBytes = null,
            bitrateKbps = bitrateKbps,
            fps = null,
            note = null,
        )
    }
}
