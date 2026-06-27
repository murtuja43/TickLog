package com.ticklog.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The single composable that every screen is wrapped in.
 *
 * It wires together the four pillars of the design system — colour, typography,
 * shape and spacing — and exposes them through [MaterialTheme] plus the custom
 * [TickLogTheme] accessor. The colour schemes are intentionally hand-built (not
 * derived from a seed colour) so the palette stays strictly monochrome in both
 * light and dark modes.
 */

// --- Light colour scheme -----------------------------------------------------
private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Grey900,
    onPrimaryContainer = White,

    secondary = Grey700,
    onSecondary = White,
    secondaryContainer = Grey100,
    onSecondaryContainer = Black,

    tertiary = Black,
    onTertiary = White,
    tertiaryContainer = Grey100,
    onTertiaryContainer = Black,

    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey600,

    surfaceContainerLowest = White,
    surfaceContainerLow = Grey50,
    surfaceContainer = Grey100,
    surfaceContainerHigh = Grey200,
    surfaceContainerHighest = Grey300,

    inverseSurface = Black,
    inverseOnSurface = White,
    inversePrimary = White,

    outline = Grey300,
    outlineVariant = Grey200,

    error = Black,
    onError = White,
    errorContainer = Grey200,
    onErrorContainer = Black,

    scrim = Black,
)

// --- Dark colour scheme ------------------------------------------------------
private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Grey200,
    onPrimaryContainer = Black,

    secondary = Grey300,
    onSecondary = Black,
    secondaryContainer = Grey800,
    onSecondaryContainer = White,

    tertiary = White,
    onTertiary = Black,
    tertiaryContainer = Grey800,
    onTertiaryContainer = White,

    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey400,

    surfaceContainerLowest = Black,
    surfaceContainerLow = Grey900,
    surfaceContainer = Grey800,
    surfaceContainerHigh = Grey700,
    surfaceContainerHighest = Grey600,

    inverseSurface = White,
    inverseOnSurface = Black,
    inversePrimary = Black,

    outline = Grey600,
    outlineVariant = Grey700,

    error = White,
    onError = Black,
    errorContainer = Grey700,
    onErrorContainer = White,

    scrim = Black,
)

/**
 * Applies the TickLog design system to [content].
 *
 * @param darkTheme whether to use the dark colour scheme. Hoisted as a parameter
 *   (rather than read from the system here) so the caller can honour the user's
 *   explicit theme preference from settings.
 */
@Composable
fun TickLogTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TickLogTypography,
            shapes = TickLogShapes,
            content = content,
        )
    }
}

/**
 * Convenience accessor for design-system tokens that Material 3 does not model
 * natively (currently: spacing). Usage: `TickLogTheme.spacing.large`.
 */
object TickLogTheme {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}
