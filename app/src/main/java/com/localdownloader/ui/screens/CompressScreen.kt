package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
import com.localdownloader.ui.components.PreferencePageScaffold
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
        title = "share_fast",
        body = "share_fast_body",
        maxHeight = "480",
        videoBitrate = "500",
        audioBitrate = "96",
    ),
    CompressionGoal(
        title = "balanced",
        body = "balanced_body",
        maxHeight = "720",
        videoBitrate = "1000",
        audioBitrate = "128",
    ),
    CompressionGoal(
        title = "keep_detail",
        body = "keep_detail_body",
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
    var showFineTuning by rememberSaveable { mutableStateOf(false) }
    val canCompress = uiState.compressInputFileInfo != null && !uiState.isCompressing

    PreferencePageScaffold(
        title = stringResource(R.string.compressor_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            CompressPanel {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CompressPanelHeader(title = stringResource(R.string.compressor_section_source))
                    CompressInputFileCard(
                        fileName = uiState.compressInputFileInfo?.name,
                        fileSizeBytes = uiState.compressInputFileInfo?.sizeBytes,
                        filePath = uiState.compressInputFileInfo?.path,
                    )
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
                            Text(stringResource(R.string.compressor_choose_video))
                        }
                        if (uiState.compressInputFileInfo != null) {
                            OutlinedButton(
                                onClick = { onInputPathChanged("") },
                                modifier = Modifier.widthIn(min = 96.dp),
                            ) {
                                Text(stringResource(R.string.common_clear))
                            }
                        }
                    }
                }
            }
        }
        item {
            CompressPanel {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CompressPanelHeader(title = stringResource(R.string.compressor_section_goal))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        QUICK_COMPRESSION_GOALS.forEach { goal ->
                            CompressionGoalCard(
                                goal = goal,
                                title = when (goal.title) {
                                    "share_fast" -> stringResource(R.string.compressor_goal_share_fast)
                                    "balanced" -> stringResource(R.string.compressor_goal_balanced)
                                    else -> stringResource(R.string.compressor_goal_keep_detail)
                                },
                                body = when (goal.body) {
                                    "share_fast_body" -> stringResource(R.string.compressor_goal_share_fast_body)
                                    "balanced_body" -> stringResource(R.string.compressor_goal_balanced_body)
                                    else -> stringResource(R.string.compressor_goal_keep_detail_body)
                                },
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
        }
        item {
            CompressPanel {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CompressPanelHeader(
                        title = stringResource(R.string.compressor_section_output),
                        actionLabel = if (showFineTuning) {
                            stringResource(R.string.common_hide_advanced)
                        } else {
                            stringResource(R.string.common_advanced)
                        },
                        onAction = { showFineTuning = !showFineTuning },
                    )
                    CompressPreviewCard(
                        label = stringResource(R.string.compressor_will_save_as),
                        value = buildCompressedOutputPreview(uiState),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CompressSummaryChip("Max ${uiState.compressMaxHeight.ifBlank { stringResource(R.string.common_auto) }}p")
                        CompressSummaryChip("Video ${bitrateSummary(uiState.compressVideoBitrate)}")
                        CompressSummaryChip("Audio ${bitrateSummary(uiState.compressAudioBitrate)}")
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
                                text = stringResource(R.string.compressor_height),
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
                                label = { Text(stringResource(R.string.compressor_max_height)) },
                                placeholder = { Text("720") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            CompressionDropdownField(
                                label = stringResource(R.string.compressor_video_preset),
                                options = VIDEO_BITRATE_PRESETS.map { it.label },
                                selectedIndex = uiState.compressVideoBitratePresetIndex.coerceIn(0, VIDEO_BITRATE_PRESETS.lastIndex),
                                onSelected = onVideoBitratePresetSelected,
                                supportingText = "",
                            )
                            OutlinedTextField(
                                value = uiState.compressVideoBitrate,
                                onValueChange = onVideoBitrateChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.compressor_video_kbps)) },
                                placeholder = { Text("1000") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            CompressionDropdownField(
                                label = stringResource(R.string.compressor_audio_preset),
                                options = AUDIO_BITRATE_PRESETS.map { it.label },
                                selectedIndex = uiState.compressAudioBitratePresetIndex.coerceIn(0, AUDIO_BITRATE_PRESETS.lastIndex),
                                onSelected = onAudioBitratePresetSelected,
                                supportingText = "",
                            )
                            OutlinedTextField(
                                value = uiState.compressAudioBitrate,
                                onValueChange = onAudioBitrateChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.compressor_audio_kbps)) },
                                placeholder = { Text("128") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                }
            }
        }
        if (uiState.isCompressing || uiState.compressResult != null || uiState.compressError != null) {
            item {
                CompressStatusCard(
                    title = when {
                        uiState.isCompressing -> stringResource(R.string.compressor_compressing)
                        uiState.compressError != null -> stringResource(R.string.common_stopped)
                        else -> stringResource(R.string.common_done)
                    },
                    body = uiState.compressError
                        ?: uiState.compressResult
                        ?: stringResource(R.string.common_working),
                    progress = uiState.compressProgress,
                    success = uiState.compressResult != null && uiState.compressError == null,
                    sourceBytes = uiState.compressSourceSizeBytes,
                    resultBytes = uiState.compressResultSizeBytes,
                )
            }
        }
        item {
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
                    Text(stringResource(R.string.compressor_compressing))
                } else {
                    Icon(Icons.Outlined.Transform, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        if (canCompress) {
                            stringResource(R.string.compressor_action)
                        } else {
                            stringResource(R.string.compressor_pick_file)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompressPanelHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun CompressPreviewCard(
    label: String,
    value: String,
) {
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
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompressionGoalCard(
    goal: CompressionGoal,
    title: String,
    body: String,
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
                    text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
                Text(
                    text = body,
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
