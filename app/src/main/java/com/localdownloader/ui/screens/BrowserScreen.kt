package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.localdownloader.R
import com.localdownloader.downloader.isAutomaticContainerSelection
import com.localdownloader.downloader.isChoiceCompatibleWithRequestedContainer
import com.localdownloader.downloader.resolveMergeContainerCompatibility
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.LinkAnalysisResult
import com.localdownloader.domain.models.OutputTransform
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.domain.models.AnalyzedLinkRecord
import com.localdownloader.domain.models.audioFormatSupportsBitrateControl
import com.localdownloader.domain.models.choicesForStreamType
import com.localdownloader.domain.models.effectiveOutputStreamType
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.model.toReadableSize
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState
import com.localdownloader.viewmodel.PlaylistItemUiState
import kotlinx.coroutines.launch
import java.util.Locale

private enum class DownloadSetupSheetStep {
    Intro,
    Setup,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    uiState: FormatUiState,
    onUrlChanged: (String) -> Unit,
    onAnalyzeClicked: () -> Unit,
    onQualityChanged: (VideoQuality) -> Unit,
    onStreamTypeChanged: (StreamType) -> Unit,
    onOutputTransformChanged: (OutputTransform) -> Unit,
    onFormatSelectorChanged: (String) -> Unit,
    onContainerChanged: (String) -> Unit,
    onAudioFormatChanged: (String) -> Unit,
    onAudioBitrateChanged: (Int) -> Unit,
    onDownloadSubtitlesChanged: (Boolean) -> Unit,
    onEmbedSubtitlesChanged: (Boolean) -> Unit,
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onWriteThumbnailChanged: (Boolean) -> Unit,
    onPlaylistEnabledChanged: (Boolean) -> Unit,
    onPlaylistSelectAllChanged: (Boolean) -> Unit,
    onPlaylistItemSelectedChanged: (Int, Boolean) -> Unit,
    onPlaylistItemExpandedChanged: (Int, Boolean) -> Unit,
    onPlaylistItemUseGlobalChanged: (Int, Boolean) -> Unit,
    onPlaylistItemStreamTypeChanged: (Int, StreamType) -> Unit,
    onPlaylistItemOutputTransformChanged: (Int, OutputTransform) -> Unit,
    onPlaylistItemFormatSelectorChanged: (Int, String) -> Unit,
    onPlaylistItemContainerChanged: (Int, String) -> Unit,
    onPlaylistItemAudioFormatChanged: (Int, String) -> Unit,
    onPlaylistItemAudioBitrateChanged: (Int, Int) -> Unit,
    onCustomFileNameChanged: (String) -> Unit,
    onPlaylistItemFileNameChanged: (Int, String) -> Unit,
    onOutputTemplateChanged: (String) -> Unit,
    onAudioOutputTemplateChanged: (String) -> Unit,
    onPrepareDownloadClicked: () -> Unit,
    onClearBrowserState: () -> Unit,
    onClearAnalyzedResult: () -> Unit,
    onOpenReadyItem: (String) -> Unit,
    onRemoveReadyItem: (String) -> Unit,
    onQueueDownloadClicked: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompress: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onDismissDownloadSetupNotice: () -> Unit,
    onDismissMessage: () -> Unit,
    onDismissMeteredNetworkDialog: () -> Unit,
    onQueueWhenWifiAvailable: () -> Unit,
    onAllowCellularDownloadsAndQueue: () -> Unit,
    onOptionsSheetRequestConsumed: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isDownloadButtonEnabled: Boolean = true,
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var downloadSetupSheetStep by rememberSaveable { mutableStateOf(DownloadSetupSheetStep.Intro) }
    var showBrowseMenu by rememberSaveable { mutableStateOf(false) }
    val homeScrollState = rememberScrollState()
    val errorMessage = uiState.errorMessageFor(FormatMessageScope.BROWSER)
    val infoMessage = uiState.infoMessageFor(FormatMessageScope.BROWSER)
    val darkThemeLabel = stringResource(
        if (uiState.isDarkTheme) {
            R.string.browser_use_light_theme
        } else {
            R.string.browser_use_dark_theme
        },
    )

    if (uiState.showMeteredNetworkDialog) {
        AlertDialog(
            onDismissRequest = onDismissMeteredNetworkDialog,
            title = { Text(stringResource(R.string.browser_metered_title)) },
            text = {
                Text(
                    stringResource(R.string.browser_metered_body),
                )
            },
            confirmButton = {
                TextButton(onClick = onAllowCellularDownloadsAndQueue) {
                    Text(stringResource(R.string.browser_allow_cellular))
                }
            },
            dismissButton = {
                TextButton(onClick = onQueueWhenWifiAvailable) {
                    Text(stringResource(R.string.browser_wait_for_wifi))
                }
            },
        )
    }

    LaunchedEffect(uiState.shouldOpenOptionsSheet, uiState.shouldShowDownloadSetupNotice) {
        if (uiState.shouldOpenOptionsSheet && !uiState.shouldShowDownloadSetupNotice) {
            showOptionsSheet = true
            onOptionsSheetRequestConsumed()
        }
    }

