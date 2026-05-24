package com.localdownloader.viewmodel

internal fun friendlyPlaybackErrorMessage(
    rawMessage: String?,
    playablePath: String?,
): String {
    val trimmed = rawMessage?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return "Unable to play this media file."
    }

    val normalized = trimmed.lowercase()
    if (
        normalized.contains("no_exceeds_capabilities") ||
        normalized.contains("mediacodecvideorenderer error") ||
        normalized.contains("decoder init failed")
    ) {
        val resolution = Regex("\\[(\\d+),\\s*(\\d+)").find(trimmed)?.let {
            "${it.groupValues[1]}x${it.groupValues[2]}"
        }
        val codecLabel = when {
            normalized.contains("video/x-vnd.on2.vp9") || normalized.contains("vp9") -> "VP9"
            normalized.contains("av01") || normalized.contains("av1") -> "AV1"
            normalized.contains("hev1") || normalized.contains("hvc1") || normalized.contains("hevc") -> "HEVC"
            normalized.contains("avc1") || normalized.contains("h264") -> "H.264"
            else -> null
        }
        val containerLabel = playablePath
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
        val formatSummary = listOfNotNull(resolution, codecLabel, containerLabel)
            .joinToString(" ")
            .ifBlank { "this format" }
        return "This device can't play $formatSummary in the built-in player. Try a 1080p MP4 download or open it in another player."
    }

    return trimmed
}
