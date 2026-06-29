package com.ticklog.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Minimal, monochrome charts drawn entirely with Compose Canvas — no third-party
 * chart libraries. Each takes already-normalised values in [0f, 1f] so the
 * drawing code stays trivial and the domain owns the maths.
 */

/**
 * A simple vertical bar chart. Each value draws a faint full-height track with a
 * solid bar filled to its value, so even zero days remain visible.
 *
 * @param values normalised bar heights in [0f, 1f].
 * @param modifier external layout modifier (set a height).
 * @param barColor colour of the filled bars.
 * @param trackColor colour of the background tracks.
 */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.onSurface,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val count = values.size
        val gap = if (count > 1) size.width / (count * 6f) else 0f
        val barWidth = (size.width - gap * (count - 1)) / count
        val radius = CornerRadius(barWidth.coerceAtMost(8f) / 2f)

        values.forEachIndexed { index, raw ->
            val value = raw.coerceIn(0f, 1f)
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = radius,
            )
            val barHeight = size.height * value
            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/**
 * A simple line chart of normalised values, drawn as a single stroked path.
 *
 * @param values normalised points in [0f, 1f].
 * @param modifier external layout modifier (set a height).
 * @param lineColor colour of the trend line.
 */
@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val strokePx = 3f
        fun pointFor(index: Int): Offset {
            val x = if (values.size == 1) size.width / 2f else size.width * index / (values.size - 1)
            val y = size.height * (1f - values[index].coerceIn(0f, 1f))
            return Offset(x, y)
        }

        if (values.size == 1) {
            drawCircle(color = lineColor, radius = strokePx * 1.5f, center = pointFor(0))
            return@Canvas
        }

        val path = Path().apply {
            val first = pointFor(0)
            moveTo(first.x, first.y)
            for (index in 1 until values.size) {
                val point = pointFor(index)
                lineTo(point.x, point.y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
