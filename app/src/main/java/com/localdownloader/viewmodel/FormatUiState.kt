package com.localdownloader.viewmodel

import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.domain.models.YoutubeAuthConfig

enum class FormatMessageScope {
    BROWSER,
    SETTINGS,
    COOKIES,
    YOUTUBE_ACCESS,
}

data class FormatUiState(
    val urlInput: String = "",
    val isAnalyzing: Boolean = false,
    val isQueueing: Boolean = false,
    val videoInfo: VideoInfo? = null,
    val availableVideoAudioChoices: List<FormatChoice> = emptyList(),
    val availableVideoOnlyChoices: List<FormatChoice> = emptyList(),
    val availableAudioOnlyChoices: List<FormatChoice> = emptyList(),
    val selectedFormatSelector: String? = null,
    // Quality and type selectors (replaces raw format-ID picker)
    val selectedQuality: VideoQuality = VideoQuality.BEST,
    val selectedStreamType: StreamType = StreamType.VIDEO_AUDIO,
    val selectedContainer: String = "mp4",
    val selectedAudioFormat: String = "mp3",
    val audioBitrateKbps: Int = 192,
    // Advanced options
    val downloadSubtitles: Boolean = false,
    val embedSubtitles: Boolean = false,
    val embedMetadata: Boolean = true,
    val embedThumbnail: Boolean = false,
    val writeThumbnail: Boolean = false,
    val languageTag: String = "en",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentPreset: AccentPreset = AccentPreset.TEAL,
    val contrastMode: ContrastMode = ContrastMode.ULTRA,
    val autoRemoveMissingFilesFromLibrary: Boolean = true,
    val deleteFromStorageWhenRemovedInApp: Boolean = true,
    val notifyCompletedDownloads: Boolean = true,
    val notifyDownloadErrors: Boolean = true,
    val notifyCanceledDownloads: Boolean = true,
    val notifyPromotions: Boolean = true,
    val enablePlaylist: Boolean = false,
    val outputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val audioOutputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val downloadsRootFolderName: String = "LocalDownloader",
    val videoSubfolderName: String = "Videos",
    val audioSubfolderName: String = "Audio",
    val otherSubfolderName: String = "Files",
    val maxConcurrentDownloads: Int = 2,
    val cookiesEnabled: Boolean = false,
    val cookieUserAgentEnabled: Boolean = false,
    val cookieProfiles: List<CookieProfile> = emptyList(),
    val youtubeAuthConfig: YoutubeAuthConfig = YoutubeAuthConfig(),
    val appSettings: AppSettings = AppSettings(),
    val hasLoadedSettings: Boolean = false,
    val messageScope: FormatMessageScope = FormatMessageScope.BROWSER,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isDarkTheme: Boolean = false,
    // Download button state management
    val isDownloadButtonDisabled: Boolean = false,
    val downloadButtonDisabledAt: Long = 0L,
    // Track last queued settings to detect changes
    val lastQueuedStreamType: StreamType? = null,
    val lastQueuedFormatSelector: String? = null,
    val lastQueuedContainer: String? = null,
    val lastQueuedAudioFormat: String? = null,
    val lastQueuedAudioBitrate: Int? = null,
    val lastQueuedQuality: VideoQuality? = null,
) {
    val shouldShowDownloadSetupNotice: Boolean
        get() = hasLoadedSettings &&
            !appSettings.hasSeenDownloadSetupNotice &&
            cookieProfiles.isEmpty() &&
            !youtubeAuthConfig.isConfigured()

    fun infoMessageFor(scope: FormatMessageScope): String? =
        infoMessage?.takeIf { messageScope == scope }

    fun errorMessageFor(scope: FormatMessageScope): String? =
        errorMessage?.takeIf { messageScope == scope }
}
