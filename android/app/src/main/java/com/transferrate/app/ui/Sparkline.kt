package com.transferrate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Compact line chart for showing rate movement over time.
 *
 * Auto-scales to the local min/max of the data so small movements are
 * still visible. Renders nothing if there's only one data point (a
 * single dot would be visually noisy).
 *
 * v0.25 visual upgrade:
 *   * Stroke width bumped 1.8 -> 3.5 px so the line reads as a chart,
 *     not a hairline. Looks correct at projection size and on
 *     high-DPI displays without losing crispness on phones.
 *   * Soft area-fill gradient beneath the line — premium feel,
 *     matches Stripe / Linear chart conventions.  Gradient fades from
 *     ~25% alpha at the line down to 0 at the bottom edge so the fill
 *     never competes with text rendered over it.
 *   * Highlight dot grown 2.5 -> 4.0 px with a soft outer ring for
 *     senior-management-readable emphasis on "where we are now".
 *   * Round corners on path joins so any sharp inflection still reads
 *     smoothly (matters for 30-day series with occasional spikes).
 *
 * @param values   ordered list of rates (oldest first)
 * @param color    line color; pass theme-aware color from caller
 * @param modifier sets size — typically Modifier.fillMaxWidth().height(40.dp)
 */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier.height(40.dp),
) {
    if (values.size < 2) return

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val min = values.min()
        val max = values.max()
        // Avoid divide-by-zero on flat lines
        val range = (max - min).coerceAtLeast(0.0001)

        // Inset so the line doesn't kiss the edges and the highlight
        // dot has room to live without clipping.  6 dp top + 4 dp
        // bottom — top has more room because the dot lives there
        // when rates are climbing.
        val padTop = 6f
        val padBottom = 4f
        val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

        // Build the line path (oldest left -> newest right).
        val linePath = Path()
        values.forEachIndexed { i, v ->
            val x = i.toFloat() / (values.size - 1) * w
            val y = padTop + plotH - ((v - min) / range * plotH).toFloat()
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Build the area-fill path: same line, then drop down to the
        // bottom edge and close back to the start.
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Soft vertical gradient under the line — alpha from line
        // colour down to transparent at the bottom edge.
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.28f),
                    color.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                startY = padTop,
                endY = h,
            ),
        )

        // Main line — meaningfully thicker than the v0.13 hairline so
        // the chart stays legible on small phones and projection.
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(
                width = 3.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Highlight the most-recent point with a tiered dot:
        //   * outer halo (low alpha) — premium accent, easy on the eye
        //   * inner solid — locks the eye to "now"
        val lastX = w
        val lastY = padTop + plotH - ((values.last() - min) / range * plotH).toFloat()
        drawCircle(
            color = color.copy(alpha = 0.22f),
            radius = 7.0f,
            center = Offset(lastX, lastY),
        )
        drawCircle(
            color = color,
            radius = 4.0f,
            center = Offset(lastX, lastY),
        )
    }
}
