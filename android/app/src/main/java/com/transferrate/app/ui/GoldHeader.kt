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
import androidx.compose.ui.res.stringResource
import com.transferrate.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.data.GoldDocument
import com.transferrate.app.ui.theme.LocalMetalColors

/**
 * UAE vs India precious-metals rate module — sits at half-width next
 * to MidMarketHeader.  Tap opens the bottom-sheet detail view.
 *
 * v0.26 design:
 *   - Two-column inner layout (Gold | Silver) so each metal sits
 *     beside the other for at-a-glance comparison.
 *   - Each column carries its OWN tonal background (warm gold, cool
 *     steel) so the metals read as themselves rather than borrowing
 *     the indigo accent palette.  Backgrounds are subtle gradients
 *     between the metal's two lightest tonal steps so the card stays
 *     legible without shouting.
 *   - When silver data is missing (older rates.json without silver
 *     fields), the silver column is omitted and the gold column
 *     takes full width — graceful degradation for forward compat.
 *
 * Uses the v0.26 [LocalMetalColors] composition local rather than
 * hand-rolled hex literals — palette decisions ladder back to
 * Theme.kt and tone-map automatically between light and dark mode.
 */
@Composable
fun GoldHeader(
    gold: GoldDocument,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val goldOk = gold.uae.status == "ok" && gold.india.status == "ok"

    // v0.29.6: silver section shows whenever EITHER side has a value.
    // Previous all-or-nothing gate threw away India silver (10-day
    // history, working) on days when UAE silver's spot-XAG fetch
    // transiently failed (DNS / gold-api.com hiccup, no fallback
    // because UAE silver has no daily history to reuse).
    val uaeSilverOk = gold.uaeSilver?.status == "ok" && gold.uaeSilver?.perG != null
    val indiaSilverOk = gold.indiaSilver?.status == "ok" && gold.indiaSilver?.perG != null
    val silverOk = uaeSilverOk || indiaSilverOk

    val metals = LocalMetalColors.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 156.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        if (!goldOk) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                AutoSizeText(
                    text = stringResource(R.string.metals_gold),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = metals.goldText,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.gold_unavailable),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            return@Card
        }

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // ===== GOLD COLUMN =====
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(metals.goldLightest, metals.goldLight),
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 13.sp, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    // v0.31.1: AutoSizeText replaces the static 10 sp Text
                    // because the bare metal label (even without the
                    // " · AED" suffix) was still ellipsizing in Tamil
                    // ("தங்..." in v0.30.8 screenshot) — Tamil "தங்கம்"
                    // and Malayalam "സ്വർണം" use 5–6 glyphs each wider
                    // than English "GOLD" at the same sp.  AutoSizeText
                    // starts at 10 sp and shrinks to ~7 sp if needed so
                    // the full label is always visible; English stays
                    // at 10 sp because it fits.
                    AutoSizeText(
                        text = stringResource(R.string.metals_gold),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = metals.goldText,
                    )
                }
                Spacer(Modifier.height(8.dp))
                MetalRow(
                    label = "24K",
                    aedValue = gold.uae.perG24k,
                    inrValue = gold.india.perG24k,
                    accentColor = metals.goldText,
                    valueColor = metals.goldDeep,
                    aedDecimals = 0,
                    emphasis = true,
                )
                Spacer(Modifier.height(6.dp))
                MetalRow(
                    label = "22K",
                    aedValue = gold.uae.perG22k,
                    inrValue = gold.india.perG22k,
                    accentColor = metals.goldText,
                    valueColor = metals.goldDeep,
                    aedDecimals = 0,
                    emphasis = false,
                )
            }

            if (silverOk) {
                // ===== SILVER COLUMN =====
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(metals.silverLightest, metals.silverLight),
                            ),
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◇", fontSize = 13.sp,
                             color = metals.silverAccent, maxLines = 1)
                        Spacer(Modifier.width(4.dp))
                        AutoSizeText(
                            text = stringResource(R.string.metals_silver),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = metals.silverText,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    MetalRow(
                        label = "1g",
                        aedValue = gold.uaeSilver?.perG,
                        inrValue = gold.indiaSilver?.perG,
                        accentColor = metals.silverText,
                        valueColor = metals.silverDeep,
                        aedDecimals = 2,
                        emphasis = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    MetalRow(
                        label = "1kg",
                        aedValue = gold.uaeSilver?.perG?.times(1000),
                        inrValue = gold.indiaSilver?.perG?.times(1000),
                        accentColor = metals.silverText,
                        valueColor = metals.silverDeep,
                        aedDecimals = 0,
                        emphasis = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetalRow(
    label: String,
    aedValue: Double?,
    inrValue: Double?,
    accentColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
    aedDecimals: Int,
    emphasis: Boolean,
) {
    // v0.30.7: AED prefix is gone — unit moved to the column header
    // ("GOLD · AED" / "SILVER · AED").  Values render as bare numbers,
    // freeing ~28 dp per row on 360 dp phones where the silver column
    // previously clipped "AED 10.06" mid-glyph.
    val aedFormat = "%.${aedDecimals}f"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(
                        accentColor.copy(alpha = 0.12f),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                // Row labels here are always unit codes ("24K", "22K",
                // "1g", "1kg") — English-only, fixed-width.  Static Text
                // is fine; AutoSizeText would be overkill since these
                // never localise.
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (aedValue != null) aedFormat.format(aedValue) else "—",
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (emphasis) 14.sp else 12.sp,
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            // Defensive ellipsis — if a future locale renders digits
            // wider (Devanagari, Tamil numerals) the value tails off
            // with "…" instead of being chopped mid-glyph.
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFeatureSettings = "tnum",
            ),
        )
        Text(
            text = if (inrValue != null) "₹ %,.0f".format(inrValue) else "—",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = accentColor.copy(alpha = 0.85f),
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFeatureSettings = "tnum",
            ),
        )
    }
}
