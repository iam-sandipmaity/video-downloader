package com.localdownloader.downloader

import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType

data class MergeContainerCompatibility(
    val resolvedContainer: String?,
    val queueNote: String? = null,
)

internal fun isAutomaticContainerSelection(requestedContainer: String?): Boolean {
    return normalizeRequestedContainer(requestedContainer) == AUTO_CONTAINER
}

internal fun isChoiceCompatibleWithRequestedContainer(
    requestedContainer: String?,
    selectedChoice: FormatChoice?,
): Boolean {
    val normalizedContainer = normalizeRequestedContainer(requestedContainer) ?: return true
    val choice = selectedChoice ?: return true

    if (normalizedContainer == AUTO_CONTAINER || choice.streamType != StreamType.VIDEO_AUDIO) {
        return true
    }

    return when {
        normalizedContainer == "mkv" -> true
        normalizedContainer == "webm" -> shouldPreferWebmContainer(choice)
        normalizedContainer in MP4_FAMILY_CONTAINERS ->
            resolveMergeContainerCompatibility(normalizedContainer, choice).resolvedContainer == normalizedContainer
        else -> true
    }
}

internal fun resolveMergeContainerCompatibility(
    requestedContainer: String?,
    selectedChoice: FormatChoice?,
): MergeContainerCompatibility {
    val normalizedContainer = normalizeRequestedContainer(requestedContainer)
        ?: return MergeContainerCompatibility(resolvedContainer = null)
    val choice = selectedChoice ?: return MergeContainerCompatibility(resolvedContainer = normalizedContainer)

    if (choice.streamType != StreamType.VIDEO_AUDIO) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
    if (!choice.isMerged && !choice.selector.contains("+")) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
    if (normalizedContainer in MP4_FAMILY_CONTAINERS && shouldPreferWebmContainer(choice)) {
        return MergeContainerCompatibility(
            resolvedContainer = "webm",
            queueNote = "WebM-friendly video was switched from MP4 to WEBM for a more reliable merge.",
        )
    }
    if (normalizedContainer !in MP4_FAMILY_CONTAINERS) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
    if (!isAv1Codec(choice.videoCodec)) {
        return MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }

    return if (isAv1Codec(choice.videoCodec)) {
        MergeContainerCompatibility(
            resolvedContainer = "mkv",
            queueNote = "AV1 video was switched from MP4 to MKV for more reliable merging.",
        )
    } else {
        MergeContainerCompatibility(resolvedContainer = normalizedContainer)
    }
}

private fun normalizeRequestedContainer(requestedContainer: String?): String? {
    return requestedContainer?.trim()?.lowercase()?.ifBlank { null }
}

private fun isAv1Codec(codec: String?): Boolean {
    val normalized = codec?.trim()?.lowercase().orEmpty()
    return normalized == "av1" || normalized.startsWith("av01")
}

private fun shouldPreferWebmContainer(choice: FormatChoice): Boolean {
    val normalizedContainer = choice.container.trim().lowercase()
    val normalizedVideoCodec = choice.videoCodec?.trim()?.lowercase().orEmpty()
    val normalizedAudioCodec = choice.audioCodec?.trim()?.lowercase().orEmpty()

    return normalizedContainer == "webm" ||
        normalizedVideoCodec.startsWith("vp9") ||
        normalizedVideoCodec.startsWith("vp8") ||
        normalizedAudioCodec.startsWith("opus") ||
        normalizedAudioCodec.startsWith("vorbis")
}

private val MP4_FAMILY_CONTAINERS = setOf(
    "mp4",
    "m4v",
    "mov",
)

private const val AUTO_CONTAINER = "auto"
