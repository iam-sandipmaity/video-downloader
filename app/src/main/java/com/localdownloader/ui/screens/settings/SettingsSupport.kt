package com.localdownloader.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.ThemeMode

data class SettingChoiceOption(
    val title: String,
    val subtitle: String? = null,
    val onSelect: () -> Unit,
)

data class SettingChoiceDialogState(
    val title: String,
    val selected: String,
    val options: List<SettingChoiceOption>,
)

data class SettingTextDialogState(
    val title: String,
    val value: String,
    val label: String,
    val supporting: String,
    val confirmLabel: String,
    val onConfirm: (String) -> Unit,
)

data class SettingConfirmDialogState(
    val title: String,
    val body: String,
    val confirmLabel: String,
    val destructive: Boolean = false,
    val onConfirm: () -> Unit,
)

data class FilenameTemplateDialogState(
    val title: String,
    val value: String,
    val supporting: String,
    val presets: List<FilenameTemplatePreset>,
    val tokens: List<String>,
    val onConfirm: (String) -> Unit,
)

data class FilenameTemplatePreset(
    val title: String,
    val template: String,
)

@Composable
fun SettingChoiceDialog(
    state: SettingChoiceDialogState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                state.options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                option.onSelect()
                                onDismiss()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (state.selected == option.title) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(18.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.selected == option.title) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                shape = CircleShape,
                                            ),
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            option.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
fun SettingTextDialog(
    state: SettingTextDialogState,
    onDismiss: () -> Unit,
) {
    var value by remember(state) { mutableStateOf(state.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(state.label) },
                supportingText = { Text(state.supporting) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    state.onConfirm(value.trim())
                    onDismiss()
                },
            ) {
                Text(state.confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilenameTemplateDialog(
    state: FilenameTemplateDialogState,
    onDismiss: () -> Unit,
) {
    var value by remember(state) { mutableStateOf(state.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.common_filename_template)) },
                    supportingText = { Text(state.supporting) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.common_quick_presets),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.presets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (value.trim() == preset.template) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { value = preset.template },
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = preset.template,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.common_suggested_fields),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.tokens.forEach { token ->
                        FilterChip(
                            selected = value.contains(token),
                            onClick = {
                                value = appendTemplateToken(
                                    template = value,
                                    token = token,
                                )
                            },
                            label = { Text(token) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.common_template_ext_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    state.onConfirm(value.trim())
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
fun SettingConfirmDialog(
    state: SettingConfirmDialogState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title) },
        text = { Text(state.body) },
        confirmButton = {
            Button(onClick = state.onConfirm) {
                Text(state.confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    if (state.destructive) {
                        stringResource(R.string.common_cancel)
                    } else {
                        stringResource(R.string.common_keep_current)
                    },
                )
            }
        },
    )
}

fun themeModeLabel(context: Context, mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> context.getString(R.string.theme_mode_system)
        ThemeMode.LIGHT -> context.getString(R.string.theme_mode_light)
        ThemeMode.DARK -> context.getString(R.string.theme_mode_dark)
    }
}

fun accentLabel(context: Context, accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> context.getString(R.string.accent_material_you)
        AccentPreset.OCEAN -> context.getString(R.string.accent_blue)
        AccentPreset.COBALT -> context.getString(R.string.accent_cobalt)
        AccentPreset.INDIGO -> context.getString(R.string.accent_indigo)
        AccentPreset.SKY -> context.getString(R.string.accent_sky)
        AccentPreset.AQUA -> context.getString(R.string.accent_aqua)
        AccentPreset.TEAL -> context.getString(R.string.accent_teal)
        AccentPreset.MINT -> context.getString(R.string.accent_mint)
        AccentPreset.EMERALD -> context.getString(R.string.accent_emerald)
        AccentPreset.FOREST -> context.getString(R.string.accent_green)
        AccentPreset.ROSE -> context.getString(R.string.accent_rose)
        AccentPreset.CRIMSON -> context.getString(R.string.accent_crimson)
        AccentPreset.MAGENTA -> context.getString(R.string.accent_magenta)
        AccentPreset.PURPLE -> context.getString(R.string.accent_purple)
        AccentPreset.YELLOW -> context.getString(R.string.accent_yellow)
        AccentPreset.LIME -> context.getString(R.string.accent_lime)
        AccentPreset.ORANGE -> context.getString(R.string.accent_orange)
        AccentPreset.PEACH -> context.getString(R.string.accent_peach)
        AccentPreset.COPPER -> context.getString(R.string.accent_copper)
        AccentPreset.MONOCHROME -> context.getString(R.string.accent_monochrome)
    }
}

