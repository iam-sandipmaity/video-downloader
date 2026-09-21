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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.localdownloader.R
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferencePillButton
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSwitchRow
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun StorageSettingsScreen(
    uiState: FormatUiState,
    savedItemsCount: Int,
    duplicateSavedItemsCount: Int,
    availableStorageBytes: Long,
    modifier: Modifier = Modifier,
    mediaInfoMessage: String? = null,
    mediaErrorMessage: String? = null,
    onDismissMediaLibraryMessage: () -> Unit = {},
    onDownloadsRootFolderNameChanged: (String) -> Unit,
    onDownloadsRootPublicPathChanged: (String) -> Unit,
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
) {
    val settingsInfoMessage = uiState.infoMessageFor(FormatMessageScope.SETTINGS)
    val settingsErrorMessage = uiState.errorMessageFor(FormatMessageScope.SETTINGS)
    val defaults = remember { com.localdownloader.domain.models.AppSettings() }
    val commonSaveLabel = stringResource(R.string.common_save)
    val storageRootTitle = stringResource(R.string.storage_root_title)
    val storageFolderLabelRoot = stringResource(R.string.storage_folder_label_root)
    val storageRootSupporting = stringResource(R.string.storage_root_supporting)
    val storageVideoTitle = stringResource(R.string.storage_video_title)
    val storageFolderLabelInsideRoot = stringResource(R.string.storage_folder_label_inside_root)
    val storageVideoSupporting = stringResource(R.string.storage_video_supporting)
    val storageAudioTitle = stringResource(R.string.storage_audio_title)
    val storageAudioSupporting = stringResource(R.string.storage_audio_supporting)
    val storageOtherTitle = stringResource(R.string.storage_other_title)
    val storageOtherSupporting = stringResource(R.string.storage_other_supporting)
    val storageClearAppListDialogTitle = stringResource(R.string.storage_clear_app_list_dialog_title)
    val storageClearAppListDialogBody = stringResource(R.string.storage_clear_app_list_dialog_body)
    val commonRemoveEntriesLabel = stringResource(R.string.common_remove_entries)
    val storageDeleteAllDialogTitle = stringResource(R.string.storage_delete_all_dialog_title)
    val storageDeleteAllDialogBody = stringResource(R.string.storage_delete_all_dialog_body)
    val commonDeleteAllLabel = stringResource(R.string.common_delete_all)
    val storageResetDialogTitle = stringResource(R.string.storage_reset_dialog_title)
    val storageResetDialogBody = stringResource(R.string.storage_reset_dialog_body)
    val commonResetNowLabel = stringResource(R.string.common_reset_now)

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
        title = stringResource(R.string.settings_storage_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        if (!settingsInfoMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.settings_feedback_label),
                    message = settingsInfoMessage,
                    isError = false,
                )
            }
        }
        if (!settingsErrorMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.settings_feedback_label),
                    message = settingsErrorMessage,
                    isError = true,
                )
            }
        }
        if (!mediaInfoMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.settings_library_label),
                    message = mediaInfoMessage,
                    isError = false,
                    onDismiss = onDismissMediaLibraryMessage,
                )
            }
        }
        if (!mediaErrorMessage.isNullOrBlank()) {
            item {
                InlineFeedbackCard(
                    label = stringResource(R.string.settings_library_label),
                    message = mediaErrorMessage,
                    isError = true,
                    onDismiss = onDismissMediaLibraryMessage,
                )
            }
        }
        item {
            PreferenceSubtitle(text = "DOWNLOAD DIRECTORIES")
        }
        item {
            FolderPreferenceRow(
                title = stringResource(R.string.storage_root_title),
                value = uiState.downloadsRootDisplayPath(
                    defaultLabel = stringResource(R.string.storage_default_root),
                ),
                onEditClick = {
                    textDialog = SettingTextDialogState(
                        title = storageRootTitle,
                        value = uiState.downloadsRootPublicPath.ifBlank { uiState.downloadsRootFolderName },
                        label = storageFolderLabelRoot,
                        supporting = storageRootSupporting,
                        confirmLabel = commonSaveLabel,
                        onConfirm = { value ->
                            if (value.isPublicStorageRootPath()) {
                                onDownloadsRootPublicPathChanged(value)
                            } else {
                                onDownloadsRootFolderNameChanged(value)
                            }
                        },
                    )
                },
                onBrowseClick = onBrowseDownloadsRootFolder,
            )
        }
        item {
            FolderPreferenceRow(
                title = stringResource(R.string.storage_video_title),
                value = uiState.videoSubfolderName.folderPreview(
                    stringResource(R.string.storage_downloads_root),
                ),
                onEditClick = {
                    textDialog = SettingTextDialogState(
                        title = storageVideoTitle,
                        value = uiState.videoSubfolderName,
                        label = storageFolderLabelInsideRoot,
                        supporting = storageVideoSupporting,
                        confirmLabel = commonSaveLabel,
                        onConfirm = onVideoSubfolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseVideoFolder,
            )
        }
        item {
            FolderPreferenceRow(
                title = stringResource(R.string.storage_audio_title),
                value = uiState.audioSubfolderName.folderPreview(
                    stringResource(R.string.storage_downloads_root),
                ),
                onEditClick = {
                    textDialog = SettingTextDialogState(
                        title = storageAudioTitle,
                        value = uiState.audioSubfolderName,
                        label = storageFolderLabelInsideRoot,
                        supporting = storageAudioSupporting,
                        confirmLabel = commonSaveLabel,
                        onConfirm = onAudioSubfolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseAudioFolder,
            )
        }
        item {
            FolderPreferenceRow(
                title = stringResource(R.string.storage_other_title),
                value = uiState.otherSubfolderName.folderPreview(
                    stringResource(R.string.storage_downloads_root),
                ),
                onEditClick = {
                    textDialog = SettingTextDialogState(
                        title = storageOtherTitle,
                        value = uiState.otherSubfolderName,
                        label = storageFolderLabelInsideRoot,
                        supporting = storageOtherSupporting,
                        confirmLabel = commonSaveLabel,
                        onConfirm = onOtherSubfolderNameChanged,
                    )
                },
                onBrowseClick = onBrowseOtherFolder,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Refresh,
                title = stringResource(R.string.storage_reset_folders_title),
                onClick = {
                    onDownloadsRootFolderNameChanged(defaults.downloadsRootFolderName)
                    onVideoSubfolderNameChanged(defaults.videoSubfolderName)
                    onAudioSubfolderNameChanged(defaults.audioSubfolderName)
                    onOtherSubfolderNameChanged(defaults.otherSubfolderName)
                },
            )
        }
        item {
            PreferenceSubtitle(text = "MEDIA LIBRARY & STORAGE")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.AutoDelete,
                title = stringResource(R.string.storage_auto_remove_title),
                description = stringResource(R.string.storage_auto_remove_subtitle),
                isChecked = uiState.autoRemoveMissingFilesFromLibrary,
                onClick = { onAutoRemoveMissingFilesFromLibraryChanged(!uiState.autoRemoveMissingFilesFromLibrary) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.DeleteSweep,
                title = stringResource(R.string.storage_delete_storage_title),
                description = stringResource(R.string.storage_delete_storage_subtitle),
                isChecked = uiState.deleteFromStorageWhenRemovedInApp,
                onClick = { onDeleteFromStorageWhenRemovedInAppChanged(!uiState.deleteFromStorageWhenRemovedInApp) },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.LibraryBooks,
                title = stringResource(R.string.storage_saved_items_title),
                value = savedItemsCount.toString(),
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.LibraryBooks,
                title = "Possible duplicates",
                value = duplicateSavedItemsCount.toString(),
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Storage,
                title = "Available storage",
                value = formatFileSize(availableStorageBytes),
                onClick = null,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Storage,
                title = stringResource(R.string.storage_temp_cache_title),
                value = formatFileSize(cacheSize),
                onClick = null,
            )
        }
        item {
            PreferenceSubtitle(text = "CLEANUP")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.CleaningServices,
                title = stringResource(R.string.storage_clear_cache_title),
                description = stringResource(R.string.storage_clear_cache_subtitle),
                onClick = onClearCache,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.DeleteSweep,
                title = stringResource(R.string.storage_clear_app_list_title),
                description = stringResource(R.string.storage_clear_app_list_subtitle),
                onClick = {
                    confirmDialog = SettingConfirmDialogState(
                        title = storageClearAppListDialogTitle,
                        body = storageClearAppListDialogBody,
                        confirmLabel = commonRemoveEntriesLabel,
                        onConfirm = {
                            onClearVideoTabEntries()
                            confirmDialog = null
                        },
                    )
                },
                enabled = savedItemsCount > 0,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.DeleteSweep,
                title = stringResource(R.string.storage_delete_all_title),
                description = stringResource(R.string.storage_delete_all_subtitle),
                onClick = {
                    confirmDialog = SettingConfirmDialogState(
                        title = storageDeleteAllDialogTitle,
                        body = storageDeleteAllDialogBody,
                        confirmLabel = commonDeleteAllLabel,
                        destructive = true,
                        onConfirm = {
                            onDeleteAllSavedMedia()
                            confirmDialog = null
                        },
                    )
                },
                enabled = savedItemsCount > 0,
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.storage_reset_title),
                description = stringResource(R.string.storage_reset_subtitle),
                onClick = {
                    confirmDialog = SettingConfirmDialogState(
                        title = storageResetDialogTitle,
                        body = storageResetDialogBody,
                        confirmLabel = commonResetNowLabel,
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

private fun FormatUiState.downloadsRootDisplayPath(defaultLabel: String): String {
    return downloadsRootPublicPath
        .ifBlank { "Download/${downloadsRootFolderName.folderPreview(defaultLabel)}" }
        .folderPreview(defaultLabel)
}

private fun String.isPublicStorageRootPath(): Boolean {
    val firstSegment = trim()
        .replace('\\', '/')
        .trim('/')
        .substringBefore('/')
        .lowercase()
    return firstSegment in setOf(
        "download",
        "downloads",
        "documents",
        "movies",
        "music",
        "pictures",
        "dcim",
        "audiobooks",
        "podcasts",
        "ringtones",
    )
}

@Composable
private fun FolderPreferenceRow(
    title: String,
    value: String,
    onEditClick: () -> Unit,
    onBrowseClick: () -> Unit,
) {
    PreferenceRow(
        icon = Icons.Rounded.Folder,
        title = title,
        subtitle = value,
        onClick = onEditClick,
        trailing = {
            PreferencePillButton(
                text = stringResource(R.string.common_browse),
                onClick = onBrowseClick,
            )
        },
    )
}
