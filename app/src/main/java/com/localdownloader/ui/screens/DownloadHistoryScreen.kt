package com.localdownloader.ui.screens

import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.support.shareAppLogs
import com.localdownloader.utils.SensitiveDataSanitizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    tasks: List<DownloadTask>,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val historyItems = remember(tasks) {
        tasks.filter {
            it.status == DownloadStatus.COMPLETED ||
                it.status == DownloadStatus.FAILED ||
                it.status == DownloadStatus.CANCELED
        }.sortedByDescending { it.updatedAtEpochMs }
    }
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryFilter.All.name) }
    var selectedTask by remember { mutableStateOf<DownloadTask?>(null) }

    val currentFilter = runCatching { HistoryFilter.valueOf(selectedFilter) }
        .getOrDefault(HistoryFilter.All)
    val filteredItems = historyItems.filter { currentFilter.matches(it.status) }
    val completedCount = historyItems.count { it.status == DownloadStatus.COMPLETED }
    val failedCount = historyItems.count { it.status == DownloadStatus.FAILED }
    val canceledCount = historyItems.count { it.status == DownloadStatus.CANCELED }

    PreferencePageScaffold(
        title = stringResource(R.string.history_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.history_task_summary),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.history_summary_showing,
                                    filteredItems.size,
                                    historyItems.size,
                                    historyFilterLabel(currentFilter, context).lowercase(Locale.getDefault()),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            HistorySummaryChip(
                                label = stringResource(R.string.history_filter_completed),
                                value = completedCount,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            HistorySummaryChip(
                                label = stringResource(R.string.history_filter_failed),
                                value = failedCount,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            HistorySummaryChip(
                                label = stringResource(R.string.history_filter_canceled),
                                value = canceledCount,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { selectedFilter = filter.name },
                        label = { Text(historyFilterLabel(filter, context)) },
                    )
                }
            }
        }
        if (filteredItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(68.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Article,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.history_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.history_empty_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { task ->
                HistoryCard(
                    task = task,
                    onOpenLog = { selectedTask = task },
                )
            }
        }
    }

    selectedTask?.let { task ->
        HistoryLogSheet(
            task = task,
            onDismiss = { selectedTask = null },
        )
    }
}

