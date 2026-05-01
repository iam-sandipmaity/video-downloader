package com.localplayer.model

data class PlayerMedia(
    val id: String,
    val title: String,
    val uriString: String,
    val mimeType: String? = null,
)
