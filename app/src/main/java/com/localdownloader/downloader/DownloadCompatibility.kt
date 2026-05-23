package com.localdownloader.downloader

import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType

data class MergeContainerCompatibility(
    val resolvedContainer: String?,
    val queueNote: String? = null,
)

internal fun resolveMergeContainerCompatibility(
    requestedContainer: String?,
    selectedChoice: FormatChoice?,
): MergeContainerCompatibility {
    val normalizedContainer = requestedContainer?.trim()?.lowercase()?.ifBlank { null }
        ?: return MergeContainerCompatibility(resolvedContainer = null)
    val choice = selectedChoice ?: return MergeContainerCompatibility(resolvedContainer = normalizedContainer)

    if (choice.streamType != StreamType.VIDEO_AUDIO) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
    if (!choice.isMerged && !choice.selector.contains("+")) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
    if (normalizedContainer !in MP4_FAMILY_CONTAINERS) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
    if (!isAv1Codec(choice.videoCodec)) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }

    return if ((choice.height ?: 0) >= HIGH_RES_AV1_HEIGHT_THRESHOLD) {
        MergeContainerCompatibility(
            resolvedContainer = "mkv",
            queueNote = "High-resolution AV1 video was switched from MP4 to MKV for more reliable merging.",
        )
    } else {
        MergeContainerCompatibility(
            resolvedContainer = normalizedContainer,
            queueNote = "AV1 MP4 merges can still vary by FFmpeg support on some devices.",
        )
    }
}

private fun isAv1Codec(codec: String?): Boolean {
    val normalized = codec?.trim()?.lowercase().orEmpty()
    return normalized == "av1" || normalized.startsWith("av01")
}

private const val HIGH_RES_AV1_HEIGHT_THRESHOLD = 1440

private val MP4_FAMILY_CONTAINERS = setOf(
    "mp4",
    "m4v",
    "mov",
)
