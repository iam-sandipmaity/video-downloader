package com.localdownloader.media

val VIDEO_FILE_EXTENSIONS = setOf(
    "mp4", "m4v", "mov", "mkv", "webm", "avi", "3gp",
    "ts", "m2ts", "mts", "mpeg", "mpg", "m2v", "vob",
    "wmv", "flv", "ogv", "ogm", "mxf", "asf",
)

val AUDIO_FILE_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "opus", "ogg", "oga", "wav", "flac",
    "amr", "3ga", "wma", "weba", "mka",
)

val PLAYABLE_MEDIA_EXTENSIONS: Set<String> = VIDEO_FILE_EXTENSIONS + AUDIO_FILE_EXTENSIONS

private val VARIABLE_DEVICE_VIDEO_EXTENSIONS = setOf(
    "webm", "mkv", "avi", "ts", "m2ts", "mts", "mpeg", "mpg",
    "m2v", "vob", "wmv", "flv", "ogv", "ogm", "mxf", "asf",
)

fun normalizeMediaExtension(value: String?): String {
    return value
        ?.substringAfterLast('.', "")
        ?.trim()
        ?.lowercase()
        .orEmpty()
}

fun isLikelyVideoPath(path: String?): Boolean = normalizeMediaExtension(path) in VIDEO_FILE_EXTENSIONS

fun isLikelyAudioPath(path: String?): Boolean = normalizeMediaExtension(path) in AUDIO_FILE_EXTENSIONS

fun isLikelyPlayableMediaPath(path: String?): Boolean = normalizeMediaExtension(path) in PLAYABLE_MEDIA_EXTENSIONS

fun resolvePreferredMediaMimeType(pathOrName: String?): String? {
    return resolvePreferredMediaMimeTypeForExtension(normalizeMediaExtension(pathOrName))
}

fun resolvePreferredMediaMimeTypeForExtension(extension: String?): String? {
    return when (extension?.trim()?.lowercase().orEmpty()) {
        "mp4", "m4v" -> "video/mp4"
        "mov" -> "video/quicktime"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "3gp" -> "video/3gpp"
        "ts", "m2ts", "mts" -> "video/mp2t"
        "mpeg", "mpg", "m2v", "vob" -> "video/mpeg"
        "wmv" -> "video/x-ms-wmv"
        "flv" -> "video/x-flv"
        "ogv", "ogm" -> "video/ogg"
        "mp3" -> "audio/mpeg"
        "aac" -> "audio/aac"
        "m4a" -> "audio/mp4"
        "opus", "ogg", "oga" -> "audio/ogg"
        "wav" -> "audio/wav"
        "flac" -> "audio/flac"
        "amr", "3ga" -> "audio/amr"
        "wma" -> "audio/x-ms-wma"
        "weba" -> "audio/webm"
        "mka" -> "audio/x-matroska"
        else -> null
    }
}

fun builtInPlaybackCompatibilityLabel(path: String?): String? {
    val extension = normalizeMediaExtension(path)
    return extension
        .takeIf { it in VARIABLE_DEVICE_VIDEO_EXTENSIONS }
        ?.uppercase()
}
