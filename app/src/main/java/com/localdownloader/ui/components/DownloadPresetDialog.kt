package com.localdownloader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.domain.models.DownloadPreset
import com.localdownloader.domain.models.PresetTargetType
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EditDownloadPresetDialog(
    preset: DownloadPreset?,
    isDefaultPreset: Boolean,
    onSave: (DownloadPreset) -> Unit,
    onDelete: ((String) -> Unit)?,
    onDismiss: () -> Unit,
) {
    val isCreating = preset == null
    val presetId = remember { preset?.id ?: UUID.randomUUID().toString() }
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var description by remember { mutableStateOf(preset?.description ?: "") }
    var targetType by remember { mutableStateOf(preset?.targetType ?: PresetTargetType.VIDEO) }
    var preferredQuality by remember { mutableStateOf(preset?.preferredQuality ?: "best") }
    var videoContainer by remember { mutableStateOf(preset?.videoContainer ?: "auto") }
    var audioFormat by remember { mutableStateOf(preset?.audioFormat ?: "mp3") }
    var audioBitrateKbps by remember { mutableIntStateOf(preset?.audioBitrateKbps ?: 320) }
    var autoDownloadSubtitles by remember { mutableStateOf(preset?.autoDownloadSubtitles ?: false) }
    var embedSubtitles by remember { mutableStateOf(preset?.embedSubtitles ?: false) }
    var embedMetadata by remember { mutableStateOf(preset?.embedMetadata ?: true) }
    var embedThumbnail by remember { mutableStateOf(preset?.embedThumbnail ?: true) }
    var isDefault by remember { mutableStateOf(isDefaultPreset) }

    val videoQualities = listOf("best", "4320p", "2160p", "1800p", "1440p", "1080p", "720p", "480p", "360p")
    val videoContainers = listOf("auto", "mp4", "mkv", "webm")
    val audioFormats = listOf("mp3", "m4a", "opus", "flac", "wav")
    val audioBitrates = listOf(320, 256, 192, 128)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isCreating) "Create Download Preset" else "Edit Preset",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. 1080p High Quality") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("e.g. Full HD MP4 with embedded tags") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "TARGET TYPE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = targetType == PresetTargetType.VIDEO,
                        onClick = { targetType = PresetTargetType.VIDEO },
                        label = { Text("Video") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                    FilterChip(
                        selected = targetType == PresetTargetType.AUDIO_ONLY,
                        onClick = { targetType = PresetTargetType.AUDIO_ONLY },
                        label = { Text("Audio Only") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }

                HorizontalDivider()

                if (targetType == PresetTargetType.VIDEO) {
                    Text(
                        text = "PREFERRED QUALITY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        videoQualities.forEach { q ->
                            FilterChip(
                                selected = preferredQuality.equals(q, ignoreCase = true),
                                onClick = { preferredQuality = q },
                                label = { Text(if (q == "best") "Best" else q) },
                            )
                        }
                    }

                    Text(
                        text = "CONTAINER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        videoContainers.forEach { c ->
                            FilterChip(
                                selected = videoContainer.equals(c, ignoreCase = true),
                                onClick = { videoContainer = c },
                                label = { Text(c.uppercase()) },
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Subtitles", style = MaterialTheme.typography.bodyMedium)
                            Text("Auto-download available subtitle tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = autoDownloadSubtitles, onCheckedChange = { autoDownloadSubtitles = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Embed Subtitles", style = MaterialTheme.typography.bodyMedium)
                            Text("Mux subtitles directly into video file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = embedSubtitles, onCheckedChange = { embedSubtitles = it })
                    }
                } else {
                    Text(
                        text = "AUDIO FORMAT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        audioFormats.forEach { fmt ->
                            FilterChip(
                                selected = audioFormat.equals(fmt, ignoreCase = true),
                                onClick = { audioFormat = fmt },
                                label = { Text(fmt.uppercase()) },
                            )
                        }
                    }

                    Text(
                        text = "BITRATE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        audioBitrates.forEach { br ->
                            FilterChip(
                                selected = audioBitrateKbps == br,
                                onClick = { audioBitrateKbps = br },
                                label = { Text("${br}k") },
                            )
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Embed Metadata", style = MaterialTheme.typography.bodyMedium)
                        Text("Write title, artist, and album tags", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = embedMetadata, onCheckedChange = { embedMetadata = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Embed Thumbnail", style = MaterialTheme.typography.bodyMedium)
                        Text("Include cover artwork in the file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = embedThumbnail, onCheckedChange = { embedThumbnail = it })
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Set as Default", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Automatically preselect this preset for downloads", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPreset = DownloadPreset(
                        id = presetId,
                        name = name.trim().ifBlank { if (targetType == PresetTargetType.AUDIO_ONLY) "Custom Audio" else "Custom Video" },
                        description = description.trim(),
                        isDefault = isDefault,
                        isBuiltIn = preset?.isBuiltIn ?: false,
                        targetType = targetType,
                        preferredQuality = preferredQuality,
                        videoContainer = videoContainer,
                        audioFormat = audioFormat,
                        audioBitrateKbps = audioBitrateKbps,
                        autoDownloadSubtitles = autoDownloadSubtitles,
                        embedSubtitles = embedSubtitles,
                        embedMetadata = embedMetadata,
                        embedThumbnail = embedThumbnail,
                    )
                    onSave(finalPreset)
                    onDismiss()
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCreating && onDelete != null && preset?.isBuiltIn != true) {
                    IconButton(
                        onClick = {
                            onDelete(presetId)
                            onDismiss()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete preset",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        },
    )
}
