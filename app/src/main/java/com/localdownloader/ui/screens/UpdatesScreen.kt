package com.localdownloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.viewmodel.UpdateSectionUiState
import com.localdownloader.viewmodel.UpdatesUiState
import com.localdownloader.updates.FfmpegReleaseChannel
import com.localdownloader.updates.YtDlpReleaseChannel

@Composable
fun UpdatesScreen(
    uiState: UpdatesUiState,
    onBack: () -> Unit,
    onRefreshAll: () -> Unit,
    onRefreshApp: () -> Unit,
    onRefreshYtDlp: () -> Unit,
    onRefreshFfmpeg: () -> Unit,
    onInstallYtDlpUpdate: () -> Unit,
    onInstallFfmpegUpdate: () -> Unit,
    onYtDlpChannelChanged: (YtDlpReleaseChannel) -> Unit,
    onFfmpegChannelChanged: (FfmpegReleaseChannel) -> Unit,
    onAutoUpdateYtDlpChanged: (Boolean) -> Unit,
    onIncludePrereleaseAppReleasesChanged: (Boolean) -> Unit,
    onOpenChangelog: (String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var ytDlpChannelDialog by remember { mutableStateOf(false) }
    var ffmpegChannelDialog by remember { mutableStateOf(false) }

    if (ytDlpChannelDialog) {
        ChoiceDialog(
            title = "yt-dlp Source",
            selected = uiState.preferences.ytDlpChannel.title,
            options = YtDlpReleaseChannel.entries.map { channel ->
                UpdateChoiceOption(
                    title = channel.title,
                    subtitle = channel.description,
                    onSelect = { onYtDlpChannelChanged(channel) },
                )
            },
            onDismiss = { ytDlpChannelDialog = false },
        )
    }

    if (ffmpegChannelDialog) {
        ChoiceDialog(
            title = "FFmpeg Source",
            selected = uiState.preferences.ffmpegChannel.title,
            options = FfmpegReleaseChannel.entries.map { channel ->
                UpdateChoiceOption(
                    title = channel.title,
                    subtitle = channel.description,
                    onSelect = { onFfmpegChannelChanged(channel) },
                )
            },
            onDismiss = { ffmpegChannelDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (uiState.infoMessage != null || uiState.errorMessage != null) {
            FeedbackBanner(
                message = uiState.errorMessage ?: uiState.infoMessage.orEmpty(),
                isError = uiState.errorMessage != null,
                onDismiss = onDismissMessage,
            )
        }

        UpdatesHeader(onBack = onBack, onRefreshAll = onRefreshAll)

        UpdateSectionCard(section = uiState.app) {
            UpdateValueRow(
                icon = Icons.Outlined.Info,
                title = "Current version",
                subtitle = "Installed app build on this device.",
                value = uiState.app.currentVersion ?: "Unknown",
                onClick = null,
            )
            DividerInset()
            UpdateToggleRow(
                icon = Icons.Outlined.Settings,
                title = "Beta releases",
                subtitle = "Include pre-release GitHub builds when checking for app updates.",
                checked = uiState.preferences.includePrereleaseAppReleases,
                onCheckedChange = onIncludePrereleaseAppReleasesChanged,
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Check now",
                subtitle = buildCheckSubtitle(uiState.app),
                onClick = onRefreshApp,
            )
            if (uiState.app.updateAvailable) {
                DividerInset()
                UpdateActionRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Open app update",
                    subtitle = buildBrowserInstallSubtitle(uiState.app),
                    onClick = {
                        openExternalUrl(
                            context = context,
                            url = uiState.app.latestCheck?.downloadUrl ?: uiState.app.releasePageUrl,
                        )
                    },
                )
            }
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.Description,
                title = "Changelog",
                subtitle = if (uiState.app.releaseNotes.isNullOrBlank()) {
                    "Open the documentation-style changelog page for the app."
                } else {
                    "Read what changed in the latest app release."
                },
                onClick = { onOpenChangelog(UpdateChangelogSections.APP) },
            )
        }

        UpdateSectionCard(section = uiState.ytDlp) {
            UpdateValueRow(
                icon = Icons.Outlined.Code,
                title = "Current version",
                subtitle = "The embedded yt-dlp runtime currently used by the app.",
                value = uiState.ytDlp.currentVersion ?: "Unknown",
                onClick = null,
            )
            DividerInset()
            UpdateValueRow(
                icon = Icons.Outlined.Settings,
                title = "yt-dlp Source",
                subtitle = uiState.preferences.ytDlpChannel.description,
                value = uiState.preferences.ytDlpChannel.id,
                onClick = { ytDlpChannelDialog = true },
            )
            DividerInset()
            UpdateToggleRow(
                icon = Icons.Outlined.Sync,
                title = "Auto-update yt-dlp",
                subtitle = "Keep checking the selected source in the background when the app starts.",
                checked = uiState.preferences.autoUpdateYtDlp,
                onCheckedChange = onAutoUpdateYtDlpChanged,
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Check now",
                subtitle = buildCheckSubtitle(uiState.ytDlp),
                onClick = onRefreshYtDlp,
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.FileDownload,
                title = "Install new version of yt-dlp",
                subtitle = buildInstallSubtitle(uiState.ytDlp),
                onClick = onInstallYtDlpUpdate,
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.Description,
                title = "Changelog",
                subtitle = if (uiState.ytDlp.releaseNotes.isNullOrBlank()) {
                    "Open the documentation-style changelog page for yt-dlp."
                } else {
                    "Read what changed in the latest yt-dlp release."
                },
                onClick = { onOpenChangelog(UpdateChangelogSections.YT_DLP) },
            )
        }

        UpdateSectionCard(section = uiState.ffmpeg) {
            UpdateValueRow(
                icon = Icons.Outlined.Code,
                title = "Current version",
                subtitle = "The FFmpeg runtime currently resolving for merges and media processing.",
                value = uiState.ffmpeg.currentVersion ?: "Unknown",
                onClick = null,
            )
            DividerInset()
            UpdateValueRow(
                icon = Icons.Outlined.Settings,
                title = "FFmpeg Source",
                subtitle = uiState.preferences.ffmpegChannel.description,
                value = uiState.preferences.ffmpegChannel.id,
                onClick = { ffmpegChannelDialog = true },
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Check now",
                subtitle = buildCheckSubtitle(uiState.ffmpeg),
                onClick = onRefreshFfmpeg,
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.FileDownload,
                title = "Install new version of FFmpeg",
                subtitle = buildInstallSubtitle(uiState.ffmpeg),
                onClick = onInstallFfmpegUpdate,
            )
            DividerInset()
            UpdateActionRow(
                icon = Icons.Outlined.Description,
                title = "Changelog",
                subtitle = if (uiState.ffmpeg.releaseNotes.isNullOrBlank()) {
                    "Open the documentation-style changelog page for FFmpeg."
                } else {
                    "Read what changed in the latest FFmpeg runtime package."
                },
                onClick = { onOpenChangelog(UpdateChangelogSections.FFMPEG) },
            )
        }
    }
}

@Composable
private fun UpdatesHeader(
    onBack: () -> Unit,
    onRefreshAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                )
            }
            IconButton(onClick = onRefreshAll) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Refresh all",
                )
            }
        }
        Text(
            text = "Updating",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Manage app, yt-dlp, and FFmpeg update flows from one place without changing your download setup.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UpdateSectionCard(
    section: UpdateSectionUiState,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = section.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun UpdateValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    UpdateRowShell(
        icon = icon,
        title = title,
        subtitle = subtitle,
        value = value,
        onClick = onClick,
    )
}

@Composable
private fun UpdateActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    UpdateRowShell(
        icon = icon,
        title = title,
        subtitle = subtitle,
        value = null,
        onClick = onClick,
    )
}

