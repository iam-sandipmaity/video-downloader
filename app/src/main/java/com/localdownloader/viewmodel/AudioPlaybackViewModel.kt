package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import com.localdownloader.audio.AudioPlaybackManager
import com.localdownloader.audio.AudioPlaybackState
import com.localdownloader.audio.AudioQueueItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AudioPlaybackViewModel @Inject constructor(
    private val audioPlaybackManager: AudioPlaybackManager,
) : ViewModel() {
    val uiState: StateFlow<AudioPlaybackState> = audioPlaybackManager.state

    fun playQueue(
        items: List<AudioQueueItem>,
        startTaskId: String? = null,
        shuffleRequested: Boolean = false,
    ) {
        audioPlaybackManager.playQueue(
            items = items,
            startTaskId = startTaskId,
            shuffleRequested = shuffleRequested,
        )
    }

    fun togglePlayback() {
        audioPlaybackManager.togglePlayback()
    }

    fun seekBy(offsetMs: Long) {
        audioPlaybackManager.seekBy(offsetMs)
    }

    fun seekTo(positionMs: Long) {
        audioPlaybackManager.seekTo(positionMs)
    }

    fun skipPrevious() {
        audioPlaybackManager.skipPrevious()
    }

    fun skipNext() {
        audioPlaybackManager.skipNext()
    }

    fun toggleShuffle() {
        audioPlaybackManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        audioPlaybackManager.cycleRepeatMode()
    }

    fun setSleepTimer(durationMs: Long?) {
        audioPlaybackManager.setSleepTimer(durationMs)
    }

    fun stopPlayback() {
        audioPlaybackManager.stopPlayback()
    }

    fun dismissError() {
        audioPlaybackManager.dismissError()
    }
}
