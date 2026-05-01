package com.localdownloader.data

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackSessionStore @Inject constructor() {
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
