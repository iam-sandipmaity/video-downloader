package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.localdownloader.R
import com.localdownloader.domain.models.StreamType
import com.localdownloader.viewmodel.QuickDownloadUiState
import com.localdownloader.viewmodel.QuickFormatOption
import com.localdownloader.viewmodel.QuickQualityOption

@Composable
fun QuickDownloadScreen(
    uiState: QuickDownloadUiState,
    onDismiss: () -> Unit,
    onStreamTypeChanged: (StreamType) -> Unit,
    onTitleChanged: (String) -> Unit,
    onVideoQualitySelected: (QuickQualityOption) -> Unit,
    onAudioQualitySelected: (QuickQualityOption) -> Unit,
    onVideoFormatSelected: (QuickFormatOption) -> Unit,
    onAudioFormatSelected: (QuickFormatOption) -> Unit,
    onThreadsChanged: (Int) -> Unit,
    onDownloadClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onShowTitleEditDialog: (Boolean) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onDismissMeteredNetworkDialog: () -> Unit = {},
    onAllowCellularAndDownload: () -> Unit = {},
    onDownloadWhenWifiAvailable: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (uiState.showMeteredNetworkDialog) {
        AlertDialog(
            onDismissRequest = onDismissMeteredNetworkDialog,
            title = { Text(stringResource(R.string.browser_metered_title)) },
            text = { Text(stringResource(R.string.browser_metered_body)) },
            confirmButton = {
                TextButton(onClick = onAllowCellularAndDownload) {
                    Text(stringResource(R.string.browser_allow_cellular))
                }
            },
            dismissButton = {
                TextButton(onClick = onDownloadWhenWifiAvailable) {
                    Text(stringResource(R.string.browser_wait_for_wifi))
                }
            },
        )
    }

    if (uiState.showTitleEditDialog) {
        var tempTitle by remember(uiState.title) { mutableStateOf(uiState.title) }
        AlertDialog(
            onDismissRequest = { onShowTitleEditDialog(false) },
            title = { Text(stringResource(R.string.quick_download_edit_title_dialog)) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text(stringResource(R.string.quick_download_label_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTitleChanged(tempTitle)
                        onShowTitleEditDialog(false)
                    },
                ) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowTitleEditDialog(false) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // Modal Bottom Sheet Container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .clickable(enabled = false) {}, // Prevent backdrop clicks through sheet
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )

                if (uiState.isAnalyzing) {
                    // Shimmer Skeleton Loader during Link Analysis
                    QuickDownloadSkeletonLoader(onDismiss = onDismiss)
                } else if (uiState.errorMessage != null && uiState.videoInfo == null) {
                    // Error State
                    QuickDownloadErrorContent(
                        errorMessage = uiState.errorMessage,
                        onDismiss = onDismiss,
                        onRetryClicked = onRetryClicked,
                    )
                } else {
                    // Rich Media Content & Categorized Selection Flow
                    QuickMediaHeaderCard(
                        thumbnailUrl = uiState.thumbnailUrl,
                        durationFormatted = uiState.durationFormatted,
                        title = uiState.title,
                        uploader = uiState.uploader,
                        domainHost = uiState.domainHost,
                        onEditTitleClicked = { onShowTitleEditDialog(true) },
                        onCloseClicked = onDismiss,
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        thickness = 0.75.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )

                    // Scrollable Area for Categorized Grids and Options
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // 🎵 Audio / Music Section
                        if (uiState.audioQualityOptions.isNotEmpty()) {
                            CategorizedSection(
                                title = stringResource(R.string.quick_download_section_music),
                                icon = Icons.Outlined.Audiotrack,
                                isSectionActive = uiState.isAudioMode,
                            ) {
                                QuickMediaOptionGrid(
                                    options = uiState.audioQualityOptions,
                                    selectedOptionId = if (uiState.isAudioMode) uiState.selectedAudioQuality?.id else null,
                                    isAudio = true,
                                    onOptionSelected = onAudioQualitySelected,
                                )
                            }
                        }

                        // 🎬 Video Section
                        if (uiState.videoQualityOptions.isNotEmpty()) {
                            CategorizedSection(
                                title = stringResource(R.string.quick_download_section_video),
                                icon = Icons.Outlined.Videocam,
                                isSectionActive = !uiState.isAudioMode,
                            ) {
                                QuickMediaOptionGrid(
                                    options = uiState.videoQualityOptions,
                                    selectedOptionId = if (!uiState.isAudioMode) uiState.selectedVideoQuality?.id else null,
                                    isAudio = false,
                                    onOptionSelected = onVideoQualitySelected,
                                )
                            }
                        }

                        // ⚙️ Collapsible Quick Tuning Drawer (Threads & Container)
                        QuickTuningDrawer(
                            isExpanded = uiState.isQuickSettingsExpanded,
                            threads = uiState.threads,
                            isAudioMode = uiState.isAudioMode,
                            selectedFormat = if (uiState.isAudioMode) uiState.selectedAudioFormat else uiState.selectedVideoFormat,
                            formatOptions = if (uiState.isAudioMode) uiState.audioFormatOptions else uiState.videoFormatOptions,
                            onToggleExpanded = onToggleQuickSettings,
                            onThreadsChanged = onThreadsChanged,
                            onFormatSelected = { format ->
                                if (uiState.isAudioMode) {
                                    onAudioFormatSelected(format)
                                } else {
                                    onVideoFormatSelected(format)
                                }
                            },
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // ⬇️ Sticky Bottom Action Bar
                    QuickDownloadStickyActionBar(
                        label = uiState.ctaButtonLabel,
                        isQueueing = uiState.isQueueing,
                        onDownloadClicked = onDownloadClicked,
                    )
                }
            }
        }
    }
}