    LaunchedEffect(uiState.shouldShowDownloadSetupNotice) {
        if (uiState.shouldShowDownloadSetupNotice) {
            downloadSetupSheetStep = DownloadSetupSheetStep.Intro
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(homeScrollState)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f,
                ),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.browser_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.browser_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenHistory) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = stringResource(R.string.browser_open_history),
                    )
                }
                Box {
                    IconButton(onClick = { showBrowseMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.browser_actions),
                        )
                    }
                    DropdownMenu(
                        expanded = showBrowseMenu,
                        onDismissRequest = { showBrowseMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_settings)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showBrowseMenu = false
                                onOpenSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.common_help)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showBrowseMenu = false
                                onOpenHelp()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(darkThemeLabel) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showBrowseMenu = false
                                onDarkThemeChanged(!uiState.isDarkTheme)
                            },
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = onUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.browser_url_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Web,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                showOptionsSheet = false
                                if (uiState.videoInfo != null) onClearBrowserState() else onUrlChanged("")
                            },
                            enabled = uiState.urlInput.isNotBlank() || uiState.videoInfo != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.browser_clear_state),
                            )
                        }
                    },
                )
                AnimatedVisibility(
                    visible = uiState.isAnalyzing || uiState.isLoadingFormats,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                        expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                clipboardManager.getText()?.text
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let(onUrlChanged)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.browser_paste),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Button(
                        onClick = onAnalyzeClicked,
                        enabled = !uiState.isAnalyzing && !uiState.isLoadingFormats,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        val buttonText = when {
                            uiState.isAnalyzing -> stringResource(R.string.browser_analyzing)
                            uiState.isLoadingFormats -> "Loading formats..."
                            else -> stringResource(R.string.browser_analyze_link)
                        }
                        Text(buttonText)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !errorMessage.isNullOrBlank(),
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        ) {
            errorMessage?.let { message ->
                InlineFeedbackCard(
                    label = stringResource(R.string.browser_feedback_label),
                    message = message,
                    isError = true,
                    onDismiss = onDismissMessage,
                )
            }
        }
        AnimatedVisibility(
            visible = !infoMessage.isNullOrBlank(),
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
        ) {
            infoMessage?.let { message ->
                InlineFeedbackCard(
                    label = stringResource(R.string.browser_feedback_label),
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                )
            }
        }

        uiState.linkAnalysis?.let { analysis ->
            DiscoveredResultsCard(
                analysis = analysis,
                isLoading = uiState.isLoadingFormats,
                isReady = uiState.videoInfo != null && !uiState.isLoadingFormats,
                onDownload = onPrepareDownloadClicked,
            )
        }

        val currentAnalysisUrl = uiState.linkAnalysis?.let { analysis ->
            if (analysis.isCollection) {
                analysis.webpageUrl ?: analysis.input
            } else {
                analysis.primaryItem?.webpageUrl ?: analysis.webpageUrl ?: analysis.input
            }
        }
        val visibleReadyItems = uiState.readyAnalyzedItems.filterNot { item ->
            currentAnalysisUrl != null && item.webpageUrl == currentAnalysisUrl
        }
        AnimatedVisibility(
            visible = visibleReadyItems.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                expandVertically(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                shrinkVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                visibleReadyItems.forEach { item ->
                    ReadyAnalyzedCard(
                        item = item,
                        isActive = uiState.videoInfo?.webpageUrl == item.webpageUrl,
                        isLoading = uiState.restoringReadyItemUrl == item.webpageUrl &&
                            (uiState.isAnalyzing || uiState.isLoadingFormats),
                        onOpen = {
                            if (uiState.videoInfo?.webpageUrl == item.webpageUrl) {
                                showOptionsSheet = true
                            } else {
                                onOpenReadyItem(item.webpageUrl)
                            }
                        },
                        onQueue = onQueueDownloadClicked,
                        onRemove = {
                            showOptionsSheet = false
                            if (uiState.videoInfo?.webpageUrl == item.webpageUrl) {
                                onClearAnalyzedResult()
                            } else {
                                onRemoveReadyItem(item.webpageUrl)
                            }
                        },
                        isQueueEnabled = !uiState.isQueueing && isDownloadButtonEnabled,
                        isQueueing = uiState.isQueueing,
                    )
                }
            }
        }
    }

    if (uiState.shouldShowDownloadSetupNotice) {
        ModalBottomSheet(
            onDismissRequest = onDismissDownloadSetupNotice,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DownloadSetupOnboardingSheet(
                step = downloadSetupSheetStep,
                onSetUpNow = { downloadSetupSheetStep = DownloadSetupSheetStep.Setup },
                onOpenCookies = {
                    onDismissDownloadSetupNotice()
                    onOpenCookies()
                },
                onOpenYoutubeAccess = {
                    onDismissDownloadSetupNotice()
                    onOpenYoutubeAccess()
                },
                onContinueWithoutCookies = onDismissDownloadSetupNotice,
            )
        }
    }

    if (showOptionsSheet && uiState.videoInfo != null && !uiState.shouldShowDownloadSetupNotice) {
        val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val optionsListState = rememberLazyListState()
        val sheetScrollGuard = rememberBottomSheetScrollGuard(optionsListState)
        val footerDragBlocker = rememberBottomSheetDragBlocker()
        val containers = listOf("auto", "mp4", "webm", "mkv", "mov")
        val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
        val bitrates = listOf(64, 96, 128, 192, 256, 320)
        val downloadActionSummary = buildDownloadActionSummary(uiState)
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = optionsSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .nestedScroll(sheetScrollGuard),
                    state = optionsListState,
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        OptionsSheetHeader(
                            info = uiState.videoInfo,
                            onClear = {
                                showOptionsSheet = false
                            },
                        )
                    }

                    if (uiState.videoInfo.isPlaylist) {
                        item {
                            OptionSectionCard(title = stringResource(R.string.browser_section_global)) {
                                SelectionOptionsCard(
                                    streamType = uiState.selectedStreamType,
                                    onStreamTypeChanged = onStreamTypeChanged,
                                    outputTransform = uiState.selectedOutputTransform,
                                    onOutputTransformChanged = onOutputTransformChanged,
                                    hasVideoAudioChoices = uiState.availableVideoAudioChoices.isNotEmpty(),
                                    hasVideoOnlyChoices = uiState.availableVideoOnlyChoices.isNotEmpty(),
                                    hasAudioOnlyChoices = uiState.availableAudioOnlyChoices.isNotEmpty(),
                                    choices = choicesForStreamType(
                                        streamType = uiState.selectedStreamType,
                                        videoAudioChoices = uiState.availableVideoAudioChoices,
                                        videoOnlyChoices = uiState.availableVideoOnlyChoices,
                                        audioOnlyChoices = uiState.availableAudioOnlyChoices,
                                    ),
                                    selectedFormatSelector = uiState.selectedFormatSelector,
                                    onFormatSelectorChanged = onFormatSelectorChanged,
                                    quality = uiState.selectedQuality,
                                    onQualityChanged = onQualityChanged,
                                    container = uiState.selectedContainer,
                                    onContainerChanged = onContainerChanged,
                                    audioFormat = uiState.selectedAudioFormat,
                                    onAudioFormatChanged = onAudioFormatChanged,
                                    audioBitrateKbps = uiState.audioBitrateKbps,
                                    onAudioBitrateChanged = onAudioBitrateChanged,
                                    containers = containers,
                                    audioFormats = audioFormats,
                                    bitrates = bitrates,
                                    emptyChoicesMessage = stringResource(R.string.browser_closest_available_format),
                                )
                            }
                        }
                        item {
                            val effectiveStreamType = effectiveOutputStreamType(
                                uiState.selectedStreamType,
                                uiState.selectedOutputTransform,
                            )
                            val currentTemplate = if (effectiveStreamType == StreamType.AUDIO_ONLY) {
                                uiState.audioOutputTemplate
                            } else {
                                uiState.outputTemplate
                            }
                            OptionSectionCard(title = stringResource(R.string.browser_section_template)) {
                                OutlinedTextField(
                                    value = currentTemplate,
                                    onValueChange = { newValue ->
                                        if (effectiveStreamType == StreamType.AUDIO_ONLY) {
                                            onAudioOutputTemplateChanged(newValue)
                                        } else {
                                            onOutputTemplateChanged(newValue)
                                        }
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                if (effectiveStreamType == StreamType.AUDIO_ONLY) {
                                                    R.string.browser_playlist_audio_template
                                                } else {
                                                    R.string.browser_playlist_template
                                                },
                                            ),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                ToggleChipRow(
                                    items = buildList {
                                        if (effectiveStreamType != StreamType.AUDIO_ONLY) {
                                            add(ToggleConfig(stringResource(R.string.browser_toggle_subtitles), uiState.downloadSubtitles, onDownloadSubtitlesChanged))
                                            add(ToggleConfig(stringResource(R.string.browser_toggle_embed_subs), uiState.embedSubtitles, onEmbedSubtitlesChanged))
                                        }
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_metadata), uiState.embedMetadata, onEmbedMetadataChanged))
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_embed_thumb), uiState.embedThumbnail, onEmbedThumbnailChanged))
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_write_thumb), uiState.writeThumbnail, onWriteThumbnailChanged))
                                    },
                                )
                            }
                        }
                        item {
                            OptionSectionCard(title = stringResource(R.string.browser_section_files)) {
                                PlaylistSelectionSummaryCard(
                                    totalCount = uiState.playlistItems.size,
                                    selectedCount = uiState.selectedPlaylistItemCount,
                                    allSelected = uiState.areAllPlaylistItemsSelected,
                                    onSelectAllChanged = onPlaylistSelectAllChanged,
                                )
                            }
                        }
                        uiState.playlistItems.forEachIndexed { index, playlistItem ->
                            item {
                                PlaylistItemCard(
                                    item = playlistItem,
                                    globalStreamType = uiState.selectedStreamType,
                                    globalOutputTransform = uiState.selectedOutputTransform,
                                    globalFormatSelector = uiState.selectedFormatSelector,
                                    globalContainer = uiState.selectedContainer,
                                    globalAudioFormat = uiState.selectedAudioFormat,
                                    globalAudioBitrateKbps = uiState.audioBitrateKbps,
                                    containers = containers,
                                    audioFormats = audioFormats,
                                    bitrates = bitrates,
                                    onSelectedChanged = { onPlaylistItemSelectedChanged(index, it) },
                                    onExpandedChanged = { onPlaylistItemExpandedChanged(index, it) },
                                    onUseGlobalChanged = { onPlaylistItemUseGlobalChanged(index, it) },
                                    onStreamTypeChanged = { onPlaylistItemStreamTypeChanged(index, it) },
                                    onOutputTransformChanged = { onPlaylistItemOutputTransformChanged(index, it) },
                                    onFormatSelectorChanged = { onPlaylistItemFormatSelectorChanged(index, it) },
                                    onContainerChanged = { onPlaylistItemContainerChanged(index, it) },
                                    onAudioFormatChanged = { onPlaylistItemAudioFormatChanged(index, it) },
                                    onAudioBitrateChanged = { onPlaylistItemAudioBitrateChanged(index, it) },
                                    onFileNameChanged = { onPlaylistItemFileNameChanged(index, it) },
                                )
                            }
                        }
                    } else {
                        item {
                            OptionSectionCard(title = stringResource(R.string.browser_section_format)) {
                                SelectionOptionsCard(
                                    streamType = uiState.selectedStreamType,
                                    onStreamTypeChanged = onStreamTypeChanged,
                                    outputTransform = uiState.selectedOutputTransform,
                                    onOutputTransformChanged = onOutputTransformChanged,
                                    hasVideoAudioChoices = uiState.availableVideoAudioChoices.isNotEmpty(),
                                    hasVideoOnlyChoices = uiState.availableVideoOnlyChoices.isNotEmpty(),
                                    hasAudioOnlyChoices = uiState.availableAudioOnlyChoices.isNotEmpty(),
                                    choices = choicesForStreamType(
                                        streamType = uiState.selectedStreamType,
                                        videoAudioChoices = uiState.availableVideoAudioChoices,
                                        videoOnlyChoices = uiState.availableVideoOnlyChoices,
                                        audioOnlyChoices = uiState.availableAudioOnlyChoices,
                                    ),
                                    selectedFormatSelector = uiState.selectedFormatSelector,
                                    onFormatSelectorChanged = onFormatSelectorChanged,
                                    quality = uiState.selectedQuality,
                                    onQualityChanged = onQualityChanged,
                                    container = uiState.selectedContainer,
                                    onContainerChanged = onContainerChanged,
                                    audioFormat = uiState.selectedAudioFormat,
                                    onAudioFormatChanged = onAudioFormatChanged,
                                    audioBitrateKbps = uiState.audioBitrateKbps,
                                    onAudioBitrateChanged = onAudioBitrateChanged,
                                    containers = containers,
                                    audioFormats = audioFormats,
                                    bitrates = bitrates,
                                    emptyChoicesMessage = null,
                                )
                            }
                        }
                        item {
                            val effectiveStreamType = effectiveOutputStreamType(
                                uiState.selectedStreamType,
                                uiState.selectedOutputTransform,
                            )
                            val currentTemplate = if (effectiveStreamType == StreamType.AUDIO_ONLY) {
                                uiState.audioOutputTemplate
                            } else {
                                uiState.outputTemplate
                            }
                            OptionSectionCard(title = stringResource(R.string.browser_section_name)) {
                                OutlinedTextField(
                                    value = uiState.customFileName,
                                    onValueChange = onCustomFileNameChanged,
                                    label = { Text(stringResource(R.string.browser_file_name)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = currentTemplate,
                                    onValueChange = { newValue ->
                                        if (effectiveStreamType == StreamType.AUDIO_ONLY) {
                                            onAudioOutputTemplateChanged(newValue)
                                        } else {
                                            onOutputTemplateChanged(newValue)
                                        }
                                    },
                                    label = {
                                        Text(
                                            stringResource(
                                                if (effectiveStreamType == StreamType.AUDIO_ONLY) {
                                                    R.string.browser_audio_template
                                                } else {
                                                    R.string.browser_video_template
                                                },
                                            ),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                        }
                        item {
                            val effectiveStreamType = effectiveOutputStreamType(
                                uiState.selectedStreamType,
                                uiState.selectedOutputTransform,
                            )
                            OptionSectionCard(title = stringResource(R.string.browser_section_extras)) {
                                ToggleChipRow(
                                    items = buildList {
                                        if (effectiveStreamType != StreamType.AUDIO_ONLY) {
                                            add(ToggleConfig(stringResource(R.string.browser_toggle_subtitles), uiState.downloadSubtitles, onDownloadSubtitlesChanged))
                                            add(ToggleConfig(stringResource(R.string.browser_toggle_embed_subs), uiState.embedSubtitles, onEmbedSubtitlesChanged))
                                        }
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_metadata), uiState.embedMetadata, onEmbedMetadataChanged))
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_embed_thumb), uiState.embedThumbnail, onEmbedThumbnailChanged))
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_write_thumb), uiState.writeThumbnail, onWriteThumbnailChanged))
                                        add(ToggleConfig(stringResource(R.string.browser_toggle_playlist), uiState.enablePlaylist, onPlaylistEnabledChanged))
                                    },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp)
                        .nestedScroll(footerDragBlocker),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isDownloadButtonEnabled) {
                            Text(
                                text = stringResource(R.string.browser_wait_for_current_job),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        downloadActionSummary?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Button(
                            onClick = onQueueDownloadClicked,
                            enabled = !uiState.isQueueing && isDownloadButtonEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            val buttonText = when {
                                uiState.isQueueing -> stringResource(R.string.browser_queueing)
                                !isDownloadButtonEnabled -> stringResource(R.string.browser_please_wait)
                                else -> stringResource(R.string.browser_download_button)
                            }
                            Text(buttonText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun buildDownloadActionSummary(uiState: FormatUiState): String? {
    val videoInfo = uiState.videoInfo ?: return null
    if (videoInfo.isPlaylist) {
        return pluralStringResource(
            R.plurals.browser_selected_files_count,
            uiState.selectedPlaylistItemCount,
            uiState.selectedPlaylistItemCount,
        )
    }

    return when (uiState.selectedStreamType) {
        StreamType.AUDIO_ONLY -> buildList {
            add(uiState.selectedAudioFormat.uppercase())
            if (audioFormatSupportsBitrateControl(uiState.selectedAudioFormat)) {
                add("${uiState.audioBitrateKbps} kbps")
            }
        }.joinToString(" | ")

        else -> {
            val currentChoices = compatibleChoicesForStreamType(
                streamType = uiState.selectedStreamType,
                container = uiState.selectedContainer,
                videoAudioChoices = uiState.availableVideoAudioChoices,
                videoOnlyChoices = uiState.availableVideoOnlyChoices,
                audioOnlyChoices = uiState.availableAudioOnlyChoices,
            )
            val choice = currentChoices.firstOrNull { it.selector == uiState.selectedFormatSelector }
                ?: currentChoices.firstOrNull()
            listOfNotNull(
                choice?.height?.let { "${it}p" } ?: localizedContainerLabel(uiState.selectedContainer),
                choice?.let {
                    resolvedOutputContainer(
                        streamType = uiState.selectedStreamType,
                        requestedContainer = uiState.selectedContainer,
                        choice = it,
                    ).uppercase()
                },
                choice?.let { formatChoicePrimarySizeLabel(it) },
            ).joinToString(" | ")
        }
    }.ifBlank { null }
}

@Composable
private fun ReadyAnalyzedCard(
    item: AnalyzedLinkRecord,
    isActive: Boolean,
    isLoading: Boolean,
    onOpen: () -> Unit,
    onQueue: () -> Unit,
    onRemove: () -> Unit,
    isQueueEnabled: Boolean,
    isQueueing: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f,
                ),
            ),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        if (isActive) {
                            R.string.browser_ready
                        } else {
                            R.string.browser_saved_ready_link
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.browser_remove_ready_download),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buildReadyHistoryChips(item).forEach { chip ->
                    BrowserMetaChip(text = chip)
                }
            }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpen)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.uploader ?: stringResource(R.string.common_unknown_uploader),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpen,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            isLoading -> stringResource(R.string.common_loading)
                            isActive -> stringResource(R.string.browser_open_options)
                            else -> stringResource(R.string.browser_load)
                        },
                    )
                }
                if (isActive) {
                    TextButton(
                        onClick = onQueue,
                        enabled = isQueueEnabled,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            when {
                                isQueueing -> stringResource(R.string.browser_queueing)
                                !isQueueEnabled -> stringResource(R.string.browser_please_wait)
                                else -> stringResource(R.string.browser_queue_now)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredResultsCard(
    analysis: LinkAnalysisResult,
    isLoading: Boolean,
    isReady: Boolean,
    onDownload: () -> Unit,
) {
    val previewItems = if (analysis.isCollection) analysis.items.take(20) else emptyList()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f,
                ),
            ),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (analysis.isCollection) "Found files" else "Found file",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOfNotNull(
                        if (analysis.isCollection) "${analysis.playlistCount ?: analysis.items.size} items" else null,
                        playlistDurationLabel(analysis.durationSeconds),
                    ).forEach { chip ->
                        BrowserMetaChip(text = chip)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = analysis.thumbnailUrl ?: analysis.primaryItem?.thumbnailUrl,
                        contentDescription = analysis.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 120.dp, height = 68.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = analysis.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = analysis.uploader
                                ?: analysis.primaryItem?.uploader
                                ?: stringResource(R.string.common_unknown_uploader),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Button(
                onClick = onDownload,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    when {
                        isLoading -> "Loading options..."
                        isReady -> stringResource(R.string.browser_open_options)
                        else -> stringResource(R.string.browser_download_button)
                    },
                )
            }

            if (previewItems.isNotEmpty()) {
                Text(
                    text = if (analysis.isCollection) "Files" else "File",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                previewItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = item.thumbnailUrl ?: analysis.thumbnailUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 92.dp, height = 52.dp),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.uploader
                                        ?: analysis.uploader
                                        ?: stringResource(R.string.common_unknown_uploader),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            playlistDurationLabel(item.durationSeconds)?.let { duration ->
                                BrowserMetaChip(text = duration)
                            }
                        }
                    }
                }
                if (analysis.isCollection && analysis.items.size > previewItems.size) {
                    Text(
                        text = "Showing first ${previewItems.size} of ${analysis.items.size} files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadSetupOnboardingSheet(
    step: DownloadSetupSheetStep,
    onSetUpNow: () -> Unit,
    onOpenCookies: () -> Unit,
    onOpenYoutubeAccess: () -> Unit,
    onContinueWithoutCookies: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(
                            if (step == DownloadSetupSheetStep.Intro) {
                                R.string.browser_download_setup_intro_title
                            } else {
                                R.string.browser_download_setup_setup_title
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (step == DownloadSetupSheetStep.Intro) {
                    Text(
                        text = stringResource(R.string.browser_download_setup_intro_body_1),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.browser_download_setup_intro_body_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.browser_download_setup_setup_body_1),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.browser_download_setup_setup_body_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f),
                    )
                }
            }
        }

        if (step == DownloadSetupSheetStep.Intro) {
            Button(
                onClick = onSetUpNow,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(stringResource(R.string.browser_setup_now))
            }
            TextButton(
                onClick = onContinueWithoutCookies,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.browser_continue_without_cookies))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onOpenCookies,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Web,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.browser_open_cookies),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = onOpenYoutubeAccess,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.browser_open_youtube_access),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(
                    onClick = onContinueWithoutCookies,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.browser_done_for_now))
                }
            }
        }
    }
}


