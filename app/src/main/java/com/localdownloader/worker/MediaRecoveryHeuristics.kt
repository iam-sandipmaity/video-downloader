package com.localdownloader.worker

internal fun isRecoveredDurationPlausible(
    actualDurationMs: Long,
    expectedDurationSeconds: Long?,
): Boolean {
    if (actualDurationMs <= 0L) return false

    val expectedSeconds = expectedDurationSeconds?.takeIf { it > 0L } ?: return true
    val actualSeconds = ((actualDurationMs + 999L) / 1000L).coerceAtLeast(1L)

    if (expectedSeconds <= SHORT_CLIP_SECONDS) {
        return actualSeconds in 1L..(expectedSeconds + SHORT_CLIP_UPPER_SLACK_SECONDS)
    }

    val lowerBoundSeconds = maxOf(1L, (expectedSeconds * EXPECTED_DURATION_LOWER_PERCENT) / 100L)
    val upperBoundSeconds = expectedSeconds + maxOf(
        EXPECTED_DURATION_MIN_UPPER_SLACK_SECONDS,
        (expectedSeconds * EXPECTED_DURATION_UPPER_PERCENT_SLACK) / 100L,
    )
    return actualSeconds in lowerBoundSeconds..upperBoundSeconds
}

private const val SHORT_CLIP_SECONDS = 5L
private const val SHORT_CLIP_UPPER_SLACK_SECONDS = 15L
private const val EXPECTED_DURATION_LOWER_PERCENT = 60L
private const val EXPECTED_DURATION_UPPER_PERCENT_SLACK = 50L
private const val EXPECTED_DURATION_MIN_UPPER_SLACK_SECONDS = 15L
