package com.localdownloader.ffmpeg

private const val MAX_USER_VISIBLE_FAILURE_CHARS = 240

private val UNSUPPORTED_DECODER_REGEX =
    Regex("""decoder \(codec ([^)]+)\) not found for input stream""", RegexOption.IGNORE_CASE)
private val UNKNOWN_ENCODER_REGEX =
    Regex("""unknown encoder ['"]?([^'"\s]+)['"]?""", RegexOption.IGNORE_CASE)

fun suggestedCompressionOutputExtension(inputPath: String): String {
    val ext = inputPath.substringAfterLast('.', "").lowercase()
    return when {
        ext in AUDIO_OUTPUT_FORMATS -> ext
        ext in VIDEO_OUTPUT_FORMATS -> ext
        ext == "webm" -> "mp4"
        inputPath.guessContentType() == MediaContentType.AUDIO -> "mp3"
        else -> "mp4"
    }
}

fun audioCodecArgsForOutput(outputExt: String): List<String> {
    return when (outputExt.lowercase()) {
        "mp3" -> listOf("-c:a", "libmp3lame")
        "m4a", "aac" -> listOf("-c:a", "aac")
        "wav" -> listOf("-c:a", "pcm_s16le")
        "flac" -> listOf("-c:a", "flac")
        "ogg" -> listOf("-c:a", "libvorbis")
        "opus" -> listOf("-c:a", "libopus")
        else -> emptyList()
    }
}

fun videoAudioCodecArgsForContainer(outputExt: String): List<String> {
    return when (outputExt.lowercase()) {
        "mp4", "mov" -> listOf("-c:a", "aac", "-movflags", "+faststart")
        "mkv" -> listOf("-c:a", "aac")
        "avi", "flv" -> listOf("-c:a", "mp3")
        else -> listOf("-c:a", "aac")
    }
}

fun summarizeMediaToolFailure(
    rawError: String,
    fallbackMessage: String,
): String {
    unsupportedDecoderMessage(rawError)?.let { return it }
    unknownEncoderMessage(rawError)?.let { return it }
    containerCompatibilityMessage(rawError)?.let { return it }

    return preferredFfmpegFailureLine(rawError)
        ?.take(MAX_USER_VISIBLE_FAILURE_CHARS)
        ?.ifBlank { fallbackMessage }
        ?: fallbackMessage
}

private fun unsupportedDecoderMessage(rawError: String): String? {
    val codec = UNSUPPORTED_DECODER_REGEX.find(rawError)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val codecLabel = when (codec.lowercase()) {
        "av1" -> "AV1"
        "hevc", "h265" -> "HEVC/H.265"
        "h264", "avc1" -> "H.264"
        "vp9" -> "VP9"
        "vp8" -> "VP8"
        else -> codec.uppercase()
    }

    return "This file uses $codecLabel media, but the current FFmpeg runtime can't decode it. Open More > Updates > FFmpeg update, install the latest runtime, and try again."
}

private fun unknownEncoderMessage(rawError: String): String? {
    val encoder = UNKNOWN_ENCODER_REGEX.find(rawError)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return "The current FFmpeg runtime can't encode $encoder output yet. Try MP4 or MP3, or install the latest FFmpeg runtime from More > Updates."
}

private fun containerCompatibilityMessage(rawError: String): String? {
    val normalized = rawError.lowercase()
    if (
        normalized.contains("codec not currently supported in container") ||
        normalized.contains("could not find tag for codec") ||
        (normalized.contains("could not write header") && normalized.contains("incorrect codec parameters"))
    ) {
        return "That output container is not compatible with the current encode settings. Try MP4 or MKV instead."
    }
    return null
}

private fun preferredFfmpegFailureLine(detail: String): String? {
    val lines = detail.lineSequence()
        .map { it.trim().removePrefix("ERROR: ") }
        .filter { it.isNotBlank() }
        .toList()
    if (lines.isEmpty()) return null

    val prioritySnippets = listOf(
        "decoder (codec",
        "not found for input stream",
        "unknown encoder",
        "unknown decoder",
        "codec not currently supported in container",
        "could not find tag for codec",
        "could not write header",
        "invalid argument",
        "permission denied",
        "no such file or directory",
        "error while",
    )

    prioritySnippets.forEach { snippet ->
        lines.asReversed().firstOrNull { it.lowercase().contains(snippet) }?.let { return it }
    }

    return lines.lastOrNull { line ->
        val lower = line.lowercase()
        !lower.startsWith("ffmpeg version") &&
            !lower.startsWith("built with") &&
            !lower.startsWith("configuration:") &&
            !lower.startsWith("libav")
    } ?: lines.last()
}
