package com.ticklog.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Constrains content to a comfortable reading width and centres it.
 *
 * On phones the content simply fills the width. On tablets and in landscape the
 * available width can be very large; stretching forms and text that wide hurts
 * readability and looks unpolished. This container caps the width and centres
 * the column, which is the single most effective tweak for premium-feeling
 * large-screen support. Screens wrap their primary column in this.
 *
 * @param modifier external layout modifier (applied to the outer, full-width box).
 * @param maxContentWidth the maximum width the inner content may occupy.
 * @param content the constrained content, scoped to the centred [BoxScope].
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    maxContentWidth: androidx.compose.ui.unit.Dp = 600.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxWidth(),
            content = content,
        )
    }
}
