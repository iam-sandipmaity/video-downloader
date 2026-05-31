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
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.SYSTEM_LANGUAGE_TAG
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.domain.models.YoutubeAuthConfig
import com.localdownloader.utils.SensitiveDataSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
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
    private val secureCookieDir: File
        get() = File(context.noBackupFilesDir, "cookies")
    private val secureYoutubeAuthFile: File
        get() = File(File(context.noBackupFilesDir, "auth"), "youtube-auth-config.json")

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
        val downloadsRootPublicPath = stringPreferencesKey("downloads_root_public_path")
        val videoSubfolderName = stringPreferencesKey("video_subfolder_name")
        val audioSubfolderName = stringPreferencesKey("audio_subfolder_name")
        val otherSubfolderName = stringPreferencesKey("other_subfolder_name")
        val autoDownloadSubtitles = booleanPreferencesKey("auto_download_subtitles")
        val autoEmbedSubtitles = booleanPreferencesKey("auto_embed_subtitles")
        val autoEmbedMetadata = booleanPreferencesKey("auto_embed_metadata")
        val autoEmbedThumbnail = booleanPreferencesKey("auto_embed_thumbnail")
        val autoRemoveMissingFilesFromLibrary = booleanPreferencesKey("auto_remove_missing_files_from_library")
        val deleteFromStorageWhenRemovedInApp = booleanPreferencesKey("delete_from_storage_when_removed_in_app")
        val notifyCompletedDownloads = booleanPreferencesKey("notify_completed_downloads")
        val notifyDownloadErrors = booleanPreferencesKey("notify_download_errors")
        val notifyCanceledDownloads = booleanPreferencesKey("notify_canceled_downloads")
        val notifyPromotions = booleanPreferencesKey("notify_promotions")
        val backupLogsToDevice = booleanPreferencesKey("backup_logs_to_device")
        val autoDeleteOldAppLogs = booleanPreferencesKey("auto_delete_old_app_logs")
        val appLogRetentionDays = intPreferencesKey("app_log_retention_days")
        val keepAnalyzedLinkHistory = booleanPreferencesKey("keep_analyzed_link_history")
        val analyzedLinkHistoryRetentionDays = intPreferencesKey("analyzed_link_history_retention_days")
        val downloadHistoryRetentionDays = intPreferencesKey("download_history_retention_days")
        val cookiesEnabled = booleanPreferencesKey("cookies_enabled")
        val cookieUserAgentEnabled = booleanPreferencesKey("cookie_user_agent_enabled")
        val cookieProfiles = stringPreferencesKey("cookie_profiles_json")
        val youtubeAuthConfig = stringPreferencesKey("youtube_auth_config_json")
        val hasSeenDownloadSetupNotice = booleanPreferencesKey("has_seen_download_setup_notice")
        val maxConcurrent = intPreferencesKey("max_concurrent")
        val allowMeteredDownloads = booleanPreferencesKey("allow_metered_downloads")
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
                    languageTag = prefs[Keys.languageTag] ?: SYSTEM_LANGUAGE_TAG,
                    themeMode = prefs[Keys.themeMode]?.toEnumOrDefault(ThemeMode.LIGHT) ?: ThemeMode.LIGHT,
                    accentPreset = prefs[Keys.accentPreset]?.toEnumOrDefault(AccentPreset.TEAL) ?: AccentPreset.TEAL,
                    contrastMode = prefs[Keys.contrastMode]?.toEnumOrDefault(ContrastMode.ULTRA) ?: ContrastMode.ULTRA,
                    defaultOutputTemplate = prefs[Keys.template] ?: "%(title)s [%(id)s].%(ext)s",
                    defaultAudioOutputTemplate = prefs[Keys.audioTemplate] ?: "%(title)s [%(id)s].%(ext)s",
                    defaultMergeContainer = prefs[Keys.mergeContainer] ?: AppSettings().defaultMergeContainer,
                    defaultAudioFormat = prefs[Keys.audioFormat] ?: "mp3",
                    downloadsRootFolderName = prefs[Keys.downloadsRootFolderName] ?: "LocalDownloader",
                    downloadsRootPublicPath = prefs[Keys.downloadsRootPublicPath].orEmpty(),
                    videoSubfolderName = prefs[Keys.videoSubfolderName] ?: "Videos",
                    audioSubfolderName = prefs[Keys.audioSubfolderName] ?: "Audio",
                    otherSubfolderName = prefs[Keys.otherSubfolderName] ?: "Files",
                    autoDownloadSubtitles = prefs[Keys.autoDownloadSubtitles] ?: false,
                    autoEmbedSubtitles = prefs[Keys.autoEmbedSubtitles] ?: false,
                    autoEmbedMetadata = prefs[Keys.autoEmbedMetadata] ?: true,
                    autoEmbedThumbnail = prefs[Keys.autoEmbedThumbnail] ?: true,
                    autoRemoveMissingFilesFromLibrary = prefs[Keys.autoRemoveMissingFilesFromLibrary] ?: true,
                    deleteFromStorageWhenRemovedInApp = prefs[Keys.deleteFromStorageWhenRemovedInApp] ?: true,
                    notifyCompletedDownloads = prefs[Keys.notifyCompletedDownloads] ?: true,
                    notifyDownloadErrors = prefs[Keys.notifyDownloadErrors] ?: true,
                    notifyCanceledDownloads = prefs[Keys.notifyCanceledDownloads] ?: true,
                    notifyPromotions = prefs[Keys.notifyPromotions] ?: true,
                    backupLogsToDevice = prefs[Keys.backupLogsToDevice] ?: false,
                    autoDeleteOldAppLogs = prefs[Keys.autoDeleteOldAppLogs] ?: false,
                    appLogRetentionDays = prefs[Keys.appLogRetentionDays] ?: 15,
                    keepAnalyzedLinkHistory = prefs[Keys.keepAnalyzedLinkHistory] ?: true,
                    analyzedLinkHistoryRetentionDays = prefs[Keys.analyzedLinkHistoryRetentionDays] ?: 15,
                    downloadHistoryRetentionDays = prefs[Keys.downloadHistoryRetentionDays] ?: 30,
                    cookiesEnabled = prefs[Keys.cookiesEnabled] ?: false,
                    cookieUserAgentEnabled = prefs[Keys.cookieUserAgentEnabled] ?: false,
                    cookieProfiles = decodeCookieProfiles(prefs[Keys.cookieProfiles]),
                    youtubeAuthConfig = decodeYoutubeAuthConfig(prefs[Keys.youtubeAuthConfig]),
                    hasSeenDownloadSetupNotice = prefs[Keys.hasSeenDownloadSetupNotice] ?: false,
                    maxConcurrentDownloads = prefs[Keys.maxConcurrent] ?: 2,
                    allowMeteredDownloads = prefs[Keys.allowMeteredDownloads] ?: false,
                    darkTheme = prefs[Keys.darkTheme] ?: false,
                )
            }
    }

    suspend fun updateSettings(settings: AppSettings) {
        val persistedCookieProfiles = prepareCookieProfilesForPersistence(settings.cookieProfiles)
        persistYoutubeAuthConfig(settings.youtubeAuthConfig)
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
            prefs[Keys.downloadsRootPublicPath] = settings.downloadsRootPublicPath
            prefs[Keys.videoSubfolderName] = settings.videoSubfolderName
            prefs[Keys.audioSubfolderName] = settings.audioSubfolderName
            prefs[Keys.otherSubfolderName] = settings.otherSubfolderName
            prefs[Keys.autoDownloadSubtitles] = settings.autoDownloadSubtitles
            prefs[Keys.autoEmbedSubtitles] = settings.autoEmbedSubtitles
            prefs[Keys.autoEmbedMetadata] = settings.autoEmbedMetadata
            prefs[Keys.autoEmbedThumbnail] = settings.autoEmbedThumbnail
            prefs[Keys.autoRemoveMissingFilesFromLibrary] = settings.autoRemoveMissingFilesFromLibrary
            prefs[Keys.deleteFromStorageWhenRemovedInApp] = settings.deleteFromStorageWhenRemovedInApp
            prefs[Keys.notifyCompletedDownloads] = settings.notifyCompletedDownloads
            prefs[Keys.notifyDownloadErrors] = settings.notifyDownloadErrors
            prefs[Keys.notifyCanceledDownloads] = settings.notifyCanceledDownloads
            prefs[Keys.notifyPromotions] = settings.notifyPromotions
            prefs[Keys.backupLogsToDevice] = settings.backupLogsToDevice
            prefs[Keys.autoDeleteOldAppLogs] = settings.autoDeleteOldAppLogs
            prefs[Keys.appLogRetentionDays] = settings.appLogRetentionDays
            prefs[Keys.keepAnalyzedLinkHistory] = settings.keepAnalyzedLinkHistory
            prefs[Keys.analyzedLinkHistoryRetentionDays] = settings.analyzedLinkHistoryRetentionDays
            prefs[Keys.downloadHistoryRetentionDays] = settings.downloadHistoryRetentionDays
            prefs[Keys.cookiesEnabled] = settings.cookiesEnabled
            prefs[Keys.cookieUserAgentEnabled] = settings.cookieUserAgentEnabled
            prefs[Keys.cookieProfiles] = json.encodeToString(persistedCookieProfiles)
            prefs[Keys.youtubeAuthConfig] = json.encodeToString(redactedYoutubeAuthConfig(settings.youtubeAuthConfig))
            prefs[Keys.hasSeenDownloadSetupNotice] = settings.hasSeenDownloadSetupNotice
            prefs[Keys.maxConcurrent] = settings.maxConcurrentDownloads
            prefs[Keys.allowMeteredDownloads] = settings.allowMeteredDownloads
            prefs[Keys.darkTheme] = settings.darkTheme
        }
    }

    private fun decodeCookieProfiles(raw: String?): List<CookieProfile> {
        val payload = raw?.trim().orEmpty()
        if (payload.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<CookieProfile>>(payload) }
            .getOrDefault(emptyList())
            .map(::hydrateCookieProfile)
    }

    private fun decodeYoutubeAuthConfig(raw: String?): YoutubeAuthConfig {
        val payload = raw?.trim().orEmpty()
        val prefConfig = if (payload.isBlank()) {
            YoutubeAuthConfig()
        } else {
            runCatching { json.decodeFromString<YoutubeAuthConfig>(payload) }.getOrDefault(YoutubeAuthConfig())
        }
        return readPersistedYoutubeAuthConfig() ?: prefConfig
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T {
        return runCatching { enumValueOf<T>(this) }.getOrDefault(default)
    }

    private fun prepareCookieProfilesForPersistence(profiles: List<CookieProfile>): List<CookieProfile> {
        return profiles.map { profile ->
            val cookiesText = profile.cookiesText.trim()
            if (cookiesText.isBlank()) {
                profile.copy(cookiesText = "")
            } else {
                val targetFile = secureCookieFile(profile.id)
                targetFile.parentFile?.mkdirs()
                if (!targetFile.exists() || runCatching { targetFile.readText() }.getOrNull() != cookiesText) {
                    targetFile.writeText(cookiesText)
                }
                profile.copy(
                    cookiesText = "",
                    localFilePath = targetFile.absolutePath,
                )
            }
        }
    }

    private fun hydrateCookieProfile(profile: CookieProfile): CookieProfile {
        val resolvedText = profile.localFilePath
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.exists() && it.isFile }
            ?.let { file -> runCatching { file.readText() }.getOrNull() }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: profile.cookiesText

        return profile.copy(
            cookiesText = resolvedText,
            localFilePath = profile.localFilePath.trim(),
        )
    }

    private fun persistYoutubeAuthConfig(config: YoutubeAuthConfig) {
        if (config == YoutubeAuthConfig()) {
            runCatching { secureYoutubeAuthFile.delete() }
            return
        }

        secureYoutubeAuthFile.parentFile?.mkdirs()
        val serialized = json.encodeToString(config)
        if (!secureYoutubeAuthFile.exists() || runCatching { secureYoutubeAuthFile.readText() }.getOrNull() != serialized) {
            secureYoutubeAuthFile.writeText(serialized)
        }
    }

    private fun readPersistedYoutubeAuthConfig(): YoutubeAuthConfig? {
        return secureYoutubeAuthFile
            .takeIf { it.exists() && it.isFile }
            ?.let { file ->
                runCatching { json.decodeFromString<YoutubeAuthConfig>(file.readText()) }
                    .onFailure { error ->
                        android.util.Log.w(
                            "SettingsStore",
                            SensitiveDataSanitizer.sanitize("Failed reading secure YouTube auth config: ${error.message.orEmpty()}"),
                        )
                    }
                    .getOrNull()
            }
    }

    private fun redactedYoutubeAuthConfig(config: YoutubeAuthConfig): YoutubeAuthConfig {
        return config.copy(
            gvsToken = "",
            playerToken = "",
            subsToken = "",
            visitorData = "",
            dataSyncId = "",
        )
    }

    private fun secureCookieFile(profileId: String): File {
        return File(secureCookieDir.apply { mkdirs() }, "cookie-$profileId.txt")
    }
}
