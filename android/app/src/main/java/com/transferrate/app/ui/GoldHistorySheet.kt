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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.transferrate.app.data.GoldDocument
import com.transferrate.app.ui.theme.LocalMetalColors
import com.transferrate.app.ui.theme.MetalColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Single dated rate observation, used as the unified shape across
 *  gold and silver so the sheet's stats / history-table code doesn't
 *  have to branch on whether the carat is gold-style or silver-style. */
private data class DatedRate(val date: String, val rate: Double)

/** Unified accessor: given a carat label, return (uaeRates, indiaRates)
 *  pre-sorted newest-first.  Silver has no UAE history (spot-only) so
 *  the UAE list is empty for "Ag".  Caller should handle empty lists
 *  gracefully (no stats row, "—" placeholders in tables). */
private fun ratesForCarat(carat: String, gold: GoldDocument): Pair<List<DatedRate>, List<DatedRate>> {
    val uaeGold = gold.uae.history.sortedByDescending { it.date }
    val indiaGold = gold.india.history.sortedByDescending { it.date }
    return when (carat) {
        "24K" -> uaeGold.map { DatedRate(it.date, it.perG24k) } to
                  indiaGold.map { DatedRate(it.date, it.perG24k) }
        "22K" -> uaeGold.map { DatedRate(it.date, it.perG22k) } to
                  indiaGold.map { DatedRate(it.date, it.perG22k) }
        "Ag"  -> emptyList<DatedRate>() to (gold.indiaSilver?.history.orEmpty()
                  .sortedByDescending { it.date }
                  .map { DatedRate(it.date, it.perG) })
        else -> emptyList<DatedRate>() to emptyList()
    }
}

