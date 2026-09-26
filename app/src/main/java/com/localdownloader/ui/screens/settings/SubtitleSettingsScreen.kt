package com.localdownloader.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VerticalAlignBottom
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
                checked = uiState.appSettings.autoDownloadSubtitles,
                onCheckedChange = onAutoDownloadSubtitlesChanged,
            )
        }
        item {
            PreferenceSwitch(
                icon = Icons.Rounded.ClosedCaption,
                title = "Auto-embed into video",
                description = "Mux downloaded subtitles into MP4/MKV container so video players can toggle them.",
                checked = uiState.appSettings.autoEmbedSubtitles,
                onCheckedChange = onAutoEmbedSubtitlesChanged,
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
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0D1117),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Simulated video background with gradient and playback icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617)),
                        ),
                    ),
            )

            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center),
            )

            // Header watermark
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
                Text(
                    text = "Player Subtitle Display Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                )
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
                        bottom = if (settings.bottomOffsetFraction > 0.5f) 0.dp else (settings.bottomOffsetFraction * 140).dp.coerceIn(8.dp, 36.dp),
                        top = if (settings.bottomOffsetFraction > 0.5f) 28.dp else 0.dp,
                    )
                    .clip(RoundedCornerShape(6.dp))
                    .background(subtitleBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
