package com.localdownloader.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackConflictManagerTest {

    @Test
    fun onAudioPlaybackStarting_invokesRegisteredVideoPauseHandler() {
        val manager = PlaybackConflictManager()
        var pauseCount = 0
        val owner = Any()
        manager.registerVideoPauseHandler(owner) { pauseCount += 1 }

        manager.onAudioPlaybackStarting()

        assertEquals(1, pauseCount)
    }

    @Test
    fun onVideoPlaybackStarting_invokesRegisteredAudioPauseHandler() {
        val manager = PlaybackConflictManager()
        var pauseCount = 0
        val owner = Any()
        manager.registerAudioPauseHandler(owner) { pauseCount += 1 }

        manager.onVideoPlaybackStarting()

        assertEquals(1, pauseCount)
    }

    @Test
    fun unregisteringPauseHandlers_stopsFurtherCallbacks() {
        val manager = PlaybackConflictManager()
        var audioPauseCount = 0
        var videoPauseCount = 0
        val audioOwner = Any()
        val videoOwner = Any()
        manager.registerAudioPauseHandler(audioOwner) { audioPauseCount += 1 }
        manager.registerVideoPauseHandler(videoOwner) { videoPauseCount += 1 }

        manager.unregisterAudioPauseHandler(audioOwner)
        manager.unregisterVideoPauseHandler(videoOwner)
        manager.onVideoPlaybackStarting()
        manager.onAudioPlaybackStarting()

        assertEquals(0, audioPauseCount)
        assertEquals(0, videoPauseCount)
    }

    @Test
    fun unregisteringOldOwner_keepsNewerHandlerRegistered() {
        val manager = PlaybackConflictManager()
        val oldOwner = Any()
        val newOwner = Any()
        var pauseCount = 0
        manager.registerVideoPauseHandler(oldOwner) { pauseCount += 1 }
        manager.registerVideoPauseHandler(newOwner) { pauseCount += 1 }

        manager.unregisterVideoPauseHandler(oldOwner)
        manager.onAudioPlaybackStarting()

        assertEquals(1, pauseCount)
    }
}