/**
 * Gold & silver bottom sheet — UAE vs India.
 *
 * Layout (top -> bottom):
 *   1. Header (icon, title, subtitle)
 *   2. Snapshot grid: today's snapshot per metal (24K, 22K, optional Ag)
 *      x per weight (1g + 8g) for both UAE and India.
 *   3. 24K trend sparklines: UAE | India side-by-side
 *   4. 22K trend sparklines: UAE | India side-by-side
 *   5. Carat selector pills (24K | 22K | Ag-if-available) — controls #6/#7
 *   6. 30-day stats: High / Low / Avg pills, one row per region.
 *   7. Daily history table: Date | UAE | IN, values track selected carat.
 *
 * Silver (Ag) was added in v0.23.  UAE silver is spot-only (no history),
 * so the UAE columns in #6 / #7 show "—" while Ag is selected.  India
 * silver has 10-day history from LiveChennai.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldHistorySheet(
    gold: GoldDocument,
    onDismiss: () -> Unit,
) {
    // v0.27 structure: outer body is a LazyColumn (not a Column) so the
    // sheet has a single scrollable child, cooperating with M3's
    // ModalBottomSheet nestedScrollConnection.  The inline 30-row table
    // that used to sit inside a fixed-height LazyColumn is now a 5-row
    // mini-preview with a "View full history" tap-target — the full
    // table lives in its own dedicated [FullHistorySheet] sub-popup.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val silverAvailable = gold.uaeSilver?.status == "ok"
        && gold.indiaSilver?.status == "ok"
        && gold.uaeSilver?.perG != null
        && gold.indiaSilver?.perG != null

    var selectedCarat by remember { mutableStateOf("24K") }
    var fullHistoryOpen by remember { mutableStateOf(false) }

    val (uaeSelectedRates, indiaSelectedRates) = remember(gold, selectedCarat) {
        ratesForCarat(selectedCarat, gold)
    }

    // For the trend sparklines (always 24K + 22K, gold-only).
    val uaeGoldHistory = remember(gold) { gold.uae.history.sortedBy { it.date } }
    val indiaGoldHistory = remember(gold) { gold.india.history.sortedBy { it.date } }

    val allHistoryDates = remember(uaeSelectedRates, indiaSelectedRates) {
        (uaeSelectedRates.map { it.date } + indiaSelectedRates.map { it.date })
            .toSortedSet(reverseOrder())
            .toList()
    }
    val miniHistoryDates = remember(allHistoryDates) { allHistoryDates.take(5) }
    val uaeMap = remember(uaeSelectedRates) { uaeSelectedRates.associateBy { it.date } }
    val inrMap = remember(indiaSelectedRates) { indiaSelectedRates.associateBy { it.date } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            // 1. Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 22.sp, maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (silverAvailable)
                                "Gold & silver · UAE vs India"
                            else
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
            }

            // 2. Snapshot grid (gold + optional silver column)
            item {
                SnapshotGrid(gold = gold, silverAvailable = silverAvailable)
                Spacer(Modifier.height(18.dp))
            }

            // 3-4. Gold trend sparklines (24K + 22K)
            if (uaeGoldHistory.size >= 2 || indiaGoldHistory.size >= 2) {
                item {
                    TrendRow(
                        title = "24K trend (newest right)",
                        uaeValues = uaeGoldHistory.map { it.perG24k },
                        indiaValues = indiaGoldHistory.map { it.perG24k },
                    )
                    Spacer(Modifier.height(12.dp))
                    TrendRow(
                        title = "22K trend (newest right)",
                        uaeValues = uaeGoldHistory.map { it.perG22k },
                        indiaValues = indiaGoldHistory.map { it.perG22k },
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Optional silver trend (India only — UAE silver is spot, no history)
            if (silverAvailable) {
                val indiaSilverChrono = gold.indiaSilver?.history.orEmpty()
                    .sortedBy { it.date }
                if (indiaSilverChrono.size >= 2) {
                    item {
                        TrendRow(
                            title = "Silver trend · India (newest right)",
                            uaeValues = emptyList(),
                            indiaValues = indiaSilverChrono.map { it.perG },
                            uaePlaceholder = "spot only",
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // 5. Carat selector — controls the mini-history + stats + sub-sheet
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CaratChip("24K", selected = selectedCarat == "24K") { selectedCarat = "24K" }
                    Spacer(Modifier.width(10.dp))
                    CaratChip("22K", selected = selectedCarat == "22K") { selectedCarat = "22K" }
                    if (silverAvailable) {
                        Spacer(Modifier.width(10.dp))
                        CaratChip("Ag", selected = selectedCarat == "Ag") { selectedCarat = "Ag" }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // 6. 30-day stats (per region, follows selectedCarat)
            val showStatsBlock = uaeSelectedRates.isNotEmpty() || indiaSelectedRates.isNotEmpty()
            if (showStatsBlock) {
                item {
                    Text(
                        text = if (selectedCarat == "Ag")
                            "30-day stats · Silver"
                        else
                            "30-day stats · $selectedCarat",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (uaeSelectedRates.isNotEmpty()) {
                        StatRegionRow(
                            regionLabel = "UAE",
                            currencySym = "AED",
                            rates = uaeSelectedRates.map { it.rate },
                        )
                        Spacer(Modifier.height(8.dp))
                    } else if (selectedCarat == "Ag") {
                        // Honest spot disclosure: UAE silver has no daily
                        // history, just a live spot-converted rate.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "UAE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp),
                                maxLines = 1,
                                softWrap = false,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "spot price only — no daily history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (indiaSelectedRates.isNotEmpty()) {
                        StatRegionRow(
                            regionLabel = "India",
                            currencySym = "₹",
                            rates = indiaSelectedRates.map { it.rate },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 7. Recent days mini-table (5 rows) + "View full history" tap-target
            item {
                Text(
                    text = if (selectedCarat == "Ag")
                        "Recent days · Silver"
                    else
                        "Recent days · $selectedCarat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.height(6.dp))

                HistoryTableHeader(uaeLabel = "UAE", indiaLabel = "IN")
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (miniHistoryDates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (selectedCarat == "Ag")
                                "Silver history is India-only (UAE shows live spot)."
                            else
                                "Building history — check back tomorrow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    miniHistoryDates.forEach { date ->
                        HistoryRow(
                            date = date,
                            uaeRate = uaeMap[date]?.rate,
                            indiaRate = inrMap[date]?.rate,
                            isSilver = selectedCarat == "Ag",
                        )
                    }

                    // Show the "View full" CTA when we have more days than
                    // the mini-preview can fit.  Tapping opens a dedicated
                    // sub-sheet that scrolls the full date range.
                    if (allHistoryDates.size > miniHistoryDates.size) {
                        Spacer(Modifier.height(10.dp))
                        // Neutral-tinted surface with primary-coloured text:
                        // pale-indigo bg + dark-indigo text was technically
                        // contrast-passing but read as muddy because both
                        // colours sat in the same hue family.  Splitting bg
                        // (neutral) from fg (indigo) gives a proper hue cut
                        // for legibility while keeping the brand accent.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { fullHistoryOpen = true }
                                .padding(vertical = 13.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "View full ${allHistoryDates.size}-day history  →",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }

            // 8. Source attribution footer
            item {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (silverAvailable)
                        "Sources: Khaleej Times (UAE gold) · LiveChennai (India gold + silver) · " +
                        "spot XAG via gold-api.com (UAE silver). Indicative; jeweller prices may differ."
                    else
                        "Sources: Khaleej Times (UAE) · LiveChennai (India). " +
                        "Indicative; actual jeweller prices may differ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Sub-sheet: full daily history for the currently selected carat.
    // Stacks on top of the parent sheet (sheet-on-sheet); dismissing it
    // returns the user to the overview sheet without closing the parent.
    if (fullHistoryOpen) {
        FullHistorySheet(
            carat = selectedCarat,
            uaeRates = uaeSelectedRates,
            indiaRates = indiaSelectedRates,
            sheetState = historySheetState,
            onDismiss = { fullHistoryOpen = false },
        )
    }
}

/**
 * Dedicated full-history popup, opened from the parent gold sheet.
 *
 * Body is a single LazyColumn so the table scrolls cleanly inside the
 * ModalBottomSheet — no nested-scroll trap, no fixed-height cap.  Title,
 * column headers, and rows are all `item`/`items` blocks so the sheet
 * grows with the data and the OS handles the swipe-to-dismiss gesture
 * uniformly across the entire sheet body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullHistorySheet(
    carat: String,
    uaeRates: List<DatedRate>,
    indiaRates: List<DatedRate>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    val allDates = remember(uaeRates, indiaRates) {
        (uaeRates.map { it.date } + indiaRates.map { it.date })
            .toSortedSet(reverseOrder())
            .toList()
    }
    val uaeMap = remember(uaeRates) { uaeRates.associateBy { it.date } }
    val inrMap = remember(indiaRates) { indiaRates.associateBy { it.date } }
    val isSilver = carat == "Ag"
    val metals = LocalMetalColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            // Title row — metal-tinted icon + carat-aware label
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSilver) {
                        Text(
                            "◇",
                            fontSize = 22.sp,
                            color = metals.silverAccent,
                            maxLines = 1,
                        )
                    } else {
                        Text("🪙", fontSize = 22.sp, maxLines = 1)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isSilver)
                                "Silver · ${allDates.size}-day history"
                            else
                                "$carat gold · ${allDates.size}-day history",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = "UAE vs India · per gram",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
            }

            // Column headers — wider currency labels because there's room
            item {
                HistoryTableHeader(
                    uaeLabel = "UAE (AED)",
                    indiaLabel = "India (₹)",
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            if (allDates.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isSilver)
                                "Silver history is India-only (UAE shows live spot)."
                            else
                                "Building history — check back tomorrow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(allDates) { date ->
                    HistoryRow(
                        date = date,
                        uaeRate = uaeMap[date]?.rate,
                        indiaRate = inrMap[date]?.rate,
                        isSilver = isSilver,
                    )
                }
            }
        }
    }
}

/** Shared 3-column header row used by both the inline mini-table and
 *  the full-history sub-sheet.  Pulled out so the column weights stay
 *  identical between the two contexts.  Uses `onSurfaceVariant` for
 *  proper "muted-but-legible" body contrast (was `outline` — which is
 *  the M3 role for divider strokes, too pale for text). */
