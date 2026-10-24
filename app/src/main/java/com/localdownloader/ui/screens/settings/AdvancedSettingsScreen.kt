package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun AdvancedSettingsScreen(
    uiState: FormatUiState,
    onNetworkTimeoutSecondsChanged: (Int) -> Unit,
    onMaxDownloadRetriesChanged: (Int) -> Unit,
    onDownloadRateLimitKbpsChanged: (Long) -> Unit,
    onGeoBypassChanged: (Boolean) -> Unit,
    onWriteDescriptionChanged: (Boolean) -> Unit,
    onWriteInfoJsonChanged: (Boolean) -> Unit,
    onRestrictFilenamesChanged: (Boolean) -> Unit,
    onWindowsFilenamesChanged: (Boolean) -> Unit,
    onDefaultPlaybackSpeedChanged: (Float) -> Unit,
    onDoubleTapSeekSecondsChanged: (Int) -> Unit,
    onBackgroundPlaybackChanged: (Boolean) -> Unit,
    onQueueOrderFifoChanged: (Boolean) -> Unit,
    onAutoRetryFailedDownloadsChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    val timeoutOptions = listOf(15, 30, 60, 120)
    val retriesOptions = listOf(0, 3, 6, 10, 20)
    val rateLimitOptions = listOf(
        0L to "Unlimited",
        500L to "500 KB/s",
        1000L to "1 MB/s",
        2000L to "2 MB/s",
        5000L to "5 MB/s",
        10000L to "10 MB/s",
        20000L to "20 MB/s",
    )
    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val seekOptions = listOf(5, 10, 15, 30)

    PreferencePageScaffold(
        title = "Advanced settings",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "NETWORK & DOWNLOAD ENGINE")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Timer,
                title = "Socket timeout",
                description = "${uiState.appSettings.networkTimeoutSeconds} seconds before connection times out",
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Socket timeout",
                        selected = "${uiState.appSettings.networkTimeoutSeconds}s",
                        options = timeoutOptions.map { sec ->
                            SettingChoiceOption(
                                title = "${sec}s",
                                subtitle = if (sec == 30) "Default recommended timeout" else null,
                                onSelect = { onNetworkTimeoutSecondsChanged(sec) },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Replay,
                title = "Max connection retries",
                description = if (uiState.appSettings.maxDownloadRetries == 0) "No retries" else "${uiState.appSettings.maxDownloadRetries} attempts before failing",
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Max connection retries",
                        selected = "${uiState.appSettings.maxDownloadRetries}",
                        options = retriesOptions.map { count ->
                            SettingChoiceOption(
                                title = if (count == 0) "No retries (0)" else "$count retries",
                                subtitle = if (count == 6) "Default recommended retry count" else null,
                                onSelect = { onMaxDownloadRetriesChanged(count) },
                            )
                        },
                    )
                },
            )
        }
        item {
            val currentRateLimit = uiState.appSettings.downloadRateLimitKbps
            val currentRateLimitLabel = rateLimitOptions.firstOrNull { it.first == currentRateLimit }?.second ?: "${currentRateLimit} KB/s"
            PreferenceItem(
                icon = Icons.Rounded.Speed,
                title = "Download rate limit",
                description = currentRateLimitLabel,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Download rate limit",
                        selected = currentRateLimitLabel,
                        options = rateLimitOptions.map { (kbps, label) ->
                            SettingChoiceOption(
                                title = label,
                                subtitle = if (kbps == 0L) "Maximum bandwidth" else "Cap speed to $label",
                                onSelect = { onDownloadRateLimitKbpsChanged(kbps) },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Language,
                title = "Bypass geographic restrictions",
                description = "Forward simulated client IP headers to access geo-blocked content",
                isChecked = uiState.appSettings.geoBypass,
                onClick = { onGeoBypassChanged(!uiState.appSettings.geoBypass) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Description,
                title = "Save video description",
                description = "Write video description to a separate .description text file",
                isChecked = uiState.appSettings.writeDescription,
                onClick = { onWriteDescriptionChanged(!uiState.appSettings.writeDescription) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Description,
                title = "Save metadata info JSON",
                description = "Write full stream and format metadata to a .info.json file",
                isChecked = uiState.appSettings.writeInfoJson,
                onClick = { onWriteInfoJsonChanged(!uiState.appSettings.writeInfoJson) },
            )
        }

        item {
            PreferenceSubtitle(text = "FILENAME COMPATIBILITY")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Title,
                title = "Windows / FAT32 file naming",
                description = "Sanitize characters not supported on Windows or FAT32 drives (: * ? \" < > |)",
                isChecked = uiState.appSettings.windowsFilenames,
                onClick = { onWindowsFilenamesChanged(!uiState.appSettings.windowsFilenames) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.TextFormat,
                title = "Restrict filenames to ASCII",
                description = "Enforce ASCII characters only and replace whitespace with underscores",
                isChecked = uiState.appSettings.restrictFilenames,
                onClick = { onRestrictFilenamesChanged(!uiState.appSettings.restrictFilenames) },
            )
        }

        item {
            PreferenceSubtitle(text = "PLAYBACK & GESTURES")
        }
        item {
            val speedLabel = "${uiState.appSettings.defaultPlaybackSpeed}x"
            PreferenceItem(
                icon = Icons.Rounded.FastForward,
                title = "Default playback speed",
                description = speedLabel,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Default playback speed",
                        selected = speedLabel,
                        options = speedOptions.map { sp ->
                            SettingChoiceOption(
                                title = "${sp}x",
                                subtitle = if (sp == 1.0f) "Normal speed" else null,
                                onSelect = { onDefaultPlaybackSpeedChanged(sp) },
                            )
                        },
                    )
                },
            )
        }
        item {
            val seekLabel = "${uiState.appSettings.doubleTapSeekSeconds} seconds"
            PreferenceItem(
                icon = Icons.Rounded.AvTimer,
                title = "Double-tap seek duration",
                description = seekLabel,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Double-tap seek duration",
                        selected = seekLabel,
                        options = seekOptions.map { sec ->
                            SettingChoiceOption(
                                title = "$sec seconds",
                                subtitle = if (sec == 10) "Standard default" else null,
                                onSelect = { onDoubleTapSeekSecondsChanged(sec) },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.PictureInPicture,
                title = "Background playback",
                description = "Keep video and audio playing when switching apps or locking screen",
                isChecked = uiState.appSettings.backgroundPlayback,
                onClick = { onBackgroundPlaybackChanged(!uiState.appSettings.backgroundPlayback) },
            )
        }

        item {
            PreferenceSubtitle(text = "DOWNLOAD QUEUE MANAGEMENT")
        }
        item {
            val isFifo = uiState.appSettings.queueOrderFifo
            val currentOrderTitle = if (isFifo) "First In, First Out (FIFO)" else "Last In, First Out (LIFO)"
            PreferenceItem(
                icon = Icons.Rounded.Queue,
                title = "Queue processing order",
                description = currentOrderTitle,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Queue processing order",
                        selected = currentOrderTitle,
                        options = listOf(
                            SettingChoiceOption(
                                title = "First In, First Out (FIFO)",
                                subtitle = "Downloads start in the order they were queued (oldest first)",
                                onSelect = { onQueueOrderFifoChanged(true) },
                            ),
                            SettingChoiceOption(
                                title = "Last In, First Out (LIFO)",
                                subtitle = "Newly queued downloads take immediate priority (newest first)",
                                onSelect = { onQueueOrderFifoChanged(false) },
                            ),
                        ),
                    )
                },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Replay,
                title = "Auto-retry failed downloads",
                description = "Automatically schedule background retries when network errors occur",
                isChecked = uiState.appSettings.autoRetryFailedDownloads,
                onClick = { onAutoRetryFailedDownloadsChanged(!uiState.appSettings.autoRetryFailedDownloads) },
            )
        }
    }
}
