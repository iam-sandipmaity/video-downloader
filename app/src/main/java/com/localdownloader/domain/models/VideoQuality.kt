package com.localdownloader.domain.models

enum class VideoQuality(val label: String, val maxHeight: Int?) {
    BEST("Best available", null),
    UHD_4K("4K · 2160p", 2160),
    QHD_2K("2K · 1440p", 1440),
    FHD_1080("1080p", 1080),
    HD_720("720p", 720),
    SD_480("480p", 480),
    SD_360("360p", 360),
    LOW_240("240p", 240),
    LOW_144("144p", 144),
}

enum class StreamType(val label: String) {
    VIDEO_AUDIO("Video + Audio"),
    VIDEO_ONLY("Video only"),
    AUDIO_ONLY("Audio only"),
}