@Composable
private fun OptionsSheetHeader(
    info: VideoInfo,
    onClear: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.browser_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.browser_clear_ready_download),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(width = 112.dp, height = 64.dp),
                ) {
                    AsyncImage(
                        model = info.thumbnailUrl,
                        contentDescription = info.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = info.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = info.uploader ?: stringResource(R.string.common_unknown_uploader),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buildReadyDownloadChips(info).forEach { chip ->
                    BrowserMetaChip(text = chip)
                }
            }
        }
    }
}

@Composable
private fun OptionSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                content()
            },
        )
    }
}

@Composable
private fun BrowserMetaChip(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun buildReadyDownloadChips(info: VideoInfo): List<String> {
    return buildList {
        if (info.isPlaylist) {
            val count = info.playlistCount ?: info.playlistEntries.size
            add(pluralStringResource(R.plurals.browser_files_count, count, count))
        } else {
            add(pluralStringResource(R.plurals.browser_formats_count, info.formats.size, info.formats.size))
        }
        playlistDurationLabel(info.durationSeconds)?.let(::add)
        info.uploader?.takeIf { it.isNotBlank() }?.let { uploader ->
            add(
                if (uploader.length <= 28) {
                    uploader
                } else {
                    "${uploader.take(28)}..."
                },
            )
        }
    }
}

@Composable
private fun buildReadyHistoryChips(item: AnalyzedLinkRecord): List<String> {
    return buildList {
        if (item.isPlaylist) {
            val count = item.playlistCount ?: 0
            add(pluralStringResource(R.plurals.browser_files_count, count, count))
        } else {
            add(pluralStringResource(R.plurals.browser_formats_count, item.formatCount, item.formatCount))
        }
        playlistDurationLabel(item.durationSeconds)?.let(::add)
    }
}

private fun compatibleChoicesForStreamType(
    streamType: StreamType,
    container: String,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): List<FormatChoice> {
    val baseChoices = choicesForStreamType(
        streamType = streamType,
        videoAudioChoices = videoAudioChoices,
        videoOnlyChoices = videoOnlyChoices,
        audioOnlyChoices = audioOnlyChoices,
    )
    if (streamType != StreamType.VIDEO_AUDIO) {
        return baseChoices
    }

    val compatibleChoices = baseChoices.filter { choice ->
        isChoiceCompatibleWithRequestedContainer(container, choice)
    }
    return compatibleChoices.ifEmpty { baseChoices }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionOptionsCard(
    streamType: StreamType,
    onStreamTypeChanged: (StreamType) -> Unit,
    outputTransform: OutputTransform,
    onOutputTransformChanged: (OutputTransform) -> Unit,
    hasVideoAudioChoices: Boolean,
    hasVideoOnlyChoices: Boolean,
    hasAudioOnlyChoices: Boolean,
    choices: List<FormatChoice>,
    selectedFormatSelector: String?,
    onFormatSelectorChanged: (String) -> Unit,
    quality: VideoQuality,
    onQualityChanged: ((VideoQuality) -> Unit)?,
    container: String,
    onContainerChanged: (String) -> Unit,
    audioFormat: String,
    onAudioFormatChanged: (String) -> Unit,
    audioBitrateKbps: Int,
    onAudioBitrateChanged: (Int) -> Unit,
    containers: List<String>,
    audioFormats: List<String>,
    bitrates: List<Int>,
    emptyChoicesMessage: String?,
) {
    val availableTransforms = remember(streamType, hasVideoAudioChoices, hasVideoOnlyChoices, hasAudioOnlyChoices) {
        if (streamType != StreamType.VIDEO_AUDIO || !hasVideoAudioChoices) {
            listOf(OutputTransform.NONE)
        } else {
            buildList {
                add(OutputTransform.NONE)
                if (!hasAudioOnlyChoices) add(OutputTransform.EXTRACT_AUDIO)
                if (!hasVideoOnlyChoices) add(OutputTransform.REMOVE_AUDIO)
            }
        }
    }
    val effectiveStreamType = effectiveOutputStreamType(streamType, outputTransform)
    val visibleChoices = remember(choices, streamType, container) {
        if (streamType == StreamType.VIDEO_AUDIO) {
            choices.filter { choice -> isChoiceCompatibleWithRequestedContainer(container, choice) }
                .ifEmpty { choices }
        } else {
            choices
        }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StreamType.entries.forEach { item ->
                val isEnabled = when (item) {
                    StreamType.VIDEO_AUDIO -> hasVideoAudioChoices
                    StreamType.VIDEO_ONLY -> hasVideoOnlyChoices
                    StreamType.AUDIO_ONLY -> hasAudioOnlyChoices
                }
                FilterChip(
                    selected = item == streamType,
                    onClick = { if (isEnabled) onStreamTypeChanged(item) },
                    enabled = isEnabled,
                    label = { Text(localizedStreamTypeLabel(item)) },
                )
            }
        }

        if (visibleChoices.isNotEmpty()) {
            val selectedChoice = visibleChoices
                .getOrNull(visibleChoices.indexOfFirst { it.selector == selectedFormatSelector })
                ?: visibleChoices.first()
            FormatChoiceDropdownRow(
                label = stringResource(R.string.browser_picker_label_format),
                choices = visibleChoices,
                selectedIndex = visibleChoices.indexOfFirst { it.selector == selectedFormatSelector }
                    .coerceAtLeast(0),
                selectedValue = buildSelectedFormatHeadline(
                    choice = selectedChoice,
                    streamType = streamType,
                    requestedContainer = container,
                ),
                selectedSupporting = buildSelectedFormatMetadata(
                    choice = selectedChoice,
                    streamType = streamType,
                    requestedContainer = container,
                ),
                onSelected = { onFormatSelectorChanged(visibleChoices[it].selector) },
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buildInlineFormatSummaryChips(
                    choice = selectedChoice,
                    streamType = streamType,
                    requestedContainer = container,
                ).forEach { chip ->
                    BrowserMetaChip(text = chip)
                }
            }
            Text(
                text = buildFormatChoiceHint(selectedChoice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            onQualityChanged?.let { qualityChanged ->
                BrowserDropdownRow(
                    label = stringResource(R.string.browser_picker_label_quality),
                    options = VideoQuality.entries.map { localizedVideoQualityLabel(it) },
                    selectedIndex = VideoQuality.entries.indexOf(quality).coerceAtLeast(0),
                    onSelected = { qualityChanged(VideoQuality.entries[it]) },
                )
            }
            emptyChoicesMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (availableTransforms.size > 1) {
            BrowserDropdownRow(
                label = stringResource(R.string.browser_picker_label_output_transform),
                options = availableTransforms.map { localizedOutputTransformLabel(it) },
                selectedIndex = availableTransforms.indexOf(outputTransform).coerceAtLeast(0),
                onSelected = { onOutputTransformChanged(availableTransforms[it]) },
            )
        }

        if (effectiveStreamType == StreamType.AUDIO_ONLY) {
            val showsBitrateControl = audioFormatSupportsBitrateControl(audioFormat)
            BrowserDropdownRow(
                label = stringResource(R.string.browser_picker_label_audio_format),
                options = audioFormats,
                selectedIndex = audioFormats.indexOf(audioFormat).coerceAtLeast(0),
                onSelected = { onAudioFormatChanged(audioFormats[it]) },
            )
            if (showsBitrateControl) {
                BrowserDropdownRow(
                    label = stringResource(R.string.browser_picker_label_bitrate),
                    options = bitrates.map { "$it kbps" },
                    selectedIndex = bitrates.indexOf(audioBitrateKbps).coerceAtLeast(0),
                    onSelected = { onAudioBitrateChanged(bitrates[it]) },
                )
            }
        } else {
            val containerLabels = containers.map { localizedContainerLabel(it) }
            BrowserDropdownRow(
                label = stringResource(R.string.browser_picker_label_container),
                options = containerLabels,
                selectedIndex = containers.indexOf(container).coerceAtLeast(0),
                onSelected = { onContainerChanged(containers[it]) },
            )
        }
    }
}

@Composable
private fun localizedStreamTypeLabel(streamType: StreamType): String {
    return stringResource(
        when (streamType) {
            StreamType.VIDEO_AUDIO -> R.string.browser_video_audio_chip
            StreamType.VIDEO_ONLY -> R.string.browser_video_only_chip
            StreamType.AUDIO_ONLY -> R.string.browser_audio_only_chip
        },
    )
}

@Composable
private fun localizedOutputTransformLabel(outputTransform: OutputTransform): String {
    return stringResource(
        when (outputTransform) {
            OutputTransform.NONE -> R.string.browser_output_transform_none
            OutputTransform.EXTRACT_AUDIO -> R.string.browser_output_transform_extract_audio
            OutputTransform.REMOVE_AUDIO -> R.string.browser_output_transform_remove_audio
        },
    )
}

@Composable
private fun localizedVideoQualityLabel(quality: VideoQuality): String {
    return when (quality) {
        VideoQuality.BEST -> stringResource(R.string.browser_quality_best_available)
        else -> quality.label
    }
}

@Composable
private fun PlaylistSelectionSummaryCard(
    totalCount: Int,
    selectedCount: Int,
    allSelected: Boolean,
    onSelectAllChanged: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = { onSelectAllChanged(it) },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.browser_all_files),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.browser_selected_count_of_total, selectedCount, totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onSelectAllChanged(!allSelected) }) {
                Text(
                    stringResource(
                        if (allSelected) {
                            R.string.common_clear
                        } else {
                            R.string.common_select
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun PlaylistItemCard(
    item: PlaylistItemUiState,
    globalStreamType: StreamType,
    globalOutputTransform: OutputTransform,
    globalFormatSelector: String?,
    globalContainer: String,
    globalAudioFormat: String,
    globalAudioBitrateKbps: Int,
    containers: List<String>,
    audioFormats: List<String>,
    bitrates: List<Int>,
    onSelectedChanged: (Boolean) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onUseGlobalChanged: (Boolean) -> Unit,
    onStreamTypeChanged: (StreamType) -> Unit,
    onOutputTransformChanged: (OutputTransform) -> Unit,
    onFormatSelectorChanged: (String) -> Unit,
    onContainerChanged: (String) -> Unit,
    onAudioFormatChanged: (String) -> Unit,
    onAudioBitrateChanged: (Int) -> Unit,
    onFileNameChanged: (String) -> Unit,
) {
    val activeStreamType = if (item.useGlobalSettings) globalStreamType else item.selectedStreamType
    val activeOutputTransform = if (item.useGlobalSettings) globalOutputTransform else item.selectedOutputTransform
    val effectiveActiveStreamType = effectiveOutputStreamType(activeStreamType, activeOutputTransform)
    val activeFormatSelector = if (item.useGlobalSettings) globalFormatSelector else item.selectedFormatSelector
    val activeContainer = if (item.useGlobalSettings) globalContainer else item.selectedContainer
    val activeAudioFormat = if (item.useGlobalSettings) globalAudioFormat else item.selectedAudioFormat
    val activeAudioBitrate = if (item.useGlobalSettings) globalAudioBitrateKbps else item.audioBitrateKbps
    val activeChoices = compatibleChoicesForStreamType(
        streamType = activeStreamType,
        container = activeContainer,
        videoAudioChoices = item.availableVideoAudioChoices,
        videoOnlyChoices = item.availableVideoOnlyChoices,
        audioOnlyChoices = item.availableAudioOnlyChoices,
    )
    val activeChoice = activeChoices.firstOrNull { it.selector == activeFormatSelector } ?: activeChoices.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onSelectedChanged(it) },
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.size(width = 84.dp, height = 52.dp),
                ) {
                    if (!item.entry.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.entry.thumbnailUrl,
                            contentDescription = item.entry.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.entry.playlistItemIndex.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            item.entry.uploader?.takeIf { it.isNotBlank() },
                            playlistDurationLabel(item.entry.durationSeconds),
                        ).joinToString(" | ").ifBlank {
                            stringResource(R.string.browser_playlist_item_fallback, item.entry.playlistItemIndex)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        buildPlaylistItemSummaryChips(
                            useGlobalSettings = item.useGlobalSettings,
                            streamType = effectiveActiveStreamType,
                            outputTransform = activeOutputTransform,
                            choice = activeChoice,
                            container = activeContainer,
                            audioFormat = activeAudioFormat,
                            audioBitrateKbps = activeAudioBitrate,
                        ).forEach { chip ->
                            BrowserMetaChip(text = chip)
                        }
                    }
                }
                TextButton(onClick = { onExpandedChanged(!item.isExpanded) }) {
                    Text(
                        stringResource(
                            if (item.isExpanded) {
                                R.string.browser_playlist_hide
                            } else {
                                R.string.browser_playlist_edit
                            },
                        ),
                    )
                }
            }

            AnimatedVisibility(visible = item.isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    OutlinedTextField(
                        value = item.customFileName,
                        onValueChange = onFileNameChanged,
                        label = { Text(stringResource(R.string.browser_file_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    ToggleChipRow(
                        items = listOf(
                            ToggleConfig(stringResource(R.string.browser_toggle_use_global_settings), item.useGlobalSettings, onUseGlobalChanged),
                        ),
                    )
                    if (item.useGlobalSettings) {
                        BrowserMetaChip(text = stringResource(R.string.browser_using_global_format))
                    } else {
                        SelectionOptionsCard(
                            streamType = item.selectedStreamType,
                            onStreamTypeChanged = onStreamTypeChanged,
                            outputTransform = item.selectedOutputTransform,
                            onOutputTransformChanged = onOutputTransformChanged,
                            hasVideoAudioChoices = item.availableVideoAudioChoices.isNotEmpty(),
                            hasVideoOnlyChoices = item.availableVideoOnlyChoices.isNotEmpty(),
                            hasAudioOnlyChoices = item.availableAudioOnlyChoices.isNotEmpty(),
                            choices = choicesForStreamType(
                                streamType = item.selectedStreamType,
                                videoAudioChoices = item.availableVideoAudioChoices,
                                videoOnlyChoices = item.availableVideoOnlyChoices,
                                audioOnlyChoices = item.availableAudioOnlyChoices,
                            ),
                            selectedFormatSelector = item.selectedFormatSelector,
                            onFormatSelectorChanged = onFormatSelectorChanged,
                            quality = VideoQuality.BEST,
                            onQualityChanged = null,
                            container = item.selectedContainer,
                            onContainerChanged = onContainerChanged,
                            audioFormat = item.selectedAudioFormat,
                            onAudioFormatChanged = onAudioFormatChanged,
                            audioBitrateKbps = item.audioBitrateKbps,
                            onAudioBitrateChanged = onAudioBitrateChanged,
                            containers = containers,
                            audioFormats = audioFormats,
                            bitrates = bitrates,
                            emptyChoicesMessage = stringResource(R.string.browser_playlist_auto_format),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun buildPlaylistItemSummaryChips(
    useGlobalSettings: Boolean,
    streamType: StreamType,
    outputTransform: OutputTransform,
    choice: FormatChoice?,
    container: String,
    audioFormat: String,
    audioBitrateKbps: Int,
): List<String> {
    return buildList {
        add(
            stringResource(
                if (useGlobalSettings) {
                    R.string.browser_global_chip
                } else {
                    R.string.browser_custom_chip
                },
            ),
        )
        if (outputTransform != OutputTransform.NONE) {
            add(localizedOutputTransformLabel(outputTransform))
        }
        when (streamType) {
            StreamType.AUDIO_ONLY -> {
                add(audioFormat.uppercase())
                if (audioFormatSupportsBitrateControl(audioFormat)) {
                    add("${audioBitrateKbps} kbps")
                }
            }

            else -> {
                add(choice?.height?.let { "${it}p" } ?: localizedContainerLabel(container))
                choice?.let {
                    add(
                        resolvedOutputContainer(
                            streamType = streamType,
                            requestedContainer = container,
                            choice = it,
                        ).uppercase(),
                    )
                }
                formatChoicePrimarySizeLabel(choice ?: return@buildList)?.let(::add)
            }
        }
    }
}

private fun playlistDurationLabel(durationSeconds: Long?): String? {
    val totalSeconds = durationSeconds ?: return null
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

private data class ToggleConfig(
    val label: String,
    val value: Boolean,
    val onToggle: (Boolean) -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToggleChipRow(
    items: List<ToggleConfig>,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item.value,
                onClick = { item.onToggle(!item.value) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun buildInlineFormatSummaryChips(
    choice: FormatChoice,
    streamType: StreamType,
    requestedContainer: String,
): List<String> {
    return buildList {
        resolvedOutputContainer(
            streamType = streamType,
            requestedContainer = requestedContainer,
            choice = choice,
        ).takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        choice.height?.let { add("${it}p") }
        choice.fps?.takeIf { it > 0 }?.let { add("${it.toInt()} fps") }
        when (choice.streamType) {
            StreamType.VIDEO_AUDIO -> add(
                stringResource(
                    if (choice.isMerged) {
                        R.string.browser_video_audio_chip
                    } else {
                        R.string.browser_muxed_chip
                    },
                ),
            )
            StreamType.VIDEO_ONLY -> add(stringResource(R.string.browser_video_only_chip))
            StreamType.AUDIO_ONLY -> add(stringResource(R.string.browser_audio_only_chip))
        }
        formatChoicePrimarySizeLabel(choice)?.let { add(it) }
        choice.bitrateKbps?.let { add("${it} kbps") }
    }
}

@Composable
private fun buildFormatChoiceHint(choice: FormatChoice): String {
    val baseHint = when (choice.streamType) {
        StreamType.AUDIO_ONLY ->
            if ((choice.bitrateKbps ?: 0) >= 256) {
                stringResource(R.string.browser_hint_high_quality)
            } else {
                stringResource(R.string.browser_hint_smaller_audio)
            }

        StreamType.VIDEO_ONLY,
        StreamType.VIDEO_AUDIO,
        -> when {
            (choice.height ?: 0) >= 1080 || (choice.fps ?: 0.0) >= 50.0 ->
                stringResource(R.string.browser_hint_quality_first)

            (choice.fileSizeBytes ?: choice.estimatedSizeBytes ?: Long.MAX_VALUE) <= 80L * 1024L * 1024L ->
                stringResource(R.string.browser_hint_storage_friendly)

            else ->
                stringResource(R.string.browser_hint_balanced)
        }
    }
    val estimateNote = if (choice.fileSizeBytes == null && choice.estimatedSizeBytes != null) {
        stringResource(R.string.browser_hint_estimated_suffix)
    } else {
        ""
    }
    return baseHint + estimateNote
}

private fun compactCodecLabel(codec: String): String {
    return codec
        .substringBefore('.')
        .substringBefore(':')
        .uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatChoiceDropdownRow(
    label: String,
    choices: List<FormatChoice>,
    selectedIndex: Int,
    selectedValue: String,
    selectedSupporting: String?,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedChoice = choices.getOrNull(selectedIndex) ?: choices.firstOrNull() ?: return
    Box(modifier = Modifier.fillMaxWidth()) {
        PickerSurface(
            label = label,
            value = listOfNotNull(
                selectedValue,
                formatChoicePrimarySizeLabel(selectedChoice),
            ).joinToString(" | "),
            supporting = selectedSupporting,
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            choices.forEachIndexed { index, choice ->
                DropdownMenuItem(
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = choice.label,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                formatChoicePrimarySizeLabel(choice)?.let { sizeLabel ->
                                    Text(
                                        text = sizeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            buildFormatMenuMetadata(choice)?.let { metadata ->
                                Text(
                                    text = metadata,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

private fun formatChoiceDisplayLabel(choice: FormatChoice): String {
    val sizeLabel = formatChoiceSizeLabel(choice.fileSizeBytes) ?: return choice.label
    return "${choice.label} • $sizeLabel"
}

private fun buildFormatMenuMetadata(choice: FormatChoice): String? {
    return buildFormatMenuMetadata(choice = choice, containerOverride = choice.container)
}

private fun buildFormatMenuMetadata(
    choice: FormatChoice,
    containerOverride: String,
): String? {
    return buildList {
        choice.height?.let { add("${it}p") }
        choice.fps?.takeIf { it > 0 }?.let { add("${it.toInt()}fps") }
        add(containerOverride.uppercase())
        choice.videoCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
            add("v:${compactCodecLabel(it)}")
        }
        choice.audioCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
            add("a:${compactCodecLabel(it)}")
        }
    }.joinToString(" | ").ifBlank { null }
}

private fun resolvedOutputContainer(
    streamType: StreamType,
    requestedContainer: String,
    choice: FormatChoice,
): String {
    if (streamType == StreamType.AUDIO_ONLY) {
        return choice.container.lowercase()
    }
    if (streamType != StreamType.VIDEO_AUDIO) {
        return choice.container.ifBlank { requestedContainer }.lowercase()
    }

    val effectiveRequestedContainer = if (isAutomaticContainerSelection(requestedContainer)) {
        choice.container
    } else {
        requestedContainer
    }.trim().lowercase().ifBlank { choice.container.lowercase() }

    return resolveMergeContainerCompatibility(
        requestedContainer = effectiveRequestedContainer,
        selectedChoice = choice,
    ).resolvedContainer ?: effectiveRequestedContainer
}

@Composable
private fun buildSelectedFormatHeadline(
    choice: FormatChoice,
    streamType: StreamType,
    requestedContainer: String,
): String {
    if (streamType == StreamType.AUDIO_ONLY) {
        return choice.label
    }

    return buildList {
        choice.height?.let { add("${it}p") }
        add(
            resolvedOutputContainer(
                streamType = streamType,
                requestedContainer = requestedContainer,
                choice = choice,
            ).uppercase(),
        )
        choice.fps?.takeIf { it > 0 }?.let { add("${it.toInt()}fps") }
    }.joinToString(" ").ifBlank { choice.label }
}

private fun buildSelectedFormatMetadata(
    choice: FormatChoice,
    streamType: StreamType,
    requestedContainer: String,
): String? {
    return buildFormatMenuMetadata(
        choice = choice,
        containerOverride = resolvedOutputContainer(
            streamType = streamType,
            requestedContainer = requestedContainer,
            choice = choice,
        ),
    )
}

@Composable
private fun localizedContainerLabel(container: String): String {
    return if (isAutomaticContainerSelection(container)) {
        stringResource(R.string.common_auto)
    } else {
        container.uppercase()
    }
}

@Composable
private fun formatChoicePrimarySizeLabel(choice: FormatChoice): String? {
    val exactSizeLabel = formatChoiceSizeLabel(choice.fileSizeBytes)
    if (exactSizeLabel != null) return exactSizeLabel
    return formatChoiceSizeLabel(choice.estimatedSizeBytes)?.let { stringResource(R.string.common_estimated_size, it) }
}

private fun formatChoiceSizeLabel(fileSizeBytes: Long?): String? {
    return fileSizeBytes
        ?.takeIf { it > 0L }
        ?.toReadableSize()
        ?.takeIf { it.isNotBlank() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserDropdownRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        PickerSurface(
            label = label,
            value = options.getOrElse(selectedIndex) { options.firstOrNull().orEmpty() },
            supporting = null,
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(index)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun PickerSurface(
    label: String,
    value: String,
    supporting: String?,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = stringResource(
                    if (expanded) {
                        R.string.browser_picker_close
                    } else {
                        R.string.browser_picker_change
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
