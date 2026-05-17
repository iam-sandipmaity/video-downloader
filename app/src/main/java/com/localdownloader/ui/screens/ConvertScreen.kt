package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localdownloader.ffmpeg.AUDIO_OUTPUT_FORMATS
import com.localdownloader.ffmpeg.CONVERSION_PRESETS
import com.localdownloader.ffmpeg.ConversionPreset
import com.localdownloader.ffmpeg.VIDEO_OUTPUT_FORMATS
import com.localdownloader.viewmodel.MediaToolsUiState
import com.localdownloader.viewmodel.formatFileSize
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val selectedPresetIndex = uiState.convertPresetIndex.coerceIn(0, CONVERSION_PRESETS.lastIndex)
    val selectedPreset = CONVERSION_PRESETS[selectedPresetIndex]
    val canConvert = uiState.convertInputFileInfo != null && !uiState.isConverting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ConvertHeroCard(
            preset = selectedPreset,
            inputName = uiState.convertInputFileInfo?.name,
            onBack = onBack,
        )

        ToolPanel {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionEyebrow("File")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = onBrowseFile,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Choose file")
                    }
                    if (uiState.convertInputFileInfo != null) {
                        OutlinedButton(
                            onClick = { onInputPathChanged("") },
                            modifier = Modifier.widthIn(min = 96.dp),
                        ) {
                            Text("Clear")
                        }
                    }
                }
                InputFileCard(
                    fileName = uiState.convertInputFileInfo?.name,
                    fileSizeBytes = uiState.convertInputFileInfo?.sizeBytes,
                    filePath = uiState.convertInputFileInfo?.path,
                    emptyMessage = "No file yet",
                )
            }
        }

        ToolPanel {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionEyebrow("Presets")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CONVERSION_PRESETS.forEachIndexed { index, preset ->
                        ConversionPresetCard(
                            preset = preset,
                            selected = index == selectedPresetIndex,
                            onClick = { onConversionPresetSelected(index) },
                        )
                    }
                }
            }
        }

        ToolPanel {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionEyebrow("Output")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryChip("Format ${uiState.convertOutputFormat.uppercase()}")
                    SummaryChip("Video ${bitrateLabel(uiState.convertVideoBitrate)}")
                    SummaryChip("Audio ${bitrateLabel(uiState.convertAudioBitrate)}")
                    SummaryChip("Downloads output")
                }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Filename",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = buildConvertOutputPreview(uiState),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        ToolPanel {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        SectionEyebrow("Advanced")
                    }
                    OutlinedButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "Hide" else "Edit")
                    }
                }
                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        FormatSelectorGroup(
                            title = "Video",
                            formats = VIDEO_OUTPUT_FORMATS,
                            selectedFormat = uiState.convertOutputFormat,
                            onSelect = onOutputFormatChanged,
                        )
                        FormatSelectorGroup(
                            title = "Audio",
                            formats = AUDIO_OUTPUT_FORMATS,
                            selectedFormat = uiState.convertOutputFormat,
                            onSelect = onOutputFormatChanged,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedTextField(
                                value = uiState.convertVideoBitrate,
                                onValueChange = onVideoBitrateChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text("Video") },
                                placeholder = { Text("Auto") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            OutlinedTextField(
                                value = uiState.convertAudioBitrate,
                                onValueChange = onAudioBitrateChanged,
                                modifier = Modifier.weight(1f),
                                label = { Text("Audio") },
                                placeholder = { Text("Auto") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isConverting || uiState.convertResult != null || uiState.convertError != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ToolStatusCard(
                title = when {
                    uiState.isConverting -> "Converting"
                    uiState.convertError != null -> "Stopped"
                    else -> "Done"
                },
                body = uiState.convertError
                    ?: uiState.convertResult
                    ?: "Working...",
                progress = uiState.convertProgress,
                success = uiState.convertResult != null && uiState.convertError == null,
                sourceBytes = uiState.convertSourceSizeBytes,
                resultBytes = uiState.convertResultSizeBytes,
            )
        }

        Button(
            onClick = onConvertClicked,
            enabled = canConvert,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            if (uiState.isConverting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(10.dp))
                Text("Converting")
            } else {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(if (canConvert) "Convert" else "Pick file")
            }
        }
    }
}

