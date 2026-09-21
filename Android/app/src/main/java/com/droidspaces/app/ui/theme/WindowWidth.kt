package com.droidspaces.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Width breakpoints aligned with Material window size classes. */
enum class WidthClass {
    Compact,
    Medium,
    Expanded
}

@Immutable
data class AdaptiveMetrics(
    val widthClass: WidthClass,
    val screenWidthDp: Dp,
    val useRail: Boolean,
    val dualPaneHome: Boolean,
    val contentMaxWidth: Dp
)

@Composable
fun rememberAdaptiveMetrics(): AdaptiveMetrics {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp.dp
    val widthClass = when {
        config.screenWidthDp < 600 -> WidthClass.Compact
        config.screenWidthDp < 840 -> WidthClass.Medium
        else -> WidthClass.Expanded
    }
    return AdaptiveMetrics(
        widthClass = widthClass,
        screenWidthDp = widthDp,
        useRail = widthClass != WidthClass.Compact,
        dualPaneHome = widthClass == WidthClass.Expanded,
        contentMaxWidth = when (widthClass) {
            WidthClass.Compact -> widthDp
            WidthClass.Medium -> 720.dp
            WidthClass.Expanded -> 1100.dp
        }
    )
}
