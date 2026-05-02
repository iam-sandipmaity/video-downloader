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
    val subtitlePaths: List<String> = emptyList(),
    val downloadedStr: String? = null,
    val totalSizeStr: String? = null,
    val errorMessage: String? = null,
    val debugTrace: String? = null,
    val pauseExpiresAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

enum class DownloadStatus {
    QUEUED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED,
}
