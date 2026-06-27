package com.ticklog.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius scale.
 *
 * TickLog favours soft, generously rounded corners across the board. The scale
 * maps onto Material 3's shape slots so any component that reads from the theme
 * (cards, buttons, dialogs, menus) picks up the rounded look automatically.
 */
val TickLogShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
