package com.astrogolem.sidequest.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SidequestTheme(
    themePreset: ThemePreset = ThemePreset.SOLAR,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(themePreset)
    SidequestPaletteRegistry.current = palette
    val sidequestColors = darkColorScheme(
        primary = palette.accentPrimary,
        secondary = palette.accentSecondary,
        tertiary = palette.accentBronze,
        background = palette.bgPrimary,
        surface = palette.bgElevated,
        error = palette.danger,
        onPrimary = palette.bgPrimary,
        onSecondary = palette.bgPrimary,
        onTertiary = palette.bgPrimary,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary,
        onError = palette.textPrimary,
    )
    MaterialTheme(
        colorScheme = sidequestColors,
        typography = SidequestTypography,
        content = content,
    )
}
