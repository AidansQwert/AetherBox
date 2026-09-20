package com.droidspaces.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

/**
 * Soft dual-orb ambient wash behind the main chrome.
 * Slow drift keeps depth without fighting DESIGN.md's flat surface language.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val topOrb = scheme.primary.copy(alpha = 0.14f + 0.04f * pulse)
    val midOrb = scheme.secondary.copy(alpha = 0.08f)
    val bottomOrb = scheme.tertiary.copy(alpha = 0.11f + 0.03f * pulse)
    val base = scheme.background

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = with(density) { maxWidth.toPx() }
        val h = with(density) { maxHeight.toPx() }
        val radius = w.coerceAtLeast(h)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(base)
                .background(
                    Brush.radialGradient(
                        colors = listOf(topOrb, Color.Transparent),
                        center = Offset(
                            w * (0.08f + 0.10f * drift),
                            h * (0.06f + 0.04f * (1f - drift))
                        ),
                        radius = radius * 0.72f * pulse
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(midOrb, Color.Transparent),
                        center = Offset(
                            w * (0.55f - 0.08f * drift),
                            h * (0.42f + 0.06f * drift)
                        ),
                        radius = radius * 0.55f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(bottomOrb, Color.Transparent),
                        center = Offset(
                            w * (0.90f - 0.06f * drift),
                            h * (0.94f - 0.05f * drift)
                        ),
                        radius = radius * 0.68f * pulse
                    )
                ),
            content = content
        )
    }
}
