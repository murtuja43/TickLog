package com.ticklog.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * The complete TickLog colour palette.
 *
 * TickLog is deliberately monochrome: the brand is built from pure black, pure
 * white and a small ramp of greys used only for surfaces, outlines and
 * disabled states. There are no accent hues, gradients or tints anywhere in the
 * product. Keeping every literal colour in this one file means the palette can
 * be audited at a glance and never drifts.
 */

// --- Absolutes ---------------------------------------------------------------
internal val Black = Color(0xFF000000)
internal val White = Color(0xFFFFFFFF)

// --- Neutral ramp (light theme) ----------------------------------------------
internal val Grey50 = Color(0xFFF7F7F7)
internal val Grey100 = Color(0xFFF0F0F0)
internal val Grey200 = Color(0xFFE2E2E2)
internal val Grey300 = Color(0xFFCFCFCF)
internal val Grey400 = Color(0xFFAFAFAF)

// --- Neutral ramp (dark theme) -----------------------------------------------
internal val Grey500 = Color(0xFF8A8A8A)
internal val Grey600 = Color(0xFF5C5C5C)
internal val Grey700 = Color(0xFF3A3A3A)
internal val Grey800 = Color(0xFF222222)
internal val Grey900 = Color(0xFF121212)
