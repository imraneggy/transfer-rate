package com.transferrate.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.R
import kotlinx.coroutines.delay

/**
 * Branded Transfer Rate splash that runs AFTER the OS native splash and
 * BEFORE the rates list. Holds for at least [minDurationMs] so the
 * brand registers, then dismisses when the caller's data is ready.
 *
 * Composition ("infinity DXR" brand refresh, artifacts/Splash Dark.png
 * + Splash White.png):
 *   - Backdrop matches the OS splash (visual continuity)
 *   - The Transfer Rate brand mark: teal infinity money-flow loop with
 *     white AED/INR glyphs, on a Deep Navy badge
 *   - "Transfer Rate" wordmark in Space Grotesk Bold, with "Rate" in
 *     brand teal
 *   - Tagline: "Compare. Choose. Save."
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
                text = buildAnnotatedString {
                    append("Transfer ")
                    withStyle(SpanStyle(color = Color(0xFF14BBA6))) {
                        append("Rate")
                    }
                },
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
                color = Color(0xFF14BBA6), // brand teal (infinity DXR refresh)
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
 * Reused by SplashScreen and AboutScreen.
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
