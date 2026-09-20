package com.droidspaces.app.ui.theme

import androidx.compose.ui.graphics.Color

val AMOLED_BLACK = Color(0xFF000000)

/**
 * Pre-defined accent color palettes for the app, mimicking Android's
 * "Wallpaper & style" palette picker.
 *
 * Each palette defines primary, secondary, and tertiary colors for both
 * light and dark modes.
 */
enum class ThemePalette(
    val displayName: String,
    val primaryLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color,
    val primaryDark: Color,
    val secondaryDark: Color,
    val tertiaryDark: Color
) {
    CATPPUCCIN(
        displayName = "Catppuccin",
        primaryLight = Color(0xFF8AADF4),   // Blue
        secondaryLight = Color(0xFFB7BDF8), // Lavender
        tertiaryLight = Color(0xFFA6DA95),  // Green
        primaryDark = Color(0xFF7DC4E4),    // Sky
        secondaryDark = Color(0xFFF5BDE6),  // Pink
        tertiaryDark = Color(0xFFA6DA95)    // Green
    ),
    OCEAN(
        displayName = "Ocean",
        primaryLight = Color(0xFF0277BD),   // Deep Blue
        secondaryLight = Color(0xFF00ACC1), // Cyan
        tertiaryLight = Color(0xFF26A69A),  // Teal
        primaryDark = Color(0xFF4FC3F7),    // Light Blue
        secondaryDark = Color(0xFF4DD0E1),  // Light Cyan
        tertiaryDark = Color(0xFF80CBC4)    // Light Teal
    ),
    FOREST(
        displayName = "Forest",
        primaryLight = Color(0xFF2E7D32),   // Deep Green
        secondaryLight = Color(0xFF558B2F), // Olive Green
        tertiaryLight = Color(0xFF8D6E63),  // Brown
        primaryDark = Color(0xFF81C784),    // Light Green
        secondaryDark = Color(0xFFA5D6A7),  // Pale Green
        tertiaryDark = Color(0xFFBCAAA4)    // Light Brown
    ),
    SUNSET(
        displayName = "Sunset",
        primaryLight = Color(0xFFD84315),   // Deep Orange
        secondaryLight = Color(0xFFF4511E), // Orange-Red
        tertiaryLight = Color(0xFFFFB300),  // Amber
        primaryDark = Color(0xFFFF8A65),    // Light Orange
        secondaryDark = Color(0xFFFF8A80),  // Light Coral
        tertiaryDark = Color(0xFFFFD54F)    // Light Amber
    ),
    AMETHYST(
        displayName = "Amethyst",
        primaryLight = Color(0xFF6A1B9A),   // Deep Purple
        secondaryLight = Color(0xFF8E24AA), // Purple
        tertiaryLight = Color(0xFFAD1457),  // Deep Pink
        primaryDark = Color(0xFFCE93D8),    // Light Purple
        secondaryDark = Color(0xFFBA68C8),  // Medium Purple
        tertiaryDark = Color(0xFFF48FB1)    // Light Pink
    ),
    SAKURA(
        displayName = "Sakura",
        primaryLight = Color(0xFFD81B60),   // Pink
        secondaryLight = Color(0xFFEC407A), // Rose
        tertiaryLight = Color(0xFF7E57C2),  // Violet
        primaryDark = Color(0xFFF48FB1),    // Light Pink
        secondaryDark = Color(0xFFF8BBD0),  // Pale Pink
        tertiaryDark = Color(0xFFB39DDB)    // Light Violet
    ),
    NORD(
        displayName = "Nord",
        primaryLight = Color(0xFF5E81AC),
        secondaryLight = Color(0xFF81A1C1),
        tertiaryLight = Color(0xFF88C0D0),
        primaryDark = Color(0xFF88C0D0),
        secondaryDark = Color(0xFF81A1C1),
        tertiaryDark = Color(0xFF8FBCBB)
    ),
    MOCHA(
        displayName = "Mocha",
        primaryLight = Color(0xFF6F4E37),
        secondaryLight = Color(0xFFA0522D),
        tertiaryLight = Color(0xFFC4A484),
        primaryDark = Color(0xFFD4A574),
        secondaryDark = Color(0xFFE8B88A),
        tertiaryDark = Color(0xFFF0D5B8)
    ),
    EMBER(
        displayName = "Ember",
        primaryLight = Color(0xFFB71C1C),
        secondaryLight = Color(0xFFE65100),
        tertiaryLight = Color(0xFFFF8F00),
        primaryDark = Color(0xFFEF9A9A),
        secondaryDark = Color(0xFFFFAB91),
        tertiaryDark = Color(0xFFFFE082)
    ),
    GRAPHITE(
        displayName = "Graphite",
        primaryLight = Color(0xFF455A64),
        secondaryLight = Color(0xFF607D8B),
        tertiaryLight = Color(0xFF78909C),
        primaryDark = Color(0xFFB0BEC5),
        secondaryDark = Color(0xFF90A4AE),
        tertiaryDark = Color(0xFFCFD8DC)
    ),
    AURORA(
        displayName = "Aurora",
        primaryLight = Color(0xFF00897B),
        secondaryLight = Color(0xFF43A047),
        tertiaryLight = Color(0xFF1E88E5),
        primaryDark = Color(0xFF4DB6AC),
        secondaryDark = Color(0xFF81C784),
        tertiaryDark = Color(0xFF64B5F6)
    ),
    // Cool midnight teal — distinct from the purple / cream defaults.
    NEBULA(
        displayName = "Nebula",
        primaryLight = Color(0xFF006D77),
        secondaryLight = Color(0xFF0A9396),
        tertiaryLight = Color(0xFF94D2BD),
        primaryDark = Color(0xFF2EC4B6),
        secondaryDark = Color(0xFF48CAE4),
        tertiaryDark = Color(0xFF90E0EF)
    ),
    GLACIER(
        displayName = "Glacier",
        primaryLight = Color(0xFF1565C0),
        secondaryLight = Color(0xFF0277BD),
        tertiaryLight = Color(0xFF00838F),
        primaryDark = Color(0xFF90CAF9),
        secondaryDark = Color(0xFF81D4FA),
        tertiaryDark = Color(0xFF80DEEA)
    );

    companion object {
        fun fromName(name: String): ThemePalette =
            entries.find { it.name == name } ?: NEBULA
    }
}
