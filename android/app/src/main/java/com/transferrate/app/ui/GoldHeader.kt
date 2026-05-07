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
 * to MidMarketHeader. Tap opens the 30-day GoldHistorySheet.
 *
 * Design:
 *   - Three rate rows: 24K gold (emphasized), 22K gold, Ag silver.
 *   - Silver row only renders when uae_silver / india_silver are both
 *     present and "ok"; older rates.json without silver fields gets
 *     the original two-row look unchanged.
 *   - Eyebrow stays "GOLD" because silver is a secondary signal — most
 *     users open the card for gold, the silver line is a useful
 *     incidental.
 *   - Mirrors MidMarketHeader's vertical rhythm so the two cards align
 *     perfectly in a 50/50 Row.  heightIn min bumped from 156 -> 178
 *     to fit the third row without squashing.
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

    val minHeight = if (silverOk) 178.dp else 156.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            // Brand-gold container — warm contrast next to MidMarket's
            // teal primaryContainer.
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Eyebrow — full-opacity bold, mirrors MidMarket
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GOLD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(6.dp))
                Text("🪙", fontSize = 13.sp, maxLines = 1)
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

            // 24K row (emphasized)
            CaratRow(
                label = "24K",
                aed = gold.uae.perG24k,
                inr = gold.india.perG24k,
                emphasis = true,
            )
            Spacer(Modifier.height(6.dp))

            // 22K row
            CaratRow(
                label = "22K",
                aed = gold.uae.perG22k,
                inr = gold.india.perG22k,
                emphasis = false,
            )

            // Silver row — only when both sides have ok data.
            // AED uses 2 decimals (~9.53/g — single-digit needs precision);
            // INR uses 0 decimals (275/g — already integer-like).
            if (silverOk) {
                Spacer(Modifier.height(6.dp))
                CaratRow(
                    label = "Ag",
                    aed = gold.uaeSilver?.perG,
                    inr = gold.indiaSilver?.perG,
                    emphasis = false,
                    aedDecimals = 2,
                )
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
    aedDecimals: Int = 0,
) {
    val valueColor = MaterialTheme.colorScheme.onSecondaryContainer
    val aedFormat = "AED %.${aedDecimals}f"

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
                text = if (aed != null) aedFormat.format(aed) else "—",
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
