package com.localdownloader.domain.models

/**
 * Concrete selectable format option mapped to a yt-dlp format selector.
 */
data class FormatChoice(
    val selector: String,
    val label: String,
    val streamType: StreamType,
    val container: String,
    val height: Int?,
    val isMerged: Boolean,
)
