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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.ui.components.LocalVideoThumbnail
import com.localdownloader.ui.components.rememberLocalMediaSnapshot
import com.localdownloader.ui.support.openSupportIssue
import com.localdownloader.ui.support.SourceSiteVisual
import com.localdownloader.ui.support.shareAppLogs
import com.localdownloader.ui.support.sourceHostLabel
import com.localdownloader.ui.support.sourceSiteVisualForUrl
import com.localdownloader.viewmodel.DownloadUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    uiState: DownloadUiState,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onPauseTasks: (List<String>) -> Unit,
    onResumeTasks: (List<String>) -> Unit,
    onCancelTasks: (List<String>) -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
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
                QueueBatchActionRow(
                    currentFilter = currentFilter,
                    tasks = filteredTasks,
                    onPauseTasks = onPauseTasks,
                    onResumeTasks = onResumeTasks,
                    onCancelTasks = onCancelTasks,
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
                                onRetry = onRetry,
                                onCancel = onCancel,
                                onOpenCookies = onOpenCookies,
                                onOpenYoutubeAccess = onOpenYoutubeAccess,
                            )
                        } else {
                            ProgressTaskCard(
                                task = task,
                                currentTimeMs = currentTimeMs,
                                onPause = onPause,
                                onResume = onResume,
                                onRetry = onRetry,
                                onCancel = onCancel,
                                onOpenCookies = onOpenCookies,
                                onOpenYoutubeAccess = onOpenYoutubeAccess,
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
private fun QueueBatchActionRow(
    currentFilter: ProgressFilter,
    tasks: List<DownloadTask>,
    onPauseTasks: (List<String>) -> Unit,
    onResumeTasks: (List<String>) -> Unit,
    onCancelTasks: (List<String>) -> Unit,
) {
    val taskIds = tasks.map { it.id }
    val actions = when (currentFilter) {
        ProgressFilter.Queue -> listOf(
            QueueBatchAction(
                label = "Pause All In Queue",
                onClick = { onPauseTasks(taskIds) },
            ),
            QueueBatchAction(
                label = "Cancel All In Queue",
                onClick = { onCancelTasks(taskIds) },
            ),
        )

        ProgressFilter.Paused -> listOf(
            QueueBatchAction(
                label = "Resume All Scheduled",
                onClick = { onResumeTasks(taskIds) },
            ),
        )

        else -> emptyList()
    }
    if (actions.isEmpty() || taskIds.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        actions.forEach { action ->
            Surface(
                modifier = Modifier.clickable(onClick = action.onClick),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = action.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private data class QueueBatchAction(
    val label: String,
    val onClick: () -> Unit,
)

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
            .widthIn(min = 92.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (count > 0) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        } else {
            Spacer(modifier = Modifier.height(26.dp))
        }
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
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(104.dp)
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
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DownloadTaskHeroCard(
        task = task,
        currentTimeMs = currentTimeMs,
        onPause = onPause,
        onResume = onResume,
        onRetry = onRetry,
        onCancel = onCancel,
        onOpenCookies = onOpenCookies,
        onOpenYoutubeAccess = onOpenYoutubeAccess,
        modifier = modifier,
    )
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
private fun DownloadTaskThumbnail(
    task: DownloadTask,
    accent: Color,
    statusIcon: ImageVector,
    showStatusBadge: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        when {
            !task.outputPath.isNullOrBlank() -> {
                LocalVideoThumbnail(
                    filePath = task.outputPath,
                    contentDescription = task.title,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            !task.thumbnailUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = task.thumbnailUrl,
                    contentDescription = task.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            else -> {
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
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        if (showStatusBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp),
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
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DownloadTaskHeroCard(
        task = task,
        currentTimeMs = currentTimeMs,
        onPause = onPause,
        onResume = onResume,
        onRetry = onRetry,
        onCancel = onCancel,
        onOpenCookies = onOpenCookies,
        onOpenYoutubeAccess = onOpenYoutubeAccess,
        modifier = modifier,
    )
}

private data class DownloadTaskAction(
    val icon: ImageVector,
    val contentDescription: String,
    val label: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun DownloadTaskHeroCard(
    task: DownloadTask,
    currentTimeMs: Long,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCancel: (String) -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    val accent = statusAccent(task.status)
    val snapshot = rememberLocalMediaSnapshot(task.outputPath)
    val isStuck = isPotentiallyStuck(task, currentTimeMs)
    val isYoutubeTask = isYoutubeUrl(task.url)
    val sourceVisual = remember(task.url) { sourceSiteVisualForUrl(task.url) }
    val showRecoveryPanel = task.status == DownloadStatus.FAILED || isStuck
    val progressColor = when (task.status) {
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELED -> MaterialTheme.colorScheme.outline
        else -> accent
    }
    val taskStatusIcon = statusIcon(task.status)
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue(task),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "taskHeroProgress",
    )
    val pauseExpiryLabel = task.pauseExpiresAtEpochMs?.let { pauseExpiresAt ->
        buildPauseExpiryLabel(
            pauseExpiresAtEpochMs = pauseExpiresAt,
            currentTimeMs = currentTimeMs,
        )
    }
    val actions = when (task.status) {
        DownloadStatus.RUNNING, DownloadStatus.QUEUED -> listOf(
            DownloadTaskAction(
                icon = Icons.Outlined.PauseCircle,
                contentDescription = "Pause",
                onClick = { onPause(task.id) },
            ),
            DownloadTaskAction(
                icon = Icons.Outlined.Cancel,
                contentDescription = "Cancel",
                onClick = { onCancel(task.id) },
            ),
        )

        DownloadStatus.PAUSED -> listOf(
            DownloadTaskAction(
                icon = Icons.Outlined.PlayCircle,
                contentDescription = "Resume",
                onClick = { onResume(task.id) },
            ),
            DownloadTaskAction(
                icon = Icons.Outlined.Cancel,
                contentDescription = "Cancel",
                onClick = { onCancel(task.id) },
            ),
        )

        DownloadStatus.FAILED,
        DownloadStatus.CANCELED,
        -> listOf(
            DownloadTaskAction(
                icon = Icons.Outlined.Refresh,
                contentDescription = "Retry",
                label = "Retry",
                onClick = { onRetry(task.id) },
            ),
        )

        else -> emptyList()
    }
    val headerSummary = buildTaskHeaderSummaryEnhanced(task, snapshot)
    val subtitle = buildTaskSubtitleEnhanced(
        task = task,
        pauseExpiryLabel = pauseExpiryLabel,
        showSourceInBadge = sourceVisual != null,
    )
    val footerMessage = buildTaskFooterMessageEnhanced(task, snapshot, pauseExpiryLabel, currentTimeMs)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f,
                ),
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(218.dp)
                    .clip(RoundedCornerShape(32.dp)),
            ) {
                DownloadTaskThumbnail(
                    task = task,
                    accent = accent,
                    statusIcon = taskStatusIcon,
                    showStatusBadge = sourceVisual != null,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.30f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.62f),
                                ),
                            ),
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    if (sourceVisual != null) {
                        TaskSourceBadge(
                            sourceVisual = sourceVisual,
                            hostLabel = sourceVisual.label,
                            imageLoader = imageLoader,
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        ) {
                            Icon(
                                imageVector = taskStatusIcon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                    headerSummary?.let { summary ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        ) {
                            Text(
                                text = summary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (actions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        actions.forEach { action ->
                            DownloadTaskHeroActionButton(action = action)
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            top = 16.dp,
                            end = if (actions.isNotEmpty()) 188.dp else 20.dp,
                            bottom = 18.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.86f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    footerMessage?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(5.dp),
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.16f),
                )
            }

            if (showRecoveryPanel) {
                RecoveryHelperCard(
                    task = task,
                    currentTimeMs = currentTimeMs,
                    isStuck = isStuck,
                    isYoutubeTask = isYoutubeTask,
                    onRetry = if (task.status == DownloadStatus.FAILED) {
                        { onRetry(task.id) }
                    } else {
                        null
                    },
                    onOpenCookies = onOpenCookies,
                    onOpenYoutubeAccess = onOpenYoutubeAccess,
                    onExportLogs = { shareAppLogs(context, task, isStuck) },
                    onReportIssue = { openSupportIssue(context, task, isStuck) },
                )
            }
        }
    }
}

@Composable
private fun DownloadTaskHeroActionButton(
    action: DownloadTaskAction,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .widthIn(min = if (action.label != null) 112.dp else 84.dp)
            .clickable(onClick = action.onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
    ) {
        if (action.label != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.contentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskSourceBadge(
    sourceVisual: SourceSiteVisual,
    hostLabel: String,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(sourceVisual.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = sourceVisual.assetPath,
                    imageLoader = imageLoader,
                    contentDescription = "${sourceVisual.label} logo",
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = hostLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecoveryHelperCard(
    task: DownloadTask,
    currentTimeMs: Long,
    isStuck: Boolean,
    isYoutubeTask: Boolean,
    onRetry: (() -> Unit)?,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onExportLogs: () -> Unit,
    onReportIssue: () -> Unit,
) {
    val guidance = buildRecoveryGuidance(
        task = task,
        currentTimeMs = currentTimeMs,
        isStuck = isStuck,
        isYoutubeTask = isYoutubeTask,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (isStuck) "Download may be stuck" else "Troubleshooting help",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = guidance,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            onRetry?.let {
                RecoveryActionChip(label = "Retry", onClick = it)
            }
            RecoveryActionChip(label = "Try Cookies", onClick = onOpenCookies)
            if (isYoutubeTask) {
                RecoveryActionChip(label = "PO generation", onClick = onOpenYoutubeAccess)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecoveryActionChip(label = "Export log.txt", onClick = onExportLogs)
            RecoveryActionChip(label = "Report issue", onClick = onReportIssue)
        }
    }
}

@Composable
private fun RecoveryActionChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
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

private fun buildTaskHeaderSummary(
    task: DownloadTask,
    snapshot: com.localdownloader.ui.components.LocalMediaSnapshot,
): String? {
    val segments = buildList {
        compactResolutionLabel(snapshot.resolutionLabel)?.let(::add)
        snapshot.formatLabel?.takeIf { it.isNotBlank() }?.let(::add)
        (
            normalizedTaskSize(task.totalSizeStr)
            ?: normalizedTaskSize(snapshot.sizeLabel)
            ?: normalizedTaskSize(task.downloadedStr)
            )?.let(::add)
    }
    return segments.joinToString(" • ").ifBlank { null }
}

private fun buildTaskSubtitle(
    task: DownloadTask,
    pauseExpiryLabel: String?,
): String? {
    return when (task.status) {
        DownloadStatus.RUNNING -> listOfNotNull(
            statusLabel(task.status),
            task.speed?.takeIf { it.isNotBlank() },
            task.eta?.takeIf { it.isNotBlank() }?.let { "ETA $it" },
        ).joinToString(" • ")

        DownloadStatus.QUEUED -> "Waiting in queue"
        DownloadStatus.PAUSED -> pauseExpiryLabel ?: "Paused"
        DownloadStatus.COMPLETED -> "Finished"
        DownloadStatus.FAILED -> "Needs attention"
        DownloadStatus.CANCELED -> "Canceled"
    }.ifBlank { null }
}

private fun buildTaskFooterMessage(
    task: DownloadTask,
    snapshot: com.localdownloader.ui.components.LocalMediaSnapshot,
    pauseExpiryLabel: String?,
): String? {
    task.errorMessage?.takeIf { it.isNotBlank() }?.let { return it }
    latestDebugMessage(task.debugTrace)?.let { return it }
    return when (task.status) {
        DownloadStatus.RUNNING -> listOfNotNull(
            progressSizeLabel(task, snapshot.sizeLabel).takeIf { it.isNotBlank() },
            "${task.progressPercent}% complete".takeIf { task.progressPercent > 0 },
        ).joinToString(" • ").ifBlank { null }

        DownloadStatus.QUEUED -> "Queued and ready for the worker to start."
        DownloadStatus.PAUSED -> pauseExpiryLabel
        DownloadStatus.COMPLETED -> "Saved to your downloads library."
        DownloadStatus.FAILED -> "This item needs another try."
        DownloadStatus.CANCELED -> "Canceled by user."
    }
}

private fun compactResolutionLabel(value: String?): String? {
    val raw = value?.trim().orEmpty()
    if (raw.isBlank()) return null
    val dimensions = raw.substringAfter('(', "").substringBefore(')').trim()
    return dimensions.takeIf { it.isNotBlank() }?.uppercase() ?: raw.uppercase()
}

private fun normalizedTaskSize(value: String?): String? {
    val normalized = value?.trim().orEmpty()
    return normalized.takeIf {
        it.isNotBlank() &&
            !it.equals("na", ignoreCase = true) &&
            !it.equals("n/a", ignoreCase = true) &&
            !it.equals("unknown", ignoreCase = true)
    }
}

private fun latestDebugMessage(debugTrace: String?): String? {
    return debugTrace
        ?.lineSequence()
        ?.map { line ->
            val trimmed = line.trim()
            trimmed.substringAfter(": ", trimmed).trim()
        }
        ?.lastOrNull { it.isNotBlank() }
}

private fun buildTaskHeaderSummaryEnhanced(
    task: DownloadTask,
    snapshot: com.localdownloader.ui.components.LocalMediaSnapshot,
): String? {
    val segments = buildList {
        compactResolutionLabel(snapshot.resolutionLabel)?.let(::add)
        snapshot.formatLabel?.takeIf { it.isNotBlank() }?.let(::add)
        (
            normalizedTaskSize(task.totalSizeStr)
                ?: normalizedTaskSize(snapshot.sizeLabel)
                ?: normalizedTaskSize(task.downloadedStr)
            )?.let(::add)
    }
    return segments.joinToString(" | ").ifBlank { null }
}

private fun buildTaskSubtitleEnhanced(
    task: DownloadTask,
    pauseExpiryLabel: String?,
    showSourceInBadge: Boolean,
): String? {
    val sourceLabel = sourceHostLabel(task.url)?.takeUnless { showSourceInBadge }
    return when (task.status) {
        DownloadStatus.RUNNING -> listOfNotNull(
            sourceLabel,
            statusLabel(task.status),
            task.speed?.takeIf { it.isNotBlank() },
            task.eta?.takeIf { it.isNotBlank() }?.let { "ETA $it" },
        ).joinToString(" | ")

        DownloadStatus.QUEUED -> listOfNotNull(sourceLabel, "Waiting in queue").joinToString(" | ")
        DownloadStatus.PAUSED -> listOfNotNull(sourceLabel, pauseExpiryLabel ?: "Paused").joinToString(" | ")
        DownloadStatus.COMPLETED -> listOfNotNull(sourceLabel, "Finished").joinToString(" | ")
        DownloadStatus.FAILED -> listOfNotNull(sourceLabel, "Needs attention").joinToString(" | ")
        DownloadStatus.CANCELED -> listOfNotNull(sourceLabel, "Canceled").joinToString(" | ")
    }.ifBlank { null }
}

private fun buildTaskFooterMessageEnhanced(
    task: DownloadTask,
    snapshot: com.localdownloader.ui.components.LocalMediaSnapshot,
    pauseExpiryLabel: String?,
    currentTimeMs: Long,
): String? {
    task.errorMessage?.takeIf { it.isNotBlank() }?.let { return it }
    if (isPotentiallyStuck(task, currentTimeMs)) {
        return "No progress update for ${formatElapsedLabel(currentTimeMs - task.updatedAtEpochMs)}. This item may be stuck."
    }
    latestDebugMessage(task.debugTrace)?.let { return it }
    return when (task.status) {
        DownloadStatus.RUNNING -> listOfNotNull(
            progressSizeLabel(task, snapshot.sizeLabel).takeIf { it.isNotBlank() },
            "${task.progressPercent}% complete".takeIf { task.progressPercent > 0 },
        ).joinToString(" | ").ifBlank { null }

        DownloadStatus.QUEUED -> "Queued and ready for the worker to start."
        DownloadStatus.PAUSED -> pauseExpiryLabel
        DownloadStatus.COMPLETED -> "Saved to your downloads library."
        DownloadStatus.FAILED -> "This item needs another try."
        DownloadStatus.CANCELED -> "Canceled by user."
    }
}

private fun buildRecoveryGuidance(
    task: DownloadTask,
    currentTimeMs: Long,
    isStuck: Boolean,
    isYoutubeTask: Boolean,
): String {
    return when {
        isStuck && isYoutubeTask ->
            "This YouTube item has not updated for ${formatElapsedLabel(currentTimeMs - task.updatedAtEpochMs)}. Cookies or PO generation often help when playback access checks interrupt the download."

        isStuck ->
            "This item has not updated for ${formatElapsedLabel(currentTimeMs - task.updatedAtEpochMs)}. Cookies can help if the site needs a signed-in or region-matched session."

        isYoutubeTask ->
            "YouTube failures often improve after adding cookies and refreshing PO generation from More."

        else ->
            "If this download failed after redirects, rate limits, or protected access, try cookies first. If it still fails, export the logs and report the issue."
    }
}

private fun isYoutubeUrl(url: String): Boolean {
    val normalized = url.lowercase()
    return normalized.contains("youtube.com") || normalized.contains("youtu.be")
}

private fun isPotentiallyStuck(task: DownloadTask, currentTimeMs: Long): Boolean {
    if (task.status != DownloadStatus.RUNNING) return false
    if (task.progressPercent >= 100) return false
    return currentTimeMs - task.updatedAtEpochMs >= STUCK_THRESHOLD_MS
}

private fun formatElapsedLabel(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}

private const val STUCK_THRESHOLD_MS = 3L * 60L * 1_000L
