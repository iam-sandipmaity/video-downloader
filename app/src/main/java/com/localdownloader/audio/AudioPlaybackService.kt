package com.localdownloader.audio

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.localdownloader.AppLaunchRouter
import com.localdownloader.notifications.AppNotifications
import com.localdownloader.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint(Service::class)
class AudioPlaybackService : Hilt_AudioPlaybackService() {
    @Inject
    lateinit var audioPlaybackManager: AudioPlaybackManager

    @Inject
    lateinit var logger: Logger

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateCollectionJob: Job? = null
    private var startedInForeground = false
    private lateinit var mediaSession: MediaSessionCompat
    private val mediaInfoCache = mutableMapOf<String, NotificationMediaInfo>()

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            if (!audioPlaybackManager.state.value.isPlaying) {
                audioPlaybackManager.togglePlayback()
            }
        }

        override fun onPause() {
            if (audioPlaybackManager.state.value.isPlaying) {
                audioPlaybackManager.togglePlayback()
            }
        }

        override fun onSkipToNext() {
            audioPlaybackManager.skipNext()
        }

        override fun onSkipToPrevious() {
            audioPlaybackManager.skipPrevious()
        }

        override fun onSeekTo(pos: Long) {
            audioPlaybackManager.seekTo(pos)
        }

        override fun onFastForward() {
            audioPlaybackManager.seekBy(AudioPlaybackManager.SEEK_INCREMENT_MS)
        }

        override fun onRewind() {
            audioPlaybackManager.seekBy(-AudioPlaybackManager.SEEK_INCREMENT_MS)
        }

        override fun onStop() {
            audioPlaybackManager.stopPlayback()
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppNotifications.ensureChannels(this)
        mediaSession = MediaSessionCompat(this, "AudioPlaybackService").apply {
            setCallback(mediaSessionCallback)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
        }
        stateCollectionJob = serviceScope.launch {
            audioPlaybackManager.state.collectLatest { state ->
                if (!state.hasQueue) {
                    updateMediaSession(state = state, contentIntent = null)
                    stopForegroundAndSelf()
                } else {
                    val contentIntent = buildContentIntent(state)
                    updateMediaSession(state = state, contentIntent = contentIntent)
                    val notification = buildNotification(
                        state = state,
                        contentIntent = contentIntent,
                    )
                    val notificationManager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (!startedInForeground) {
                        startForeground(AppNotifications.audioPlaybackNotificationId(), notification)
                        startedInForeground = true
                    } else {
                        notificationManager.notify(
                            AppNotifications.audioPlaybackNotificationId(),
                            notification,
                        )
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> audioPlaybackManager.togglePlayback()
            ACTION_PREVIOUS -> audioPlaybackManager.skipPrevious()
            ACTION_NEXT -> audioPlaybackManager.skipNext()
            ACTION_SEEK_BACK -> audioPlaybackManager.seekBy(-AudioPlaybackManager.SEEK_INCREMENT_MS)
            ACTION_SEEK_FORWARD -> audioPlaybackManager.seekBy(AudioPlaybackManager.SEEK_INCREMENT_MS)
            ACTION_STOP -> audioPlaybackManager.stopPlayback()
            ACTION_START -> Unit
            else -> Unit
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stateCollectionJob?.cancel()
        mediaSession.release()
        runCatching {
            cacheDir.listFiles { _, name -> name.startsWith("vault_play_") }?.forEach { it.delete() }
        }
        super.onDestroy()
    }

    private fun stopForegroundAndSelf() {
        mediaSession.isActive = false
        if (startedInForeground) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            startedInForeground = false
        }
        stopSelf()
    }

    private fun buildContentIntent(state: AudioPlaybackState): PendingIntent {
        return PendingIntent.getActivity(
            this,
            ROUTE_REQUEST_CODE,
            AppLaunchRouter.buildIntent(
                context = this,
                route = AppLaunchRouter.ROUTE_MUSIC,
                taskId = state.currentTaskId,
            ),
            pendingIntentFlags(updateCurrent = true),
        )
    }

    private fun buildNotification(
        state: AudioPlaybackState,
        contentIntent: PendingIntent,
    ): Notification {
        val mediaInfo = resolveMediaInfo(state)

        val builder = NotificationCompat.Builder(this, AppNotifications.CHANNEL_AUDIO_PLAYBACK)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(mediaInfo.title.ifBlank { "Audio playback" })
            .setContentText(
                mediaInfo.artist
                    ?: mediaInfo.album
                    ?: buildNotificationLine(state),
            )
            .setSubText(
                mediaInfo.album
                    ?: buildNotificationLine(state),
            )
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setContentIntent(contentIntent)
            .setDeleteIntent(commandPendingIntent(ACTION_STOP, DELETE_REQUEST_CODE))
            .setOnlyAlertOnce(true)
            .setOngoing(state.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setShowWhen(false)
            .setColorized(true)
            .setColor(0xFF78D8F3.toInt())
            .setProgress(
                state.durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                state.positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                state.durationMs <= 0L,
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                commandPendingIntent(ACTION_PREVIOUS, PREVIOUS_REQUEST_CODE),
            )
            .addAction(
                android.R.drawable.ic_media_rew,
                "-10s",
                commandPendingIntent(ACTION_SEEK_BACK, SEEK_BACK_REQUEST_CODE),
            )
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isPlaying) "Pause" else "Play",
                commandPendingIntent(ACTION_PLAY_PAUSE, PLAY_PAUSE_REQUEST_CODE),
            )
            .addAction(
                android.R.drawable.ic_media_ff,
                "+10s",
                commandPendingIntent(ACTION_SEEK_FORWARD, SEEK_FORWARD_REQUEST_CODE),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                commandPendingIntent(ACTION_NEXT, NEXT_REQUEST_CODE),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                commandPendingIntent(ACTION_STOP, STOP_REQUEST_CODE),
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 2, 4),
            )

        mediaInfo.artwork?.let(builder::setLargeIcon)
        return builder.build()
    }

    private fun commandPendingIntent(action: String, requestCode: Int) =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, AudioPlaybackService::class.java)
                .setAction(action)
                .setPackage(packageName),
            pendingIntentFlags(updateCurrent = true),
        )

    private fun pendingIntentFlags(updateCurrent: Boolean): Int {
        val base = if (updateCurrent) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            0
        }
        return base or android.app.PendingIntent.FLAG_IMMUTABLE
    }

    private fun buildNotificationLine(state: AudioPlaybackState): String {
        val details = buildList {
            if (state.currentTrackNumber > 0 && state.queueCount > 0) {
                add("Track ${state.currentTrackNumber}/${state.queueCount}")
            }
            if (state.shuffleEnabled) add("Shuffle")
            when (state.repeatMode) {
                PlaylistRepeatMode.ALL -> add("Repeat all")
                PlaylistRepeatMode.ONE -> add("Repeat one")
                PlaylistRepeatMode.OFF -> Unit
            }
            state.sleepTimerRemainingMs
                ?.takeIf { it > 0L }
                ?.let { remaining -> add("Sleep ${remaining.toReadableDuration()}") }
        }
        return details.joinToString(" | ").ifBlank { "Playback controls ready" }
    }

    private fun updateMediaSession(
        state: AudioPlaybackState,
        contentIntent: PendingIntent?,
    ) {
        val mediaInfo = resolveMediaInfo(state)
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP,
            )
            .setState(
                when {
                    !state.hasQueue -> PlaybackStateCompat.STATE_STOPPED
                    state.isBuffering -> PlaybackStateCompat.STATE_BUFFERING
                    state.isPlaying -> PlaybackStateCompat.STATE_PLAYING
                    else -> PlaybackStateCompat.STATE_PAUSED
                },
                state.positionMs,
                if (state.isPlaying) 1f else 0f,
            )
            .build()

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaInfo.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaInfo.artist.orEmpty())
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mediaInfo.album.orEmpty())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, state.durationMs)
            .apply {
                mediaInfo.artwork?.let {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
                }
            }
            .build()

        mediaSession.setPlaybackState(playbackState)
        mediaSession.setMetadata(metadata)
        if (contentIntent != null) {
            mediaSession.setSessionActivity(contentIntent)
        }
        mediaSession.isActive = state.hasQueue
    }

    private fun resolveMediaInfo(state: AudioPlaybackState): NotificationMediaInfo {
        val filePath = state.currentFilePath
        if (filePath.isNullOrBlank()) {
            return NotificationMediaInfo(
                title = state.currentTitle.ifBlank { "Audio playback" },
                artist = null,
                album = null,
                artwork = null,
            )
        }
        val cached = mediaInfoCache.getOrPut(filePath) {
            loadMediaInfo(filePath, state.currentTitle)
        }
        return cached.copy(
            title = state.currentTitle.ifBlank { cached.title },
        )
    }

    private fun loadMediaInfo(
        filePath: String,
        fallbackTitle: String,
    ): NotificationMediaInfo {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            if (filePath.startsWith("content://", ignoreCase = true)) {
                retriever.setDataSource(this, Uri.parse(filePath))
            } else {
                retriever.setDataSource(filePath)
            }
            NotificationMediaInfo(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }
                    ?: fallbackTitle.ifBlank { File(filePath).nameWithoutExtension },
                artist = listOf(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR),
                ).firstOrNull { !it.isNullOrBlank() },
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeIf { it.isNotBlank() },
                artwork = retriever.embeddedPicture?.let { artwork ->
                    BitmapFactory.decodeByteArray(artwork, 0, artwork.size)
                } ?: retriever.frameAtTime,
            )
        }.getOrElse {
            NotificationMediaInfo(
                title = fallbackTitle.ifBlank { File(filePath).nameWithoutExtension },
                artist = null,
                album = null,
                artwork = null,
            )
        }.also {
            runCatching { retriever.release() }
        }
    }

    private fun Long.toReadableDuration(): String {
        val totalMinutes = (this / 60_000L).coerceAtLeast(0L)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    private data class NotificationMediaInfo(
        val title: String,
        val artist: String?,
        val album: String?,
        val artwork: Bitmap?,
    )

    companion object {
        private const val ACTION_START = "com.localdownloader.audio.START"
        private const val ACTION_PLAY_PAUSE = "com.localdownloader.audio.PLAY_PAUSE"
        private const val ACTION_PREVIOUS = "com.localdownloader.audio.PREVIOUS"
        private const val ACTION_NEXT = "com.localdownloader.audio.NEXT"
        private const val ACTION_SEEK_BACK = "com.localdownloader.audio.SEEK_BACK"
        private const val ACTION_SEEK_FORWARD = "com.localdownloader.audio.SEEK_FORWARD"
        private const val ACTION_STOP = "com.localdownloader.audio.STOP"

        private const val ROUTE_REQUEST_CODE = 81_001
        private const val PREVIOUS_REQUEST_CODE = 81_002
        private const val SEEK_BACK_REQUEST_CODE = 81_003
        private const val PLAY_PAUSE_REQUEST_CODE = 81_004
        private const val SEEK_FORWARD_REQUEST_CODE = 81_005
        private const val NEXT_REQUEST_CODE = 81_006
        private const val STOP_REQUEST_CODE = 81_007
        private const val DELETE_REQUEST_CODE = 81_008

        fun start(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AudioPlaybackService::class.java))
        }
    }
}
