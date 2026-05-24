package com.localdownloader.domain.models

internal fun preferredDefaultChoiceForStreamType(
    streamType: StreamType,
    requestedContainer: String?,
    choices: List<FormatChoice>,
): FormatChoice? {
    if (choices.isEmpty()) return null
    if (streamType == StreamType.AUDIO_ONLY) return choices.firstOrNull()

    val normalizedContainer = requestedContainer?.trim()?.lowercase().orEmpty()
    if (normalizedContainer == "webm") {
        return choices.firstOrNull()
    }

    val usePlaybackFriendlyDefaults = normalizedContainer.isBlank() ||
        normalizedContainer == "auto" ||
        normalizedContainer == "mp4" ||
        normalizedContainer == "m4v" ||
        normalizedContainer == "mov" ||
        normalizedContainer == "mkv"
    if (!usePlaybackFriendlyDefaults) {
        return choices.firstOrNull()
    }

    return choices.maxWithOrNull(
        compareBy<FormatChoice>(
            { defaultChoicePriorityScore(normalizedContainer, it) },
            { it.height ?: 0 },
            { (it.fps ?: 0.0).toInt() },
            { it.bitrateKbps ?: 0 },
        ),
    )
}

private fun defaultChoicePriorityScore(
    requestedContainer: String,
    choice: FormatChoice,
): Int {
    val normalizedContainer = choice.container.trim().lowercase()
    return codecCompatibilityScore(choice.videoCodec) +
        containerCompatibilityScore(requestedContainer, normalizedContainer) +
        resolutionCompatibilityScore(choice.height) +
        mergedPlaybackScore(choice) +
        imageLikePenalty(choice)
}

private fun codecCompatibilityScore(codec: String?): Int {
    val normalized = codec?.trim()?.lowercase().orEmpty()
    return when {
        normalized.startsWith("avc1") || normalized.startsWith("h264") -> 520
        normalized.startsWith("hev1") || normalized.startsWith("hvc1") || normalized.startsWith("hevc") -> 440
        normalized.startsWith("vp9") -> 300
        normalized.startsWith("vp8") -> 270
        normalized.startsWith("av01") || normalized == "av1" -> 220
        normalized.isBlank() -> 260
        else -> 320
    }
}

private fun containerCompatibilityScore(
    requestedContainer: String,
    choiceContainer: String,
): Int {
    val isMp4Family = choiceContainer in MP4_FAMILY_CONTAINERS
    return when {
        requestedContainer in MP4_FAMILY_CONTAINERS && isMp4Family -> 180
        isMp4Family -> 140
        choiceContainer == "mkv" -> 90
        choiceContainer == "webm" -> 20
        else -> 70
    }
}

private fun resolutionCompatibilityScore(height: Int?): Int {
    val safeHeight = height ?: return 0
    return when {
        safeHeight <= 720 -> 150
        safeHeight <= 1080 -> 220
        safeHeight <= 1440 -> 100
        safeHeight <= 2160 -> 10
        else -> -20
    }
}

private fun mergedPlaybackScore(choice: FormatChoice): Int {
    return when {
        choice.streamType != StreamType.VIDEO_AUDIO -> 0
        choice.isMerged || choice.selector.contains("+") -> 30
        else -> 10
    }
}

private fun imageLikePenalty(choice: FormatChoice): Int {
    return if (choice.isImageLike) -500 else 0
}

private val MP4_FAMILY_CONTAINERS = setOf(
    "mp4",
    "m4v",
    "mov",
)
