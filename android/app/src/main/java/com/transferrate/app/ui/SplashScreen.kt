package com.transferrate.app.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.transferrate.app.R
import kotlinx.coroutines.delay

/**
 * Branded Transfer Rate splash that runs AFTER the OS native splash and
 * BEFORE the rates list. Holds for at least [minDurationMs] so the
 * brand animation registers, then dismisses when the caller's data is
 * ready.
 *
 * Composition ("infinity DXR" brand refresh, res/raw/splash.gif):
 *   - Full-bleed playback of the animated brand reveal — the teal
 *     infinity money-flow loop draws in, then the "Transfer Rate"
 *     wordmark and "Compare. Choose. Save." tagline appear, settling
 *     on a Deep Navy backdrop. The GIF loops forever by default
 *     (NETSCAPE2.0 loop=0), so we force play-once and hold the final
 *     frame — which already matches the desired static splash state.
 *   - [minDurationMs] defaults to the GIF's ~3.1s runtime so the
 *     animation completes before the rates list can take over.
 *   - Subtle loading indicator near the bottom, overlaid on the GIF's
 *     final frame.
 *   - Whole composition fades in over 400ms for a softer transition
 *     from the OS splash.
 */
@Composable
fun SplashScreen(
    minDurationMs: Long = 3100L,
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
            .background(MaterialTheme.colorScheme.background)
            .alpha(alpha),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val source = ImageDecoder.createSource(ctx.resources, R.raw.splash)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = 0 // play once, then hold the final frame
                        drawable.start()
                    }
                }
            },
        )

        CircularProgressIndicator(
            modifier = Modifier
                .padding(bottom = 64.dp)
                .size(28.dp),
            color = Color(0xFF14BBA6), // brand teal (infinity DXR refresh)
            strokeWidth = 2.5.dp,
        )
    }
}

/**
 * In-app Transfer Rate logo. Renders the high-resolution PNG mark
 * (res/drawable-nodpi/transfer_rate_logo.png) — the "infinity DXR"
 * symbol (teal money-flow loop + white AED/INR glyphs) extracted from
 * the brand source.
 *
 * Wrapped in a Deep Navy (#071827) circular badge, matching the
 * Adaptive Icon Background and the "Dark Mode Avatar" treatment in
 * artifacts/Logo System.png — the symbol's AED/INR glyphs are white,
 * so they need a dark backing for contrast on both the light splash
 * bg (#F6F9FC, visible navy badge per Splash White.png) and the dark
 * splash bg (#081324, the navy badge nearly disappears into it,
 * letting the symbol float per Splash Dark.png). One fixed badge
 * colour keeps the brand mark consistent regardless of app theme.
 *
 * Used by AboutScreen (and the toolbar).
 */
@Composable
fun TransferRateLogo(size: androidx.compose.ui.unit.Dp = 96.dp) {
    val padding = (size.value * 0.06f).dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF071827)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.transfer_rate_logo),
            contentDescription = "Transfer Rate",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .padding(padding),
        )
    }
}

/** Backwards-compat alias for the v0.11/v0.13 names. */
@Composable
@Deprecated(
    "Renamed to TransferRateLogo for the v0.17 brand refresh.",
    replaceWith = ReplaceWith("TransferRateLogo(size)"),
)
fun ExchangiaLogo(size: androidx.compose.ui.unit.Dp = 96.dp) = TransferRateLogo(size)

@Composable
@Deprecated(
    "Renamed to TransferRateLogo for the v0.17 brand refresh.",
    replaceWith = ReplaceWith("TransferRateLogo(size)"),
)
fun BarsLogo(size: androidx.compose.ui.unit.Dp = 96.dp) = TransferRateLogo(size)
