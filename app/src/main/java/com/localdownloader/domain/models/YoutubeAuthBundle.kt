package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeAuthBundle(
    val targetUrl: String? = null,
    val generatedAt: String? = null,
    val cookiesFile: String? = null,
    val poTokenFile: String? = null,
    val poTokenClientHint: String = "web.gvs",
    val poToken: String? = null,
    val cookiesContent: String? = null,
)
