package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generated provider avatar — colored circle with the provider's initials.
 *
 * Why generated rather than provider-supplied logos:
 *   * No trademark exposure. We never claim affiliation with the providers.
 *   * Deterministic and offline. The avatar is the same across devices,
 *     never broken by a CDN outage, never different shape than expected.
 *   * Visually consistent. All providers feel equally "weighted" in the
 *     comparison list, which is the right framing for an aggregator.
 *
 * The color is derived deterministically from the provider_id slug, so
 * each provider gets a stable, distinct color across runs and devices.
 */

private val AVATAR_PALETTE = listOf(
    Color(0xFF1E88E5), // blue
    Color(0xFF8E24AA), // purple
    Color(0xFFD81B60), // pink
    Color(0xFFE53935), // red
    Color(0xFFF4511E), // deep orange
    Color(0xFFF9A825), // amber
    Color(0xFF00897B), // teal
    Color(0xFF43A047), // green
    Color(0xFF3949AB), // indigo
    Color(0xFF6D4C41), // brown
    Color(0xFF546E7A), // blue-grey
    Color(0xFF00ACC1), // cyan
)

/** Stable hash → palette index. */
private fun colorFor(providerId: String): Color {
    val h = providerId.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7FFFFFFF }
    return AVATAR_PALETTE[h % AVATAR_PALETTE.size]
}

/** Pull two letters that read as initials. "Al Ansari Exchange" -> "AA". */
private fun initialsFor(displayName: String): String {
    val words = displayName.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() && it[0].isLetterOrDigit() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words[0].first()}${words[1].first()}".uppercase()
    }
}

@Composable
fun ProviderAvatar(
    providerId: String,
    displayName: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val base = colorFor(providerId)
    val brush = Brush.linearGradient(
        colors = listOf(
            base,
            base.copy(red = (base.red * 0.78f).coerceIn(0f, 1f),
                      green = (base.green * 0.78f).coerceIn(0f, 1f),
                      blue = (base.blue * 0.78f).coerceIn(0f, 1f)),
        ),
    )
    val initials = initialsFor(displayName)
    val fontSize = (size.value * 0.40f).sp

    Box(
        modifier = modifier
            .size(size)
            .background(brush, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
        )
    }
}
