package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.ui.components.LocalVideoThumbnail
import com.localdownloader.ui.components.MediaRowCard
import com.localdownloader.ui.components.MediaRowChip
import com.localdownloader.ui.components.rememberLocalMediaSnapshot
import com.localdownloader.viewmodel.DownloadUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    uiState: DownloadUiState,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val currentTimeMs by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }
    val isQueueMode = onBack != null
    val allTasks = uiState.tasks.sortedByDescending { it.updatedAtEpochMs }
    val runningCount = allTasks.count { it.status == DownloadStatus.RUNNING }
    val queuedCount = allTasks.count { it.status == DownloadStatus.QUEUED }
    val pausedCount = allTasks.count { it.status == DownloadStatus.PAUSED }
    val completedCount = allTasks.count { it.status == DownloadStatus.COMPLETED }
    val failedCount = allTasks.count { it.status == DownloadStatus.FAILED }
    val canceledCount = allTasks.count { it.status == DownloadStatus.CANCELED }
    val initialFilter = remember(
        isQueueMode,
        runningCount,
        queuedCount,
        pausedCount,
        failedCount,
        canceledCount,
    ) {
        if (!isQueueMode) {
            ProgressFilter.All.name
        } else {
            when {
                runningCount > 0 -> ProgressFilter.Downloading.name
                queuedCount > 0 -> ProgressFilter.Queue.name
                pausedCount > 0 -> ProgressFilter.Paused.name
                failedCount > 0 -> ProgressFilter.Error.name
                canceledCount > 0 -> ProgressFilter.Canceled.name
                else -> ProgressFilter.Downloading.name
            }
        }
    }
    var selectedFilter by rememberSaveable { mutableStateOf(initialFilter) }
    val currentFilter = runCatching { ProgressFilter.valueOf(selectedFilter) }.getOrDefault(ProgressFilter.All)
    val filteredTasks = allTasks.filter { currentFilter.matches(it.status) }
    var showTopMenu by remember { mutableStateOf(false) }

    val content: @Composable (PaddingValues) -> Unit = { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.9f,
                        stiffness = 500f,
                    ),
                )
                .padding(innerPadding)
                .padding(
                    horizontal = if (isQueueMode) 14.dp else 18.dp,
                    vertical = if (isQueueMode) 8.dp else 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (isQueueMode) 12.dp else 16.dp),
        ) {
            if (!isQueueMode) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = buildProgressSubtitle(
                            filter = currentFilter,
                            filteredCount = filteredTasks.size,
                            totalCount = allTasks.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!isQueueMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Status overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            StatusMetricPill(label = "Downloading", count = runningCount)
                            StatusMetricPill(label = "Queue", count = queuedCount)
                            StatusMetricPill(label = "Paused", count = pausedCount)
                            StatusMetricPill(label = "Done", count = completedCount)
                            StatusMetricPill(label = "Error", count = failedCount)
                            StatusMetricPill(label = "Canceled", count = canceledCount)
                        }
                    }
                }
            }

            if (isQueueMode) {
                QueueFilterRow(
                    currentFilter = currentFilter,
                    runningCount = runningCount,
                    queuedCount = queuedCount,
                    pausedCount = pausedCount,
                    failedCount = failedCount,
                    canceledCount = canceledCount,
                    onSelect = { selectedFilter = it.name },
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProgressFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { selectedFilter = filter.name },
                            label = { Text(filter.label) },
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = filteredTasks.isEmpty(),
                enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                    expandVertically(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (isQueueMode) 28.dp else 24.dp),
                    tonalElevation = if (isQueueMode) 0.dp else 2.dp,
                    color = if (isQueueMode) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = if (isQueueMode) 38.dp else 22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = if (isQueueMode) Alignment.CenterHorizontally else Alignment.Start,
                    ) {
                        if (isQueueMode) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        Text(
                            text = if (isQueueMode) "No Results" else buildEmptyStateTitle(currentFilter),
                            style = if (isQueueMode) {
                                MaterialTheme.typography.headlineSmall
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (isQueueMode) {
                                buildQueueEmptyStateBody(currentFilter)
                            } else {
                                buildEmptyStateBody(currentFilter)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isQueueMode) 2 else Int.MAX_VALUE,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = filteredTasks.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    expandVertically(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                    shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = if (isQueueMode) 10.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isQueueMode) 12.dp else 14.dp),
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        if (isQueueMode) {
                            QueueTaskRow(
                                task = task,
                                currentTimeMs = currentTimeMs,
                                onPause = onPause,
                                onResume = onResume,
                                onCancel = onCancel,
                            )
                        } else {
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
    }

    if (onBack != null) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Download Queue") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Queue options")
                            }
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Show all statuses") },
                                    onClick = {
                                        showTopMenu = false
                                        selectedFilter = ProgressFilter.All.name
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Show finished") },
                                    onClick = {
                                        showTopMenu = false
                                        selectedFilter = ProgressFilter.Done.name
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Show running") },
                                    onClick = {
                                        showTopMenu = false
                                        selectedFilter = ProgressFilter.Downloading.name
                                    },
                                )
                            }
                        }
                    },
                )
            },
        ) { innerPadding ->
            content(innerPadding)
        }
    } else {
        content(PaddingValues())
    }
}

