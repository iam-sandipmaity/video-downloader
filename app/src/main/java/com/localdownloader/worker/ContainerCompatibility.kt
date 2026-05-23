package com.localdownloader.worker

internal fun resolveCompatibleContainerFallback(
    requestedExtension: String,
    videoExtension: String,
    audioExtension: String,
    stderr: String,
): String? {
    val extension = requestedExtension.trim().lowercase()
    val normalizedVideoExtension = videoExtension.trim().lowercase()
    val normalizedAudioExtension = audioExtension.trim().lowercase()
    val lower = stderr.lowercase()

    val hasExplicitContainerFailure = lower.contains("codec not currently supported in container") ||
        lower.contains("could not find tag for codec") ||
        (lower.contains("could not write header") && lower.contains("incorrect codec parameters"))
    val hasStreamCopyMuxerFailure = lower.contains("stream #") ||
        lower.contains("could not write header") ||
        lower.contains("error initializing output stream") ||
        lower.contains("incorrect codec parameters")
    val hasWebmLikeSource = normalizedVideoExtension == "webm" ||
        normalizedAudioExtension in WEBM_LIKE_AUDIO_EXTENSIONS

    return when {
        extension in MP4_FAMILY_EXTENSIONS && (hasExplicitContainerFailure || (extension == "mp4" && hasWebmLikeSource)) -> {
            if (normalizedVideoExtension == "webm" && normalizedAudioExtension in WEBM_LIKE_AUDIO_EXTENSIONS) {
                "webm"
            } else {
                "mkv"
            }
        }
        extension == "webm" && hasStreamCopyMuxerFailure -> "mkv"
        else -> null
    }
}

private val MP4_FAMILY_EXTENSIONS = setOf(
    "mp4",
    "m4v",
    "mov",
    "3gp",
)

private val WEBM_LIKE_AUDIO_EXTENSIONS = setOf(
    "webm",
    "weba",
    "opus",
    "ogg",
)