@Composable
private fun ConvertHeroCard(
    preset: ConversionPreset,
    inputName: String?,
    onBack: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                        ),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                    SummaryChip("FFmpeg")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp).size(28.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Converter",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Switch format or pull audio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Preset",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = inputName?.let(::compactPath) ?: compactConvertPresetDescription(preset),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConversionPresetCard(
    preset: ConversionPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = if (selected) 3.dp else 0.dp,
        modifier = Modifier
            .widthIn(min = 220.dp, max = 240.dp)
            .animateContentSize()
            .background(color = MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = compactConvertPresetDescription(preset),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip(preset.format.uppercase())
                SummaryChip("Video ${preset.videoBitrateKbps?.let { "${it}k" } ?: "auto"}")
                SummaryChip("Audio ${preset.audioBitrateKbps?.let { "${it}k" } ?: "auto"}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatSelectorGroup(
    title: String,
    formats: List<String>,
    selectedFormat: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            formats.forEach { format ->
                FilterChip(
                    selected = selectedFormat == format,
                    onClick = { onSelect(format) },
                    label = { Text(format.uppercase()) },
                )
            }
        }
    }
}

@Composable
private fun ToolStatusCard(
    title: String,
    body: String,
    progress: Float?,
    success: Boolean,
    sourceBytes: Long?,
    resultBytes: Long?,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (success) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else if (body.isNotBlank() && progress == null) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = when {
                        success -> Icons.Outlined.CheckCircle
                        progress != null -> Icons.Outlined.GraphicEq
                        else -> Icons.Outlined.ErrorOutline
                    },
                    contentDescription = null,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (progress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (sourceBytes != null && resultBytes != null) {
                Text(
                    text = buildSizeDeltaText(sourceBytes, resultBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ToolPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun InputFileCard(
    fileName: String?,
    fileSizeBytes: Long?,
    filePath: String?,
    emptyMessage: String,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (fileName == null) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = fileSizeBytes?.let(::formatFileSize).orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                filePath?.let {
                    Text(
                        text = compactPath(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SummaryChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun buildConvertOutputPreview(uiState: MediaToolsUiState): String {
    val sourceName = uiState.convertInputFileInfo?.name
        ?: uiState.convertInputPath.substringAfterLast('/').substringAfterLast('\\').ifBlank { "converted-media" }
    val baseName = sourceName.substringBeforeLast('.', sourceName)
    return "$baseName.${uiState.convertOutputFormat.lowercase()}"
}

private fun bitrateLabel(rawValue: String): String {
    val parsed = rawValue.trim().toIntOrNull()
    return parsed?.let { "${it} kbps" } ?: "Auto"
}

private fun compactPath(path: String): String {
    val normalized = path.replace('\\', '/')
    val parts = normalized.split('/').filter { it.isNotBlank() }
    return when {
        parts.size <= 3 -> normalized
        else -> ".../${parts.takeLast(3).joinToString("/")}"
    }
}

private fun compactConvertPresetDescription(preset: ConversionPreset): String {
    return when {
        preset.format.lowercase() in setOf("mp3", "wav", "flac", "aac", "m4a", "opus", "ogg") -> "Audio only"
        (preset.videoBitrateKbps ?: 0) >= 2500 -> "Higher quality"
        (preset.videoBitrateKbps ?: 0) in 1..1000 -> "Smaller size"
        else -> "Balanced"
    }
}

private fun buildSizeDeltaText(sourceBytes: Long, resultBytes: Long): String {
    val deltaBytes = resultBytes - sourceBytes
    val percent = if (sourceBytes > 0) {
        (kotlin.math.abs(deltaBytes).toDouble() / sourceBytes * 100).roundToInt()
    } else {
        0
    }
    val direction = if (deltaBytes >= 0) "larger" else "smaller"
    return "${formatFileSize(sourceBytes)} -> ${formatFileSize(resultBytes)}  •  $percent% $direction"
}