/**
 * Rich Media Header Card showing thumbnail artwork, duration pill overlay,
 * editable title, and uploader / source metadata.
 */
@Composable
private fun QuickMediaHeaderCard(
    thumbnailUrl: String?,
    durationFormatted: String?,
    title: String,
    uploader: String?,
    domainHost: String?,
    onEditTitleClicked: () -> Unit,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail with Duration Pill
        Box(
            modifier = Modifier
                .size(width = 86.dp, height = 54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            if (!durationFormatted.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp),
                ) {
                    Text(
                        text = durationFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }

        // Title and Metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val metaText = listOfNotNull(
                uploader?.takeIf { it.isNotBlank() },
                domainHost?.takeIf { it.isNotBlank() },
            ).joinToString(" • ")

            if (metaText.isNotBlank()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                )
            }
        }

        // Actions: Edit Title and Close
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = onEditTitleClicked,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.quick_download_edit_title_dialog),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(
                onClick = onCloseClicked,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Categorized Section Header with Icon
 */
@Composable
private fun CategorizedSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSectionActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSectionActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSectionActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        content()
    }
}

/**
 * 2-Column Selectable Grid for Quality & Format Tiles
 */
@Composable
private fun QuickMediaOptionGrid(
    options: List<QuickQualityOption>,
    selectedOptionId: String?,
    isAudio: Boolean,
    onOptionSelected: (QuickQualityOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = options.chunked(2)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowOptions.forEach { option ->
                    val isSelected = option.id == selectedOptionId
                    QuickOptionCard(
                        option = option,
                        isSelected = isSelected,
                        isAudio = isAudio,
                        onClick = { onOptionSelected(option) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // If odd number of items in row, fill spacer
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Individual Selectable Card Tile
 */
@Composable
private fun QuickOptionCard(
    option: QuickQualityOption,
    isSelected: Boolean,
    isAudio: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        },
        label = "borderColor",
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        label = "containerColor",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier.height(58.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Top line: Quality Title + Badge Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val isHighQuality = (option.height != null && option.height >= 1080) ||
                        (option.bitrateKbps != null && option.bitrateKbps >= 320)

                    if (isHighQuality) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = stringResource(R.string.quick_download_hq_tag),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }

                // Bottom line: Subtitle with file size
                if (!option.subtitle.isNullOrBlank()) {
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }

            // Selection Checkmark Indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

/**
 * Collapsible Quick Tuning Drawer for Threads and Format Container
 */
@Composable
private fun QuickTuningDrawer(
    isExpanded: Boolean,
    threads: Int,
    isAudioMode: Boolean,
    selectedFormat: QuickFormatOption?,
    formatOptions: List<QuickFormatOption>,
    onToggleExpanded: () -> Unit,
    onThreadsChanged: (Int) -> Unit,
    onFormatSelected: (QuickFormatOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Toggle Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${stringResource(R.string.quick_download_quick_settings)} (${stringResource(R.string.quick_download_label_threads)}: $threads • ${selectedFormat?.label ?: "Auto"})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expanded Controls
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )

                    // Threads Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.quick_download_label_threads),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = threads > 1) {
                                        onThreadsChanged(threads - 1)
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease threads",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (threads > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    )
                                }
                            }

                            Text(
                                text = threads.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 24.dp),
                                textAlign = TextAlign.Center,
                            )

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = threads < 16) {
                                        onThreadsChanged(threads + 1)
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase threads",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (threads < 16) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    )
                                }
                            }
                        }
                    }

                    // Container / Codec Format Row
                    if (formatOptions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.quick_download_label_format),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                formatOptions.take(4).forEach { format ->
                                    val isSelected = format.id == selectedFormat?.id
                                    Surface(
                                        onClick = { onFormatSelected(format) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.height(28.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = format.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sticky Bottom Dynamic Action Bar with prominent CTA
 */
@Composable
private fun QuickDownloadStickyActionBar(
    label: String,
    isQueueing: Boolean,
    onDownloadClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        color = Color.Transparent,
    ) {
        Button(
            onClick = onDownloadClicked,
            enabled = !isQueueing,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (isQueueing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

/**
 * Shimmer Skeleton Loader shown while extracting video metadata
 */
@Composable
private fun QuickDownloadSkeletonLoader(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Skeleton Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 86.dp, height = 54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
                )
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Skeleton Cards 2x2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
                    )
                }
            }
        }

        // Analyzing indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.quick_download_analyzing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Error content with retry and cancel buttons
 */
@Composable
private fun QuickDownloadErrorContent(
    errorMessage: String,
    onDismiss: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
            Button(
                onClick = onRetryClicked,
                shape = RoundedCornerShape(50),
            ) {
                Text(stringResource(R.string.quick_download_retry))
            }
        }
    }
}
