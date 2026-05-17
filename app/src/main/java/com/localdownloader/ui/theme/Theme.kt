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
    background = if (contrastMode == ContrastMode.HIGH) Color(0xFFFFFBFD) else DawnBackground,
    onBackground = DawnText,
    surface = if (contrastMode == ContrastMode.HIGH) Color(0xFFFFFFFF) else DawnSurface,
    onSurface = DawnText,
    surfaceVariant = if (contrastMode == ContrastMode.HIGH) Color(0xFFF1E8F4) else DawnSurfaceVariant,
    onSurfaceVariant = if (contrastMode == ContrastMode.HIGH) Color(0xFF463C51) else DawnSecondary,
    outline = if (contrastMode == ContrastMode.HIGH) Color(0xFF4C4457) else AshOutline,
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
    secondary = if (contrastMode == ContrastMode.HIGH) Color(0xFFE8E0F3) else Mist,
    onSecondary = Color(0xFF2A2233),
    secondaryContainer = accentPreset.secondaryContainerDark(),
    onSecondaryContainer = MoonText,
    tertiary = accentPreset.tertiary(),
    onTertiary = Color(0xFF102100),
    tertiaryContainer = Color(0xFF284900),
    onTertiaryContainer = Color(0xFFD0F8B5),
    background = if (contrastMode == ContrastMode.HIGH) Color(0xFF0F0C13) else AshBackground,
    onBackground = MoonText,
    surface = if (contrastMode == ContrastMode.HIGH) Color(0xFF18141D) else AshSurface,
    onSurface = MoonText,
    surfaceVariant = if (contrastMode == ContrastMode.HIGH) Color(0xFF3A3444) else AshSurfaceVariant,
    onSurfaceVariant = if (contrastMode == ContrastMode.HIGH) Color(0xFFF1EAF8) else Mist,
    outline = if (contrastMode == ContrastMode.HIGH) Color(0xFFD5CEE0) else AshOutline,
    error = Color(0xFFFFB4AB),
)

private fun AccentPreset.primary(): Color = when (this) {
    AccentPreset.AMBER -> Ember
    AccentPreset.OCEAN -> Ocean
    AccentPreset.FOREST -> Forest
    AccentPreset.ROSE -> Rose
    AccentPreset.PURPLE -> Violet
    AccentPreset.YELLOW -> Gold
    AccentPreset.ORANGE -> Tangerine
    AccentPreset.MONOCHROME -> Graphite
}

private fun AccentPreset.primaryContainerLight(): Color = when (this) {
    AccentPreset.AMBER -> Color(0xFFF0D4C1)
    AccentPreset.OCEAN -> Color(0xFFD0E8FF)
    AccentPreset.FOREST -> Color(0xFFD3F0DE)
    AccentPreset.ROSE -> Color(0xFFF5D3DC)
    AccentPreset.PURPLE -> Color(0xFFE7DDFF)
    AccentPreset.YELLOW -> Color(0xFFF8E7AE)
    AccentPreset.ORANGE -> Color(0xFFFFDEBF)
    AccentPreset.MONOCHROME -> Color(0xFFE1E3E8)
}

private fun AccentPreset.primaryContainerDark(): Color = when (this) {
    AccentPreset.AMBER -> EmberContainer
    AccentPreset.OCEAN -> Color(0xFF0C3A63)
    AccentPreset.FOREST -> Color(0xFF143D2A)
    AccentPreset.ROSE -> Color(0xFF5A2433)
    AccentPreset.PURPLE -> Color(0xFF37256C)
    AccentPreset.YELLOW -> Color(0xFF4D3900)
    AccentPreset.ORANGE -> Color(0xFF5C2F00)
    AccentPreset.MONOCHROME -> Color(0xFF3A3D44)
}

private fun AccentPreset.secondaryLight(): Color = when (this) {
    AccentPreset.AMBER -> DawnSecondary
    AccentPreset.OCEAN -> Color(0xFF546E90)
    AccentPreset.FOREST -> Color(0xFF5B7165)
    AccentPreset.ROSE -> Color(0xFF7B5A69)
    AccentPreset.PURPLE -> Color(0xFF6A5C8D)
    AccentPreset.YELLOW -> Color(0xFF76633D)
    AccentPreset.ORANGE -> Color(0xFF7B604A)
    AccentPreset.MONOCHROME -> Color(0xFF60646C)
}

private fun AccentPreset.secondaryContainerLight(): Color = when (this) {
    AccentPreset.AMBER -> Color(0xFFE4DAF0)
    AccentPreset.OCEAN -> Color(0xFFE0E8F6)
    AccentPreset.FOREST -> Color(0xFFE0EBDD)
    AccentPreset.ROSE -> Color(0xFFF0DDE7)
    AccentPreset.PURPLE -> Color(0xFFE9E1F7)
    AccentPreset.YELLOW -> Color(0xFFF1E7CE)
    AccentPreset.ORANGE -> Color(0xFFF3E1D2)
    AccentPreset.MONOCHROME -> Color(0xFFE7E8EC)
}

private fun AccentPreset.secondaryContainerDark(): Color = when (this) {
    AccentPreset.AMBER -> Color(0xFF4D4558)
    AccentPreset.OCEAN -> Color(0xFF34485E)
    AccentPreset.FOREST -> Color(0xFF3A4E43)
    AccentPreset.ROSE -> Color(0xFF563C47)
    AccentPreset.PURPLE -> Color(0xFF4B4161)
    AccentPreset.YELLOW -> Color(0xFF594C30)
    AccentPreset.ORANGE -> Color(0xFF5E4737)
    AccentPreset.MONOCHROME -> Color(0xFF474B53)
}

private fun AccentPreset.tertiary(): Color = when (this) {
    AccentPreset.AMBER -> LimeAccent
    AccentPreset.OCEAN -> Color(0xFF52D1C8)
    AccentPreset.FOREST -> Color(0xFF8DD95E)
    AccentPreset.ROSE -> Color(0xFFFF8FA9)
    AccentPreset.PURPLE -> Color(0xFFE18FFF)
    AccentPreset.YELLOW -> Color(0xFFE3B53B)
    AccentPreset.ORANGE -> Color(0xFFFF9C52)
    AccentPreset.MONOCHROME -> Color(0xFFB7BCC7)
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
