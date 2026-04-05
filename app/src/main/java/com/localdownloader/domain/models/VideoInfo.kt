package com.localdownloader.domain.models

/**
 * Summary metadata returned by yt-dlp analysis.
 */
data class VideoInfo(
    val id: String,
    val title: String,
    val uploader: String?,
    val durationSeconds: Long?,
    val thumbnailUrl: String?,
    val webpageUrl: String,
    val formats: List<MediaFormat>,
    val extractorArgs: String?,
    val isPlaylist: Boolean,
    val playlistCount: Int?,
)
