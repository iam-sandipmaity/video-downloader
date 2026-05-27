package com.localdownloader.domain.models

/**
 * Root discovery result used by the new link analyzer flow.
 */
data class LinkAnalysisResult(
    val sourceKind: LinkSourceKind,
    val input: String,
    val rootId: String,
    val title: String,
    val uploader: String? = null,
    val durationSeconds: Long? = null,
    val thumbnailUrl: String? = null,
    val webpageUrl: String? = null,
    val rootFormats: List<MediaFormat> = emptyList(),
    val extractorArgs: String? = null,
    val infoJsonPath: String? = null,
    val playlistCount: Int? = null,
    val items: List<LinkAnalysisItem> = emptyList(),
) {
    val isCollection: Boolean
        get() = sourceKind == LinkSourceKind.PLAYLIST || sourceKind == LinkSourceKind.CHANNEL

    val primaryItem: LinkAnalysisItem?
        get() = items.firstOrNull()
}
