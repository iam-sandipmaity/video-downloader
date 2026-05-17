package com.localdownloader.domain.models

/**
 * User preferences that influence yt-dlp argument generation.
 */
data class AppSettings(
    val languageTag: String = "en",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentPreset: AccentPreset = AccentPreset.AMBER,
    val contrastMode: ContrastMode = ContrastMode.STANDARD,
    val defaultOutputTemplate: String = "%(title)s [%(id)s].%(ext)s",
    val defaultMergeContainer: String = "mp4",
    val downloadsRootFolderName: String = "LocalDownloader",
    val videoSubfolderName: String = "Videos",
    val audioSubfolderName: String = "Audio",
    val otherSubfolderName: String = "Files",
    val autoEmbedMetadata: Boolean = true,
    val autoEmbedThumbnail: Boolean = false,
    val autoRemoveMissingFilesFromLibrary: Boolean = true,
    val deleteFromStorageWhenRemovedInApp: Boolean = true,
    val cookiesEnabled: Boolean = false,
    val cookieUserAgentEnabled: Boolean = false,
    val cookieProfiles: List<CookieProfile> = emptyList(),
    val youtubeAuthConfig: YoutubeAuthConfig = YoutubeAuthConfig(),
    val maxConcurrentDownloads: Int = 2,
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
    FOREST,
    ROSE,
    PURPLE,
    YELLOW,
    ORANGE,
    MONOCHROME,
}

enum class ContrastMode {
    STANDARD,
    HIGH,
}
