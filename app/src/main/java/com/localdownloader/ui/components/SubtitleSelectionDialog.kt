package com.localdownloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun SubtitleSelectionDialog(
    availableSubtitles: List<SubtitleTrack>,
    availableAutoCaptions: List<SubtitleTrack>,
    selectedLanguages: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (selectedLangs: List<String>) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedCodes = remember { mutableStateListOf<String>().apply { addAll(selectedLanguages) } }

    val filteredSuggested = remember(availableSubtitles, searchQuery) {
        if (searchQuery.isBlank()) availableSubtitles
        else availableSubtitles.filter { track ->
            track.code.contains(searchQuery, ignoreCase = true) ||
                track.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredAuto = remember(availableAutoCaptions, searchQuery) {
        if (searchQuery.isBlank()) availableAutoCaptions
        else availableAutoCaptions.filter { track ->
            track.code.contains(searchQuery, ignoreCase = true) ||
                track.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalTracksCount = availableSubtitles.size + availableAutoCaptions.size

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Subtitles,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(R.string.subtitle_selection_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (totalTracksCount > 4) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.subtitle_search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.subtitle_selected_count, selectedCodes.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        TextButton(
                            onClick = {
                                val allCodes = (availableSubtitles + availableAutoCaptions).map { it.code }.distinct()
                                selectedCodes.clear()
                                selectedCodes.addAll(allCodes)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(text = stringResource(R.string.subtitle_select_all), style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = { selectedCodes.clear() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(text = stringResource(R.string.subtitle_clear_all), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (filteredSuggested.isEmpty() && filteredAuto.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.subtitle_no_subtitles_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        if (filteredSuggested.isNotEmpty()) {
                            item(key = "header_suggested") {
                                Text(
                                    text = stringResource(R.string.subtitle_suggested_header),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                )
                            }
                            items(filteredSuggested, key = { "sub_${it.code}" }) { track ->
                                SubtitleCheckboxRow(
                                    track = track,
                                    isChecked = selectedCodes.contains(track.code),
                                    onToggle = { isChecked ->
                                        if (isChecked) {
                                            if (!selectedCodes.contains(track.code)) selectedCodes.add(track.code)
                                        } else {
                                            selectedCodes.remove(track.code)
                                        }
                                    },
                                )
                            }
                        }

                        if (filteredAuto.isNotEmpty()) {
                            item(key = "header_auto") {
                                Text(
                                    text = stringResource(R.string.subtitle_auto_header),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                )
                            }
                            items(filteredAuto, key = { "auto_${it.code}" }) { track ->
                                SubtitleCheckboxRow(
                                    track = track,
                                    isChecked = selectedCodes.contains(track.code),
                                    onToggle = { isChecked ->
                                        if (isChecked) {
                                            if (!selectedCodes.contains(track.code)) selectedCodes.add(track.code)
                                        } else {
                                            selectedCodes.remove(track.code)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCodes.toList())
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp),
    )
}

@Composable
private fun SubtitleCheckboxRow(
    track: SubtitleTrack,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle(!isChecked) }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onToggle,
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (track.isAutoGenerated) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 6.dp),
            ) {
                Text(
                    text = "AUTO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
