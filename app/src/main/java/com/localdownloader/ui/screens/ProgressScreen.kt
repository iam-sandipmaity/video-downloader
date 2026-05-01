package com.localdownloader.ui.screens

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier,
) {
    val currentTimeMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }
    val progressTasks = uiState.tasks.filter {
        it.status == DownloadStatus.RUNNING ||
            it.status == DownloadStatus.QUEUED ||
            it.status == DownloadStatus.PAUSED
    }.sortedBy { it.createdAtEpochMs }

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
                        currentTimeMs = currentTimeMs,
                        onPause = onPause,
                        onResume = onResume,
                        onCancel = onCancel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressTaskCard(
    task: DownloadTask,
    currentTimeMs: Long,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val accent = when (task.status) {
        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    val pauseExpiryLabel = task.pauseExpiresAtEpochMs?.let { pauseExpiresAt ->
        buildPauseExpiryLabel(
            pauseExpiresAtEpochMs = pauseExpiresAt,
            currentTimeMs = currentTimeMs,
        )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                    )
                        ProgressStatusChip(task = task, accent = accent)
                    }
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
                    text = buildProgressHeadline(task),
                    style = MaterialTheme.typography.bodyLarge,
                )
                pauseExpiryLabel?.let { label ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(label) },
                    )
                }
                task.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatusChip(
    task: DownloadTask,
    accent: androidx.compose.ui.graphics.Color,
) {
    val label = when (task.status) {
        DownloadStatus.RUNNING -> "Running"
        DownloadStatus.PAUSED -> "Paused"
        DownloadStatus.QUEUED -> "Queued"
        DownloadStatus.COMPLETED -> "Done"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELED -> "Canceled"
    }
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

private fun buildProgressHeadline(task: DownloadTask): String {
    return when (task.status) {
        DownloadStatus.QUEUED -> "Waiting in queue"
        DownloadStatus.PAUSED -> "Paused. Resume will continue from saved partial data."
        DownloadStatus.RUNNING -> "Downloading now"
        DownloadStatus.COMPLETED -> "Completed"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELED -> "Canceled"
    }
}

private fun buildPauseExpiryLabel(
    pauseExpiresAtEpochMs: Long,
    currentTimeMs: Long,
): String {
    val remainingMs = (pauseExpiresAtEpochMs - currentTimeMs).coerceAtLeast(0L)
    val totalSeconds = remainingMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (remainingMs == 0L) {
        "Resume window expired"
    } else {
        "Resume within %02d:%02d".format(minutes, seconds)
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
