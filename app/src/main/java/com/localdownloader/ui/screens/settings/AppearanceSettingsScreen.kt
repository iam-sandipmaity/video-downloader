package com.localdownloader.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferenceHeroCard
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSectionHeader
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun AppearanceSettingsScreen(
    uiState: FormatUiState,
    onLanguageChanged: (String) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onAccentPresetChanged: (AccentPreset) -> Unit,
    onContrastModeChanged: (ContrastMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    PreferencePageScaffold(
        title = "Appearance",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceHeroCard(
                eyebrow = "Look and feel",
                title = "Keep the app light, smooth, and easy to scan",
                subtitle = "This page controls the same visual language you feel across Home, Settings, the queue, and playback surfaces.",
                badges = listOf(
                    themeModeLabel(uiState.themeMode),
                    accentLabel(uiState.accentPreset),
                    contrastLabel(uiState.contrastMode),
                ),
            )
        }
        item {
            PreferenceSectionHeader(
                title = "Color system",
                subtitle = "A Seal-style flow: fewer giant walls of controls, more focused choices with instant feedback.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Style,
                    title = "Theme mode",
                    subtitle = "Choose whether the app follows the device, stays light, or stays dark.",
                    value = themeModeLabel(uiState.themeMode),
                    onClick = {
                        choiceDialog = SettingChoiceDialogState(
                            title = "Theme mode",
                            selected = themeModeLabel(uiState.themeMode),
                            options = listOf(
                                ThemeMode.SYSTEM,
                                ThemeMode.DARK,
                                ThemeMode.LIGHT,
                            ).map { mode ->
                                SettingChoiceOption(
                                    title = themeModeLabel(mode),
                                    subtitle = when (mode) {
                                        ThemeMode.SYSTEM -> "Follow the phone mode automatically."
                                        ThemeMode.DARK -> "Always use the darker app surface."
                                        ThemeMode.LIGHT -> "Always use the lighter app surface."
                                    },
                                    onSelect = { onThemeModeChanged(mode) },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Palette,
                    title = "Accent palette",
                    subtitle = "Shape the color language for highlights, buttons, playback details, and utility surfaces.",
                    value = accentLabel(uiState.accentPreset),
                    onClick = {
                        val accentOrder = listOf(
                            AccentPreset.AMBER,
                            AccentPreset.OCEAN,
                            AccentPreset.COBALT,
                            AccentPreset.AQUA,
                            AccentPreset.TEAL,
                            AccentPreset.MINT,
                            AccentPreset.EMERALD,
                            AccentPreset.FOREST,
                            AccentPreset.ROSE,
                            AccentPreset.CRIMSON,
                            AccentPreset.MAGENTA,
                            AccentPreset.PURPLE,
                            AccentPreset.YELLOW,
                            AccentPreset.ORANGE,
                            AccentPreset.COPPER,
                            AccentPreset.MONOCHROME,
                        )
                        choiceDialog = SettingChoiceDialogState(
                            title = "Accent palette",
                            selected = accentLabel(uiState.accentPreset),
                            options = accentOrder.map { preset ->
                                SettingChoiceOption(
                                    title = accentLabel(preset),
                                    subtitle = accentSubtitle(preset),
                                    onSelect = { onAccentPresetChanged(preset) },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Tune,
                    title = "Contrast",
                    subtitle = "Tune how gently or sharply cards, text, and backgrounds separate from each other.",
                    value = contrastLabel(uiState.contrastMode),
                    onClick = {
                        choiceDialog = SettingChoiceDialogState(
                            title = "Contrast",
                            selected = contrastLabel(uiState.contrastMode),
                            options = ContrastMode.entries.map { mode ->
                                SettingChoiceOption(
                                    title = contrastLabel(mode),
                                    subtitle = contrastSubtitle(mode),
                                    onSelect = { onContrastModeChanged(mode) },
                                )
                            },
                        )
                    },
                )
            }
        }
        item {
            PreferenceSectionHeader(
                title = "Language",
                subtitle = "The app is still English-only right now, but this stays structured for future localization work.",
            )
        }
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Language,
                    title = "Language",
                    subtitle = "Only English is available in the current build.",
                    value = "English",
                    onClick = {
                        choiceDialog = SettingChoiceDialogState(
                            title = "Language",
                            selected = "English",
                            options = listOf(
                                SettingChoiceOption(
                                    title = "English",
                                    subtitle = "Keep the current interface language.",
                                    onSelect = { onLanguageChanged("en") },
                                ),
                            ),
                        )
                    },
                )
            }
        }
    }
}
