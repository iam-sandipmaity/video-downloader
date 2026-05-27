package com.localdownloader.domain.models

/**
 * Request model for loading formats for one or more discovered items.
 */
data class FormatLoadRequest(
    val webpageUrls: List<String>,
)
