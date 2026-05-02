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
                text = "Mid-market reference + 28 providers",
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
 * The same three-bars logo as the launcher icon, drawn in Canvas so it
 * can sit at any size in the splash without resource scaling artefacts.
 *
 * Bars sit on a shared baseline; tops rounded; ascending heights from
 * left to right at proportions matching the launcher icon (28/44/60
 * fractional heights of the safe-zone vertical extent).
 */
@Composable
fun BarsLogo(size: androidx.compose.ui.unit.Dp = 96.dp) {
    val white = Color.White
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        // Bars occupy the central 90% of the canvas
        val bottom = h * 0.93f
        val top1 = h * 0.55f   // shortest
        val top2 = h * 0.36f   // medium
        val top3 = h * 0.18f   // tallest
        val barWidth = w * 0.18f
        val gap = (w - barWidth * 3) / 4f
        val r = barWidth / 2f

        listOf(
            (gap + barWidth * 0) to top1,
            (gap * 2 + barWidth * 1) to top2,
            (gap * 3 + barWidth * 2) to top3,
        ).forEach { (x, top) ->
            // Body of the bar (rectangle)
            drawRoundRect(
                color = white,
                topLeft = Offset(x, top),
                size = Size(barWidth, bottom - top),
                cornerRadius = CornerRadius(r, r),
            )
            // Cover the bottom corners (we only want the top rounded;
            // the round-rect would round all four corners equally)
            drawRect(
                color = white,
                topLeft = Offset(x, top + r),
                size = Size(barWidth, bottom - top - r),
            )
        }
    }
}
