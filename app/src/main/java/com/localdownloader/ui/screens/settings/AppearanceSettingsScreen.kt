package com.localdownloader.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.SYSTEM_LANGUAGE_TAG
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
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
    val context = LocalContext.current
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
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Language,
                    title = "App language",
                    subtitle = "Follow Android's app language system, or pin a specific language here. English is still the most complete UI today.",
                    value = appLanguageLabel(uiState.languageTag),
                    onClick = {
                        val languageOptions = supportedAppLanguageOptions()
                        choiceDialog = SettingChoiceDialogState(
                            title = "App language",
                            selected = appLanguageLabel(uiState.languageTag),
                            options = buildList {
                                add(
                                    SettingChoiceOption(
                                        title = "System default",
                                        subtitle = "Follow the language Android already uses for this app.",
                                        onSelect = { onLanguageChanged(SYSTEM_LANGUAGE_TAG) },
                                    ),
                                )
                                addAll(
                                    languageOptions.map { option ->
                                        SettingChoiceOption(
                                            title = option.title,
                                            subtitle = option.subtitle,
                                            onSelect = { onLanguageChanged(option.tag) },
                                        )
                                    },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Settings,
                    title = "Android language settings",
                    subtitle = "Open the system app-language picker for the full Android language flow.",
                    onClick = {
                        val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        }
                        context.startActivity(settingsIntent)
                    },
                )
            }
        }
    }
}
