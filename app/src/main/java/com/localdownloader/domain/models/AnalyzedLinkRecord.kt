package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedLinkRecord(
    val webpageUrl: String,
    val title: String,
    val uploader: String? = null,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null,
    val isPlaylist: Boolean = false,
    val playlistCount: Int? = null,
    val formatCount: Int = 0,
    val analyzedAtEpochMs: Long,
)
