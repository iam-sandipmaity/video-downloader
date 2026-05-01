package com.localplayer.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
import com.localplayer.data.PlaybackSession
import com.localplayer.data.PlaybackSessionStore
import com.localplayer.model.PlayerMedia
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private val playbackSessionStore = PlaybackSessionStore()

    val player: ExoPlayer = ExoPlayer.Builder(application)
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

    private var currentSessionKey: String? = null
    private var currentPlayablePath: String? = null
    private var shouldResumeOnForeground = false
    private var progressJob: Job? = null
    private var currentPlaybackSpeed: Float = 1.0f
    private var currentResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var isLocked: Boolean = false
    private var audioDisabled: Boolean = false
    private var subtitlesDisabled: Boolean = false

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
            Log.e("PlayerViewModel", "Playback error", error)
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
            audioDisabled = audioDisabled,
            subtitlesDisabled = subtitlesDisabled,
        )
        startProgressUpdates()
    }

    fun bindMedia(media: PlayerMedia?) {
        val playablePath = media?.uriString?.takeIf { it.isNotBlank() }
        val title = media?.title.orEmpty()
        val sessionKey = media?.id ?: playablePath

        if (playablePath == null || sessionKey == null) {
            currentSessionKey = null
            currentPlayablePath = null
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
                    audioDisabled = audioDisabled,
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
                    audioDisabled = audioDisabled,
                    subtitlesDisabled = subtitlesDisabled,
                )
            }
            return
        }

        persistPlaybackState()

        currentSessionKey = sessionKey
        currentPlayablePath = playablePath

        val storedSession = playbackSessionStore.get(sessionKey)
        val savedPosition = storedSession?.positionMs
            ?: 0L
        val savedPlayWhenReady = storedSession?.playWhenReady
            ?: true

        Log.i("PlayerViewModel", "Loading media for playback sessionKey=$sessionKey uri=$playablePath")
        player.setMediaItem(MediaItem.fromUri(Uri.parse(playablePath)))
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
                audioDisabled = audioDisabled,
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
        player.setPlaybackSpeed(speed)
        _uiState.update { state -> state.copy(playbackSpeed = speed) }
    }

    fun setResizeMode(resizeMode: Int) {
        currentResizeMode = resizeMode
        _uiState.update { state -> state.copy(resizeMode = resizeMode) }
    }

    fun toggleLock() {
        isLocked = !isLocked
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
        audioDisabled = false
        player.trackSelectionParameters = builder.build()
        updateTrackOptions(player.currentTracks)
    }

    fun disableAudio() {
        audioDisabled = true
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
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
        player.trackSelectionParameters = builder.build()
        updateTrackOptions(player.currentTracks)
    }

    fun disableSubtitles() {
        subtitlesDisabled = true
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        updateTrackOptions(player.currentTracks)
    }

    fun enableSubtitlesAuto() {
        subtitlesDisabled = false
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

    fun onAppBackgrounded(allowBackgroundPlayback: Boolean) {
        shouldResumeOnForeground = !allowBackgroundPlayback && player.isPlaying
        persistPlaybackState(forcePlayWhenReady = player.isPlaying)
        if (!allowBackgroundPlayback && player.isPlaying) {
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
                audioDisabled = audioDisabled,
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
    val audioDisabled: Boolean = false,
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
