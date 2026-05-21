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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.ui.components.InlineFeedbackCard
import com.localdownloader.ui.components.VideoCard
import com.localdownloader.ui.model.toReadableSize
import com.localdownloader.viewmodel.FormatMessageScope
import com.localdownloader.viewmodel.FormatUiState
import com.localdownloader.viewmodel.PlaylistItemUiState
import kotlinx.coroutines.launch

private data class QuickLink(
    val label: String,
    val seedUrl: String,
    val logoAssetPath: String,
    val accent: Color,
)

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
    onPlaylistItemFormatSelectorChanged: (Int, String) -> Unit,
    onPlaylistItemContainerChanged: (Int, String) -> Unit,
    onPlaylistItemAudioFormatChanged: (Int, String) -> Unit,
    onPlaylistItemAudioBitrateChanged: (Int, Int) -> Unit,
    onCustomFileNameChanged: (String) -> Unit,
    onPlaylistItemFileNameChanged: (Int, String) -> Unit,
    onOutputTemplateChanged: (String) -> Unit,
    onAudioOutputTemplateChanged: (String) -> Unit,
    onClearBrowserState: () -> Unit,
    onClearAnalyzedResult: () -> Unit,
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
    onDarkThemeChanged: (Boolean) -> Unit,
    isDownloadButtonEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }
    var downloadSetupSheetStep by rememberSaveable { mutableStateOf(DownloadSetupSheetStep.Intro) }
    val homeScrollState = rememberScrollState()
    val errorMessage = uiState.errorMessageFor(FormatMessageScope.BROWSER)
    val infoMessage = uiState.infoMessageFor(FormatMessageScope.BROWSER)

    if (uiState.showMeteredNetworkDialog) {
        AlertDialog(
            onDismissRequest = onDismissMeteredNetworkDialog,
            title = { Text("Wi-Fi only is on") },
            text = {
                Text(
                    "You're on a metered network right now. Keep this download queued until Wi-Fi is available, or allow cellular downloads now.",
                )
            },
            confirmButton = {
                TextButton(onClick = onAllowCellularDownloadsAndQueue) {
                    Text("Allow cellular")
                }
            },
            dismissButton = {
                TextButton(onClick = onQueueWhenWifiAvailable) {
                    Text("Wait for Wi-Fi")
                }
            },
        )
    }

    LaunchedEffect(uiState.videoInfo?.webpageUrl, uiState.shouldShowDownloadSetupNotice) {
        showOptionsSheet = uiState.videoInfo != null && !uiState.shouldShowDownloadSetupNotice
    }

    LaunchedEffect(uiState.shouldShowDownloadSetupNotice) {
        if (uiState.shouldShowDownloadSetupNotice) {
            downloadSetupSheetStep = DownloadSetupSheetStep.Intro
        }
    }

    val quickLinks = remember {
        listOf(
            QuickLink("YouTube", "https://youtube.com", "file:///android_asset/platform_logos/youtube.svg", Color(0xFFFF3B30)),
            QuickLink("Instagram", "https://instagram.com", "file:///android_asset/platform_logos/instagram.svg", Color(0xFFE4405F)),
            QuickLink("TikTok", "https://tiktok.com", "file:///android_asset/platform_logos/tiktok.svg", Color(0xFF25F4EE)),
            QuickLink("X", "https://x.com", "file:///android_asset/platform_logos/x.svg", Color(0xFFB0A7BC)),
            QuickLink("Facebook", "https://facebook.com", "file:///android_asset/platform_logos/facebook.svg", Color(0xFF1877F2)),
            QuickLink("Dailymotion", "https://www.dailymotion.com", "file:///android_asset/platform_logos/dailymotion.svg", Color(0xFF0066DC)),
            QuickLink("Pinterest", "https://pinterest.com", "file:///android_asset/platform_logos/pinterest.svg", Color(0xFFE60023)),
            QuickLink("Twitch", "https://twitch.tv", "file:///android_asset/platform_logos/twitch.svg", Color(0xFF9146FF)),
        )
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
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Paste a link and keep the same download engine underneath.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = onUrlChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search or enter URL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
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
                                    contentDescription = "Paste URL",
                                )
                            }
                            IconButton(
                                onClick = {
                                    showOptionsSheet = false
                                    if (uiState.videoInfo != null) onClearBrowserState() else onUrlChanged("")
                                },
                                enabled = uiState.urlInput.isNotBlank() || uiState.videoInfo != null,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear browser state",
                                )
                            }
                            IconButton(onClick = onAnalyzeClicked, enabled = !uiState.isAnalyzing) {
                                Icon(
                                    imageVector = Icons.Outlined.TravelExplore,
                                    contentDescription = "Analyze",
                                )
                            }
                        }
                    },
                )
                AnimatedVisibility(
                    visible = uiState.isAnalyzing,
                    enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                        expandVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                        shrinkVertically(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Button(
                    onClick = onAnalyzeClicked,
                    enabled = !uiState.isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(if (uiState.isAnalyzing) "Analyzing..." else "Analyze link")
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
                    label = "Home",
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
                    label = "Home",
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Quick links",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(236.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(quickLinks.size) { index ->
                    val link = quickLinks[index]
                    QuickLinkTile(
                        label = link.label,
                        logoAssetPath = link.logoAssetPath,
                        accent = link.accent,
                        imageLoader = imageLoader,
                        onClick = { onUrlChanged(link.seedUrl) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.videoInfo != null,
            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                expandVertically(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 160)) +
                shrinkVertically(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)),
        ) {
            uiState.videoInfo?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = 500f,
                            ),
                        )
                        .clickable { showOptionsSheet = true },
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
                                text = "Ready to download",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(
                                onClick = {
                                    showOptionsSheet = false
                                    onClearAnalyzedResult()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Dismiss ready download",
                                )
                            }
                        }
                        VideoCard(info = info)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showOptionsSheet = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Open options")
                            }
                            TextButton(
                                onClick = onQueueDownloadClicked,
                                enabled = !uiState.isQueueing && isDownloadButtonEnabled,
                                modifier = Modifier.weight(1f),
                            ) {
                                val buttonText = when {
                                    uiState.isQueueing -> "Queueing..."
                                    !isDownloadButtonEnabled -> "Please wait..."
                                    else -> "Quick download"
                                }
                                Text(buttonText)
                            }
                        }
                    }
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
        val containers = listOf("mp4", "webm", "mkv", "mov")
        val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
        val bitrates = listOf(64, 96, 128, 192, 256, 320)
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = optionsSheetState,
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
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Download options",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(
                                onClick = {
                                    showOptionsSheet = false
                                    onClearAnalyzedResult()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear ready download",
                                )
                            }
                        }
                    }
                    item {
                        VideoCard(info = uiState.videoInfo)
                    }

                    if (uiState.videoInfo.isPlaylist) {
                        item {
                            Text(
                                text = "Global format",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        item {
                            SelectionOptionsCard(
                                streamType = uiState.selectedStreamType,
                                onStreamTypeChanged = onStreamTypeChanged,
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
                                emptyChoicesMessage = "If a file does not expose the exact same formats, the downloader will fall back to the closest matching selector for that item.",
                            )
                        }
                        item {
                            val currentTemplate = if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                                uiState.audioOutputTemplate
                            } else {
                                uiState.outputTemplate
                            }
                            OutlinedTextField(
                                value = currentTemplate,
                                onValueChange = { newValue ->
                                    if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                                        onAudioOutputTemplateChanged(newValue)
                                    } else {
                                        onOutputTemplateChanged(newValue)
                                    }
                                },
                                label = {
                                    Text(
                                        if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                                            "Playlist audio filename template"
                                        } else {
                                            "Playlist filename template"
                                        },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                        item {
                            ToggleChipRow(
                                items = buildList {
                                    if (uiState.selectedStreamType != StreamType.AUDIO_ONLY) {
                                        add(ToggleConfig("Subtitles", uiState.downloadSubtitles, onDownloadSubtitlesChanged))
                                        add(ToggleConfig("Embed subs", uiState.embedSubtitles, onEmbedSubtitlesChanged))
                                    }
                                    add(ToggleConfig("Metadata", uiState.embedMetadata, onEmbedMetadataChanged))
                                    add(ToggleConfig("Embed thumb", uiState.embedThumbnail, onEmbedThumbnailChanged))
                                    add(ToggleConfig("Write thumb", uiState.writeThumbnail, onWriteThumbnailChanged))
                                },
                            )
                        }
                        item {
                            PlaylistSelectionSummaryCard(
                                totalCount = uiState.playlistItems.size,
                                selectedCount = uiState.selectedPlaylistItemCount,
                                allSelected = uiState.areAllPlaylistItemsSelected,
                                onSelectAllChanged = onPlaylistSelectAllChanged,
                            )
                        }
                        item {
                            Text(
                                text = "Files-wise format section",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        item {
                            Text(
                                text = "Every selected file appears below. Keep global settings on, or expand one item to override its format for that file only.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        uiState.playlistItems.forEachIndexed { index, playlistItem ->
                            item {
                                PlaylistItemCard(
                                    item = playlistItem,
                                    globalStreamType = uiState.selectedStreamType,
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
                            SelectionOptionsCard(
                                streamType = uiState.selectedStreamType,
                                onStreamTypeChanged = onStreamTypeChanged,
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
                        item {
                            OutlinedTextField(
                                value = uiState.customFileName,
                                onValueChange = onCustomFileNameChanged,
                                label = { Text("File name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    Text("Edit the saved file name before download, or keep the source title as-is.")
                                },
                            )
                        }
                        item {
                            val currentTemplate = if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                                uiState.audioOutputTemplate
                            } else {
                                uiState.outputTemplate
                            }
                            OutlinedTextField(
                                value = currentTemplate,
                                onValueChange = { newValue ->
                                    if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                                        onAudioOutputTemplateChanged(newValue)
                                    } else {
                                        onOutputTemplateChanged(newValue)
                                    }
                                },
                                label = {
                                    Text(
                                        if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                                            "Audio filename template"
                                        } else {
                                            "Video filename template"
                                        },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                        item {
                            ToggleChipRow(
                                items = buildList {
                                    if (uiState.selectedStreamType != StreamType.AUDIO_ONLY) {
                                        add(ToggleConfig("Subtitles", uiState.downloadSubtitles, onDownloadSubtitlesChanged))
                                        add(ToggleConfig("Embed subs", uiState.embedSubtitles, onEmbedSubtitlesChanged))
                                    }
                                    add(ToggleConfig("Metadata", uiState.embedMetadata, onEmbedMetadataChanged))
                                    add(ToggleConfig("Embed thumb", uiState.embedThumbnail, onEmbedThumbnailChanged))
                                    add(ToggleConfig("Write thumb", uiState.writeThumbnail, onWriteThumbnailChanged))
                                    add(ToggleConfig("Playlist", uiState.enablePlaylist, onPlaylistEnabledChanged))
                                },
                            )
                        }
                    }

                    if (!isDownloadButtonEnabled) {
                        item {
                            Text(
                                text = "Wait for the active job to settle before starting another download.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Button(
                    onClick = onQueueDownloadClicked,
                    enabled = !uiState.isQueueing && isDownloadButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    val buttonText = when {
                        uiState.isQueueing -> "Queueing..."
                        !isDownloadButtonEnabled -> "Please wait..."
                        else -> "Download"
                    }
                    Text(buttonText)
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
                        text = if (step == DownloadSetupSheetStep.Intro) {
                            "Before your first download"
                        } else {
                            "Set up smoother access"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (step == DownloadSetupSheetStep.Intro) {
                    Text(
                        text = "You can download without cookies, but it is recommended to add cookies first from Settings > Access and network or the More shortcuts. For YouTube, PO generation from YouTube access helps with sign-in and playback checks.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "If a download fails or gets stuck later, cookies and PO generation are the first things to try before reporting an issue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f),
                    )
                } else {
                    Text(
                        text = "Start with cookies if you want the safest setup. For YouTube, PO generation is the extra step that usually helps with blocked or signed-in videos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "You can still skip all of this and come back later from Settings or More whenever you need it.",
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
                Text("Set up now")
            }
            TextButton(
                onClick = onContinueWithoutCookies,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue without cookies")
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
                        text = "Open Cookies",
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
                        text = "Open YouTube access",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(
                    onClick = onContinueWithoutCookies,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Done for now")
                }
            }
        }
    }
}


@Composable
private fun QuickLinkTile(
    label: String,
    logoAssetPath: String,
    accent: Color,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = logoAssetPath,
                    imageLoader = imageLoader,
                    contentDescription = "$label logo",
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun choicesForStreamType(
    streamType: StreamType,
    videoAudioChoices: List<FormatChoice>,
    videoOnlyChoices: List<FormatChoice>,
    audioOnlyChoices: List<FormatChoice>,
): List<FormatChoice> {
    return when (streamType) {
        StreamType.VIDEO_AUDIO -> videoAudioChoices.ifEmpty { videoOnlyChoices }
        StreamType.VIDEO_ONLY -> videoOnlyChoices
        StreamType.AUDIO_ONLY -> audioOnlyChoices
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionOptionsCard(
    streamType: StreamType,
    onStreamTypeChanged: (StreamType) -> Unit,
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StreamType.entries.forEach { item ->
            FilterChip(
                selected = item == streamType,
                onClick = { onStreamTypeChanged(item) },
                label = { Text(item.label) },
            )
        }
    }

    if (choices.isNotEmpty()) {
        val selectedChoice = choices
            .getOrNull(choices.indexOfFirst { it.selector == selectedFormatSelector })
            ?: choices.first()
        FormatChoiceDropdownRow(
            label = "Format",
            choices = choices,
            selectedIndex = choices.indexOfFirst { it.selector == selectedFormatSelector }
                .coerceAtLeast(0),
            onSelected = { onFormatSelectorChanged(choices[it].selector) },
        )
        FormatChoiceInsightCard(choice = selectedChoice)
    } else {
        onQualityChanged?.let { qualityChanged ->
            BrowserDropdownRow(
                label = "Quality",
                options = VideoQuality.entries.map { it.label },
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

    if (streamType == StreamType.AUDIO_ONLY) {
        BrowserDropdownRow(
            label = "Audio format",
            options = audioFormats,
            selectedIndex = audioFormats.indexOf(audioFormat).coerceAtLeast(0),
            onSelected = { onAudioFormatChanged(audioFormats[it]) },
        )
        BrowserDropdownRow(
            label = "Bitrate",
            options = bitrates.map { "$it kbps" },
            selectedIndex = bitrates.indexOf(audioBitrateKbps).coerceAtLeast(0),
            onSelected = { onAudioBitrateChanged(bitrates[it]) },
        )
    } else {
        BrowserDropdownRow(
            label = "Container",
            options = containers,
            selectedIndex = containers.indexOf(container).coerceAtLeast(0),
            onSelected = { onContainerChanged(containers[it]) },
        )
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
                    text = "Select all files",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$selectedCount of $totalCount items selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onSelectAllChanged(!allSelected) }) {
                Text(if (allSelected) "Clear" else "Select")
            }
        }
    }
}

@Composable
private fun PlaylistItemCard(
    item: PlaylistItemUiState,
    globalStreamType: StreamType,
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
    onFormatSelectorChanged: (String) -> Unit,
    onContainerChanged: (String) -> Unit,
    onAudioFormatChanged: (String) -> Unit,
    onAudioBitrateChanged: (Int) -> Unit,
    onFileNameChanged: (String) -> Unit,
) {
    val activeStreamType = if (item.useGlobalSettings) globalStreamType else item.selectedStreamType
    val activeFormatSelector = if (item.useGlobalSettings) globalFormatSelector else item.selectedFormatSelector
    val activeContainer = if (item.useGlobalSettings) globalContainer else item.selectedContainer
    val activeAudioFormat = if (item.useGlobalSettings) globalAudioFormat else item.selectedAudioFormat
    val activeAudioBitrate = if (item.useGlobalSettings) globalAudioBitrateKbps else item.audioBitrateKbps
    val activeChoices = choicesForStreamType(
        streamType = activeStreamType,
        videoAudioChoices = item.availableVideoAudioChoices,
        videoOnlyChoices = item.availableVideoOnlyChoices,
        audioOnlyChoices = item.availableAudioOnlyChoices,
    )
    val activeChoice = activeChoices.firstOrNull { it.selector == activeFormatSelector } ?: activeChoices.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    verticalArrangement = Arrangement.spacedBy(3.dp),
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
                        ).joinToString(" | ").ifBlank { "Playlist item ${item.entry.playlistItemIndex}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = playlistItemFormatSummary(
                            useGlobalSettings = item.useGlobalSettings,
                            streamType = activeStreamType,
                            choice = activeChoice,
                            container = activeContainer,
                            audioFormat = activeAudioFormat,
                            audioBitrateKbps = activeAudioBitrate,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = { onExpandedChanged(!item.isExpanded) }) {
                    Text(if (item.isExpanded) "Hide" else "Edit")
                }
            }

            AnimatedVisibility(visible = item.isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    OutlinedTextField(
                        value = item.customFileName,
                        onValueChange = onFileNameChanged,
                        label = { Text("File name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    ToggleChipRow(
                        items = listOf(
                            ToggleConfig("Use global settings", item.useGlobalSettings, onUseGlobalChanged),
                        ),
                    )
                    if (item.useGlobalSettings) {
                        Text(
                            text = "This file will use the global format, container, and bitrate settings above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        SelectionOptionsCard(
                            streamType = item.selectedStreamType,
                            onStreamTypeChanged = onStreamTypeChanged,
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
                            emptyChoicesMessage = "This item did not expose a richer format list during playlist analysis, so the downloader will use an automatic selector for the chosen stream type.",
                        )
                    }
                }
            }
        }
    }
}

private fun playlistItemFormatSummary(
    useGlobalSettings: Boolean,
    streamType: StreamType,
    choice: FormatChoice?,
    container: String,
    audioFormat: String,
    audioBitrateKbps: Int,
): String {
    val prefix = if (useGlobalSettings) "Global" else "Custom"
    return when (streamType) {
        StreamType.AUDIO_ONLY -> "$prefix | ${audioFormat.uppercase()} | ${audioBitrateKbps} kbps"
        else -> {
            val formatLabel = choice?.label ?: container.uppercase()
            "$prefix | $formatLabel"
        }
    }
}

private fun playlistDurationLabel(durationSeconds: Long?): String? {
    val totalSeconds = durationSeconds ?: return null
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatChoiceInsightCard(
    choice: FormatChoice,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Selected format details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = choice.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                buildFormatInsightChips(choice).forEach { chip ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text = chip,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
            Text(
                text = buildFormatChoiceHint(choice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildFormatInsightChips(choice: FormatChoice): List<String> {
    return buildList {
        formatChoicePrimarySizeLabel(choice)?.let { sizeLabel ->
            add(if (choice.fileSizeBytes != null) "Size $sizeLabel" else sizeLabel)
        }
        choice.height?.let { add("${it}p") }
        choice.fps?.takeIf { it > 0 }?.let { add("${it.toInt()} fps") }
        add("Container ${choice.container.uppercase()}")
        when (choice.streamType) {
            StreamType.VIDEO_AUDIO -> add(if (choice.isMerged) "Video + audio" else "Muxed")
            StreamType.VIDEO_ONLY -> add("Video only")
            StreamType.AUDIO_ONLY -> add("Audio only")
        }
        choice.bitrateKbps?.let { add("${it} kbps") }
        choice.videoCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
            add("Video ${compactCodecLabel(it)}")
        }
        choice.audioCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
            add("Audio ${compactCodecLabel(it)}")
        }
    }
}

private fun buildFormatChoiceHint(choice: FormatChoice): String {
    val baseHint = when (choice.streamType) {
        StreamType.AUDIO_ONLY -> {
            if ((choice.bitrateKbps ?: 0) >= 256) {
                "Higher bitrate audio. Better quality, but it uses more storage."
            } else {
                "Lighter audio option. Good when you want to save storage space."
            }
        }

        StreamType.VIDEO_ONLY,
        StreamType.VIDEO_AUDIO,
        -> when {
            (choice.height ?: 0) >= 1080 || (choice.fps ?: 0.0) >= 50.0 ->
                "Quality-first pick. Great for playback quality, but expect a larger download."

            (choice.fileSizeBytes ?: choice.estimatedSizeBytes ?: Long.MAX_VALUE) <= 80L * 1024L * 1024L ->
                "Storage-friendlier pick. Good when you want a smaller file first."

            else ->
                "Balanced option. A good middle ground between quality and file size."
        }
    }
    val estimateNote = if (choice.fileSizeBytes == null && choice.estimatedSizeBytes != null) {
        " Estimated final size is based on bitrate and duration, so the finished file can vary a little."
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
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedChoice = choices.getOrNull(selectedIndex) ?: choices.firstOrNull() ?: return
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = listOfNotNull(
                selectedChoice.label,
                formatChoicePrimarySizeLabel(selectedChoice),
            ).joinToString(" | "),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
    return buildList {
        choice.height?.let { add("${it}p") }
        choice.fps?.takeIf { it > 0 }?.let { add("${it.toInt()}fps") }
        add(choice.container.uppercase())
        choice.videoCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
            add("v:${compactCodecLabel(it)}")
        }
        choice.audioCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
            add("a:${compactCodecLabel(it)}")
        }
    }.joinToString(" | ").ifBlank { null }
}

private fun formatChoicePrimarySizeLabel(choice: FormatChoice): String? {
    val exactSizeLabel = formatChoiceSizeLabel(choice.fileSizeBytes)
    if (exactSizeLabel != null) return exactSizeLabel
    return formatChoiceSizeLabel(choice.estimatedSizeBytes)?.let { "Est. $it" }
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
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { options.firstOrNull().orEmpty() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
