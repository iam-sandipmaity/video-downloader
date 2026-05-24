package com.localdownloader.domain.models

private val AUDIO_FORMATS_WITH_FIXED_QUALITY = setOf(
    "flac",
    "wav",
)

internal fun audioFormatSupportsBitrateControl(format: String?): Boolean {
    val normalized = format?.trim()?.lowercase().orEmpty()
    return normalized.isNotBlank() && normalized !in AUDIO_FORMATS_WITH_FIXED_QUALITY
}
