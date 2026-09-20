package com.droidspaces.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.droidspaces.app.ui.theme.ThemePalette

/**
 * A single circular color-palette swatch for the accent color picker.
 *
 * Visually splits the circle into 3 geometric sections:
 *  - Top half → primary color
 *  - Bottom-left quarter → secondary color
 *  - Bottom-right quarter → tertiary color
 *
 * Selected state shows:
 *  - Animated checkmark icon in center
 *  - Outer ring border matching primary
 */
@Composable
fun ColorPaletteSwatch(
    palette: ThemePalette,
    selected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchSize: Float = 52f
) {
    val primary = if (isDarkTheme) palette.primaryDark else palette.primaryLight
    val secondary = if (isDarkTheme) palette.secondaryDark else palette.secondaryLight
    val tertiary = if (isDarkTheme) palette.tertiaryDark else palette.tertiaryLight
    ColorPaletteSwatch(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        selected = selected,
        label = palette.displayName,
        onClick = onClick,
        modifier = modifier,
        swatchSize = swatchSize
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColorPaletteSwatch(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchSize: Float = 52f,
    onLongClick: (() -> Unit)? = null
) {
    val selectionProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selectionAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(swatchSize.dp)
            .semantics { contentDescription = label }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = false, radius = (swatchSize / 2 + 4).dp),
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val canvasSize = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = canvasSize / 2f

        val ringPadding = 4.dp.toPx()
        val outerRadius = radius + ringPadding
        if (selectionProgress > 0f) {
            drawCircle(
                color = primary.copy(alpha = selectionProgress * 0.8f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx() * selectionProgress)
            )
        }

        val arcRect = Size(canvasSize, canvasSize)
        val arcTopLeft = Offset(
            (size.width - canvasSize) / 2f,
            (size.height - canvasSize) / 2f
        )

        drawArc(
            color = primary,
            startAngle = -180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcRect
        )
        drawArc(
            color = secondary,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcRect
        )
        drawArc(
            color = tertiary,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcRect
        )

        if (selectionProgress > 0.01f) {
            drawCheckmark(center, radius * 0.35f, selectionProgress)
        }
    }
}

private fun DrawScope.drawCheckmark(
    center: Offset,
    checkSize: Float,
    progress: Float
) {
    val strokeWidth = 2.5.dp.toPx()
    val alpha = progress.coerceIn(0f, 1f)
    val scale = 0.5f + 0.5f * progress
    val scaledSize = checkSize * scale

    val startX = center.x - scaledSize * 0.5f
    val startY = center.y + scaledSize * 0.05f
    val midX = center.x - scaledSize * 0.1f
    val midY = center.y + scaledSize * 0.45f
    val endX = center.x + scaledSize * 0.6f
    val endY = center.y - scaledSize * 0.35f

    drawCircle(
        color = Color.Black.copy(alpha = 0.35f * alpha),
        radius = checkSize * 1.1f * scale,
        center = center
    )

    val checkColor = Color.White.copy(alpha = alpha)
    drawLine(
        color = checkColor,
        start = Offset(startX, startY),
        end = Offset(midX, midY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = checkColor,
        start = Offset(midX, midY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
