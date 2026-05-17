package com.localdownloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val audioItems = remember(uiState.tasks) {
        buildVideoLibraryItems(uiState.tasks)
            .filter { it.exists && it.mediaKind == MediaKind.AUDIO }
    }
    val audioQueueItems = remember(audioItems) { audioItems.toAudioQueueItems() }
    val currentItem = audioItems.firstOrNull { it.task.id == audioPlaybackState.currentTaskId }
        ?: audioItems.firstOrNull()

    var isScrubbing by remember(audioPlaybackState.currentTaskId) { mutableStateOf(false) }
    var scrubPositionMs by remember(audioPlaybackState.currentTaskId) {
        mutableFloatStateOf(audioPlaybackState.positionMs.toFloat())
    }
    var showOptionsSheet by remember { mutableStateOf(false) }

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

    if (showOptionsSheet && currentItem != null) {
        PlayerOptionsSheet(
            item = currentItem,
            audioPlaybackState = audioPlaybackState,
            onDismiss = { showOptionsSheet = false },
            onToggleShuffle = {
                onToggleAudioShuffle()
                showOptionsSheet = false
            },
            onCycleRepeat = {
                onCycleAudioRepeatMode()
                showOptionsSheet = false
            },
            onSetSleepTimer = { durationMs ->
                onSetAudioSleepTimer(durationMs)
                showOptionsSheet = false
            },
            onStopPlayback = {
                onStopAudioPlayback()
                showOptionsSheet = false
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        if (audioItems.isEmpty()) {
            EmptyMusicState(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            )
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MusicTopBar(
                title = if (audioPlaybackState.hasQueue) "Now playing" else "Downloaded music",
                queueCount = audioItems.size,
                onBack = onBack,
            )

            Surface(
                shape = RoundedCornerShape(36.dp),
                color = Color.Black.copy(alpha = 0.16f),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LocalVideoThumbnail(
                        filePath = currentItem?.file?.absolutePath,
                        contentDescription = currentItem?.displayTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .clip(RoundedCornerShape(28.dp)),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = audioPlaybackState.currentTaskId?.let {
                                        "Track ${audioPlaybackState.currentTrackNumber}/${audioPlaybackState.queueCount.coerceAtLeast(audioItems.size)}"
                                    } ?: "${audioItems.size} downloaded tracks",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }

                    playbackModesOverview(audioPlaybackState)
                        .takeIf { it.isNotBlank() }
                        ?.let { summary ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                            ) {
                                Text(
                                    text = summary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = currentItem?.displayTitle ?: "Downloaded music",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = currentItem?.file?.parentFile?.name?.ifBlank { null }
                                    ?: currentItem?.mediaKind?.label
                                    ?: "Audio library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { showOptionsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More playback options",
                            )
                        }
                    }

                    if (!audioPlaybackState.errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = audioPlaybackState.errorMessage,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
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

                    if (!audioPlaybackState.hasQueue) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    onPlayAudioQueue(audioQueueItems, audioQueueItems.firstOrNull()?.taskId, false)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.PlayCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Play all")
                            }
                            OutlinedButton(
                                onClick = {
                                    onPlayAudioQueue(audioQueueItems, null, true)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.Shuffle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Shuffle")
                            }
                        }
                    }

                    Slider(
                        value = scrubPositionMs.coerceIn(
                            0f,
                            audioPlaybackState.durationMs.toFloat().coerceAtLeast(0f),
                        ),
                        onValueChange = { updated ->
                            isScrubbing = true
                            scrubPositionMs = updated
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            onSeekAudioTo(scrubPositionMs.toLong())
                        },
                        valueRange = 0f..audioPlaybackState.durationMs.toFloat().coerceAtLeast(0f),
                        enabled = audioPlaybackState.durationMs > 0L,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatPlaybackTime(scrubPositionMs.toLong()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatPlaybackTime(audioPlaybackState.durationMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiniControlButton(
                            icon = Icons.Outlined.Replay10,
                            label = "-10",
                            onClick = { onSeekAudioBy(-10_000L) },
                        )
                        IconButton(onClick = onSkipToPreviousAudio, modifier = Modifier.size(60.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(102.dp),
                        ) {
                            IconButton(onClick = onToggleAudioPlayback, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = if (audioPlaybackState.isPlaying) {
                                        Icons.Outlined.PauseCircle
                                    } else {
                                        Icons.Outlined.PlayCircle
                                    },
                                    contentDescription = if (audioPlaybackState.isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(60.dp),
                                )
                            }
                        }
                        IconButton(onClick = onSkipToNextAudio, modifier = Modifier.size(60.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(34.dp),
                            )
                        }
                        MiniControlButton(
                            icon = Icons.Outlined.Forward10,
                            label = "+10",
                            onClick = { onSeekAudioBy(10_000L) },
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Up next",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    audioItems.forEach { item ->
                        QueueTrackRow(
                            item = item,
                            isActive = item.task.id == audioPlaybackState.currentTaskId,
                            isPlaying = item.task.id == audioPlaybackState.currentTaskId && audioPlaybackState.isPlaying,
                            onClick = { onPlayAudioQueue(audioQueueItems, item.task.id, false) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicTopBar(
    title: String,
    queueCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(text = queueCount.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerOptionsSheet(
    item: VideoLibraryItem,
    audioPlaybackState: AudioPlaybackState,
    onDismiss: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
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
                        .clip(RoundedCornerShape(20.dp)),
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

            PlayerOptionRow(
                icon = Icons.Outlined.Shuffle,
                title = if (audioPlaybackState.shuffleEnabled) "Turn shuffle off" else "Turn shuffle on",
                subtitle = if (audioPlaybackState.shuffleEnabled) {
                    "Play tracks in their normal order."
                } else {
                    "Mix the queue into a less predictable order."
                },
                onClick = onToggleShuffle,
            )
            PlayerOptionRow(
                icon = repeatModeIcon(audioPlaybackState.repeatMode),
                title = repeatModeLabel(audioPlaybackState.repeatMode),
                subtitle = "Tap to cycle between off, repeat all, and repeat one.",
                onClick = onCycleRepeat,
            )
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

@Composable
private fun PlayerOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun QueueTrackRow(
    item: VideoLibraryItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surface
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
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp)),
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
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = listOf(item.displaySize.ifBlank { "--" }, formatMediaDate(item.task.updatedAtEpochMs))
                        .joinToString(" | "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun MiniControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            shape = RoundedCornerShape(34.dp),
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
                    text = "Download audio from Home first, then this music player will turn into your local listening lounge.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun repeatModeIcon(mode: PlaylistRepeatMode): androidx.compose.ui.graphics.vector.ImageVector {
    return when (mode) {
        PlaylistRepeatMode.ONE -> Icons.Outlined.RepeatOne
        PlaylistRepeatMode.ALL, PlaylistRepeatMode.OFF -> Icons.Outlined.Repeat
    }
}

private fun playbackModesOverview(state: AudioPlaybackState): String {
    val labels = buildList {
        if (state.shuffleEnabled) add("Shuffle on")
        if (state.repeatMode != PlaylistRepeatMode.OFF) add(repeatModeLabel(state.repeatMode))
        state.sleepTimerRemainingMs?.takeIf { it > 0L }?.let { add("Sleep ${it.toShortTimerLabel()}") }
    }
    return labels.joinToString(" | ")
}
