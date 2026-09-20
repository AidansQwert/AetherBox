package com.droidspaces.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Soft, modern shape scale — slightly rounder than the previous 20dp card language
 * so surfaces feel closer to current Material expressive / iOS-style soft chrome.
 */
val DsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val CardShape = RoundedCornerShape(24.dp)
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
val PillShape = RoundedCornerShape(999.dp)
val NavBarShape = RoundedCornerShape(28.dp)
val ActionButtonShape = RoundedCornerShape(18.dp)
