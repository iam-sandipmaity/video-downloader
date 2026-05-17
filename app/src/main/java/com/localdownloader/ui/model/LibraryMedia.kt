package com.localdownloader.ui.model

import com.localdownloader.audio.AudioQueueItem
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun buildVideoLibraryItems(tasks: List<DownloadTask>): List<VideoLibraryItem> {
    return tasks
        .filter { it.status == DownloadStatus.COMPLETED }
        .sortedByDescending { it.updatedAtEpochMs }
        .map { task ->
            val file = task.outputPath?.let(::File)
            val resolvedSize = file
                ?.takeIf { it.exists() }
                ?.length()
                ?.toReadableSize()
                ?.takeIf { it.isNotBlank() }
                ?: task.totalSizeStr.meaningfulSizeLabel()
                ?: task.downloadedStr.meaningfulSizeLabel()
                ?: ""
            VideoLibraryItem(
                task = task,
                file = file,
                displayTitle = task.title.ifBlank { file?.nameWithoutExtension ?: file?.name ?: "Saved media" },
                displaySize = resolvedSize,
                exists = file?.exists() == true,
                mediaKind = resolveMediaKind(file),
            )
        }
}

fun List<VideoLibraryItem>.toAudioQueueItems(): List<AudioQueueItem> {
    return filter { it.exists && it.mediaKind == MediaKind.AUDIO }
        .map { item ->
            AudioQueueItem(
                taskId = item.task.id,
                title = item.displayTitle,
                filePath = item.file?.absolutePath.orEmpty(),
            )
        }
}

fun resolveMediaKind(file: File?): MediaKind {
    val extension = file?.extension?.lowercase().orEmpty()
    return when (extension) {
        "mp4", "mkv", "webm", "mov", "avi", "m4v", "3gp" -> MediaKind.VIDEO
        "mp3", "m4a", "aac", "opus", "ogg", "wav", "flac", "amr" -> MediaKind.AUDIO
        else -> MediaKind.OTHER
    }
}

val MediaKind.label: String
    get() = when (this) {
        MediaKind.VIDEO -> "Video"
        MediaKind.AUDIO -> "Audio"
        MediaKind.OTHER -> "Other"
    }

fun formatMediaDate(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    return formatter.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
}

fun Long.toReadableSize(): String {
    if (this <= 0L) return ""
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    return when {
        this >= gib -> "${(this / gib * 10.0).roundToInt() / 10.0} GB"
        this >= mib -> "${(this / mib * 10.0).roundToInt() / 10.0} MB"
        this >= kib -> "${(this / kib * 10.0).roundToInt() / 10.0} KB"
        else -> "$this B"
    }
}

fun String?.meaningfulSizeLabel(): String? {
    val normalized = this?.trim().orEmpty()
    if (normalized.isBlank()) return null
    if (normalized.equals("na", ignoreCase = true)) return null
    if (normalized.equals("n/a", ignoreCase = true)) return null
    if (normalized.equals("unknown", ignoreCase = true)) return null
    return normalized
}

fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun Long.toShortTimerLabel(): String {
    val totalMinutes = (this / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
