package com.localdownloader.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.localdownloader.audio.AudioPlaybackState
import com.localdownloader.audio.AudioQueueItem
import com.localdownloader.audio.PlaylistRepeatMode
import com.localdownloader.ui.components.LocalVideoThumbnail
import com.localdownloader.ui.model.MediaKind
import com.localdownloader.ui.model.VideoLibraryItem
import com.localdownloader.ui.model.buildVideoLibraryItems
import com.localdownloader.ui.model.formatMediaDate
import com.localdownloader.ui.model.formatPlaybackTime
import com.localdownloader.ui.model.label
import com.localdownloader.ui.model.toAudioQueueItems
import com.localdownloader.ui.model.toShortTimerLabel
import com.localdownloader.viewmodel.DownloadUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    uiState: DownloadUiState,
    audioPlaybackState: AudioPlaybackState,
    onPlayAudioQueue: (List<AudioQueueItem>, String?, Boolean) -> Unit,
    onToggleAudioPlayback: () -> Unit,
    onSeekAudioBy: (Long) -> Unit,
    onSeekAudioTo: (Long) -> Unit,
    onSkipToPreviousAudio: () -> Unit,
    onSkipToNextAudio: () -> Unit,
    onToggleAudioShuffle: () -> Unit,
    onCycleAudioRepeatMode: () -> Unit,
    onSetAudioSleepTimer: (Long?) -> Unit,
    onStopAudioPlayback: () -> Unit,
    onDismissAudioError: () -> Unit,
    favoriteTaskIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    fileExists: (String) -> Boolean = { path -> java.io.File(path).exists() },
) {
    val audioItems = remember(uiState.tasks, fileExists) {
        buildVideoLibraryItems(uiState.tasks, fileExists)
            .filter { it.exists && it.mediaKind == MediaKind.AUDIO }
    }
    val audioQueueItems = remember(audioItems) { audioItems.toAudioQueueItems() }
    val currentItem = audioItems.firstOrNull { it.task.id == audioPlaybackState.currentTaskId }
        ?: audioItems.firstOrNull()

    var isScrubbing by remember(audioPlaybackState.currentTaskId) { mutableStateOf(false) }
    var scrubPositionMs by remember(audioPlaybackState.currentTaskId) {
        mutableFloatStateOf(audioPlaybackState.positionMs.toFloat())
    }
    var activeSheet by remember { mutableStateOf<PlayerSheet?>(null) }
    var loopStartMs by remember(audioPlaybackState.currentTaskId) { mutableStateOf<Long?>(null) }
    var loopEndMs by remember(audioPlaybackState.currentTaskId) { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    LaunchedEffect(
        audioPlaybackState.currentTaskId,
        audioPlaybackState.positionMs,
        audioPlaybackState.durationMs,
        isScrubbing,
    ) {
        if (!isScrubbing) {
            scrubPositionMs = audioPlaybackState.positionMs.toFloat()
        }
    }

    LaunchedEffect(loopStartMs, loopEndMs, audioPlaybackState.positionMs, audioPlaybackState.isPlaying) {
        val startMs = loopStartMs
        val endMs = loopEndMs
        if (audioPlaybackState.isPlaying && startMs != null && endMs != null && audioPlaybackState.positionMs >= endMs) {
            onSeekAudioTo(startMs)
        }
    }

    val queueDisplayItems = remember(audioPlaybackState.queue, audioItems) {
        if (audioPlaybackState.queue.isEmpty()) {
            audioItems
        } else {
            val byTaskId = audioItems.associateBy { it.task.id }
            audioPlaybackState.queue.mapNotNull { queueItem -> byTaskId[queueItem.taskId] }
        }
    }
    val queueDisplayAudioItems = remember(queueDisplayItems) { queueDisplayItems.toAudioQueueItems() }

    if (activeSheet != null && currentItem != null) {
        when (activeSheet) {
            PlayerSheet.More -> PlayerOptionsSheet(
                item = currentItem,
                audioPlaybackState = audioPlaybackState,
                onDismiss = { activeSheet = null },
                onSetSleepTimer = { durationMs ->
                    onSetAudioSleepTimer(durationMs)
                    activeSheet = null
                },
                onStopPlayback = {
                    onStopAudioPlayback()
                    activeSheet = null
                },
            )
            PlayerSheet.Lyrics -> LyricsSheet(
                item = currentItem,
                onDismiss = { activeSheet = null },
            )
            PlayerSheet.Queue -> PlayingQueueSheet(
                audioItems = queueDisplayItems,
                audioPlaybackState = audioPlaybackState,
                onPlayTrack = { item ->
                    onPlayAudioQueue(queueDisplayAudioItems, item.task.id, audioPlaybackState.shuffleEnabled)
                    activeSheet = null
                },
                onDismiss = { activeSheet = null },
            )
            PlayerSheet.AudioTools -> AudioToolsSheet(
                audioPlaybackState = audioPlaybackState,
                loopStartMs = loopStartMs,
                loopEndMs = loopEndMs,
                onSetLoopStart = { loopStartMs = audioPlaybackState.positionMs },
                onSetLoopEnd = {
                    audioPlaybackState.positionMs
                        .takeIf { position -> loopStartMs?.let { position > it + 1_000L } == true }
                        ?.let { loopEndMs = it }
                },
                onClearLoop = {
                    loopStartMs = null
                    loopEndMs = null
                },
                onDismiss = { activeSheet = null },
            )
            null -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050709)),
    ) {
        currentItem?.let { item ->
            LocalVideoThumbnail(
                filePath = item.file?.absolutePath,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.54f),
                            Color(0xCC050709),
                            Color(0xFF050709),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (audioItems.isEmpty()) {
                EmptyMusicState(
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                )
                return@Column
            }

            NowPlayingDeck(
                item = currentItem,
                trackCount = audioItems.size,
                audioPlaybackState = audioPlaybackState,
                accentColor = MaterialTheme.colorScheme.primary,
                scrubPositionMs = scrubPositionMs,
                onScrubbingChanged = { isScrubbing = it },
                onScrubPositionChanged = { scrubPositionMs = it },
                onSeekAudioTo = onSeekAudioTo,
                onToggleAudioPlayback = {
                    if (audioPlaybackState.hasQueue) {
                        onToggleAudioPlayback()
                    } else {
                        onPlayAudioQueue(audioQueueItems, currentItem?.task?.id, false)
                    }
                },
                onSkipToPreviousAudio = onSkipToPreviousAudio,
                onSkipToNextAudio = onSkipToNextAudio,
                onToggleAudioShuffle = {
                    if (audioPlaybackState.hasQueue) {
                        onToggleAudioShuffle()
                    } else {
                        onPlayAudioQueue(audioQueueItems, null, true)
                    }
                },
                onCycleAudioRepeatMode = onCycleAudioRepeatMode,
                isFavorite = currentItem?.task?.id?.let { it in favoriteTaskIds } == true,
                isAbLoopActive = loopStartMs != null && loopEndMs != null,
                onToggleFavorite = {
                    currentItem?.task?.id?.let(onToggleFavorite)
                },
                onShare = {
                    currentItem?.file?.let { file -> shareAudioFile(context, file) }
                },
                onSetAbLoopPoint = {
                    if (loopStartMs == null) {
                        loopStartMs = audioPlaybackState.positionMs
                    } else if (loopEndMs == null && audioPlaybackState.positionMs > loopStartMs.orEmptyMs() + 1_000L) {
                        loopEndMs = audioPlaybackState.positionMs
                    } else {
                        loopStartMs = null
                        loopEndMs = null
                    }
                },
                onOpenAudioTools = { activeSheet = PlayerSheet.AudioTools },
                onOpenLyrics = { activeSheet = PlayerSheet.Lyrics },
                onOpenQueue = { activeSheet = PlayerSheet.Queue },
                onOptions = { activeSheet = PlayerSheet.More },
                onBack = onBack,
                onDismissAudioError = onDismissAudioError,
            )

            QueueSection(
                audioItems = queueDisplayItems,
                audioPlaybackState = audioPlaybackState,
                onPlayTrack = { item ->
                    onPlayAudioQueue(queueDisplayAudioItems, item.task.id, audioPlaybackState.shuffleEnabled)
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun NowPlayingDeck(
    item: VideoLibraryItem?,
    trackCount: Int,
    audioPlaybackState: AudioPlaybackState,
    accentColor: Color,
    scrubPositionMs: Float,
    onScrubbingChanged: (Boolean) -> Unit,
    onScrubPositionChanged: (Float) -> Unit,
    onSeekAudioTo: (Long) -> Unit,
    onToggleAudioPlayback: () -> Unit,
    onSkipToPreviousAudio: () -> Unit,
    onSkipToNextAudio: () -> Unit,
    onToggleAudioShuffle: () -> Unit,
    onCycleAudioRepeatMode: () -> Unit,
    isFavorite: Boolean,
    isAbLoopActive: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onSetAbLoopPoint: () -> Unit,
    onOpenAudioTools: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOptions: () -> Unit,
    onBack: () -> Unit,
    onDismissAudioError: () -> Unit,
) {
    val title = item?.displayTitle ?: "Downloaded music"
    val artist = item?.file?.parentFile?.name?.ifBlank { null }
        ?: item?.mediaKind?.label
        ?: "Audio library"
    val trackLabel = audioPlaybackState.currentTaskId?.let {
        "Track ${audioPlaybackState.currentTrackNumber}/${audioPlaybackState.queueCount.coerceAtLeast(trackCount)}"
    } ?: "$trackCount downloaded tracks"

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        tonalElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xDB070B0B)),
        ) {
            item?.let {
                LocalVideoThumbnail(
                    filePath = it.file?.absolutePath,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(34.dp),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.30f),
                                Color(0xE8070B0B),
                                Color(0xF7070808),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.44f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close music player",
                            tint = Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share current track",
                            tint = Color.White,
                        )
                    }
                }

                VinylArtwork(
                    item = item,
                    isPlaying = audioPlaybackState.isPlaying,
                    accentColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .aspectRatio(1f),
                )

                Text(
                    text = trackLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.68f),
                )

                PlayerUtilityRow(
                    repeatMode = audioPlaybackState.repeatMode,
                    accentColor = accentColor,
                    isFavorite = isFavorite,
                    isAbLoopActive = isAbLoopActive,
                    onShuffle = onToggleAudioShuffle,
                    onRepeat = onCycleAudioRepeatMode,
                    onFavorite = onToggleFavorite,
                    onAbLoop = onSetAbLoopPoint,
                    onAudioTools = onOpenAudioTools,
                    onOptions = onOptions,
                )

                if (!audioPlaybackState.errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = audioPlaybackState.errorMessage,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(onClick = onDismissAudioError) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }

                PremiumProgressSlider(
                    positionMs = scrubPositionMs,
                    durationMs = audioPlaybackState.durationMs,
                    accentColor = accentColor,
                    onPositionChanged = { updated ->
                        onScrubbingChanged(true)
                        onScrubPositionChanged(updated)
                    },
                    onPositionChangeFinished = { targetPositionMs ->
                        onScrubbingChanged(false)
                        onScrubPositionChanged(targetPositionMs)
                        onSeekAudioTo(targetPositionMs.toLong())
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatPlaybackTime(scrubPositionMs.toLong()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                    Text(
                        text = formatPlaybackTime(audioPlaybackState.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerIconButton(
                        icon = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle",
                        selected = audioPlaybackState.shuffleEnabled,
                        accentColor = accentColor,
                        onClick = onToggleAudioShuffle,
                    )
                    PlayerIconButton(
                        icon = Icons.Outlined.SkipPrevious,
                        contentDescription = "Previous",
                        accentColor = accentColor,
                        onClick = onSkipToPreviousAudio,
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.92f)),
                        modifier = Modifier.size(82.dp),
                    ) {
                        IconButton(onClick = onToggleAudioPlayback, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (audioPlaybackState.isPlaying) {
                                    Icons.Outlined.PauseCircle
                                } else {
                                    Icons.Outlined.PlayCircle
                                },
                                contentDescription = if (audioPlaybackState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp),
                            )
                        }
                    }
                    PlayerIconButton(
                        icon = Icons.Outlined.SkipNext,
                        contentDescription = "Next",
                        accentColor = accentColor,
                        onClick = onSkipToNextAudio,
                    )
                    PlayerIconButton(
                        icon = repeatModeIcon(audioPlaybackState.repeatMode),
                        contentDescription = repeatModeLabel(audioPlaybackState.repeatMode),
                        selected = audioPlaybackState.repeatMode != PlaylistRepeatMode.OFF,
                        accentColor = accentColor,
                        onClick = onCycleAudioRepeatMode,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FooterAction(
                        icon = Icons.Outlined.GraphicEq,
                        label = "Lyrics",
                        onClick = onOpenLyrics,
                    )
                    FooterAction(
                        icon = Icons.Outlined.QueueMusic,
                        label = "Playing queue",
                        onClick = onOpenQueue,
                    )
                }
            }
        }
    }
}

