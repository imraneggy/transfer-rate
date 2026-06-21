package com.transferrate.app.ui

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
 * Branded Transfer Rate splash — two phases:
 *
 *   Phase 1 (0 → gifDurationMs): "infinity DXR" GIF plays once and
 *     holds its final frame. A teal loading indicator sits near the
 *     bottom while data loads in the background.
 *
 *   Phase 2 (gifDurationMs → gifDurationMs + flashDurationMs): The
 *     brand flash card (drawable-nodpi/brand_flash.png — "Proud of UAE"
 *     card) cross-fades in over the GIF's final frame and is held for
 *     [flashDurationMs]. The loading indicator is hidden during this
 *     phase so the full card is unobstructed.
 *
 *   Dismiss: after both phases complete AND [isReady] is true, [onDone]
 *     is called and the rates screen takes over.
 */
@Composable
fun SplashScreen(
    minDurationMs: Long = 3100L,
    flashDurationMs: Long = 1800L,
    isReady: Boolean = false,
    onDone: () -> Unit,
) {
    // Phase 1 done when GIF has played through.
    var gifDone by remember { mutableStateOf(false) }
    // Phase 2 done when flash card has been held long enough.
    var flashDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(minDurationMs)
        gifDone = true
        delay(flashDurationMs)
        flashDone = true
    }
    LaunchedEffect(flashDone, isReady) {
        if (flashDone && isReady) onDone()
    }

    // Fade-in for the whole splash on first appearance.
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "splash-fade",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071827)) // Deep Navy — matches GIF + flash card bg
            .alpha(alpha),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Phase 1: GIF animation (always rendered so it plays immediately).
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    val source = ImageDecoder.createSource(ctx.resources, R.raw.splash)
                    val drawable = ImageDecoder.decodeDrawable(source)
                    setImageDrawable(drawable)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.repeatCount = 0 // play once, hold final frame
                        drawable.start()
                    }
                }
            },
        )

        // Phase 2: brand flash card cross-fades in over the GIF once it ends.
        AnimatedContent(
            targetState = gifDone,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "flash-card-transition",
        ) { showFlash ->
            if (showFlash) {
                Image(
                    painter = painterResource(id = R.drawable.brand_flash),
                    contentDescription = "Transfer Rate — Proud of UAE",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }

        // Loading indicator — visible only during Phase 1.
        if (!gifDone) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .size(28.dp),
                color = Color(0xFF14BBA6),
                strokeWidth = 2.5.dp,
            )
        }
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
