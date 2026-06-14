package com.transferrate.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.R

/**
 * Provider avatar — official logo when bundled, otherwise initials.
 *
 * We bundle small (~144x144) favicon-derived PNGs for known providers in
 * res/drawable-nodpi/logo_<provider_id>.png. When the resource exists,
 * we render it on a white circle; otherwise we fall back to the
 * deterministic colored-initials avatar.
 *
 * Logos are bundled at BUILD TIME from publicly-accessible favicons, so
 * there is no runtime network call to provider domains — the privacy
 * guarantee (only fetch from GitHub Pages) is preserved.
 *
 * Trademark note: provider names and logos are trademarks of their
 * respective owners. They are used here for nominative identification
 * in a comparison context, not branding or endorsement.
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

/**
 * Look up the bundled logo drawable resource id for a provider, or 0 if none.
 *
 * Resource resolution by name lets us add logos by simply dropping a new PNG
 * into res/drawable-nodpi/ as logo_<provider_id>.png — no code change needed.
 */
@Composable
private fun logoResIdFor(providerId: String): Int {
    val ctx = LocalContext.current
    val name = "logo_${providerId}"
    return ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
}

@Composable
fun ProviderAvatar(
    providerId: String,
    displayName: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    // The mid-market "provider" is us — show the Transfer Rate brand mark
    // rather than falling through to the initials fallback (which produces
    // a generic "MR" colored chip indistinguishable from a third-party
    // provider's avatar).  Routed through the same white-circle treatment
    // as a real provider logo so the visual rhythm of the rates list and
    // history sheets stays consistent.
    //
    // IMPORTANT: use `transfer_rate_logo` (a direct PNG in drawable-nodpi)
    // not `ic_splash` (which is a <bitmap> XML wrapper around a mipmap).
    // Compose's painterResource() throws IllegalArgumentException on
    // <bitmap> XML drawables — only vector drawables and direct raster
    // assets (PNG/JPG/WEBP) are accepted.
    val effectiveLogoRes = if (providerId == "mid_market") {
        R.drawable.transfer_rate_logo
    } else {
        logoResIdFor(providerId)
    }

    if (effectiveLogoRes != 0) {
        // Bundled logo: render on a near-white circle (logos are
        // typically designed to read on white). Apply a subtle
        // theme-aware ring so the white doesn't look harsh on dark
        // backgrounds and the avatar feels intentional rather than
        // floating.
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val containerColor = if (isDark) Color(0xFFEDEDED) else Color(0xFFFFFFFF)
        val ringColor = if (isDark) {
            Color.White.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        }
        Box(
            modifier = modifier
                .size(size)
                .background(containerColor, CircleShape)
                .border(width = 0.75.dp, color = ringColor, shape = CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = effectiveLogoRes),
                contentDescription = "$displayName logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .padding(size * 0.06f),
            )
        }
        return
    }

    // Fallback: generated colored circle with initials.
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
