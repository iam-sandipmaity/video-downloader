package com.localdownloader.domain.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatSupportTest {
    @Test
    fun audioFormatSupportsBitrateControl_returnsFalseForLosslessAndUncompressedFormats() {
        assertFalse(audioFormatSupportsBitrateControl("flac"))
        assertFalse(audioFormatSupportsBitrateControl("wav"))
    }

    @Test
    fun audioFormatSupportsBitrateControl_returnsTrueForLossyFormats() {
        assertTrue(audioFormatSupportsBitrateControl("mp3"))
        assertTrue(audioFormatSupportsBitrateControl("m4a"))
        assertTrue(audioFormatSupportsBitrateControl("opus"))
    }
}
