package com.localdownloader.ui.model

data class ExternalOpenRequest(
    val path: String,
    val displayName: String,
    val mimeType: String?,
)
