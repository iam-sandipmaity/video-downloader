package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.viewmodel.DownloadUiState

@Composable
fun ProgressScreen(
    uiState: DownloadUiState,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onToggleDebug: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressTasks = uiState.tasks.filter {
        it.status == DownloadStatus.RUNNING ||
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.PAUSED
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (progressTasks.isEmpty()) "No active downloads right now." else "${progressTasks.size} downloads in motion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (progressTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Nothing is downloading yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Analyze a link from Browser to start a new task. Completed files will move to the Video tab automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(progressTasks, key = { it.id }) { task ->
                    ProgressTaskCard(
                        task = task,
                        showDebug = task.id in uiState.expandedDebugTaskIds,
                        onPause = onPause,
                        onResume = onResume,
                        onCancel = onCancel,
                        onToggleDebug = onToggleDebug,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressTaskCard(
    task: DownloadTask,
    showDebug: Boolean,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onToggleDebug: (String) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val accent = when (task.status) {
        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (task.status == DownloadStatus.PAUSED) {
                            Icons.Outlined.PauseCircle
                        } else {
                            Icons.Outlined.CloudDownload
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildProgressMeta(task),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Task actions")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        when (task.status) {
                            DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                                DropdownMenuItem(
                                    text = { Text("Pause") },
                                    leadingIcon = { Icon(Icons.Outlined.PauseCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onPause(task.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel") },
                                    leadingIcon = { Icon(Icons.Outlined.StopCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onCancel(task.id)
                                    },
                                )
                            }

                            DownloadStatus.PAUSED -> {
                                DropdownMenuItem(
                                    text = { Text("Resume") },
                                    leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onResume(task.id)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancel") },
                                    leadingIcon = { Icon(Icons.Outlined.StopCircle, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onCancel(task.id)
                                    },
                                )
                            }

                            else -> Unit
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = {
                    if (task.status == DownloadStatus.QUEUED) 0f
                    else task.progressPercent.coerceIn(0, 100) / 100f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when (task.status) {
                        DownloadStatus.QUEUED -> "Waiting in queue..."
                        DownloadStatus.PAUSED -> "${buildProgressMeta(task)} - paused"
                        else -> "${buildProgressMeta(task)} - downloading"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                val debugLine = task.debugTrace?.lineSequence()?.lastOrNull()?.takeIf { it.isNotBlank() }
                if (debugLine != null) {
                    Text(
                        text = debugLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (!task.debugTrace.isNullOrBlank()) {
                TextButton(
                    onClick = { onToggleDebug(task.id) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(if (showDebug) "Hide logs" else "Show logs")
                }
            }

            AnimatedVisibility(
                visible = showDebug,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = task.debugTrace.orEmpty(),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}

private fun buildProgressMeta(task: DownloadTask): String {
    val progress = "${task.progressPercent}%"
    val sizeLabel = when {
        !task.downloadedStr.isNullOrBlank() && !task.totalSizeStr.isNullOrBlank() -> "${task.downloadedStr}/${task.totalSizeStr}"
        !task.totalSizeStr.isNullOrBlank() -> task.totalSizeStr
        !task.downloadedStr.isNullOrBlank() -> task.downloadedStr
        else -> progress
    }
    val transfer = listOfNotNull(
        task.speed?.takeIf { it.isNotBlank() },
        task.eta?.takeIf { it.isNotBlank() }?.let { "ETA $it" },
    )
    return listOf(sizeLabel, transfer.joinToString(" - ").ifBlank { null })
        .filterNotNull()
        .joinToString(" - ")
}
