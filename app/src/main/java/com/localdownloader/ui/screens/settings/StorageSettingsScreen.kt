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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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
            PreferenceGroup {
                FolderPreferenceRow(
                    title = stringResource(R.string.storage_root_title),
                    subtitle = stringResource(R.string.storage_root_subtitle),
                    value = uiState.downloadsRootFolderName.folderPreview(
                        stringResource(R.string.storage_default_root),
                    ),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
                            title = context.getString(R.string.storage_root_title),
                            value = uiState.downloadsRootFolderName,
                            label = context.getString(R.string.storage_folder_label_root),
                            supporting = context.getString(R.string.storage_root_supporting),
                            confirmLabel = context.getString(R.string.common_save),
                            onConfirm = onDownloadsRootFolderNameChanged,
                        )
                    },
                    onBrowseClick = onBrowseDownloadsRootFolder,
                )
                PreferenceDivider()
                FolderPreferenceRow(
                    title = stringResource(R.string.storage_video_title),
                    subtitle = stringResource(R.string.storage_video_subtitle),
                    value = uiState.videoSubfolderName.folderPreview(
                        stringResource(R.string.storage_downloads_root),
                    ),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
                            title = context.getString(R.string.storage_video_title),
                            value = uiState.videoSubfolderName,
                            label = context.getString(R.string.storage_folder_label_inside_root),
                            supporting = context.getString(R.string.storage_video_supporting),
                            confirmLabel = context.getString(R.string.common_save),
                            onConfirm = onVideoSubfolderNameChanged,
                        )
                    },
                    onBrowseClick = onBrowseVideoFolder,
                )
                PreferenceDivider()
                FolderPreferenceRow(
                    title = stringResource(R.string.storage_audio_title),
                    subtitle = stringResource(R.string.storage_audio_subtitle),
                    value = uiState.audioSubfolderName.folderPreview(
                        stringResource(R.string.storage_downloads_root),
                    ),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
                            title = context.getString(R.string.storage_audio_title),
                            value = uiState.audioSubfolderName,
                            label = context.getString(R.string.storage_folder_label_inside_root),
                            supporting = context.getString(R.string.storage_audio_supporting),
                            confirmLabel = context.getString(R.string.common_save),
                            onConfirm = onAudioSubfolderNameChanged,
                        )
                    },
                    onBrowseClick = onBrowseAudioFolder,
                )
                PreferenceDivider()
                FolderPreferenceRow(
                    title = stringResource(R.string.storage_other_title),
                    subtitle = stringResource(R.string.storage_other_subtitle),
                    value = uiState.otherSubfolderName.folderPreview(
                        stringResource(R.string.storage_downloads_root),
                    ),
                    onEditClick = {
                        textDialog = SettingTextDialogState(
                            title = context.getString(R.string.storage_other_title),
                            value = uiState.otherSubfolderName,
                            label = context.getString(R.string.storage_folder_label_inside_root),
                            supporting = context.getString(R.string.storage_other_supporting),
                            confirmLabel = context.getString(R.string.common_save),
                            onConfirm = onOtherSubfolderNameChanged,
                        )
                    },
                    onBrowseClick = onBrowseOtherFolder,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Refresh,
                    title = stringResource(R.string.storage_reset_folders_title),
                    subtitle = stringResource(R.string.storage_reset_folders_subtitle),
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
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.AutoDelete,
                    title = stringResource(R.string.storage_auto_remove_title),
                    subtitle = stringResource(R.string.storage_auto_remove_subtitle),
                    checked = uiState.autoRemoveMissingFilesFromLibrary,
                    onCheckedChange = onAutoRemoveMissingFilesFromLibraryChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.storage_delete_storage_title),
                    subtitle = stringResource(R.string.storage_delete_storage_subtitle),
                    checked = uiState.deleteFromStorageWhenRemovedInApp,
                    onCheckedChange = onDeleteFromStorageWhenRemovedInAppChanged,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.LibraryBooks,
                    title = stringResource(R.string.storage_saved_items_title),
                    subtitle = stringResource(R.string.storage_saved_items_subtitle),
                    value = savedItemsCount.toString(),
                    onClick = null,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Storage,
                    title = stringResource(R.string.storage_temp_cache_title),
                    subtitle = stringResource(R.string.storage_temp_cache_subtitle),
                    value = formatFileSize(cacheSize),
                    onClick = null,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.CleaningServices,
                    title = stringResource(R.string.storage_clear_cache_title),
                    subtitle = stringResource(R.string.storage_clear_cache_subtitle),
                    onClick = onClearCache,
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.storage_clear_app_list_title),
                    subtitle = stringResource(R.string.storage_clear_app_list_subtitle),
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = context.getString(R.string.storage_clear_app_list_dialog_title),
                            body = context.getString(R.string.storage_clear_app_list_dialog_body),
                            confirmLabel = context.getString(R.string.common_remove_entries),
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
                    title = stringResource(R.string.storage_delete_all_title),
                    subtitle = stringResource(R.string.storage_delete_all_subtitle),
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = context.getString(R.string.storage_delete_all_dialog_title),
                            body = context.getString(R.string.storage_delete_all_dialog_body),
                            confirmLabel = context.getString(R.string.common_delete_all),
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
                    title = stringResource(R.string.storage_reset_title),
                    subtitle = stringResource(R.string.storage_reset_subtitle),
                    onClick = {
                        confirmDialog = SettingConfirmDialogState(
                            title = context.getString(R.string.storage_reset_dialog_title),
                            body = context.getString(R.string.storage_reset_dialog_body),
                            confirmLabel = context.getString(R.string.common_reset_now),
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
                    text = stringResource(R.string.common_browse),
                    onClick = onBrowseClick,
                )
            }
        },
    )
}
