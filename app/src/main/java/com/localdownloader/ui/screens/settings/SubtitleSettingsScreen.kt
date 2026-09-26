package com.localdownloader.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localdownloader.domain.models.SubtitleViewSettings
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun SubtitleSettingsScreen(
    uiState: FormatUiState,
    onAutoDownloadSubtitlesChanged: (Boolean) -> Unit,
    onAutoEmbedSubtitlesChanged: (Boolean) -> Unit,
    onSubtitleViewSettingsChanged: (SubtitleViewSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }
    val currentViewSettings = uiState.appSettings.subtitleViewSettings

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    PreferencePageScaffold(
        title = "Subtitles & Captions",
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(
                onClick = {
                    onSubtitleViewSettingsChanged(SubtitleViewSettings())
                },
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reset subtitle styles to default",
                )
            }
        },
    ) {
        // Live Subtitle Preview Card
        item {
            PreferenceSubtitle(text = "LIVE PLAYER PREVIEW")
        }
        item {
            SubtitleLivePreviewBox(settings = currentViewSettings)
        }

        // Section: Download & Extraction Defaults
        item {
            PreferenceSubtitle(text = "DOWNLOAD & EMBEDDING")
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.Subtitles,
                title = "Auto-download subtitles",
                description = "Automatically query and download subtitles during analysis.",
                isChecked = uiState.appSettings.autoDownloadSubtitles,
                onClick = { onAutoDownloadSubtitlesChanged(!uiState.appSettings.autoDownloadSubtitles) },
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.ClosedCaption,
                title = "Auto-embed into video",
                description = "Mux downloaded subtitles into MP4/MKV container so video players can toggle them.",
                isChecked = uiState.appSettings.autoEmbedSubtitles,
                onClick = { onAutoEmbedSubtitlesChanged(!uiState.appSettings.autoEmbedSubtitles) },
            )
        }

        // Section: Subtitle Colors & Styling
        item {
            PreferenceSubtitle(text = "COLORS & EDGES")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.FormatColorText,
                title = "Text color",
                description = currentViewSettings.textColor,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Subtitle Text Color",
                        selected = currentViewSettings.textColor,
                        options = listOf(
                            "White" to "Standard crisp white",
                            "Yellow" to "High-visibility amber yellow",
                            "Cyan" to "Sky cyan",
                            "Green" to "Emerald green",
                            "Magenta" to "Vivid magenta",
                            "Black" to "Deep black",
                        ).map { (color, desc) ->
                            SettingChoiceOption(
                                title = color,
                                subtitle = desc,
                                onSelect = {
                                    onSubtitleViewSettingsChanged(currentViewSettings.copy(textColor = color))
                                },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Palette,
                title = "Background box",
                description = when (currentViewSettings.backgroundColor) {
                    "SemiTransparentBlack" -> "50% Translucent Black (Recommended)"
                    "SolidBlack" -> "Solid Black"
                    "None" -> "Transparent"
                    "TranslucentYellow" -> "Amber Tint"
                    else -> currentViewSettings.backgroundColor
                },
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Subtitle Background",
                        selected = currentViewSettings.backgroundColor,
                        options = listOf(
                            "SemiTransparentBlack" to "50% Translucent Black (Balanced)",
                            "SolidBlack" to "Solid Black (High contrast)",
                            "None" to "Transparent (No background box)",
                            "TranslucentYellow" to "Amber Tint",
                        ).map { (bg, label) ->
                            SettingChoiceOption(
                                title = bg,
                                subtitle = label,
                                onSelect = {
                                    onSubtitleViewSettingsChanged(currentViewSettings.copy(backgroundColor = bg))
                                },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.Layers,
                title = "Edge styling",
                description = currentViewSettings.edgeType,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Subtitle Edge Type",
                        selected = currentViewSettings.edgeType,
                        options = listOf(
                            "Outline" to "Sharp bordered text outline",
                            "DropShadow" to "Subtle soft drop shadow",
                            "None" to "No edge styling",
                        ).map { (edge, desc) ->
                            SettingChoiceOption(
                                title = edge,
                                subtitle = desc,
                                onSelect = {
                                    onSubtitleViewSettingsChanged(currentViewSettings.copy(edgeType = edge))
                                },
                            )
                        },
                    )
                },
            )
        }

        // Section: Typography & Position
        item {
            PreferenceSubtitle(text = "TYPOGRAPHY & POSITION")
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.FormatSize,
                title = "Font size scale",
                description = formatSizeScaleLabel(currentViewSettings.fontSizeScale),
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Subtitle Font Size",
                        selected = "${(currentViewSettings.fontSizeScale * 100).toInt()}%",
                        options = listOf(
                            0.75f to "75% (Small / Compact)",
                            1.0f to "100% (Normal / Standard)",
                            1.25f to "125% (Medium)",
                            1.5f to "150% (Large)",
                            2.0f to "200% (Extra Large / TV)",
                        ).map { (scale, label) ->
                            SettingChoiceOption(
                                title = "${(scale * 100).toInt()}%",
                                subtitle = label,
                                onSelect = {
                                    onSubtitleViewSettingsChanged(currentViewSettings.copy(fontSizeScale = scale))
                                },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.TextFields,
                title = "Font typeface",
                description = currentViewSettings.typeface,
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Subtitle Typeface",
                        selected = currentViewSettings.typeface,
                        options = listOf(
                            "Default" to "System default font",
                            "SansSerif" to "Clean modern sans-serif",
                            "Serif" to "Classic editorial serif",
                            "Monospace" to "Monospaced terminal typeface",
                        ).map { (font, desc) ->
                            SettingChoiceOption(
                                title = font,
                                subtitle = desc,
                                onSelect = {
                                    onSubtitleViewSettingsChanged(currentViewSettings.copy(typeface = font))
                                },
                            )
                        },
                    )
                },
            )
        }
        item {
            PreferenceItem(
                icon = Icons.Rounded.VerticalAlignBottom,
                title = "Screen position offset",
                description = formatPositionOffsetLabel(currentViewSettings.bottomOffsetFraction),
                onClick = {
                    choiceDialog = SettingChoiceDialogState(
                        title = "Vertical Position Offset",
                        selected = "${(currentViewSettings.bottomOffsetFraction * 100).toInt()}%",
                        options = listOf(
                            0.04f to "4% (Low / Bottom edge)",
                            0.08f to "8% (Standard default)",
                            0.12f to "12% (Higher above controls)",
                            0.18f to "18% (Substantial bottom margin)",
                            0.80f to "80% (Top of screen)",
                        ).map { (offset, label) ->
                            SettingChoiceOption(
                                title = "${(offset * 100).toInt()}%",
                                subtitle = label,
                                onSelect = {
                                    onSubtitleViewSettingsChanged(currentViewSettings.copy(bottomOffsetFraction = offset))
                                },
                            )
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SubtitleLivePreviewBox(
    settings: SubtitleViewSettings,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF090D16),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(195.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated cinematic realistic video frame background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B132B),
                                Color(0xFF1C2541),
                                Color(0xFF243B55),
                                Color(0xFF141E30),
                                Color(0xFF050811),
                            ),
                        ),
                    ),
            )

            // Ambient lighting horizon effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.Center)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x334F46E5),
                                Color(0x1A38BDF8),
                                Color.Transparent,
                            ),
                            radius = 400f,
                        ),
                    ),
            )

            // Center subtle playback watermark
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.10f),
                modifier = Modifier
                    .size(76.dp)
                    .align(Alignment.Center),
            )

            // Top Video Player HUD (Title, Resolution, Audio track)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xCC000000), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E)),
                    )
                    Text(
                        text = "1080p 60fps • Video Preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "CC LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            // Subtitle text rendered with the chosen settings!
            val subtitleColor = parseTextColor(settings.textColor)
            val subtitleBg = parseBackgroundColor(settings.backgroundColor)
            val subtitleFont = parseTypeface(settings.typeface)
            val baseFontSize = 14.sp * settings.fontSizeScale
            val textShadow = when (settings.edgeType) {
                "DropShadow" -> Shadow(
                    color = Color.Black.copy(alpha = 0.85f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f,
                )
                "Outline" -> Shadow(
                    color = Color.Black,
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 1f,
                )
                else -> null
            }

            Box(
                modifier = Modifier
                    .align(if (settings.bottomOffsetFraction > 0.5f) Alignment.TopCenter else Alignment.BottomCenter)
                    .padding(
                        bottom = if (settings.bottomOffsetFraction > 0.5f) 0.dp else ((settings.bottomOffsetFraction * 130) + 38).dp.coerceIn(38.dp, 80.dp),
                        top = if (settings.bottomOffsetFraction > 0.5f) 38.dp else 0.dp,
                        start = 16.dp,
                        end = 16.dp,
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(subtitleBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "The quick brown fox jumps over the lazy dog.",
                    color = subtitleColor,
                    fontSize = baseFontSize,
                    fontFamily = subtitleFont,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(shadow = textShadow),
                )
            }

            // Bottom Realistic Player Controls Bar (Timecode, Scrubber & Controls)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xEE000000)),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Mini Scrubber Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.38f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "01:24 / 03:45",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.75f),
                            fontFamily = FontFamily.Monospace,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(15.dp),
                        )
                        Icon(
                            imageVector = Icons.Outlined.Hd,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun parseTextColor(name: String): Color = when (name.lowercase()) {
    "yellow" -> Color(0xFFFFEB3B)
    "cyan" -> Color(0xFF00E5FF)
    "green" -> Color(0xFF00E676)
    "magenta" -> Color(0xFFFF4081)
    "black" -> Color(0xFF1E293B)
    else -> Color.White
}

private fun parseBackgroundColor(name: String): Color = when (name.lowercase()) {
    "semitransparentblack" -> Color(0x99000000)
    "solidblack" -> Color(0xFF000000)
    "translucentyellow" -> Color(0x66FFC107)
    "none" -> Color.Transparent
    else -> Color(0x99000000)
}

private fun parseTypeface(name: String): FontFamily = when (name.lowercase()) {
    "sansserif" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    else -> FontFamily.Default
}

private fun formatSizeScaleLabel(scale: Float): String = when {
    scale <= 0.75f -> "Small (75%)"
    scale <= 1.0f -> "Normal (100%)"
    scale <= 1.25f -> "Medium (125%)"
    scale <= 1.5f -> "Large (150%)"
    else -> "Huge (${(scale * 100).toInt()}%)"
}

private fun formatPositionOffsetLabel(fraction: Float): String = when {
    fraction <= 0.04f -> "Low (4% from bottom)"
    fraction <= 0.08f -> "Standard (8% from bottom)"
    fraction <= 0.12f -> "High (12% from bottom)"
    fraction <= 0.20f -> "Elevated (18% from bottom)"
    else -> "Top (${(fraction * 100).toInt()}%)"
}
