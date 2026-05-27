package com.localdownloader.domain.models

/**
 * Lightweight metadata for one discovered downloadable item.
 */
data class LinkAnalysisItem(
    val id: String,
    val title: String,
    val webpageUrl: String,
    val uploader: String? = null,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null,
    val playlistItemIndex: Int? = null,
    val formats: List<MediaFormat> = emptyList(),
    val extractorArgs: String? = null,
    val infoJsonPath: String? = null,
)
