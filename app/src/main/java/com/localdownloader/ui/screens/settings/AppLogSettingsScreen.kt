package com.localdownloader.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.viewmodel.AppLogEntry
import com.localdownloader.viewmodel.AppLogEntryCategory
import com.localdownloader.viewmodel.AppLogOutcomeFilter
import com.localdownloader.viewmodel.AppLogUiState
import com.localdownloader.viewmodel.formatAppLogDayLabel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppLogSettingsScreen(
    uiState: AppLogUiState,
    onRefresh: () -> Unit,
    onOutcomeFilterChanged: (AppLogOutcomeFilter) -> Unit,
    onDayFilterChanged: (String?) -> Unit,
    onBackupLogsToDeviceChanged: (Boolean) -> Unit,
    onAutoDeleteOldAppLogsChanged: (Boolean) -> Unit,
    onAppLogRetentionDaysChanged: (Int) -> Unit,
    onBackupNow: () -> Unit,
    onDismissFeedback: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val filteredText = remember(uiState.filteredEntries) {
        uiState.filteredEntries.joinToString("\n\n") { it.rawText.trimEnd() }.trim()
    }
    val failedCount = remember(uiState.entries) {
        uiState.entries.count { it.category == AppLogEntryCategory.FAILED }
    }
    val successfulCount = remember(uiState.entries) {
        uiState.entries.count { it.category == AppLogEntryCategory.SUCCESSFUL }
    }
    var copied by remember { mutableStateOf(false) }
    var retentionDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    LaunchedEffect(filteredText) {
        copied = false
    }

    retentionDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { retentionDialog = null },
        )
    }

    PreferencePageScaffold(
        title = "App log",
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh log",
                )
            }
            IconButton(
                onClick = {
                    clipboardManager.setText(
                        AnnotatedString(
                            filteredText.ifBlank { "No app.log lines match the current filters." },
                        ),
                    )
                    copied = true
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy filtered log",
                )
            }
            IconButton(
                onClick = {
                    exportLogText(
                        context = context,
                        fileName = "app-log-filtered.txt",
                        text = filteredText.ifBlank { "No app.log lines match the current filters." },
                    )
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Export filtered log",
                )
            }
        },
    ) {
        item {
            AppLogOverviewStrip(
                entryCount = uiState.entries.size,
                failedCount = failedCount,
                successfulCount = successfulCount,
                statusText = when {
                    copied -> "Copied"
                    uiState.lastUpdatedAt != null -> "Updated ${formatLogRefreshTime(uiState.lastUpdatedAt)}"
                    else -> "Not refreshed yet"
                },
            )
        }
        item {
            AppLogPanel {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppLogChipGroup(
                        label = "Outcome",
                        chips = AppLogOutcomeFilter.entries.map { filter ->
                            AppLogFilterChip(
                                label = filter.label,
                                selected = uiState.selectedOutcome == filter,
                                onClick = { onOutcomeFilterChanged(filter) },
                            )
                        },
                    )
                    AppLogChipGroup(
                        label = "Day",
                        chips = buildList {
                            add(
                                AppLogFilterChip(
                                    label = "All days",
                                    selected = uiState.selectedDay == null,
                                    onClick = { onDayFilterChanged(null) },
                                ),
                            )
                            uiState.availableDays.forEach { day ->
                                add(
                                    AppLogFilterChip(
                                        label = formatAppLogDayLabel(day),
                                        selected = uiState.selectedDay == day,
                                        onClick = { onDayFilterChanged(day) },
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }

        item {
            AppLogPanel {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppLogSwitchRow(
                        title = "Backup to device",
                        checked = uiState.backupLogsToDevice,
                        onCheckedChange = onBackupLogsToDeviceChanged,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    AppLogSwitchRow(
                        title = "Auto cleanup",
                        checked = uiState.autoDeleteOldAppLogs,
                        onCheckedChange = onAutoDeleteOldAppLogsChanged,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                retentionDialog = SettingChoiceDialogState(
                                    title = "App log retention",
                                    selected = "${uiState.appLogRetentionDays} days",
                                    options = listOf(3, 7, 15, 30, 60, 90).map { days ->
                                        SettingChoiceOption(
                                            title = "$days days",
                                            subtitle = when {
                                                days <= 7 -> "Smaller footprint"
                                                days <= 30 -> "Balanced history"
                                                else -> "Longer history"
                                            },
                                            onSelect = { onAppLogRetentionDaysChanged(days) },
                                        )
                                    },
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("${uiState.appLogRetentionDays} days")
                        }
                        FilledTonalButton(
                            onClick = onBackupNow,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Back up now")
                        }
                    }
                }
            }
        }

        if (!uiState.infoMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = "App log",
                    message = uiState.infoMessage,
                    isError = false,
                    onDismiss = onDismissFeedback,
                )
            }
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = "App log",
                    message = uiState.errorMessage,
                    isError = true,
                    onDismiss = onDismissFeedback,
                )
            }
        }

        if (uiState.isLoading) {
            item {
                AppLogPanel {
                    Text(
                        text = "Loading app.log...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        } else if (uiState.filteredEntries.isEmpty()) {
            item {
                AppLogPanel {
                    Text(
                        text = if (uiState.entries.isEmpty()) {
                            "app.log is empty."
                        } else {
                            "No matching entries."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        } else {
            uiState.filteredEntries.forEach { entry ->
                item {
                    AppLogEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun AppLogOverviewStrip(
    entryCount: Int,
    failedCount: Int,
    successfulCount: Int,
    statusText: String,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            "$entryCount entries",
            "$failedCount failed",
            "$successfulCount successful",
            statusText,
        ).forEach { label ->
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class AppLogFilterChip(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppLogChipGroup(
    label: String,
    chips: List<AppLogFilterChip>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEach { chip ->
                FilterChip(
                    selected = chip.selected,
                    onClick = chip.onClick,
                    label = { Text(chip.label) },
                )
            }
        }
    }
}

@Composable
private fun AppLogPanel(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun AppLogSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun AppLogEntryCard(
    entry: AppLogEntry,
) {
    AppLogPanel {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildLogEntryHeader(entry),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = logHeaderColor(entry.category),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                SelectionContainer {
                    Text(
                        text = entry.rawText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun logHeaderColor(category: AppLogEntryCategory) = when (category) {
    AppLogEntryCategory.FAILED -> MaterialTheme.colorScheme.error
    AppLogEntryCategory.SUCCESSFUL -> MaterialTheme.colorScheme.primary
    AppLogEntryCategory.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun buildLogEntryHeader(entry: AppLogEntry): String {
    val parts = listOfNotNull(
        entry.level?.let { level ->
            when (level) {
                "E" -> "Failed"
                "W" -> "Warning"
                "I" -> "Info"
                "D" -> "Debug"
                else -> level
            }
        },
        entry.tag,
        entry.day?.let(::formatAppLogDayLabel),
    )
    return parts.joinToString(" | ").ifBlank { "Log entry" }
}

private fun formatLogRefreshTime(epochMs: Long): String {
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy | HH:mm"))
}

private fun exportLogText(
    context: Context,
    fileName: String,
    text: String,
) {
    val exportFile = File(context.cacheDir, fileName).apply {
        parentFile?.mkdirs()
        writeText(text)
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportFile,
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export app log"))
}
