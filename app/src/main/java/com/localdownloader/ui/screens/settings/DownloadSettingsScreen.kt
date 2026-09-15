package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewAgenda
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
import com.localdownloader.domain.models.FormatSelectorStyle
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
    onShowFormatFpsChanged: (Boolean) -> Unit = {},
    onShowFormatCodecChanged: (Boolean) -> Unit = {},
    onShowFormatBitrateChanged: (Boolean) -> Unit = {},
    onFormatSelectorStyleChanged: (FormatSelectorStyle) -> Unit = {},
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
    val selectorStyleTitle = stringResource(R.string.download_defaults_selector_style_title)
    val selectorStyleSheetTitle = stringResource(R.string.format_selector_style_sheet)
    val selectorStyleSheetDesc = stringResource(R.string.format_selector_style_sheet_desc)
    val selectorStyleDropdownTitle = stringResource(R.string.format_selector_style_dropdown)
    val selectorStyleDropdownDesc = stringResource(R.string.format_selector_style_dropdown_desc)

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
        title = stringResource(R.string.settings_item_download_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.VideoFile,
                    title = videoFilenameTitle,
                    subtitle = filenameSupporting,
                    value = uiState.appSettings.defaultOutputTemplate,
                    onClick = {
                        filenameTemplateDialog = FilenameTemplateDialogState(
                            title = videoFilenameTitle,
                            initialTemplate = uiState.appSettings.defaultOutputTemplate,
                            onSave = onDefaultVideoOutputTemplateChanged,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.AudioFile,
                    title = audioFilenameTitle,
                    subtitle = filenameSupporting,
                    value = uiState.appSettings.defaultAudioOutputTemplate,
                    onClick = {
                        filenameTemplateDialog = FilenameTemplateDialogState(
                            title = audioFilenameTitle,
                            initialTemplate = uiState.appSettings.defaultAudioOutputTemplate,
                            onSave = onDefaultAudioOutputTemplateChanged,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.VideoFile,
                    title = videoContainerTitle,
                    subtitle = stringResource(R.string.download_defaults_video_container_subtitle),
                    value = uiState.appSettings.defaultMergeContainer.uppercase(),
                    onClick = {
                        val choices = listOf("auto", "mp4", "webm", "mkv", "mov").map { container ->
                            SettingChoiceOption(
                                title = container.uppercase(),
                                subtitle = stringResource(
                                    when (container) {
                                        "mp4" -> R.string.container_desc_mp4
                                        "webm" -> R.string.container_desc_webm
                                        "mkv" -> R.string.container_desc_mkv
                                        "mov" -> R.string.container_desc_mov
                                        else -> R.string.container_desc_auto
                                    },
                                ),
                                onSelect = { onDefaultVideoContainerChanged(container) },
                            )
                        }
                        choiceDialog = SettingChoiceDialogState(
                            title = videoContainerTitle,
                            selected = uiState.appSettings.defaultMergeContainer.uppercase(),
                            options = choices,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.AudioFile,
                    title = audioContainerTitle,
                    subtitle = stringResource(R.string.download_defaults_audio_container_subtitle),
                    value = uiState.appSettings.defaultAudioFormat.uppercase(),
                    onClick = {
                        val choices = listOf("mp3", "m4a", "aac", "opus", "flac", "wav").map { format ->
                            SettingChoiceOption(
                                title = format.uppercase(),
                                subtitle = stringResource(
                                    when (format) {
                                        "mp3" -> R.string.audio_desc_mp3
                                        "m4a" -> R.string.audio_desc_m4a
                                        "aac" -> R.string.audio_desc_aac
                                        "opus" -> R.string.audio_desc_opus
                                        "flac" -> R.string.audio_desc_flac
                                        "wav" -> R.string.audio_desc_wav
                                        else -> R.string.audio_desc_default
                                    },
                                ),
                                onSelect = { onDefaultAudioContainerChanged(format) },
                            )
                        }
                        choiceDialog = SettingChoiceDialogState(
                            title = audioContainerTitle,
                            selected = uiState.appSettings.defaultAudioFormat.uppercase(),
                            options = choices,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = stringResource(R.string.download_defaults_download_subtitles_title),
                    subtitle = stringResource(R.string.download_defaults_download_subtitles_subtitle),
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
                    icon = Icons.Rounded.ViewAgenda,
                    title = selectorStyleTitle,
                    subtitle = stringResource(R.string.download_defaults_selector_style_subtitle),
                    value = when (uiState.appSettings.formatSelectorStyle) {
                        FormatSelectorStyle.BOTTOM_SHEET -> selectorStyleSheetTitle
                        FormatSelectorStyle.DROPDOWN -> selectorStyleDropdownTitle
                    },
                    onClick = {
                        val styleChoices = listOf(
                            SettingChoiceOption(
                                title = selectorStyleSheetTitle,
                                subtitle = selectorStyleSheetDesc,
                                onSelect = { onFormatSelectorStyleChanged(FormatSelectorStyle.BOTTOM_SHEET) },
                            ),
                            SettingChoiceOption(
                                title = selectorStyleDropdownTitle,
                                subtitle = selectorStyleDropdownDesc,
                                onSelect = { onFormatSelectorStyleChanged(FormatSelectorStyle.DROPDOWN) },
                            ),
                        )
                        choiceDialog = SettingChoiceDialogState(
                            title = selectorStyleTitle,
                            selected = when (uiState.appSettings.formatSelectorStyle) {
                                FormatSelectorStyle.BOTTOM_SHEET -> selectorStyleSheetTitle
                                FormatSelectorStyle.DROPDOWN -> selectorStyleDropdownTitle
                            },
                            options = styleChoices,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.download_defaults_show_fps_title),
                    subtitle = stringResource(R.string.download_defaults_show_fps_subtitle),
                    checked = uiState.appSettings.showFormatFps,
                    onCheckedChange = onShowFormatFpsChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.download_defaults_show_codec_title),
                    subtitle = stringResource(R.string.download_defaults_show_codec_subtitle),
                    checked = uiState.appSettings.showFormatCodec,
                    onCheckedChange = onShowFormatCodecChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Equalizer,
                    title = stringResource(R.string.download_defaults_show_bitrate_title),
                    subtitle = stringResource(R.string.download_defaults_show_bitrate_subtitle),
                    checked = uiState.appSettings.showFormatBitrate,
                    onCheckedChange = onShowFormatBitrateChanged,
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
