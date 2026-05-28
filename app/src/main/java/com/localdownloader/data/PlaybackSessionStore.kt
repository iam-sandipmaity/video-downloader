package com.localdownloader.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val sessions = ConcurrentHashMap<String, PlaybackSession>()

    fun get(sessionKey: String): PlaybackSession? {
        sessions[sessionKey]?.let { return it }

        val positionKey = positionKey(sessionKey)
        if (!preferences.contains(positionKey)) return null

        return PlaybackSession(
            positionMs = preferences.getLong(positionKey, 0L).coerceAtLeast(0L),
            playWhenReady = preferences.getBoolean(playWhenReadyKey(sessionKey), false),
        ).also { session ->
            sessions[sessionKey] = session
        }
    }

    fun save(sessionKey: String, session: PlaybackSession) {
        sessions[sessionKey] = session
        preferences.edit {
            putLong(positionKey(sessionKey), session.positionMs.coerceAtLeast(0L))
            putBoolean(playWhenReadyKey(sessionKey), session.playWhenReady)
        }
    }

    private fun positionKey(sessionKey: String): String = "$KEY_POSITION_PREFIX$sessionKey"

    private fun playWhenReadyKey(sessionKey: String): String = "$KEY_PLAY_WHEN_READY_PREFIX$sessionKey"

    private companion object {
        const val PREFERENCES_NAME = "playback_sessions"
        const val KEY_POSITION_PREFIX = "position:"
        const val KEY_PLAY_WHEN_READY_PREFIX = "play_when_ready:"
    }
}

data class PlaybackSession(
    val positionMs: Long,
    val playWhenReady: Boolean,
)
