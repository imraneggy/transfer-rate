package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.data.GoldDocument

/**
 * UAE vs India precious-metals rate module — sits at half-width next
 * to MidMarketHeader.  Tap opens the bottom-sheet detail view.
 *
 * Layout (v0.25):
 *   ┌──────────────────────────────────────────────────┐
 *   │ 🪙  GOLD                              SILVER     │  ← eyebrow row
 *   │  24K  AED 570        ·         AED 9.53          │
 *   │       ₹ 15,436                 ₹ 275             │
 *   │  22K  AED 527                                    │
 *   │       ₹ 14,150                                   │
 *   │  per gram · tap for chart                        │
 *   └──────────────────────────────────────────────────┘
 *
 * The two-column inner layout (Gold | Silver) is the v0.25 redesign
 * per user request — silver sits BESIDE gold rather than below it,
 * so users compare the metals at a glance rather than reading silver
 * as a footnote.  When silver data is missing (older rates.json
 * without uae_silver / india_silver fields), the silver column is
 * omitted and the card collapses back to the v0.22 single-column
 * gold-only look.
 */
@Composable
fun GoldHeader(
    gold: GoldDocument,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val goldOk = gold.uae.status == "ok" && gold.india.status == "ok"

    val silverOk = gold.uaeSilver?.status == "ok"
        && gold.indiaSilver?.status == "ok"
        && gold.uaeSilver?.perG != null
        && gold.indiaSilver?.perG != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 156.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            // Brand-gold container — warm contrast next to MidMarket's
            // teal primaryContainer.
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Eyebrow row.  When silver is present, we place "GOLD" and
            // "SILVER" labels side by side so the column header doubles
            // as a section header for each metal column below.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 13.sp, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Text(
                    "GOLD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
                if (silverOk) {
                    Text(
                        "SILVER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (!goldOk) {
                Text(
                    text = "Rates unavailable",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                )
                return@Column
            }

            // Two-column body row: gold (24K + 22K stacked) on the left,
            // silver (single value) on the right.  Silver column omitted
            // when silver data is missing — gold then takes full width
            // exactly as in v0.22.
            Row(modifier = Modifier.fillMaxWidth()) {
                // Gold column
                Column(modifier = Modifier.weight(1f)) {
                    CaratRow(
                        label = "24K",
                        aed = gold.uae.perG24k,
                        inr = gold.india.perG24k,
                        emphasis = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    CaratRow(
                        label = "22K",
                        aed = gold.uae.perG22k,
                        inr = gold.india.perG22k,
                        emphasis = false,
                    )
                }

                if (silverOk) {
                    Spacer(Modifier.width(12.dp))
                    // Silver column.  No carat pill — the SILVER label in
                    // the eyebrow already heads this column; another pill
                    // would be redundant.  Top-aligned so silver value
                    // sits at the same baseline as the 24K gold value.
                    Column(modifier = Modifier.weight(1f)) {
                        SilverValuePair(
                            aed = gold.uaeSilver?.perG,
                            inr = gold.indiaSilver?.perG,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "per gram · tap for 30-day chart",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun CaratRow(
    label: String,
    aed: Double?,
    inr: Double?,
    emphasis: Boolean,
) {
    val valueColor = MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Carat pill — bold for high contrast
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                    RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = if (aed != null) "AED %.0f".format(aed) else "—",
                fontWeight = if (emphasis) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = if (emphasis) 16.sp else 13.sp,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(
                text = if (inr != null) "₹ %,.0f".format(inr) else "—",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor.copy(alpha = 0.78f),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}

/** Compact AED-over-INR pair for the silver column.  No pill prefix — the
 *  SILVER eyebrow above does the labelling.  Slightly smaller font sizes
 *  than the gold 24K row so the gold column visually dominates (gold is
 *  the primary signal; silver is contextual). */
@Composable
private fun SilverValuePair(aed: Double?, inr: Double?) {
    val valueColor = MaterialTheme.colorScheme.onSecondaryContainer
    Column {
        Text(
            text = if (aed != null) "AED %.2f".format(aed) else "—",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = "tnum",
            ),
        )
        Text(
            text = if (inr != null) "₹ %,.0f".format(inr) else "—",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor.copy(alpha = 0.78f),
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFeatureSettings = "tnum",
            ),
        )
    }
}
