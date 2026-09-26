package com.localdownloader.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WrapText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.localdownloader.BuildConfig
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.utils.SensitiveDataSanitizer
import com.localdownloader.viewmodel.AppLogEntry
import com.localdownloader.viewmodel.AppLogEntryCategory
import com.localdownloader.viewmodel.AppLogOutcomeFilter
import com.localdownloader.viewmodel.AppLogUiState
import com.localdownloader.viewmodel.formatAppLogDayLabel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLogSettingsScreen(
    uiState: AppLogUiState,
    onRefresh: () -> Unit,
    onOutcomeFilterChanged: (AppLogOutcomeFilter) -> Unit,
    onDayFilterChanged: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit = {},
    onClearAllLogs: () -> Unit = {},
    onMaxLogSizeBytesChanged: (Long) -> Unit = {},
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val filteredText = remember(uiState.filteredEntries) {
        uiState.filteredEntries.joinToString("\n\n") { it.rawText.trimEnd() }.trim()
    }

    val failedCount = remember(uiState.entries) {
        uiState.entries.count { it.category == AppLogEntryCategory.FAILED || it.level == "E" }
    }
    val warnCount = remember(uiState.entries) {
        uiState.entries.count { it.level == "W" }
    }
    val successfulCount = remember(uiState.entries) {
        uiState.entries.count { it.category == AppLogEntryCategory.SUCCESSFUL }
    }

    var copied by remember { mutableStateOf(false) }
    var wrapLines by remember { mutableStateOf(true) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    LaunchedEffect(filteredText) {
        copied = false
    }

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Clear all app logs?") },
            text = {
                Text(
                    "This will immediately wipe app.log, crash.log, and all historical rotated archives from disk.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearAllLogs()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Clear now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            StorageSettingsSheetContent(
                uiState = uiState,
                onAutoDeleteOldAppLogsChanged = onAutoDeleteOldAppLogsChanged,
                onMaxLogSizeBytesChanged = onMaxLogSizeBytesChanged,
                onAppLogRetentionDaysChanged = onAppLogRetentionDaysChanged,
                onBackupLogsToDeviceChanged = onBackupLogsToDeviceChanged,
                onBackupNow = onBackupNow,
                onOpenChoiceDialog = { choiceDialog = it },
                onExportDiagnostics = {
                    exportLogText(
                        context = context,
                        fileName = "troubleshooting-report.txt",
                        text = buildTroubleshootingReport(uiState.entries),
                    )
                },
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Log & Terminal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Disk: ${formatLogSizeLabel(uiState.totalLogSizeBytes)} · ${uiState.entries.size} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search logs",
                            tint = if (isSearchVisible || uiState.searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { wrapLines = !wrapLines }) {
                        Icon(
                            imageVector = Icons.Outlined.WrapText,
                            contentDescription = "Toggle wrap",
                            tint = if (wrapLines) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = {
                            exportLogText(
                                context = context,
                                fileName = "app-log-${System.currentTimeMillis()}.txt",
                                text = filteredText.ifBlank { "No app.log lines match the current filters." },
                            )
                        },
                    ) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Storage Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Collapsible Search Bar
            AnimatedVisibility(visible = isSearchVisible || uiState.searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    placeholder = { Text("Search logs, error traces, tags, threads...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
            }

            // Quick Level Filter Chips - Single Horizontal Scroll Row!
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedOutcome == AppLogOutcomeFilter.ALL,
                        onClick = { onOutcomeFilterChanged(AppLogOutcomeFilter.ALL) },
                        label = { Text("All (${uiState.entries.size})") },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedOutcome == AppLogOutcomeFilter.FAILED,
                        onClick = { onOutcomeFilterChanged(AppLogOutcomeFilter.FAILED) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TerminalColors.ErrorRed))
                                Text("Errors ($failedCount)")
                            }
                        },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedOutcome == AppLogOutcomeFilter.WARNINGS,
                        onClick = { onOutcomeFilterChanged(AppLogOutcomeFilter.WARNINGS) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TerminalColors.WarnAmber))
                                Text("Warnings ($warnCount)")
                            }
                        },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedOutcome == AppLogOutcomeFilter.SUCCESSFUL,
                        onClick = { onOutcomeFilterChanged(AppLogOutcomeFilter.SUCCESSFUL) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TerminalColors.SuccessGreen))
                                Text("Success ($successfulCount)")
                            }
                        },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedOutcome == AppLogOutcomeFilter.INFO,
                        onClick = { onOutcomeFilterChanged(AppLogOutcomeFilter.INFO) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TerminalColors.InfoCyan))
                                Text("Info")
                            }
                        },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedOutcome == AppLogOutcomeFilter.DEBUG,
                        onClick = { onOutcomeFilterChanged(AppLogOutcomeFilter.DEBUG) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TerminalColors.DebugLavender))
                                Text("Debug")
                            }
                        },
                    )
                }

                if (uiState.availableDays.isNotEmpty()) {
                    items(uiState.availableDays) { day ->
                        FilterChip(
                            selected = uiState.selectedDay == day,
                            onClick = {
                                if (uiState.selectedDay == day) onDayFilterChanged(null) else onDayFilterChanged(day)
                            },
                            label = { Text(formatAppLogDayLabel(day)) },
                        )
                    }
                }
            }

            if (!uiState.infoMessage.isNullOrBlank()) {
                InlineFeedbackCard(
                    label = "App log",
                    message = uiState.infoMessage,
                    isError = false,
                    onDismiss = onDismissFeedback,
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                InlineFeedbackCard(
                    label = "App log",
                    message = uiState.errorMessage,
                    isError = true,
                    onDismiss = onDismissFeedback,
                )
            }

            // Hero Full-Screen Terminal Viewport
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TerminalColors.Background,
                border = BorderStroke(1.dp, TerminalColors.Border),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Terminal Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TerminalColors.TitleBar)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Terminal,
                                contentDescription = null,
                                tint = TerminalColors.MutedGray,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "localdownloader@system:~/app.log",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TerminalColors.MutedGray,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${uiState.filteredEntries.size} lines",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TerminalColors.PromptGreen,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear logs",
                                tint = TerminalColors.ErrorRed,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { showClearConfirmDialog = true },
                            )
                        }
                    }

                    HorizontalDivider(color = TerminalColors.Border)

                    // Terminal Logs Body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (uiState.isLoading) {
                            Text(
                                text = "$ Reading log stream...\n$ Please wait...",
                                fontFamily = FontFamily.Monospace,
                                color = TerminalColors.PromptGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                            )
                        } else if (uiState.filteredEntries.isEmpty()) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    "$ No log entries matching query \"${uiState.searchQuery}\"."
                                } else {
                                    "$ app.log is currently empty.\n$ Operations and events will stream here live."
                                },
                                fontFamily = FontFamily.Monospace,
                                color = TerminalColors.MutedGray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                            )
                        } else {
                            SelectionContainer {
                                val scrollModifier = if (!wrapLines) {
                                    Modifier
                                        .fillMaxSize()
                                        .horizontalScroll(rememberScrollState())
                                } else {
                                    Modifier.fillMaxSize()
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = scrollModifier,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    itemsIndexed(uiState.filteredEntries) { index, entry ->
                                        TerminalLogLineItem(
                                            entry = entry,
                                            lineNumber = index + 1,
                                            wrapLines = wrapLines,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogLineItem(
    entry: AppLogEntry,
    lineNumber: Int,
    wrapLines: Boolean,
) {
    val levelColor = when (entry.level) {
        "E" -> TerminalColors.ErrorRed
        "W" -> TerminalColors.WarnAmber
        "I" -> when (entry.category) {
            AppLogEntryCategory.SUCCESSFUL -> TerminalColors.SuccessGreen
            else -> TerminalColors.InfoCyan
        }
        "D" -> TerminalColors.DebugLavender
        else -> TerminalColors.DefaultText
    }

    val levelTag = when (entry.level) {
        "E" -> "[ERROR]"
        "W" -> "[WARN]"
        "I" -> if (entry.category == AppLogEntryCategory.SUCCESSFUL) "[OK]" else "[INFO]"
        "D" -> "[DEBUG]"
        else -> "[LOG]"
    }

    val annotatedText = buildAnnotatedString {
        // Gutter Line Number with sleek separator
        withStyle(SpanStyle(color = TerminalColors.LineNumber, fontWeight = FontWeight.Normal)) {
            append("%3d │ ".format(lineNumber))
        }

        // Timestamp
        if (entry.timestamp != null) {
            withStyle(SpanStyle(color = TerminalColors.Timestamp)) {
                append(entry.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS ")))
            }
        }

        // Level Badge
        withStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.Bold)) {
            append("$levelTag ")
        }

        // Tag
        if (!entry.tag.isNullOrBlank()) {
            withStyle(SpanStyle(color = TerminalColors.TagCyan, fontWeight = FontWeight.Medium)) {
                append("${entry.tag}: ")
            }
        }

        // Message
        val messageColor = when {
            entry.category == AppLogEntryCategory.FAILED || entry.level == "E" -> TerminalColors.ErrorRed
            entry.category == AppLogEntryCategory.SUCCESSFUL -> TerminalColors.SuccessGreen
            entry.message.contains("ignoring", ignoreCase = true) || entry.message.contains("skipped", ignoreCase = true) -> TerminalColors.MutedGray
            else -> TerminalColors.DefaultText
        }

        withStyle(SpanStyle(color = messageColor)) {
            append(entry.message)
        }

        // Details / Stack trace
        if (!entry.details.isNullOrBlank()) {
            append("\n")
            withStyle(SpanStyle(color = TerminalColors.StackTraceRed)) {
                append("     │ ${entry.details.replace("\n", "\n     │ ")}")
            }
        }
    }

    Text(
        text = annotatedText,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        softWrap = wrapLines,
    )
}

@Composable
private fun StorageSettingsSheetContent(
    uiState: AppLogUiState,
    onAutoDeleteOldAppLogsChanged: (Boolean) -> Unit,
    onMaxLogSizeBytesChanged: (Long) -> Unit,
    onAppLogRetentionDaysChanged: (Int) -> Unit,
    onBackupLogsToDeviceChanged: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onOpenChoiceDialog: (SettingChoiceDialogState) -> Unit,
    onExportDiagnostics: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Storage & Auto-Cleanup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Current Disk Usage: ${formatLogSizeLabel(uiState.totalLogSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        AppLogSwitchRow(
            title = "Auto-delete threshold",
            subtitle = "Automatically rotate and trim logs when threshold or retention expires",
            checked = uiState.autoDeleteOldAppLogs,
            onCheckedChange = onAutoDeleteOldAppLogsChanged,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Size threshold picker
            FilledTonalButton(
                onClick = {
                    onOpenChoiceDialog(
                        SettingChoiceDialogState(
                            title = "Log size threshold",
                            selected = formatLogSizeLabel(uiState.appLogMaxSizeBytes),
                            options = listOf(
                                500L * 1024L to "500 KB (Minimal)",
                                1L * 1024L * 1024L to "1 MB (Light)",
                                2L * 1024L * 1024L to "2 MB (Default)",
                                5L * 1024L * 1024L to "5 MB (Standard)",
                                10L * 1024L * 1024L to "10 MB (Extended)",
                                25L * 1024L * 1024L to "25 MB (Heavy)",
                                0L to "Unlimited (No auto-cap)",
                            ).map { (bytes, label) ->
                                SettingChoiceOption(
                                    title = label,
                                    subtitle = if (bytes > 0) "Prunes logs beyond ${formatLogSizeLabel(bytes)}" else "Keep all lines until manual clear",
                                    onSelect = { onMaxLogSizeBytesChanged(bytes) },
                                )
                            },
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Size Limit", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatLogSizeLabel(uiState.appLogMaxSizeBytes),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Retention days picker
            FilledTonalButton(
                onClick = {
                    onOpenChoiceDialog(
                        SettingChoiceDialogState(
                            title = "App log retention",
                            selected = "${uiState.appLogRetentionDays} days",
                            options = listOf(3, 7, 15, 30, 60, 90).map { days ->
                                SettingChoiceOption(
                                    title = "$days days",
                                    subtitle = when {
                                        days <= 7 -> "Smaller storage footprint"
                                        days <= 30 -> "Balanced history"
                                        else -> "Longer diagnostic history"
                                    },
                                    onSelect = { onAppLogRetentionDaysChanged(days) },
                                )
                            },
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Retention", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${uiState.appLogRetentionDays} days",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        AppLogSwitchRow(
            title = "Backup to device storage",
            subtitle = "Save snapshots of rotated logs to Download/LocalDownloader/Logs",
            checked = uiState.backupLogsToDevice,
            onCheckedChange = onBackupLogsToDeviceChanged,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(
                onClick = onBackupNow,
                modifier = Modifier.weight(1f),
            ) {
                Text("Back up now")
            }

            FilledTonalButton(
                onClick = onExportDiagnostics,
                modifier = Modifier.weight(1f),
            ) {
                Text("Diagnostics report")
            }
        }
    }
}

@Composable
private fun AppLogSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private object TerminalColors {
    val Background = Color(0xFF0C1017)
    val TitleBar = Color(0xFF161B22)
    val Border = Color(0xFF30363D)
    val DefaultText = Color(0xFFE6EDF3)
    val LineNumber = Color(0xFF484F58)
    val Timestamp = Color(0xFF8B949E)
    val TagCyan = Color(0xFF38BDF8)
    val PromptGreen = Color(0xFF4ADE80)
    val ErrorRed = Color(0xFFFF5252)
    val StackTraceRed = Color(0xFFF87171)
    val WarnAmber = Color(0xFFFBBF24)
    val SuccessGreen = Color(0xFF34D399)
    val InfoCyan = Color(0xFF38BDF8)
    val DebugLavender = Color(0xFFA5B4FC)
    val MutedGray = Color(0xFF6E7681)
}

private fun formatLogSizeLabel(bytes: Long): String {
    return when {
        bytes <= 0L -> "No limit"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    }
}

private fun exportLogText(
    context: Context,
    fileName: String,
    text: String,
) {
    val exportFile = File(File(context.cacheDir, "shared"), fileName).apply {
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

private fun buildTroubleshootingReport(entries: List<AppLogEntry>): String {
    val latestFailure = entries
        .asReversed()
        .firstOrNull { it.category == AppLogEntryCategory.FAILED || it.level == "E" }
        ?.rawText
        ?.lineSequence()
        ?.firstOrNull { it.isNotBlank() }
        ?.let(SensitiveDataSanitizer::sanitize)
        ?: "No failed command has been logged yet."
    val ytDlpStatus = latestLineMatching(entries, "yt-dlp")
    val ffmpegStatus = latestLineMatching(entries, "ffmpeg")

    return buildString {
        appendLine("==============================================")
        appendLine("Video Downloader Diagnostics & System Report")
        appendLine("Generated: ${Instant.now()}")
        appendLine("==============================================")
        appendLine()
        appendLine("[App Environment]")
        appendLine("- Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
        appendLine("- Channel: ${BuildConfig.APP_RELEASE_CHANNEL}")
        appendLine("- Package: ${BuildConfig.APPLICATION_ID}")
        appendLine()
        appendLine("[Device Hardware & OS]")
        appendLine("- Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("- Manufacturer: ${Build.MANUFACTURER}")
        appendLine("- Model: ${Build.MODEL}")
        appendLine("- Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        appendLine()
        appendLine("[Execution Runtime Engines]")
        appendLine("- yt-dlp Status: ${ytDlpStatus ?: "No recent yt-dlp log line found."}")
        appendLine("- FFmpeg Status: ${ffmpegStatus ?: "No recent FFmpeg log line found."}")
        appendLine()
        appendLine("[Last Failure Incident]")
        appendLine(latestFailure)
    }
}

private fun latestLineMatching(entries: List<AppLogEntry>, needle: String): String? {
    return entries
        .asReversed()
        .flatMap { entry -> entry.rawText.lineSequence().toList().asReversed() }
        .firstOrNull { line -> line.contains(needle, ignoreCase = true) }
        ?.take(400)
        ?.let(SensitiveDataSanitizer::sanitize)
}