fun accentSubtitle(context: Context, accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> context.getString(R.string.accent_subtitle_amber)
        AccentPreset.OCEAN -> context.getString(R.string.accent_subtitle_ocean)
        AccentPreset.COBALT -> context.getString(R.string.accent_subtitle_cobalt)
        AccentPreset.INDIGO -> context.getString(R.string.accent_subtitle_indigo)
        AccentPreset.SKY -> context.getString(R.string.accent_subtitle_sky)
        AccentPreset.AQUA -> context.getString(R.string.accent_subtitle_aqua)
        AccentPreset.TEAL -> context.getString(R.string.accent_subtitle_teal)
        AccentPreset.MINT -> context.getString(R.string.accent_subtitle_mint)
        AccentPreset.EMERALD -> context.getString(R.string.accent_subtitle_emerald)
        AccentPreset.FOREST -> context.getString(R.string.accent_subtitle_forest)
        AccentPreset.ROSE -> context.getString(R.string.accent_subtitle_rose)
        AccentPreset.CRIMSON -> context.getString(R.string.accent_subtitle_crimson)
        AccentPreset.MAGENTA -> context.getString(R.string.accent_subtitle_magenta)
        AccentPreset.PURPLE -> context.getString(R.string.accent_subtitle_purple)
        AccentPreset.YELLOW -> context.getString(R.string.accent_subtitle_yellow)
        AccentPreset.LIME -> context.getString(R.string.accent_subtitle_lime)
        AccentPreset.ORANGE -> context.getString(R.string.accent_subtitle_orange)
        AccentPreset.PEACH -> context.getString(R.string.accent_subtitle_peach)
        AccentPreset.COPPER -> context.getString(R.string.accent_subtitle_copper)
        AccentPreset.MONOCHROME -> context.getString(R.string.accent_subtitle_monochrome)
    }
}

fun contrastLabel(context: Context, mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.SOFT -> context.getString(R.string.contrast_soft)
        ContrastMode.STANDARD -> context.getString(R.string.contrast_standard)
        ContrastMode.HIGH -> context.getString(R.string.contrast_high)
        ContrastMode.ULTRA -> context.getString(R.string.contrast_ultra)
    }
}

fun contrastSubtitle(context: Context, mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.SOFT -> context.getString(R.string.contrast_subtitle_soft)
        ContrastMode.STANDARD -> context.getString(R.string.contrast_subtitle_standard)
        ContrastMode.HIGH -> context.getString(R.string.contrast_subtitle_high)
        ContrastMode.ULTRA -> context.getString(R.string.contrast_subtitle_ultra)
    }
}

fun containerDescription(context: Context, container: String): String {
    return when (container) {
        "mp4" -> context.getString(R.string.container_desc_mp4)
        "webm" -> context.getString(R.string.container_desc_webm)
        "mkv" -> context.getString(R.string.container_desc_mkv)
        "mov" -> context.getString(R.string.container_desc_mov)
        else -> context.getString(R.string.container_desc_default)
    }
}

fun audioFormatDescription(context: Context, format: String): String {
    return when (format) {
        "mp3" -> context.getString(R.string.audio_desc_mp3)
        "m4a" -> context.getString(R.string.audio_desc_m4a)
        "aac" -> context.getString(R.string.audio_desc_aac)
        "opus" -> context.getString(R.string.audio_desc_opus)
        "flac" -> context.getString(R.string.audio_desc_flac)
        "wav" -> context.getString(R.string.audio_desc_wav)
        else -> context.getString(R.string.audio_desc_default)
    }
}

fun videoFilenameTemplatePresets(context: Context): List<FilenameTemplatePreset> {
    return listOf(
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_title_and_id),
            template = "%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_uploader_and_title),
            template = "%(uploader)s - %(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_playlist_friendly),
            template = "%(playlist_index,playlist_autonumber&{}. |)s%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_date_first),
            template = "%(upload_date>%Y-%m-%d)s - %(title)s [%(id)s].%(ext)s",
        ),
    )
}

fun audioFilenameTemplatePresets(context: Context): List<FilenameTemplatePreset> {
    return listOf(
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_title_and_id),
            template = "%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_artist_and_title),
            template = "%(artist,uploader)s - %(title)s.%(ext)s",
        ),
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_album_track),
            template = "%(album,uploader)s/%(track_number,playlist_index&{}. )s%(title)s.%(ext)s",
        ),
        FilenameTemplatePreset(
            title = context.getString(R.string.preset_date_and_title),
            template = "%(release_date,upload_date>%Y-%m-%d)s - %(title)s.%(ext)s",
        ),
    )
}

fun suggestedFilenameTokens(): List<String> {
    return listOf(
        "%(title)s",
        "%(uploader)s",
        "%(artist)s",
        "%(album)s",
        "%(track)s",
        "%(playlist_index,playlist_autonumber&{}. |)s",
        "%(upload_date>%Y-%m-%d)s",
        "%(release_date>%Y-%m-%d)s",
        "%(duration_string)s",
        "%(id)s",
        "%(ext)s",
    )
}

fun appendTemplateToken(template: String, token: String): String {
    val trimmed = template.trimEnd()
    if (trimmed.isBlank()) return token
    val separator = when {
        trimmed.endsWith("/") -> ""
        trimmed.endsWith("\\") -> ""
        trimmed.endsWith("-") -> " "
        trimmed.endsWith("_") -> ""
        trimmed.endsWith("[") -> ""
        trimmed.endsWith("(") -> ""
        else -> " "
    }
    return trimmed + separator + token
}

fun String.folderPreview(defaultLabel: String): String {
    return trim().ifBlank { defaultLabel }
}

fun String.cleanPreview(): String {
    return trim().ifBlank { "Default" }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
