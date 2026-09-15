package com.localdownloader.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdownloader.R
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.FormatChoice
import com.localdownloader.domain.models.StreamType
import com.localdownloader.ui.model.toReadableSize

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormatSelectionBottomSheet(
    choices: List<FormatChoice>,
    selectedFormatSelector: String?,
    streamType: StreamType,
    onStreamTypeChanged: (StreamType) -> Unit,
    hasVideoAudioChoices: Boolean,
    hasVideoOnlyChoices: Boolean,
    hasAudioOnlyChoices: Boolean,
    requestedContainer: String,
    appSettings: AppSettings,
    onFormatSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(50),
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.format_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.format_picker_subtitle, choices.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Stream Type filter chips row
            val hasMultipleTypes = listOf(hasVideoAudioChoices, hasVideoOnlyChoices, hasAudioOnlyChoices).count { it } > 1
            if (hasMultipleTypes) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (hasVideoAudioChoices) {
                        StreamTypeFilterChip(
                            label = stringResource(R.string.format_tab_video_audio),
                            icon = Icons.Rounded.Movie,
                            isSelected = streamType == StreamType.VIDEO_AUDIO,
                            onClick = { onStreamTypeChanged(StreamType.VIDEO_AUDIO) },
                        )
                    }
                    if (hasVideoOnlyChoices) {
                        StreamTypeFilterChip(
                            label = stringResource(R.string.format_tab_video_only),
                            icon = Icons.Rounded.Videocam,
                            isSelected = streamType == StreamType.VIDEO_ONLY,
                            onClick = { onStreamTypeChanged(StreamType.VIDEO_ONLY) },
                        )
                    }
                    if (hasAudioOnlyChoices) {
                        StreamTypeFilterChip(
                            label = stringResource(R.string.format_tab_audio_only),
                            icon = Icons.Rounded.Audiotrack,
                            isSelected = streamType == StreamType.AUDIO_ONLY,
                            onClick = { onStreamTypeChanged(StreamType.AUDIO_ONLY) },
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Format choices list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                itemsIndexed(choices) { index, choice ->
                    val isSelected = choice.selector == selectedFormatSelector ||
                        (selectedFormatSelector == null && index == 0)
                    val isBest = index == 0

                    FormatChoiceCard(
                        choice = choice,
                        isSelected = isSelected,
                        isBest = isBest,
                        appSettings = appSettings,
                        onClick = {
                            onFormatSelected(choice.selector)
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamTypeFilterChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatChoiceCard(
    choice: FormatChoice,
    isSelected: Boolean,
    isBest: Boolean,
    appSettings: AppSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolutionLabel = choice.height?.let { "${it}p" }
        ?: choice.label.substringBefore(' ').takeIf { it.isNotBlank() }
        ?: if (choice.streamType == StreamType.AUDIO_ONLY) "Audio" else "Video"

    val primarySize = choice.fileSizeBytes?.toReadableSize()
        ?: choice.estimatedSizeBytes?.let { "~${it.toReadableSize()}" }

    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        },
        animationSpec = tween(150),
        label = "cardBorder",
    )

    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(150),
        label = "cardBg",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Left content: Badges and Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Top Row: Resolution / Main Badge + Tags
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Quality / Resolution badge
                    QualityBadge(
                        text = resolutionLabel,
                        isHighQuality = (choice.height ?: 0) >= 1080 || choice.streamType == StreamType.AUDIO_ONLY,
                        isSelected = isSelected,
                    )

                    // Best / Recommended Tag
                    if (isBest) {
                        PillBadge(
                            text = stringResource(R.string.format_badge_best),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            isBold = true,
                        )
                    }

                    // FPS badge (if applicable and enabled)
                    if (appSettings.showFormatFps && choice.fps != null && choice.fps > 0) {
                        val fpsInt = choice.fps.toInt()
                        if (fpsInt >= 50) {
                            PillBadge(
                                text = "${fpsInt}fps",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                isBold = true,
                            )
                        } else if (appSettings.showFormatFps) {
                            PillBadge(
                                text = "${fpsInt}fps",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Container badge
                    if (choice.container.isNotBlank()) {
                        PillBadge(
                            text = choice.container.uppercase(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Bottom Row: Codecs, Bitrate, and Merged metadata
                val metadataItems = buildList {
                    if (appSettings.showFormatCodec) {
                        choice.videoCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
                            add("v:${compactCodecName(it)}")
                        }
                        choice.audioCodec?.takeIf { it.isNotBlank() && !it.equals("none", ignoreCase = true) }?.let {
                            add("a:${compactCodecName(it)}")
                        }
                    }
                    if (appSettings.showFormatBitrate && choice.bitrateKbps != null && choice.bitrateKbps > 0) {
                        add("${choice.bitrateKbps} kbps")
                    }
                    if (choice.isMerged) {
                        add(stringResource(R.string.format_badge_merged))
                    }
                }

                if (metadataItems.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        metadataItems.forEach { item ->
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Right side: Size pill + Radio selection
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                primarySize?.let { size ->
                    Text(
                        text = size,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun QualityBadge(
    text: String,
    isHighQuality: Boolean,
    isSelected: Boolean,
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isHighQuality -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isHighQuality -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    isBold: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun compactCodecName(codec: String): String {
    return codec
        .substringBefore('.')
        .substringBefore(':')
        .uppercase()
}
