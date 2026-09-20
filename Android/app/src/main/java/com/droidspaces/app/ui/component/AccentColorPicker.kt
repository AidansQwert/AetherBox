package com.droidspaces.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidspaces.app.R
import com.droidspaces.app.ui.theme.CustomThemeColors
import com.droidspaces.app.ui.theme.ThemePalette
import com.droidspaces.app.util.PreferencesManager

/**
 * A horizontally scrollable row of [ColorPaletteSwatch] items,
 * mimicking Android's "Wallpaper & style" accent color picker.
 *
 * Includes a Custom tile and a create-theme action.
 */
@Composable
fun AccentColorPicker(
    selectedPalette: ThemePalette,
    isDarkTheme: Boolean,
    customThemeColors: CustomThemeColors,
    onPaletteSelected: (ThemePalette) -> Unit,
    onCustomColorsApplied: (CustomThemeColors) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditor by remember { mutableStateOf(false) }
    val resolvedCustom = customThemeColors.asPalette()
    val selectedLabel = if (selectedPalette == ThemePalette.CUSTOM) {
        stringResource(R.string.theme_custom_label)
    } else {
        selectedPalette.displayName
    }

    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(R.string.accent_color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            userScrollEnabled = true
        ) {
            items(
                items = ThemePalette.presets,
                key = { it.name }
            ) { palette ->
                ColorPaletteSwatch(
                    palette = palette,
                    selected = palette == selectedPalette,
                    isDarkTheme = isDarkTheme,
                    onClick = { onPaletteSelected(palette) }
                )
            }
            item(key = "custom") {
                ColorPaletteSwatch(
                    primary = if (isDarkTheme) resolvedCustom.primaryDark else resolvedCustom.primaryLight,
                    secondary = if (isDarkTheme) resolvedCustom.secondaryDark else resolvedCustom.secondaryLight,
                    tertiary = if (isDarkTheme) resolvedCustom.tertiaryDark else resolvedCustom.tertiaryLight,
                    selected = selectedPalette == ThemePalette.CUSTOM,
                    label = stringResource(R.string.theme_custom_label),
                    onClick = {
                        onPaletteSelected(ThemePalette.CUSTOM)
                    },
                    onLongClick = { showEditor = true }
                )
            }
            item(key = "create") {
                Surface(
                    onClick = { showEditor = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.theme_create_title),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { showEditor = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                )
            ) {
                Text(
                    text = stringResource(R.string.theme_create_cta),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            if (selectedPalette == ThemePalette.CUSTOM) {
                Surface(
                    onClick = { showEditor = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.theme_edit_custom),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    if (showEditor) {
        CustomThemeEditorDialog(
            initial = customThemeColors,
            isDarkTheme = isDarkTheme,
            onDismiss = { showEditor = false },
            onApply = { colors ->
                val prefs = PreferencesManager.getInstance(context)
                applyCustomTheme(prefs, colors)
                onCustomColorsApplied(colors)
                showEditor = false
            }
        )
    }
}