@Composable
private fun HistoryTableHeader(uaeLabel: String, indiaLabel: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Date",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.2f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            uaeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            indiaLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Single date row (Date | UAE | INR), shared between the mini-preview
 *  in the parent sheet and the LazyColumn rows in the sub-sheet. */
@Composable
private fun HistoryRow(
    date: String,
    uaeRate: Double?,
    indiaRate: Double?,
    isSilver: Boolean,
) {
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
            text = uaeRate?.let {
                if (isSilver) "%.2f".format(it) else "%.0f".format(it)
            } ?: "—",
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
            text = indiaRate?.let { "%,.0f".format(it) } ?: "—",
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

/** Row of "UAE" + "India" sparklines with a shared title.  When uaeValues
 *  is empty AND a placeholder is supplied, we render the placeholder text
 *  in the UAE column instead of an empty box (used for silver where UAE
 *  has no daily history). */
@Composable
private fun TrendRow(
    title: String,
    uaeValues: List<Double>,
    indiaValues: List<Double>,
    uaePlaceholder: String? = null,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uaeValues.isEmpty() && uaePlaceholder != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "UAE  (AED)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        uaePlaceholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            SparkColumn(
                label = "UAE",
                unit = "AED",
                values = uaeValues,
                modifier = Modifier.weight(1f),
            )
        }
        SparkColumn(
            label = "India",
            unit = "₹",
            values = indiaValues,
            modifier = Modifier.weight(1f),
        )
    }
}

