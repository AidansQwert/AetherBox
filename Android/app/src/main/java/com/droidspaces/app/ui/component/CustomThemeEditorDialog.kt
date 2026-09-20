package com.droidspaces.app.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidspaces.app.R
import com.droidspaces.app.ui.theme.CustomThemeColors
import com.droidspaces.app.util.PreferencesManager

/**
 * Dialog to author a custom accent triad from three hue sliders.
 * Saves as [ThemePalette.CUSTOM] and turns off dynamic colour so the accents apply.
 */
@Composable
fun CustomThemeEditorDialog(
    initial: CustomThemeColors,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onApply: (CustomThemeColors) -> Unit
) {
    val context = LocalContext.current
    var primaryHue by remember { mutableFloatStateOf(initial.primaryHue) }
    var secondaryHue by remember { mutableFloatStateOf(initial.secondaryHue) }
    var tertiaryHue by remember { mutableFloatStateOf(initial.tertiaryHue) }

    val draft = CustomThemeColors(primaryHue, secondaryHue, tertiaryHue)
    val previewPrimary = if (isDarkTheme) draft.primaryDark else draft.primaryLight
    val previewSecondary = if (isDarkTheme) draft.secondaryDark else draft.secondaryLight
    val previewTertiary = if (isDarkTheme) draft.tertiaryDark else draft.tertiaryLight

    DsDialog(
        onDismiss = onDismiss,
        footer = {
            DialogFooterRow(
                dismissLabel = context.getString(R.string.cancel),
                confirmLabel = context.getString(R.string.theme_apply_custom),
                onDismiss = onDismiss,
                onConfirm = { onApply(draft) }
            )
        }
    ) {
        Text(
            text = stringResource(R.string.theme_create_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.theme_create_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviewDot(previewPrimary)
            PreviewDot(previewSecondary)
            PreviewDot(previewTertiary)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = previewPrimary.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(1.dp, previewPrimary.copy(alpha = 0.35f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    Text(
                        text = stringResource(R.string.theme_preview_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = previewPrimary
                    )
                }
            }
        }

        HueSliderRow(
            label = stringResource(R.string.theme_hue_primary),
            hue = primaryHue,
            onHueChange = { primaryHue = it },
            activeColor = previewPrimary
        )
        HueSliderRow(
            label = stringResource(R.string.theme_hue_secondary),
            hue = secondaryHue,
            onHueChange = { secondaryHue = it },
            activeColor = previewSecondary
        )
        HueSliderRow(
            label = stringResource(R.string.theme_hue_tertiary),
            hue = tertiaryHue,
            onHueChange = { tertiaryHue = it },
            activeColor = previewTertiary
        )
    }
}

@Composable
private fun PreviewDot(color: Color) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = color,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {}
}

@Composable
private fun HueSliderRow(
    label: String,
    hue: Float,
    onHueChange: (Float) -> Unit,
    activeColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${hue.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(horizontal = 2.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                val colors = (0..12).map { i ->
                    CustomThemeColors.hsv(i * 30f, 0.85f, 0.9f)
                }
                drawRoundRect(
                    brush = Brush.horizontalGradient(colors),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )
            }
        }
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor.copy(alpha = 0.55f),
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }
}

/**
 * Persist custom hues, select CUSTOM palette, and disable dynamic colour.
 */
fun applyCustomTheme(prefs: PreferencesManager, colors: CustomThemeColors) {
    prefs.customThemeColors = colors
    prefs.useDynamicColor = false
    prefs.themePalette = com.droidspaces.app.ui.theme.ThemePalette.CUSTOM.name
}
