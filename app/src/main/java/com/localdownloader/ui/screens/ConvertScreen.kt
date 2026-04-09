package com.localdownloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.localdownloader.ffmpeg.CONVERSION_PRESETS
import com.localdownloader.ffmpeg.VIDEO_OUTPUT_FORMATS
import com.localdownloader.ffmpeg.AUDIO_OUTPUT_FORMATS
import com.localdownloader.viewmodel.MediaToolsUiState
import com.localdownloader.viewmodel.formatFileSize
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    uiState: MediaToolsUiState,
    onInputPathChanged: (String) -> Unit,
    onOutputFormatChanged: (String) -> Unit,
    onAudioBitrateChanged: (String) -> Unit,
    onVideoBitrateChanged: (String) -> Unit,
    onConvertClicked: () -> Unit,
    onBrowseFile: () -> Unit,
    onConversionPresetSelected: (Int) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Convert",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "Change the format of any local media file via FFmpeg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Input file ────────────────────────────────────────────
            ToolSectionLabel("Choose file")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onBrowseFile,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Select media file from device")
                    }
                    // File info card
                    uiState.convertInputFileInfo?.let { info ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = info.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Size: ${formatFileSize(info.sizeBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Conversion preset ─────────────────────────────────────
            ToolSectionLabel("Output format — pick a preset")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ConversionPresetDropdown(
                        presets = CONVERSION_PRESETS,
                        selectedIndex = uiState.convertPresetIndex.coerceIn(0, CONVERSION_PRESETS.lastIndex),
                        onPresetSelected = onConversionPresetSelected,
                    )
                    Text(
                        text = "Presets set the output format and recommended bitrates automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Advanced: manual format override ──────────────────────
            ToggleableAdvanced(
                expanded = showAdvanced,
                onToggle = { showAdvanced = it },
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "Custom format",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        // Video formats
                        Text("Video", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        FormatChips(VIDEO_OUTPUT_FORMATS, uiState.convertOutputFormat) { onOutputFormatChanged(it) }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        // Audio formats
                        Text("Audio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        FormatChips(AUDIO_OUTPUT_FORMATS, uiState.convertOutputFormat) { onOutputFormatChanged(it) }
                        // Manual bitrate overrides
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = uiState.convertVideoBitrate,
                                onValueChange = onVideoBitrateChanged,
                                label = { Text("Video kbps") },
                                placeholder = { Text("auto") },
                                supportingText = { Text("Leave blank for auto") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = uiState.convertAudioBitrate,
                                onValueChange = onAudioBitrateChanged,
                                label = { Text("Audio kbps") },
                                placeholder = { Text("auto") },
                                supportingText = { Text("Leave blank for auto") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                        }
                    }
                }
            }

            // ── Convert button ────────────────────────────────────────
            Button(
                onClick = onConvertClicked,
                enabled = !uiState.isConverting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                if (uiState.isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Convert file", style = MaterialTheme.typography.labelLarge)
                }
            }

            // ── Progress ─────────────────────────────────────────────
            if (uiState.isConverting) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val progress = uiState.convertProgress
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${(progress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // ── Result / error ────────────────────────────────────────
            uiState.convertResult?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        val srcSize = uiState.convertSourceSizeBytes
                        val resSize = uiState.convertResultSizeBytes
                        if (srcSize != null && resSize != null) {
                            val diff = resSize - srcSize
                            Text(
                                text = "Before: ${formatFileSize(srcSize)}  ->  After: ${formatFileSize(resSize)}  (${if (diff > 0) "+${(diff.toDouble() / srcSize * 100).roundToInt()}%" else "-${(-diff.toDouble() / srcSize * 100).roundToInt()}%"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
            uiState.convertError?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            // ── Info card ─────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("How it works", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "FFmpeg re-encodes or re-muxes the selected file into your chosen container. " +
                            "If no bitrate is specified the source quality is preserved. " +
                            "The output is saved in the same folder as the input with the new extension.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversionPresetDropdown(
    presets: List<com.localdownloader.ffmpeg.ConversionPreset>,
    selectedIndex: Int,
    onPresetSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Conversion preset", style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = presets.getOrElse(selectedIndex) { presets.first() }.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                singleLine = true,
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                presets.forEachIndexed { index, preset ->
                    DropdownMenuItem(
                        text = { Text(preset.label) },
                        onClick = {
                            onPresetSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleableAdvanced(
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        OutlinedButton(
            onClick = { onToggle(!expanded) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (expanded) "Hide advanced options" else "Show custom format options")
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun FormatChips(
    formats: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        formats.forEach { fmt ->
            val chipColor = if (fmt == selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            Surface(
                onClick = { onSelected(fmt) },
                color = chipColor,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = fmt,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (fmt == selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