/** One region's High / Low / Avg pills, weighted to fill row width evenly. */
@Composable
private fun StatRegionRow(
    regionLabel: String,
    currencySym: String,
    rates: List<Double>,
) {
    val high = rates.max()
    val low = rates.min()
    val avg = rates.average()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            regionLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatPill(
                label = "High",
                value = "$currencySym ${formatGold(high)}",
                modifier = Modifier.weight(1f),
            )
            StatPill(
                label = "Low",
                value = "$currencySym ${formatGold(low)}",
                modifier = Modifier.weight(1f),
            )
            StatPill(
                label = "Avg",
                value = "$currencySym ${formatGold(avg)}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SnapshotGrid(gold: GoldDocument, silverAvailable: Boolean) {
    val metals = LocalMetalColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Gold card — warm-tinted
        MetalSnapshotCard(
            modifier = Modifier.weight(1f),
            metals = metals,
            isGold = true,
            uae24 = gold.uae.perG24k, uae22 = gold.uae.perG22k,
            india24 = gold.india.perG24k, india22 = gold.india.perG22k,
            uaeSilver = null, indiaSilver = null,
        )
        if (silverAvailable) {
            MetalSnapshotCard(
                modifier = Modifier.weight(1f),
                metals = metals,
                isGold = false,
                uae24 = null, uae22 = null,
                india24 = null, india22 = null,
                uaeSilver = gold.uaeSilver?.perG,
                indiaSilver = gold.indiaSilver?.perG,
            )
        }
    }
}

/**
 * One metal's snapshot card — gold or silver — with that metal's
 * own warm/cool tonal palette.  Gold card lists 24K + 22K rates per
 * region; silver card lists 1g + 1kg per region.
 */