@Composable
private fun QueueFilterRow(
    currentFilter: ProgressFilter,
    runningCount: Int,
    queuedCount: Int,
    pausedCount: Int,
    failedCount: Int,
    canceledCount: Int,
    onSelect: (ProgressFilter) -> Unit,
) {
    val queueTabs = listOf(
        ProgressFilter.Downloading,
        ProgressFilter.Queue,
        ProgressFilter.Paused,
        ProgressFilter.Canceled,
        ProgressFilter.Error,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        queueTabs.forEach { filter ->
            QueueFilterTab(
                label = queueLabel(filter),
                count = when (filter) {
                    ProgressFilter.Downloading -> runningCount
                    ProgressFilter.Queue -> queuedCount
                    ProgressFilter.Paused -> pausedCount
                    ProgressFilter.Canceled -> canceledCount
                    ProgressFilter.Error -> failedCount
                    else -> 0
                },
                selected = currentFilter == filter,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun QueueFilterTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 74.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (count > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = CircleShape,
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(82.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                ),
        )
    }
}

@Composable
private fun QueueTaskRow(
    task: DownloadTask,
    currentTimeMs: Long,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val accent = statusAccent(task.status)
    val snapshot = rememberLocalMediaSnapshot(task.outputPath)
    val progressColor = when (task.status) {
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELED -> MaterialTheme.colorScheme.outline
        else -> accent
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue(task),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "queueTaskProgress",
    )
    val pauseExpiryLabel = task.pauseExpiresAtEpochMs?.let { pauseExpiresAt ->
        buildPauseExpiryLabel(
            pauseExpiresAtEpochMs = pauseExpiresAt,
            currentTimeMs = currentTimeMs,
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .width(126.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!task.outputPath.isNullOrBlank()) {
                        LocalVideoThumbnail(
                            filePath = task.outputPath,
                            contentDescription = task.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = statusIcon(task.status),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = task.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Task actions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                            ) {
                                when (task.status) {
                                    DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                                        DropdownMenuItem(
                                            text = { Text("Pause") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.PauseCircle, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onPause(task.id)
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cancel") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.StopCircle, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onCancel(task.id)
                                            },
                                        )
                                    }

                                    DownloadStatus.PAUSED -> {
                                        DropdownMenuItem(
                                            text = { Text("Resume") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.PlayCircle, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onResume(task.id)
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Cancel") },
                                            leadingIcon = {
                                                Icon(Icons.Outlined.StopCircle, contentDescription = null)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onCancel(task.id)
                                            },
                                        )
                                    }

                                    else -> {
                                        DropdownMenuItem(
                                            text = { Text("No actions available") },
                                            onClick = { showMenu = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        QueueMetaChip(icon = statusIcon(task.status), accent = accent)
                        QueueMetaChip(text = progressSizeLabel(task, snapshot.sizeLabel))
                        QueueMetaChip(text = snapshot.resolutionLabel ?: progressStateChip(task))
                        QueueMetaChip(text = snapshot.formatLabel ?: statusLabel(task.status).uppercase())
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        task.speed?.takeIf { it.isNotBlank() }?.let { speed ->
                            QueueMetaChip(text = speed)
                        }
                        task.eta?.takeIf { it.isNotBlank() }?.let { eta ->
                            QueueMetaChip(text = "ETA $eta")
                        }
                        pauseExpiryLabel?.let { label ->
                            QueueMetaChip(text = label)
                        }
                        if (task.status == DownloadStatus.COMPLETED ||
                            task.status == DownloadStatus.FAILED ||
                            task.status == DownloadStatus.CANCELED
                        ) {
                            QueueMetaChip(text = statusLabel(task.status))
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            )
            task.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QueueMetaChip(
    text: String? = null,
    icon: ImageVector? = null,
    accent: Color? = null,
) {
    if (text.isNullOrBlank() && icon == null) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (text.isNullOrBlank()) 10.dp else 12.dp,
                vertical = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            text?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val hasActions = task.status == DownloadStatus.RUNNING ||
        task.status == DownloadStatus.QUEUED ||
        task.status == DownloadStatus.PAUSED
    val accent = statusAccent(task.status)
    val snapshot = rememberLocalMediaSnapshot(task.outputPath)
    val progressColor = when (task.status) {
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELED -> MaterialTheme.colorScheme.outline
        else -> accent
    }
    val statusIcon = statusIcon(task.status)
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue(task),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "taskProgress",
    )
    val pauseExpiryLabel = task.pauseExpiresAtEpochMs?.let { pauseExpiresAt ->
        buildPauseExpiryLabel(
            pauseExpiresAtEpochMs = pauseExpiresAt,
            currentTimeMs = currentTimeMs,
        )
    }
    val chips = buildList {
        add(MediaRowChip(icon = statusIcon, accent = accent))
        add(MediaRowChip(text = progressSizeLabel(task, snapshot.sizeLabel)))
        add(MediaRowChip(text = snapshot.resolutionLabel ?: progressStateChip(task)))
        add(MediaRowChip(text = snapshot.formatLabel ?: statusLabel(task.status).uppercase()))
        task.speed?.takeIf { it.isNotBlank() }?.let { speed ->
            add(MediaRowChip(text = speed))
        }
        task.eta?.takeIf { it.isNotBlank() }?.let { eta ->
            add(MediaRowChip(text = "ETA $eta"))
        }
    }

    MediaRowCard(
        title = task.title,
        chips = chips,
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = 500f,
            ),
        ),
        thumbnail = {
            if (!task.outputPath.isNullOrBlank()) {
                LocalVideoThumbnail(
                    filePath = task.outputPath,
                    contentDescription = task.title,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        },
        trailing = if (hasActions) {
            {
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
        } else {
            null
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                    )
                }
            }
        },
    )
}

@Composable
private fun StatusMetricPill(
    label: String,
    count: Int,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
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

private enum class ProgressFilter(val label: String) {
    All("All"),
    Downloading("Downloading"),
    Queue("In queue"),
    Paused("Paused"),
    Done("Done"),
    Error("Error"),
    Canceled("Canceled");

    fun matches(status: DownloadStatus): Boolean {
        return when (this) {
            All -> true
            Downloading -> status == DownloadStatus.RUNNING
            Queue -> status == DownloadStatus.QUEUED
            Paused -> status == DownloadStatus.PAUSED
            Done -> status == DownloadStatus.COMPLETED
            Error -> status == DownloadStatus.FAILED
            Canceled -> status == DownloadStatus.CANCELED
        }
    }
}

private fun buildProgressSubtitle(
    filter: ProgressFilter,
    filteredCount: Int,
    totalCount: Int,
): String {
    return when {
        totalCount == 0 -> "No downloads yet. As tasks start, their states will appear here."
        filter == ProgressFilter.All -> "$filteredCount downloads tracked across every status"
        else -> "$filteredCount items in ${filter.label.lowercase()} right now"
    }
}

private fun buildEmptyStateTitle(filter: ProgressFilter): String {
    return when (filter) {
        ProgressFilter.All -> "No downloads yet"
        ProgressFilter.Downloading -> "Nothing is downloading right now"
        ProgressFilter.Queue -> "Queue is empty"
        ProgressFilter.Paused -> "Nothing is paused"
        ProgressFilter.Done -> "No finished downloads yet"
        ProgressFilter.Error -> "No failed downloads"
        ProgressFilter.Canceled -> "No canceled downloads"
    }
}

private fun buildEmptyStateBody(filter: ProgressFilter): String {
    return when (filter) {
        ProgressFilter.All,
        ProgressFilter.Downloading,
        ProgressFilter.Queue,
        ProgressFilter.Paused ->
            "Analyze a link from Browser to start a new task. Downloads will move through these states automatically."

        ProgressFilter.Done ->
            "Completed items will stay visible here so you can quickly check what finished."

        ProgressFilter.Error ->
            "If a download fails, the task and its message will appear here for easier troubleshooting."

        ProgressFilter.Canceled ->
            "Canceled tasks stay grouped here so the history feels easier to scan."
    }
}

private fun buildQueueEmptyStateBody(filter: ProgressFilter): String {
    return when (filter) {
        ProgressFilter.Downloading -> "Nothing is downloading right now."
        ProgressFilter.Queue -> "The queue is clear for now."
        ProgressFilter.Paused -> "No scheduled or paused downloads right now."
        ProgressFilter.Error -> "No errored items need attention."
        ProgressFilter.Canceled -> "No canceled items in the queue."
        ProgressFilter.Done -> "Finished downloads can still be checked from the queue menu."
        ProgressFilter.All -> "New downloads from Home will appear here automatically."
    }
}

private fun queueLabel(filter: ProgressFilter): String {
    return when (filter) {
        ProgressFilter.Downloading -> "Running"
        ProgressFilter.Queue -> "In Queue"
        ProgressFilter.Paused -> "Scheduled"
        ProgressFilter.Error -> "Errored"
        ProgressFilter.Canceled -> "Cancelled"
        ProgressFilter.Done -> "Done"
        ProgressFilter.All -> "All"
    }
}

@Composable
private fun statusAccent(status: DownloadStatus): Color {
    return when (status) {
        DownloadStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELED -> MaterialTheme.colorScheme.outline
    }
}

private fun statusLabel(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.RUNNING -> "Downloading"
        DownloadStatus.QUEUED -> "Queued"
        DownloadStatus.PAUSED -> "Paused"
        DownloadStatus.COMPLETED -> "Done"
        DownloadStatus.FAILED -> "Error"
        DownloadStatus.CANCELED -> "Canceled"
    }
}

private fun statusIcon(status: DownloadStatus): ImageVector {
    return when (status) {
        DownloadStatus.RUNNING -> Icons.Outlined.CloudDownload
        DownloadStatus.QUEUED -> Icons.Outlined.Schedule
        DownloadStatus.PAUSED -> Icons.Outlined.PauseCircle
        DownloadStatus.COMPLETED -> Icons.Outlined.CheckCircle
        DownloadStatus.FAILED -> Icons.Outlined.ErrorOutline
        DownloadStatus.CANCELED -> Icons.Outlined.Cancel
    }
}

private fun progressValue(task: DownloadTask): Float {
    return when (task.status) {
        DownloadStatus.QUEUED -> 0f
        DownloadStatus.COMPLETED -> 1f
        else -> task.progressPercent.coerceIn(0, 100) / 100f
    }
}

private fun progressSizeLabel(
    task: DownloadTask,
    snapshotSizeLabel: String?,
): String {
    return when {
        !task.downloadedStr.isNullOrBlank() && !task.totalSizeStr.isNullOrBlank() ->
            "${task.downloadedStr}/${task.totalSizeStr}"

        !task.totalSizeStr.isNullOrBlank() -> task.totalSizeStr
        !task.downloadedStr.isNullOrBlank() -> task.downloadedStr
        !snapshotSizeLabel.isNullOrBlank() -> snapshotSizeLabel
        else -> "${task.progressPercent}%"
    }
}

private fun progressStateChip(task: DownloadTask): String {
    return when (task.status) {
        DownloadStatus.RUNNING -> "${task.progressPercent}% complete"
        DownloadStatus.QUEUED -> "Waiting"
        DownloadStatus.PAUSED -> "Paused"
        DownloadStatus.COMPLETED -> "Finished"
        DownloadStatus.FAILED -> "Error"
        DownloadStatus.CANCELED -> "Canceled"
    }
}
