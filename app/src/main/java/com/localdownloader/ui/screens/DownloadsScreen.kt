package com.localdownloader.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.localdownloader.audio.AudioPlaybackState
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.ui.components.LocalVideoThumbnail
import com.localdownloader.ui.model.MediaKind
import com.localdownloader.ui.model.VideoLibraryItem
import com.localdownloader.ui.model.buildVideoLibraryItems
import com.localdownloader.ui.model.formatMediaDate
import com.localdownloader.ui.model.label
import com.localdownloader.viewmodel.DownloadUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(
    uiState: DownloadUiState,
    audioPlaybackState: AudioPlaybackState,
    onOpenMusic: () -> Unit,
    onPlayMusic: (String?, Boolean) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onDismissAudioError: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(DownloadsFilter.All.name) }
    var sortNewestFirst by rememberSaveable { mutableStateOf(true) }
    var renameTarget by remember { mutableStateOf<VideoLibraryItem?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<VideoLibraryItem?>(null) }
    val context = LocalContext.current

    val items = remember(uiState.tasks) { buildVideoLibraryItems(uiState.tasks) }
    val audioItems = remember(items) { items.filter { it.exists && it.mediaKind == MediaKind.AUDIO } }
    val currentFilter = runCatching { DownloadsFilter.valueOf(selectedFilter) }
        .getOrDefault(DownloadsFilter.All)
    val filteredItems = items
        .filter { currentFilter.matches(it.mediaKind) }
        .let { candidates ->
            if (sortNewestFirst) {
                candidates.sortedByDescending { it.task.updatedAtEpochMs }
            } else {
                candidates.sortedBy { it.task.updatedAtEpochMs }
            }
        }

    val activeDownloadsCount = uiState.tasks.count {
        it.status == DownloadStatus.RUNNING ||
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.PAUSED
    }
    val hasActiveDownloads = activeDownloadsCount > 0

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename file") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTarget?.let { onRename(it.task.id, renameValue.trim()) }
                        renameTarget = null
                    },
                    enabled = renameValue.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete file") },
            text = {
                Text(
                    if (uiState.deleteFromStorageWhenRemovedInApp) {
                        "This removes the saved media item from the library and deletes the device file when it still exists."
                    } else {
                        "This removes the saved media item from the app library but leaves the real device file untouched."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget?.let { onDelete(it.task.id) }
                        deleteTarget = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f,
                ),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = when {
                        items.isNotEmpty() -> {
                            "${items.size} saved ${if (items.size == 1) "item" else "items"} ready to open"
                        }
                        hasActiveDownloads -> {
                            "$activeDownloadsCount download${if (activeDownloadsCount == 1) " is" else "s are"} in progress. Finished items will appear here automatically."
                        }
                        else -> {
                            "Completed downloads show up here once they are saved."
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                )
            }
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QueueActionButton(
                    isActive = hasActiveDownloads,
                    activeCount = activeDownloadsCount,
                    onClick = onOpenQueue,
                )
                IconButton(onClick = onOpenMore) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Open more options",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        if (items.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = sortNewestFirst,
                    onClick = { sortNewestFirst = !sortNewestFirst },
                    label = { Text(if (sortNewestFirst) "Newest first" else "Oldest first") },
                )
                DownloadsFilter.entries.filter { it != DownloadsFilter.All }.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = {
                            selectedFilter = if (currentFilter == filter) {
                                DownloadsFilter.All.name
                            } else {
                                filter.name
                            }
                        },
                        label = { Text(filter.label) },
                    )
                }
            }
        }

        if (audioItems.isNotEmpty()) {
            MusicLaunchButton(
                trackCount = audioItems.size,
                currentTitle = audioPlaybackState.currentTitle.takeIf { audioPlaybackState.hasQueue },
                isPlaying = audioPlaybackState.isPlaying,
                onClick = {
                    if (audioPlaybackState.hasQueue) {
                        onOpenMusic()
                    } else {
                        onPlayMusic(audioItems.firstOrNull()?.task?.id, false)
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = !uiState.infoMessage.isNullOrBlank(),
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        ) {
            uiState.infoMessage?.let { message ->
                MessageBanner(
                    message = message,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onDismiss = onDismissMessage,
                )
            }
        }

        AnimatedVisibility(
            visible = !uiState.errorMessage.isNullOrBlank() || !audioPlaybackState.errorMessage.isNullOrBlank(),
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        ) {
            MessageBanner(
                message = audioPlaybackState.errorMessage ?: uiState.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.errorContainer,
                textColor = MaterialTheme.colorScheme.onErrorContainer,
                onDismiss = {
                    onDismissMessage()
                    onDismissAudioError()
                },
            )
        }

        AnimatedVisibility(
            visible = filteredItems.isEmpty(),
            enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                expandVertically(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 0.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = when (currentFilter) {
                        DownloadsFilter.All -> {
                            if (hasActiveDownloads) {
                                "Downloads are still running. Completed files will show up here automatically."
                            } else {
                                "No downloads yet. Finish one from Home and it will show up here automatically."
                            }
                        }
                        else -> {
                            if (hasActiveDownloads) {
                                "No ${currentFilter.label.lowercase()} files are finished yet. Your active downloads are still moving through the queue."
                            } else {
                                "No ${currentFilter.label.lowercase()} downloads yet. Finish one from Home and it will show up here automatically."
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(22.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = filteredItems.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                expandVertically(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(filteredItems, key = { it.task.id }) { item ->
                    DownloadHeroCard(
                        item = item,
                        isPlayingNow = item.task.id == audioPlaybackState.currentTaskId && audioPlaybackState.isPlaying,
                        onPrimaryPlay = {
                            when (item.mediaKind) {
                                MediaKind.VIDEO -> onOpenPlayer(item.task.id)
                                MediaKind.AUDIO -> {
                                    if (item.task.id == audioPlaybackState.currentTaskId && audioPlaybackState.hasQueue) {
                                        onOpenMusic()
                                    } else {
                                        onPlayMusic(item.task.id, false)
                                    }
                                }
                                MediaKind.OTHER -> Unit
                            }
                        },
                        onShare = {
                            item.file?.takeIf { file -> file.exists() }?.let { file ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file,
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, "Share media"),
                                )
                            }
                        },
                        onRename = {
                            renameTarget = item
                            renameValue = item.file?.nameWithoutExtension ?: item.displayTitle
                        },
                        onDelete = { deleteTarget = item },
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicLaunchButton(
    trackCount: Int,
    currentTitle: String?,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            currentTitle?.let {
                if (isPlaying) "Open music player" else "Resume music player"
            } ?: "Play music ($trackCount)",
        )
    }
}

@Composable
private fun MessageBanner(
    message: String,
    color: Color,
    textColor: Color,
    onDismiss: () -> Unit,
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = textColor,
            )
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}

@Composable
private fun QueueActionButton(
    isActive: Boolean,
    activeCount: Int,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "queueButton")
    val iconLift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) -2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "queueIconLift",
    )
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "queuePulse",
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = if (isActive) 3.dp else 0.dp,
        modifier = Modifier
            .padding(end = 2.dp)
            .widthIn(min = 104.dp, max = 132.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = "Open download queue",
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .offset { IntOffset(0, iconLift.dp.roundToPx()) }
                        .size((24.dp * pulseScale)),
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(if (activeCount > 9) 18.dp else 14.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(999.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (activeCount > 1) {
                            Text(
                                text = if (activeCount > 9) "9+" else activeCount.toString(),
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
            Text(
                text = "Queue",
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun DownloadHeroCard(
    item: VideoLibraryItem,
    isPlayingNow: Boolean,
    onPrimaryPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp),
        ) {
            LocalVideoThumbnail(
                filePath = item.file?.absolutePath,
                contentDescription = item.displayTitle,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f)),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = item.displayTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(
                                item.displaySize.ifBlank { null },
                                formatMediaDate(item.task.updatedAtEpochMs),
                            ).joinToString(" | "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Download actions",
                                tint = Color.White,
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            if (item.exists && item.mediaKind != MediaKind.OTHER) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Text(
                                            when {
                                                item.mediaKind == MediaKind.AUDIO && isPlayingNow -> "Open music player"
                                                item.mediaKind == MediaKind.AUDIO -> "Play in music player"
                                                else -> "Play"
                                            },
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (item.mediaKind == MediaKind.AUDIO && isPlayingNow) {
                                                Icons.Outlined.PauseCircle
                                            } else {
                                                Icons.Outlined.PlayCircle
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onPrimaryPlay()
                                    },
                                )
                            }
                            if (item.exists) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onShare()
                                    },
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onRename()
                                    },
                                )
                            }
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(if (item.exists) "Delete" else "Remove entry") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPlayingNow) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            } else {
                                Color.Black.copy(alpha = 0.28f)
                            },
                        ) {
                            Text(
                                text = when {
                                    !item.exists -> "File unavailable"
                                    isPlayingNow -> "Playing now"
                                    else -> item.mediaKind.label
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                        Text(
                            text = formatMediaDate(item.task.updatedAtEpochMs),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    ) {
                        IconButton(
                            onClick = onPrimaryPlay,
                            enabled = item.exists && item.mediaKind != MediaKind.OTHER,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Icon(
                                imageVector = if (item.mediaKind == MediaKind.AUDIO && isPlayingNow) {
                                    Icons.Outlined.PauseCircle
                                } else {
                                    Icons.Outlined.PlayCircle
                                },
                                contentDescription = "Open media",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class DownloadsFilter(val label: String) {
    All("All"),
    Audio("Audio"),
    Video("Video"),
    Other("Other");

    fun matches(kind: MediaKind): Boolean {
        return when (this) {
            All -> true
            Audio -> kind == MediaKind.AUDIO
            Video -> kind == MediaKind.VIDEO
            Other -> kind == MediaKind.OTHER
        }
    }
}
