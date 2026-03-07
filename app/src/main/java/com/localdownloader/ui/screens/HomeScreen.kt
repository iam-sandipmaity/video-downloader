package com.localdownloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localdownloader.domain.models.StreamType
import com.localdownloader.domain.models.VideoQuality
import com.localdownloader.ui.components.UrlInput
import com.localdownloader.ui.components.VideoCard
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun HomeScreen(
    uiState: FormatUiState,
    onUrlChanged: (String) -> Unit,
    onAnalyzeClicked: () -> Unit,
    onQualityChanged: (VideoQuality) -> Unit,
    onStreamTypeChanged: (StreamType) -> Unit,
    onContainerChanged: (String) -> Unit,
    onAudioFormatChanged: (String) -> Unit,
    onAudioBitrateChanged: (Int) -> Unit,
    onDownloadSubtitlesChanged: (Boolean) -> Unit,
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onWriteThumbnailChanged: (Boolean) -> Unit,
    onPlaylistEnabledChanged: (Boolean) -> Unit,
    onOutputTemplateChanged: (String) -> Unit,
    onQueueDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UrlInput(
            url = uiState.urlInput,
            onUrlChanged = onUrlChanged,
            onAnalyzeClicked = onAnalyzeClicked,
            isAnalyzing = uiState.isAnalyzing,
        )

        uiState.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        uiState.infoMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.secondary)
        }

        uiState.videoInfo?.let { info ->
            VideoCard(info = info)
        }

        // Download options card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Download options", style = MaterialTheme.typography.titleMedium)

                DropdownRow(
                    label = "Quality",
                    options = VideoQuality.entries.map { it.label },
                    selectedIndex = VideoQuality.entries.indexOf(uiState.selectedQuality).coerceAtLeast(0),
                    onSelected = { onQualityChanged(VideoQuality.entries[it]) },
                )

                DropdownRow(
                    label = "Type",
                    options = StreamType.entries.map { it.label },
                    selectedIndex = StreamType.entries.indexOf(uiState.selectedStreamType).coerceAtLeast(0),
                    onSelected = { onStreamTypeChanged(StreamType.entries[it]) },
                )

                val containers = listOf("mp4", "webm", "mkv", "mov")
                val audioFormats = listOf("mp3", "m4a", "aac", "opus", "flac", "wav")
                val bitrates = listOf(64, 96, 128, 192, 256, 320)

                if (uiState.selectedStreamType == StreamType.AUDIO_ONLY) {
                    DropdownRow(
                        label = "Audio format",
                        options = audioFormats,
                        selectedIndex = audioFormats.indexOf(uiState.selectedAudioFormat).coerceAtLeast(0),
                        onSelected = { onAudioFormatChanged(audioFormats[it]) },
                    )
                    DropdownRow(
                        label = "Bitrate",
                        options = bitrates.map { "$it kbps" },
                        selectedIndex = bitrates.indexOf(uiState.audioBitrateKbps).coerceAtLeast(0),
                        onSelected = { onAudioBitrateChanged(bitrates[it]) },
                    )
                } else {
                    DropdownRow(
                        label = "Container",
                        options = containers,
                        selectedIndex = containers.indexOf(uiState.selectedContainer).coerceAtLeast(0),
                        onSelected = { onContainerChanged(containers[it]) },
                    )
                }
            }
        }

        // Advanced options card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Advanced", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = uiState.outputTemplate,
                    onValueChange = onOutputTemplateChanged,
                    label = { Text("Output filename template") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                ToggleRow("Download subtitles", uiState.downloadSubtitles, onDownloadSubtitlesChanged)
                ToggleRow("Embed metadata", uiState.embedMetadata, onEmbedMetadataChanged)
                ToggleRow("Embed thumbnail", uiState.embedThumbnail, onEmbedThumbnailChanged)
                ToggleRow("Write thumbnail file", uiState.writeThumbnail, onWriteThumbnailChanged)
                ToggleRow("Enable playlists", uiState.enablePlaylist, onPlaylistEnabledChanged)
            }
        }

        Button(
            onClick = onQueueDownloadClicked,
            enabled = !uiState.isQueueing && uiState.videoInfo != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isQueueing) "Queueing..." else "Download")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownRow(
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

