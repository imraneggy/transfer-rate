package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
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
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurface
    Text(
        text = label,
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 8.dp),
        fontWeight = FontWeight.SemiBold,
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        softWrap = false,
    )
}