@Composable
private fun VinylArtwork(
    item: VideoLibraryItem?,
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val recordRotation = remember { Animatable(0f) }
    val tonearmProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = LinearEasing),
        label = "tonearmProgress",
    )

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                recordRotation.animateTo(
                    targetValue = recordRotation.value + 360f,
                    animationSpec = tween(durationMillis = 9000, easing = LinearEasing),
                )
                recordRotation.snapTo(recordRotation.value % 360f)
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height * 0.56f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius * 1.15f,
                ),
                radius = radius * 1.05f,
                center = center,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .align(Alignment.BottomCenter)
                .rotate(recordRotation.value),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Color(0xFF111414), radius = radius, center = center)
                drawCircle(Color(0xFF050505), radius = radius * 0.88f, center = center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.58f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius * 0.88f,
                    center = center,
                )
                repeat(8) { index ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.038f),
                        radius = radius * (0.3f + index * 0.07f),
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
                drawCircle(Color(0xFF1D2020), radius = radius * 0.2f, center = center)
            }

            LocalVideoThumbnail(
                filePath = item?.file?.absolutePath,
                contentDescription = item?.displayTitle,
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .aspectRatio(1f)
                    .border(2.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .clip(CircleShape),
                fallbackContent = {
                    DefaultRecordArtwork(
                        accentColor = accentColor,
                        contentDescription = item?.displayTitle,
                    )
                },
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val base = Offset(size.width * 0.80f, size.height * 0.13f)
            fun lerp(start: Float, end: Float): Float = start + (end - start) * tonearmProgress

            val elbow = Offset(
                x = lerp(size.width * 0.90f, size.width * 0.72f),
                y = lerp(size.height * 0.31f, size.height * 0.29f),
            )
            val needle = Offset(
                x = lerp(size.width * 0.91f, size.width * 0.68f),
                y = lerp(size.height * 0.54f, size.height * 0.39f),
            )
            val cartridgeEnd = Offset(
                x = needle.x + size.width * 0.052f,
                y = needle.y + size.height * 0.032f,
            )
            drawCircle(Color.White.copy(alpha = 0.86f), radius = radius * 0.074f, center = base)
            drawCircle(Color(0xFF6D6F73), radius = radius * 0.044f, center = base)
            drawLine(
                color = Color.White.copy(alpha = 0.82f),
                start = base,
                end = elbow,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.82f),
                start = elbow,
                end = needle,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFFE8EAEE),
                start = needle,
                end = cartridgeEnd,
                strokeWidth = 9.dp.toPx(),
                cap = StrokeCap.Square,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.62f),
                start = cartridgeEnd,
                end = Offset(cartridgeEnd.x + size.width * 0.014f, cartridgeEnd.y + size.height * 0.016f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DefaultRecordArtwork(
    accentColor: Color,
    contentDescription: String?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        accentColor.copy(alpha = 0.34f),
                        Color(0xFF241E2F),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = radius * 0.96f,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.18f),
                radius = radius * 0.30f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.34f)),
            modifier = Modifier.size(58.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(34.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerUtilityRow(
    repeatMode: PlaylistRepeatMode,
    accentColor: Color,
    isFavorite: Boolean,
    isAbLoopActive: Boolean,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onAbLoop: () -> Unit,
    onAudioTools: () -> Unit,
    onOptions: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerIconButton(
            icon = Icons.Outlined.GraphicEq,
            contentDescription = "Equalizer",
            accentColor = accentColor,
            onClick = onAudioTools,
        )
        UtilityTextButton(
            label = "A-B",
            selected = isAbLoopActive,
            accentColor = accentColor,
            onClick = onAbLoop,
        )
        PlayerIconButton(
            icon = repeatModeIcon(repeatMode),
            contentDescription = repeatModeLabel(repeatMode),
            selected = repeatMode != PlaylistRepeatMode.OFF,
            accentColor = accentColor,
            onClick = onRepeat,
        )
        PlayerIconButton(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            selected = isFavorite,
            accentColor = accentColor,
            onClick = onFavorite,
        )
        PlayerIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "More playback options",
            accentColor = accentColor,
            onClick = onOptions,
        )
    }
}

