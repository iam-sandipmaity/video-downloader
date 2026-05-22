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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.localdownloader.R
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
    val systemDefaultLabel = stringResource(R.string.common_system_default)
    val interfaceLanguageLabel = stringResource(R.string.common_interface_language)

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.appearance_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceGroup {
                PreferenceRow(
                    icon = Icons.Rounded.Style,
                    title = stringResource(R.string.appearance_theme_title),
                    subtitle = stringResource(R.string.appearance_theme_subtitle),
                    value = themeModeLabel(context, uiState.themeMode),
                    onClick = {
                        val themeTitle = context.getString(R.string.appearance_theme_title)
                        choiceDialog = SettingChoiceDialogState(
                            title = themeTitle,
                            selected = themeModeLabel(context, uiState.themeMode),
                            options = listOf(
                                ThemeMode.SYSTEM,
                                ThemeMode.DARK,
                                ThemeMode.LIGHT,
                            ).map { mode ->
                                SettingChoiceOption(
                                    title = themeModeLabel(context, mode),
                                    subtitle = when (mode) {
                                        ThemeMode.SYSTEM -> context.getString(R.string.appearance_theme_system_subtitle)
                                        ThemeMode.DARK -> context.getString(R.string.appearance_theme_dark_subtitle)
                                        ThemeMode.LIGHT -> context.getString(R.string.appearance_theme_light_subtitle)
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
                    title = stringResource(R.string.appearance_accent_title),
                    subtitle = stringResource(R.string.appearance_accent_subtitle),
                    value = accentLabel(context, uiState.accentPreset),
                    onClick = {
                        val accentTitle = context.getString(R.string.appearance_accent_title)
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
                            title = accentTitle,
                            selected = accentLabel(context, uiState.accentPreset),
                            options = accentOrder.map { preset ->
                                SettingChoiceOption(
                                    title = accentLabel(context, preset),
                                    subtitle = accentSubtitle(context, preset),
                                    onSelect = { onAccentPresetChanged(preset) },
                                )
                            },
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.appearance_contrast_title),
                    subtitle = stringResource(R.string.appearance_contrast_subtitle),
                    value = contrastLabel(context, uiState.contrastMode),
                    onClick = {
                        val contrastTitle = context.getString(R.string.appearance_contrast_title)
                        choiceDialog = SettingChoiceDialogState(
                            title = contrastTitle,
                            selected = contrastLabel(context, uiState.contrastMode),
                            options = ContrastMode.entries.map { mode ->
                                SettingChoiceOption(
                                    title = contrastLabel(context, mode),
                                    subtitle = contrastSubtitle(context, mode),
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
                    title = stringResource(R.string.appearance_language_title),
                    subtitle = stringResource(R.string.appearance_language_subtitle),
                    value = appLanguageLabel(uiState.languageTag, systemDefaultLabel),
                    onClick = {
                        val languageOptions = supportedAppLanguageOptions(interfaceLanguageLabel)
                        val languageTitle = context.getString(R.string.appearance_language_title)
                        choiceDialog = SettingChoiceDialogState(
                            title = languageTitle,
                            selected = appLanguageLabel(uiState.languageTag, systemDefaultLabel),
                            options = buildList {
                                add(
                                    SettingChoiceOption(
                                        title = systemDefaultLabel,
                                        subtitle = context.getString(R.string.appearance_language_system_subtitle),
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
            }
        }
    }
}
