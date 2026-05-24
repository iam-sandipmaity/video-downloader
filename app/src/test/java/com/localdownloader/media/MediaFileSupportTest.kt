package com.localdownloader.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFileSupportTest {

    @Test
    fun recognizesTransportStreamAsVideo() {
        assertTrue(isLikelyVideoPath("sample.m2ts"))
        assertTrue(isLikelyPlayableMediaPath("sample.ts"))
    }

    @Test
    fun resolvesPreferredMimeTypesForCommonContainers() {
        assertEquals("video/x-matroska", resolvePreferredMediaMimeType("movie.mkv"))
        assertEquals("video/mp2t", resolvePreferredMediaMimeType("movie.ts"))
        assertEquals("audio/webm", resolvePreferredMediaMimeType("track.weba"))
    }

    @Test
    fun flagsVariableCompatibilityContainers() {
        assertEquals("WEBM", builtInPlaybackCompatibilityLabel("clip.webm"))
        assertNull(builtInPlaybackCompatibilityLabel("clip.mp4"))
    }
}
