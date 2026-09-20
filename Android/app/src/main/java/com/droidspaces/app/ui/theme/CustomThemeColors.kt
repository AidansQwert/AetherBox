package com.droidspaces.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * User-authored accent triad stored as hues (0–360).
 * Light/dark variants are derived so one custom theme works in both modes.
 */
data class CustomThemeColors(
    val primaryHue: Float = 175f,
    val secondaryHue: Float = 195f,
    val tertiaryHue: Float = 145f
) {
    val primaryLight: Color get() = hsv(primaryHue, 0.78f, 0.52f)
    val secondaryLight: Color get() = hsv(secondaryHue, 0.70f, 0.55f)
    val tertiaryLight: Color get() = hsv(tertiaryHue, 0.62f, 0.58f)
    val primaryDark: Color get() = hsv(primaryHue, 0.52f, 0.86f)
    val secondaryDark: Color get() = hsv(secondaryHue, 0.48f, 0.84f)
    val tertiaryDark: Color get() = hsv(tertiaryHue, 0.45f, 0.82f)

    fun asPalette(): ThemePaletteColors = ThemePaletteColors(
        displayName = "Custom",
        primaryLight = primaryLight,
        secondaryLight = secondaryLight,
        tertiaryLight = tertiaryLight,
        primaryDark = primaryDark,
        secondaryDark = secondaryDark,
        tertiaryDark = tertiaryDark
    )

    companion object {
        fun hsv(hue: Float, saturation: Float, value: Float): Color {
            val h = ((hue % 360f) + 360f) % 360f
            return Color.hsv(h, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
        }
    }
}

/** Resolved triad used by [DroidspacesTheme] — preset or custom. */
data class ThemePaletteColors(
    val displayName: String,
    val primaryLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color,
    val primaryDark: Color,
    val secondaryDark: Color,
    val tertiaryDark: Color
)

fun ThemePalette.toPaletteColors(): ThemePaletteColors = ThemePaletteColors(
    displayName = displayName,
    primaryLight = primaryLight,
    secondaryLight = secondaryLight,
    tertiaryLight = tertiaryLight,
    primaryDark = primaryDark,
    secondaryDark = secondaryDark,
    tertiaryDark = tertiaryDark
)

fun resolveThemePaletteColors(
    palette: ThemePalette,
    custom: CustomThemeColors
): ThemePaletteColors =
    if (palette == ThemePalette.CUSTOM) custom.asPalette() else palette.toPaletteColors()
