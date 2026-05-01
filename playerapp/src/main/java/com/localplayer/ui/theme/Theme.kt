package com.localplayer.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Ember,
    onPrimary = MoonText,
    primaryContainer = Color(0xFFF0D4C1),
    onPrimaryContainer = DawnText,
    secondary = DawnSecondary,
    onSecondary = MoonText,
    secondaryContainer = Color(0xFFE4DAF0),
    onSecondaryContainer = DawnText,
    tertiary = LimeAccent,
    onTertiary = Color(0xFF0F1E00),
    tertiaryContainer = Color(0xFFD3F7B5),
    onTertiaryContainer = Color(0xFF182900),
    background = DawnBackground,
    onBackground = DawnText,
    surface = DawnSurface,
    onSurface = DawnText,
    surfaceVariant = DawnSurfaceVariant,
    onSurfaceVariant = DawnSecondary,
    outline = AshOutline,
    error = CoralError,
)

private val DarkColors = darkColorScheme(
    primary = Ember,
    onPrimary = MoonText,
    primaryContainer = EmberContainer,
    onPrimaryContainer = Color(0xFFF7DCC8),
    secondary = Mist,
    onSecondary = Color(0xFF2A2233),
    secondaryContainer = Color(0xFF4D4558),
    onSecondaryContainer = MoonText,
    tertiary = LimeAccent,
    onTertiary = Color(0xFF102100),
    tertiaryContainer = Color(0xFF284900),
    onTertiaryContainer = Color(0xFFD0F8B5),
    background = AshBackground,
    onBackground = MoonText,
    surface = AshSurface,
    onSurface = MoonText,
    surfaceVariant = AshSurfaceVariant,
    onSurfaceVariant = Mist,
    outline = AshOutline,
    error = Color(0xFFFFB4AB),
)

@Composable
fun LocalPlayerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                window.statusBarColor = colorScheme.primary.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
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

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
