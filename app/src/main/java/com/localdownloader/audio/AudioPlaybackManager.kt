package com.localdownloader.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlaybackManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val logger: Logger,
    private val playbackConflictManager: PlaybackConflictManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
        }

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var currentQueue: List<AudioQueueItem> = emptyList()
    private var originalQueue: List<AudioQueueItem> = emptyList()
    private var shuffleEnabled: Boolean = false
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastErrorMessage: String? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            logger.e("AudioPlaybackManager", "Playback error", error)
            lastErrorMessage = error.message ?: "Unable to play this audio."
            updateState()
        }
    }

    init {
        playbackConflictManager.registerAudioPauseHandler(this, ::pausePlaybackForConflict)
        player.addListener(playerListener)
        startProgressUpdates()
        updateState()
    }

    fun playQueue(
        items: List<AudioQueueItem>,
        startTaskId: String? = null,
        shuffleRequested: Boolean = false,
    ) {
        scope.launch {
            val sanitizedItems = items
                .filter { it.filePath.isPlayableAudioLocation() }
                .distinctBy { it.taskId }

            if (sanitizedItems.isEmpty()) {
                clearQueueAndStopInternal()
                return@launch
            }

            val orderedQueue = if (shuffleRequested && sanitizedItems.size > 1) {
                shuffledQueueKeepingStart(
                    items = sanitizedItems,
                    startTaskId = startTaskId,
                )
            } else {
                sanitizedItems
            }
            val startIndex = startTaskId
                ?.let { taskId -> orderedQueue.indexOfFirst { it.taskId == taskId } }
                ?.takeIf { it >= 0 }
                ?: 0

            originalQueue = sanitizedItems
            currentQueue = orderedQueue
            shuffleEnabled = shuffleRequested
            lastErrorMessage = null
            playbackConflictManager.onAudioPlaybackStarting()
            player.repeatMode = state.value.repeatMode.asPlayerRepeatMode()
            player.shuffleModeEnabled = false
            player.setMediaItems(
                orderedQueue.map { queueItem -> queueItem.toMediaItem() },
                startIndex,
                0L,
            )
            player.prepare()
            player.playWhenReady = true
            player.play()
            AudioPlaybackService.start(appContext)
            updateState()
        }
    }

    fun togglePlayback() {
        scope.launch {
            if (currentQueue.isEmpty()) return@launch
            lastErrorMessage = null
            if (player.isPlaying) {
                player.pause()
            } else {
                playbackConflictManager.onAudioPlaybackStarting()
                if (player.playbackState == Player.STATE_ENDED) {
                    val currentIndex = player.currentMediaItemIndex.takeIf { it >= 0 } ?: 0
                    player.seekTo(currentIndex, 0L)
                }
                player.playWhenReady = true
                player.play()
            }
            AudioPlaybackService.start(appContext)
            updateState()
        }
    }

    fun pausePlayback() {
        scope.launch {
            pausePlaybackInternal()
        }
    }

    fun seekBy(offsetMs: Long) {
        scope.launch {
            if (currentQueue.isEmpty()) return@launch
            val durationMs = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            val targetPosition = (player.currentPosition + offsetMs).coerceIn(0L, durationMs)
            player.seekTo(targetPosition)
            updateState()
        }
    }

    fun seekTo(positionMs: Long) {
        scope.launch {
            if (currentQueue.isEmpty()) return@launch
            val durationMs = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
            player.seekTo(positionMs.coerceIn(0L, durationMs))
            updateState()
        }
    }

    fun skipPrevious() {
        scope.launch {
            if (currentQueue.isEmpty()) return@launch
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            } else {
                player.seekTo(0L)
            }
            AudioPlaybackService.start(appContext)
            updateState()
        }
    }

    fun skipNext() {
        scope.launch {
            if (currentQueue.isEmpty()) return@launch
            when {
                player.hasNextMediaItem() -> player.seekToNext()
                state.value.repeatMode == PlaylistRepeatMode.ALL && currentQueue.isNotEmpty() -> {
                    player.seekToDefaultPosition(0)
                    player.playWhenReady = true
                    player.play()
                }
                else -> player.seekTo(player.currentPosition)
            }
            AudioPlaybackService.start(appContext)
            updateState()
        }
    }

    fun toggleShuffle() {
        scope.launch {
            if (currentQueue.isEmpty()) return@launch
            val currentTaskId = state.value.currentTaskId
            val currentPosition = player.currentPosition.coerceAtLeast(0L)
            val repeatMode = player.repeatMode
            val shouldShuffle = !shuffleEnabled
            currentQueue = if (shouldShuffle) {
                shuffledQueueKeepingStart(
                    items = originalQueue.ifEmpty { currentQueue },
                    startTaskId = currentTaskId,
                )
            } else {
                originalQueue.ifEmpty { currentQueue }
            }
            shuffleEnabled = shouldShuffle
            val startIndex = currentTaskId
                ?.let { taskId -> currentQueue.indexOfFirst { it.taskId == taskId } }
                ?.takeIf { it >= 0 }
                ?: 0
            player.shuffleModeEnabled = false
            player.setMediaItems(
                currentQueue.map { queueItem -> queueItem.toMediaItem() },
                startIndex,
                currentPosition,
            )
            player.repeatMode = repeatMode
            player.prepare()
            player.playWhenReady = true
            updateState()
        }
    }

    fun cycleRepeatMode() {
        scope.launch {
            val nextMode = when (state.value.repeatMode) {
                PlaylistRepeatMode.OFF -> PlaylistRepeatMode.ALL
                PlaylistRepeatMode.ALL -> PlaylistRepeatMode.ONE
                PlaylistRepeatMode.ONE -> PlaylistRepeatMode.OFF
            }
            player.repeatMode = nextMode.asPlayerRepeatMode()
            updateState()
        }
    }

    fun setSleepTimer(durationMs: Long?) {
        scope.launch {
            sleepTimerJob?.cancel()
            val endsAt = durationMs
                ?.takeIf { it > 0L }
                ?.let { System.currentTimeMillis() + it }

            _state.update { current ->
                current.copy(
                    sleepTimerEndsAtEpochMs = endsAt,
                    sleepTimerRemainingMs = endsAt?.minus(System.currentTimeMillis())?.coerceAtLeast(0L),
                )
            }

            if (endsAt == null) return@launch

            sleepTimerJob = scope.launch {
                val delayMs = (endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
                delay(delayMs)
                player.pause()
                _state.update { current ->
                    current.copy(
                        sleepTimerEndsAtEpochMs = null,
                        sleepTimerRemainingMs = null,
                    )
                }
                AudioPlaybackService.start(appContext)
                updateState()
            }
        }
    }

    fun stopPlayback() {
        scope.launch {
            clearQueueAndStopInternal()
        }
    }

    fun dismissError() {
        scope.launch {
            if (lastErrorMessage == null) return@launch
            lastErrorMessage = null
            updateState()
        }
    }

    private fun clearQueueAndStopInternal() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        currentQueue = emptyList()
        originalQueue = emptyList()
        shuffleEnabled = false
        lastErrorMessage = null
        player.pause()
        player.clearMediaItems()
        _state.value = AudioPlaybackState()
        AudioPlaybackService.stop(appContext)
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateState()
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun updateState() {
        val currentIndex = player.currentMediaItemIndex
            .takeIf { index -> index in currentQueue.indices }
            ?: -1
        val currentItem = currentQueue.getOrNull(currentIndex)
        val durationMs = player.duration.takeIf { it > 0 } ?: 0L
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val sleepEndsAt = _state.value.sleepTimerEndsAtEpochMs
        val sleepRemaining = sleepEndsAt
            ?.minus(System.currentTimeMillis())
            ?.coerceAtLeast(0L)
            ?.takeIf { currentQueue.isNotEmpty() }

        _state.value = AudioPlaybackState(
            queue = currentQueue,
            currentIndex = currentIndex,
            currentTaskId = currentItem?.taskId,
            currentTitle = currentItem?.title.orEmpty(),
            currentFilePath = currentItem?.filePath,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = durationMs,
            positionMs = positionMs.coerceAtMost(durationMs.takeIf { it > 0 } ?: positionMs),
            repeatMode = player.repeatMode.asPlaylistRepeatMode(),
            shuffleEnabled = shuffleEnabled,
            sleepTimerEndsAtEpochMs = sleepEndsAt?.takeIf { currentQueue.isNotEmpty() },
            sleepTimerRemainingMs = sleepRemaining,
            errorMessage = lastErrorMessage,
        )
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        playbackConflictManager.unregisterAudioPauseHandler(this)
        player.removeListener(playerListener)
        player.release()
    }

    private fun pausePlaybackForConflict() {
        pausePlaybackInternal()
    }

    private fun pausePlaybackInternal() {
        if (currentQueue.isEmpty()) return
        player.pause()
        AudioPlaybackService.start(appContext)
        updateState()
    }

    private fun AudioQueueItem.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(taskId)
            .setUri(filePath.toPlaybackUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .build(),
            )
            .build()
    }

    private fun String.isPlayableAudioLocation(): Boolean {
        if (isBlank()) return false
        return startsWith("content://", ignoreCase = true) ||
            startsWith("file://", ignoreCase = true) ||
            File(this).exists()
    }

    private fun String.toPlaybackUri(): Uri {
        return when {
            startsWith("content://", ignoreCase = true) -> Uri.parse(this)
            startsWith("file://", ignoreCase = true) -> Uri.parse(this)
            else -> Uri.fromFile(File(this))
        }
    }

    private fun shuffledQueueKeepingStart(
        items: List<AudioQueueItem>,
        startTaskId: String?,
    ): List<AudioQueueItem> {
        val startItem = startTaskId
            ?.let { taskId -> items.firstOrNull { it.taskId == taskId } }
            ?: items.randomOrNull()
            ?: return emptyList()
        val shuffledTail = items
            .filterNot { it.taskId == startItem.taskId }
            .shuffled()
        return listOf(startItem) + shuffledTail
    }

    private fun PlaylistRepeatMode.asPlayerRepeatMode(): Int {
        return when (this) {
            PlaylistRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            PlaylistRepeatMode.ALL -> Player.REPEAT_MODE_ALL
            PlaylistRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    private fun Int.asPlaylistRepeatMode(): PlaylistRepeatMode {
        return when (this) {
            Player.REPEAT_MODE_ALL -> PlaylistRepeatMode.ALL
            Player.REPEAT_MODE_ONE -> PlaylistRepeatMode.ONE
            else -> PlaylistRepeatMode.OFF
        }
    }

    companion object {
        private const val PROGRESS_UPDATE_MS = 1_000L
        const val SEEK_INCREMENT_MS = 10_000L
    }
}

data class AudioQueueItem(
    val taskId: String,
    val title: String,
    val filePath: String,
)

data class AudioPlaybackState(
    val queue: List<AudioQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val currentTaskId: String? = null,
    val currentTitle: String = "",
    val currentFilePath: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val repeatMode: PlaylistRepeatMode = PlaylistRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val sleepTimerEndsAtEpochMs: Long? = null,
    val sleepTimerRemainingMs: Long? = null,
    val errorMessage: String? = null,
) {
    val hasQueue: Boolean
        get() = queue.isNotEmpty()

    val queueCount: Int
        get() = queue.size

    val currentTrackNumber: Int
        get() = if (currentIndex >= 0) currentIndex + 1 else 0
}

enum class PlaylistRepeatMode {
    OFF,
    ALL,
    ONE,
}
