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
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import com.localdownloader.viewmodel.AUDIO_BITRATE_PRESETS
import com.localdownloader.viewmodel.MediaToolsUiState
import com.localdownloader.viewmodel.RESOLUTION_PRESETS
import com.localdownloader.viewmodel.VIDEO_BITRATE_PRESETS
import com.localdownloader.viewmodel.formatFileSize
import kotlin.math.abs
import kotlin.math.roundToInt

private data class CompressionGoal(
    val title: String,
    val body: String,
    val maxHeight: String,
    val videoBitrate: String,
    val audioBitrate: String,
)

private val QUICK_COMPRESSION_GOALS = listOf(
    CompressionGoal(
        title = "Share fast",
        body = "Great for messaging apps and faster uploads.",
        maxHeight = "480",
        videoBitrate = "500",
        audioBitrate = "96",
    ),
    CompressionGoal(
        title = "Balanced",
        body = "A safer everyday choice for mixed content.",
        maxHeight = "720",
        videoBitrate = "1000",
        audioBitrate = "128",
    ),
    CompressionGoal(
        title = "Keep detail",
        body = "Use when the image matters more than size savings.",
        maxHeight = "1080",
        videoBitrate = "2500",
        audioBitrate = "192",
    ),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompressScreen(
    uiState: MediaToolsUiState,
    onInputPathChanged: (String) -> Unit,
    onResolutionPresetSelected: (Int) -> Unit,
    onVideoBitratePresetSelected: (Int) -> Unit,
    onAudioBitratePresetSelected: (Int) -> Unit,
    onMaxHeightChanged: (String) -> Unit,
    onVideoBitrateChanged: (String) -> Unit,
    onAudioBitrateChanged: (String) -> Unit,
    onCompressClicked: () -> Unit,
    onBrowseFile: () -> Unit,
    onCompressQuickPresetSelected: (String, String, String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showFineTuning by rememberSaveable { mutableStateOf(true) }
    val canCompress = uiState.compressInputFileInfo != null && !uiState.isCompressing
    val selectedResolution = RESOLUTION_PRESETS
        .getOrElse(uiState.compressResolutionPresetIndex.coerceIn(0, RESOLUTION_PRESETS.lastIndex)) {
            RESOLUTION_PRESETS.first()
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        CompressHeroCard(
            selectedHeight = selectedResolution.label,
            inputName = uiState.compressInputFileInfo?.name,
            onBack = onBack,
        )

        CompressPanel {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CompressEyebrow("File")
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
                        Text("Choose video")
                    }
                    if (uiState.compressInputFileInfo != null) {
                        OutlinedButton(
                            onClick = { onInputPathChanged("") },
                            modifier = Modifier.widthIn(min = 96.dp),
                        ) {
                            Text("Clear")
                        }
                    }
                }
                CompressInputFileCard(
                    fileName = uiState.compressInputFileInfo?.name,
                    fileSizeBytes = uiState.compressInputFileInfo?.sizeBytes,
                    filePath = uiState.compressInputFileInfo?.path,
                )
            }
        }

        CompressPanel {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CompressEyebrow("Goals")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QUICK_COMPRESSION_GOALS.forEach { goal ->
                        CompressionGoalCard(
                            goal = goal,
                            selected = uiState.compressMaxHeight == goal.maxHeight &&
                                uiState.compressVideoBitrate == goal.videoBitrate &&
                                uiState.compressAudioBitrate == goal.audioBitrate,
                            onClick = {
                                onCompressQuickPresetSelected(
                                    goal.videoBitrate,
                                    goal.audioBitrate,
                                    goal.maxHeight,
                                )
                            },
                        )
                    }
                }
            }
        }

        CompressPanel {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CompressEyebrow("Output")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompressSummaryChip("Max ${uiState.compressMaxHeight.ifBlank { "Auto" }}p")
                    CompressSummaryChip("Video ${bitrateSummary(uiState.compressVideoBitrate)}")
                    CompressSummaryChip("Audio ${bitrateSummary(uiState.compressAudioBitrate)}")
                    CompressSummaryChip("Downloads output")
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
                            text = buildCompressedOutputPreview(uiState),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        CompressPanel {
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
                        CompressEyebrow("Advanced")
                    }
                    OutlinedButton(onClick = { showFineTuning = !showFineTuning }) {
                        Text(if (showFineTuning) "Hide" else "Edit")
                    }
                }
                AnimatedVisibility(
                    visible = showFineTuning,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        modifier = Modifier.animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Height",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RESOLUTION_PRESETS.forEachIndexed { index, preset ->
                                FilterChip(
                                    selected = uiState.compressResolutionPresetIndex == index,
                                    onClick = { onResolutionPresetSelected(index) },
                                    label = { Text(preset.label) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = uiState.compressMaxHeight,
                            onValueChange = onMaxHeightChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Max height") },
                            placeholder = { Text("720") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        CompressionDropdownField(
                            label = "Video preset",
                            options = VIDEO_BITRATE_PRESETS.map { it.label },
                            selectedIndex = uiState.compressVideoBitratePresetIndex.coerceIn(0, VIDEO_BITRATE_PRESETS.lastIndex),
                            onSelected = onVideoBitratePresetSelected,
                            supportingText = "",
                        )
                        OutlinedTextField(
                            value = uiState.compressVideoBitrate,
                            onValueChange = onVideoBitrateChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Video kbps") },
                            placeholder = { Text("1000") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        CompressionDropdownField(
                            label = "Audio preset",
                            options = AUDIO_BITRATE_PRESETS.map { it.label },
                            selectedIndex = uiState.compressAudioBitratePresetIndex.coerceIn(0, AUDIO_BITRATE_PRESETS.lastIndex),
                            onSelected = onAudioBitratePresetSelected,
                            supportingText = "",
                        )
                        OutlinedTextField(
                            value = uiState.compressAudioBitrate,
                            onValueChange = onAudioBitrateChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Audio kbps") },
                            placeholder = { Text("128") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isCompressing || uiState.compressResult != null || uiState.compressError != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CompressStatusCard(
                title = when {
                    uiState.isCompressing -> "Compressing"
                    uiState.compressError != null -> "Stopped"
                    else -> "Done"
                },
                body = uiState.compressError
                    ?: uiState.compressResult
                    ?: "Working...",
                progress = uiState.compressProgress,
                success = uiState.compressResult != null && uiState.compressError == null,
                sourceBytes = uiState.compressSourceSizeBytes,
                resultBytes = uiState.compressResultSizeBytes,
            )
        }

        Button(
            onClick = onCompressClicked,
            enabled = canCompress,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            if (uiState.isCompressing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(10.dp))
                Text("Compressing")
            } else {
                Icon(Icons.Outlined.Transform, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(if (canCompress) "Compress" else "Pick video")
            }
        }
    }
}

@Composable
private fun CompressHeroCard(
    selectedHeight: String,
    inputName: String?,
    onBack: (() -> Unit)?,
) {
    Surface(
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
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
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
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
                    CompressSummaryChip(selectedHeight)
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
                            imageVector = Icons.Outlined.Transform,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(16.dp).size(28.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Compressor",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Shrink video for sharing.",
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
                            text = "Target",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "Max ${selectedHeight}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = inputName?.let(::compactCompressPath) ?: "Quick size cut",
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
private fun CompressionGoalCard(
    goal: CompressionGoal,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = if (selected) 3.dp else 0.dp,
        modifier = Modifier
            .widthIn(min = 220.dp, max = 240.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = goal.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompressSummaryChip("${goal.maxHeight}p")
                CompressSummaryChip("Video ${goal.videoBitrate}k")
                CompressSummaryChip("Audio ${goal.audioBitrate}k")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompressionDropdownField(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    supportingText: String,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = options.getOrElse(selectedIndex) { options.first() },
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            )
            androidx.compose.material3.DropdownMenu(
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
                    )
                }
            }
        }
        if (supportingText.isNotBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompressStatusCard(
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
                    text = buildCompressDeltaText(sourceBytes, resultBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompressPanel(content: @Composable ColumnScope.() -> Unit) {
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
private fun CompressInputFileCard(
    fileName: String?,
    fileSizeBytes: Long?,
    filePath: String?,
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
                    text = "No video yet",
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
                        text = compactCompressPath(it),
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
private fun CompressEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun CompressSummaryChip(text: String) {
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

private fun buildCompressedOutputPreview(uiState: MediaToolsUiState): String {
    val sourceName = uiState.compressInputFileInfo?.name
        ?: uiState.compressInputPath.substringAfterLast('/').substringAfterLast('\\').ifBlank { "compressed-media.mp4" }
    val baseName = sourceName.substringBeforeLast('.', sourceName)
    val extension = sourceName.substringAfterLast('.', "mp4")
    return "${baseName}_compressed.$extension"
}

private fun bitrateSummary(rawValue: String): String {
    val parsed = rawValue.trim().toIntOrNull()
    return parsed?.let { "${it} kbps" } ?: "Auto"
}

private fun compactCompressPath(path: String): String {
    val normalized = path.replace('\\', '/')
    val parts = normalized.split('/').filter { it.isNotBlank() }
    return when {
        parts.size <= 3 -> normalized
        else -> ".../${parts.takeLast(3).joinToString("/")}"
    }
}

private fun buildCompressDeltaText(sourceBytes: Long, resultBytes: Long): String {
    val delta = sourceBytes - resultBytes
    val percent = if (sourceBytes > 0) {
        (abs(delta).toDouble() / sourceBytes * 100).roundToInt()
    } else {
        0
    }
    val summary = if (delta >= 0) "$percent% smaller" else "$percent% larger"
    return "${formatFileSize(sourceBytes)} -> ${formatFileSize(resultBytes)}  •  $summary"
}
