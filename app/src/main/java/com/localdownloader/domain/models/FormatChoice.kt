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
    val isImageLike: Boolean,
    val fileSizeBytes: Long? = null,
    val estimatedSizeBytes: Long? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val fps: Double? = null,
    val bitrateKbps: Int? = null,
    val note: String? = null,
)
