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
    val normalizedExtension: String
        get() = extension.trim().lowercase()

    val normalizedContainer: String
        get() = container.trim().lowercase()

    val isAudioOnly: Boolean
        get() = videoCodec == "none" && audioCodec != "none"

    val isVideoOnly: Boolean
        get() = videoCodec != "none" && audioCodec == "none"

    val isImageLike: Boolean
        get() = normalizedExtension in IMAGE_LIKE_EXTENSIONS || normalizedContainer in IMAGE_LIKE_EXTENSIONS

    fun asReadableLabel(): String {
        val qualityPart = resolution ?: if (isAudioOnly) "audio" else "unknown"
        val bitratePart = bitrateKbps?.let { "${it}kbps" } ?: ""
        return listOf(formatId, extension, qualityPart, bitratePart, note.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString(" | ")
    }

    private companion object {
        private val IMAGE_LIKE_EXTENSIONS = setOf(
            "apng",
            "avif",
            "bmp",
            "gif",
            "jpeg",
            "jpg",
            "png",
            "webp",
        )
    }
}
