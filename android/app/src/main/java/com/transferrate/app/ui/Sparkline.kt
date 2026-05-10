package com.transferrate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact line chart for showing rate movement over time.
 *
 * Auto-scales to the local min/max of the data so small movements are
 * still visible.  Renders nothing if there's only one data point.
 *
 * **v0.29.6: caption-row labels.** Earlier versions tried to label
 * min/max/last directly on the curve, but the on-canvas labels overlap
 * the line, clip on edges, and read like accidents.  The professional
 * pattern (Robinhood, Bloomberg, TradingView's compact mode) is a
 * small text row below the chart with high / low / now values.  This
 * keeps the chart shape clean and makes the values explicitly readable
 * as a summary, not a guess-from-position.
 *
 * When `showLabels = true`, the composable renders:
 *
 *     <Canvas with line + fill + last-point dot>
 *     <Row: "↑ 25.84   ↓ 25.71   • 25.78">
 *
 * Caller passes a formatter so the same Sparkline composable can render
 * "25.78", "₹ 7,950", "AED 312", etc.
 *
 * @param values    ordered list of rates (oldest first)
 * @param color     line color; pass theme-aware color from caller
 * @param modifier  sets size — typically `Modifier.fillMaxWidth().height(40.dp)`
 *                  for a label-less chart, or 64.dp+ when [showLabels]=true
 *                  so the caption row has room
 * @param showLabels render high/low/now caption row below the chart
 * @param formatter  how to format each numeric value (default `%.4f`)
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

    if (!showLabels) {
        // Just the chart — no caption.
        SparklineCanvas(values = values, color = color, modifier = modifier)
        return
    }

    // With labels: stack the canvas on top of a small caption row.  The
    // canvas takes most of the height (chart needs room to read); the
    // caption is a fixed 18 dp.
    Column(modifier = modifier) {
        SparklineCanvas(
            values = values,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Spacer(Modifier.height(2.dp))
        SparklineCaption(
            high = values.max(),
            low = values.min(),
            last = values.last(),
            color = color,
            formatter = formatter,
        )
    }
}


/**
 * The actual line + fill + dot.  Pulled out so the wrapper can place
 * a caption below without making the canvas have to know about it.
 */
@Composable
private fun SparklineCanvas(
    values: List<Double>,
    color: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(0.0001)

        // Inset so the line doesn't kiss the edges and the highlight
        // dot has room to live without clipping.
        val padTop = 6f
        val padBottom = 4f
        val plotH = (h - padTop - padBottom).coerceAtLeast(1f)

        val linePath = Path()
        values.forEachIndexed { i, v ->
            val x = i.toFloat() / (values.size - 1) * w
            val y = padTop + plotH - ((v - min) / range * plotH).toFloat()
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
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

        // Highlight dot at "now"
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


/**
 * One-line summary row showing high / low / now under the sparkline.
 *
 * Format: "↑ 25.8400   ↓ 25.7100   • 25.7800"
 *
 * Spaced evenly across the row width so values align across stacked
 * sparklines (e.g. when 24K and 22K trends sit one above the other,
 * the digits line up vertically).
 */
@Composable
private fun SparklineCaption(
    high: Double,
    low: Double,
    last: Double,
    color: Color,
    formatter: (Double) -> String,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CaptionStat(label = "↑", value = formatter(high), labelColor = muted)
        CaptionStat(label = "↓", value = formatter(low), labelColor = muted)
        CaptionStat(label = "•", value = formatter(last), labelColor = color)
    }
}


@Composable
private fun CaptionStat(label: String, value: String, labelColor: Color) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = labelColor,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = "tnum",
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
