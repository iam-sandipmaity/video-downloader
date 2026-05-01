package com.localdownloader.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.localdownloader.data.PlaybackSession
import com.localdownloader.data.PlaybackSessionStore
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val playbackSessionStore: PlaybackSessionStore,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15_000,
                    50_000,
                    2_500,
                    5_000,
                )
                .build(),
        )
        .build()

    private var currentSessionKey: String? = savedStateHandle[STATE_SESSION_KEY]
    private var currentPlayablePath: String? = savedStateHandle[STATE_PLAYABLE_PATH]
    private var shouldResumeOnForeground = false
    private var progressJob: Job? = null
    private var currentPlaybackSpeed: Float = savedStateHandle[STATE_PLAYBACK_SPEED] ?: 1.0f
    private var currentResizeMode: Int = savedStateHandle[STATE_RESIZE_MODE] ?: AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var isLocked: Boolean = savedStateHandle[STATE_IS_LOCKED] ?: false
    private var subtitlesDisabled: Boolean = savedStateHandle[STATE_SUBTITLES_DISABLED] ?: false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val isCompleted = playbackState == Player.STATE_ENDED
            if (isCompleted) {
                persistPlaybackState(forcePlayWhenReady = false)
            }
            _uiState.update { state ->
                state.copy(
                    isBuffering = isBuffering,
                    isCompleted = isCompleted,
                    durationMs = player.duration.takeIf { it > 0 } ?: state.durationMs,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    bufferedPositionMs = player.bufferedPosition.coerceAtLeast(player.currentPosition),
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { state ->
                state.copy(
                    isPlaying = isPlaying,
                    isCompleted = !isPlaying && player.playbackState == Player.STATE_ENDED,
                )
            }
            persistPlaybackState()
        }

        override fun onPlayerError(error: PlaybackException) {
            logger.e("PlayerViewModel", "Playback error", error)
            _uiState.update { state ->
                state.copy(
                    errorMessage = error.message ?: "Unable to play this media file.",
                    isBuffering = false,
                )
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackOptions(tracks)
        }
    }

    init {
        player.addListener(playerListener)
        player.setPlaybackSpeed(currentPlaybackSpeed)
        _uiState.value = _uiState.value.copy(
            playbackSpeed = currentPlaybackSpeed,
            resizeMode = currentResizeMode,
            isLocked = isLocked,
            subtitlesDisabled = subtitlesDisabled,
        )
        startProgressUpdates()
    }

    fun bindTask(task: DownloadTask?) {
        val previousSessionKey = currentSessionKey
        val playablePath = task?.outputPath?.takeIf { path ->
            path.isNotBlank() && File(path).exists()
        }
        val title = task?.title.orEmpty()
        val sessionKey = task?.id ?: playablePath

        if (playablePath == null || sessionKey == null) {
            currentSessionKey = null
            currentPlayablePath = null
            savedStateHandle.remove<String>(STATE_SESSION_KEY)
            savedStateHandle.remove<String>(STATE_PLAYABLE_PATH)
            savedStateHandle.remove<Long>(STATE_POSITION_MS)
            savedStateHandle.remove<Boolean>(STATE_PLAY_WHEN_READY)
            player.pause()
            player.clearMediaItems()
            _uiState.update { state ->
                state.copy(
                    title = title,
                    isAvailable = false,
                    isPlaying = false,
                    isBuffering = false,
                    isCompleted = false,
                    isLocked = isLocked,
                    durationMs = 0L,
                    positionMs = 0L,
                    bufferedPositionMs = 0L,
                    playbackSpeed = currentPlaybackSpeed,
                    resizeMode = currentResizeMode,
                    subtitlesDisabled = subtitlesDisabled,
                    audioTracks = emptyList(),
                    subtitleTracks = emptyList(),
                    errorMessage = null,
                )
            }
            return
        }

        if (currentSessionKey == sessionKey && currentPlayablePath == playablePath && player.mediaItemCount > 0) {
            _uiState.update { state ->
                state.copy(
                    title = title,
                    isAvailable = true,
                    playbackSpeed = currentPlaybackSpeed,
                    resizeMode = currentResizeMode,
                    isLocked = isLocked,
                    subtitlesDisabled = subtitlesDisabled,
                )
            }
            return
        }

        persistPlaybackState()

        currentSessionKey = sessionKey
        currentPlayablePath = playablePath
        savedStateHandle[STATE_SESSION_KEY] = sessionKey
        savedStateHandle[STATE_PLAYABLE_PATH] = playablePath

        val storedSession = playbackSessionStore.get(sessionKey)
        val restoredSavedStatePosition = if (previousSessionKey == sessionKey) {
            savedStateHandle.get<Long>(STATE_POSITION_MS)
        } else {
            null
        }
        val restoredSavedStatePlayWhenReady = if (previousSessionKey == sessionKey) {
            savedStateHandle.get<Boolean>(STATE_PLAY_WHEN_READY)
        } else {
            null
        }
        val savedPosition = storedSession?.positionMs
            ?: restoredSavedStatePosition
            ?: 0L
        val savedPlayWhenReady = storedSession?.playWhenReady
            ?: restoredSavedStatePlayWhenReady
            ?: true

        logger.i("PlayerViewModel", "Loading media for playback sessionKey=$sessionKey path=$playablePath")
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(playablePath))))
        player.prepare()
        if (savedPosition > 0L) {
            player.seekTo(savedPosition)
        }
        player.setPlaybackSpeed(currentPlaybackSpeed)
        player.playWhenReady = savedPlayWhenReady

        _uiState.update { state ->
            state.copy(
                title = title,
                isAvailable = true,
                isPlaying = savedPlayWhenReady,
                isBuffering = true,
                isCompleted = false,
                isLocked = isLocked,
                durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                positionMs = savedPosition,
                bufferedPositionMs = savedPosition,
                playbackSpeed = currentPlaybackSpeed,
                resizeMode = currentResizeMode,
                subtitlesDisabled = subtitlesDisabled,
                errorMessage = null,
            )
        }
    }

    fun togglePlayback() {
        if (!uiState.value.isAvailable) return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
        persistPlaybackState()
    }

    fun seekTo(positionMs: Long) {
        if (!uiState.value.isAvailable) return
        player.seekTo(positionMs.coerceIn(0L, player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        snapshotPlaybackState()
        persistPlaybackState()
    }

    fun seekBy(offsetMs: Long): Long {
        if (!uiState.value.isAvailable) return 0L
        val durationMs = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val previousPosition = player.currentPosition
        val targetPosition = (previousPosition + offsetMs).coerceIn(0L, durationMs)
        player.seekTo(targetPosition)
        snapshotPlaybackState()
        persistPlaybackState()
        return targetPosition - previousPosition
    }

    fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed
        savedStateHandle[STATE_PLAYBACK_SPEED] = speed
        player.setPlaybackSpeed(speed)
        _uiState.update { state -> state.copy(playbackSpeed = speed) }
    }

    fun setResizeMode(resizeMode: Int) {
        currentResizeMode = resizeMode
        savedStateHandle[STATE_RESIZE_MODE] = resizeMode
        _uiState.update { state -> state.copy(resizeMode = resizeMode) }
    }

    fun toggleLock() {
        isLocked = !isLocked
        savedStateHandle[STATE_IS_LOCKED] = isLocked
        _uiState.update { state -> state.copy(isLocked = isLocked) }
    }

    fun selectAudioTrack(option: PlayerTrackOption?) {
        val builder = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        if (option != null) {
            builder.setOverrideForType(
                TrackSelectionOverride(option.trackGroup, option.trackIndex),
            )
        }
        player.trackSelectionParameters = builder.build()
        updateTrackOptions(player.currentTracks)
    }

    fun selectSubtitleTrack(option: PlayerTrackOption?) {
        val builder = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        if (option != null) {
            builder.setOverrideForType(
                TrackSelectionOverride(option.trackGroup, option.trackIndex),
            )
        }
        subtitlesDisabled = false
        savedStateHandle[STATE_SUBTITLES_DISABLED] = false
        player.trackSelectionParameters = builder.build()
        updateTrackOptions(player.currentTracks)
    }

    fun disableSubtitles() {
        subtitlesDisabled = true
        savedStateHandle[STATE_SUBTITLES_DISABLED] = true
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        updateTrackOptions(player.currentTracks)
    }

    fun enableSubtitlesAuto() {
        subtitlesDisabled = false
        savedStateHandle[STATE_SUBTITLES_DISABLED] = false
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        updateTrackOptions(player.currentTracks)
    }

    fun onAppForegrounded() {
        if (shouldResumeOnForeground && uiState.value.isAvailable) {
            player.playWhenReady = true
            player.play()
        }
        shouldResumeOnForeground = false
        snapshotPlaybackState()
    }

    fun onAppBackgrounded() {
        shouldResumeOnForeground = player.isPlaying
        persistPlaybackState(forcePlayWhenReady = player.isPlaying)
        if (player.isPlaying) {
            player.pause()
        }
    }

    fun persistPlaybackState(forcePlayWhenReady: Boolean? = null) {
        val sessionKey = currentSessionKey ?: return
        val session = PlaybackSession(
            positionMs = player.currentPosition.coerceAtLeast(0L),
            playWhenReady = forcePlayWhenReady ?: player.playWhenReady,
        )
        playbackSessionStore.save(sessionKey, session)
        savedStateHandle[STATE_POSITION_MS] = session.positionMs
        savedStateHandle[STATE_PLAY_WHEN_READY] = session.playWhenReady
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                snapshotPlaybackState()
                delay(PROGRESS_UPDATE_MS)
            }
        }
    }

    private fun snapshotPlaybackState() {
        if (!uiState.value.isAvailable) return
        _uiState.update { state ->
            state.copy(
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                isCompleted = player.playbackState == Player.STATE_ENDED,
                durationMs = player.duration.takeIf { it > 0 } ?: state.durationMs,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(player.currentPosition),
            )
        }
    }

    private fun updateTrackOptions(tracks: Tracks) {
        val audioTracks = mutableListOf<PlayerTrackOption>()
        val subtitleTracks = mutableListOf<PlayerTrackOption>()

        tracks.groups.forEach { group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (trackIndex in 0 until group.length) {
                        if (!group.isTrackSupported(trackIndex)) continue
                        audioTracks += PlayerTrackOption(
                            title = buildTrackTitle(group.getTrackFormat(trackIndex), trackIndex, "Audio"),
                            subtitle = buildTrackSubtitle(group.getTrackFormat(trackIndex)),
                            trackGroup = group.mediaTrackGroup,
                            trackIndex = trackIndex,
                            isSelected = group.isTrackSelected(trackIndex),
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (trackIndex in 0 until group.length) {
                        if (!group.isTrackSupported(trackIndex)) continue
                        subtitleTracks += PlayerTrackOption(
                            title = buildTrackTitle(group.getTrackFormat(trackIndex), trackIndex, "Subtitle"),
                            subtitle = buildTrackSubtitle(group.getTrackFormat(trackIndex)),
                            trackGroup = group.mediaTrackGroup,
                            trackIndex = trackIndex,
                            isSelected = group.isTrackSelected(trackIndex),
                        )
                    }
                }
            }
        }

        _uiState.update { state ->
            state.copy(
                audioTracks = audioTracks,
                subtitleTracks = subtitleTracks,
                subtitlesDisabled = subtitlesDisabled,
            )
        }
    }

    private fun buildTrackTitle(format: Format, trackIndex: Int, fallbackPrefix: String): String {
        return format.label?.takeIf { it.isNotBlank() }
            ?: format.language?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
            ?: "$fallbackPrefix ${trackIndex + 1}"
    }

    private fun buildTrackSubtitle(format: Format): String? {
        val parts = buildList {
            format.language
                ?.takeIf { it.isNotBlank() && !it.equals("und", ignoreCase = true) }
                ?.let { add(it.uppercase()) }
            if (format.channelCount > 0) {
                add("${format.channelCount}ch")
            }
            format.sampleMimeType
                ?.substringAfterLast('.')
                ?.takeIf { it.isNotBlank() }
                ?.uppercase()
                ?.let { add(it) }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" | ")
    }

    override fun onCleared() {
        persistPlaybackState()
        progressJob?.cancel()
        player.removeListener(playerListener)
        player.release()
        super.onCleared()
    }

    companion object {
        private const val STATE_SESSION_KEY = "player_session_key"
        private const val STATE_PLAYABLE_PATH = "player_playable_path"
        private const val STATE_POSITION_MS = "player_position_ms"
        private const val STATE_PLAY_WHEN_READY = "player_play_when_ready"
        private const val STATE_PLAYBACK_SPEED = "player_playback_speed"
        private const val STATE_RESIZE_MODE = "player_resize_mode"
        private const val STATE_IS_LOCKED = "player_is_locked"
        private const val STATE_SUBTITLES_DISABLED = "player_subtitles_disabled"
        private const val PROGRESS_UPDATE_MS = 500L
    }
}

data class PlayerUiState(
    val title: String = "",
    val isAvailable: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    val subtitlesDisabled: Boolean = false,
    val audioTracks: List<PlayerTrackOption> = emptyList(),
    val subtitleTracks: List<PlayerTrackOption> = emptyList(),
    val errorMessage: String? = null,
)

data class PlayerTrackOption(
    val title: String,
    val subtitle: String?,
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val isSelected: Boolean,
)
