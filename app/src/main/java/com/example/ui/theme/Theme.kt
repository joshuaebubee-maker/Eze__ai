package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EzeColorScheme = darkColorScheme(
    primary = EzePrimary,
    onPrimary = EzeBackground,
    primaryContainer = EzeSurfaceVariant,
    onPrimaryContainer = EzePrimary,
    secondary = EzeSecondary,
    onSecondary = EzeBackground,
    secondaryContainer = EzeSurfaceVariant,
    onSecondaryContainer = EzeOnSurface,
    tertiary = EzeTertiary,
    background = EzeBackground,
    onBackground = EzeOnBackground,
    surface = EzeSurface,
    onSurface = EzeOnSurface,
    surfaceVariant = EzeSurfaceVariant,
    onSurfaceVariant = EzeOnSurfaceVariant,
    error = EzeError,
    outline = EzeBorder
)

@Composable
fun EzeTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = EzeBackground.toArgb()
            window.navigationBarColor = EzeBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = EzeColorScheme,
        typography = Typography,
        content = content
    )
}
