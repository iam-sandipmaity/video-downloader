package com.localdownloader.worker

import java.io.File

internal enum class SplitArtifactRole {
    VIDEO,
    AUDIO,
}

internal fun isManagedSplitArtifactName(fileName: String): Boolean {
    val lower = fileName.lowercase()
    return lower.contains(".video.") || lower.contains(".audio.")
}

internal fun isLooseSplitArtifactName(fileName: String): Boolean {
    val lower = fileName.lowercase()
    return Regex(""".*\.f[a-z0-9_-]+.*\..+""").matches(lower) ||
        lower.contains(".fdash-") ||
        isManagedSplitArtifactName(lower)
}

internal fun splitArtifactRoleFromName(file: File): SplitArtifactRole? {
    val lower = file.name.lowercase()
    return when {
        lower.contains(".video.") -> SplitArtifactRole.VIDEO
        lower.contains(".audio.") -> SplitArtifactRole.AUDIO
        file.extension.lowercase() in EXCLUSIVE_VIDEO_ARTIFACT_EXTENSIONS -> SplitArtifactRole.VIDEO
        file.extension.lowercase() in EXCLUSIVE_AUDIO_ARTIFACT_EXTENSIONS -> SplitArtifactRole.AUDIO
        else -> null
    }
}

internal fun selectDistinctSplitArtifacts(
    candidates: List<File>,
    allowLooseArtifacts: Boolean,
    detectRole: (File) -> SplitArtifactRole?,
): Pair<File, File>? {
    val filtered = candidates
        .filter { candidate ->
            candidate.isFile &&
                candidate.length() > 0L &&
                if (allowLooseArtifacts) {
                    isLooseSplitArtifactName(candidate.name)
                } else {
                    isManagedSplitArtifactName(candidate.name)
                }
        }
        .sortedByDescending { it.lastModified() }

    val video = filtered.firstOrNull { detectRole(it) == SplitArtifactRole.VIDEO } ?: return null
    val audio = filtered.firstOrNull { it.absolutePath != video.absolutePath && detectRole(it) == SplitArtifactRole.AUDIO }
        ?: return null
    return video to audio
}

private val EXCLUSIVE_VIDEO_ARTIFACT_EXTENSIONS = setOf(
    "mp4",
    "m4v",
    "mkv",
    "mov",
    "avi",
    "3gp",
)

private val EXCLUSIVE_AUDIO_ARTIFACT_EXTENSIONS = setOf(
    "m4a",
    "mp3",
    "aac",
    "opus",
    "ogg",
    "weba",
    "flac",
    "wav",
)
