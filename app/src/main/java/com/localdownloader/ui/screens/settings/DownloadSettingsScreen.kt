package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DownloadForOffline
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
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }
    var filenameTemplateDialog by remember { mutableStateOf<FilenameTemplateDialogState?>(null) }

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
        title = "Download defaults",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceHeroCard(
                eyebrow = "Future downloads",
                title = "Make the next save feel predictable",
                subtitle = "Set the naming, containers, subtitles, and saved defaults you want the app to reach for before you tweak anything item by item.",
                badges = listOf(
                    uiState.selectedContainer.uppercase(),
                    uiState.selectedAudioFormat.uppercase(),
                    "${uiState.maxConcurrentDownloads} slots",
                ),
            )
        }
        item {
            PreferenceSectionHeader(
                title = "Naming and containers",
                subtitle = "These defaults are reused for future downloads and act like the clean baseline for the rest of the app.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Description,
                    title = "Filename template [video]",
                    subtitle = "Used for future video and merged downloads.",
                    value = uiState.outputTemplate,
                    onClick = {
                        filenameTemplateDialog = FilenameTemplateDialogState(
                            title = "Filename template [video]",
                            value = uiState.outputTemplate,
                            supporting = "Use yt-dlp placeholders. Keep %(ext)s in the template so the final file extension stays correct.",
                            presets = videoFilenameTemplatePresets(),
                            tokens = suggestedFilenameTokens(),
                            onConfirm = onDefaultVideoOutputTemplateChanged,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.AudioFile,
                    title = "Filename template [audio]",
                    subtitle = "Used for future audio-only downloads and extracts.",
                    value = uiState.audioOutputTemplate,
                    onClick = {
                        filenameTemplateDialog = FilenameTemplateDialogState(
                            title = "Filename template [audio]",
                            value = uiState.audioOutputTemplate,
                            supporting = "Use yt-dlp placeholders. Keep %(ext)s in the template so the final file extension stays correct.",
                            presets = audioFilenameTemplatePresets(),
                            tokens = suggestedFilenameTokens(),
                            onConfirm = onDefaultAudioOutputTemplateChanged,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.VideoFile,
                    title = "Default video container",
                    subtitle = "Preferred output format for merged video downloads.",
                    value = uiState.selectedContainer.uppercase(),
                    onClick = {
                        val containers = listOf("mp4", "webm", "mkv", "mov")
                        choiceDialog = SettingChoiceDialogState(
                            title = "Default video container",
                            selected = uiState.selectedContainer.uppercase(),
                            options = containers.map { container ->
                                SettingChoiceOption(
                                    title = container.uppercase(),
                                    subtitle = containerDescription(container),
                                    onSelect = { onDefaultVideoContainerChanged(container) },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.AudioFile,
                    title = "Default audio container",
                    subtitle = "Preferred output format for future audio extracts.",
                    value = uiState.selectedAudioFormat.uppercase(),
                    onClick = {
                        val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
                        choiceDialog = SettingChoiceDialogState(
                            title = "Default audio container",
                            selected = uiState.selectedAudioFormat.uppercase(),
                            options = audioFormats.map { format ->
                                SettingChoiceOption(
                                    title = format.uppercase(),
                                    subtitle = audioFormatDescription(format),
                                    onSelect = { onDefaultAudioContainerChanged(format) },
                                )
                            },
                        )
                    },
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Post-processing",
                subtitle = "These switches define how much extra metadata the app should pull in or embed once the file is on its way out.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = "Download subtitles",
                    subtitle = "Fetch subtitle sidecars automatically when they are available.",
                    checked = uiState.downloadSubtitles,
                    onCheckedChange = onDefaultDownloadSubtitlesChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Subtitles,
                    title = "Embed subtitles",
                    subtitle = "Try to place subtitles inside the final video file when the container supports it.",
                    checked = uiState.embedSubtitles,
                    onCheckedChange = onDefaultEmbedSubtitlesChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Description,
                    title = "Embed metadata",
                    subtitle = "Write title, creator, album, and related tags into supported files.",
                    checked = uiState.embedMetadata,
                    onCheckedChange = onDefaultEmbedMetadataChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.PhotoSizeSelectActual,
                    title = "Embed thumbnail",
                    subtitle = "Attach artwork or cover images directly into compatible media files.",
                    checked = uiState.embedThumbnail,
                    onCheckedChange = onDefaultEmbedThumbnailChanged,
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Queue preference",
                subtitle = "UI-only for now: we can still save how many slots you want the app to target as the queue system keeps evolving.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Queue,
                    title = "Concurrent downloads",
                    subtitle = "Store the preferred number of parallel download slots for the updated queue experience.",
                    value = uiState.maxConcurrentDownloads.toString(),
                    onClick = {
                        val slotChoices = (1..4).map { slotCount ->
                            SettingChoiceOption(
                                title = slotCount.toString(),
                                subtitle = when (slotCount) {
                                    1 -> "Keep the flow single-file and predictable."
                                    2 -> "A balanced default for most phones."
                                    3 -> "Push more work through together."
                                    else -> "Favor throughput over quiet background behavior."
                                },
                                onSelect = { onMaxConcurrentDownloadsChanged(slotCount) },
                            )
                        }
                        choiceDialog = SettingChoiceDialogState(
                            title = "Concurrent downloads",
                            selected = uiState.maxConcurrentDownloads.toString(),
                            options = slotChoices,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.DownloadForOffline,
                    title = "Default setup summary",
                    subtitle = "Current defaults at a glance for quick sanity checks before you leave this page.",
                    value = "${uiState.selectedContainer.uppercase()} / ${uiState.selectedAudioFormat.uppercase()}",
                    onClick = null,
                )
            }
        }
    }
}
