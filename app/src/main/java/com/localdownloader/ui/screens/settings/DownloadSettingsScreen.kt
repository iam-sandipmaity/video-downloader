package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSwitchRow
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun DownloadSettingsScreen(
    uiState: FormatUiState,
    onDefaultVideoOutputTemplateChanged: (String) -> Unit,
    onDefaultAudioOutputTemplateChanged: (String) -> Unit,
    onDefaultVideoContainerChanged: (String) -> Unit,
    onDefaultAudioContainerChanged: (String) -> Unit,
    onDefaultDownloadSubtitlesChanged: (Boolean) -> Unit,
    onDefaultEmbedSubtitlesChanged: (Boolean) -> Unit,
    onDefaultEmbedMetadataChanged: (Boolean) -> Unit,
    onDefaultEmbedThumbnailChanged: (Boolean) -> Unit,
    onMaxConcurrentDownloadsChanged: (Int) -> Unit,
    onKeepAnalyzedLinkHistoryChanged: (Boolean) -> Unit,
    onAnalyzedLinkHistoryRetentionDaysChanged: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }
    var filenameTemplateDialog by remember { mutableStateOf<FilenameTemplateDialogState?>(null) }
    val videoFilenameTitle = stringResource(R.string.download_defaults_filename_video_title)
    val audioFilenameTitle = stringResource(R.string.download_defaults_filename_audio_title)
    val filenameSupporting = stringResource(R.string.download_defaults_filename_supporting)
    val videoContainerTitle = stringResource(R.string.download_defaults_video_container_title)
    val audioContainerTitle = stringResource(R.string.download_defaults_audio_container_title)
    val concurrentTitle = stringResource(R.string.download_defaults_concurrent_title)
    val slotSubtitles = mapOf(
        1 to stringResource(R.string.download_defaults_slot_1),
        2 to stringResource(R.string.download_defaults_slot_2),
        3 to stringResource(R.string.download_defaults_slot_3),
        4 to stringResource(R.string.download_defaults_slot_4),
    )
    val retentionTitle = stringResource(R.string.download_defaults_retention_title)
    val retentionChoices = listOf(1, 3, 7, 15, 30, 90)
    val retentionLabels = retentionChoices.associateWith { days ->
        pluralStringResource(R.plurals.common_days, days, days)
    }
    val retentionSubtitles = mapOf(
        1 to stringResource(R.string.download_defaults_retention_1),
        3 to stringResource(R.string.download_defaults_retention_3),
        7 to stringResource(R.string.download_defaults_retention_7),
        15 to stringResource(R.string.download_defaults_retention_15),
        30 to stringResource(R.string.download_defaults_retention_30),
        90 to stringResource(R.string.download_defaults_retention_90),
    )
    val selectedRetentionLabel = pluralStringResource(
        R.plurals.common_days,
        uiState.appSettings.analyzedLinkHistoryRetentionDays,
        uiState.appSettings.analyzedLinkHistoryRetentionDays,
    )

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }
    filenameTemplateDialog?.let { state ->
        FilenameTemplateDialog(
            state = state,
            onDismiss = { filenameTemplateDialog = null },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.settings_download_defaults_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.download_defaults_filename_video_title),
                    subtitle = stringResource(R.string.download_defaults_filename_video_subtitle),
                    value = uiState.outputTemplate,
                    onClick = {
                        filenameTemplateDialog = FilenameTemplateDialogState(
                            title = videoFilenameTitle,
                            value = uiState.outputTemplate,
                            supporting = filenameSupporting,
                            presets = videoFilenameTemplatePresets(context),
                            tokens = suggestedFilenameTokens(),
                            onConfirm = onDefaultVideoOutputTemplateChanged,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.AudioFile,
                    title = stringResource(R.string.download_defaults_filename_audio_title),
                    subtitle = stringResource(R.string.download_defaults_filename_audio_subtitle),
                    value = uiState.audioOutputTemplate,
                    onClick = {
                        filenameTemplateDialog = FilenameTemplateDialogState(
                            title = audioFilenameTitle,
                            value = uiState.audioOutputTemplate,
                            supporting = filenameSupporting,
                            presets = audioFilenameTemplatePresets(context),
                            tokens = suggestedFilenameTokens(),
                            onConfirm = onDefaultAudioOutputTemplateChanged,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.VideoFile,
                    title = stringResource(R.string.download_defaults_video_container_title),
                    subtitle = stringResource(R.string.download_defaults_video_container_subtitle),
                    value = containerDisplayLabel(context, uiState.selectedContainer),
                    onClick = {
                        val containers = listOf("auto", "mp4", "webm", "mkv", "mov")
                        choiceDialog = SettingChoiceDialogState(
                            title = videoContainerTitle,
                            selected = containerDisplayLabel(context, uiState.selectedContainer),
                            options = containers.map { container ->
                                SettingChoiceOption(
                                    title = containerDisplayLabel(context, container),
                                    subtitle = containerDescription(context, container),
                                    onSelect = { onDefaultVideoContainerChanged(container) },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.AudioFile,
                    title = stringResource(R.string.download_defaults_audio_container_title),
                    subtitle = stringResource(R.string.download_defaults_audio_container_subtitle),
                    value = uiState.selectedAudioFormat.uppercase(),
                    onClick = {
                        val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
                        choiceDialog = SettingChoiceDialogState(
                            title = audioContainerTitle,
                            selected = uiState.selectedAudioFormat.uppercase(),
                            options = audioFormats.map { format ->
                                SettingChoiceOption(
                                    title = format.uppercase(),
                                    subtitle = audioFormatDescription(context, format),
                                    onSelect = { onDefaultAudioContainerChanged(format) },
                                )
                            },
                        )
                    },
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = stringResource(R.string.download_defaults_subtitles_title),
                    subtitle = stringResource(R.string.download_defaults_subtitles_subtitle),
                    checked = uiState.downloadSubtitles,
                    onCheckedChange = onDefaultDownloadSubtitlesChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = stringResource(R.string.download_defaults_embed_subtitles_title),
                    subtitle = stringResource(R.string.download_defaults_embed_subtitles_subtitle),
                    checked = uiState.embedSubtitles,
                    onCheckedChange = onDefaultEmbedSubtitlesChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.download_defaults_embed_metadata_title),
                    subtitle = stringResource(R.string.download_defaults_embed_metadata_subtitle),
                    checked = uiState.embedMetadata,
                    onCheckedChange = onDefaultEmbedMetadataChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.PhotoSizeSelectActual,
                    title = stringResource(R.string.download_defaults_embed_thumbnail_title),
                    subtitle = stringResource(R.string.download_defaults_embed_thumbnail_subtitle),
                    checked = uiState.embedThumbnail,
                    onCheckedChange = onDefaultEmbedThumbnailChanged,
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Queue,
                    title = stringResource(R.string.download_defaults_concurrent_title),
                    subtitle = stringResource(R.string.download_defaults_concurrent_subtitle),
                    value = uiState.maxConcurrentDownloads.toString(),
                    onClick = {
                        val slotChoices = (1..4).map { slotCount ->
                            SettingChoiceOption(
                                title = slotCount.toString(),
                                subtitle = slotSubtitles.getValue(slotCount),
                                onSelect = { onMaxConcurrentDownloadsChanged(slotCount) },
                            )
                        }
                        choiceDialog = SettingChoiceDialogState(
                            title = concurrentTitle,
                            selected = uiState.maxConcurrentDownloads.toString(),
                            options = slotChoices,
                        )
                    },
                )
            }
        }
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Description,
                    title = stringResource(R.string.download_defaults_keep_links_title),
                    subtitle = stringResource(R.string.download_defaults_keep_links_subtitle),
                    checked = uiState.appSettings.keepAnalyzedLinkHistory,
                    onCheckedChange = onKeepAnalyzedLinkHistoryChanged,
                )
                if (uiState.appSettings.keepAnalyzedLinkHistory) {
                    PreferenceDivider()
                    PreferenceRow(
                        icon = Icons.Rounded.Queue,
                        title = stringResource(R.string.download_defaults_retention_title),
                        subtitle = stringResource(R.string.download_defaults_retention_subtitle),
                        value = pluralStringResource(
                            R.plurals.common_days,
                            uiState.appSettings.analyzedLinkHistoryRetentionDays,
                            uiState.appSettings.analyzedLinkHistoryRetentionDays,
                        ),
                        onClick = {
                            choiceDialog = SettingChoiceDialogState(
                                title = retentionTitle,
                                selected = selectedRetentionLabel,
                                options = retentionChoices.map { days ->
                                    SettingChoiceOption(
                                        title = retentionLabels.getValue(days),
                                        subtitle = retentionSubtitles.getValue(days),
                                        onSelect = { onAnalyzedLinkHistoryRetentionDaysChanged(days) },
                                    )
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
