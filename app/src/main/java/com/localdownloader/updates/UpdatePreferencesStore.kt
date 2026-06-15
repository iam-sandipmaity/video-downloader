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

    private companion object {
        private const val PREFS_NAME = "update_preferences"
        private const val KEY_INCLUDE_PRERELEASE_APP_RELEASES = "include_prerelease_app_releases"
        private const val KEY_AUTO_UPDATE_YTDLP = "auto_update_ytdlp"
        private const val KEY_YTDLP_CHANNEL = "ytdlp_channel"
    }
}
