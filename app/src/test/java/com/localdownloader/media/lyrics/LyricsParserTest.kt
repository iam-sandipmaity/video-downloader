package com.localdownloader.media.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {

    @Test
    fun parseLrc_standardFormat_parsesLinesCorrectly() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [al:Test Album]
            [00:10.50]First line of lyric
            [00:20.00]Second line of lyric
            [01:05.250]Third line with 3 digits millis
        """.trimIndent()

        val doc = LyricsParser.parse(lrc, "lrc")

        assertTrue(doc.isSynced)
        assertEquals("Test Title", doc.title)
        assertEquals("Test Artist", doc.artist)
        assertEquals("Test Album", doc.album)
        assertEquals(3, doc.lines.size)

        assertEquals(10500L, doc.lines[0].startTimeMs)
        assertEquals("First line of lyric", doc.lines[0].text)
        assertEquals(20000L, doc.lines[0].endTimeMs)

        assertEquals(20000L, doc.lines[1].startTimeMs)
        assertEquals("Second line of lyric", doc.lines[1].text)
        assertEquals(65250L, doc.lines[1].endTimeMs)

        assertEquals(65250L, doc.lines[2].startTimeMs)
        assertEquals("Third line with 3 digits millis", doc.lines[2].text)
    }

    @Test
    fun parseLrc_multipleTimestampsOnSingleLine_expandsAndSorts() {
        val lrc = """
            [00:10.00][00:30.00]Repeated chorus
            [00:20.00]Verse in between
        """.trimIndent()

        val doc = LyricsParser.parse(lrc, "lrc")

        assertEquals(3, doc.lines.size)
        assertEquals(10000L, doc.lines[0].startTimeMs)
        assertEquals("Repeated chorus", doc.lines[0].text)

        assertEquals(20000L, doc.lines[1].startTimeMs)
        assertEquals("Verse in between", doc.lines[1].text)

        assertEquals(30000L, doc.lines[2].startTimeMs)
        assertEquals("Repeated chorus", doc.lines[2].text)
    }

    @Test
    fun parseLrc_withOffsetHeader_adjustsTimestamps() {
        val lrc = """
            [offset:500]
            [00:10.00]Offset adjusted line
        """.trimIndent()

        val doc = LyricsParser.parse(lrc, "lrc")

        assertEquals(10500L, doc.lines[0].startTimeMs)
    }

    @Test
    fun parseSrt_parsesSubtitlesAndStripsHtmlTags() {
        val srt = """
            1
            00:00:10,500 --> 00:00:14,200
            <i>First line</i> with formatting

            2
            00:00:15,000 --> 00:00:18,500
            Second line <font color="red">styled</font>
        """.trimIndent()

        val doc = LyricsParser.parse(srt, "srt")

        assertTrue(doc.isSynced)
        assertEquals(2, doc.lines.size)

        assertEquals(10500L, doc.lines[0].startTimeMs)
        assertEquals(14200L, doc.lines[0].endTimeMs)
        assertEquals("First line with formatting", doc.lines[0].text)

        assertEquals(15000L, doc.lines[1].startTimeMs)
        assertEquals(18500L, doc.lines[1].endTimeMs)
        assertEquals("Second line styled", doc.lines[1].text)
    }

    @Test
    fun parseVtt_parsesWebVttFormat() {
        val vtt = """
            WEBVTT

            00:00:05.100 --> 00:00:08.200
            <v Singer>Hello world</v>

            00:00:10.000 --> 00:00:15.000
            <c.yellow>Second line</c>
        """.trimIndent()

        val doc = LyricsParser.parse(vtt, "vtt")

        assertTrue(doc.isSynced)
        assertEquals(2, doc.lines.size)

        assertEquals(5100L, doc.lines[0].startTimeMs)
        assertEquals(8200L, doc.lines[0].endTimeMs)
        assertEquals("Hello world", doc.lines[0].text)

        assertEquals(10000L, doc.lines[1].startTimeMs)
        assertEquals(15000L, doc.lines[1].endTimeMs)
        assertEquals("Second line", doc.lines[1].text)
    }

    @Test
    fun parseFallback_plainText_marksUnsynced() {
        val text = """
            Line one of poem
            Line two of poem
        """.trimIndent()

        val doc = LyricsParser.parse(text)

        assertFalse(doc.isSynced)
        assertEquals(2, doc.lines.size)
        assertEquals("Line one of poem", doc.lines[0].text)
        assertEquals("Line two of poem", doc.lines[1].text)
    }

    @Test
    fun lyricsDocument_getActiveIndex_returnsCorrectIndex() {
        val doc = LyricsDocument(
            lines = listOf(
                LyricLine(startTimeMs = 1000L, text = "Line 1"),
                LyricLine(startTimeMs = 5000L, text = "Line 2"),
                LyricLine(startTimeMs = 10000L, text = "Line 3"),
            ),
            isSynced = true,
        )

        assertEquals(-1, doc.getActiveIndex(500L))
        assertEquals(0, doc.getActiveIndex(1000L))
        assertEquals(0, doc.getActiveIndex(4999L))
        assertEquals(1, doc.getActiveIndex(5000L))
        assertEquals(1, doc.getActiveIndex(7000L))
        assertEquals(2, doc.getActiveIndex(10000L))
        assertEquals(2, doc.getActiveIndex(20000L))
    }

    @Test
    fun lyricsDocument_getLyricWindow_returns7ItemsCentered() {
        val lines = (0..10).map { i -> LyricLine(startTimeMs = i * 1000L, text = "Line $i") }
        val doc = LyricsDocument(lines = lines, isSynced = true)

        val window = doc.getLyricWindow(centerIndex = 5, radius = 3)
        assertEquals(7, window.size)
        assertEquals(-3, window[0].relativeOffset)
        assertEquals("Line 2", window[0].line?.text)
        assertEquals(0, window[3].relativeOffset)
        assertEquals("Line 5", window[3].line?.text)
        assertEquals(3, window[6].relativeOffset)
        assertEquals("Line 8", window[6].line?.text)

        val startWindow = doc.getLyricWindow(centerIndex = 0, radius = 3)
        assertEquals(7, startWindow.size)
        assertEquals(null, startWindow[0].line) // index -3 is null
        assertEquals(null, startWindow[1].line) // index -2 is null
        assertEquals(null, startWindow[2].line) // index -1 is null
        assertEquals("Line 0", startWindow[3].line?.text)
        assertEquals("Line 1", startWindow[4].line?.text)
    }
}
