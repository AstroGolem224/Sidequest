package com.astrogolem.sidequest.core.ui.theme

import androidx.compose.ui.graphics.Color

enum class ThemePreset { SOLAR, AETHER, FROST, HEARTH, CYBER }

data class SidequestPalette(
    val bgPrimary: Color,
    val bgElevated: Color,
    val bgGlow: Color,
    val bgPanel: Color,
    val cardSurface: Color,
    val cardSurfaceStrong: Color,
    val cardStroke: Color,
    val cardStrokeStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentBronze: Color,
    val accentCoral: Color,
    val hudInfo: Color,
    val hudWarn: Color,
    val hudAlt: Color,
    val hudNeutral: Color,
    val danger: Color,
)

private val SolarPalette = SidequestPalette(
    bgPrimary = Color(0xFF121212),
    bgElevated = Color(0xFF171514),
    bgGlow = Color(0xFF211A12),
    bgPanel = Color(0xDE1C1814),
    cardSurface = Color(0xFF1C1814),
    cardSurfaceStrong = Color(0xFF26211A),
    cardStroke = Color(0x24D4AF37),
    cardStrokeStrong = Color(0x40D4AF37),
    textPrimary = Color(0xFFF4ECDD),
    textSecondary = Color(0xFFCAB074),
    accentPrimary = Color(0xFFFFB300),
    accentSecondary = Color(0xFFD4AF37),
    accentBronze = Color(0xFFFF8F00),
    accentCoral = Color(0xFFFFD79B),
    hudInfo = Color(0xFFFFB300),
    hudWarn = Color(0xFFD4AF37),
    hudAlt = Color(0xFFFF8F00),
    hudNeutral = Color(0xFF9B7A33),
    danger = Color(0xFFFF5D73),
)

private val AetherPalette = SidequestPalette(
    bgPrimary = Color(0xFF12121B),
    bgElevated = Color(0xFF181826),
    bgGlow = Color(0xFF241E37),
    bgPanel = Color(0xD91B1B2A),
    cardSurface = Color(0xFF191923),
    cardSurfaceStrong = Color(0xFF242436),
    cardStroke = Color(0x249D4EDD),
    cardStrokeStrong = Color(0x409D4EDD),
    textPrimary = Color(0xFFF0ECFF),
    textSecondary = Color(0xFFC0B4DB),
    accentPrimary = Color(0xFF9D4EDD),
    accentSecondary = Color(0xFF00E5FF),
    accentBronze = Color(0xFFFFD700),
    accentCoral = Color(0xFFC87FFF),
    hudInfo = Color(0xFF00E5FF),
    hudWarn = Color(0xFFFFD700),
    hudAlt = Color(0xFFC87FFF),
    hudNeutral = Color(0xFF7E729D),
    danger = Color(0xFFFF5D73),
)

private val FrostPalette = SidequestPalette(
    bgPrimary = Color(0xFF0A1929),
    bgElevated = Color(0xFF112132),
    bgGlow = Color(0xFF173046),
    bgPanel = Color(0xD915273B),
    cardSurface = Color(0xFF102133),
    cardSurfaceStrong = Color(0xFF183249),
    cardStroke = Color(0x24B0BEC5),
    cardStrokeStrong = Color(0x40B0BEC5),
    textPrimary = Color(0xFFEAF7FA),
    textSecondary = Color(0xFFB0BEC5),
    accentPrimary = Color(0xFF00E5FF),
    accentSecondary = Color(0xFFB0BEC5),
    accentBronze = Color(0xFFE0F7FA),
    accentCoral = Color(0xFFAEEFFF),
    hudInfo = Color(0xFF00E5FF),
    hudWarn = Color(0xFFE0F7FA),
    hudAlt = Color(0xFFB0BEC5),
    hudNeutral = Color(0xFF708996),
    danger = Color(0xFFFF5D73),
)

