package com.localdownloader.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.BuildConfig
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.CacheCleanupPolicy
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.DownloadNetworkMode
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.notifications.AppNotifications
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.support.clearAppLogs
import com.localdownloader.ui.support.shareAppLogs
import com.localdownloader.viewmodel.FormatMessageScope
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
    onDefaultVideoOutputTemplateChanged: (String) -> Unit,
    onDefaultAudioOutputTemplateChanged: (String) -> Unit,
    onDownloadsRootFolderNameChanged: (String) -> Unit,
    onVideoSubfolderNameChanged: (String) -> Unit,
    onAudioSubfolderNameChanged: (String) -> Unit,
    onOtherSubfolderNameChanged: (String) -> Unit,
    onBrowseDownloadsRootFolder: () -> Unit,
    onBrowseVideoFolder: () -> Unit,
    onBrowseAudioFolder: () -> Unit,
    onBrowseOtherFolder: () -> Unit,
    onDefaultVideoContainerChanged: (String) -> Unit,
    onDefaultAudioContainerChanged: (String) -> Unit,
    onDownloadNetworkModeChanged: (DownloadNetworkMode) -> Unit,
    onMaxConcurrentDownloadsChanged: (Int) -> Unit,
    onDefaultAudioBitrateChanged: (Int) -> Unit,
    onDefaultDownloadSubtitlesChanged: (Boolean) -> Unit,
    onDefaultEmbedSubtitlesChanged: (Boolean) -> Unit,
    onDefaultEmbedMetadataChanged: (Boolean) -> Unit,
    onDefaultEmbedThumbnailChanged: (Boolean) -> Unit,
    onDefaultWriteThumbnailChanged: (Boolean) -> Unit,
    onAutoRemoveMissingFilesFromLibraryChanged: (Boolean) -> Unit,
    onDeleteFromStorageWhenRemovedInAppChanged: (Boolean) -> Unit,
    onCacheCleanupPolicyChanged: (CacheCleanupPolicy) -> Unit,
    onClearVideoTabEntries: () -> Unit,
    onDeleteAllSavedMedia: () -> Unit,
    onResetSettings: () -> Unit,
    onCopyAppRuntimeInfo: () -> Unit,
    onConsumePendingAppRuntimeInfo: () -> Unit,
    onClearCache: () -> Unit,
    cacheSize: Long = 0L,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val defaults = remember { AppSettings() }
    val settingsInfoMessage = uiState.infoMessageFor(FormatMessageScope.SETTINGS)
    val settingsErrorMessage = uiState.errorMessageFor(FormatMessageScope.SETTINGS)
    val svgImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    var choiceDialog by remember { mutableStateOf<ChoiceDialogState?>(null) }
    var textDialog by remember { mutableStateOf<TextDialogState?>(null) }
    var filenameTemplateDialog by remember { mutableStateOf<FilenameTemplateDialogState?>(null) }
    var showLibraryClearDialog by remember { mutableStateOf(false) }
    var showDeleteAllMediaDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.pendingAppRuntimeInfo) {
        val pending = uiState.pendingAppRuntimeInfo ?: return@LaunchedEffect
        clipboardManager.setText(AnnotatedString(pending))
        Toast.makeText(context, "App diagnostics copied.", Toast.LENGTH_SHORT).show()
        onConsumePendingAppRuntimeInfo()
    }

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

    fun openFilenameTemplateDialog(
        title: String,
        currentValue: String,
        presets: List<FilenameTemplatePreset>,
        onConfirm: (String) -> Unit,
    ) {
        filenameTemplateDialog = FilenameTemplateDialogState(
            title = title,
            value = currentValue,
            supporting = "Use yt-dlp placeholders. Keep %(ext)s in the template so the final file extension stays correct.",
            presets = presets,
            tokens = suggestedFilenameTokens(),
            onConfirm = onConfirm,
        )
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

    if (filenameTemplateDialog != null) {
        FilenameTemplateDialog(
            state = filenameTemplateDialog!!,
            onDismiss = { filenameTemplateDialog = null },
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

        if (!settingsInfoMessage.isNullOrBlank()) {
            InlineFeedbackCard(
                label = "Settings",
                message = settingsInfoMessage,
                isError = false,
                onDismiss = null,
            )
        }
        if (!settingsErrorMessage.isNullOrBlank()) {
            InlineFeedbackCard(
                label = "Settings",
                message = settingsErrorMessage,
                isError = true,
                onDismiss = null,
            )
        }
        if (!mediaInfoMessage.isNullOrBlank()) {
            InlineFeedbackCard(
                label = "Library",
                message = mediaInfoMessage,
                isError = false,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }
        if (!mediaErrorMessage.isNullOrBlank()) {
            InlineFeedbackCard(
                label = "Library",
                message = mediaErrorMessage,
                isError = true,
                onDismiss = onDismissMediaLibraryMessage,
            )
        }

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
                        AccentPreset.COBALT,
                        AccentPreset.AQUA,
                        AccentPreset.TEAL,
                        AccentPreset.MINT,
                        AccentPreset.EMERALD,
                        AccentPreset.FOREST,
                        AccentPreset.ROSE,
                        AccentPreset.CRIMSON,
                        AccentPreset.MAGENTA,
                        AccentPreset.PURPLE,
                        AccentPreset.YELLOW,
                        AccentPreset.ORANGE,
                        AccentPreset.COPPER,
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
                subtitle = "Tune how gently or sharply the app separates cards, text, and backgrounds.",
                value = contrastLabel(uiState.contrastMode),
                onClick = {
                    choiceDialog = ChoiceDialogState(
                        title = "Contrast",
                        selected = contrastLabel(uiState.contrastMode),
                        options = ContrastMode.entries.map { mode ->
                            ChoiceOption(
                                title = contrastLabel(mode),
                                subtitle = contrastSubtitle(mode),
                                onSelect = { onContrastModeChanged(mode) },
                            )
                        },
                    )
                },
            )
        }

        SectionLabel("Folders")
        SettingsListCard {
            SettingsBrowseValueRow(
                icon = Icons.Outlined.Folder,
                title = "Downloads root",
                subtitle = "Type a folder path under Downloads or browse to a subfolder you want the app to use as its main root.",
                value = uiState.downloadsRootFolderName.folderPreview("Default root"),
                onEditClick = {
                    textDialog = TextDialogState(
                        title = "Downloads root",
                        value = uiState.downloadsRootFolderName,
                        label = "Folder path under Downloads",
                        supporting = "Examples: LocalDownloader or Media/LocalDownloader",
                        confirmLabel = "Save",
                        onConfirm = onDownloadsRootFolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseDownloadsRootFolder,
            )
            DividerInset()
            SettingsBrowseValueRow(
                icon = Icons.Outlined.Folder,
                title = "Video folder",
                subtitle = "Type a subfolder path or browse to a folder inside the current downloads root for videos.",
                value = uiState.videoSubfolderName.folderPreview("Downloads root"),
                onEditClick = {
                    textDialog = TextDialogState(
                        title = "Video folder",
                        value = uiState.videoSubfolderName,
                        label = "Folder path inside the downloads root",
                        supporting = "Examples: Videos or Media/Videos. Leave blank to save videos directly in the downloads root.",
                        confirmLabel = "Save",
                        onConfirm = onVideoSubfolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseVideoFolder,
            )
            DividerInset()
            SettingsBrowseValueRow(
                icon = Icons.Outlined.Folder,
                title = "Audio folder",
                subtitle = "Type a subfolder path or browse to a folder inside the current downloads root for audio.",
                value = uiState.audioSubfolderName.folderPreview("Downloads root"),
                onEditClick = {
                    textDialog = TextDialogState(
                        title = "Audio folder",
                        value = uiState.audioSubfolderName,
                        label = "Folder path inside the downloads root",
                        supporting = "Examples: Audio or Music/Tracks. Leave blank to save audio directly in the downloads root.",
                        confirmLabel = "Save",
                        onConfirm = onAudioSubfolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseAudioFolder,
            )
            DividerInset()
            SettingsBrowseValueRow(
                icon = Icons.Outlined.Folder,
                title = "Other files folder",
                subtitle = "Type a subfolder path or browse to a folder inside the current downloads root for anything else.",
                value = uiState.otherSubfolderName.folderPreview("Downloads root"),
                onEditClick = {
                    textDialog = TextDialogState(
                        title = "Other files folder",
                        value = uiState.otherSubfolderName,
                        label = "Folder path inside the downloads root",
                        supporting = "Examples: Files or Archives/Misc. Leave blank to save these files directly in the downloads root.",
                        confirmLabel = "Save",
                        onConfirm = onOtherSubfolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseOtherFolder,
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
                title = "Filename template [video]",
                subtitle = "Used for future video and merged downloads.",
                value = uiState.outputTemplate,
                onClick = {
                    openFilenameTemplateDialog(
                        title = "Filename template [video]",
                        currentValue = uiState.outputTemplate,
                        presets = videoFilenameTemplatePresets(),
                        onConfirm = onDefaultVideoOutputTemplateChanged,
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Description,
                title = "Filename template [audio]",
                subtitle = "Used for future audio-only downloads and extracts.",
                value = uiState.audioOutputTemplate,
                onClick = {
                    openFilenameTemplateDialog(
                        title = "Filename template [audio]",
                        currentValue = uiState.audioOutputTemplate,
                        presets = audioFilenameTemplatePresets(),
                        onConfirm = onDefaultAudioOutputTemplateChanged,
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
                        selected = uiState.selectedContainer.uppercase(),
                        options = containers.map { container ->
                            ChoiceOption(
                                title = container.uppercase(),
                                subtitle = containerDescription(container),
                                onSelect = { onDefaultVideoContainerChanged(container) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.CloudDownload,
                title = "Default audio container",
                subtitle = "Preferred output format for future audio extracts.",
                value = uiState.selectedAudioFormat.uppercase(),
                onClick = {
                    val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
                    choiceDialog = ChoiceDialogState(
                        title = "Default audio container",
                        selected = uiState.selectedAudioFormat.uppercase(),
                        options = audioFormats.map { format ->
                            ChoiceOption(
                                title = format.uppercase(),
                                subtitle = audioFormatDescription(format),
                                onSelect = { onDefaultAudioContainerChanged(format) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Description,
                title = "Download subtitles",
                subtitle = "Fetch subtitle sidecars automatically when they are available.",
                checked = uiState.downloadSubtitles,
                onCheckedChange = onDefaultDownloadSubtitlesChanged,
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Save,
                title = "Embed subtitles",
                subtitle = "Try to place subtitles inside the final video file when the container supports it.",
                checked = uiState.embedSubtitles,
                onCheckedChange = onDefaultEmbedSubtitlesChanged,
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Save,
                title = "Embed metadata",
                subtitle = "Write title, creator, album, and related tags into supported files.",
                checked = uiState.embedMetadata,
                onCheckedChange = onDefaultEmbedMetadataChanged,
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Palette,
                title = "Embed thumbnail",
                subtitle = "Attach artwork or cover images directly into compatible media files.",
                checked = uiState.embedThumbnail,
                onCheckedChange = onDefaultEmbedThumbnailChanged,
            )
        }

        SectionLabel("Download rules")
        SettingsListCard {
            SettingsValueRow(
                icon = Icons.Outlined.CloudDownload,
                title = "Download network",
                subtitle = "Choose when queued downloads are allowed to start.",
                value = networkModeLabel(uiState.downloadNetworkMode),
                onClick = {
                    choiceDialog = ChoiceDialogState(
                        title = "Download network",
                        selected = networkModeLabel(uiState.downloadNetworkMode),
                        options = DownloadNetworkMode.entries.map { mode ->
                            ChoiceOption(
                                title = networkModeLabel(mode),
                                subtitle = networkModeSubtitle(mode),
                                onSelect = { onDownloadNetworkModeChanged(mode) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.CloudDownload,
                title = "Concurrent downloads",
                subtitle = "Limit how many download workers can run at the same time.",
                value = uiState.maxConcurrentDownloads.toString(),
                onClick = {
                    val options = listOf(1, 2, 3)
                    choiceDialog = ChoiceDialogState(
                        title = "Concurrent downloads",
                        selected = uiState.maxConcurrentDownloads.toString(),
                        options = options.map { value ->
                            ChoiceOption(
                                title = value.toString(),
                                subtitle = concurrentDownloadsSubtitle(value),
                                onSelect = { onMaxConcurrentDownloadsChanged(value) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsValueRow(
                icon = Icons.Outlined.Description,
                title = "Default audio bitrate",
                subtitle = "Used for future audio-only downloads and extracts.",
                value = "${uiState.audioBitrateKbps} kbps",
                onClick = {
                    val bitrates = listOf(64, 96, 128, 192, 256, 320)
                    choiceDialog = ChoiceDialogState(
                        title = "Default audio bitrate",
                        selected = "${uiState.audioBitrateKbps} kbps",
                        options = bitrates.map { bitrate ->
                            ChoiceOption(
                                title = "$bitrate kbps",
                                subtitle = audioBitrateDescription(bitrate),
                                onSelect = { onDefaultAudioBitrateChanged(bitrate) },
                            )
                        },
                    )
                },
            )
            DividerInset()
            SettingsToggleRow(
                icon = Icons.Outlined.Palette,
                title = "Write thumbnail file",
                subtitle = "Save a separate poster or cover image beside future downloads when the site provides one.",
                checked = uiState.writeThumbnail,
                onCheckedChange = onDefaultWriteThumbnailChanged,
            )
        }

        SettingsListCard {
            SettingsActionRow(
                icon = Icons.Outlined.Refresh,
                title = "Reset all settings",
                subtitle = "Restore appearance, folders, download defaults, and library behavior back to the default setup.",
                onClick = { showResetDialog = true },
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
            SettingsValueRow(
                icon = Icons.Outlined.Refresh,
                title = "Auto cache cleanup",
                subtitle = "Clean older temporary files automatically so the cache does not grow forever.",
                value = cacheCleanupPolicyLabel(uiState.cacheCleanupPolicy),
                onClick = {
                    choiceDialog = ChoiceDialogState(
                        title = "Auto cache cleanup",
                        selected = cacheCleanupPolicyLabel(uiState.cacheCleanupPolicy),
                        options = CacheCleanupPolicy.entries.map { policy ->
                            ChoiceOption(
                                title = cacheCleanupPolicyLabel(policy),
                                subtitle = cacheCleanupPolicySubtitle(policy),
                                onSelect = { onCacheCleanupPolicyChanged(policy) },
                            )
                        },
                    )
                },
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

        SectionLabel("Diagnostics")
        SettingsListCard {
            SettingsActionRow(
                icon = Icons.Outlined.Description,
                title = "Export logs",
                subtitle = "Share the internal app and crash logs when a download needs troubleshooting.",
                onClick = { shareAppLogs(context) },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Delete,
                title = "Clear logs",
                subtitle = "Remove the saved log files after you are done debugging or reporting an issue.",
                onClick = {
                    val freedBytes = clearAppLogs(context)
                    val message = if (freedBytes > 0L) {
                        "Cleared ${formatFileSize(freedBytes)} of logs."
                    } else {
                        "No log files were available to clear."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
            )
            DividerInset()
            SettingsActionRow(
                icon = Icons.Outlined.Code,
                title = "Copy app/runtime info",
                subtitle = "Copy version, device, ABI, and runtime details for bug reports.",
                onClick = onCopyAppRuntimeInfo,
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
                icon = Icons.Outlined.Web,
                title = "Official website",
                subtitle = "video.sandipmaity.me",
                onClick = { openUrl("https://video.sandipmaity.me") },
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
private fun SettingsBrowseValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onEditClick: () -> Unit,
    onBrowseClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
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
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.clickable(onClick = onBrowseClick),
            ) {
                Text(
                    text = "Browse",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilenameTemplateDialog(
    state: FilenameTemplateDialogState,
    onDismiss: () -> Unit,
) {
    var value by remember(state) { mutableStateOf(state.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Filename template") },
                    supportingText = { Text(state.supporting) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Quick presets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.presets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (value.trim() == preset.template) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { value = preset.template },
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = preset.template,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Suggested fields",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.tokens.forEach { token ->
                        FilterChip(
                            selected = value.contains(token),
                            onClick = {
                                value = appendTemplateToken(
                                    template = value,
                                    token = token,
                                )
                            },
                            label = { Text(token) },
                        )
                    }
                }

                Text(
                    text = "Tip: keep %(ext)s somewhere in the template so the saved file keeps the correct extension.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    state.onConfirm(value.trim())
                    onDismiss()
                },
            ) {
                Text("Use template")
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

private data class FilenameTemplateDialogState(
    val title: String,
    val value: String,
    val supporting: String,
    val presets: List<FilenameTemplatePreset>,
    val tokens: List<String>,
    val onConfirm: (String) -> Unit,
)

private data class FilenameTemplatePreset(
    val title: String,
    val template: String,
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
        AccentPreset.COBALT -> "Cobalt"
        AccentPreset.AQUA -> "Aqua"
        AccentPreset.TEAL -> "Teal"
        AccentPreset.MINT -> "Mint"
        AccentPreset.EMERALD -> "Emerald"
        AccentPreset.FOREST -> "Green"
        AccentPreset.ROSE -> "Rose"
        AccentPreset.CRIMSON -> "Crimson"
        AccentPreset.MAGENTA -> "Magenta"
        AccentPreset.PURPLE -> "Purple"
        AccentPreset.YELLOW -> "Yellow"
        AccentPreset.ORANGE -> "Orange"
        AccentPreset.COPPER -> "Copper"
        AccentPreset.MONOCHROME -> "Monochrome"
    }
}

private fun accentSubtitle(accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> "A warm default with the soft amber look already used by the app."
        AccentPreset.OCEAN -> "Cool blue highlights for a calmer downloader mood."
        AccentPreset.COBALT -> "A deeper electric blue with stronger player and action contrast."
        AccentPreset.AQUA -> "Bright aqua accents with a cleaner, glassier utility feel."
        AccentPreset.TEAL -> "Blue-green accents that feel crisp, modern, and a little lighter."
        AccentPreset.MINT -> "Fresh mint accents for a softer, cleaner utility look."
        AccentPreset.EMERALD -> "A richer jewel-green palette with stronger contrast than mint."
        AccentPreset.FOREST -> "A greener look with a softer natural feel."
        AccentPreset.ROSE -> "Warm rose accents for a brighter and friendlier red tone."
        AccentPreset.CRIMSON -> "A richer red tone with more drama than the standard rose theme."
        AccentPreset.MAGENTA -> "Bold magenta highlights for a more vivid music and creator vibe."
        AccentPreset.PURPLE -> "A richer violet palette for a more dramatic music vibe."
        AccentPreset.YELLOW -> "Bright yellow accents with higher energy."
        AccentPreset.ORANGE -> "Warm orange action tones similar to media apps."
        AccentPreset.COPPER -> "Copper-orange accents that feel warmer and more grounded than amber."
        AccentPreset.MONOCHROME -> "Muted grayscale accents for a cleaner neutral setup."
    }
}

private fun contrastLabel(mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.SOFT -> "Soft"
        ContrastMode.STANDARD -> "Standard"
        ContrastMode.HIGH -> "High contrast"
        ContrastMode.ULTRA -> "Ultra contrast"
    }
}

private fun contrastSubtitle(mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.SOFT -> "Gentler surfaces and softer separation for a calmer look."
        ContrastMode.STANDARD -> "Balanced contrast for the normal theme surfaces."
        ContrastMode.HIGH -> "Sharper text and stronger separation between cards and background."
        ContrastMode.ULTRA -> "Maximum separation for the clearest edges and strongest readability."
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

private fun networkModeLabel(mode: DownloadNetworkMode): String {
    return when (mode) {
        DownloadNetworkMode.ANY -> "Any network"
        DownloadNetworkMode.WIFI_ONLY -> "Wi-Fi only"
        DownloadNetworkMode.UNMETERED -> "Unmetered only"
    }
}

private fun networkModeSubtitle(mode: DownloadNetworkMode): String {
    return when (mode) {
        DownloadNetworkMode.ANY -> "Start downloads on mobile data, Wi-Fi, or any other connected network."
        DownloadNetworkMode.WIFI_ONLY -> "Only start queued downloads when the device is on Wi-Fi or ethernet."
        DownloadNetworkMode.UNMETERED -> "Allow only networks Android marks as unmetered, which helps avoid data charges."
    }
}

private fun concurrentDownloadsSubtitle(value: Int): String {
    return when (value) {
        1 -> "Run one download at a time for the safest bandwidth and battery use."
        2 -> "A balanced default that keeps the queue moving without overloading most connections."
        else -> "Move the queue faster when your network and storage are comfortable with heavier parallel work."
    }
}

private fun audioBitrateDescription(bitrate: Int): String {
    return when (bitrate) {
        64 -> "Smallest audio files, best when size matters more than detail."
        96 -> "Lightweight audio that still sounds fine for speech and casual listening."
        128 -> "A practical balance for everyday listening and smaller file sizes."
        192 -> "A cleaner default for music with broader quality headroom."
        256 -> "Higher-quality audio for users who want more detail without going lossless."
        else -> "Largest lossy option for users who want to keep as much detail as possible."
    }
}

private fun cacheCleanupPolicyLabel(policy: CacheCleanupPolicy): String {
    return when (policy) {
        CacheCleanupPolicy.NEVER -> "Never"
        CacheCleanupPolicy.ONE_DAY -> "After 1 day"
        CacheCleanupPolicy.THREE_DAYS -> "After 3 days"
        CacheCleanupPolicy.SEVEN_DAYS -> "After 7 days"
        CacheCleanupPolicy.THIRTY_DAYS -> "After 30 days"
    }
}

private fun cacheCleanupPolicySubtitle(policy: CacheCleanupPolicy): String {
    return when (policy) {
        CacheCleanupPolicy.NEVER -> "Keep cached analysis and processing files until you clear them manually."
        CacheCleanupPolicy.ONE_DAY -> "Aggressively clear old temporary files for tighter storage control."
        CacheCleanupPolicy.THREE_DAYS -> "Clean old cache fairly often while still allowing short-term reuse."
        CacheCleanupPolicy.SEVEN_DAYS -> "A balanced cleanup window for regular use."
        CacheCleanupPolicy.THIRTY_DAYS -> "Keep reusable cache around longer before older files are removed."
    }
}

private fun audioFormatDescription(format: String): String {
    return when (format) {
        "mp3" -> "The broadest device and car-player compatibility."
        "m4a" -> "AAC audio in a compact container that works well on most phones."
        "aac" -> "Raw AAC output for lighter files when you need a simpler audio stream."
        "opus" -> "High efficiency audio that is great when the source already supports it."
        "flac" -> "Lossless output when you want to preserve as much audio quality as possible."
        "wav" -> "Large but simple audio files that work well in editors."
        else -> "Use this format for future audio extracts."
    }
}

private fun videoFilenameTemplatePresets(): List<FilenameTemplatePreset> {
    return listOf(
        FilenameTemplatePreset(
            title = "Title and ID",
            template = "%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Uploader and title",
            template = "%(uploader)s - %(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Playlist-friendly",
            template = "%(playlist_index,playlist_autonumber&{}. |)s%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Date first",
            template = "%(upload_date>%Y-%m-%d)s - %(title)s [%(id)s].%(ext)s",
        ),
    )
}

private fun audioFilenameTemplatePresets(): List<FilenameTemplatePreset> {
    return listOf(
        FilenameTemplatePreset(
            title = "Title and ID",
            template = "%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Artist and title",
            template = "%(artist,uploader)s - %(title)s.%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Album track",
            template = "%(album,uploader)s/%(track_number,playlist_index&{}. )s%(title)s.%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Date and title",
            template = "%(release_date,upload_date>%Y-%m-%d)s - %(title)s.%(ext)s",
        ),
    )
}

private fun suggestedFilenameTokens(): List<String> {
    return listOf(
        "%(title)s",
        "%(uploader)s",
        "%(artist)s",
        "%(album)s",
        "%(track)s",
        "%(playlist_index,playlist_autonumber&{}. |)s",
        "%(upload_date>%Y-%m-%d)s",
        "%(release_date>%Y-%m-%d)s",
        "%(duration_string)s",
        "%(id)s",
        "%(ext)s",
    )
}

private fun appendTemplateToken(template: String, token: String): String {
    val trimmed = template.trimEnd()
    if (trimmed.isBlank()) return token
    val separator = when {
        trimmed.endsWith("/") -> ""
        trimmed.endsWith("\\") -> ""
        trimmed.endsWith("-") -> " "
        trimmed.endsWith("_") -> ""
        trimmed.endsWith("[") -> ""
        trimmed.endsWith("(") -> ""
        else -> " "
    }
    return trimmed + separator + token
}

private fun String.cleanPreview(): String {
    return trim().ifBlank { "Default" }
}

private fun String.folderPreview(defaultLabel: String): String {
    return trim().ifBlank { defaultLabel }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
