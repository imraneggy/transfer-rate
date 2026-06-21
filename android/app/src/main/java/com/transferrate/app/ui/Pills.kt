package com.transferrate.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared label + value pill used in history sheets to show
 * High / Low / Avg style stats. Centered, surface-variant background,
 * tabular-figures for value alignment.
 *
 * Pass `Modifier.weight(1f)` from a Row to spread evenly; otherwise
 * the pill is intrinsically sized by its padding (18 horizontal, 8 vertical).
 */
@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // v0.31.1: AutoSizeText so localised stat labels (Tamil
        // "அதிகம் / குறைவு / சராசரி", Malayalam "ഉയർന്നത് / താഴ്ന്നത് /
        // ശരാശരി") shrink rather than ellipsise inside the pill.
        AutoSizeText(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            minFontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = "tnum",
            ),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Toggle pill used to select between two options (e.g. 24K / 22K).
 * When selected, uses primary color; otherwise surface-variant.
 */
@Composable
fun CaratChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "carat-chip-bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200),
        label = "carat-chip-fg",
    )
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(bg, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 8.dp),
    ) {
        AutoSizeText(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            minFontSize = 11.sp,
            color = fg,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
