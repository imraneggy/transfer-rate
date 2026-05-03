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
 * UAE vs India gold rate module — sits at half-width next to
 * MidMarketHeader. Tap opens the 30-day GoldHistorySheet.
 *
 * Design (v0.13.3): matches MidMarketHeader's vertical rhythm exactly
 * — same eyebrow row, same heightIn(min=156dp), same padding so the
 * two cards align perfectly when placed in a 50/50 Row.
 */
@Composable
fun GoldHeader(
    gold: GoldDocument,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val ok = gold.uae.status == "ok" && gold.india.status == "ok"

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

            if (!ok) {
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
