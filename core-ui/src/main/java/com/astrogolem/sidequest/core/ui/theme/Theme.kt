package com.astrogolem.sidequest.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SidequestColors = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentLime,
    tertiary = AccentGold,
    background = BgPrimary,
    surface = BgElevated,
    error = Danger,
    onPrimary = BgPrimary,
    onSecondary = BgPrimary,
    onTertiary = BgPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = TextPrimary,
)

@Composable
fun SidequestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SidequestColors,
        typography = SidequestTypography,
        content = content,
    )
}
