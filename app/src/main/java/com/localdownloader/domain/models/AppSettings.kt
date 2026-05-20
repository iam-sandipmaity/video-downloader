package com.localdownloader.domain.models

/**
 * User preferences that influence yt-dlp argument generation.
 */
data class AppSettings(
    val languageTag: String = "en",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentPreset: AccentPreset = AccentPreset.AQUA,
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
    val autoEmbedThumbnail: Boolean = false,
    val autoRemoveMissingFilesFromLibrary: Boolean = true,
    val deleteFromStorageWhenRemovedInApp: Boolean = true,
    val cookiesEnabled: Boolean = false,
    val cookieUserAgentEnabled: Boolean = false,
    val cookieProfiles: List<CookieProfile> = emptyList(),
    val youtubeAuthConfig: YoutubeAuthConfig = YoutubeAuthConfig(),
    val hasSeenDownloadSetupNotice: Boolean = false,
    val downloadNetworkMode: DownloadNetworkMode = DownloadNetworkMode.ANY,
    val maxConcurrentDownloads: Int = 1,
    val defaultAudioBitrateKbps: Int = 192,
    val defaultWriteThumbnail: Boolean = false,
    val cacheCleanupPolicy: CacheCleanupPolicy = CacheCleanupPolicy.SEVEN_DAYS,
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
    ORANGE,
    COPPER,
    MONOCHROME,
}

enum class ContrastMode {
    SOFT,
    STANDARD,
    HIGH,
    ULTRA,
}

enum class DownloadNetworkMode {
    ANY,
    WIFI_ONLY,
    UNMETERED,
}

enum class CacheCleanupPolicy(
    val days: Int?,
) {
    NEVER(days = null),
    ONE_DAY(days = 1),
    THREE_DAYS(days = 3),
    SEVEN_DAYS(days = 7),
    THIRTY_DAYS(days = 30),
}
