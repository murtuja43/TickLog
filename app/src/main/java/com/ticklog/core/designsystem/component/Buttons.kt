package com.ticklog.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Reusable button components.
 *
 * Both buttons share a single internal content layout and the same subtle
 * press animation so their sizing, shape and optional leading icon stay
 * perfectly consistent. Only the Material 3 container they delegate to differs
 * (filled vs. outlined), which is exactly the high-level distinction a caller
 * cares about.
 */

private val ButtonMinHeight = 56.dp
private val ButtonIconSize = 20.dp

/**
 * The primary call-to-action: a high-emphasis filled button. Use at most one per
 * screen region (e.g. "Continue" in onboarding).
 *
 * @param text label shown on the button.
 * @param onClick invoked on tap; ignored while [enabled] is false.
 * @param modifier external layout modifier.
 * @param enabled whether the button is interactive.
 * @param leadingIcon optional icon rendered before the label.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by pressScale(interactionSource)

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        interactionSource = interactionSource,
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon)
    }
}

/**
 * The secondary, lower-emphasis action: an outlined button used alongside or
 * beneath a [PrimaryButton] (e.g. "Cancel").
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by pressScale(interactionSource)

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        interactionSource = interactionSource,
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon)
    }
}

/** Shared row of optional icon + label used by both button variants. */
@Composable
private fun ButtonContent(text: String, leadingIcon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null, // decorative; the label conveys meaning
                modifier = Modifier.size(ButtonIconSize),
            )
        }
        Text(text = text)
    }
}

/**
 * A small "squish" animation: the button scales to 96% while pressed and springs
 * back on release, giving every tap a tactile, premium feel.
 */
@Composable
private fun pressScale(interactionSource: MutableInteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "buttonPressScale",
    )
}
