package com.localdownloader.domain.models

/**
 * User preferences that influence yt-dlp argument generation.
 */
data class AppSettings(
    val defaultOutputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val defaultMergeContainer: String = "mp4",
    val autoEmbedMetadata: Boolean = true,
    val autoEmbedThumbnail: Boolean = false,
    val autoRemoveMissingFilesFromLibrary: Boolean = true,
    val deleteFromStorageWhenRemovedInApp: Boolean = true,
    val youtubeAuthEnabled: Boolean = false,
    val youtubeCookiesPath: String = "",
    val youtubePoToken: String = "",
    val youtubePoTokenClientHint: String = "web.gvs",
    val maxConcurrentDownloads: Int = 2,
    val darkTheme: Boolean = false,
)
