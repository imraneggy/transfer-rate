package com.transferrate.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.R
import kotlinx.coroutines.delay

/**
 * Branded Transfer Rate splash that runs AFTER the OS native splash and
 * BEFORE the rates list. Holds for at least [minDurationMs] so the
 * brand registers, then dismisses when the caller's data is ready.
 *
 * Composition (per brand brief 2026-05-06 — v0.17 refresh):
 *   - Soft-white backdrop matching the OS splash (visual continuity)
 *   - The Transfer Rate brand mark: navy "TR" with dual circular
 *     refresh arrows + bar chart + rupee currency cue
 *   - "Transfer Rate" wordmark in Space Grotesk Bold
 *   - Tagline: "Compare. Choose. Save more."
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

    // v0.32.0: text colour adapts to theme so the splash works on
    // both the light soft-white backdrop AND the OLED-true-black dark
    // backdrop.  Pre-v0.32 used a hardcoded #0A1F44 navy that became
    // invisible on the new pure-black dark splash bg.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textColor = if (isDark) Color(0xFFE0F1FF) else Color(0xFF0A1F44)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            TransferRateLogo(size = 128.dp)

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Transfer Rate",
                color = textColor,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                ),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(R.string.app_tagline),
                color = textColor.copy(alpha = 0.65f),
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(Modifier.height(44.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color(0xFF06B59C), // brand teal
                strokeWidth = 2.5.dp,
            )
        }
    }
}

/**
 * In-app Transfer Rate logo. Renders the high-resolution PNG mark
 * (res/drawable-nodpi/transfer_rate_logo.png) extracted from the brand
 * source — preserves the dual circular refresh arrows, mini bar chart
 * and ₹ currency cue that hand-coded vector paths cannot match.
 *
 * Reused by SplashScreen and AboutScreen.
 */
@Composable
fun TransferRateLogo(size: androidx.compose.ui.unit.Dp = 96.dp) {
    Image(
        painter = painterResource(id = R.drawable.transfer_rate_logo),
        contentDescription = "Transfer Rate",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size),
    )
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
