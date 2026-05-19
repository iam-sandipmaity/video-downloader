package com.localdownloader.viewmodel

internal fun estimateFormatSizeBytes(durationSeconds: Long?, bitrateKbps: Int?): Long? {
    val safeDurationSeconds = durationSeconds?.takeIf { it > 0L } ?: return null
    val safeBitrateKbps = bitrateKbps?.takeIf { it > 0 } ?: return null
    return (safeDurationSeconds * safeBitrateKbps * 1_000L) / 8L
}
