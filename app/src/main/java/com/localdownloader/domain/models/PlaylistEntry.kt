package com.localdownloader.domain.models

/**
 * Minimal metadata for a single item inside an analyzed playlist.
 */
data class PlaylistEntry(
    val playlistItemIndex: Int,
    val id: String,
    val title: String,
    val webpageUrl: String,
    val uploader: String? = null,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null,
)
