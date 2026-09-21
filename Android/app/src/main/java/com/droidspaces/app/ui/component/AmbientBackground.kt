package com.droidspaces.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Soft static wash — calm backdrop without drifting orbs.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.06f),
                        Color.Transparent,
                        scheme.secondary.copy(alpha = 0.03f)
                    )
                )
            ),
        content = content
    )
}
