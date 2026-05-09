package com.transferrate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact line chart for showing rate movement over time.
 *
 * Auto-scales to the local min/max of the data so small movements are
 * still visible. Renders nothing if there's only one data point.
 *
 * **v0.29.4: optional value labels.** When `showLabels = true`, three
 * numeric labels are drawn — at the **min** point (just below the
 * trough), the **max** point (just above the peak), and the **last**
 * point (next to the highlight dot, prefixed with the rate value).
 * Caller passes a formatter so the same Sparkline composable can render
 * "25.78", "₹ 7,950", "AED 312", etc.
 *
 * @param values    ordered list of rates (oldest first)
 * @param color     line color; pass theme-aware color from caller
 * @param modifier  sets size — typically `Modifier.fillMaxWidth().height(40.dp)`
 *                  (or 64.dp when [showLabels] is true so the labels have room)
 * @param showLabels render min/max/last value labels along the curve
 * @param formatter  how to format each numeric value (defaults to "%.4f")
 */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier.height(40.dp),
    showLabels: Boolean = false,
    formatter: (Double) -> String = { "%.4f".format(it) },
) {
    if (values.size < 2) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(0.0001)

        // Reserve more vertical room when labels are on so they don't
        // clip the chart edges or each other.
        val padTop = if (showLabels) 14f else 6f
        val padBottom = if (showLabels) 14f else 4f
        val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

        // Compute (x, y) for each point so we can reuse positions for
        // both the line path and the label placements.
        val points = values.mapIndexed { i, v ->
            val x = i.toFloat() / (values.size - 1) * w
            val y = padTop + plotH - ((v - min) / range * plotH).toFloat()
            Offset(x, y)
        }

        // ---- Line path ---------------------------------------------------
        val linePath = Path().apply {
            points.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

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

        drawPath(
            path = linePath,
            color = color,
            style = Stroke(
                width = 3.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // ---- Highlight dot at "now" --------------------------------------
        val last = points.last()
        drawCircle(color.copy(alpha = 0.22f), radius = 7.0f, center = last)
        drawCircle(color, radius = 4.0f, center = last)

        // ---- Value labels (v0.29.4) --------------------------------------
        if (!showLabels) return@Canvas

        // Find the first occurrence of the min and max so labels land
        // on a real data point (matters when the series has multiple
        // tied extrema — e.g. a flat segment that hits the max twice).
        val minIdx = values.indexOf(min)
        val maxIdx = values.indexOf(max)
        val lastIdx = values.lastIndex

        // Draw a label at a point.  Side ("above" / "below") is chosen
        // by the caller because peaks want labels above the curve and
        // troughs want them below; otherwise the label sits on top of
        // the chart line.
        fun drawLabelAt(idx: Int, side: String) {
            val p = points[idx]
            val text = formatter(values[idx])
            val layout = textMeasurer.measure(
                text = AnnotatedString(text),
                style = labelStyle,
            )
            val tw = layout.size.width.toFloat()
            val th = layout.size.height.toFloat()
            // Horizontal: clamp so the label doesn't run past the canvas
            // (matters for max/min landing at index 0 or lastIdx).
            val labelX = (p.x - tw / 2f).coerceIn(0f, w - tw)
            val labelY = if (side == "above") {
                (p.y - th - 2f).coerceAtLeast(0f)
            } else {
                (p.y + 2f).coerceAtMost(h - th)
            }
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(labelX, labelY),
            )
        }

        // Special-case: if min and max are both extremes of a tiny range
        // (essentially flat), don't double-label — one is enough.
        val flat = (max - min) < (max * 0.0005)  // <0.05% range = "flat"
        if (!flat) {
            drawLabelAt(maxIdx, side = "above")
            drawLabelAt(minIdx, side = "below")
        }
        // Last value always rendered; if it coincides with min or max
        // index we'd draw twice, but the second draw is just a redraw
        // of the same text in the same place — visually invisible.
        if (lastIdx != minIdx && lastIdx != maxIdx) {
            drawLabelAt(lastIdx, side = "above")
        }
    }
}
