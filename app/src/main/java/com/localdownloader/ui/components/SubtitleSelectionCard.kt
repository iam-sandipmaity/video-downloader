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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
        (nativeList + origAutoList).distinctBy { it.code }
    }

    val totalCount = allTracks.size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        border = BorderStroke(
            width = 1.dp,
            color = if (downloadSubtitles) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Row: Icon + Title + Total Available Badge + Master Switch
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
                        shape = RoundedCornerShape(10.dp),
                        color = if (downloadSubtitles) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier.size(36.dp),
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
                    Spacer(modifier = Modifier.width(10.dp))
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
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (!downloadSubtitles) {
                                "Subtitles disabled"
                            } else if (selectedSubtitleLanguages.isEmpty()) {
                                "All available languages"
                            } else if (selectedSubtitleLanguages.size == 1) {
                                val track = allTracks.firstOrNull { it.code == selectedSubtitleLanguages.first() }
                                track?.displayName ?: selectedSubtitleLanguages.first()
                            } else {
                                "${selectedSubtitleLanguages.size} languages selected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Quick Preset Chips (None, Native/Original, English, All)
                    if (totalCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 2.dp),
                            )

                            if (nativeTracks.isNotEmpty()) {
                                val nativeCodes = nativeTracks.map { it.code }.toSet()
                                val isNativeSelected = selectedSubtitleLanguages.isNotEmpty() &&
                                    selectedSubtitleLanguages.toSet() == nativeCodes
                                FilterChip(
                                    selected = isNativeSelected,
                                    onClick = {
                                        onSubtitleLanguagesChanged(nativeTracks.map { it.code })
                                    },
                                    label = { Text("Native", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                )
                            }

                            val englishTrack = allTracks.firstOrNull { it.code.startsWith("en", ignoreCase = true) }
                            if (englishTrack != null) {
                                val isEnglishSelected = selectedSubtitleLanguages.size == 1 &&
                                    selectedSubtitleLanguages.first() == englishTrack.code
                                FilterChip(
                                    selected = isEnglishSelected,
                                    onClick = {
                                        onSubtitleLanguagesChanged(listOf(englishTrack.code))
                                    },
                                    label = { Text("English", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                )
                            }

                            val isAllSelected = selectedSubtitleLanguages.isEmpty()
                            FilterChip(
                                selected = isAllSelected,
                                onClick = {
                                    onSubtitleLanguagesChanged(emptyList())
                                },
                                label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }

                    // Selected Language Chips Row + Add Button
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
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(12.dp),
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

                        // Open Dialog Button Pill
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenSubtitleDialog),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = if (selectedSubtitleLanguages.isEmpty()) Icons.Outlined.Language else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = if (selectedSubtitleLanguages.isEmpty()) "Select Languages" else "More",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // Options: Embed in container vs Sidecar files
                    if (!isAudioOnly) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.browser_toggle_embed_subs),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = if (embedSubtitles) {
                                            "Embedded directly inside video container"
                                        } else {
                                            "Saved as separate subtitle files (.srt/.vtt)"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
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
