package com.localdownloader.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdownloader.domain.models.SubtitleViewSettings

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerSubtitleStyleDialog(
    currentSettings: SubtitleViewSettings,
    syncOffsetMs: Long,
    onSaveSettings: (SubtitleViewSettings) -> Unit,
    onAdjustSyncOffset: (Long) -> Unit,
    onResetSyncOffset: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    var settings by remember(currentSettings) { mutableStateOf(currentSettings) }
    var localSyncOffset by remember(syncOffsetMs) { mutableLongStateOf(syncOffsetMs) }

    val textColorOptions = listOf("White", "Yellow", "Cyan", "Green", "Magenta", "Black")
    val bgOptions = listOf(
        "None" to "Transparent",
        "SemiTransparentBlack" to "50% Black",
        "SolidBlack" to "Solid Black",
        "TranslucentYellow" to "Amber",
    )
    val edgeOptions = listOf(
        "Outline" to "Outline",
        "DropShadow" to "Shadow",
        "None" to "None",
    )
    val sizeOptions = listOf(
        0.75f to "Small",
        1.0f to "Normal",
        1.25f to "Medium",
        1.5f to "Large",
        2.0f to "Huge",
    )
    val fontOptions = listOf(
        "Default" to "Default",
        "SansSerif" to "Sans-Serif",
        "Serif" to "Serif",
        "Monospace" to "Monospace",
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Subtitles,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = "Subtitle Appearance & Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Live Subtitle Preview Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161A22),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val parsedTextColor = when (settings.textColor.lowercase()) {
                            "yellow" -> Color(0xFFFFEB3B)
                            "cyan" -> Color(0xFF00E5FF)
                            "green" -> Color(0xFF76FF03)
                            "magenta" -> Color(0xFFFF4081)
                            "black" -> Color(0xFF000000)
                            else -> Color(0xFFFFFFFF)
                        }

                        val parsedBgColor = when (settings.backgroundColor.lowercase()) {
                            "semitransparentblack" -> Color(0x88000000)
                            "solidblack" -> Color(0xFF000000)
                            "translucentyellow" -> Color(0x66FFC107)
                            else -> Color.Transparent
                        }

                        val parsedFontFamily = when (settings.typeface.lowercase()) {
                            "serif" -> FontFamily.Serif
                            "monospace" -> FontFamily.Monospace
                            "sansserif" -> FontFamily.SansSerif
                            else -> FontFamily.Default
                        }

                        val shadowEffect = when (settings.edgeType.lowercase()) {
                            "dropshadow" -> Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 3f)
                            "outline" -> Shadow(color = Color.Black, offset = Offset(0f, 0f), blurRadius = 4f)
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(parsedBgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "Sample Subtitle Text Preview",
                                style = TextStyle(
                                    color = parsedTextColor,
                                    fontSize = (15 * settings.fontSizeScale).sp,
                                    fontFamily = parsedFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    shadow = shadowEffect,
                                ),
                            )
                        }
                    }
                }

                // Subtitle Timing / Sync Controller
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Subtitle Sync Offset",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = if (localSyncOffset == 0L) "Synced (0.0s)" else "${if (localSyncOffset > 0) "+" else ""}${String.format("%.1f", localSyncOffset / 1000f)}s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (localSyncOffset == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = {
                                localSyncOffset -= 500L
                                onAdjustSyncOffset(-500L)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("-0.5s", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                localSyncOffset -= 100L
                                onAdjustSyncOffset(-100L)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("-0.1s", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                localSyncOffset += 100L
                                onAdjustSyncOffset(100L)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("+0.1s", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                localSyncOffset += 500L
                                onAdjustSyncOffset(500L)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("+0.5s", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (localSyncOffset != 0L) {
                        TextButton(
                            onClick = {
                                localSyncOffset = 0L
                                onResetSyncOffset()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Sync to 0.0s", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Text Size
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        sizeOptions.forEach { (scale, label) ->
                            FilterChip(
                                selected = settings.fontSizeScale == scale,
                                onClick = { settings = settings.copy(fontSizeScale = scale) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }

                // Text Color
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Text Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        textColorOptions.forEach { colorName ->
                            FilterChip(
                                selected = settings.textColor.equals(colorName, ignoreCase = true),
                                onClick = { settings = settings.copy(textColor = colorName) },
                                label = { Text(colorName, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }

                // Background
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Background Box",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        bgOptions.forEach { (bgKey, bgLabel) ->
                            FilterChip(
                                selected = settings.backgroundColor.equals(bgKey, ignoreCase = true),
                                onClick = { settings = settings.copy(backgroundColor = bgKey) },
                                label = { Text(bgLabel, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }

                // Text Edge / Outline
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Outline & Shadow",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        edgeOptions.forEach { (edgeKey, edgeLabel) ->
                            FilterChip(
                                selected = settings.edgeType.equals(edgeKey, ignoreCase = true),
                                onClick = { settings = settings.copy(edgeType = edgeKey) },
                                label = { Text(edgeLabel, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }

                // Font Family
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Font Family",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        fontOptions.forEach { (fontKey, fontLabel) ->
                            FilterChip(
                                selected = settings.typeface.equals(fontKey, ignoreCase = true),
                                onClick = { settings = settings.copy(typeface = fontKey) },
                                label = { Text(fontLabel, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSettings(settings)
                    onDismissRequest()
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    settings = SubtitleViewSettings()
                    onSaveSettings(SubtitleViewSettings())
                },
            ) {
                Text("Reset Defaults")
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}
