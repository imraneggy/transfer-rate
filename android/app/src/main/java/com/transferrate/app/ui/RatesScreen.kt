package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transferrate.app.R
import com.transferrate.app.data.CURRENCIES
import com.transferrate.app.data.CurrencyInfo
import com.transferrate.app.data.ProviderQuote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(vm: RatesViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Text("↻", fontSize = 22.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is RatesUiState.Loading -> CenteredSpinner()
                is RatesUiState.Failed -> ErrorView(s.message) { vm.refresh() }
                is RatesUiState.Ready -> ReadyView(
                    state = s,
                    onSelectCurrency = vm::selectCurrency,
                )
            }
        }
    }
}

@Composable
private fun stringResource(id: Int): String =
    androidx.compose.ui.res.stringResource(id)

@Composable
private fun CenteredSpinner() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.error_loading), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        IconButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ReadyView(
    state: RatesUiState.Ready,
    onSelectCurrency: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val selected = state.selectedCurrency
    val info = CURRENCIES[selected]
    val midMarket = state.midMarketRate

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            CurrencyChipRow(
                available = state.doc.corridors.keys,
                selected = selected,
                onSelect = onSelectCurrency,
            )
        }
        item {
            MidMarketHeader(
                info = info,
                midMarket = midMarket,
                completedAt = state.doc.completedAt,
            )
        }
        items(state.visibleQuotes, key = { "${selected}-${it.providerId}" }) { p ->
            val isBest = p.status == "ok"
                    && state.bestRate != null
                    && (p.effectiveRate ?: p.rate) == state.bestRate
            ProviderCard(
                p = p,
                isBest = isBest,
                midMarket = midMarket,
                onClick = {
                    p.url?.let { url ->
                        runCatching {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(intent)
                        }
                    }
                },
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Mid-market header — the prominent objective benchmark for this corridor.
 *
 * Format:
 *   1 AED
 *   = 25.8041 ₹
 *   Indian Rupee · Mid-market rate
 *   Updated 2 minutes ago
 *
 * Sourced from Wise's /rates/live endpoint, which IS the mid-market
 * (interbank midpoint) rate. We label it as such so users know it's the
 * objective benchmark, not Wise's marketing position.
 */
@Composable
private fun MidMarketHeader(
    info: CurrencyInfo?,
    midMarket: Double?,
    completedAt: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "1 AED",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.width(8.dp))
                if (info != null) {
                    Text(text = info.flag, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "=",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (midMarket != null) "%.4f".format(midMarket) else "—",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                )
                Spacer(Modifier.width(8.dp))
                if (info != null) {
                    Text(
                        text = info.symbol,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (info != null) "${info.name} · Mid-market rate" else "Mid-market rate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Updated $completedAt · Wise live FX",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun CurrencyChipRow(
    available: Set<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val ordered = CURRENCIES.values.filter { it.code in available }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(ordered, key = { it.code }) { info ->
            CurrencyChip(
                info = info,
                isSelected = info.code == selected,
                onClick = { onSelect(info.code) },
            )
        }
    }
}

@Composable
private fun CurrencyChip(info: CurrencyInfo, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
    val fgColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = info.flag, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = info.code,
            color = fgColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun ProviderCard(
    p: ProviderQuote,
    isBest: Boolean,
    midMarket: Double?,
    onClick: () -> Unit,
) {
    val containerColor = when {
        isBest -> MaterialTheme.colorScheme.secondaryContainer
        p.status == "ok" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val border = if (isBest) {
        Modifier.border(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(16.dp),
        )
    } else Modifier

    Card(
        modifier = Modifier.fillMaxWidth().then(border),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isBest) 2.dp else 0.dp),
    ) {
        Column(Modifier.padding(14.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderAvatar(p.providerId, p.providerName, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            p.providerName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (isBest) {
                            Spacer(Modifier.width(8.dp))
                            BestBadge()
                        }
                    }
                    val sub = p.deliveryEstimate
                        ?: when (p.status) {
                            "ok" -> ""
                            "stale" -> "Last good rate"
                            "investigating" -> "Coming soon"
                            else -> "Unavailable"
                        }
                    if (sub.isNotEmpty()) {
                        Text(
                            sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (p.status != "ok" && p.status != "investigating" && p.note != null) {
                        Text(
                            p.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                RateView(p, midMarket = midMarket)
            }
            if (p.status == "ok" && p.promoRate != null) {
                Spacer(Modifier.height(10.dp))
                PromoBadge(p.promoRate, p.promoNote, p.quote)
            }
        }
    }
}

@Composable
private fun BestBadge() {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondary,
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "BEST",
            color = MaterialTheme.colorScheme.onSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun RateView(p: ProviderQuote, midMarket: Double?) {
    val rate = p.effectiveRate ?: p.rate
    val symbol = CURRENCIES[p.quote]?.symbol ?: ""
    Column(horizontalAlignment = Alignment.End) {
        when (p.status) {
            "ok" -> {
                Text(
                    text = if (rate != null) "%.4f".format(rate) else "—",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                )
                if (rate != null && midMarket != null) {
                    val delta = rate - midMarket
                    val (label, color) = when {
                        delta > 0.001 -> "+%.4f".format(delta) to Color(0xFF1B7B33)
                        delta < -0.001 -> "%.4f".format(delta) to Color(0xFFB71C1C)
                        else -> "= mid-market" to MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = "$label vs mid-market",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = color,
                    )
                } else {
                    Text(
                        "$symbol per AED",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            "stale" -> {
                Text(
                    text = if (rate != null) "%.4f".format(rate) else "—",
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                )
                Text(
                    "stale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            "investigating" -> StatusDot(Color(0xFF94A3B8))
            else -> StatusDot(MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PromoBadge(promoRate: Double, note: String?, quoteCode: String) {
    val symbol = CURRENCIES[quoteCode]?.symbol ?: ""
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "%.4f".format(promoRate),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 13.sp,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFeatureSettings = "tnum",
            ),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "$symbol",
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontSize = 13.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = note ?: "Promotional rate",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        Modifier.size(10.dp).background(color, CircleShape),
    )
}
