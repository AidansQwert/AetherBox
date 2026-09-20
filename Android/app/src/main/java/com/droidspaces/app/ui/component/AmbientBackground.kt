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
 * Aether mist — diagonal wash + drifting orbs.
 * Stronger brand atmosphere than a flat Material scaffold.
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
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val topOrb = scheme.primary.copy(alpha = 0.18f + 0.05f * pulse)
    val midOrb = scheme.secondary.copy(alpha = 0.10f)
    val bottomOrb = scheme.tertiary.copy(alpha = 0.09f + 0.03f * pulse)
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
                    Brush.linearGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.10f),
                            Color.Transparent,
                            scheme.secondary.copy(alpha = 0.06f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.95f, h * 0.55f)
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(topOrb, Color.Transparent),
                        center = Offset(
                            w * (0.12f + 0.12f * drift),
                            h * (0.04f + 0.05f * (1f - drift))
                        ),
                        radius = radius * 0.78f * pulse
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(midOrb, Color.Transparent),
                        center = Offset(
                            w * (0.72f - 0.10f * drift),
                            h * (0.38f + 0.08f * drift)
                        ),
                        radius = radius * 0.58f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(bottomOrb, Color.Transparent),
                        center = Offset(
                            w * (0.88f - 0.07f * drift),
                            h * (0.92f - 0.06f * drift)
                        ),
                        radius = radius * 0.70f * pulse
                    )
                ),
            content = content
        )
    }
}
