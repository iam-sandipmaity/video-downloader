package com.localdownloader.domain.models

/**
 * User preferences that influence yt-dlp argument generation.
 */
data class AppSettings(
    val defaultOutputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val defaultMergeContainer: String = "mp4",
    val autoEmbedMetadata: Boolean = true,
    val autoEmbedThumbnail: Boolean = false,
    val maxConcurrentDownloads: Int = 2,
)
