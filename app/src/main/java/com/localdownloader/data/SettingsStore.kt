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
import com.localdownloader.domain.models.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val template = stringPreferencesKey("output_template")
        val mergeContainer = stringPreferencesKey("merge_container")
        val autoEmbedMetadata = booleanPreferencesKey("auto_embed_metadata")
        val autoEmbedThumbnail = booleanPreferencesKey("auto_embed_thumbnail")
        val maxConcurrent = intPreferencesKey("max_concurrent")
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
                    defaultOutputTemplate = prefs[Keys.template] ?: "%(title)s [%(id)s].%(ext)s",
                    defaultMergeContainer = prefs[Keys.mergeContainer] ?: "mp4",
                    autoEmbedMetadata = prefs[Keys.autoEmbedMetadata] ?: true,
                    autoEmbedThumbnail = prefs[Keys.autoEmbedThumbnail] ?: false,
                    maxConcurrentDownloads = prefs[Keys.maxConcurrent] ?: 2,
                    darkTheme = prefs[Keys.darkTheme] ?: false,
                )
            }
    }

    suspend fun updateSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.template] = settings.defaultOutputTemplate
            prefs[Keys.mergeContainer] = settings.defaultMergeContainer
            prefs[Keys.autoEmbedMetadata] = settings.autoEmbedMetadata
            prefs[Keys.autoEmbedThumbnail] = settings.autoEmbedThumbnail
            prefs[Keys.maxConcurrent] = settings.maxConcurrentDownloads
            prefs[Keys.darkTheme] = settings.darkTheme
        }
    }
}
