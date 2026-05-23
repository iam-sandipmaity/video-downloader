package com.localdownloader.domain.models

/**
 * User preferences that influence yt-dlp argument generation.
 */
const val SYSTEM_LANGUAGE_TAG = "system"

data class AppSettings(
    val languageTag: String = SYSTEM_LANGUAGE_TAG,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentPreset: AccentPreset = AccentPreset.TEAL,
    val contrastMode: ContrastMode = ContrastMode.ULTRA,
    val defaultOutputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val defaultAudioOutputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val defaultMergeContainer: String = "mp4",
    val defaultAudioFormat: String = "mp3",
    val downloadsRootFolderName: String = "LocalDownloader",
    val videoSubfolderName: String = "Videos",
    val audioSubfolderName: String = "Audio",
    val otherSubfolderName: String = "Files",
    val autoDownloadSubtitles: Boolean = false,
    val autoEmbedSubtitles: Boolean = false,
    val autoEmbedMetadata: Boolean = true,
    val autoEmbedThumbnail: Boolean = true,
    val autoRemoveMissingFilesFromLibrary: Boolean = true,
    val deleteFromStorageWhenRemovedInApp: Boolean = true,
    val notifyCompletedDownloads: Boolean = true,
    val notifyDownloadErrors: Boolean = true,
    val notifyCanceledDownloads: Boolean = true,
    val notifyPromotions: Boolean = true,
    val backupLogsToDevice: Boolean = false,
    val autoDeleteOldAppLogs: Boolean = false,
    val appLogRetentionDays: Int = 15,
    val keepAnalyzedLinkHistory: Boolean = true,
    val analyzedLinkHistoryRetentionDays: Int = 15,
    val downloadHistoryRetentionDays: Int = 30,
    val cookiesEnabled: Boolean = false,
    val cookieUserAgentEnabled: Boolean = false,
    val cookieProfiles: List<CookieProfile> = emptyList(),
    val youtubeAuthConfig: YoutubeAuthConfig = YoutubeAuthConfig(),
    val hasSeenDownloadSetupNotice: Boolean = false,
    val maxConcurrentDownloads: Int = 2,
    val allowMeteredDownloads: Boolean = false,
    val darkTheme: Boolean = false,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AccentPreset {
    AMBER,
    OCEAN,
    COBALT,
    INDIGO,
    SKY,
    AQUA,
    TEAL,
    MINT,
    EMERALD,
    FOREST,
    ROSE,
    CRIMSON,
    MAGENTA,
    PURPLE,
    YELLOW,
    LIME,
    ORANGE,
    PEACH,
    COPPER,
    MONOCHROME,
}

enum class ContrastMode {
    SOFT,
    STANDARD,
    HIGH,
    ULTRA,
}
