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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                Text("Cancel")
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
                Text("Cancel")
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
                    label = { Text("Filename template") },
                    supportingText = { Text(state.supporting) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Quick presets",
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
                    text = "Suggested fields",
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
                    text = "Tip: keep %(ext)s somewhere in the template so the saved file keeps the correct extension.",
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
                Text("Use template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
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
                Text(if (state.destructive) "Cancel" else "Keep current")
            }
        },
    )
}

fun themeModeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
}

fun accentLabel(accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> "Material you"
        AccentPreset.OCEAN -> "Blue"
        AccentPreset.COBALT -> "Cobalt"
        AccentPreset.AQUA -> "Aqua"
        AccentPreset.TEAL -> "Teal"
        AccentPreset.MINT -> "Mint"
        AccentPreset.EMERALD -> "Emerald"
        AccentPreset.FOREST -> "Green"
        AccentPreset.ROSE -> "Rose"
        AccentPreset.CRIMSON -> "Crimson"
        AccentPreset.MAGENTA -> "Magenta"
        AccentPreset.PURPLE -> "Purple"
        AccentPreset.YELLOW -> "Yellow"
        AccentPreset.ORANGE -> "Orange"
        AccentPreset.COPPER -> "Copper"
        AccentPreset.MONOCHROME -> "Monochrome"
    }
}

fun accentSubtitle(accentPreset: AccentPreset): String {
    return when (accentPreset) {
        AccentPreset.AMBER -> "A warm default with the soft amber look already used by the app."
        AccentPreset.OCEAN -> "Cool blue highlights for a calmer downloader mood."
        AccentPreset.COBALT -> "A deeper electric blue with stronger player and action contrast."
        AccentPreset.AQUA -> "Bright aqua accents with a cleaner, glassier utility feel."
        AccentPreset.TEAL -> "Blue-green accents that feel crisp, modern, and a little lighter."
        AccentPreset.MINT -> "Fresh mint accents for a softer, cleaner utility look."
        AccentPreset.EMERALD -> "A richer jewel-green palette with stronger contrast than mint."
        AccentPreset.FOREST -> "A greener look with a softer natural feel."
        AccentPreset.ROSE -> "Warm rose accents for a brighter and friendlier red tone."
        AccentPreset.CRIMSON -> "A richer red tone with more drama than the standard rose theme."
        AccentPreset.MAGENTA -> "Bold magenta highlights for a more vivid music and creator vibe."
        AccentPreset.PURPLE -> "A richer violet palette for a more dramatic music vibe."
        AccentPreset.YELLOW -> "Bright yellow accents with higher energy."
        AccentPreset.ORANGE -> "Warm orange action tones similar to media apps."
        AccentPreset.COPPER -> "Copper-orange accents that feel warmer and more grounded than amber."
        AccentPreset.MONOCHROME -> "Muted grayscale accents for a cleaner neutral setup."
    }
}

fun contrastLabel(mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.SOFT -> "Soft"
        ContrastMode.STANDARD -> "Standard"
        ContrastMode.HIGH -> "High contrast"
        ContrastMode.ULTRA -> "Ultra contrast"
    }
}

fun contrastSubtitle(mode: ContrastMode): String {
    return when (mode) {
        ContrastMode.SOFT -> "Gentler surfaces and softer separation for a calmer look."
        ContrastMode.STANDARD -> "Balanced contrast for the normal theme surfaces."
        ContrastMode.HIGH -> "Sharper text and stronger separation between cards and background."
        ContrastMode.ULTRA -> "Maximum separation for the clearest edges and strongest readability."
    }
}

fun containerDescription(container: String): String {
    return when (container) {
        "mp4" -> "Best general compatibility across Android devices and players."
        "webm" -> "Smaller web-friendly container when the source supports it well."
        "mkv" -> "Flexible container for mixed codecs and more unusual source formats."
        "mov" -> "Apple-style container when you want a closer edit-friendly export."
        else -> "Use this container for future merged video downloads."
    }
}

fun audioFormatDescription(format: String): String {
    return when (format) {
        "mp3" -> "The broadest device and car-player compatibility."
        "m4a" -> "AAC audio in a compact container that works well on most phones."
        "aac" -> "Raw AAC output for lighter files when you need a simpler audio stream."
        "opus" -> "High efficiency audio that is great when the source already supports it."
        "flac" -> "Lossless output when you want to preserve as much audio quality as possible."
        "wav" -> "Large but simple audio files that work well in editors."
        else -> "Use this format for future audio extracts."
    }
}

fun videoFilenameTemplatePresets(): List<FilenameTemplatePreset> {
    return listOf(
        FilenameTemplatePreset(
            title = "Title and ID",
            template = "%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Uploader and title",
            template = "%(uploader)s - %(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Playlist-friendly",
            template = "%(playlist_index,playlist_autonumber&{}. |)s%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Date first",
            template = "%(upload_date>%Y-%m-%d)s - %(title)s [%(id)s].%(ext)s",
        ),
    )
}

fun audioFilenameTemplatePresets(): List<FilenameTemplatePreset> {
    return listOf(
        FilenameTemplatePreset(
            title = "Title and ID",
            template = "%(title)s [%(id)s].%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Artist and title",
            template = "%(artist,uploader)s - %(title)s.%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Album track",
            template = "%(album,uploader)s/%(track_number,playlist_index&{}. )s%(title)s.%(ext)s",
        ),
        FilenameTemplatePreset(
            title = "Date and title",
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
