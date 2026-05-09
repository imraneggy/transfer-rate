package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.data.CURRENCIES
import com.transferrate.app.data.HistoryPoint
import com.transferrate.app.data.ProviderQuote
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Bottom sheet showing the last 10 days of rate history for a provider.
 *
 * Replaces the old "tap → open external website" intent. Opens in-app so
 * users can compare history without context-switching to a browser. The
 * provider's website link is preserved as a small secondary action at the
 * bottom of the sheet for users who still want to visit.
 *
 * Data source: HistoryDocument loaded once per refresh from
 * /history.json on GitHub Pages. We never call provider websites at
 * runtime — privacy guarantee preserved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderHistorySheet(
    provider: ProviderQuote,
    history: List<HistoryPoint>,
    midMarket: Double?,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val symbol = CURRENCIES[provider.quote]?.symbol ?: ""

    // Sort newest-first and cap at 10 days. The history file is already
    // 10-day-pruned by the scraper, but extra defense is cheap.
    val sortedHistory = remember(history) {
        history.sortedByDescending { it.t }.take(40) // up to 10 days × ~4 fetches/day
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            // Header: avatar + provider name + current rate
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProviderAvatar(
                    providerId = provider.providerId,
                    displayName = provider.providerName,
                    size = 48.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        provider.providerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Rate history (last 10 days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val current = provider.effectiveRate ?: provider.rate
                if (current != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.4f".format(current),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                            ),
                        )
                        Text(
                            "$symbol per AED",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // Sparkline of full history at top (if we have enough points)
            if (sortedHistory.size >= 2) {
                Sparkline(
                    values = sortedHistory.reversed().map { it.rate },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(4.dp))
            }

            // Stats: highest / lowest / average over the displayed window
            if (sortedHistory.isNotEmpty()) {
                val rates = sortedHistory.map { it.rate }
                val high = rates.max()
                val low = rates.min()
                val avg = rates.average()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatPill("High", "%.4f".format(high))
                    StatPill("Low",  "%.4f".format(low))
                    StatPill("Avg",  "%.4f".format(avg))
                }
                Spacer(Modifier.height(12.dp))
            }

            // History entries: date + time + rate + delta vs previous
            if (sortedHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No history yet for this provider — check back tomorrow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    "Date · Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val pairs = sortedHistory.zipWithNext()
                    items(sortedHistory) { entry ->
                        val prev = pairs.firstOrNull { it.first === entry }?.second
                        HistoryRow(
                            point = entry,
                            previous = prev,
                            midMarket = midMarket,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Secondary action: still allow opening the provider's site
            provider.url?.let { url ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            runCatching {
                                ctx.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url),
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = "Visit ${provider.providerName} ↗",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    point: HistoryPoint,
    previous: HistoryPoint?,
    midMarket: Double?,
) {
    val (date, time) = formatDateTime(point.t)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val positive = if (isDark) Color(0xFF6FDBA0) else Color(0xFF1B7B33)
    val negative = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)

    val deltaText: String?
    val deltaColor: Color
    if (previous != null) {
        val d = point.rate - previous.rate
        when {
            d > 0.0001 -> {
                deltaText = "+%.4f".format(d)
                deltaColor = positive
            }
            d < -0.0001 -> {
                deltaText = "%.4f".format(d)
                deltaColor = negative
            }
            else -> {
                deltaText = "—"
                deltaColor = MaterialTheme.colorScheme.outline
            }
        }
    } else {
        deltaText = null
        deltaColor = MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                date,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (deltaText != null) {
            Text(
                text = deltaText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum",
                ),
                color = deltaColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Text(
            text = "%.4f".format(point.rate),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = "tnum",
            ),
        )
    }
}

/** Converts ISO8601 UTC timestamp to (e.g. "Fri, 02 May", "16:42 UTC"). */
private fun formatDateTime(iso: String): Pair<String, String> {
    return try {
        val instant = Instant.parse(iso)
        val zoned = instant.atZone(ZoneId.of("UTC"))
        val date = zoned.format(
            DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.ENGLISH),
        )
        val time = zoned.format(
            DateTimeFormatter.ofPattern("HH:mm 'UTC'", Locale.ENGLISH),
        )
        date to time
    } catch (_: DateTimeParseException) {
        iso to ""
    }
}
