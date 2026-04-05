package com.localdownloader.viewmodel

import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality

data class FormatUiState(
    val urlInput: String = "",
    val isAnalyzing: Boolean = false,
    val isQueueing: Boolean = false,
    val videoInfo: VideoInfo? = null,
    // Quality and type selectors (replaces raw format-ID picker)
    val selectedQuality: VideoQuality = VideoQuality.BEST,
    val selectedStreamType: StreamType = StreamType.VIDEO_AUDIO,
    val selectedContainer: String = "mp4",
    val selectedAudioFormat: String = "mp3",
    val audioBitrateKbps: Int = 192,
    // Advanced options
    val downloadSubtitles: Boolean = false,
    val embedMetadata: Boolean = true,
    val embedThumbnail: Boolean = false,
    val writeThumbnail: Boolean = false,
    val enablePlaylist: Boolean = false,
    val outputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val appSettings: AppSettings = AppSettings(),
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isDarkTheme: Boolean = false,
)
