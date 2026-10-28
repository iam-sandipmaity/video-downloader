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
        name = "Best Video",
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
        name = "1080p MP4",
        description = "Full HD in compatible MP4 container",
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
        id = "video_720p",
        name = "720p HD",
        description = "Standard 720p high definition",
        isDefault = false,
        isBuiltIn = true,
        targetType = PresetTargetType.VIDEO,
        preferredQuality = "720p",
        videoContainer = "auto",
        audioFormat = "mp3",
        embedMetadata = true,
        embedThumbnail = true,
    ),
    DownloadPreset(
        id = "video_480p_saver",
        name = "480p Saver",
        description = "Data-saving standard definition",
        isDefault = false,
        isBuiltIn = true,
        targetType = PresetTargetType.VIDEO,
        preferredQuality = "480p",
        videoContainer = "auto",
        audioFormat = "mp3",
        embedMetadata = true,
        embedThumbnail = false,
    ),
    DownloadPreset(
        id = "audio_mp3_hq",
        name = "Audio MP3",
        description = "High quality MP3 audio with tags",
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
)
