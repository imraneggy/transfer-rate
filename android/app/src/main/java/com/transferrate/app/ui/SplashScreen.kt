package com.transferrate.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Branded splash that runs AFTER the OS native splash and BEFORE the
 * rates list shows. Holds for at least [minDurationMs] so the brand
 * registers, then fades when the caller's data is ready.
 *
 * Design:
 *   - Same teal background as the OS splash (visual continuity)
 *   - The three-bar logo, drawn fresh in Canvas at a comfortable size
 *   - "Transfer Rate" wordmark
 *   - Tagline: "Compare AED -> INR rates"
 *   - Subtle loading indicator at the bottom
 *   - Whole composition fades in over 400ms for a softer transition
 *     from the OS splash
 *
 * The OS splash already showed the icon for ~200ms before this — so by
 * the time the user perceives the "splash", they've actually been seeing
 * the brand for ~1.2 seconds total. Plenty for recognition.
 */
@Composable
fun SplashScreen(
    minDurationMs: Long = 1000L,
    isReady: Boolean = false,
    onDone: () -> Unit,
) {
    var minElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minDurationMs)
        minElapsed = true
    }
    LaunchedEffect(minElapsed, isReady) {
        if (minElapsed && isReady) onDone()
    }

    // Fade-in alpha for the whole splash content
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "splash-fade",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            BarsLogo(size = 96.dp)

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Transfer Rate",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Live AED → INR rates",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Mid-market benchmark + 9 providers",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(40.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White.copy(alpha = 0.55f),
                strokeWidth = 2.5.dp,
            )
        }
    }
}

/**
 * The same chart-line logo as the launcher icon, drawn in Canvas so it
 * can sit at any size in the splash and about screen without resource
 * scaling artefacts. Mirrors res/drawable/ic_launcher_foreground.xml.
 *
 * Composition: a faint baseline band, a bold gold polyline rising
 * left-to-right with two mid markers and a larger highlighted final
 * data point — communicates "rates trending / latest reading."
 */
@Composable
fun BarsLogo(size: androidx.compose.ui.unit.Dp = 96.dp) {
    val gold = Color(0xFFFFD980)
    val baseline = Color.White.copy(alpha = 0.10f)
    val midMarker = gold.copy(alpha = 0.55f)
    val innerHighlight = Color.White.copy(alpha = 0.32f)

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Baseline band along the bottom (axis suggestion)
        drawRoundRect(
            color = baseline,
            topLeft = Offset(w * 0.20f, h * 0.76f),
            size = Size(w * 0.60f, h * 0.04f),
            cornerRadius = CornerRadius(h * 0.02f, h * 0.02f),
        )

        // Three line segments forming an ascending zigzag with rounded joins
        val p0 = Offset(w * 0.24f, h * 0.66f)
        val p1 = Offset(w * 0.42f, h * 0.52f)
        val p2 = Offset(w * 0.58f, h * 0.62f)
        val p3 = Offset(w * 0.79f, h * 0.30f)
        val strokeW = w * 0.060f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(color = gold, start = p0, end = p1, strokeWidth = strokeW, cap = cap)
        drawLine(color = gold, start = p1, end = p2, strokeWidth = strokeW, cap = cap)
        drawLine(color = gold, start = p2, end = p3, strokeWidth = strokeW, cap = cap)

        // Mid data-point markers
        drawCircle(color = midMarker, radius = w * 0.030f, center = p1)
        drawCircle(color = midMarker, radius = w * 0.030f, center = p2)

        // Final / "current" reading: large filled marker with inner highlight
        drawCircle(color = gold, radius = w * 0.066f, center = p3)
        drawCircle(color = innerHighlight, radius = w * 0.033f, center = p3)
    }
}
