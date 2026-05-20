package com.localdownloader.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaToolSupportTest {

    @Test
    fun `suggested compression output keeps supported video container`() {
        assertEquals("mkv", suggestedCompressionOutputExtension("/storage/emulated/0/Movies/sample.mkv"))
    }

    @Test
    fun `suggested compression output converts webm to mp4`() {
        assertEquals("mp4", suggestedCompressionOutputExtension("/storage/emulated/0/Movies/sample.webm"))
    }

    @Test
    fun `suggested compression output keeps audio extension`() {
        assertEquals("mp3", suggestedCompressionOutputExtension("/storage/emulated/0/Music/sample.mp3"))
    }

    @Test
    fun `mkv container audio args do not include movflags`() {
        val args = videoAudioCodecArgsForContainer("mkv")
        assertTrue(args.containsAll(listOf("-c:a", "aac")))
        assertFalse(args.contains("-movflags"))
    }

    @Test
    fun `summarize failure returns friendly decoder guidance`() {
        val raw = """
            Stream mapping:
              Stream #0:0 -> #0:0 (? (?) -> mpeg4 (native))
            Decoder (codec av1) not found for input stream #0:0
        """.trimIndent()

        assertEquals(
            "This file uses AV1 media, but the current FFmpeg runtime can't decode it. Open More > Updates > FFmpeg update, install the latest runtime, and try again.",
            summarizeMediaToolFailure(raw, "FFmpeg conversion failed"),
        )
    }

    @Test
    fun `summarize failure prefers short actionable line`() {
        val raw = """
            ffmpeg version 3.3.2
            configuration: --disable-everything
            Could not write header for output file #0 (incorrect codec parameters ?): Invalid argument
        """.trimIndent()

        assertEquals(
            "That output container is not compatible with the current encode settings. Try MP4 or MKV instead.",
            summarizeMediaToolFailure(raw, "FFmpeg compression failed"),
        )
    }
}
