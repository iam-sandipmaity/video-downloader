package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class PresetTargetType {
    VIDEO,
    AUDIO_ONLY,
}

@Serializable
data class DownloadPreset(
    val id: String,
    val name: String,
    val description: String = "",
    val isDefault: Boolean = false,
    val isBuiltIn: Boolean = false,
    val targetType: PresetTargetType = PresetTargetType.VIDEO,
    val preferredQuality: String = "best", // "best", "2160p", "1080p", "720p", "480p", "360p", "audio_only"
    val videoContainer: String = "auto", // "auto", "mp4", "mkv", "webm"
    val audioFormat: String = "mp3", // "mp3", "m4a", "opus", "flac", "wav"
    val audioBitrateKbps: Int = 320,
    val embedSubtitles: Boolean = false,
    val autoDownloadSubtitles: Boolean = false,
    val embedMetadata: Boolean = true,
    val embedThumbnail: Boolean = true,
    val concurrentFragments: Int = 4,
)

fun defaultBuiltInPresets(): List<DownloadPreset> = listOf(
    DownloadPreset(
        id = "best_video",
        name = "Best Quality Video",
        description = "Highest resolution available with auto container",
        isDefault = true,
        isBuiltIn = true,
        targetType = PresetTargetType.VIDEO,
        preferredQuality = "best",
        videoContainer = "auto",
        audioFormat = "mp3",
        embedMetadata = true,
        embedThumbnail = true,
    ),
    DownloadPreset(
        id = "video_1080p_mp4",
        name = "High Definition (1080p MP4)",
        description = "Full HD video in compatible MP4 container",
        isDefault = false,
        isBuiltIn = true,
        targetType = PresetTargetType.VIDEO,
        preferredQuality = "1080p",
        videoContainer = "mp4",
        audioFormat = "mp3",
        embedMetadata = true,
        embedThumbnail = true,
    ),
    DownloadPreset(
        id = "video_480p_saver",
        name = "Data Saver (480p)",
        description = "Standard definition for smaller storage and faster downloads",
        isDefault = false,
        isBuiltIn = true,
        targetType = PresetTargetType.VIDEO,
        preferredQuality = "480p",
        videoContainer = "mp4",
        audioFormat = "mp3",
        embedMetadata = true,
        embedThumbnail = false,
    ),
    DownloadPreset(
        id = "audio_mp3_hq",
        name = "High Quality Audio (MP3 320k)",
        description = "Crystal clear MP3 audio with embedded album artwork",
        isDefault = false,
        isBuiltIn = true,
        targetType = PresetTargetType.AUDIO_ONLY,
        preferredQuality = "audio_only",
        videoContainer = "auto",
        audioFormat = "mp3",
        audioBitrateKbps = 320,
        embedMetadata = true,
        embedThumbnail = true,
    ),
    DownloadPreset(
        id = "audio_flac_lossless",
        name = "Lossless Audio (FLAC)",
        description = "Studio quality lossless FLAC with embedded tags",
        isDefault = false,
        isBuiltIn = true,
        targetType = PresetTargetType.AUDIO_ONLY,
        preferredQuality = "audio_only",
        videoContainer = "auto",
        audioFormat = "flac",
        audioBitrateKbps = 320,
        embedMetadata = true,
        embedThumbnail = true,
    ),
)
