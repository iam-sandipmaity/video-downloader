package com.localdownloader.domain.models

/**
 * Download queue item tracked in memory and surfaced to UI.
 */
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val status: DownloadStatus,
    val activeWorkId: String? = null,
    val progressPercent: Int = 0,
    val speed: String? = null,
    val eta: String? = null,
    val outputPath: String? = null,
    val thumbnailUrl: String? = null,
    val subtitlePaths: List<String> = emptyList(),
    val subtitleStatus: String? = null,
    val downloadedStr: String? = null,
    val totalSizeStr: String? = null,
    val errorMessage: String? = null,
    val debugTrace: String? = null,
    val pauseExpiresAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val isInVault: Boolean = false,
)

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED,
}

fun DownloadStatus.blocksRuntimeUpdates(): Boolean {
    return this == DownloadStatus.QUEUED ||
        this == DownloadStatus.RUNNING ||
        this == DownloadStatus.PAUSED
}

/**
 * Outcome of the optional subtitle step for a completed download.
 * [OK] means a subtitle sidecar (or embedded track) is present; [SKIPPED] means the user did
 * not request subtitles; [FAILED] means subtitles were requested but could not be fetched/embedded.
 */
enum class SubtitleStatus {
    OK,
    SKIPPED,
    FAILED,
    ;
}

fun SubtitleStatus.toMessage(): String? = when (this) {
    SubtitleStatus.OK -> null
    SubtitleStatus.SKIPPED -> null
    SubtitleStatus.FAILED -> "Subtitles were unavailable for this video; media downloaded without captions."
}

fun String?.toSubtitleStatus(): SubtitleStatus? = this?.let { raw ->
    runCatching { SubtitleStatus.valueOf(raw) }.getOrNull()
}
