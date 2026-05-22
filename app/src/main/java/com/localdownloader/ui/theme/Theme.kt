package com.localdownloader.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.ThemeMode

@Composable
fun LocalDownloaderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPreset: AccentPreset = AccentPreset.AMBER,
    contrastMode: ContrastMode = ContrastMode.STANDARD,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) {
        buildDarkColors(accentPreset = accentPreset, contrastMode = contrastMode)
    } else {
        buildLightColors(accentPreset = accentPreset, contrastMode = contrastMode)
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private fun buildLightColors(
    accentPreset: AccentPreset,
    contrastMode: ContrastMode,
) = lightColorScheme(
    primary = accentPreset.primary(),
    onPrimary = MoonText,
    primaryContainer = accentPreset.primaryContainerLight(),
    onPrimaryContainer = DawnText,
    secondary = accentPreset.secondaryLight(),
    onSecondary = MoonText,
    secondaryContainer = accentPreset.secondaryContainerLight(),
    onSecondaryContainer = DawnText,
    tertiary = accentPreset.tertiary(),
    onTertiary = Color(0xFF0F1E00),
    tertiaryContainer = Color(0xFFD3F7B5),
    onTertiaryContainer = Color(0xFF182900),
    background = lightBackground(contrastMode),
    onBackground = DawnText,
    surface = lightSurface(contrastMode),
    onSurface = DawnText,
    surfaceVariant = lightSurfaceVariant(contrastMode),
    onSurfaceVariant = lightOnSurfaceVariant(contrastMode),
    outline = lightOutline(contrastMode),
    error = CoralError,
)

private fun buildDarkColors(
    accentPreset: AccentPreset,
    contrastMode: ContrastMode,
) = darkColorScheme(
    primary = accentPreset.primary(),
    onPrimary = MoonText,
    primaryContainer = accentPreset.primaryContainerDark(),
    onPrimaryContainer = Color(0xFFF7DCC8),
    secondary = darkSecondary(contrastMode),
    onSecondary = Color(0xFF2A2233),
    secondaryContainer = accentPreset.secondaryContainerDark(),
    onSecondaryContainer = MoonText,
    tertiary = accentPreset.tertiary(),
    onTertiary = Color(0xFF102100),
    tertiaryContainer = Color(0xFF284900),
    onTertiaryContainer = Color(0xFFD0F8B5),
    background = darkBackground(contrastMode),
    onBackground = MoonText,
    surface = darkSurface(contrastMode),
    onSurface = MoonText,
    surfaceVariant = darkSurfaceVariant(contrastMode),
    onSurfaceVariant = darkOnSurfaceVariant(contrastMode),
    outline = darkOutline(contrastMode),
    error = Color(0xFFFFB4AB),
)

private fun AccentPreset.primary(): Color = when (this) {
    AccentPreset.AMBER -> Ember
    AccentPreset.OCEAN -> Ocean
    AccentPreset.COBALT -> Cobalt
    AccentPreset.INDIGO -> Indigo
    AccentPreset.SKY -> Sky
    AccentPreset.AQUA -> Aqua
    AccentPreset.TEAL -> Teal
    AccentPreset.MINT -> Mint
    AccentPreset.EMERALD -> Emerald
    AccentPreset.FOREST -> Forest
    AccentPreset.ROSE -> Rose
    AccentPreset.CRIMSON -> Crimson
    AccentPreset.MAGENTA -> Magenta
    AccentPreset.PURPLE -> Violet
    AccentPreset.YELLOW -> Gold
    AccentPreset.LIME -> Lime
    AccentPreset.ORANGE -> Tangerine
    AccentPreset.PEACH -> Peach
    AccentPreset.COPPER -> Copper
    AccentPreset.MONOCHROME -> Graphite
}

private fun AccentPreset.primaryContainerLight(): Color = when (this) {
    AccentPreset.AMBER -> Color(0xFFF0D4C1)
    AccentPreset.OCEAN -> Color(0xFFD0E8FF)
    AccentPreset.COBALT -> Color(0xFFD9E2FF)
    AccentPreset.INDIGO -> Color(0xFFE0E2FF)
    AccentPreset.SKY -> Color(0xFFD8ECFF)
    AccentPreset.AQUA -> Color(0xFFCEEFFE)
    AccentPreset.TEAL -> Color(0xFFCDEEEB)
    AccentPreset.MINT -> Color(0xFFD8F2E2)
    AccentPreset.EMERALD -> Color(0xFFD7F2E3)
    AccentPreset.FOREST -> Color(0xFFD3F0DE)
    AccentPreset.ROSE -> Color(0xFFF5D3DC)
    AccentPreset.CRIMSON -> Color(0xFFF7D5D8)
    AccentPreset.MAGENTA -> Color(0xFFF3D8FB)
    AccentPreset.PURPLE -> Color(0xFFE7DDFF)
    AccentPreset.YELLOW -> Color(0xFFF8E7AE)
    AccentPreset.LIME -> Color(0xFFE6F1B8)
    AccentPreset.ORANGE -> Color(0xFFFFDEBF)
    AccentPreset.PEACH -> Color(0xFFFFE0D5)
    AccentPreset.COPPER -> Color(0xFFF5DDCC)
    AccentPreset.MONOCHROME -> Color(0xFFE1E3E8)
}

private fun AccentPreset.primaryContainerDark(): Color = when (this) {
    AccentPreset.AMBER -> EmberContainer
    AccentPreset.OCEAN -> Color(0xFF0C3A63)
    AccentPreset.COBALT -> Color(0xFF182D6B)
    AccentPreset.INDIGO -> Color(0xFF28317A)
    AccentPreset.SKY -> Color(0xFF12466F)
    AccentPreset.AQUA -> Color(0xFF0B4B5E)
    AccentPreset.TEAL -> Color(0xFF0F4541)
    AccentPreset.MINT -> Color(0xFF134E35)
    AccentPreset.EMERALD -> Color(0xFF0E4A32)
    AccentPreset.FOREST -> Color(0xFF143D2A)
    AccentPreset.ROSE -> Color(0xFF5A2433)
    AccentPreset.CRIMSON -> Color(0xFF63232D)
    AccentPreset.MAGENTA -> Color(0xFF5B286E)
    AccentPreset.PURPLE -> Color(0xFF37256C)
    AccentPreset.YELLOW -> Color(0xFF4D3900)
    AccentPreset.LIME -> Color(0xFF344E00)
    AccentPreset.ORANGE -> Color(0xFF5C2F00)
    AccentPreset.PEACH -> Color(0xFF6B2E18)
    AccentPreset.COPPER -> Color(0xFF5A3416)
    AccentPreset.MONOCHROME -> Color(0xFF3A3D44)
}

private fun AccentPreset.secondaryLight(): Color = when (this) {
    AccentPreset.AMBER -> DawnSecondary
    AccentPreset.OCEAN -> Color(0xFF546E90)
    AccentPreset.COBALT -> Color(0xFF596DA3)
    AccentPreset.INDIGO -> Color(0xFF646E99)
    AccentPreset.SKY -> Color(0xFF58708D)
    AccentPreset.AQUA -> Color(0xFF4E7580)
    AccentPreset.TEAL -> Color(0xFF4E7672)
    AccentPreset.MINT -> Color(0xFF587462)
    AccentPreset.EMERALD -> Color(0xFF557264)
    AccentPreset.FOREST -> Color(0xFF5B7165)
    AccentPreset.ROSE -> Color(0xFF7B5A69)
    AccentPreset.CRIMSON -> Color(0xFF855A63)
    AccentPreset.MAGENTA -> Color(0xFF7D5A86)
    AccentPreset.PURPLE -> Color(0xFF6A5C8D)
    AccentPreset.YELLOW -> Color(0xFF76633D)
    AccentPreset.LIME -> Color(0xFF68724A)
    AccentPreset.ORANGE -> Color(0xFF7B604A)
    AccentPreset.PEACH -> Color(0xFF8A6557)
    AccentPreset.COPPER -> Color(0xFF7B6152)
    AccentPreset.MONOCHROME -> Color(0xFF60646C)
}

private fun AccentPreset.secondaryContainerLight(): Color = when (this) {
    AccentPreset.AMBER -> Color(0xFFE4DAF0)
    AccentPreset.OCEAN -> Color(0xFFE0E8F6)
    AccentPreset.COBALT -> Color(0xFFE2E7F8)
    AccentPreset.INDIGO -> Color(0xFFE4E6F6)
    AccentPreset.SKY -> Color(0xFFE0E9F5)
    AccentPreset.AQUA -> Color(0xFFDDECF3)
    AccentPreset.TEAL -> Color(0xFFDDECE9)
    AccentPreset.MINT -> Color(0xFFE0EEDF)
    AccentPreset.EMERALD -> Color(0xFFE0EEE5)
    AccentPreset.FOREST -> Color(0xFFE0EBDD)
    AccentPreset.ROSE -> Color(0xFFF0DDE7)
    AccentPreset.CRIMSON -> Color(0xFFF2E0E2)
    AccentPreset.MAGENTA -> Color(0xFFF1E0F5)
    AccentPreset.PURPLE -> Color(0xFFE9E1F7)
    AccentPreset.YELLOW -> Color(0xFFF1E7CE)
    AccentPreset.LIME -> Color(0xFFE8ECD7)
    AccentPreset.ORANGE -> Color(0xFFF3E1D2)
    AccentPreset.PEACH -> Color(0xFFF6E2DB)
    AccentPreset.COPPER -> Color(0xFFF2E3DA)
    AccentPreset.MONOCHROME -> Color(0xFFE7E8EC)
}

private fun AccentPreset.secondaryContainerDark(): Color = when (this) {
    AccentPreset.AMBER -> Color(0xFF4D4558)
    AccentPreset.OCEAN -> Color(0xFF34485E)
    AccentPreset.COBALT -> Color(0xFF394766)
    AccentPreset.INDIGO -> Color(0xFF464D69)
    AccentPreset.SKY -> Color(0xFF394C61)
    AccentPreset.AQUA -> Color(0xFF34545E)
    AccentPreset.TEAL -> Color(0xFF345550)
    AccentPreset.MINT -> Color(0xFF375446)
    AccentPreset.EMERALD -> Color(0xFF375246)
    AccentPreset.FOREST -> Color(0xFF3A4E43)
    AccentPreset.ROSE -> Color(0xFF563C47)
    AccentPreset.CRIMSON -> Color(0xFF603B43)
    AccentPreset.MAGENTA -> Color(0xFF573F60)
    AccentPreset.PURPLE -> Color(0xFF4B4161)
    AccentPreset.YELLOW -> Color(0xFF594C30)
    AccentPreset.LIME -> Color(0xFF4A5236)
    AccentPreset.ORANGE -> Color(0xFF5E4737)
    AccentPreset.PEACH -> Color(0xFF65483F)
    AccentPreset.COPPER -> Color(0xFF59473D)
    AccentPreset.MONOCHROME -> Color(0xFF474B53)
}

private fun AccentPreset.tertiary(): Color = when (this) {
    AccentPreset.AMBER -> LimeAccent
    AccentPreset.OCEAN -> Color(0xFF52D1C8)
    AccentPreset.COBALT -> Color(0xFF7AB8FF)
    AccentPreset.INDIGO -> Color(0xFF98A5FF)
    AccentPreset.SKY -> Color(0xFF67D7FF)
    AccentPreset.AQUA -> Color(0xFF7BE1FF)
    AccentPreset.TEAL -> Color(0xFF49D7C4)
    AccentPreset.MINT -> Color(0xFF8DDBA7)
    AccentPreset.EMERALD -> Color(0xFF7BE0B1)
    AccentPreset.FOREST -> Color(0xFF8DD95E)
    AccentPreset.ROSE -> Color(0xFFFF8FA9)
    AccentPreset.CRIMSON -> Color(0xFFFF8D7A)
    AccentPreset.MAGENTA -> Color(0xFFFF8FDB)
    AccentPreset.PURPLE -> Color(0xFFE18FFF)
    AccentPreset.YELLOW -> Color(0xFFE3B53B)
    AccentPreset.LIME -> Color(0xFFC6E35A)
    AccentPreset.ORANGE -> Color(0xFFFF9C52)
    AccentPreset.PEACH -> Color(0xFFFFB27A)
    AccentPreset.COPPER -> Color(0xFFFFB07A)
    AccentPreset.MONOCHROME -> Color(0xFFB7BCC7)
}

private fun lightBackground(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFF2EEE8)
    ContrastMode.STANDARD -> DawnBackground
    ContrastMode.HIGH -> Color(0xFFFFFBFD)
    ContrastMode.ULTRA -> Color(0xFFFFFFFF)
}

