package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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

    /** Human label used in the IconButton's contentDescription. */
    val label: String get() = when (this) {
        System -> "system"
        Light -> "light"
        Dark -> "dark"
    }

    /** Legacy glyph (kept for any non-icon callers; prefer Icons.Outlined). */
    @Deprecated("Use Icons.Outlined Settings/LightMode/DarkMode instead.")
    val glyph: String get() = when (this) {
        System -> "⚙"
        Light -> "☀"
        Dark -> "☾"
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
                    // App logo + wordmark.  maxLines=1 + ellipsize on
                    // the text defends against the (rare) case where
                    // the actions row gets so wide it pushes the title
                    // off-screen on a 360 dp phone.  Currently 3 chips
                    // + ⓘ leaves ~120 dp for the title which fits the
                    // logo + "Transfer Rate" cleanly.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Logo on a NEUTRAL near-white coin in BOTH light
                        // and dark modes.  The brand mark's own dark-navy
                        // + teal palette is what reads as "this is the
                        // logo"; tinting the coin (as in v0.29.x) muted
                        // those colours.  Apple/Stripe pattern: brand
                        // marks always sit on a neutral container.
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(0xFFFFFFFF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = R.drawable.transfer_rate_logo,
                                ),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(2.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    // Word-labeled toolbar chips per user request.
                    // Compact size + uppercase labels keeps all three
                    // visible alongside the "Transfer Rate" title even on
                    // narrow phones. About stays as a small ⓘ glyph
                    // (the word "ABOUT" would push past the title on
                    // 360dp screens; the glyph is still tappable and
                    // its content description is read aloud).
                    val themeLabel = when (themeMode) {
                        ThemeMode.System -> "AUTO"
                        ThemeMode.Light  -> "LIGHT"
                        ThemeMode.Dark   -> "DARK"
                    }
                    ToolbarChip(
                        label = themeLabel,
                        contentDescription = "Theme: ${themeMode.label} (tap to cycle)",
                        onClick = onCycleThemeMode,
                    )
                    ToolbarChip(
                        label = "REFRESH",
                        contentDescription = "Refresh rates",
                        onClick = { vm.refresh() },
                    )
                    IconButton(onClick = onShowAbout) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = R.drawable.ic_info_outline,
                            ),
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onBackground,
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

/**
 * Compact text chip used in the top app bar for the theme toggle and
 * the refresh action. Matches the height of an IconButton so the bar
 * stays a uniform 56 dp; tight horizontal padding keeps three chips
 * + the "Transfer Rate" title visible on a 360 dp screen.
 *
 * Uses an outlined surface treatment so the chips read as tappable
 * controls without being as visually heavy as filled buttons (which
 * would compete with the BEST badge for the user's eye).
 */
@Composable
private fun ToolbarChip(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .heightIn(min = 32.dp)
            .border(
                width = 1.dp,
                color = border,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(
                onClick = onClick,
                onClickLabel = contentDescription,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            softWrap = false,
        )
    }
}

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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val selected = state.selectedCurrency
    val info = CURRENCIES[selected]
    val midMarket = state.midMarketRate

    // The provider whose history sheet is currently open (null = no sheet).
    var sheetForProvider by remember { mutableStateOf<ProviderQuote?>(null) }
    var goldSheetOpen by remember { mutableStateOf(false) }

    // First-launch welcome modal — shown once until the user dismisses it.
    // Bumped to v2 in v0.29.2 because the in-card hint became a richer
    // ModalBottomSheet covering the full feature set; users who dismissed
    // the v1 hint still see the v2 once.
    val ctx0 = LocalContext.current
    val welcomePrefs = remember {
        ctx0.getSharedPreferences("transfer-rate", android.content.Context.MODE_PRIVATE)
    }
    var welcomeOpen by remember {
        mutableStateOf(!welcomePrefs.getBoolean("welcome_dismissed_v2", false))
    }

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
            // 50/50 row: mid-market FX rate (left) + gold rate module (right).
            // IntrinsicSize.Min on the parent + fillMaxHeight on each child
            // forces both cards to the same height — matches whichever
            // module's content is taller. When the gold module is
            // unavailable the FX header takes full width.
            val gold = state.doc.gold
            val midMarketQuote = state.midMarketQuote
            val onMidMarketClick: (() -> Unit)? = midMarketQuote?.let { q ->
                { sheetForProvider = q }
            }
            if (gold != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    MidMarketHeader(
                        info = info,
                        midMarket = midMarket,
                        completedAt = state.doc.completedAt,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = onMidMarketClick,
                    )
                    GoldHeader(
                        gold = gold,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { goldSheetOpen = true },
                    )
                }
            } else {
                MidMarketHeader(
                    info = info,
                    midMarket = midMarket,
                    completedAt = state.doc.completedAt,
                    onClick = onMidMarketClick,
                )
            }
        }
        // (FirstLaunchHint card removed in v0.29.2 — replaced by the
        // WelcomeSheet modal rendered at the bottom of this composable.)
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
                onClick = { sheetForProvider = p },
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    // History bottom sheet for the tapped provider.
    sheetForProvider?.let { p ->
        val historyPoints = state.history?.providers?.get(p.providerId).orEmpty()
        ProviderHistorySheet(
            provider = p,
            history = historyPoints,
            midMarket = midMarket,
            onDismiss = { sheetForProvider = null },
        )
    }

    // Gold history sheet
    if (goldSheetOpen) {
        state.doc.gold?.let { g ->
            GoldHistorySheet(
                gold = g,
                onDismiss = { goldSheetOpen = false },
            )
        }
    }

    // First-launch welcome modal (v0.29.2).  Renders over the home
    // screen on first cold start until the user taps "Got it".
    if (welcomeOpen) {
        WelcomeSheet(
            onDismiss = {
                welcomePrefs.edit().putBoolean("welcome_dismissed_v2", true).apply()
                welcomeOpen = false
            },
        )
    }
}

