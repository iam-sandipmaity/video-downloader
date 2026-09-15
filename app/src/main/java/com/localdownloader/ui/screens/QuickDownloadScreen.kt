package com.localdownloader.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdownloader.R
import com.localdownloader.domain.models.FormatSelectorStyle
import com.localdownloader.domain.models.StreamType
import com.localdownloader.ui.components.QuickOptionBottomSheet
import com.localdownloader.ui.components.QuickOptionItem
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {}, // Intercept clicks inside card
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (uiState.isAudioMode) {
                            stringResource(R.string.quick_download_title_audio)
                        } else {
                            stringResource(R.string.quick_download_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (uiState.isAnalyzing) {
                    // Loading State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.5.dp,
                        )
                        Text(
                            text = stringResource(R.string.quick_download_analyzing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (uiState.errorMessage != null && uiState.videoInfo == null) {
                    // Error State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
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
                } else {
                    // Title Input Box
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = onTitleChanged,
                        label = { Text(stringResource(R.string.quick_download_label_title)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )

                    // 2-Way Segmented Button: [✓ Video | Audio] matching Issue #98 mockup
                    SegmentedTwoWayToggle(
                        isAudioMode = uiState.isAudioMode,
                        onVideoSelected = { onStreamTypeChanged(StreamType.VIDEO_AUDIO) },
                        onAudioSelected = { onStreamTypeChanged(StreamType.AUDIO_ONLY) },
                    )

                    // Quality Row
                    val qualityItems = if (uiState.isAudioMode) {
                        uiState.audioQualityOptions.mapIndexed { index, opt ->
                            QuickOptionItem(
                                id = opt.id,
                                title = opt.title,
                                badge = opt.title,
                                isBest = index == 0,
                                isHighQuality = (opt.bitrateKbps ?: 0) >= 192,
                                sizeOrSupporting = opt.subtitle,
                                isSelected = opt.id == uiState.selectedAudioQuality?.id,
                            )
                        }
                    } else {
                        uiState.videoQualityOptions.mapIndexed { index, opt ->
                            QuickOptionItem(
                                id = opt.id,
                                title = opt.title,
                                badge = opt.title,
                                isBest = index == 0,
                                isHighQuality = (opt.height ?: 0) >= 1080,
                                sizeOrSupporting = opt.subtitle,
                                isSelected = opt.id == uiState.selectedVideoQuality?.id,
                            )
                        }
                    }

                    QuickOptionSelectorRow(
                        label = stringResource(R.string.quick_download_label_quality),
                        dialogTitle = stringResource(R.string.quick_download_select_quality_title),
                        selectedText = if (uiState.isAudioMode) {
                            uiState.selectedAudioQuality?.displayLabel ?: "160k"
                        } else {
                            uiState.selectedVideoQuality?.displayLabel ?: "720p"
                        },
                        items = qualityItems,
                        formatSelectorStyle = uiState.appSettings.formatSelectorStyle,
                        onOptionSelected = { index ->
                            if (uiState.isAudioMode) {
                                uiState.audioQualityOptions.getOrNull(index)?.let(onAudioQualitySelected)
                            } else {
                                uiState.videoQualityOptions.getOrNull(index)?.let(onVideoQualitySelected)
                            }
                        },
                    )

                    // Format Row
                    val formatItems = if (uiState.isAudioMode) {
                        uiState.audioFormatOptions.map { opt ->
                            QuickOptionItem(
                                id = opt.id,
                                title = opt.label,
                                badge = opt.label,
                                secondaryPills = listOfNotNull(opt.container.uppercase()),
                                isSelected = opt.id == uiState.selectedAudioFormat?.id,
                            )
                        }
                    } else {
                        uiState.videoFormatOptions.map { opt ->
                            QuickOptionItem(
                                id = opt.id,
                                title = opt.label,
                                badge = opt.label,
                                secondaryPills = listOfNotNull(
                                    opt.container.uppercase().takeIf { it != "AUTO" },
                                    opt.videoCodec?.uppercase(),
                                ),
                                isSelected = opt.id == uiState.selectedVideoFormat?.id,
                            )
                        }
                    }

                    QuickOptionSelectorRow(
                        label = stringResource(R.string.quick_download_label_format),
                        dialogTitle = stringResource(R.string.quick_download_select_format_title),
                        selectedText = if (uiState.isAudioMode) {
                            uiState.selectedAudioFormat?.label ?: "MP3"
                        } else {
                            uiState.selectedVideoFormat?.label ?: "H264 · MP4"
                        },
                        items = formatItems,
                        formatSelectorStyle = uiState.appSettings.formatSelectorStyle,
                        onOptionSelected = { index ->
                            if (uiState.isAudioMode) {
                                uiState.audioFormatOptions.getOrNull(index)?.let(onAudioFormatSelected)
                            } else {
                                uiState.videoFormatOptions.getOrNull(index)?.let(onVideoFormatSelected)
                            }
                        },
                    )

                    // Threads Stepper Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.quick_download_label_threads),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = uiState.threads > 1) {
                                        onThreadsChanged(uiState.threads - 1)
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease threads",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (uiState.threads > 1) {
                                             MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        },
                                    )
                                }
                            }

                            Text(
                                text = uiState.threads.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 28.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = uiState.threads < 16) {
                                        onThreadsChanged(uiState.threads + 1)
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase threads",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (uiState.threads < 16) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons: Cancel and Download
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.common_cancel),
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }

                        Button(
                            onClick = onDownloadClicked,
                            enabled = !uiState.isQueueing,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000),
                                contentColor = Color.White,
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 26.dp,
                                vertical = 12.dp,
                            ),
                        ) {
                            if (uiState.isQueueing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.quick_download_action_download),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedTwoWayToggle(
    isAudioMode: Boolean,
    onVideoSelected: () -> Unit,
    onAudioSelected: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            val isVideo = !isAudioMode
            val videoBgColor by animateColorAsState(
                targetValue = if (isVideo) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(180),
                label = "videoBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(videoBgColor)
                    .clickable { onVideoSelected() },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    if (isVideo) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.quick_download_mode_video),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val isAudio = isAudioMode
            val audioBgColor by animateColorAsState(
                targetValue = if (isAudio) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(180),
                label = "audioBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(audioBgColor)
                    .clickable { onAudioSelected() },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                ) {
                    if (isAudio) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.quick_download_mode_audio),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isAudio) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickOptionSelectorRow(
    label: String,
    dialogTitle: String,
    selectedText: String,
    items: List<QuickOptionItem>,
    formatSelectorStyle: FormatSelectorStyle,
    onOptionSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        QuickOptionBottomSheet(
            title = dialogTitle,
            subtitle = stringResource(R.string.format_picker_subtitle, items.size),
            options = items,
            onOptionSelected = onOptionSelected,
            onDismissRequest = { showBottomSheet = false },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        if (formatSelectorStyle == FormatSelectorStyle.BOTTOM_SHEET) {
                            showBottomSheet = true
                        } else {
                            expanded = true
                        }
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = selectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (formatSelectorStyle == FormatSelectorStyle.DROPDOWN) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = item.isSelected
                        val primaryText = item.badge ?: item.title
                        val supportingText = item.sizeOrSupporting
                        DropdownMenuItem(
                            text = {
                                if (supportingText != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = primaryText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = supportingText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = primaryText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 2.dp),
                                    )
                                }
                            },
                            onClick = {
                                onOptionSelected(index)
                                expanded = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
