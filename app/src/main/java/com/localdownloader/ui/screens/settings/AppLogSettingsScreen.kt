package com.localdownloader.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader
import com.localdownloader.ui.components.PreferenceSwitchRow
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
            PreferenceHeroCard(
                eyebrow = "Internal log",
                title = "Read app.log without leaving Settings",
                subtitle = "Filter the current app log by outcome or day, then copy or export only the lines you need.",
                badges = buildList {
                    add("${uiState.entries.size} entries")
                    add("$failedCount failed")
                    add("$successfulCount successful")
                },
            )
        }

        item {
            PreferenceSectionHeader(
                title = "Filters",
                subtitle = "Use one outcome filter and one day filter together.",
            )
        }

        item {
            PreferenceGroup {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Outcome",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AppLogOutcomeFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = uiState.selectedOutcome == filter,
                                    onClick = { onOutcomeFilterChanged(filter) },
                                    label = { Text(filter.label) },
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Day",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(
                                selected = uiState.selectedDay == null,
                                onClick = { onDayFilterChanged(null) },
                                label = { Text("All days") },
                            )
                            uiState.availableDays.forEach { day ->
                                FilterChip(
                                    selected = uiState.selectedDay == day,
                                    onClick = { onDayFilterChanged(day) },
                                    label = { Text(formatAppLogDayLabel(day)) },
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            copied -> "Filtered log copied."
                            uiState.lastUpdatedAt != null -> {
                                "Last refreshed ${formatLogRefreshTime(uiState.lastUpdatedAt)}"
                            }
                            else -> "Refresh to reload the latest app.log lines."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            PreferenceSectionHeader(
                title = "Storage",
                subtitle = "Manage device backups and how long rotated app logs stay inside the app.",
            )
        }

        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Outlined.Share,
                    title = "Back up logs to device",
                    subtitle = "Automatically copy rotated log archives into a device backup folder.",
                    checked = uiState.backupLogsToDevice,
                    onCheckedChange = onBackupLogsToDeviceChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Outlined.Refresh,
                    title = "Auto-delete old app logs",
                    subtitle = "Clean older rotated app logs from internal app storage after the retention window passes.",
                    checked = uiState.autoDeleteOldAppLogs,
                    onCheckedChange = onAutoDeleteOldAppLogsChanged,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Outlined.Refresh,
                    title = "Keep rotated logs for",
                    subtitle = "Choose how many days of archived log history stay inside the app.",
                    value = "${uiState.appLogRetentionDays} days",
                    onClick = {
                        retentionDialog = SettingChoiceDialogState(
                            title = "App log retention",
                            selected = "${uiState.appLogRetentionDays} days",
                            options = listOf(3, 7, 15, 30, 60, 90).map { days ->
                                SettingChoiceOption(
                                    title = "$days days",
                                    subtitle = when {
                                        days <= 7 -> "Smaller footprint with lighter history."
                                        days <= 30 -> "Balanced for everyday troubleshooting."
                                        else -> "Longer history for harder-to-reproduce bugs."
                                    },
                                    onSelect = { onAppLogRetentionDaysChanged(days) },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Outlined.Share,
                    title = "Back up now",
                    subtitle = uiState.deviceBackupPath?.let { path ->
                        "Write a snapshot of the current logs into $path"
                    } ?: "Write a snapshot of the current logs into the device backup folder.",
                    onClick = onBackupNow,
                )
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
                PreferenceGroup {
                    Text(
                        text = "Loading app.log...",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (uiState.filteredEntries.isEmpty()) {
            item {
                PreferenceGroup {
                    Text(
                        text = if (uiState.entries.isEmpty()) {
                            "app.log has no readable entries yet."
                        } else {
                            "No app.log entries match the current filters."
                        },
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            item {
                PreferenceSectionHeader(
                    title = "Log output",
                    subtitle = "${uiState.filteredEntries.size} matching entries shown below.",
                )
            }

            uiState.filteredEntries.forEach { entry ->
                item {
                    AppLogEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun AppLogEntryCard(
    entry: AppLogEntry,
) {
    PreferenceGroup {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = buildLogEntryHeader(entry),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                SelectionContainer {
                    Text(
                        text = entry.rawText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
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
