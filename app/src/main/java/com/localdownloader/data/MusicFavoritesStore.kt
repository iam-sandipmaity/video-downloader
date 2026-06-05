package com.localdownloader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.musicFavoritesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "music_favorites",
)

@Singleton
class MusicFavoritesStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val favoriteTaskIds = stringSetPreferencesKey("favorite_task_ids")
    }

    val favoriteTaskIds: Flow<Set<String>> = context.musicFavoritesDataStore.data
        .map { prefs -> prefs[Keys.favoriteTaskIds].orEmpty() }

    suspend fun toggleFavorite(taskId: String) {
        if (taskId.isBlank()) return
        context.musicFavoritesDataStore.edit { prefs ->
            val updated = prefs[Keys.favoriteTaskIds].orEmpty().toMutableSet()
            if (!updated.add(taskId)) {
                updated.remove(taskId)
            }
            prefs[Keys.favoriteTaskIds] = updated
        }
    }
}
