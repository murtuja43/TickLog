package com.ticklog.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A monochrome circular progress indicator drawn with Compose Canvas.
 *
 * Used both as the small per-day indicator in the calendar and as a larger ring
 * in statistics. With [fillWhenComplete] a 100% ring is rendered as a solid
 * disc, giving the calendar its "filled circle / partial ring / empty ring"
 * language. The fill animates so progress changes feel alive.
 *
 * @param progress completion in [0f, 1f] (values are clamped).
 * @param modifier external layout modifier (set a size on it).
 * @param strokeWidth thickness of the ring.
 * @param trackColor colour of the unfilled track.
 * @param progressColor colour of the filled portion.
 * @param fillWhenComplete render a solid disc at 100% instead of a full ring.
 * @param content optional centre content (e.g. a day number).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    fillWhenComplete: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, label = "progressRing")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (fillWhenComplete && animated >= 1f) {
                drawCircle(color = progressColor, radius = size.minDimension / 2f)
            } else {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (animated > 0f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}
