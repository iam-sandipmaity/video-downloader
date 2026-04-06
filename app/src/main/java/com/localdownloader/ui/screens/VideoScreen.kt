package com.localdownloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.ui.components.LocalVideoThumbnail
import com.localdownloader.ui.model.MediaKind
import com.localdownloader.ui.model.VideoLibraryItem
import com.localdownloader.viewmodel.DownloadUiState
import java.io.File
import kotlin.math.roundToInt

@Composable
fun VideoScreen(
    uiState: DownloadUiState,
    onOpenPlayer: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val audioPlayer = remember(context) { ExoPlayer.Builder(context).build() }
    var selectedFilter by rememberSaveable { mutableStateOf(MediaFilter.All.name) }
    var activeAudioTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }

    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isAudioPlaying = isPlayingNow
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    activeAudioTaskId = null
                    isAudioPlaying = false
                }
            }
        }
        audioPlayer.addListener(listener)
        onDispose {
            audioPlayer.removeListener(listener)
            audioPlayer.release()
        }
    }

    val items = uiState.tasks
        .filter { it.status == DownloadStatus.COMPLETED }
        .sortedByDescending { it.updatedAtEpochMs }
        .map { task ->
            val file = task.outputPath?.let(::File)
            val resolvedSize = file
                ?.takeIf { it.exists() }
                ?.length()
                ?.toReadableSize()
                ?.takeIf { it.isNotBlank() }
                ?: task.totalSizeStr.meaningfulSizeLabel()
                ?: task.downloadedStr.meaningfulSizeLabel()
                ?: ""
            VideoLibraryItem(
                task = task,
                file = file,
                displayTitle = task.title.ifBlank { file?.name ?: "Saved video" },
                displaySize = resolvedSize,
                exists = file?.exists() == true,
                mediaKind = resolveMediaKind(file),
            )
        }

    val currentFilter = runCatching { MediaFilter.valueOf(selectedFilter) }.getOrDefault(MediaFilter.All)
    val filteredItems = items.filter { item ->
        when (currentFilter) {
            MediaFilter.All -> true
            MediaFilter.Video -> item.mediaKind == MediaKind.VIDEO
            MediaFilter.Audio -> item.mediaKind == MediaKind.AUDIO
            MediaFilter.Other -> item.mediaKind == MediaKind.OTHER
        }
    }

    var renameTarget by remember { mutableStateOf<VideoLibraryItem?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<VideoLibraryItem?>(null) }

    fun handlePrimaryPlay(item: VideoLibraryItem) {
        when (item.mediaKind) {
            MediaKind.VIDEO -> onOpenPlayer(item.task.id)
            MediaKind.AUDIO -> {
                val targetFile = item.file?.takeIf { it.exists() } ?: return
                if (activeAudioTaskId == item.task.id) {
                    if (audioPlayer.isPlaying) {
                        audioPlayer.pause()
                    } else {
                        audioPlayer.play()
                    }
                } else {
                    activeAudioTaskId = item.task.id
                    audioPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(targetFile)))
                    audioPlayer.prepare()
                    audioPlayer.playWhenReady = true
                }
            }
            MediaKind.OTHER -> Unit
        }
    }

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
                ) {
                    Text("Save")
                }
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
                Text("This removes the saved media item from the library and deletes the local file when it still exists.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteTarget?.task?.id == activeAudioTaskId) {
                            audioPlayer.stop()
                            activeAudioTaskId = null
                            isAudioPlaying = false
                        }
                        deleteTarget?.let { onDelete(it.task.id) }
                        deleteTarget = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Video",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (items.isEmpty()) {
                    "Completed downloads will appear here."
                } else {
                    "${filteredItems.size} of ${items.size} saved items"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaFilter.entries.forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { selectedFilter = filter.name },
                    label = { Text(filter.label) },
                )
            }
        }

        uiState.infoMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
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
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    TextButton(onClick = onDismissMessage) { Text("Close") }
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
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
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    TextButton(onClick = onDismissMessage) { Text("Close") }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
            ) {
                Text(
                    text = if (items.isEmpty()) {
                        "Finish a download from Browser and it will move here automatically."
                    } else {
                        "No ${currentFilter.label.lowercase()} items in this section yet."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(22.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(filteredItems, key = { it.task.id }) { item ->
                    VideoLibraryCard(
                        item = item,
                        isInlineAudioPlaying = item.task.id == activeAudioTaskId && isAudioPlaying,
                        onPrimaryPlay = { handlePrimaryPlay(item) },
                        onOpenPlayer = { onOpenPlayer(item.task.id) },
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
                                context.startActivity(Intent.createChooser(intent, "Share media"))
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
private fun VideoLibraryCard(
    item: VideoLibraryItem,
    isInlineAudioPlaying: Boolean,
    onPrimaryPlay: () -> Unit,
    onOpenPlayer: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            LocalVideoThumbnail(
                filePath = item.file?.absolutePath,
                contentDescription = item.displayTitle,
                modifier = Modifier
                    .height(92.dp)
                    .weight(0.32f),
            )
            Column(
                modifier = Modifier.weight(0.68f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = item.displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.displaySize.ifBlank { "Saved file" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = item.mediaKind.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (item.mediaKind == MediaKind.AUDIO && isInlineAudioPlaying) {
                            Text(
                                text = "Playing audio",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (!item.exists) {
                            Text(
                                text = "File unavailable",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.exists && item.mediaKind != MediaKind.OTHER) {
                            IconButton(onClick = onPrimaryPlay) {
                                Icon(
                                    imageVector = if (item.mediaKind == MediaKind.AUDIO && isInlineAudioPlaying) {
                                        Icons.Outlined.Pause
                                    } else {
                                        Icons.Outlined.PlayCircle
                                    },
                                    contentDescription = if (item.mediaKind == MediaKind.AUDIO && isInlineAudioPlaying) {
                                        "Pause"
                                    } else {
                                        "Play"
                                    },
                                )
                            }
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Video actions")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            if (item.exists && item.mediaKind != MediaKind.OTHER) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (item.mediaKind == MediaKind.AUDIO && isInlineAudioPlaying) {
                                                "Pause audio"
                                            } else if (item.mediaKind == MediaKind.AUDIO) {
                                                "Play audio"
                                            } else {
                                                "Play"
                                            },
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (item.mediaKind == MediaKind.AUDIO && isInlineAudioPlaying) {
                                                Icons.Outlined.Pause
                                            } else {
                                                Icons.Outlined.PlayCircle
                                            },
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (item.mediaKind == MediaKind.AUDIO) {
                                            onPrimaryPlay()
                                        } else {
                                            onOpenPlayer()
                                        }
                                    },
                                )
                                if (item.mediaKind == MediaKind.AUDIO) {
                                    DropdownMenuItem(
                                        text = { Text("Open player") },
                                        leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) },
                                        onClick = {
                                            showMenu = false
                                            onOpenPlayer()
                                        },
                                    )
                                }
                            }
                            if (item.exists) {
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onShare()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.DriveFileRenameOutline,
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onRename()
                                    },
                                )
                            }
                            DropdownMenuItem(
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
            }
        }
    }
}

private fun Long.toReadableSize(): String {
    if (this <= 0L) return ""
    val kib = 1024.0
    val mib = kib * 1024.0
    val gib = mib * 1024.0
    return when {
        this >= gib -> "${(this / gib * 10.0).roundToInt() / 10.0} GB"
        this >= mib -> "${(this / mib * 10.0).roundToInt() / 10.0} MB"
        this >= kib -> "${(this / kib * 10.0).roundToInt() / 10.0} KB"
        else -> "$this B"
    }
}

private fun String?.meaningfulSizeLabel(): String? {
    val normalized = this?.trim().orEmpty()
    if (normalized.isBlank()) return null
    if (normalized.equals("na", ignoreCase = true)) return null
    if (normalized.equals("n/a", ignoreCase = true)) return null
    if (normalized.equals("unknown", ignoreCase = true)) return null
    return normalized
}

private fun resolveMediaKind(file: File?): MediaKind {
    val extension = file?.extension?.lowercase().orEmpty()
    return when (extension) {
        "mp4", "mkv", "webm", "mov", "avi", "m4v", "3gp" -> MediaKind.VIDEO
        "mp3", "m4a", "aac", "opus", "ogg", "wav", "flac" -> MediaKind.AUDIO
        else -> MediaKind.OTHER
    }
}

private enum class MediaFilter(val label: String) {
    All("All"),
    Video("Video"),
    Audio("Audio"),
    Other("Other"),
}

private val MediaKind.label: String
    get() = when (this) {
        MediaKind.VIDEO -> "Video"
        MediaKind.AUDIO -> "Audio"
        MediaKind.OTHER -> "Other"
    }



