package com.localplayer.data

import java.util.concurrent.ConcurrentHashMap

class PlaybackSessionStore {
    private val sessions = ConcurrentHashMap<String, PlaybackSession>()

    fun get(sessionKey: String): PlaybackSession? = sessions[sessionKey]

    fun save(sessionKey: String, session: PlaybackSession) {
        sessions[sessionKey] = session
    }
}

data class PlaybackSession(
    val positionMs: Long,
    val playWhenReady: Boolean,
)