private val HearthPalette = SidequestPalette(
    bgPrimary = Color(0xFF1A1614),
    bgElevated = Color(0xFF211A17),
    bgGlow = Color(0xFF30211B),
    bgPanel = Color(0xDC261E1A),
    cardSurface = Color(0xFF261F1B),
    cardSurfaceStrong = Color(0xFF332924),
    cardStroke = Color(0x24A0522D),
    cardStrokeStrong = Color(0x40A0522D),
    textPrimary = Color(0xFFF1E7DE),
    textSecondary = Color(0xFFD9C0AA),
    accentPrimary = Color(0xFFFF5733),
    accentSecondary = Color(0xFFA0522D),
    accentBronze = Color(0xFFF5DEB3),
    accentCoral = Color(0xFFFFB08E),
    hudInfo = Color(0xFFFF5733),
    hudWarn = Color(0xFFF5DEB3),
    hudAlt = Color(0xFFFF9B7F),
    hudNeutral = Color(0xFF8A664F),
    danger = Color(0xFFFF6E61),
)

private val CyberPalette = SidequestPalette(
    bgPrimary = Color(0xFF07070F),
    bgElevated = Color(0xFF0F0F23),
    bgGlow = Color(0xFF1E153A),
    bgPanel = Color(0xE6121226),
    cardSurface = Color(0xFF15152F),
    cardSurfaceStrong = Color(0xFF1E1E45),
    cardStroke = Color(0x33BB86FC), // Neon Violet
    cardStrokeStrong = Color(0x66BB86FC),
    textPrimary = Color(0xFFE6E6FA), // Lavender
    textSecondary = Color(0xFF9494B8),
    accentPrimary = Color(0xFFBB86FC), // Neon Violet
    accentSecondary = Color(0xFF03DAC6), // Cyan
    accentBronze = Color(0xFFFFA000), // Amber
    accentCoral = Color(0xFFFF8A80),
    hudInfo = Color(0xFF03DAC6),
    hudWarn = Color(0xFFFFA000),
    hudAlt = Color(0xFF9D4EDD),
    hudNeutral = Color(0xFF6272A4),
    danger = Color(0xFFFF5555),
)

internal object SidequestPaletteRegistry {
    var current: SidequestPalette = CyberPalette
}

fun paletteFor(themePreset: ThemePreset): SidequestPalette {
    return when (themePreset) {
        ThemePreset.SOLAR -> SolarPalette
        ThemePreset.AETHER -> AetherPalette
        ThemePreset.FROST -> FrostPalette
        ThemePreset.HEARTH -> HearthPalette
        ThemePreset.CYBER -> CyberPalette
    }
}

val BgPrimary: Color get() = SidequestPaletteRegistry.current.bgPrimary
val BgElevated: Color get() = SidequestPaletteRegistry.current.bgElevated
val BgGlow: Color get() = SidequestPaletteRegistry.current.bgGlow
val BgPanel: Color get() = SidequestPaletteRegistry.current.bgPanel
val CardSurface: Color get() = SidequestPaletteRegistry.current.cardSurface
val CardSurfaceStrong: Color get() = SidequestPaletteRegistry.current.cardSurfaceStrong
val CardStroke: Color get() = SidequestPaletteRegistry.current.cardStroke
val CardStrokeStrong: Color get() = SidequestPaletteRegistry.current.cardStrokeStrong
val TextPrimary: Color get() = SidequestPaletteRegistry.current.textPrimary
val TextSecondary: Color get() = SidequestPaletteRegistry.current.textSecondary
val AccentPrimary: Color get() = SidequestPaletteRegistry.current.accentPrimary
val AccentSecondary: Color get() = SidequestPaletteRegistry.current.accentSecondary
val AccentBronze: Color get() = SidequestPaletteRegistry.current.accentBronze
val AccentCyan: Color get() = SidequestPaletteRegistry.current.hudInfo
val AccentLime: Color get() = SidequestPaletteRegistry.current.accentSecondary
val AccentGold: Color get() = SidequestPaletteRegistry.current.hudWarn
val AccentViolet: Color get() = SidequestPaletteRegistry.current.hudAlt
val AccentCoral: Color get() = SidequestPaletteRegistry.current.accentCoral
val AccentNeutral: Color get() = SidequestPaletteRegistry.current.hudNeutral
val Danger: Color get() = SidequestPaletteRegistry.current.danger
