package com.localdownloader.domain.models

/**
 * Typed format loading result for one URL.
 */
data class FormatLoadResult(
    val webpageUrl: String,
    val title: String,
    val formats: List<MediaFormat>,
    val extractorArgs: String? = null,
    val infoJsonPath: String? = null,
)
