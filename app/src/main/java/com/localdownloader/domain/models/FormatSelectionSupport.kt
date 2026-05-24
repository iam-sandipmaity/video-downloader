package com.localdownloader.domain.models

internal fun MediaFormat.shouldTreatAsAudioOnlyChoice(): Boolean {
    return isAudioOnly || normalizedExtension in AUDIO_ONLY_CONTAINERS || normalizedContainer in AUDIO_ONLY_CONTAINERS
}

internal fun availableOutputTransforms(
    sourceStreamType: StreamType,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): List<OutputTransform> {
    if (sourceStreamType != StreamType.VIDEO_AUDIO || videoAudioChoices.isEmpty()) {
        return listOf(OutputTransform.NONE)
    }

    return buildList {
        add(OutputTransform.NONE)
        if (audioOnlyChoices.isEmpty()) {
            add(OutputTransform.EXTRACT_AUDIO)
        }
        if (videoOnlyChoices.isEmpty()) {
            add(OutputTransform.REMOVE_AUDIO)
        }
    }
}

internal fun resolveAvailableOutputTransform(
    preferredTransform: OutputTransform,
    sourceStreamType: StreamType,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): OutputTransform {
    val available = availableOutputTransforms(
        sourceStreamType = sourceStreamType,
        videoAudioChoices = videoAudioChoices,
        videoOnlyChoices = videoOnlyChoices,
        audioOnlyChoices = audioOnlyChoices,
    )
    return preferredTransform.takeIf { it in available } ?: OutputTransform.NONE
}

internal fun effectiveOutputStreamType(
    sourceStreamType: StreamType,
    outputTransform: OutputTransform,
): StreamType {
    return when (outputTransform) {
        OutputTransform.NONE -> sourceStreamType
        OutputTransform.EXTRACT_AUDIO -> StreamType.AUDIO_ONLY
        OutputTransform.REMOVE_AUDIO -> StreamType.VIDEO_ONLY
    }
}

internal fun choicesForStreamType(
    streamType: StreamType,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): List<FormatChoice> {
    return when (streamType) {
        StreamType.VIDEO_AUDIO -> videoAudioChoices
        StreamType.VIDEO_ONLY -> videoOnlyChoices
        StreamType.AUDIO_ONLY -> audioOnlyChoices
    }
}

internal fun hasChoicesForStreamType(
    streamType: StreamType,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): Boolean {
    return choicesForStreamType(
        streamType = streamType,
        videoAudioChoices = videoAudioChoices,
        videoOnlyChoices = videoOnlyChoices,
        audioOnlyChoices = audioOnlyChoices,
    ).isNotEmpty()
}

internal fun resolveAvailableStreamType(
    preferredStreamType: StreamType,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): StreamType {
    if (
        hasChoicesForStreamType(
            streamType = preferredStreamType,
            videoAudioChoices = videoAudioChoices,
            videoOnlyChoices = videoOnlyChoices,
            audioOnlyChoices = audioOnlyChoices,
        )
    ) {
        return preferredStreamType
    }

    return when {
        videoAudioChoices.isNotEmpty() -> StreamType.VIDEO_AUDIO
        videoOnlyChoices.isNotEmpty() -> StreamType.VIDEO_ONLY
        audioOnlyChoices.isNotEmpty() -> StreamType.AUDIO_ONLY
        else -> preferredStreamType
    }
}

private val AUDIO_ONLY_CONTAINERS = setOf(
    "m4a",
    "mp3",
    "aac",
    "opus",
    "ogg",
    "oga",
    "flac",
    "wav",
    "amr",
    "mka",
    "weba",
)
