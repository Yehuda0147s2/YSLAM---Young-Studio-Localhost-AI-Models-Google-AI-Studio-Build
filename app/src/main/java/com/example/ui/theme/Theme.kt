package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val YslamDarkColorScheme = darkColorScheme(
    primary = YslamCyan,
    onPrimary = YslamBackground,
    primaryContainer = YslamSurfaceVariant,
    onPrimaryContainer = YslamCyan,
    secondary = YslamPurple,
    onSecondary = YslamBackground,
    secondaryContainer = YslamSurfaceVariant,
    onSecondaryContainer = YslamPurple,
    tertiary = YslamGreen,
    onTertiary = YslamBackground,
    background = YslamBackground,
    onBackground = YslamTextPrimary,
    surface = YslamSurface,
    onSurface = YslamTextPrimary,
    surfaceVariant = YslamSurfaceVariant,
    onSurfaceVariant = YslamTextSecondary,
    outline = YslamBorder,
    outlineVariant = YslamBorder
)

@Composable
fun YslamTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = YslamBackground.toArgb()
                window.navigationBarColor = YslamBackground.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = YslamDarkColorScheme,
        typography = YslamTypography,
        content = content
    )
}
