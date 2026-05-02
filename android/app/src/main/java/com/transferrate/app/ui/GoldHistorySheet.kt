package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.data.GoldDocument
import com.transferrate.app.data.GoldHistoryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * 30-day gold rate history sheet.
 *
 * Top: today's snapshot per carat × per weight (1g + 8g) for both
 * UAE and India. Below: a unified date table with both countries
 * side by side. Sparkline at the top of each side for quick reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldHistorySheet(
    gold: GoldDocument,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val uaeHistory = remember(gold) { gold.uae.history.sortedByDescending { it.date } }
    val indiaHistory = remember(gold) { gold.india.history.sortedByDescending { it.date } }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 22.sp, maxLines = 1)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Gold rate · UAE vs India",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        "Last 30 days, per gram",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(14.dp))

            // Today's snapshot — 4 prominent cells
            SnapshotGrid(gold = gold)

            Spacer(Modifier.height(16.dp))

            // Sparklines side by side (UAE 24K + India 24K)
            if (uaeHistory.size >= 2 || indiaHistory.size >= 2) {
                Text(
                    "24K trend (newest right)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SparkColumn(
                        label = "UAE",
                        unit = "AED",
                        values = uaeHistory.reversed().map { it.perG24k },
                        modifier = Modifier.weight(1f),
                    )
                    SparkColumn(
                        label = "India",
                        unit = "₹",
                        values = indiaHistory.reversed().map { it.perG24k },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // Unified date table: date | UAE 24K | UAE 22K | India 24K | India 22K
            Text(
                "Daily history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))

            // Header row
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Date", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.outline, maxLines = 1)
                Text("UAE 24K", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
                Text("IN 24K", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Build unified date set
            val allDates = remember(uaeHistory, indiaHistory) {
                (uaeHistory.map { it.date } + indiaHistory.map { it.date })
                    .toSortedSet(reverseOrder())
                    .toList()
                    .take(30)
            }
            val uaeMap = remember(uaeHistory) { uaeHistory.associateBy { it.date } }
            val inrMap = remember(indiaHistory) { indiaHistory.associateBy { it.date } }

            if (allDates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Building history — check back tomorrow.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                ) {
                    items(allDates) { date ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatShortDate(date),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.2f),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = uaeMap[date]?.let { "%.0f".format(it.perG24k) } ?: "—",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = inrMap[date]?.let { "%,.0f".format(it.perG24k) } ?: "—",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Sources: Khaleej Times (UAE) · BankBazaar (India). " +
                "Indicative; actual jeweller prices may differ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SnapshotGrid(gold: GoldDocument) {
    val u24 = gold.uae.perG24k
    val u22 = gold.uae.perG22k
    val i24 = gold.india.perG24k
    val i22 = gold.india.perG22k

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SnapshotCard(
            modifier = Modifier.weight(1f),
            country = "UAE",
            currencySym = "AED",
            r24 = u24,
            r22 = u22,
        )
        SnapshotCard(
            modifier = Modifier.weight(1f),
            country = "India",
            currencySym = "₹",
            r24 = i24,
            r22 = i22,
        )
    }
}

@Composable
private fun SnapshotCard(
    modifier: Modifier,
    country: String,
    currencySym: String,
    r24: Double?,
    r22: Double?,
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            country,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(8.dp))
        Text("24K · 1g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
        Text(
            text = if (r24 != null) "$currencySym ${formatGold(r24)}" else "—",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(6.dp))
        Text("24K · 8g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
        Text(
            text = if (r24 != null) "$currencySym ${formatGold(r24 * 8)}" else "—",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(10.dp))
        Text("22K · 1g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
        Text(
            text = if (r22 != null) "$currencySym ${formatGold(r22)}" else "—",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(2.dp))
        Text("22K · 8g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, softWrap = false)
        Text(
            text = if (r22 != null) "$currencySym ${formatGold(r22 * 8)}" else "—",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun SparkColumn(
    label: String,
    unit: String,
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            "$label  ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(2.dp))
        if (values.size >= 2) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val sparkColor =
                if (isDark) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.tertiary
            Sparkline(
                values = values,
                color = sparkColor.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth().height(40.dp),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Building…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private fun formatGold(value: Double): String =
    if (value >= 10000) "%,.0f".format(value)
    else "%,.0f".format(value)

private fun formatShortDate(iso: String): String =
    try {
        LocalDate.parse(iso).format(
            DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH),
        )
    } catch (_: DateTimeParseException) { iso }
