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
            checkUpdatesOnStartup = prefs.getBoolean(KEY_CHECK_UPDATES_ON_STARTUP, true),
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

    fun setCheckUpdatesOnStartup(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_CHECK_UPDATES_ON_STARTUP, enabled) }
    }

    fun getLastStartupCheckEpochMs(): Long {
        return prefs.getLong(KEY_LAST_STARTUP_CHECK_EPOCH_MS, 0L)
    }

    fun setLastStartupCheckEpochMs(timestamp: Long) {
        prefs.edit { putLong(KEY_LAST_STARTUP_CHECK_EPOCH_MS, timestamp) }
    }

    fun getDismissedAppVersion(): String? {
        return prefs.getString(KEY_DISMISSED_APP_VERSION, null)
    }

    fun setDismissedAppVersion(version: String?) {
        prefs.edit {
            if (version == null) remove(KEY_DISMISSED_APP_VERSION)
            else putString(KEY_DISMISSED_APP_VERSION, version)
        }
    }

    fun getDismissedYtDlpVersion(): String? {
        return prefs.getString(KEY_DISMISSED_YTDLP_VERSION, null)
    }

    fun setDismissedYtDlpVersion(version: String?) {
        prefs.edit {
            if (version == null) remove(KEY_DISMISSED_YTDLP_VERSION)
            else putString(KEY_DISMISSED_YTDLP_VERSION, version)
        }
    }

    fun getDismissedFfmpegVersion(): String? {
        return prefs.getString(KEY_DISMISSED_FFMPEG_VERSION, null)
    }

    fun setDismissedFfmpegVersion(version: String?) {
        prefs.edit {
            if (version == null) remove(KEY_DISMISSED_FFMPEG_VERSION)
            else putString(KEY_DISMISSED_FFMPEG_VERSION, version)
        }
    }

    fun setYtDlpChannel(channel: YtDlpReleaseChannel) {
        prefs.edit { putString(KEY_YTDLP_CHANNEL, channel.id) }
    }

    fun setFfmpegChannel(channel: FfmpegReleaseChannel) {
        prefs.edit { putString(KEY_FFMPEG_CHANNEL, channel.id) }
    }

    private companion object {
        private const val PREFS_NAME = "update_preferences"
        private const val KEY_INCLUDE_PRERELEASE_APP_RELEASES = "include_prerelease_app_releases"
        private const val KEY_AUTO_UPDATE_YTDLP = "auto_update_ytdlp"
        private const val KEY_CHECK_UPDATES_ON_STARTUP = "check_updates_on_startup"
        private const val KEY_LAST_STARTUP_CHECK_EPOCH_MS = "last_startup_check_epoch_ms"
        private const val KEY_DISMISSED_APP_VERSION = "dismissed_app_version"
        private const val KEY_DISMISSED_YTDLP_VERSION = "dismissed_ytdlp_version"
        private const val KEY_DISMISSED_FFMPEG_VERSION = "dismissed_ffmpeg_version"
        private const val KEY_YTDLP_CHANNEL = "ytdlp_channel"
        private const val KEY_FFMPEG_CHANNEL = "ffmpeg_channel"
    }
}
