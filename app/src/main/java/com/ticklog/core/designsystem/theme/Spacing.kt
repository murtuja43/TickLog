package com.ticklog.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale, exposed through the theme.
 *
 * Generous, consistent whitespace is central to the TickLog look, so spacing is
 * a first-class design token rather than a pile of magic numbers. Components
 * read values via `MaterialTheme`'s companion accessor (see [LocalSpacing]),
 * which keeps call sites declarative: `padding(TickLogTheme.spacing.large)`.
 */
@Immutable
data class Spacing(
    /** 2dp — hairline gaps. */
    val extraSmall: Dp = 4.dp,
    /** 8dp — tight groupings. */
    val small: Dp = 8.dp,
    /** 16dp — the default gutter between unrelated elements. */
    val medium: Dp = 16.dp,
    /** 24dp — comfortable section padding. */
    val large: Dp = 24.dp,
    /** 32dp — screen-edge breathing room. */
    val extraLarge: Dp = 32.dp,
    /** 48dp — hero spacing around empty states and onboarding. */
    val huge: Dp = 48.dp,
)

/**
 * Static composition local carrying the active [Spacing] scale down the tree.
 * It is `static` because the value never changes at runtime, which lets Compose
 * skip recomposition bookkeeping for every reader.
 */
val LocalSpacing = staticCompositionLocalOf { Spacing() }