@Composable
private fun PremiumProgressSlider(
    positionMs: Float,
    durationMs: Long,
    accentColor: Color,
    onPositionChanged: (Float) -> Unit,
    onPositionChangeFinished: (Float) -> Unit,
) {
    val durationRange = durationMs.toFloat().coerceAtLeast(1f)
    val clampedPosition = positionMs.coerceIn(0f, durationRange)
    var latestSeekPosition by remember(durationMs) { mutableFloatStateOf(clampedPosition) }
    val progress = clampedPosition / durationRange

    LaunchedEffect(clampedPosition) {
        latestSeekPosition = clampedPosition
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val sidePadding = 4.dp.toPx()
            val startX = sidePadding
            val endX = size.width - sidePadding
            val activeEndX = startX + (endX - startX) * progress

            drawLine(
                color = Color.White.copy(alpha = 0.20f),
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.78f),
                        accentColor,
                    ),
                    startX = startX,
                    endX = activeEndX.coerceAtLeast(startX + 1f),
                ),
                start = Offset(startX, centerY),
                end = Offset(activeEndX, centerY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.24f),
                radius = 8.dp.toPx(),
                center = Offset(activeEndX, centerY),
            )
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(activeEndX, centerY),
            )
        }

        Slider(
            value = clampedPosition,
            onValueChange = { updated ->
                latestSeekPosition = updated
                onPositionChanged(updated)
            },
            onValueChangeFinished = {
                onPositionChangeFinished(latestSeekPosition)
            },
            valueRange = 0f..durationRange,
            enabled = durationMs > 0L,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0f),
        )
    }
}

