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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.R
import kotlinx.coroutines.delay

/**
 * Branded Exchangia splash that runs AFTER the OS native splash and
 * BEFORE the rates list. Holds for at least [minDurationMs] so the
 * brand registers, then dismisses when the caller's data is ready.
 *
 * Composition (per brand brief 2026-05-02):
 *   - Deep navy backdrop matching the OS splash (visual continuity)
 *   - The Exchangia "E" logo with INR + AED currency cues
 *   - "Exchangia" wordmark in Manrope ExtraBold
 *   - Tagline: "Compare INR rates across UAE"
 *   - Subtle loading indicator at the bottom
 *   - Whole composition fades in over 400ms for a softer transition
 *     from the OS splash
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

    val white = Color.White

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
            ExchangiaLogo(size = 104.dp)

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Exchangia",
                color = white,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                ),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(R.string.app_tagline),
                color = white.copy(alpha = 0.78f),
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(Modifier.height(44.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = white.copy(alpha = 0.55f),
                strokeWidth = 2.5.dp,
            )
        }
    }
}

/**
 * Compose-drawn version of the Exchangia "E" logo with INR + AED currency
 * cues. Mirrors the geometry of res/drawable/ic_launcher_foreground.xml so
 * the OS splash icon and the in-app splash logo are visually identical.
 *
 * Reused by SplashScreen and AboutScreen. Drawn in Canvas (not as a
 * resource Image) so the size and stroke widths scale precisely without
 * resource-density fuzz.
 */
@Composable
fun ExchangiaLogo(size: androidx.compose.ui.unit.Dp = 96.dp) {
    val teal = Color(0xFF00B49E)         // brand primary
    val cyan = Color(0xFF9DEAD0)         // INR cue
    val gold = Color(0xFFF4B940)         // AED cue
    val highlight = Color.White.copy(alpha = 0.45f)

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Coordinates inside a 108x108 logical canvas (matching the
        // launcher vector drawable). Multiply by w/108 and h/108 to map
        // to pixels; w == h for square Canvas.
        fun fx(x: Float) = x * w / 108f
        fun fy(y: Float) = y * h / 108f

        val armCorner = CornerRadius(fx(5f), fy(5f))

        // E spine: rounded vertical bar, x:[28..38], y:[28..80]
        drawRoundRect(
            color = teal,
            topLeft = Offset(fx(28f), fy(28f)),
            size = Size(fx(10f), fy(52f)),
            cornerRadius = armCorner,
        )
        // Top arm: x:[33..68], y:[28..38] (overlaps spine slightly)
        drawRoundRect(
            color = teal,
            topLeft = Offset(fx(33f), fy(28f)),
            size = Size(fx(35f), fy(10f)),
            cornerRadius = armCorner,
        )
        // Middle arm: x:[33..58], y:[49..59]
        drawRoundRect(
            color = teal,
            topLeft = Offset(fx(33f), fy(49f)),
            size = Size(fx(25f), fy(10f)),
            cornerRadius = armCorner,
        )
        // Bottom arm: x:[33..68], y:[70..80]
        drawRoundRect(
            color = teal,
            topLeft = Offset(fx(33f), fy(70f)),
            size = Size(fx(35f), fy(10f)),
            cornerRadius = armCorner,
        )

        // INR cue ball (cyan with white inner highlight)
        drawCircle(color = cyan, radius = fx(6f), center = Offset(fx(76f), fy(33f)))
        drawCircle(color = highlight, radius = fx(2.5f), center = Offset(fx(76f), fy(33f)))

        // AED cue ball (gold with white inner highlight)
        drawCircle(color = gold, radius = fx(6f), center = Offset(fx(76f), fy(75f)))
        drawCircle(color = highlight, radius = fx(2.5f), center = Offset(fx(76f), fy(75f)))
    }
}

/** Backwards-compat alias: BarsLogo() is referenced by AboutScreen. */
@Composable
@Deprecated("Renamed to ExchangiaLogo for the v0.11 brand refresh.",
    replaceWith = ReplaceWith("ExchangiaLogo(size)"))
fun BarsLogo(size: androidx.compose.ui.unit.Dp = 96.dp) = ExchangiaLogo(size)
