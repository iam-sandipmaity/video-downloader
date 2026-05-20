package com.localdownloader.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferencePillButton
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader
import com.localdownloader.ui.components.PreferenceSwitchRow
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun StorageSettingsScreen(
    uiState: FormatUiState,
    savedItemsCount: Int,
    mediaInfoMessage: String? = null,
    mediaErrorMessage: String? = null,
    onDismissMediaLibraryMessage: () -> Unit = {},
    onDownloadsRootFolderNameChanged: (String) -> Unit,
    onVideoSubfolderNameChanged: (String) -> Unit,
    onAudioSubfolderNameChanged: (String) -> Unit,
    onOtherSubfolderNameChanged: (String) -> Unit,
    onBrowseDownloadsRootFolder: () -> Unit,
    onBrowseVideoFolder: () -> Unit,
    onBrowseAudioFolder: () -> Unit,
    onBrowseOtherFolder: () -> Unit,
    onAutoRemoveMissingFilesFromLibraryChanged: (Boolean) -> Unit,
    onDeleteFromStorageWhenRemovedInAppChanged: (Boolean) -> Unit,
    onClearVideoTabEntries: () -> Unit,
    onDeleteAllSavedMedia: () -> Unit,
    onResetSettings: () -> Unit,
    onClearCache: () -> Unit,
    cacheSize: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settingsInfoMessage = uiState.infoMessageFor(FormatMessageScope.SETTINGS)
    val settingsErrorMessage = uiState.errorMessageFor(FormatMessageScope.SETTINGS)
    val defaults = remember { com.localdownloader.domain.models.AppSettings() }

    var textDialog by remember { mutableStateOf<SettingTextDialogState?>(null) }
    var confirmDialog by remember { mutableStateOf<SettingConfirmDialogState?>(null) }

    textDialog?.let { state ->
        SettingTextDialog(
            state = state,
            onDismiss = { textDialog = null },
        )
    }
    confirmDialog?.let { state ->
        SettingConfirmDialog(
            state = state,
            onDismiss = { confirmDialog = null },
        )
    }

    PreferencePageScaffold(
        title = "Folders and storage",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceHeroCard(
                eyebrow = "Keep it tidy",
                title = "Control where files land and how cleanup should feel",
                subtitle = "These storage preferences keep the app organized without forcing you into a rigid folder scheme.",
                badges = listOf(
                    uiState.downloadsRootFolderName.folderPreview("Default root"),
                    "${savedItemsCount} saved",
                    formatFileSize(cacheSize),
                ),
            )
        }
        if (!settingsInfoMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = "Settings",
                    message = settingsInfoMessage,
                    isError = false,
                )
            }
        }
        if (!settingsErrorMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = "Settings",
                    message = settingsErrorMessage,
                    isError = true,
                )
            }
        }
        if (!mediaInfoMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = "Library",
                    message = mediaInfoMessage,
                    isError = false,
                    onDismiss = onDismissMediaLibraryMessage,
                )
            }
        }
        if (!mediaErrorMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = "Library",
                    message = mediaErrorMessage,
                    isError = true,
                    onDismiss = onDismissMediaLibraryMessage,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Folders",
                subtitle = "Type paths manually or browse inside Downloads when you want a more visual flow.",
            )
        }
        item {
            PreferenceGroup {
                FolderPreferenceRow(
                    title = "Downloads root",
                    subtitle = "Type a folder path under Downloads or browse to a subfolder you want the app to use as its main root.",
                    value = uiState.downloadsRootFolderName.folderPreview("Default root"),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
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
                PreferenceDivider()
                FolderPreferenceRow(
                    title = "Video folder",
                    subtitle = "Type a subfolder path or browse to a folder inside the current downloads root for videos.",
                    value = uiState.videoSubfolderName.folderPreview("Downloads root"),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
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
                PreferenceDivider()
                FolderPreferenceRow(
                    title = "Audio folder",
                    subtitle = "Type a subfolder path or browse to a folder inside the current downloads root for audio.",
                    value = uiState.audioSubfolderName.folderPreview("Downloads root"),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
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
                PreferenceDivider()
                FolderPreferenceRow(
                    title = "Other files folder",
                    subtitle = "Type a subfolder path or browse to a folder inside the current downloads root for anything else.",
                    value = uiState.otherSubfolderName.folderPreview("Downloads root"),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
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
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Refresh,
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
        }
        item {
            PreferenceSectionHeader(
                title = "Library behavior",
                subtitle = "Choose how the app should react when files disappear or when you clean up saved items from inside the UI.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.AutoDelete,
                    title = "Auto-remove missing files",
                    subtitle = "Clean broken library entries when files disappear outside the app.",
                    checked = uiState.autoRemoveMissingFilesFromLibrary,
                    onCheckedChange = onAutoRemoveMissingFilesFromLibraryChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Delete from storage when removed in app",
                    subtitle = "When you remove an item here, also delete the real device file.",
                    checked = uiState.deleteFromStorageWhenRemovedInApp,
                    onCheckedChange = onDeleteFromStorageWhenRemovedInAppChanged,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.LibraryBooks,
                    title = "Saved items",
                    subtitle = "Completed downloads currently tracked in the library.",
                    value = savedItemsCount.toString(),
                    onClick = null,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Storage,
                    title = "Temporary cache",
                    subtitle = "Reusable temporary files created during analysis and processing.",
                    value = formatFileSize(cacheSize),
                    onClick = null,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Maintenance",
                subtitle = "Cleanup tools are separated so you can clear temporary clutter without touching real saved media unless you choose to.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.CleaningServices,
                    title = "Clear cache",
                    subtitle = "Remove temporary files without touching your saved downloads.",
                    onClick = onClearCache,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Clear app list",
                    subtitle = "Remove completed-library entries but keep the actual files on the device.",
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = "Remove saved items from app",
                            body = "This clears the library entries inside the app and leaves the original files on the device untouched.",
                            confirmLabel = "Remove entries",
                            onConfirm = {
                                onClearVideoTabEntries()
                                confirmDialog = null
                            },
                        )
                    },
                    enabled = savedItemsCount > 0,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Delete all saved media",
                    subtitle = "Permanently remove downloaded files from both the library and storage.",
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = "Delete all saved media",
                            body = "This permanently removes downloaded files from the app and device storage.",
                            confirmLabel = "Delete all",
                            destructive = true,
                            onConfirm = {
                                onDeleteAllSavedMedia()
                                confirmDialog = null
                            },
                        )
                    },
                    enabled = savedItemsCount > 0,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Tune,
                    title = "Reset all settings",
                    subtitle = "Restore appearance, folders, download defaults, access preferences, and library behavior to the default setup.",
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = "Reset settings",
                            body = "This restores appearance, folders, download defaults, and library behavior back to the default setup.",
                            confirmLabel = "Reset now",
                            onConfirm = {
                                onResetSettings()
                                confirmDialog = null
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderPreferenceRow(
    title: String,
    subtitle: String,
    value: String,
    onEditClick: () -> Unit,
    onBrowseClick: () -> Unit,
) {
    PreferenceRow(
        icon = Icons.Rounded.Folder,
        title = title,
        subtitle = subtitle,
        onClick = onEditClick,
        trailing = {
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
                PreferencePillButton(
                    text = "Browse",
                    onClick = onBrowseClick,
                )
            }
        },
    )
}
