package com.localdownloader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.CacheCleanupPolicy
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.DownloadNetworkMode
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.domain.models.YoutubeAuthConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private object Keys {
        val languageTag = stringPreferencesKey("language_tag")
        val themeMode = stringPreferencesKey("theme_mode")
        val accentPreset = stringPreferencesKey("accent_preset")
        val contrastMode = stringPreferencesKey("contrast_mode")
        val template = stringPreferencesKey("output_template")
        val audioTemplate = stringPreferencesKey("audio_output_template")
        val mergeContainer = stringPreferencesKey("merge_container")
        val audioFormat = stringPreferencesKey("audio_format")
        val downloadsRootFolderName = stringPreferencesKey("downloads_root_folder_name")
        val videoSubfolderName = stringPreferencesKey("video_subfolder_name")
        val audioSubfolderName = stringPreferencesKey("audio_subfolder_name")
        val otherSubfolderName = stringPreferencesKey("other_subfolder_name")
        val autoDownloadSubtitles = booleanPreferencesKey("auto_download_subtitles")
        val autoEmbedSubtitles = booleanPreferencesKey("auto_embed_subtitles")
        val autoEmbedMetadata = booleanPreferencesKey("auto_embed_metadata")
        val autoEmbedThumbnail = booleanPreferencesKey("auto_embed_thumbnail")
        val autoRemoveMissingFilesFromLibrary = booleanPreferencesKey("auto_remove_missing_files_from_library")
        val deleteFromStorageWhenRemovedInApp = booleanPreferencesKey("delete_from_storage_when_removed_in_app")
        val cookiesEnabled = booleanPreferencesKey("cookies_enabled")
        val cookieUserAgentEnabled = booleanPreferencesKey("cookie_user_agent_enabled")
        val cookieProfiles = stringPreferencesKey("cookie_profiles_json")
        val youtubeAuthConfig = stringPreferencesKey("youtube_auth_config_json")
        val hasSeenDownloadSetupNotice = booleanPreferencesKey("has_seen_download_setup_notice")
        val downloadNetworkMode = stringPreferencesKey("download_network_mode")
        val maxConcurrent = intPreferencesKey("max_concurrent")
        val defaultAudioBitrateKbps = intPreferencesKey("default_audio_bitrate_kbps")
        val defaultWriteThumbnail = booleanPreferencesKey("default_write_thumbnail")
        val defaultPlaylistEnabled = booleanPreferencesKey("default_playlist_enabled")
        val cacheCleanupPolicy = stringPreferencesKey("cache_cleanup_policy")
        val darkTheme = booleanPreferencesKey("dark_theme")
    }

    fun observeSettings(): Flow<AppSettings> {
        return context.settingsDataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs ->
                AppSettings(
                    languageTag = prefs[Keys.languageTag] ?: "en",
                    themeMode = prefs[Keys.themeMode]?.toEnumOrDefault(ThemeMode.SYSTEM) ?: ThemeMode.SYSTEM,
                    accentPreset = prefs[Keys.accentPreset]?.toEnumOrDefault(AccentPreset.AMBER) ?: AccentPreset.AMBER,
                    contrastMode = prefs[Keys.contrastMode]?.toEnumOrDefault(ContrastMode.STANDARD) ?: ContrastMode.STANDARD,
                    defaultOutputTemplate = prefs[Keys.template] ?: "%(title)s [%(id)s].%(ext)s",
                    defaultAudioOutputTemplate = prefs[Keys.audioTemplate] ?: "%(title)s [%(id)s].%(ext)s",
                    defaultMergeContainer = prefs[Keys.mergeContainer] ?: "mp4",
                    defaultAudioFormat = prefs[Keys.audioFormat] ?: "mp3",
                    downloadsRootFolderName = prefs[Keys.downloadsRootFolderName] ?: "LocalDownloader",
                    videoSubfolderName = prefs[Keys.videoSubfolderName] ?: "Videos",
                    audioSubfolderName = prefs[Keys.audioSubfolderName] ?: "Audio",
                    otherSubfolderName = prefs[Keys.otherSubfolderName] ?: "Files",
                    autoDownloadSubtitles = prefs[Keys.autoDownloadSubtitles] ?: false,
                    autoEmbedSubtitles = prefs[Keys.autoEmbedSubtitles] ?: false,
                    autoEmbedMetadata = prefs[Keys.autoEmbedMetadata] ?: true,
                    autoEmbedThumbnail = prefs[Keys.autoEmbedThumbnail] ?: false,
                    autoRemoveMissingFilesFromLibrary = prefs[Keys.autoRemoveMissingFilesFromLibrary] ?: true,
                    deleteFromStorageWhenRemovedInApp = prefs[Keys.deleteFromStorageWhenRemovedInApp] ?: true,
                    cookiesEnabled = prefs[Keys.cookiesEnabled] ?: false,
                    cookieUserAgentEnabled = prefs[Keys.cookieUserAgentEnabled] ?: false,
                    cookieProfiles = decodeCookieProfiles(prefs[Keys.cookieProfiles]),
                    youtubeAuthConfig = decodeYoutubeAuthConfig(prefs[Keys.youtubeAuthConfig]),
                    hasSeenDownloadSetupNotice = prefs[Keys.hasSeenDownloadSetupNotice] ?: false,
                    downloadNetworkMode = prefs[Keys.downloadNetworkMode]
                        ?.toEnumOrDefault(DownloadNetworkMode.ANY)
                        ?: DownloadNetworkMode.ANY,
                    maxConcurrentDownloads = (prefs[Keys.maxConcurrent] ?: 2).coerceIn(1, 3),
                    defaultAudioBitrateKbps = (prefs[Keys.defaultAudioBitrateKbps] ?: 192).coerceIn(64, 320),
                    defaultWriteThumbnail = prefs[Keys.defaultWriteThumbnail] ?: false,
                    defaultPlaylistEnabled = prefs[Keys.defaultPlaylistEnabled] ?: false,
                    cacheCleanupPolicy = prefs[Keys.cacheCleanupPolicy]
                        ?.toEnumOrDefault(CacheCleanupPolicy.SEVEN_DAYS)
                        ?: CacheCleanupPolicy.SEVEN_DAYS,
                    darkTheme = prefs[Keys.darkTheme] ?: false,
                )
            }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.languageTag] = settings.languageTag
            prefs[Keys.themeMode] = settings.themeMode.name
            prefs[Keys.accentPreset] = settings.accentPreset.name
            prefs[Keys.contrastMode] = settings.contrastMode.name
            prefs[Keys.template] = settings.defaultOutputTemplate
            prefs[Keys.audioTemplate] = settings.defaultAudioOutputTemplate
            prefs[Keys.mergeContainer] = settings.defaultMergeContainer
            prefs[Keys.audioFormat] = settings.defaultAudioFormat
            prefs[Keys.downloadsRootFolderName] = settings.downloadsRootFolderName
            prefs[Keys.videoSubfolderName] = settings.videoSubfolderName
            prefs[Keys.audioSubfolderName] = settings.audioSubfolderName
            prefs[Keys.otherSubfolderName] = settings.otherSubfolderName
            prefs[Keys.autoDownloadSubtitles] = settings.autoDownloadSubtitles
            prefs[Keys.autoEmbedSubtitles] = settings.autoEmbedSubtitles
            prefs[Keys.autoEmbedMetadata] = settings.autoEmbedMetadata
            prefs[Keys.autoEmbedThumbnail] = settings.autoEmbedThumbnail
            prefs[Keys.autoRemoveMissingFilesFromLibrary] = settings.autoRemoveMissingFilesFromLibrary
            prefs[Keys.deleteFromStorageWhenRemovedInApp] = settings.deleteFromStorageWhenRemovedInApp
            prefs[Keys.cookiesEnabled] = settings.cookiesEnabled
            prefs[Keys.cookieUserAgentEnabled] = settings.cookieUserAgentEnabled
            prefs[Keys.cookieProfiles] = json.encodeToString(settings.cookieProfiles)
            prefs[Keys.youtubeAuthConfig] = json.encodeToString(settings.youtubeAuthConfig)
            prefs[Keys.hasSeenDownloadSetupNotice] = settings.hasSeenDownloadSetupNotice
            prefs[Keys.downloadNetworkMode] = settings.downloadNetworkMode.name
            prefs[Keys.maxConcurrent] = settings.maxConcurrentDownloads.coerceIn(1, 3)
            prefs[Keys.defaultAudioBitrateKbps] = settings.defaultAudioBitrateKbps.coerceIn(64, 320)
            prefs[Keys.defaultWriteThumbnail] = settings.defaultWriteThumbnail
            prefs[Keys.defaultPlaylistEnabled] = settings.defaultPlaylistEnabled
            prefs[Keys.cacheCleanupPolicy] = settings.cacheCleanupPolicy.name
            prefs[Keys.darkTheme] = settings.darkTheme
        }
    }

    private fun decodeCookieProfiles(raw: String?): List<CookieProfile> {
        val payload = raw?.trim().orEmpty()
        if (payload.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<CookieProfile>>(payload) }.getOrDefault(emptyList())
    }

    private fun decodeYoutubeAuthConfig(raw: String?): YoutubeAuthConfig {
        val payload = raw?.trim().orEmpty()
        if (payload.isBlank()) return YoutubeAuthConfig()
        return runCatching { json.decodeFromString<YoutubeAuthConfig>(payload) }.getOrDefault(YoutubeAuthConfig())
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
        return runCatching { enumValueOf<T>(this) }.getOrDefault(default)
    }
}
