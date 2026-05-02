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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

/** Theme override modes for the user-facing toggle. */
enum class ThemeMode {
    System, Light, Dark;

    fun next(): ThemeMode = when (this) {
        System -> Light
        Light -> Dark
        Dark -> System
    }

    /** Glyph + label for the toggle button. */
    val glyph: String get() = when (this) {
        System -> "⚙"   // gear (current = system)
        Light -> "☀"    // sun (current = light)
        Dark -> "☾"     // moon (current = dark)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(
    vm: RatesViewModel = viewModel(),
    themeMode: ThemeMode = ThemeMode.System,
    onCycleThemeMode: () -> Unit = {},
    onShowAbout: () -> Unit = {},
) {
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
                    IconButton(onClick = onShowAbout) {
                        Text(
                            "ⓘ",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = onCycleThemeMode) {
                        Text(
                            themeMode.glyph,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Text(
                            "↻",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
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
                is RatesUiState.Ready -> {
                    PullToRefreshBox(
                        isRefreshing = s.refreshing,
                        onRefresh = { vm.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ReadyView(
                            state = s,
                            onSelectCurrency = vm::selectCurrency,
                            onAmountChange = vm::setAmount,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convert an ISO 8601 UTC timestamp to a friendly relative phrase
 * ("just now", "5 minutes ago", "2 hours ago").
 *
 * Relative time is far more useful than a raw timestamp when users
 * compare the displayed rate to live sources like Google. They can see
 * at a glance how stale the data is and decide whether to refresh.
 */
private fun relativeTime(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        val seconds = Duration.between(instant, Instant.now()).seconds
        when {
            seconds < 0 -> "just now"
            seconds < 60 -> "just now"
            seconds < 120 -> "1 minute ago"
            seconds < 3600 -> "${seconds / 60} minutes ago"
            seconds < 7200 -> "1 hour ago"
            seconds < 86400 -> "${seconds / 3600} hours ago"
            seconds < 172800 -> "1 day ago"
            else -> "${seconds / 86400} days ago"
        }
    } catch (_: DateTimeParseException) {
        iso
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

/**
 * Full-screen error state. Friendlier than the previous one-line message:
 * shows a large glyph, a clear primary message, the underlying technical
 * detail in a quieter style, and a prominent retry button shaped as a
 * filled tonal button rather than an icon-only IconButton.
 */
@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    val isOffline = message.contains("Unable to resolve", ignoreCase = true) ||
                    message.contains("UnknownHost", ignoreCase = true) ||
                    message.contains("ConnectException", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true)
    val (glyph, headline, hint) = when {
        isOffline -> Triple(
            "📡",
            "Can't reach the rate feed",
            "Check your internet connection and tap retry. Cached rates aren't available yet.",
        )
        else -> Triple(
            "⚠",
            "Couldn't load rates",
            "An unexpected error occurred while fetching the latest rates.",
        )
    }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(glyph, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            headline,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
        Spacer(Modifier.height(28.dp))
        androidx.compose.material3.FilledTonalButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}

@Composable
private fun ReadyView(
    state: RatesUiState.Ready,
    onSelectCurrency: (String) -> Unit,
    onAmountChange: (Double) -> Unit,
) {
    val ctx = LocalContext.current
    val selected = state.selectedCurrency
    val info = CURRENCIES[selected]
    val midMarket = state.midMarketRate

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.doc.corridors.size > 1) {
            item {
                CurrencyChipRow(
                    available = state.doc.corridors.keys,
                    selected = selected,
                    onSelect = onSelectCurrency,
                )
            }
        }
        item {
            MidMarketHeader(
                info = info,
                midMarket = midMarket,
                completedAt = state.doc.completedAt,
            )
        }
        item { FirstLaunchHint() }
        item {
            AmountPanel(
                amount = state.selectedAmount,
                onAmountChange = onAmountChange,
            )
        }
        items(state.visibleQuotes, key = { "${selected}-${it.providerId}" }) { p ->
            val isBest = (p.status == "ok" || p.status == "manual")
                    && state.bestRate != null
                    && (p.effectiveRate ?: p.rate) == state.bestRate
            val historyForProvider = state.history?.providers?.get(p.providerId)
                ?.map { it.rate }
                ?: emptyList()
            ProviderCard(
                p = p,
                isBest = isBest,
                midMarket = midMarket,
                amount = state.selectedAmount,
                history = historyForProvider,
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
 * First-launch onboarding hint shown above the amount panel until
 * dismissed. Persists via SharedPreferences (lightweight; no need for
 * DataStore for a single boolean).
 */
@Composable
private fun FirstLaunchHint() {
    val ctx = LocalContext.current
    val prefs = remember {
        ctx.getSharedPreferences("transfer-rate", android.content.Context.MODE_PRIVATE)
    }
    var dismissed by rememberSaveable { mutableStateOf(prefs.getBoolean("hint_dismissed_v1", false)) }
    if (dismissed) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "💡  Welcome",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        prefs.edit().putBoolean("hint_dismissed_v1", true).apply()
                        dismissed = true
                    },
                ) {
                    Text(
                        "✕",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
            Text(
                text = "The big number above is the mid-market rate — the " +
                       "wholesale benchmark every provider charges a markup on. " +
                       "Each card below shows how much above (+) or below (−) " +
                       "mid-market that provider is. BEST goes to whoever pays " +
                       "you the most rupees per AED.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
            )
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
                text = "Updated ${relativeTime(completedAt)} · Google Finance",
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

/**
 * Send-amount input panel. Three things:
 *   - Quick-pick chips for common amounts (1k, 5k, 10k, 25k, 50k AED)
 *   - A text field for custom amounts
 *   - Updates state on focus-loss / IME action so we don't recompute on
 *     every keystroke (which would cause the LazyColumn to re-layout
 *     every typed character)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountPanel(
    amount: Double,
    onAmountChange: (Double) -> Unit,
) {
    var fieldValue by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { it.text },
        restore = { TextFieldValue(it) },
    )) { mutableStateOf(TextFieldValue(formatAmount(amount))) }
    val focusManager = LocalFocusManager.current

    // If the underlying amount changes (e.g. quick-chip), reflect that in the
    // text field — but only if the user isn't currently editing.
    LaunchedEffect(amount) {
        val currentText = fieldValue.text.replace(",", "").trim()
        val currentNum = currentText.toDoubleOrNull()
        if (currentNum != amount) {
            fieldValue = TextFieldValue(formatAmount(amount))
        }
    }

    fun commitAmount(text: String) {
        val cleaned = text.replace(",", "").trim()
        val n = cleaned.toDoubleOrNull()
        if (n != null && n in 1.0..1_000_000.0) {
            onAmountChange(n)
            fieldValue = TextFieldValue(formatAmount(n))
        } else {
            // Snap back to the current valid amount
            fieldValue = TextFieldValue(formatAmount(amount))
        }
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { fieldValue = it },
            label = { Text("Sending") },
            prefix = { Text("AED ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    commitAmount(fieldValue.text)
                    focusManager.clearFocus()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1000.0, 5000.0, 10_000.0, 25_000.0, 50_000.0).forEach { v ->
                AmountChip(
                    label = formatAmount(v),
                    selected = (amount == v),
                    onClick = {
                        onAmountChange(v)
                        fieldValue = TextFieldValue(formatAmount(v))
                        focusManager.clearFocus()
                    },
                )
            }
        }
    }
}

@Composable
private fun AmountChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

private fun formatAmount(value: Double): String {
    val asLong = value.toLong()
    return if (asLong.toDouble() == value) "%,d".format(asLong)
    else "%,.2f".format(value)
}

@Composable
private fun ProviderCard(
    p: ProviderQuote,
    isBest: Boolean,
    midMarket: Double?,
    amount: Double,
    history: List<Double>,
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
                        if (p.status == "manual") {
                            Spacer(Modifier.width(8.dp))
                            ManualBadge()
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
            val rate = p.effectiveRate ?: p.rate
            if ((p.status == "ok" || p.status == "manual") && rate != null) {
                Spacer(Modifier.height(8.dp))
                ReceiveLine(rate = rate, amount = amount, quoteCode = p.quote)
            }
            // Sparkline of past rates (last 7 days). Only render when we
            // have at least 2 history points; one point is just a dot.
            if (history.size >= 2 && (p.status == "ok" || p.status == "manual")) {
                Spacer(Modifier.height(10.dp))
                Sparkline(
                    values = history,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                )
            }
            if (p.status == "ok" && p.promoRate != null) {
                Spacer(Modifier.height(10.dp))
                PromoBadge(p.promoRate, p.promoNote, p.quote)
            }
        }
    }
}

@Composable
private fun ReceiveLine(rate: Double, amount: Double, quoteCode: String) {
    val received = rate * amount
    val sym = CURRENCIES[quoteCode]?.symbol ?: ""
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "You receive",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$sym " + "%,.2f".format(received),
            style = MaterialTheme.typography.titleSmall.copy(
                fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
private fun ManualBadge() {
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "MANUAL",
            color = MaterialTheme.colorScheme.tertiary,
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
            "ok", "manual" -> {
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
                    // Pick colors that adapt to current theme — hardcoded dark
                    // green/red lose contrast in dark mode. Compute the
                    // active mode from the background luminance (works
                    // whether the user is on system, light, or dark via
                    // our manual toggle).
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val positive = if (isDark) Color(0xFF6FDBA0) else Color(0xFF1B7B33)
                    val negative = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C)
                    val (label, color) = when {
                        delta > 0.001  -> "+%.4f".format(delta) to positive
                        delta < -0.001 -> "%.4f".format(delta) to negative
                        else -> "= mid-market" to MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = "$label vs mid-market",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = color,
                        fontWeight = FontWeight.Medium,
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
            "investigating" -> {
                if (midMarket != null) {
                    // Honest fallback: when we don't yet have a verified provider
                    // rate, show the mid-market rate as an estimate. The "≈" prefix
                    // and "Estimated · awaiting verification" label make clear
                    // this is not the provider's actual rate.
                    Text(
                        text = "≈ %.4f".format(midMarket),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        "Estimated · awaiting verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.End,
                    )
                } else {
                    StatusDot(Color(0xFF94A3B8))
                }
            }
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
