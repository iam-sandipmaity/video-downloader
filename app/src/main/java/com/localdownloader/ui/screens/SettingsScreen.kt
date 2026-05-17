package com.localdownloader.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.BuildConfig
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.notifications.AppNotifications
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun SettingsScreen(
    uiState: FormatUiState,
    savedItemsCount: Int = 0,
    mediaInfoMessage: String? = null,
    mediaErrorMessage: String? = null,
    onDismissMediaLibraryMessage: () -> Unit = {},
    onLanguageChanged: (String) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentPresetChanged: (AccentPreset) -> Unit,
    onContrastModeChanged: (ContrastMode) -> Unit,
    onOutputTemplateChanged: (String) -> Unit,
    onDownloadsRootFolderNameChanged: (String) -> Unit,
    onVideoSubfolderNameChanged: (String) -> Unit,
    onAudioSubfolderNameChanged: (String) -> Unit,
    onOtherSubfolderNameChanged: (String) -> Unit,
    onContainerChanged: (String) -> Unit,
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onAutoRemoveMissingFilesFromLibraryChanged: (Boolean) -> Unit,
    onDeleteFromStorageWhenRemovedInAppChanged: (Boolean) -> Unit,
    onClearVideoTabEntries: () -> Unit,
    onDeleteAllSavedMedia: () -> Unit,
    onSaveClicked: () -> Unit,
    onResetSettings: () -> Unit,
    onClearCache: () -> Unit,
    cacheSize: Long = 0L,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val defaults = remember { AppSettings() }
    val svgImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    var choiceDialog by remember { mutableStateOf<ChoiceDialogState?>(null) }
    var textDialog by remember { mutableStateOf<TextDialogState?>(null) }
    var showLibraryClearDialog by remember { mutableStateOf(false) }
    var showDeleteAllMediaDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun openAppNotificationSettings() {
        val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    fun openChannelSettings(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(AndroidSettings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(AndroidSettings.EXTRA_CHANNEL_ID, channelId)
            }
            context.startActivity(intent)
        } else {
            openAppNotificationSettings()
        }
    }

    if (choiceDialog != null) {
        ChoiceDialog(
            state = choiceDialog!!,
            onDismiss = { choiceDialog = null },
        )
    }

    if (textDialog != null) {
        TextEditDialog(
            state = textDialog!!,
            onDismiss = { textDialog = null },
        )
    }

    if (showLibraryClearDialog) {
        ConfirmDialog(
            title = "Remove saved items from app",
            body = "This clears the library entries inside the app and leaves the original files on the device untouched.",
            confirmLabel = "Remove entries",
            onConfirm = {
                onClearVideoTabEntries()
                showLibraryClearDialog = false
            },
            onDismiss = { showLibraryClearDialog = false },
        )
    }

    if (showDeleteAllMediaDialog) {
        ConfirmDialog(
            title = "Delete all saved media",
            body = "This permanently removes downloaded files from the app and device storage.",
            confirmLabel = "Delete all",
            onConfirm = {
                onDeleteAllSavedMedia()
                showDeleteAllMediaDialog = false
            },
            onDismiss = { showDeleteAllMediaDialog = false },
            destructive = true,
        )
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset settings",
            body = "This restores appearance, folders, download defaults, and library behavior back to the default setup.",
            confirmLabel = "Reset now",
            onConfirm = {
                onResetSettings()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false },
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
        SettingsHeader(onBack = onBack)

        SectionLabel("General")
        SettingsListCard {
            SettingsValueRow(
                icon = Icons.Outlined.Language,
                title = "Language",
                subtitle = "English",
                value = "English",
                onClick = {
                    choiceDialog = ChoiceDialogState(
                        title = "Language",
                        selected = "English",
                        options = listOf(
                            ChoiceOption(
                                title = "English",
                                subtitle = "Only English is available right now.",
                                onSelect = { onLanguageChanged("en") },
                            ),
                        ),
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Settings,
                title = "Theme",
                subtitle = "Choose between system, dark, and light.",
                value = themeModeLabel(uiState.themeMode),
                onClick = {
                    choiceDialog = ChoiceDialogState(
                        title = "Theme",
                        selected = themeModeLabel(uiState.themeMode),
                        options = listOf(
                            ThemeMode.SYSTEM,
                            ThemeMode.DARK,
                            ThemeMode.LIGHT,
                        ).map { mode ->
                            ChoiceOption(
                                title = themeModeLabel(mode),
                                subtitle = when (mode) {
                                    ThemeMode.SYSTEM -> "Follow the device mode automatically."
                                    ThemeMode.DARK -> "Always use the darker app surface."
                                    ThemeMode.LIGHT -> "Always use the lighter app surface."
                                },
                                onSelect = { onThemeModeChanged(mode) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Palette,
                title = "Accents",
                subtitle = "Pick the color language for buttons, highlights, and player UI.",
                value = accentLabel(uiState.accentPreset),
                onClick = {
                    val accentOrder = listOf(
                        AccentPreset.AMBER,
                        AccentPreset.OCEAN,
                        AccentPreset.ROSE,
                        AccentPreset.FOREST,
                        AccentPreset.PURPLE,
                        AccentPreset.YELLOW,
                        AccentPreset.ORANGE,
                        AccentPreset.MONOCHROME,
                    )
                    choiceDialog = ChoiceDialogState(
                        title = "Accents",
                        selected = accentLabel(uiState.accentPreset),
                        options = accentOrder.map { preset ->
                            ChoiceOption(
                                title = accentLabel(preset),
                                subtitle = accentSubtitle(preset),
                                onSelect = { onAccentPresetChanged(preset) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Info,
                title = "Contrast",
                subtitle = "Choose between the softer default palette and stronger contrast.",
                value = contrastLabel(uiState.contrastMode),
                onClick = {
                    choiceDialog = ChoiceDialogState(
                        title = "Contrast",
                        selected = contrastLabel(uiState.contrastMode),
                        options = listOf(
                            ChoiceOption(
                                title = contrastLabel(ContrastMode.STANDARD),
                                subtitle = "Balanced contrast for the normal theme surfaces.",
                                onSelect = { onContrastModeChanged(ContrastMode.STANDARD) },
                            ),
                            ChoiceOption(
                                title = contrastLabel(ContrastMode.HIGH),
                                subtitle = "Sharper text and stronger separation between cards and background.",
                                onSelect = { onContrastModeChanged(ContrastMode.HIGH) },
                            ),
                        ),
                    )
                },
            )
        }

        SectionLabel("Folders")
        SettingsListCard {
            SettingsValueRow(
                icon = Icons.Outlined.Folder,
                title = "Downloads root",
                subtitle = "Main app folder under Downloads.",
                value = uiState.downloadsRootFolderName.cleanPreview(),
                onClick = {
                    textDialog = TextDialogState(
                        title = "Downloads root",
                        value = uiState.downloadsRootFolderName,
                        label = "Folder name",
                        supporting = "Example: LocalDownloader",
                        confirmLabel = "Save",
                        onConfirm = onDownloadsRootFolderNameChanged,
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Folder,
                title = "Video folder",
                subtitle = "Where downloaded video files are grouped.",
                value = uiState.videoSubfolderName.cleanPreview(),
                onClick = {
                    textDialog = TextDialogState(
                        title = "Video folder",
                        value = uiState.videoSubfolderName,
                        label = "Folder name",
                        supporting = "Example: Videos",
                        confirmLabel = "Save",
                        onConfirm = onVideoSubfolderNameChanged,
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Folder,
                title = "Audio folder",
                subtitle = "Where downloaded songs and audio extracts are grouped.",
                value = uiState.audioSubfolderName.cleanPreview(),
                onClick = {
                    textDialog = TextDialogState(
                        title = "Audio folder",
                        value = uiState.audioSubfolderName,
                        label = "Folder name",
                        supporting = "Example: Audio",
                        confirmLabel = "Save",
                        onConfirm = onAudioSubfolderNameChanged,
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Folder,
                title = "Other files folder",
                subtitle = "A fallback folder for anything that is not audio or video.",
                value = uiState.otherSubfolderName.cleanPreview(),
                onClick = {
                    textDialog = TextDialogState(
                        title = "Other files folder",
                        value = uiState.otherSubfolderName,
                        label = "Folder name",
                        supporting = "Example: Files",
                        confirmLabel = "Save",
                        onConfirm = onOtherSubfolderNameChanged,
                    )
                },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Reset folder names",
                subtitle = "Restore the default root, video, audio, and other folder names.",
                onClick = {
                    onDownloadsRootFolderNameChanged(defaults.downloadsRootFolderName)
                    onVideoSubfolderNameChanged(defaults.videoSubfolderName)
                    onAudioSubfolderNameChanged(defaults.audioSubfolderName)
                    onOtherSubfolderNameChanged(defaults.otherSubfolderName)
                },
            )
        }

        SectionLabel("Downloads")
        SettingsListCard {
            SettingsValueRow(
                icon = Icons.Outlined.Description,
                title = "Filename template",
                subtitle = "Used for future downloads.",
                value = uiState.outputTemplate,
                onClick = {
                    textDialog = TextDialogState(
                        title = "Filename template",
                        value = uiState.outputTemplate,
                        label = "Template",
                        supporting = "Example: %(title)s [%(id)s].%(ext)s",
                        confirmLabel = "Save",
                        onConfirm = onOutputTemplateChanged,
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.CloudDownload,
                title = "Default video container",
                subtitle = "Preferred output format for merged video downloads.",
                value = uiState.selectedContainer.uppercase(),
                onClick = {
                    val containers = listOf("mp4", "webm", "mkv", "mov")
                    choiceDialog = ChoiceDialogState(
                        title = "Default video container",
                        selected = uiState.selectedContainer,
                        options = containers.map { container ->
                            ChoiceOption(
                                title = container.uppercase(),
                                subtitle = containerDescription(container),
                                onSelect = { onContainerChanged(container) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Save,
                title = "Embed metadata",
                subtitle = "Write title, creator, album, and related tags into supported files.",
                checked = uiState.embedMetadata,
                onCheckedChange = onEmbedMetadataChanged,
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Palette,
                title = "Embed thumbnail",
                subtitle = "Attach artwork or cover images directly into compatible media files.",
                checked = uiState.embedThumbnail,
                onCheckedChange = onEmbedThumbnailChanged,
            )
        }

        SectionLabel("Notifications")
        SettingsListCard {
            SettingsActionRow(
                icon = Icons.Outlined.Campaign,
                title = "App notification settings",
                subtitle = "Open Android's main notification controls for this app.",
                onClick = ::openAppNotificationSettings,
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.CloudDownload,
                title = "Active downloads",
                subtitle = "Live progress cards for currently running items.",
                onClick = { openChannelSettings(AppNotifications.CHANNEL_ACTIVE_DOWNLOADS) },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Save,
                title = "Completed downloads",
                subtitle = "Completion notifications for each finished file.",
                onClick = { openChannelSettings(AppNotifications.CHANNEL_COMPLETED_DOWNLOADS) },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Info,
                title = "Download errors",
                subtitle = "Failed items that need attention.",
                onClick = { openChannelSettings(AppNotifications.CHANNEL_DOWNLOAD_ERRORS) },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Delete,
                title = "Canceled downloads",
                subtitle = "Alerts for tasks you stop yourself.",
                onClick = { openChannelSettings(AppNotifications.CHANNEL_CANCELED_DOWNLOADS) },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Palette,
                title = "Music player controls",
                subtitle = "Previous, next, seek, play, and pause notification controls.",
                onClick = { openChannelSettings(AppNotifications.CHANNEL_AUDIO_PLAYBACK) },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Web,
                title = "Promotions and updates",
                subtitle = "Optional product announcements and future promotional alerts.",
                onClick = { openChannelSettings(AppNotifications.CHANNEL_PROMOTIONS) },
            )
        }

        SectionLabel("Library and storage")
        SettingsListCard {
            SettingsToggleRow(
                icon = Icons.Outlined.Storage,
                title = "Auto-remove missing files",
                subtitle = "Clean broken library entries when files disappear outside the app.",
                checked = uiState.autoRemoveMissingFilesFromLibrary,
                onCheckedChange = onAutoRemoveMissingFilesFromLibraryChanged,
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Delete,
                title = "Delete from storage when removed in app",
                subtitle = "When you remove an item here, also delete the real device file.",
                checked = uiState.deleteFromStorageWhenRemovedInApp,
                onCheckedChange = onDeleteFromStorageWhenRemovedInAppChanged,
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Info,
                title = "Saved items",
                subtitle = "Completed downloads currently tracked in the library.",
                value = savedItemsCount.toString(),
                onClick = null,
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Storage,
                title = "Temporary cache",
                subtitle = "Reusable temporary files created during analysis and processing.",
                value = formatFileSize(cacheSize),
                onClick = null,
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Clear cache",
                subtitle = "Remove temporary files without touching your saved downloads.",
                onClick = onClearCache,
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Delete,
                title = "Clear app list",
                subtitle = "Remove completed-library entries but keep the actual files on the device.",
                onClick = { showLibraryClearDialog = true },
                enabled = savedItemsCount > 0,
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Delete,
                title = "Delete all saved media",
                subtitle = "Permanently remove downloaded files from both the library and storage.",
                onClick = { showDeleteAllMediaDialog = true },
                enabled = savedItemsCount > 0,
            )
        }

        if (!uiState.infoMessage.isNullOrBlank()) {
            FeedbackBanner(
                message = uiState.infoMessage,
                isError = false,
                onDismiss = null,
            )
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            FeedbackBanner(
                message = uiState.errorMessage,
                isError = true,
                onDismiss = null,
            )
        }
        if (!mediaInfoMessage.isNullOrBlank()) {
            FeedbackBanner(
                message = mediaInfoMessage,
                isError = false,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }
        if (!mediaErrorMessage.isNullOrBlank()) {
            FeedbackBanner(
                message = mediaErrorMessage,
                isError = true,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }

        SectionLabel("About")
        SettingsListCard {
            SettingsValueRow(
                icon = Icons.Outlined.Info,
                title = "Package name",
                subtitle = "Installed application identifier.",
                value = BuildConfig.APPLICATION_ID,
                onClick = null,
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Info,
                title = "Version",
                subtitle = "Current installed app version.",
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                onClick = null,
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Code,
                title = "App source code",
                subtitle = "github.com/iam-sandipmaity/video-downloader",
                onClick = { openUrl("https://github.com/iam-sandipmaity/video-downloader") },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Description,
                title = "yt-dlp",
                subtitle = "Open the upstream downloader engine project.",
                onClick = { openUrl("https://github.com/yt-dlp/yt-dlp") },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Description,
                title = "FFmpeg",
                subtitle = "Open the upstream media processing project.",
                onClick = { openUrl("https://github.com/FFmpeg/FFmpeg") },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Language,
                title = "Developer GitHub",
                subtitle = "@iam-sandipmaity",
                onClick = { openUrl("https://github.com/iam-sandipmaity") },
            )
            DividerInset()
            SettingsAssetActionRow(
                assetPath = "file:///android_asset/platform_logos/x.svg",
                imageLoader = svgImageLoader,
                title = "Developer X",
                subtitle = "@iam_sandipmaity",
                onClick = { openUrl("https://x.com/iam_sandipmaity") },
            )
            DividerInset()
            SettingsAssetActionRow(
                assetPath = "file:///android_asset/platform_logos/instagram.svg",
                imageLoader = svgImageLoader,
                title = "Developer Instagram",
                subtitle = "@iam_sandipmaity",
                onClick = { openUrl("https://instagram.com/iam_sandipmaity") },
            )
            DividerInset()
            SettingsAssetActionRow(
                assetPath = "file:///android_asset/platform_logos/linkedin.svg",
                imageLoader = svgImageLoader,
                title = "Developer LinkedIn",
                subtitle = "iam-sandipmaity",
                onClick = { openUrl("https://linkedin.com/in/iam-sandipmaity") },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onSaveClicked,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save changes")
            }
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsListCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    SettingsRowShell(
        icon = icon,
        title = title,
        subtitle = subtitle,
        value = value,
        onClick = onClick,
        enabled = true,
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    SettingsRowShell(
        icon = icon,
        title = title,
        subtitle = subtitle,
        value = null,
        onClick = if (enabled) onClick else null,
        enabled = enabled,
    )
}

@Composable
private fun SettingsAssetActionRow(
    assetPath: String,
    imageLoader: ImageLoader,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(24.dp),
        ) {
            AsyncImage(
                model = assetPath,
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.padding(1.dp),
            )
        }
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
    }
}

@Composable
private fun SettingsRowShell(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String?,
    onClick: (() -> Unit)?,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
            },
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
                color = if (enabled) MaterialTheme.colorScheme.onSurface else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                },
            )
        }
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
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
    state: ChoiceDialogState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                state.options.forEach { option ->
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
                            color = if (state.selected == option.title) {
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
                                if (state.selected == option.title) {
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
private fun TextEditDialog(
    state: TextDialogState,
    onDismiss: () -> Unit,
) {
    var value by remember(state) { mutableStateOf(state.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(state.label) },
                supportingText = { Text(state.supporting) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    state.onConfirm(value.trim())
                    onDismiss()
                },
            ) {
                Text(state.confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (destructive) "Cancel" else "Keep current")
            }
        },
    )
}

@Composable
private fun FeedbackBanner(
    message: String,
    isError: Boolean,
    onDismiss: (() -> Unit)?,
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
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

private data class ChoiceDialogState(
    val title: String,
    val selected: String,
    val options: List<ChoiceOption>,
)

private data class ChoiceOption(
    val title: String,
    val subtitle: String? = null,
    val onSelect: () -> Unit,
)

private data class TextDialogState(
    val title: String,
    val value: String,
    val label: String,
    val supporting: String,
    val confirmLabel: String,
    val onConfirm: (String) -> Unit,
)

private fun themeModeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}

private fun accentLabel(accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> "Material you"
        AccentPreset.OCEAN -> "Blue"
        AccentPreset.ROSE -> "Red"
        AccentPreset.FOREST -> "Green"
        AccentPreset.PURPLE -> "Purple"
        AccentPreset.YELLOW -> "Yellow"
        AccentPreset.ORANGE -> "Orange"
        AccentPreset.MONOCHROME -> "Monochrome"
    }
}

private fun accentSubtitle(accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> "A warm default with the soft amber look already used by the app."
        AccentPreset.OCEAN -> "Cool blue highlights for a calmer downloader mood."
        AccentPreset.ROSE -> "A stronger red accent for bold playback and action states."
        AccentPreset.FOREST -> "A greener look with a softer natural feel."
        AccentPreset.PURPLE -> "A richer violet palette for a more dramatic music vibe."
        AccentPreset.YELLOW -> "Bright yellow accents with higher energy."
        AccentPreset.ORANGE -> "Warm orange action tones similar to media apps."
        AccentPreset.MONOCHROME -> "Muted grayscale accents for a cleaner neutral setup."
    }
}

private fun contrastLabel(mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.STANDARD -> "Standard"
        ContrastMode.HIGH -> "High contrast"
    }
}

private fun containerDescription(container: String): String {
    return when (container) {
        "mp4" -> "Best general compatibility across Android devices and players."
        "webm" -> "Smaller web-friendly container when the source supports it well."
        "mkv" -> "Flexible container for mixed codecs and more unusual source formats."
        "mov" -> "Apple-style container when you want a closer edit-friendly export."
        else -> "Use this container for future merged video downloads."
    }
}

private fun String.cleanPreview(): String {
    return trim().ifBlank { "Default" }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
