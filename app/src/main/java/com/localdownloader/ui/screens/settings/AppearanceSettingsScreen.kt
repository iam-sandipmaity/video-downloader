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
    val appearanceThemeTitle = stringResource(R.string.appearance_theme_title)
    val appearanceThemeSystemSubtitle = stringResource(R.string.appearance_theme_system_subtitle)
    val appearanceThemeDarkSubtitle = stringResource(R.string.appearance_theme_dark_subtitle)
    val appearanceThemeLightSubtitle = stringResource(R.string.appearance_theme_light_subtitle)
    val appearanceAccentTitle = stringResource(R.string.appearance_accent_title)
    val appearanceContrastTitle = stringResource(R.string.appearance_contrast_title)
    val appearanceLanguageTitle = stringResource(R.string.appearance_language_title)
    val appearanceLanguageSystemSubtitle = stringResource(R.string.appearance_language_system_subtitle)

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
                        choiceDialog = SettingChoiceDialogState(
                            title = appearanceThemeTitle,
                            selected = themeModeLabel(context, uiState.themeMode),
                            options = listOf(
                                ThemeMode.SYSTEM,
                                ThemeMode.DARK,
                                ThemeMode.LIGHT,
                            ).map { mode ->
                                SettingChoiceOption(
                                    title = themeModeLabel(context, mode),
                                    subtitle = when (mode) {
                                        ThemeMode.SYSTEM -> appearanceThemeSystemSubtitle
                                        ThemeMode.DARK -> appearanceThemeDarkSubtitle
                                        ThemeMode.LIGHT -> appearanceThemeLightSubtitle
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
                        val accentOrder = listOf(
                            AccentPreset.AMBER,
                            AccentPreset.OCEAN,
                            AccentPreset.COBALT,
                            AccentPreset.INDIGO,
                            AccentPreset.SKY,
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
                            AccentPreset.LIME,
                            AccentPreset.ORANGE,
                            AccentPreset.PEACH,
                            AccentPreset.COPPER,
                            AccentPreset.MONOCHROME,
                        )
                        choiceDialog = SettingChoiceDialogState(
                            title = appearanceAccentTitle,
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
                        choiceDialog = SettingChoiceDialogState(
                            title = appearanceContrastTitle,
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
                        choiceDialog = SettingChoiceDialogState(
                            title = appearanceLanguageTitle,
                            selected = appLanguageLabel(uiState.languageTag, systemDefaultLabel),
                            options = buildList {
                                add(
                                    SettingChoiceOption(
                                        title = systemDefaultLabel,
                                        subtitle = appearanceLanguageSystemSubtitle,
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
