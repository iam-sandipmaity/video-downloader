package com.localdownloader.audio

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackConflictManager @Inject constructor() {
    @Volatile
    private var audioRegistration: PauseHandlerRegistration? = null

    @Volatile
    private var videoRegistration: PauseHandlerRegistration? = null

    fun registerAudioPauseHandler(owner: Any, handler: () -> Unit) {
        audioRegistration = PauseHandlerRegistration(owner = owner, handler = handler)
    }

    fun unregisterAudioPauseHandler(owner: Any) {
        if (audioRegistration?.owner === owner) {
            audioRegistration = null
        }
    }

    fun registerVideoPauseHandler(owner: Any, handler: () -> Unit) {
        videoRegistration = PauseHandlerRegistration(owner = owner, handler = handler)
    }

    fun unregisterVideoPauseHandler(owner: Any) {
        if (videoRegistration?.owner === owner) {
            videoRegistration = null
        }
    }

    fun onAudioPlaybackStarting() {
        videoRegistration?.handler?.invoke()
    }

    fun onVideoPlaybackStarting() {
        audioRegistration?.handler?.invoke()
    }

    private data class PauseHandlerRegistration(
        val owner: Any,
        val handler: () -> Unit,
    )
}
