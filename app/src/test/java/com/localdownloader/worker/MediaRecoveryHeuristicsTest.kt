package com.localdownloader.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaRecoveryHeuristicsTest {
    @Test
    fun isRecoveredDurationPlausible_acceptsCloseExpectedDuration() {
        assertTrue(
            isRecoveredDurationPlausible(
                actualDurationMs = 238_000L,
                expectedDurationSeconds = 240L,
            ),
        )
    }

    @Test
    fun isRecoveredDurationPlausible_rejectsTinyRecoveredDurationForLongVideo() {
        assertFalse(
            isRecoveredDurationPlausible(
                actualDurationMs = 3_000L,
                expectedDurationSeconds = 240L,
            ),
        )
    }

    @Test
    fun isRecoveredDurationPlausible_acceptsPositiveDurationWithoutExpectation() {
        assertTrue(
            isRecoveredDurationPlausible(
                actualDurationMs = 2_500L,
                expectedDurationSeconds = null,
            ),
        )
    }
}
