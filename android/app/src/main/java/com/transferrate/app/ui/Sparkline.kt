package com.transferrate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Compact line chart for showing rate movement over time.
 *
 * Auto-scales to the local min/max of the data so small movements are
 * still visible. Renders nothing if there's only one data point (a
 * single dot would be visually noisy).
 *
 * @param values   ordered list of rates (oldest first)
 * @param color    line color; pass theme-aware color from caller
 * @param modifier sets size — typically Modifier.fillMaxWidth().height(28.dp)
 */
@Composable
fun Sparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier.height(28.dp),
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

        // 4dp inset top/bottom so the line doesn't kiss the edges
        val padY = 2f
        val plotH = (h - padY * 2).coerceAtLeast(1f)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i.toFloat() / (values.size - 1) * w
            val y = padY + plotH - ((v - min) / range * plotH).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.8f, cap = StrokeCap.Round),
        )

        // Highlight the most-recent point with a small dot
        val lastX = w
        val lastY = padY + plotH - ((values.last() - min) / range * plotH).toFloat()
        drawCircle(
            color = color,
            radius = 2.5f,
            center = Offset(lastX, lastY),
        )
    }
}