private fun lightSurface(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFFCF7FB)
    ContrastMode.STANDARD -> DawnSurface
    ContrastMode.HIGH -> Color(0xFFFFFFFF)
    ContrastMode.ULTRA -> Color(0xFFFFFFFF)
}

private fun lightSurfaceVariant(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFECE4E9)
    ContrastMode.STANDARD -> DawnSurfaceVariant
    ContrastMode.HIGH -> Color(0xFFF1E8F4)
    ContrastMode.ULTRA -> Color(0xFFF5EEF8)
}

private fun lightOnSurfaceVariant(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFF7A7085)
    ContrastMode.STANDARD -> DawnSecondary
    ContrastMode.HIGH -> Color(0xFF463C51)
    ContrastMode.ULTRA -> Color(0xFF2F2738)
}

private fun lightOutline(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFA89FB0)
    ContrastMode.STANDARD -> AshOutline
    ContrastMode.HIGH -> Color(0xFF4C4457)
    ContrastMode.ULTRA -> Color(0xFF2D2636)
}

private fun darkSecondary(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFC5BDD0)
    ContrastMode.STANDARD -> Mist
    ContrastMode.HIGH -> Color(0xFFE8E0F3)
    ContrastMode.ULTRA -> Color(0xFFF5EEFF)
}

