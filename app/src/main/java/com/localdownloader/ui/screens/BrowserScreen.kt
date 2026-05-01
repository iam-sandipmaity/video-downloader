package com.localdownloader.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.ui.components.VideoCard
import com.localdownloader.viewmodel.FormatUiState
import kotlinx.coroutines.launch

private data class QuickLink(
    val label: String,
    val seedUrl: String,
    val logoAssetPath: String,
    val accent: Color,
)

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
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onWriteThumbnailChanged: (Boolean) -> Unit,
    onPlaylistEnabledChanged: (Boolean) -> Unit,
    onOutputTemplateChanged: (String) -> Unit,
    onClearBrowserState: () -> Unit,
    onClearAnalyzedResult: () -> Unit,
    onQueueDownloadClicked: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCompress: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
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
    var showMenu by remember { mutableStateOf(false) }
    var showOptionsSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.videoInfo?.webpageUrl) {
        showOptionsSheet = uiState.videoInfo != null
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
                    text = "Browser",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Paste a link and keep the same download engine underneath.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Browser menu",
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text("History") },
                        leadingIcon = { Icon(Icons.Outlined.History, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenHistory()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Compressor") },
                        leadingIcon = { Icon(Icons.Outlined.Transform, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenCompress()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Converter") },
                        leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenConvert()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Help") },
                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onOpenHelp()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Dark mode") },
                        leadingIcon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                        trailingIcon = {
                            Switch(
                                checked = uiState.isDarkTheme,
                                onCheckedChange = null,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDarkThemeChanged(!uiState.isDarkTheme)
                        },
                    )
                }
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
                if (uiState.isAnalyzing) {
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

        uiState.errorMessage?.let { message ->
            MessageBanner(
                message = message,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        uiState.infoMessage?.let { message ->
            MessageBanner(
                message = message,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
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

        uiState.videoInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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

    if (showOptionsSheet && uiState.videoInfo != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                VideoCard(info = uiState.videoInfo)

                val choices = when (uiState.selectedStreamType) {
                    StreamType.VIDEO_AUDIO -> uiState.availableVideoAudioChoices
                        .ifEmpty { uiState.availableVideoOnlyChoices }
                    StreamType.VIDEO_ONLY -> uiState.availableVideoOnlyChoices
                    StreamType.AUDIO_ONLY -> uiState.availableAudioOnlyChoices
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StreamType.entries.forEach { item ->
                        FilterChip(
                            selected = item == uiState.selectedStreamType,
                            onClick = { onStreamTypeChanged(item) },
                            label = { Text(item.label) },
                        )
                    }
                }

                if (choices.isNotEmpty()) {
                    BrowserDropdownRow(
                        label = "Format",
                        options = choices.map { it.label },
                        selectedIndex = choices.indexOfFirst { it.selector == uiState.selectedFormatSelector }
                            .coerceAtLeast(0),
                        onSelected = { onFormatSelectorChanged(choices[it].selector) },
                    )
                } else {
                    BrowserDropdownRow(
                        label = "Quality",
                        options = VideoQuality.entries.map { it.label },
                        selectedIndex = VideoQuality.entries.indexOf(uiState.selectedQuality).coerceAtLeast(0),
                        onSelected = { onQualityChanged(VideoQuality.entries[it]) },
                    )
                }

                val containers = listOf("mp4", "webm", "mkv", "mov")
                val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
                val bitrates = listOf(64, 96, 128, 192, 256, 320)

                if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                    BrowserDropdownRow(
                        label = "Audio format",
                        options = audioFormats,
                        selectedIndex = audioFormats.indexOf(uiState.selectedAudioFormat).coerceAtLeast(0),
                        onSelected = { onAudioFormatChanged(audioFormats[it]) },
                    )
                    BrowserDropdownRow(
                        label = "Bitrate",
                        options = bitrates.map { "$it kbps" },
                        selectedIndex = bitrates.indexOf(uiState.audioBitrateKbps).coerceAtLeast(0),
                        onSelected = { onAudioBitrateChanged(bitrates[it]) },
                    )
                } else {
                    BrowserDropdownRow(
                        label = "Container",
                        options = containers,
                        selectedIndex = containers.indexOf(uiState.selectedContainer).coerceAtLeast(0),
                        onSelected = { onContainerChanged(containers[it]) },
                    )
                }

                OutlinedTextField(
                    value = uiState.outputTemplate,
                    onValueChange = onOutputTemplateChanged,
                    label = { Text("Output filename template") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                ToggleChipRow(
                    items = listOf(
                        ToggleConfig("Subtitles", uiState.downloadSubtitles, onDownloadSubtitlesChanged),
                        ToggleConfig("Metadata", uiState.embedMetadata, onEmbedMetadataChanged),
                        ToggleConfig("Embed thumb", uiState.embedThumbnail, onEmbedThumbnailChanged),
                        ToggleConfig("Write thumb", uiState.writeThumbnail, onWriteThumbnailChanged),
                        ToggleConfig("Playlist", uiState.enablePlaylist, onPlaylistEnabledChanged),
                    ),
                )

                Button(
                    onClick = onQueueDownloadClicked,
                    enabled = !uiState.isQueueing && isDownloadButtonEnabled,
                    modifier = Modifier.fillMaxWidth(),
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
private fun MessageBanner(
    message: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
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




