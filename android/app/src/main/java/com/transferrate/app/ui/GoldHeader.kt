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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.data.GoldDocument

/**
 * UAE vs India gold rate module — sits next to MidMarketHeader at 50/50
 * width. Tap opens GoldHistorySheet showing the last 30 days.
 *
 * Composition:
 *   - Brand-gold gradient backdrop with the Exchangia colour story
 *   - Two compact rate rows: 24K and 22K
 *   - Each row shows AED/g (UAE) and INR/g (India) side by side
 *
 * The 8-gram aggregate is computed in the history sheet (less screen
 * pressure on the home screen, more space on the detail sheet).
 */
@Composable
fun GoldHeader(
    gold: GoldDocument,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val ok = gold.uae.status == "ok" && gold.india.status == "ok"

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            // Brand gold container; warm-leaning for visual contrast
            // with the cool-teal MidMarket header beside it.
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "GOLD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "🪙",
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(8.dp))

            if (!ok) {
                Text(
                    text = "Gold rates unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                )
                return@Column
            }

            // 24K row
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

            Spacer(Modifier.height(8.dp))
            Text(
                text = "per gram · tap for 30-day chart",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
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
    val labelColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
        alpha = if (emphasis) 0.92f else 0.72f,
    )
    val valueColor = MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Carat label pill
        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                    RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = if (aed != null) "AED %.0f".format(aed) else "—",
                fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = if (emphasis) 14.sp else 13.sp,
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
                color = valueColor.copy(alpha = 0.72f),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}
