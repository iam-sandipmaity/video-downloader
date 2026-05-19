package com.localdownloader.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatSizeEstimateTest {

    @Test
    fun estimateFormatSizeBytes_usesDurationAndBitrate() {
        val estimate = estimateFormatSizeBytes(
            durationSeconds = 120L,
            bitrateKbps = 256,
        )

        assertEquals(3_840_000L, estimate)
    }

    @Test
    fun estimateFormatSizeBytes_returnsNullForMissingInputs() {
        assertNull(estimateFormatSizeBytes(durationSeconds = null, bitrateKbps = 256))
        assertNull(estimateFormatSizeBytes(durationSeconds = 120L, bitrateKbps = null))
        assertNull(estimateFormatSizeBytes(durationSeconds = 0L, bitrateKbps = 256))
    }
}
