package com.localdownloader.domain.models

/**
 * Represents a single format entry returned by yt-dlp.
 */
data class MediaFormat(
    val formatId: String,
    val extension: String,
    val container: String,
    val resolution: String?,
    val videoCodec: String,
    val audioCodec: String,
    val fileSizeBytes: Long?,
    val bitrateKbps: Int?,
    val fps: Double?,
    val note: String?,
) {
    val hasVideo: Boolean
        get() = videoCodec != "none"

    val hasAudio: Boolean
        get() = audioCodec != "none"

    val isAudioOnly: Boolean
        get() = !hasVideo && hasAudio

    val isVideoOnly: Boolean
        get() = hasVideo && !hasAudio

    val heightPixels: Int?
        get() = resolution?.substringBefore("p")?.toIntOrNull()

    fun asReadableLabel(): String {
        val qualityPart = resolution ?: if (isAudioOnly) "audio" else "unknown"
        val bitratePart = bitrateKbps?.let { "${it}kbps" } ?: ""
        return listOf(formatId, extension, qualityPart, bitratePart, note.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" | ")
    }
}
