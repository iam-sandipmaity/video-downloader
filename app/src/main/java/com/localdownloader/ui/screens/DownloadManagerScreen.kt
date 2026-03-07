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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.viewmodel.DownloadUiState

@Composable
fun DownloadManagerScreen(
    uiState: DownloadUiState,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onToggleDebug: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tasks = uiState.tasks
    val runningTasks = tasks.filter { it.status == DownloadStatus.RUNNING }
    val globalSpeed = runningTasks.mapNotNull { it.speed }.firstOrNull()

    Column(modifier = modifier.fillMaxSize()) {
        // Header bar
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    if (tasks.isNotEmpty()) {
                        Text(
                            text = buildString {
                                if (runningTasks.isNotEmpty()) append("${runningTasks.size} active · ")
                                append("${tasks.size} total")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
                if (globalSpeed != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text(
                            text = "↓ $globalSpeed",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Go to Home to start a download",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
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

@Composable
private fun TaskCard(
    task: DownloadTask,
    showDebug: Boolean,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onToggleDebug: (String) -> Unit,
) {
    val statusColor = when (task.status) {
        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.QUEUED, DownloadStatus.CANCELED -> MaterialTheme.colorScheme.outline
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Colored left accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 72.dp)
                    .fillMaxHeight()
                    .background(statusColor),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Title + status chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    StatusChip(status = task.status, color = statusColor)
                }

                // Progress section
                when (task.status) {
                    DownloadStatus.RUNNING -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { task.progressPercent.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = statusColor,
                                trackColor = statusColor.copy(alpha = 0.2f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = buildSizeLabel(task),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val timeInfo = listOfNotNull(
                                    task.speed?.let { "↓ $it" },
                                    task.eta?.let { "ETA $it" },
                                ).joinToString("  ")
                                if (timeInfo.isNotBlank()) {
                                    Text(
                                        text = timeInfo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = statusColor,
                                    )
                                }
                            }
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { task.progressPercent.coerceIn(0, 100) / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = statusColor,
                                trackColor = statusColor.copy(alpha = 0.2f),
                            )
                            Text(
                                text = buildSizeLabel(task) + "  (paused)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    DownloadStatus.QUEUED -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "Waiting in queue…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        task.outputPath?.let { path ->
                            Text(
                                text = "Saved: ${shortenPath(path)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELED -> Unit
                }

                // Error message
                task.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Action row + debug toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (task.status) {
                            DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                                FilledTonalButton(onClick = { onPause(task.id) }) { Text("Pause") }
                                OutlinedButton(onClick = { onCancel(task.id) }) { Text("Cancel") }
                            }
                            DownloadStatus.PAUSED -> {
                                Button(onClick = { onResume(task.id) }) { Text("Resume") }
                                OutlinedButton(onClick = { onCancel(task.id) }) { Text("Cancel") }
                            }
                            DownloadStatus.FAILED -> {
                                Button(onClick = { onResume(task.id) }) { Text("Retry") }
                            }
                            DownloadStatus.COMPLETED, DownloadStatus.CANCELED -> Unit
                        }
                    }
                    if (!task.debugTrace.isNullOrBlank()) {
                        TextButton(onClick = { onToggleDebug(task.id) }) {
                            Text(
                                text = if (showDebug) "Hide debug" else "Show debug",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // Expandable debug trace
                AnimatedVisibility(
                    visible = showDebug,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    task.debugTrace?.let { trace ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = trace,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 60,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: DownloadStatus, color: Color) {
    val label = when (status) {
        DownloadStatus.RUNNING -> "Downloading"
        DownloadStatus.QUEUED -> "Queued"
        DownloadStatus.PAUSED -> "Paused"
        DownloadStatus.COMPLETED -> "Done ✓"
        DownloadStatus.FAILED -> "Failed"
        DownloadStatus.CANCELED -> "Cancelled"
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun buildSizeLabel(task: DownloadTask): String {
    val pct = "${task.progressPercent}%"
    val dl = task.downloadedStr
    val total = task.totalSizeStr
    return when {
        dl != null && total != null -> "$pct · $dl / $total"
        dl != null -> "$pct · $dl downloaded"
        total != null -> "$pct of $total"
        else -> pct
    }
}

private fun shortenPath(path: String): String {
    val parts = path.split("/")
    return if (parts.size > 3) "…/${parts.takeLast(2).joinToString("/")}" else path
}