@Composable
private fun UpdateRowShell(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String?,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        value?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UpdateToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun DividerInset() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    selected: String,
    options: List<UpdateChoiceOption>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                option.onSelect()
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (selected == option.title) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(18.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected == option.title) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                shape = CircleShape,
                                            ),
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            option.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun FeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            )
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}

private fun buildCheckSubtitle(section: UpdateSectionUiState): String {
    return when {
        section.isChecking -> "Checking for updates..."
        section.updateAvailable -> "Latest available: ${section.latestVersion ?: "Unknown"}"
        !section.lastStatus.isNullOrBlank() -> "${section.summary} Last status: ${section.lastStatus}"
        else -> section.summary
    }
}

private fun buildInstallSubtitle(section: UpdateSectionUiState): String {
    return when {
        section.isInstalling && section.progressPercent != null -> "Downloading... ${section.progressPercent}%"
        section.updateAvailable -> "Install ${section.latestVersion ?: "the latest version"}"
        else -> section.summary
    }
}

private fun buildBrowserInstallSubtitle(section: UpdateSectionUiState): String {
    return if (section.updateAvailable) {
        "Open ${section.latestVersion ?: "the latest version"} in your browser for download."
    } else {
        section.summary
    }
}

private fun openExternalUrl(
    context: android.content.Context,
    url: String?,
) {
    val normalizedUrl = url?.takeIf { it.isNotBlank() } ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private data class UpdateChoiceOption(
    val title: String,
    val subtitle: String?,
    val onSelect: () -> Unit,
)