/**
 * First-launch welcome sheet (v0.29.2 onward, replacing the in-list
 * FirstLaunchHint card).  Walks new users through the app's full
 * feature set in a single ModalBottomSheet — mid-market vs provider
 * rates, gold/silver section, refresh button, daily-high alerts,
 * privacy posture.  Shown once per install; dismissal is persisted in
 * SharedPreferences (`welcome_dismissed_v2`) by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeSheet(onDismiss: () -> Unit) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Welcome to Transfer Rate",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "A free, open-source comparison app for sending money " +
                    "from the UAE to India. No accounts, no ads, no analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            WelcomeBullet(
                emoji = "💱",
                title = "Live remittance rates",
                body = "Up to twelve UAE→India providers compared side-by-side: " +
                    "Wise, Aspora, Remitly, TransferGo, Al Ansari, Al Dahab, " +
                    "Ahalia, Federal, GCC, Index, Lari, LuLu. The provider " +
                    "giving you the most rupees gets a BEST badge.",
            )
            WelcomeBullet(
                emoji = "📊",
                title = "Mid-market benchmark",
                body = "The big number at the top is the wholesale interbank " +
                    "rate (Google Finance) — every provider charges some " +
                    "markup over it. Each card shows the markup explicitly " +
                    "so the comparison is honest.",
            )
            WelcomeBullet(
                emoji = "🪙",
                title = "Gold & silver",
                body = "Tap the gold/silver card on home for live UAE (Khaleej " +
                    "Times) and India (LiveChennai) rates — 24K + 22K gold, " +
                    "silver per gram and per kilogram, plus 30-day history.",
            )
            WelcomeBullet(
                emoji = "↻",
                title = "Refresh button",
                body = "The refresh button in the top bar pulls fresh rates on " +
                    "demand. The app already shows the last cron-published " +
                    "rates within a second; truly-fresh upstream rates land " +
                    "silently 30–45 seconds later.",
            )
            WelcomeBullet(
                emoji = "🔔",
                title = "Daily-high alerts (default ON)",
                body = "A status-bar notification fires when a provider beats " +
                    "today's previous best AED→INR rate. Toggle off any time " +
                    "in About → Notifications. Permission is asked once on " +
                    "first launch.",
            )
            WelcomeBullet(
                emoji = "🔒",
                title = "Private by design",
                body = "Two outbound hosts only (GitHub Pages + Cloudflare " +
                    "Worker), no telemetry, no analytics, no Google Play " +
                    "Services. Source code on GitHub.",
            )

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Got it",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "You can re-read this from the About screen any time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** One row of the welcome sheet — emoji icon on the left, bold title +
 *  description body on the right.  Matches the `SectionCard` style used
 *  on the About page for visual consistency. */
