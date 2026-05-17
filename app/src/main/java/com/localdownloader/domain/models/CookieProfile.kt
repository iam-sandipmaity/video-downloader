package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class CookieProfile(
    val id: String,
    val url: String,
    val cookiesText: String,
    val localFilePath: String = "",
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