@Composable
private fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    accentColor: Color = Color(0xFF56A9FF),
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(46.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) accentColor else Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun UtilityTextButton(
    label: String,
    selected: Boolean = false,
    accentColor: Color = Color(0xFF56A9FF),
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) accentColor else Color.White,
        )
    }
}

@Composable
private fun FooterAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerOptionsSheet(
    item: VideoLibraryItem,
    audioPlaybackState: AudioPlaybackState,
    onDismiss: () -> Unit,
    onSetSleepTimer: (Long?) -> Unit,
    onStopPlayback: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LocalVideoThumbnail(
                    filePath = item.file?.absolutePath,
                    contentDescription = item.displayTitle,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.file?.parentFile?.name?.ifBlank { null }
                            ?: item.mediaKind.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

            listOf(15, 30, 45, 60).forEach { minutes ->
                PlayerOptionRow(
                    icon = Icons.Outlined.AccessTime,
                    title = "Sleep after ${minutes}m",
                    subtitle = "Pause playback automatically after ${minutes} minutes.",
                    onClick = { onSetSleepTimer(minutes * 60_000L) },
                )
            }
            if (audioPlaybackState.sleepTimerRemainingMs != null) {
                PlayerOptionRow(
                    icon = Icons.Outlined.Close,
                    title = "Sleep off",
                    subtitle = "Current timer: ${audioPlaybackState.sleepTimerRemainingMs.toShortTimerLabel()} remaining.",
                    onClick = { onSetSleepTimer(null) },
                )
            }
            PlayerOptionRow(
                icon = Icons.Outlined.PauseCircle,
                title = "Stop playback",
                subtitle = "Clear the queue and stop the music session.",
                onClick = onStopPlayback,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSheet(
    item: VideoLibraryItem,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetTrackHeader(item = item)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "No synced lyrics are attached to this local audio yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayingQueueSheet(
    audioItems: List<VideoLibraryItem>,
    audioPlaybackState: AudioPlaybackState,
    onPlayTrack: (VideoLibraryItem) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Playing queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (audioPlaybackState.shuffleEnabled) "Shuffled" else "${audioItems.size} tracks",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            audioItems.forEach { item ->
                QueueTrackRow(
                    item = item,
                    isActive = item.task.id == audioPlaybackState.currentTaskId,
                    isPlaying = item.task.id == audioPlaybackState.currentTaskId && audioPlaybackState.isPlaying,
                    onClick = { onPlayTrack(item) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioToolsSheet(
    audioPlaybackState: AudioPlaybackState,
    loopStartMs: Long?,
    loopEndMs: Long?,
    onSetLoopStart: () -> Unit,
    onSetLoopEnd: () -> Unit,
    onClearLoop: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Audio tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            PlayerOptionRow(
                icon = Icons.Outlined.GraphicEq,
                title = "Playback status",
                subtitle = listOf(
                    if (audioPlaybackState.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    repeatModeLabel(audioPlaybackState.repeatMode),
                    "${formatPlaybackTime(audioPlaybackState.positionMs)} / ${formatPlaybackTime(audioPlaybackState.durationMs)}",
                ).joinToString(" | "),
                onClick = {},
            )
            PlayerOptionRow(
                icon = Icons.Outlined.AccessTime,
                title = "Set A point",
                subtitle = loopStartMs?.let { "A is ${formatPlaybackTime(it)}." }
                    ?: "Use the current playback time as loop start.",
                onClick = onSetLoopStart,
            )
            PlayerOptionRow(
                icon = Icons.Outlined.AccessTime,
                title = "Set B point",
                subtitle = loopEndMs?.let { "B is ${formatPlaybackTime(it)}." }
                    ?: "Use the current playback time as loop end after A.",
                onClick = onSetLoopEnd,
            )
            if (loopStartMs != null || loopEndMs != null) {
                PlayerOptionRow(
                    icon = Icons.Outlined.Close,
                    title = "Clear A-B loop",
                    subtitle = "Return to normal playback.",
                    onClick = onClearLoop,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetTrackHeader(item: VideoLibraryItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalVideoThumbnail(
            filePath = item.file?.absolutePath,
            contentDescription = item.displayTitle,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.file?.parentFile?.name?.ifBlank { null }
                    ?: item.mediaKind.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QueueSection(
    audioItems: List<VideoLibraryItem>,
    audioPlaybackState: AudioPlaybackState,
    onPlayTrack: (VideoLibraryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Playing queue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = audioItems.size.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        audioItems.forEach { item ->
            QueueTrackRow(
                item = item,
                isActive = item.task.id == audioPlaybackState.currentTaskId,
                isPlaying = item.task.id == audioPlaybackState.currentTaskId && audioPlaybackState.isPlaying,
                onClick = { onPlayTrack(item) },
            )
        }
    }
}

@Composable
private fun QueueTrackRow(
    item: VideoLibraryItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocalVideoThumbnail(
                filePath = item.file?.absolutePath,
                contentDescription = item.displayTitle,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOf(item.displaySize.ifBlank { "--" }, formatMediaDate(item.task.updatedAtEpochMs))
                        .joinToString(" | "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) {
                        Color.White.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = if (isActive) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun EmptyMusicState(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
            )
        }
        Spacer(Modifier.height(40.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(62.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "No downloaded songs yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Download audio from Home first, then this player will show your local queue.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun repeatModeLabel(mode: PlaylistRepeatMode): String {
    return when (mode) {
        PlaylistRepeatMode.OFF -> "Repeat off"
        PlaylistRepeatMode.ALL -> "Repeat all"
        PlaylistRepeatMode.ONE -> "Repeat one"
    }
}

private fun repeatModeIcon(mode: PlaylistRepeatMode): ImageVector {
    return when (mode) {
        PlaylistRepeatMode.ONE -> Icons.Outlined.RepeatOne
        PlaylistRepeatMode.ALL, PlaylistRepeatMode.OFF -> Icons.Outlined.Repeat
    }
}

private enum class PlayerSheet {
    More,
    Lyrics,
    Queue,
    AudioTools,
}

private fun Long?.orEmptyMs(): Long = this ?: 0L

private fun shareAudioFile(context: android.content.Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share audio"))
    }.onFailure {
        Toast.makeText(context, "Unable to share this audio file.", Toast.LENGTH_SHORT).show()
    }
}