@Composable
private fun WelcomeBullet(emoji: String, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = emoji,
            fontSize = 22.sp,
            modifier = Modifier
                .padding(end = 14.dp, top = 2.dp)
                .width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    // Compact version designed to sit at half-width next to GoldHeader.
    // Fixed height + tight padding so both modules visually align.
    // When onClick is provided (mid-market history is available), the
    // entire card becomes tappable to open the 30-day history sheet.
    val baseMod = modifier
        .fillMaxWidth()
        .heightIn(min = 156.dp)
    // v0.27.1: bg switched from primaryContainer (saturated pale indigo)
    // to surfaceVariant (near-neutral slate) and the body text from
    // onPrimaryContainer (deep indigo, same hue as old bg → muddy) to
    // onSurface (deep navy, distinct hue from bg → crisp).  The eyebrow
    // "MID-MARKET" stays in primary colour so the brand identity reads
    // as a tinted accent rather than a tinted slab.
    Card(
        modifier = if (onClick != null) baseMod.clickable(
            onClick = onClick,
            onClickLabel = "Show mid-market 30-day history",
        ) else baseMod,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Eyebrow label — primary-coloured to retain brand pop
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "MID-MARKET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(6.dp))
                if (info != null) {
                    Text(text = info.flag, fontSize = 13.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))

            // Hero rate — large, bold, full-contrast neutral
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (midMarket != null) "%.4f".format(midMarket) else "—",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(6.dp))
                if (info != null) {
                    Text(
                        text = info.symbol,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (info != null) "1 AED → ${info.code}" else "1 AED → ${'$'}rate",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Updated ${relativeTime(completedAt)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
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
        // Trailing "Set" affordance inside the field — visible button so
        // users on phones know the amount can be confirmed without
        // dismissing the keyboard via the system Done key.  Tapping it
        // commits the amount and removes focus (closes the keyboard).
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { fieldValue = it },
            label = { Text("Sending") },
            prefix = { Text("AED ") },
            trailingIcon = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        commitAmount(fieldValue.text)
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text(
                        "Set",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
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
        // LazyRow so chips can scroll horizontally on narrow screens.
        // A regular Row with 5 chips overflowed at ~360dp width and the
        // "50,000" label wrapped to two lines.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(1000.0, 5000.0, 10_000.0, 25_000.0, 50_000.0)) { v ->
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
        // softWrap = false + maxLines = 1: defense in depth so the chip
        // renders "50,000" as a single line even if the parent constrains
        // width tighter than the text would otherwise need.
        Text(
            text = label,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false,
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
    // BEST card carries the winning provider's brand tint so the
    // identity reads at a glance (was a generic indigo secondary
    // container in v0.27.x — visually identifiable as "best" but did
    // not say *which* provider).  bestCardTintFor() defines the
    // per-provider light/dark mode tint and falls back to the prior
    // dual-tone indigo for unknown providers.
    val containerColor = when {
        isBest -> bestCardTintFor(p.providerId)
        p.status == "ok" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    // BEST border matches the brand tint at a deeper saturation so the
    // card edge is unmistakable without overpowering the fill.
    val border = if (isBest) {
        val tint = bestCardTintFor(p.providerId)
        val borderShade = androidx.compose.ui.graphics.Color(
            red = (tint.red * 0.65f).coerceIn(0f, 1f),
            green = (tint.green * 0.65f).coerceIn(0f, 1f),
            blue = (tint.blue * 0.65f).coerceIn(0f, 1f),
            alpha = 1f,
        )
        Modifier.border(
            width = 1.5.dp,
            color = borderShade,
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
                        // weight(1f) + maxLines=1 + ellipsize so the
                        // BEST/MANUAL badge always has room next to a
                        // long provider name on narrow screens.
                        Text(
                            p.providerName,
                            fontWeight = FontWeight.SemiBold,
                            // 15sp (was 16sp) — extra horizontal breathing
                            // room so longer names like "Wall Street Exchange"
                            // and "Index Exchange" still fit alongside the
                            // BEST badge even at the 1.15x font-scale cap.
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    // 36 dp gives the v0.25 thicker stroke + fill gradient
                    // adequate plot height (28 dp clipped the highlight
                    // halo because the new Sparkline reserves more inset).
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
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
        // maxLines = 1 + softWrap = false: defends against the
        // "BES + T" wrapping bug seen on narrow phones when the
        // provider-name column was long enough to crush the badge.
        Text(
            text = "BEST",
            color = MaterialTheme.colorScheme.onSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            softWrap = false,
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
            maxLines = 1,
            softWrap = false,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                )
                Text(
                    "stale",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        "Estimated · awaiting verification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
