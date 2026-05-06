package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.data.CurrencyInfo
import com.transferrate.app.data.TodaysBestSummary
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Compact banner pinned above the rates list. Shows two complementary
 * signals so users get both the immediate decision (who's best NOW) and
 * the patience signal (did rates dip from today's high — wait, or send?).
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────────┐
 *   │ ⭐  Best now           25.91  (subtle navy gradient)     │
 *   │     Al Ansari · live                                     │
 *   │ ─────────────────────────                                │
 *   │ ⬆  Today's high          25.97 · 3h ago                  │
 *   │     0.06 below the high                                  │
 *   └─────────────────────────────────────────────────────────┘
 *
 * Renders nothing if [summary] has no current best (cold start before
 * data arrives). Falls back to single-line presentation when history is
 * unavailable.
 */
@Composable
fun TodayBestBanner(
    summary: TodaysBestSummary,
    currencyInfo: CurrencyInfo,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now(),
) {
    val current = summary.currentBest ?: return

    val brandTeal = Color(0xFF1FA89C)
    val brandNavy = Color(0xFF0A1F44)
    val isAtPeak = summary.isAtPeak

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = brandTeal.copy(alpha = if (isAtPeak) 0.7f else 0.35f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // ---- Row 1: Best now ----
            BannerRow(
                glyph = "⭐",
                glyphTint = brandTeal,
                label = "Best now",
                value = formatRate(current.rate, currencyInfo),
                subtitle = "${current.providerName} · live",
                emphasis = true,
            )

            // ---- Row 2: Today's high (if we have history for today) ----
            val peak = summary.todaysPeak
            if (peak != null) {
                Spacer(Modifier.height(10.dp))
                Divider(color = brandTeal.copy(alpha = 0.12f))
                Spacer(Modifier.height(10.dp))

                val message = peakStatusMessage(summary, now = now)
                BannerRow(
                    glyph = if (isAtPeak) "🎯" else "⬆",
                    glyphTint = if (isAtPeak) brandTeal else brandNavy,
                    label = if (isAtPeak) "At today's high" else "Today's high",
                    value = formatRate(peak.rate, currencyInfo),
                    subtitle = message,
                    emphasis = false,
                )
            }
        }
    }
}

/** A single label/value/subtitle row inside the banner. */
@Composable
private fun BannerRow(
    glyph: String,
    glyphTint: Color,
    label: String,
    value: String,
    subtitle: String,
    emphasis: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = glyph,
            color = glyphTint,
            fontSize = if (emphasis) 18.sp else 16.sp,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (emphasis) 22.sp else 16.sp,
            style = if (emphasis) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun Divider(color: Color) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .clip(RoundedCornerShape(0.5.dp))
            .background(color),
    )
}

/* ------------------------------------------------------------------ */
/*  Pure formatters — kept private so they don't leak as public API.   */
/* ------------------------------------------------------------------ */

private fun formatRate(rate: Double, info: CurrencyInfo): String {
    // Two decimal places matches the precision shown on provider cards.
    return String.format(Locale.US, "%s %.2f", info.symbol, rate)
}

/**
 * Subtitle copy for the "today's high" row.
 *
 * Three meaningful states:
 *   1. We're at-or-above today's peak — celebratory.
 *   2. We're below by a small/large amount — show the delta + how long
 *      ago the peak happened.
 *   3. Edge case: peak observed in the future (clock skew) — fall back
 *      to a neutral phrasing.
 *
 * Examples:
 *   "0.06 below — peak at 11:24 (3h ago)"
 *   "rates have come down 0.12 since 09:10 (6h ago)"
 *   "matched today's high 🎯"
 */
private fun peakStatusMessage(summary: TodaysBestSummary, now: Instant): String {
    val current = summary.currentBest ?: return ""
    val peak = summary.todaysPeak ?: return ""
    val delta = (peak.rate - current.rate).coerceAtLeast(0.0)

    val zone = ZoneId.systemDefault()
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val peakClock = timeFmt.format(peak.observedAt)
    val sinceLabel = relativeShort(peak.observedAt, now)

    return if (summary.isAtPeak) {
        "matched today's high"
    } else if (delta < 0.005) {
        "within 0.01 of the day's high"
    } else {
        "${"%.2f".format(delta)} below — peak at $peakClock ($sinceLabel)"
    }
}

/** "3h ago", "12m ago", "just now" — short form for tight banner space. */
private fun relativeShort(then: Instant, now: Instant): String {
    val secs = Duration.between(then, now).seconds
    return when {
        secs < 0 -> "just now"
        secs < 60 -> "just now"
        secs < 3_600 -> "${secs / 60}m ago"
        secs < 86_400 -> "${secs / 3_600}h ago"
        else -> "${secs / 86_400}d ago"
    }
}
