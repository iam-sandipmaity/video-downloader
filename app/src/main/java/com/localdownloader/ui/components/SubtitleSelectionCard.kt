package com.localdownloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdownloader.R
import com.localdownloader.domain.models.SubtitleTrack

/**
 * Modern, dedicated Subtitle Selection Card for Download configuration sheets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleSelectionCard(
    downloadSubtitles: Boolean,
    onDownloadSubtitlesChanged: (Boolean) -> Unit,
    selectedSubtitleLanguages: List<String>,
    onSubtitleLanguagesChanged: (List<String>) -> Unit,
    availableSubtitles: List<SubtitleTrack>,
    availableAutoCaptions: List<SubtitleTrack>,
    onOpenSubtitleDialog: () -> Unit,
    embedSubtitles: Boolean,
    onEmbedSubtitlesChanged: (Boolean) -> Unit,
    isAudioOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    val allTracks = remember(availableSubtitles, availableAutoCaptions) {
        (availableSubtitles + availableAutoCaptions).distinctBy { it.code }
    }

    val nativeTracks = remember(availableSubtitles, availableAutoCaptions) {
        val nativeList = availableSubtitles
        val origAutoList = availableAutoCaptions.filter { it.isOriginal }
        val combined = (nativeList + origAutoList).distinctBy { it.code }
        if (combined.isNotEmpty()) combined else availableSubtitles.ifEmpty { availableAutoCaptions.take(1) }
    }
    val nativeCodes = remember(nativeTracks) {
        nativeTracks.map { it.code }.distinct()
    }

    val totalCount = allTracks.size
    val isAllNativeSelected = downloadSubtitles && nativeCodes.isNotEmpty() &&
        selectedSubtitleLanguages.toSet() == nativeCodes.toSet()
    val isCustomSelected = downloadSubtitles && !isAllNativeSelected && selectedSubtitleLanguages.isNotEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(
            width = 1.dp,
            color = if (downloadSubtitles) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row: Subtitle Icon + Title & Summary + Available Badge + Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (downloadSubtitles) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Subtitles,
                                contentDescription = null,
                                tint = if (downloadSubtitles) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.subtitle_selection_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (totalCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ) {
                                    Text(
                                        text = "$totalCount available",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = when {
                                !downloadSubtitles -> "Disabled"
                                totalCount == 0 -> "No subtitle tracks found"
                                isAllNativeSelected -> if (nativeCodes.size > 1) "All native languages (${nativeCodes.size})" else "Native language selected"
                                selectedSubtitleLanguages.size == 1 -> {
                                    val matched = allTracks.firstOrNull { it.code == selectedSubtitleLanguages.first() }
                                    matched?.displayName ?: selectedSubtitleLanguages.first()
                                }
                                selectedSubtitleLanguages.isEmpty() -> "No language selected"
                                else -> "${selectedSubtitleLanguages.size} languages selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (downloadSubtitles) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Switch(
                    checked = downloadSubtitles,
                    onCheckedChange = onDownloadSubtitlesChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            // Expanded Subtitle Configuration
            AnimatedVisibility(
                visible = downloadSubtitles,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )

                    // Presets Row (All Native / English / Custom Manage)
                    if (totalCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // "All Native" chip
                            FilterChip(
                                selected = isAllNativeSelected,
                                onClick = {
                                    if (isAllNativeSelected) {
                                        onSubtitleLanguagesChanged(emptyList())
                                    } else {
                                        onSubtitleLanguagesChanged(nativeCodes)
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (nativeCodes.size > 1) "All Native (${nativeCodes.size})" else "All Native",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isAllNativeSelected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                leadingIcon = if (isAllNativeSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                } else null,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp),
                            )

                            // "English" chip if available
                            val englishTrack = allTracks.firstOrNull { it.code.startsWith("en", ignoreCase = true) }
                            if (englishTrack != null) {
                                val isEnglishOnly = selectedSubtitleLanguages.size == 1 &&
                                    selectedSubtitleLanguages.first() == englishTrack.code
                                FilterChip(
                                    selected = isEnglishOnly,
                                    onClick = {
                                        if (isEnglishOnly) {
                                            onSubtitleLanguagesChanged(emptyList())
                                        } else {
                                            onSubtitleLanguagesChanged(listOf(englishTrack.code))
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = "English",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isEnglishOnly) FontWeight.Bold else FontWeight.Medium,
                                        )
                                    },
                                    leadingIcon = if (isEnglishOnly) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp),
                                )
                            }

                            // "Customize" button
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenSubtitleDialog),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCustomSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isCustomSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    },
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isCustomSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(
                                        text = if (isCustomSelected) {
                                            "Custom (${selectedSubtitleLanguages.size})"
                                        } else {
                                            "Choose…"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCustomSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }

                    // Display selected languages chips if custom list is selected
                    if (selectedSubtitleLanguages.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            selectedSubtitleLanguages.forEach { code ->
                                val matched = allTracks.firstOrNull { it.code == code }
                                val label = matched?.displayName ?: code
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        val updated = selectedSubtitleLanguages.filter { it != code }
                                        onSubtitleLanguagesChanged(updated)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove $label",
                                            modifier = Modifier.size(13.dp),
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier.height(28.dp),
                                )
                            }
                        }
                    }

                    // Options: Embed subtitles inside container vs external sidecar files (.srt/.vtt)
                    if (!isAudioOnly) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.browser_toggle_embed_subs),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = if (embedSubtitles) {
                                                "Muxed directly inside video file"
                                            } else {
                                                "Saved as separate .srt/.vtt sidecar files"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                                Switch(
                                    checked = embedSubtitles,
                                    onCheckedChange = onEmbedSubtitlesChanged,
                                    modifier = Modifier.size(height = 24.dp, width = 42.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

