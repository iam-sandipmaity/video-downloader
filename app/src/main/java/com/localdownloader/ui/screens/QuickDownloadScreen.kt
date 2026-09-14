package com.localdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
    modifier: Modifier = Modifier,
) {
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
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )

                    // 3-Way Segmented Button: Video + Audio | Audio | Video only
                    SegmentedThreeWayToggle(
                        selectedType = uiState.selectedStreamType,
                        onStreamTypeSelected = onStreamTypeChanged,
                    )

                    // Quality Row
                    DropdownSelectorRow(
                        label = stringResource(R.string.quick_download_label_quality),
                        selectedText = if (uiState.isAudioMode) {
                            uiState.selectedAudioQuality?.displayLabel ?: "160k"
                        } else {
                            uiState.selectedVideoQuality?.displayLabel ?: "720p"
                        },
                        options = if (uiState.isAudioMode) {
                            uiState.audioQualityOptions.map { it.displayLabel }
                        } else {
                            uiState.videoQualityOptions.map { it.displayLabel }
                        },
                        onOptionSelected = { index ->
                            if (uiState.isAudioMode) {
                                uiState.audioQualityOptions.getOrNull(index)?.let(onAudioQualitySelected)
                            } else {
                                uiState.videoQualityOptions.getOrNull(index)?.let(onVideoQualitySelected)
                            }
                        },
                    )

                    // Format Row
                    DropdownSelectorRow(
                        label = stringResource(R.string.quick_download_label_format),
                        selectedText = if (uiState.isAudioMode) {
                            uiState.selectedAudioFormat?.label ?: "MP3"
                        } else {
                            uiState.selectedVideoFormat?.label ?: "MP4"
                        },
                        options = if (uiState.isAudioMode) {
                            uiState.audioFormatOptions.map { it.label }
                        } else {
                            uiState.videoFormatOptions.map { it.label }
                        },
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
private fun SegmentedThreeWayToggle(
    selectedType: StreamType,
    onStreamTypeSelected: (StreamType) -> Unit,
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
            val options = listOf(
                StreamType.VIDEO_AUDIO to stringResource(R.string.quick_download_mode_video_audio),
                StreamType.AUDIO_ONLY to stringResource(R.string.quick_download_mode_audio),
                StreamType.VIDEO_ONLY to stringResource(R.string.quick_download_mode_video_only),
            )

            options.forEachIndexed { index, (streamType, label) ->
                val isSelected = selectedType == streamType
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(180),
                    label = "segmentBg",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(bgColor)
                        .clickable { onStreamTypeSelected(streamType) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownSelectorRow(
    label: String,
    selectedText: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

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
                    .clickable { expanded = true },
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

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontWeight = if (option == selectedText) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onOptionSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
