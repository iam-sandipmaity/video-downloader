package com.localdownloader.viewmodel

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import androidx.media3.common.AudioAttributes
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
import com.localdownloader.audio.PlaybackConflictManager
import com.localdownloader.data.PlaybackSession
import com.localdownloader.data.PlaybackSessionStore
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.media.PlayerResizeModes
import com.localdownloader.media.resolvePreferredMediaMimeType
import com.localdownloader.utils.FileUtils
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
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackSessionStore: PlaybackSessionStore,
    private val savedStateHandle: SavedStateHandle,
    private val logger: Logger,
    private val playbackConflictManager: PlaybackConflictManager,
    private val fileUtils: FileUtils,
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
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
        }

    private var currentSessionKey: String? = savedStateHandle[STATE_SESSION_KEY]
    private var currentPlayablePath: String? = savedStateHandle[STATE_PLAYABLE_PATH]
    private var shouldResumeOnForeground = false
    private var progressJob: Job? = null
    private var currentPlaybackSpeed: Float = savedStateHandle[STATE_PLAYBACK_SPEED] ?: 1.0f
    private var currentResizeMode: Int = savedStateHandle[STATE_RESIZE_MODE] ?: PlayerResizeModes.FIT
    private var currentVolumeBoostMb: Int = savedStateHandle[STATE_VOLUME_BOOST_MB] ?: 0
    private var isLocked: Boolean = savedStateHandle[STATE_IS_LOCKED] ?: false
    private var audioDisabled: Boolean = savedStateHandle[STATE_AUDIO_DISABLED] ?: false
    private var subtitlesDisabled: Boolean = savedStateHandle[STATE_SUBTITLES_DISABLED] ?: false
    private var volumeBoostSupported: Boolean = true
    private var loudnessEnhancer: LoudnessEnhancer? = null

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
                    isSeekable = isCurrentMediaSeekable(),
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
                    errorMessage = friendlyPlaybackErrorMessage(
                        rawMessage = error.message,
                        playablePath = currentPlayablePath,
                    ),
                    isBuffering = false,
                )
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateTrackOptions(tracks)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachLoudnessEnhancer(audioSessionId)
        }
    }

    init {
        playbackConflictManager.registerVideoPauseHandler(this, ::pausePlaybackForConflict)
        player.addListener(playerListener)
        player.setPlaybackSpeed(currentPlaybackSpeed)
        _uiState.value = _uiState.value.copy(
            playbackSpeed = currentPlaybackSpeed,
            resizeMode = currentResizeMode,
            volumeBoostMb = currentVolumeBoostMb,
            volumeBoostSupported = volumeBoostSupported,
            isLocked = isLocked,
            audioDisabled = audioDisabled,
            subtitlesDisabled = subtitlesDisabled,
        )
        attachLoudnessEnhancer(player.audioSessionId)
        startProgressUpdates()
    }

    fun bindTask(task: DownloadTask?) {
        val previousSessionKey = currentSessionKey
        val rawPlayablePath = task?.outputPath?.takeIf { path ->
            path.isPlayableMediaLocation()
        }
        var playablePath = rawPlayablePath
        if (task?.isInVault == true && rawPlayablePath != null) {
            runCatching {
                val cacheFile = File(context.cacheDir, "vault_play_${task.id}")
                if (!cacheFile.exists()) {
                    com.localdownloader.utils.VaultCrypto.decryptFile(File(rawPlayablePath), cacheFile)
                }
                playablePath = cacheFile.absolutePath
            }
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
                    isSeekable = false,
                    playbackSpeed = currentPlaybackSpeed,
                    resizeMode = currentResizeMode,
                    volumeBoostMb = currentVolumeBoostMb,
                    volumeBoostSupported = volumeBoostSupported,
                    audioDisabled = audioDisabled,
                    subtitlesDisabled = subtitlesDisabled,
                    audioTracks = emptyList(),
                    subtitleTracks = emptyList(),
                    errorMessage = null,
                )
            }
            return
        }

        playbackConflictManager.onVideoPlaybackStarting()

        if (currentSessionKey == sessionKey && currentPlayablePath == playablePath && player.mediaItemCount > 0) {
            _uiState.update { state ->
                state.copy(
                    title = title,
                    isAvailable = true,
                    playbackSpeed = currentPlaybackSpeed,
                    resizeMode = currentResizeMode,
                    volumeBoostMb = currentVolumeBoostMb,
                    volumeBoostSupported = volumeBoostSupported,
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
        val subtitlePaths = task?.subtitlePaths.orEmpty()

        logger.i(
            "PlayerViewModel",
            "Loading media for playback sessionKey=$sessionKey path=$playablePath subtitles=${subtitlePaths.size}",
        )
        player.setMediaItem(
            buildMediaItem(
                playableLocation = playablePath,
                explicitSubtitlePaths = subtitlePaths,
            ),
        )
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
                isSeekable = isCurrentMediaSeekable(),
                playbackSpeed = currentPlaybackSpeed,
                resizeMode = currentResizeMode,
                volumeBoostMb = currentVolumeBoostMb,
                volumeBoostSupported = volumeBoostSupported,
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
            playbackConflictManager.onVideoPlaybackStarting()
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
        }
        persistPlaybackState()
    }

    fun seekTo(positionMs: Long) {
        if (!uiState.value.isAvailable) return
        val durationMs = player.duration.takeIf { it > 0 } ?: return
        if (!player.isCurrentMediaItemSeekable) return
        player.seekTo(positionMs.coerceIn(0L, durationMs))
        snapshotPlaybackState()
        persistPlaybackState()
    }

    fun seekBy(offsetMs: Long): Long {
        if (!uiState.value.isAvailable) return 0L
        val durationMs = player.duration.takeIf { it > 0 } ?: return 0L
        if (!player.isCurrentMediaItemSeekable) return 0L
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

    fun setVolumeBoostMb(targetGainMb: Int) {
        currentVolumeBoostMb = targetGainMb.coerceIn(0, MAX_VOLUME_BOOST_MB)
        savedStateHandle[STATE_VOLUME_BOOST_MB] = currentVolumeBoostMb
        applyVolumeBoost()
        _uiState.update { state ->
            state.copy(
                volumeBoostMb = currentVolumeBoostMb,
                volumeBoostSupported = volumeBoostSupported,
            )
        }
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
        audioDisabled = false
        savedStateHandle[STATE_AUDIO_DISABLED] = false
        player.trackSelectionParameters = builder.build()
        updateTrackOptions(player.currentTracks)
    }

    fun disableAudio() {
        audioDisabled = true
        savedStateHandle[STATE_AUDIO_DISABLED] = true
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
            playbackConflictManager.onVideoPlaybackStarting()
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
                isSeekable = isCurrentMediaSeekable(),
            )
        }
    }

    private fun isCurrentMediaSeekable(): Boolean {
        return player.duration > 0 && player.isCurrentMediaItemSeekable
    }

    private fun pausePlaybackForConflict() {
        if (!uiState.value.isAvailable && player.mediaItemCount == 0) return
        shouldResumeOnForeground = false
        player.playWhenReady = false
        player.pause()
        snapshotPlaybackState()
        persistPlaybackState(forcePlayWhenReady = false)
    }

    private fun attachLoudnessEnhancer(audioSessionId: Int) {
        loudnessEnhancer?.release()
        loudnessEnhancer = null

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) {
            return
        }

        loudnessEnhancer = runCatching {
            LoudnessEnhancer(audioSessionId).also { enhancer ->
                volumeBoostSupported = true
                enhancer.enabled = currentVolumeBoostMb > 0
                if (currentVolumeBoostMb > 0) {
                    enhancer.setTargetGain(currentVolumeBoostMb)
                }
            }
        }.onFailure { error ->
            volumeBoostSupported = false
            logger.w("PlayerViewModel", "Volume boost is not available on this device/session", error)
        }.getOrNull()

        _uiState.update { state ->
            state.copy(
                volumeBoostMb = currentVolumeBoostMb,
                volumeBoostSupported = volumeBoostSupported,
            )
        }
    }

    private fun applyVolumeBoost() {
        val enhancer = loudnessEnhancer
        if (enhancer == null) {
            return
        }

        runCatching {
            enhancer.setTargetGain(currentVolumeBoostMb)
            enhancer.enabled = currentVolumeBoostMb > 0
            volumeBoostSupported = true
        }.onFailure { error ->
            volumeBoostSupported = false
            logger.w("PlayerViewModel", "Failed applying volume boost", error)
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

    private fun buildMediaItem(
        playableLocation: String,
        explicitSubtitlePaths: List<String>,
    ): MediaItem {
        val playableFile = playableLocation
            .takeUnless { it.startsWith("content://", ignoreCase = true) }
            ?.let(::File)
            ?.takeIf { it.exists() }
        val subtitleConfigurations = discoverLocalSubtitleConfigurations(
            playableFile = playableFile,
            explicitSubtitlePaths = explicitSubtitlePaths,
        )
        return MediaItem.Builder()
            .apply {
                setUri(playableLocation.toPlaybackUri())
                resolvePreferredMediaMimeType(playableFile?.name ?: playableLocation)?.let(::setMimeType)
                setSubtitleConfigurations(subtitleConfigurations)
            }
            .build()
    }

    private fun discoverLocalSubtitleConfigurations(
        playableFile: File?,
        explicitSubtitlePaths: List<String>,
    ): List<MediaItem.SubtitleConfiguration> {
        val parentDir = playableFile?.parentFile ?: return emptyList()
        val stem = playableFile.nameWithoutExtension
        val explicitFiles = explicitSubtitlePaths
            .map(::File)
            .filter { it.exists() }
        val fallbackFiles = parentDir.listFiles()
            ?.asSequence()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.name.startsWith("$stem.") &&
                    candidate.name != playableFile.name
            }
            ?.sortedBy { it.name }
            ?.toList()
            .orEmpty()

        return (explicitFiles + fallbackFiles)
            .distinctBy { it.absolutePath }
            .mapNotNull { subtitleFile ->
                val mimeType = resolveSubtitleMimeType(subtitleFile.extension) ?: return@mapNotNull null
                MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subtitleFile))
                    .setMimeType(mimeType)
                    .setLabel(buildSubtitleLabel(stem, subtitleFile))
                    .setLanguage(extractSubtitleLanguage(stem, subtitleFile))
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                    .build()
            }
    }

    private fun resolveSubtitleMimeType(extension: String): String? {
        return when (extension.lowercase()) {
            "srt" -> "application/x-subrip"
            "vtt", "webvtt" -> "text/vtt"
            "ssa", "ass" -> "text/x-ssa"
            "ttml", "dfxp", "xml" -> "application/ttml+xml"
            else -> null
        }
    }

    private fun buildSubtitleLabel(stem: String, subtitleFile: File): String {
        val suffix = subtitleFile.name
            .removePrefix(stem)
            .substringBeforeLast('.', "")
            .trim('.', '-', '_', ' ')
        return suffix
            .replace('.', ' ')
            .replace('_', ' ')
            .ifBlank { subtitleFile.extension.uppercase() }
    }

    private fun extractSubtitleLanguage(stem: String, subtitleFile: File): String? {
        val suffix = subtitleFile.name
            .removePrefix(stem)
            .substringBeforeLast('.', "")
            .trim('.', '-', '_', ' ')
        return suffix
            .split('.', '_', '-')
            .firstOrNull()
            ?.lowercase()
            ?.takeIf { token -> token.length in 2..8 && token.all { it.isLetter() || it == '_' } }
    }

    override fun onCleared() {
        persistPlaybackState()
        progressJob?.cancel()
        playbackConflictManager.unregisterVideoPauseHandler(this)
        player.removeListener(playerListener)
        loudnessEnhancer?.release()
        runCatching {
            val cacheFile = File(context.cacheDir, "vault_play_${currentSessionKey}")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        }
        player.release()
        super.onCleared()
    }

    private fun String.isPlayableMediaLocation(): Boolean {
        if (isBlank()) return false
        return startsWith("content://", ignoreCase = true) ||
            startsWith("file://", ignoreCase = true) ||
            fileUtils.managedFileExists(this) ||
            File(this).exists()
    }

    private fun String.toPlaybackUri(): Uri {
        return when {
            startsWith("content://", ignoreCase = true) -> Uri.parse(this)
            startsWith("file://", ignoreCase = true) -> Uri.parse(this)
            else -> Uri.fromFile(File(this))
        }
    }

    companion object {
        private const val STATE_SESSION_KEY = "player_session_key"
        private const val STATE_PLAYABLE_PATH = "player_playable_path"
        private const val STATE_POSITION_MS = "player_position_ms"
        private const val STATE_PLAY_WHEN_READY = "player_play_when_ready"
        private const val STATE_PLAYBACK_SPEED = "player_playback_speed"
        private const val STATE_RESIZE_MODE = "player_resize_mode"
        private const val STATE_VOLUME_BOOST_MB = "player_volume_boost_mb"
        private const val STATE_IS_LOCKED = "player_is_locked"
        private const val STATE_AUDIO_DISABLED = "player_audio_disabled"
        private const val STATE_SUBTITLES_DISABLED = "player_subtitles_disabled"
        private const val PROGRESS_UPDATE_MS = 500L
        private const val MAX_VOLUME_BOOST_MB = 900
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
    val isSeekable: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val resizeMode: Int = PlayerResizeModes.FIT,
    val volumeBoostMb: Int = 0,
    val volumeBoostSupported: Boolean = true,
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