@Composable
private fun MetalSnapshotCard(
    modifier: Modifier,
    metals: MetalColors,
    isGold: Boolean,
    uae24: Double?, uae22: Double?,
    india24: Double?, india22: Double?,
    uaeSilver: Double?, indiaSilver: Double?,
) {
    val gradStart = if (isGold) metals.goldLightest else metals.silverLightest
    val gradEnd   = if (isGold) metals.goldLight    else metals.silverLight
    val accent    = if (isGold) metals.goldText     else metals.silverText
    val deep      = if (isGold) metals.goldDeep     else metals.silverDeep
    val outline   = if (isGold) metals.goldSurface  else metals.silverSurface
    val title     = if (isGold) "🪙  GOLD"          else "◇  SILVER"

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(brush = Brush.verticalGradient(colors = listOf(gradStart, gradEnd)))
            .padding(12.dp),
    ) {
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = accent,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(10.dp))
        if (isGold) {
            // Gold card content: 24K (UAE + India), 22K (UAE + India)
            MetalLine(label = "UAE  ·  24K 1g",
                      value = uae24?.let { "AED %.0f".format(it) } ?: "—",
                      accent = accent, deep = deep, emphasis = true)
            Spacer(Modifier.height(4.dp))
            MetalLine(label = "INDIA  ·  24K 1g",
                      value = india24?.let { "₹ %,.0f".format(it) } ?: "—",
                      accent = accent, deep = deep, emphasis = true)
            Spacer(Modifier.height(8.dp))
            MetalDivider(accent.copy(alpha = 0.25f))
            Spacer(Modifier.height(8.dp))
            MetalLine(label = "UAE  ·  22K 1g",
                      value = uae22?.let { "AED %.0f".format(it) } ?: "—",
                      accent = accent, deep = deep, emphasis = false)
            Spacer(Modifier.height(4.dp))
            MetalLine(label = "INDIA  ·  22K 1g",
                      value = india22?.let { "₹ %,.0f".format(it) } ?: "—",
                      accent = accent, deep = deep, emphasis = false)
        } else {
            // Silver card content: 1g (UAE + India), 1kg (UAE + India)
            MetalLine(label = "UAE  ·  1g",
                      value = uaeSilver?.let { "AED %.2f".format(it) } ?: "—",
                      accent = accent, deep = deep, emphasis = true)
            Spacer(Modifier.height(4.dp))
            MetalLine(label = "INDIA  ·  1g",
                      value = indiaSilver?.let { "₹ %,.0f".format(it) } ?: "—",
                      accent = accent, deep = deep, emphasis = true)
            Spacer(Modifier.height(8.dp))
            MetalDivider(accent.copy(alpha = 0.25f))
            Spacer(Modifier.height(8.dp))
            MetalLine(label = "UAE  ·  1kg",
                      value = uaeSilver?.let { "AED %,.0f".format(it * 1000) } ?: "—",
                      accent = accent, deep = deep, emphasis = false)
            Spacer(Modifier.height(4.dp))
            MetalLine(label = "INDIA  ·  1kg",
                      value = indiaSilver?.let { "₹ %,.0f".format(it * 1000) } ?: "—",
                      accent = accent, deep = deep, emphasis = false)
        }
    }
}

@Composable
private fun MetalLine(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    deep: androidx.compose.ui.graphics.Color,
    emphasis: Boolean,
) {
    Text(
        label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        color = accent,
        maxLines = 1,
        softWrap = false,
    )
    Text(
        value,
        fontSize = if (emphasis) 16.sp else 13.sp,
        fontWeight = if (emphasis) FontWeight.Bold else FontWeight.SemiBold,
        color = deep,
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFeatureSettings = "tnum",
        ),
    )
}

@Composable
private fun MetalDivider(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatGold(value: Double): String =
    "%,.0f".format(value)

private fun formatShortDate(iso: String): String =
    try {
        LocalDate.parse(iso).format(
            DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH),
        )
    } catch (_: DateTimeParseException) { iso }