@Composable
private fun HistorySummaryChip(
    label: String,
    value: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.86f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCard(
    task: DownloadTask,
    onOpenLog: () -> Unit,
) {
    val context = LocalContext.current
    val statusColors = statusPalette(task.status)
    val sourceLabel = remember(task.url) { historySourceLabel(task.url) }
    val outputName = task.outputPath?.substringAfterLast('/') ?: context.getString(R.string.common_unknown)
    val previewLog = task.debugTrace
        ?.lines()
        ?.takeLast(3)
        ?.joinToString("\n")
        ?.trim()
        .orEmpty()

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when (task.status) {
                            DownloadStatus.COMPLETED -> stringResource(R.string.history_finished_successfully)
                            DownloadStatus.FAILED -> stringResource(R.string.history_stopped_with_error)
                            DownloadStatus.CANCELED -> stringResource(R.string.history_canceled_before_completion)
                            else -> historyStatusLabel(task.status, context)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HistoryInlineDetail(
                            label = sourceLabel,
                            icon = Icons.Outlined.Language,
                        )
                        HistoryInlineDetail(
                            label = formatDate(task.updatedAtEpochMs),
                            icon = Icons.Outlined.Schedule,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                HistoryStatusBadge(
                    text = historyStatusLabel(task.status, context),
                    background = statusColors.container,
                    foreground = statusColors.content,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HistoryMetaPill(
                    label = task.totalSizeStr ?: task.downloadedStr ?: stringResource(R.string.common_unknown),
                    icon = Icons.Outlined.Storage,
                )
                HistoryMetaPill(
                    label = outputName,
                    icon = Icons.Outlined.Folder,
                )
                if (task.subtitlePaths.isNotEmpty()) {
                    HistoryMetaPill(
                        label = pluralStringResource(
                            R.plurals.history_subtitle_files,
                            task.subtitlePaths.size,
                            task.subtitlePaths.size,
                        ),
                        icon = Icons.Outlined.Article,
                    )
                }
            }

            task.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.history_failure_reason),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.history_saved_path),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = task.outputPath ?: stringResource(R.string.common_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (previewLog.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.history_recent_log_lines),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = previewLog,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onOpenLog,
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(
                            if (task.debugTrace.isNullOrBlank()) {
                                R.string.history_view_details
                            } else {
                                R.string.history_view_log
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryStatusBadge(
    text: String,
    background: Color,
    foreground: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
        )
    }
}

@Composable
private fun HistoryInlineDetail(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryMetaPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HistoryLogSheet(
    task: DownloadTask,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hasTaskTrace = !task.debugTrace.isNullOrBlank()
    val fullTrace = if (hasTaskTrace) task.debugTrace.orEmpty() else buildHistoryDiagnosticSummary(task, context)
    var copied by remember(task.id, hasTaskTrace) { mutableStateOf(false) }
    val logListState = rememberLazyListState()
    val sheetScrollGuard = rememberBottomSheetScrollGuard(logListState)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .nestedScroll(sheetScrollGuard),
            state = logListState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(
                            if (hasTaskTrace) {
                                R.string.history_full_log
                            } else {
                                R.string.history_task_details
                            },
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${historyStatusLabel(task.status, context)} | ${formatDate(task.updatedAtEpochMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!hasTaskTrace) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.history_log_privacy_notice),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            task.outputPath?.takeIf { it.isNotBlank() }?.let { path ->
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SelectionContainer {
                            Text(
                                text = path,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(fullTrace))
                            copied = true
                        },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (copied) {
                                stringResource(R.string.common_copied)
                            } else {
                                stringResource(
                                    if (hasTaskTrace) {
                                        R.string.history_copy_log
                                    } else {
                                        R.string.history_copy_details
                                    },
                                )
                            },
                        )
                    }
                    FilledTonalButton(
                        onClick = { shareAppLogs(context, task = task) },
                    ) {
                        Text(stringResource(R.string.history_export_app_logs))
                    }
                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SelectionContainer {
                        Text(
                            text = fullTrace,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private fun formatDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy | HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

private fun historySourceLabel(url: String): String {
    return Uri.parse(url).host
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
        ?: ""
}

private fun historyStatusLabel(status: DownloadStatus, context: android.content.Context): String {
    return when (status) {
        DownloadStatus.COMPLETED -> context.getString(R.string.history_filter_completed)
        DownloadStatus.FAILED -> context.getString(R.string.history_filter_failed)
        DownloadStatus.CANCELED -> context.getString(R.string.history_filter_canceled)
        else -> status.name.lowercase().replaceFirstChar(Char::uppercase)
    }
}

private fun historyFilterLabel(filter: HistoryFilter, context: android.content.Context): String {
    return when (filter) {
        HistoryFilter.All -> context.getString(R.string.history_filter_all)
        HistoryFilter.Completed -> context.getString(R.string.history_filter_completed)
        HistoryFilter.Failed -> context.getString(R.string.history_filter_failed)
        HistoryFilter.Canceled -> context.getString(R.string.history_filter_canceled)
    }
}

private fun buildHistoryDiagnosticSummary(
    task: DownloadTask,
    context: android.content.Context,
): String {
    return buildString {
        appendLine(context.getString(R.string.history_no_task_log))
        appendLine(context.getString(R.string.history_log_privacy_notice))
        appendLine()
        appendLine("Title: ${task.title}")
        appendLine("Status: ${historyStatusLabel(task.status, context)}")
        appendLine("Updated: ${formatDate(task.updatedAtEpochMs)}")
        appendLine("Source: ${SensitiveDataSanitizer.describeUrl(task.url)}")
        appendLine("Progress: ${task.progressPercent}%")
        task.totalSizeStr?.takeIf { it.isNotBlank() }?.let { appendLine("Total size: $it") }
        task.downloadedStr?.takeIf { it.isNotBlank() }?.let { appendLine("Downloaded: $it") }
        task.outputPath?.takeIf { it.isNotBlank() }?.let {
            appendLine("Saved file: ${SensitiveDataSanitizer.describePath(it)}")
        }
        if (task.subtitlePaths.isNotEmpty()) {
            appendLine("Subtitle files: ${task.subtitlePaths.size}")
        }
        task.errorMessage?.takeIf { it.isNotBlank() }?.let {
            appendLine("Error: ${SensitiveDataSanitizer.sanitize(it)}")
        }
    }.trim()
}

@Composable
private fun statusPalette(status: DownloadStatus): StatusPalette {
    return when (status) {
        DownloadStatus.COMPLETED -> StatusPalette(
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        DownloadStatus.FAILED -> StatusPalette(
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer,
        )
        else -> StatusPalette(
            container = MaterialTheme.colorScheme.surface,
            content = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class StatusPalette(
    val container: Color,
    val content: Color,
)

private enum class HistoryFilter(val label: String) {
    All("All"),
    Completed("Completed"),
    Failed("Failed"),
    Canceled("Canceled");

    fun matches(status: DownloadStatus): Boolean {
        return when (this) {
            All -> true
            Completed -> status == DownloadStatus.COMPLETED
            Failed -> status == DownloadStatus.FAILED
            Canceled -> status == DownloadStatus.CANCELED
        }
    }
}