private fun darkBackground(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFF1B1721)
    ContrastMode.STANDARD -> AshBackground
    ContrastMode.HIGH -> Color(0xFF0F0C13)
    ContrastMode.ULTRA -> Color(0xFF09070D)
}

private fun darkSurface(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFF2A2530)
    ContrastMode.STANDARD -> AshSurface
    ContrastMode.HIGH -> Color(0xFF18141D)
    ContrastMode.ULTRA -> Color(0xFF100D15)
}

private fun darkSurfaceVariant(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFF5A5365)
    ContrastMode.STANDARD -> AshSurfaceVariant
    ContrastMode.HIGH -> Color(0xFF3A3444)
    ContrastMode.ULTRA -> Color(0xFF282230)
}

private fun darkOnSurfaceVariant(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFC8C0D4)
    ContrastMode.STANDARD -> Mist
    ContrastMode.HIGH -> Color(0xFFF1EAF8)
    ContrastMode.ULTRA -> Color(0xFFFBF5FF)
}

private fun darkOutline(mode: ContrastMode): Color = when (mode) {
    ContrastMode.SOFT -> Color(0xFFA59DB0)
    ContrastMode.STANDARD -> AshOutline
    ContrastMode.HIGH -> Color(0xFFD5CEE0)
    ContrastMode.ULTRA -> Color(0xFFF1EBFB)
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
