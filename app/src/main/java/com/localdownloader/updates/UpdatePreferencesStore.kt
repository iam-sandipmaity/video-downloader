package com.localdownloader.updates

import android.content.Context
import androidx.core.content.edit
import com.localdownloader.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdatePreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy(LazyThreadSafetyMode.NONE) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun currentPreferences(): UpdatePreferences {
        return UpdatePreferences(
            includePrereleaseAppReleases = prefs.getBoolean(KEY_INCLUDE_PRERELEASE_APP_RELEASES, false),
            autoUpdateYtDlp = if (prefs.contains(KEY_AUTO_UPDATE_YTDLP)) {
                prefs.getBoolean(KEY_AUTO_UPDATE_YTDLP, BuildConfig.YTDLP_AUTO_UPDATE_DEFAULT)
            } else {
                BuildConfig.YTDLP_AUTO_UPDATE_DEFAULT
            },
            ytDlpChannel = YtDlpReleaseChannel.fromId(
                prefs.getString(KEY_YTDLP_CHANNEL, YtDlpReleaseChannel.STABLE.id),
            ),
            ffmpegChannel = FfmpegReleaseChannel.fromId(
                prefs.getString(KEY_FFMPEG_CHANNEL, FfmpegReleaseChannel.STABLE.id),
            ),
        )
    }

    fun setIncludePrereleaseAppReleases(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_INCLUDE_PRERELEASE_APP_RELEASES, enabled) }
    }

    fun setAutoUpdateYtDlp(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_UPDATE_YTDLP, enabled) }
    }

    fun setYtDlpChannel(channel: YtDlpReleaseChannel) {
        prefs.edit { putString(KEY_YTDLP_CHANNEL, channel.id) }
    }

    fun setFfmpegChannel(channel: FfmpegReleaseChannel) {
        prefs.edit { putString(KEY_FFMPEG_CHANNEL, channel.id) }
    }

    fun ytDlpStartupPromptCache(): YtDlpStartupPromptCache {
        return YtDlpStartupPromptCache(
            checkedAtEpochMs = prefs.getLong(KEY_YTDLP_STARTUP_PROMPT_CHECKED_AT_EPOCH_MS, 0L),
            latestVersion = prefs.getString(KEY_YTDLP_STARTUP_PROMPT_LATEST_VERSION, null),
            dismissedLatestVersion = prefs.getString(KEY_YTDLP_STARTUP_PROMPT_DISMISSED_VERSION, null),
        )
    }

    fun cacheYtDlpStartupPromptCheck(
        latestVersion: String?,
        nowEpochMs: Long = System.currentTimeMillis(),
    ) {
        prefs.edit {
            putLong(KEY_YTDLP_STARTUP_PROMPT_CHECKED_AT_EPOCH_MS, nowEpochMs)
            if (latestVersion.isNullOrBlank()) {
                remove(KEY_YTDLP_STARTUP_PROMPT_LATEST_VERSION)
            } else {
                putString(KEY_YTDLP_STARTUP_PROMPT_LATEST_VERSION, latestVersion)
            }
        }
    }

    fun markYtDlpStartupPromptDismissed(latestVersion: String?) {
        prefs.edit {
            if (latestVersion.isNullOrBlank()) {
                remove(KEY_YTDLP_STARTUP_PROMPT_DISMISSED_VERSION)
            } else {
                putString(KEY_YTDLP_STARTUP_PROMPT_DISMISSED_VERSION, latestVersion)
            }
        }
    }

    private companion object {
        private const val PREFS_NAME = "update_preferences"
        private const val KEY_INCLUDE_PRERELEASE_APP_RELEASES = "include_prerelease_app_releases"
        private const val KEY_AUTO_UPDATE_YTDLP = "auto_update_ytdlp"
        private const val KEY_YTDLP_CHANNEL = "ytdlp_channel"
        private const val KEY_FFMPEG_CHANNEL = "ffmpeg_channel"
        private const val KEY_YTDLP_STARTUP_PROMPT_CHECKED_AT_EPOCH_MS =
            "ytdlp_startup_prompt_checked_at_epoch_ms"
        private const val KEY_YTDLP_STARTUP_PROMPT_LATEST_VERSION =
            "ytdlp_startup_prompt_latest_version"
        private const val KEY_YTDLP_STARTUP_PROMPT_DISMISSED_VERSION =
            "ytdlp_startup_prompt_dismissed_version"
    }
}

data class YtDlpStartupPromptCache(
    val checkedAtEpochMs: Long = 0L,
    val latestVersion: String? = null,
    val dismissedLatestVersion: String? = null,
)
