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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transferrate.app.R
import com.transferrate.app.data.CURRENCIES
import com.transferrate.app.data.CurrencyInfo
import com.transferrate.app.data.GoldDocument
import com.transferrate.app.data.ProviderQuote
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import com.transferrate.app.ui.theme.LocalBrandColors
import com.transferrate.app.ui.theme.LocalMetalColors
import com.transferrate.app.ui.theme.LocalSemanticColors

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
                        // Logo on a Deep Navy (#071827) coin in BOTH light
                        // and dark modes — matches TransferRateLogo
                        // (SplashScreen.kt) and the Adaptive Icon
                        // Background from the "infinity DXR" brand sheet.
                        // The symbol's AED/INR glyphs are white, so they
                        // need this dark backing for contrast.
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(LocalBrandColors.current.navy),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = R.drawable.transfer_rate_logo,
                                ),
                                contentDescription = stringResource(R.string.app_name),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(2.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // v0.31.1: AutoSizeText so the wordmark shrinks
                        // rather than ellipsizing to "Transfer ..." on
                        // narrow phones in any locale.  TopAppBar's
                        // default title style is titleLarge (~22 sp).
                        // Min 16 sp keeps it legible alongside the logo.
                        AutoSizeText(
                            text = stringResource(R.string.app_name),
                            fontSize = 22.sp,
                            minFontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                actions = {
                    // Show the icon for the theme currently in effect (sun =
                    // light, moon = dark) rather than a gear for System — users
                    // read the toolbar button as a light/dark toggle, and a
                    // settings gear made it look like it did nothing until
                    // tapped. System resolves to the live system theme.
                    val themeIcon = when (themeMode) {
                        ThemeMode.System ->
                            if (androidx.compose.foundation.isSystemInDarkTheme())
                                R.drawable.ic_dark_mode else R.drawable.ic_light_mode
                        ThemeMode.Light  -> R.drawable.ic_light_mode
                        ThemeMode.Dark   -> R.drawable.ic_dark_mode
                    }
                    IconButton(onClick = onCycleThemeMode) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = themeIcon),
                            contentDescription = "Theme: ${themeMode.label} (tap to cycle)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Refresh rates",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    // v0.31.0: share-best-rate icon.  Composes a short
                    // plain-text payload of today's BEST provider rate +
                    // the AED→INR amount and fires ACTION_SEND through
                    // the system chooser (WhatsApp / SMS / email / etc.).
                    // Icon-only because a fourth labeled chip would push
                    // the title past the ellipsis threshold on 360 dp
                    // phones in Tamil/Malayalam (already tight at 2
                    // chips + 1 icon).  Disabled when state isn't Ready.
                    val context = LocalContext.current
                    val currentState = state
                    IconButton(
                        onClick = {
                            if (currentState is RatesUiState.Ready) {
                                shareBestRate(context, currentState)
                            }
                        },
                        enabled = currentState is RatesUiState.Ready,
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = R.drawable.ic_share,
                            ),
                            contentDescription = "Share today's best rate",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
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
                is RatesUiState.Loading -> SkeletonLoading()
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
/**
 * Compose a one-message summary of today's best provider rate and fire
 * Android's ACTION_SEND chooser so the user can forward it to WhatsApp,
 * SMS, email, or any other text-receiving app.  v0.31.0.
 *
 * Payload structure (en):
 *
 *   🏆 Today's best AED→INR rate
 *
 *   26.0900 via Aspora
 *   You'd get ₹78,270 for AED 3,000
 *   Mid-market: 26.0851
 *
 *   Compare 11 UAE→India providers: https://imraneggy.github.io/transfer-rate/
 *
 * Strings are split into small chunks so translators can re-order phrases
 * naturally rather than wrestling positional args inside one mega-format.
 */
private fun shareBestRate(
    context: android.content.Context,
    state: RatesUiState.Ready,
) {
    val selected = state.selectedCurrency
    val info = CURRENCIES[selected] ?: return
    val visible = state.visibleQuotes
    // First (sortedBy rate-desc) provider with a usable rate.
    val best = visible.firstOrNull {
        (it.status == "ok" || it.status == "manual") &&
            (it.effectiveRate ?: it.rate) != null
    } ?: return
    val bestRate = best.effectiveRate ?: best.rate ?: return
    val amount = state.selectedAmount
    val received = bestRate * amount
    val midMarket = state.midMarketRate

    val rateStr = "%.4f".format(bestRate)
    val receivedStr = "${info.symbol} %,.0f".format(received)
    val amountStr = "%,.0f".format(amount)
    val midStr = midMarket?.let { "%.4f".format(it) }
    val url = context.getString(R.string.share_url)

    val payload = buildString {
        append("🏆 ")
        appendLine(context.getString(R.string.share_title))
        appendLine()
        appendLine(context.getString(
            R.string.share_rate_via_format, rateStr, best.providerName,
        ))
        appendLine(context.getString(
            R.string.share_amount_format, receivedStr, amountStr,
        ))
        if (midStr != null) {
            appendLine(context.getString(R.string.share_midmarket_format, midStr))
        }
        appendLine()
        append(context.getString(R.string.share_footer_format, visible.size, url))
    }

    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, payload)
    }
    val chooserTitle = context.getString(R.string.share_chooser_title)
    val chooser = android.content.Intent.createChooser(sendIntent, chooserTitle).apply {
        // FLAG_ACTIVITY_NEW_TASK required when context isn't an Activity
        // (e.g. system overlays).  Safe to set unconditionally here —
        // Android resolves correctly either way.
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(chooser)
    } catch (_: android.content.ActivityNotFoundException) {
        // No app installed that can receive text/plain — extremely rare
        // (every Android device has at least Messages / Gmail).  Swallow
        // silently rather than crash; the user can retry after installing
        // a target app.
    }
}

/**
 * Locale-aware relative time.
 *
 * v0.32.0: rewritten to use Android's <plurals> resources so each
 * locale picks the correct quantity form per its CLDR rules.  Was
 * previously hardcoded English regardless of in-app language, which
 * meant Tamil users saw "8 minutes ago புதுப்பிக்கப்பட்டது"
 * (mixed-language) and the "minutes/minute" English plural never
 * agreed with the Tamil verb-final word order.
 *
 * Resources used: time_just_now (string), time_minutes_ago,
 * time_hours_ago, time_days_ago (plurals).  Future minor improvement:
 * use DateUtils.getRelativeTimeSpanString() — but that returns
 * platform-specific phrasing which doesn't match the app's "X ago"
 * idiom, and locale coverage outside of major language families is
 * patchy.  Hand-rolled plurals are more predictable here.
 */
@androidx.compose.runtime.Composable
private fun relativeTime(iso: String): String {
    val resources = LocalContext.current.resources
    return try {
        val instant = Instant.parse(iso)
        val seconds = Duration.between(instant, Instant.now()).seconds
        when {
            seconds < 60 -> resources.getString(R.string.time_just_now)
            seconds < 3600 -> {
                val mins = (seconds / 60).toInt()
                resources.getQuantityString(R.plurals.time_minutes_ago, mins, mins)
            }
            seconds < 86400 -> {
                val hours = (seconds / 3600).toInt()
                resources.getQuantityString(R.plurals.time_hours_ago, hours, hours)
            }
            else -> {
                val days = (seconds / 86400).toInt()
                resources.getQuantityString(R.plurals.time_days_ago, days, days)
            }
        }
    } catch (_: DateTimeParseException) {
        iso
    }
}


/**
 * Animated shimmer brush for skeleton placeholders.  A translucent band
 * sweeps left→right across each placeholder block, reading as "content
 * loading" far better than a centred spinner (skill rule: prefer skeleton
 * screens over blocking spinners for operations that may exceed ~1s).
 *
 * The brush translates a soft gradient (surfaceVariant → transparent →
 * surfaceVariant) so the same animation reads correctly on both the OLED
 * true-black dark theme and the paper-light theme without per-theme tuning.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-translate",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    return Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = 0.55f),
            base.copy(alpha = 0.18f),
            base.copy(alpha = 0.55f),
        ),
        start = Offset(translate - 350f, 0f),
        end = Offset(translate, 0f),
    )
}

/** A single rounded shimmer block used to compose skeleton layouts. */
@Composable
private fun SkeletonBox(
    modifier: Modifier = Modifier,
    brush: Brush,
    cornerRadius: Int = 8,
) {
    Box(modifier.background(brush, RoundedCornerShape(cornerRadius.dp)))
}

/**
 * Loading placeholder that mirrors [ReadyView]'s real layout — currency
 * chips, the hero rate + gold row, the amount field, and a stack of
 * provider rows.  Showing the actual content silhouette (rather than a
 * generic spinner) keeps spatial continuity so the screen doesn't visibly
 * reflow when real data arrives.
 */
@Composable
private fun SkeletonLoading() {
    val brush = rememberShimmerBrush()
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Currency chip row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                SkeletonBox(
                    Modifier.height(48.dp).width(72.dp),
                    brush = brush,
                    cornerRadius = 20,
                )
            }
        }
        // Hero rate + gold module row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBox(
                Modifier.weight(1f).height(156.dp),
                brush = brush,
                cornerRadius = 22,
            )
            SkeletonBox(
                Modifier.weight(1f).height(156.dp),
                brush = brush,
                cornerRadius = 22,
            )
        }
        // Amount field
        SkeletonBox(
            Modifier.fillMaxWidth().height(56.dp),
            brush = brush,
            cornerRadius = 12,
        )
        // Metal calculator panel
        SkeletonBox(
            Modifier.fillMaxWidth().height(96.dp),
            brush = brush,
            cornerRadius = 22,
        )
        // Provider rows
        repeat(6) {
            SkeletonProviderRow(brush)
        }
    }
}

/** One provider-card silhouette: avatar circle, two text lines, and the
 *  right-aligned rate block — matching [ProviderCard]'s real geometry. */
@Composable
private fun SkeletonProviderRow(brush: Brush) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBox(Modifier.size(44.dp), brush = brush, cornerRadius = 22)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.55f).height(15.dp), brush = brush)
            SkeletonBox(Modifier.fillMaxWidth(0.35f).height(12.dp), brush = brush)
        }
        Spacer(Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkeletonBox(Modifier.width(72.dp).height(20.dp), brush = brush)
            SkeletonBox(Modifier.width(56.dp).height(14.dp), brush = brush)
        }
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
            stringResource(R.string.error_offline_headline),
            stringResource(R.string.error_offline_hint),
        )
        else -> Triple(
            "⚠",
            stringResource(R.string.error_generic_headline),
            stringResource(R.string.error_generic_hint),
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
            Text(stringResource(R.string.error_try_again))
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
    val gold = state.doc.gold

    // v0.40: the gold module's second column follows the selected corridor's
    // country (UAE vs Pakistan / Egypt / …) using that country's own retail
    // gold + silver from rates.json `gold.secondary[CUR]`.  We swap the
    // GoldDocument's `india` side for the country's side so the existing
    // GoldHeader / GoldHistorySheet render it natively (already in local
    // currency → no conversion).  INR keeps the richer LiveChennai feed, and
    // any corridor whose secondary side is missing/errored falls back to the
    // India side shown honestly as India (₹).
    val secMetals = if (selected != "INR") gold?.secondary?.get(selected) else null
    val useCountryMetals = secMetals?.gold?.status == "ok"
    val effGold = if (gold != null && useCountryMetals)
        gold.copy(india = secMetals!!.gold!!, indiaSilver = secMetals.silver)
    else gold
    val secondCountryLabel = if (useCountryMetals) (info?.country ?: selected) else "India"
    val secondCountrySymbol = if (useCountryMetals) (info?.symbol ?: "₹") else "₹"

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
                        gold = effGold ?: gold,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onClick = { goldSheetOpen = true },
                        secondaryCurrencySymbol = secondCountrySymbol,
                        // effGold's second side is already in local currency
                        // (or India ₹ on fallback) → no conversion.
                        secondaryConversionRate = 1.0,
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
        item {
            MetalCalculatorPanel(
                amount = state.selectedAmount,
                gold = gold,
            )
        }
        items(state.visibleQuotes, key = { "${selected}-${it.providerId}" }) { p ->
            val isBest = (p.status == "ok" || p.status == "manual")
                    && state.bestRate != null
                    && (p.effectiveRate ?: p.rate) == state.bestRate
            // history.json only tracks the AED->INR corridor today (see
            // run_all.py _append_to_history) — don't show INR sparklines/
            // trend arrows while viewing another corridor's rates.
            val historyForProvider = if (selected == "INR") {
                state.history?.providers?.get(p.providerId)?.map { it.rate } ?: emptyList()
            } else {
                emptyList()
            }
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
        // Same INR-only history scoping as the list view above.
        val historyPoints = if (selected == "INR") {
            state.history?.providers?.get(p.providerId).orEmpty()
        } else {
            emptyList()
        }
        ProviderHistorySheet(
            provider = p,
            history = historyPoints,
            midMarket = midMarket,
            onDismiss = { sheetForProvider = null },
        )
    }

    // Gold history sheet
    if (goldSheetOpen) {
        (effGold ?: state.doc.gold)?.let { g ->
            GoldHistorySheet(
                gold = g,
                onDismiss = { goldSheetOpen = false },
                secondaryCurrencySymbol = secondCountrySymbol,
                // g's second side is already in local currency → no convert.
                secondaryConversionRate = 1.0,
                secondaryLabel = secondCountryLabel,
                secondaryCode = selected,
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
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            WelcomeBullet(
                emoji = "💱",
                title = stringResource(R.string.welcome_bullet_rates_title),
                body = stringResource(R.string.welcome_bullet_rates_body),
            )
            WelcomeBullet(
                emoji = "📊",
                title = stringResource(R.string.welcome_bullet_midmarket_title),
                body = stringResource(R.string.welcome_bullet_midmarket_body),
            )
            WelcomeBullet(
                emoji = "🪙",
                title = stringResource(R.string.welcome_bullet_metals_title),
                body = stringResource(R.string.welcome_bullet_metals_body),
            )
            WelcomeBullet(
                emoji = "↻",
                title = stringResource(R.string.welcome_bullet_refresh_title),
                body = stringResource(R.string.welcome_bullet_refresh_body),
            )
            WelcomeBullet(
                emoji = "🔔",
                title = stringResource(R.string.welcome_bullet_alerts_title),
                body = stringResource(R.string.welcome_bullet_alerts_body),
            )
            WelcomeBullet(
                emoji = "🔒",
                title = stringResource(R.string.welcome_bullet_privacy_title),
                body = stringResource(R.string.welcome_bullet_privacy_body),
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
                    text = stringResource(R.string.welcome_got_it),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.welcome_revisit_hint),
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // v0.33 facelift: hero shape bumped 18dp -> 22dp for a softer
    // silhouette, plus a depth treatment per theme — light mode gets a
    // soft primary-tinted shadow (cards "float"), dark mode (OLED true
    // black has no usable shadow) gets a subtle light-catch gradient
    // border instead.
    val heroShape = RoundedCornerShape(22.dp)
    val depthMod = if (isDark) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    Color.Transparent,
                ),
            ),
            shape = heroShape,
        )
    } else {
        Modifier.shadow(
            elevation = 6.dp,
            shape = heroShape,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        )
    }
    val baseMod = modifier
        .fillMaxWidth()
        .heightIn(min = 156.dp)
        .then(depthMod)
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
        shape = heroShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        // v0.33 facelift: soft radial glow behind the hero rate —
        // primary-tinted, fades to transparent so the existing
        // surfaceVariant background shows through at the edges.
        val glowAlpha = if (isDark) 0.22f else 0.14f
        val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
        Column(
            Modifier
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(
                                size.width * 0.28f,
                                size.height * 0.32f,
                            ),
                            radius = size.maxDimension * 0.85f,
                        ),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            // Eyebrow label — primary-coloured to retain brand pop.
            // AutoSizeText: Tamil "மிட்-மார்க்கெட்" + Hindi "मिड-मार्केट"
            // + Malayalam "മിഡ്-മാർക്കറ്റ്" are all wider than English
            // "MID-MARKET" at 11 sp; shrink rather than ellipsise.
            Row(verticalAlignment = Alignment.CenterVertically) {
                AutoSizeText(
                    text = stringResource(R.string.midmarket_eyebrow),
                    fontSize = 11.sp,
                    minFontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp,
                    color = MaterialTheme.colorScheme.primary,
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

            // v0.32.0: AutoSizeText because Tamil
            // "8 நிமிடங்கள் முன்பு புதுப்பிக்கப்பட்டது" (the now-localised
            // relativeTime + last_updated combo) can reach ~30 glyphs
            // which overflows the eyebrow area on 360 dp phones.
            AutoSizeText(
                text = stringResource(R.string.last_updated, relativeTime(completedAt)),
                fontSize = 10.sp,
                minFontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "currency-chip-bg",
    )
    val fgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "currency-chip-fg",
    )
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
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
 * Send-amount input panel.
 *
 * The text field updates the calculated receive amounts as the user types,
 * without a separate Set button. Quick-pick chips cover the requested common
 * send amounts: 500, 1,000, 4,000, 6,000, and 10,000 AED.
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

    LaunchedEffect(amount) {
        val currentText = fieldValue.text.replace(",", "").trim()
        val currentNum = currentText.toDoubleOrNull()
        if (currentNum != amount) {
            fieldValue = TextFieldValue(formatAmount(amount))
        }
    }

    fun applyTypedAmount(value: TextFieldValue) {
        fieldValue = value
        val cleaned = value.text.replace(",", "").trim()
        val n = cleaned.toDoubleOrNull()
        if (n != null && n in 1.0..1_000_000.0 && n != amount) {
            onAmountChange(n)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = ::applyTypedAmount,
            label = { Text(stringResource(R.string.amount_label_sending)) },
            prefix = { Text(stringResource(R.string.amount_input_prefix)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() },
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(500.0, 1000.0, 4000.0, 6000.0, 10_000.0)) { v ->
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
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "amount-chip-bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "amount-chip-fg",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
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

/** Which metal/karat the [MetalCalculatorPanel] is currently converting to. */
private enum class MetalCalcOption(val labelRes: Int) {
    Gold24k(R.string.metals_calc_option_gold_24k),
    Gold22k(R.string.metals_calc_option_gold_22k),
    Silver(R.string.metals_calc_option_silver),
}

/**
 * "What this buys" gold/silver calculator — split out of the amount
 * section in v0.34.0 so the AED amount entered in [AmountPanel] above
 * can also be read as grams of metal at today's UAE rate.
 *
 * Tapping a chip (24K gold / 22K gold / silver) divides the entered AED
 * amount by that metal's UAE per-gram rate from [GoldDocument]. Options
 * whose rate is unavailable are omitted from the chip row; if none are
 * available the whole card is hidden (mirrors [GoldHeader]'s graceful
 * degradation).
 */
@Composable
private fun MetalCalculatorPanel(
    amount: Double,
    gold: GoldDocument?,
    modifier: Modifier = Modifier,
) {
    val goldOk = gold?.uae?.status == "ok"
    val silverOk = gold?.uaeSilver?.status == "ok" && gold.uaeSilver?.perG != null

    val options = buildList {
        if (goldOk && gold?.uae?.perG24k != null) add(MetalCalcOption.Gold24k)
        if (goldOk && gold?.uae?.perG22k != null) add(MetalCalcOption.Gold22k)
        if (silverOk) add(MetalCalcOption.Silver)
    }
    if (options.isEmpty()) return

    var selected by remember { mutableStateOf(options.first()) }
    if (selected !in options) selected = options.first()

    val metals = LocalMetalColors.current
    val perGram = when (selected) {
        MetalCalcOption.Gold24k -> gold?.uae?.perG24k
        MetalCalcOption.Gold22k -> gold?.uae?.perG22k
        MetalCalcOption.Silver -> gold?.uaeSilver?.perG
    }
    val grams = perGram?.takeIf { it > 0.0 }?.let { amount / it }
    val isSilver = selected == MetalCalcOption.Silver
    val accentColor = if (isSilver) metals.silverText else metals.goldText
    val valueColor = if (isSilver) metals.silverDeep else metals.goldDeep

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = when (selected) {
                    MetalCalcOption.Gold24k -> stringResource(R.string.metals_calc_option_gold_24k)
                    MetalCalcOption.Gold22k -> stringResource(R.string.metals_calc_option_gold_22k)
                    MetalCalcOption.Silver  -> stringResource(R.string.metals_calc_option_silver)
                }.uppercase() + " · " + stringResource(R.string.metals_calc_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    AmountChip(
                        label = stringResource(option.labelRes),
                        selected = (option == selected),
                        onClick = { selected = option },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (grams != null && perGram != null) {
                Text(
                    text = "%,.2f g".format(grams),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = valueColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.metals_calc_rate_format,
                        "%,.2f".format(perGram),
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            } else {
                Text(
                    text = stringResource(R.string.metals_calc_unavailable),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
    // v0.33 facelift: corner radius bumped 16dp -> 20dp for a softer
    // silhouette, matching the hero/gold header cards.
    val cardShape = RoundedCornerShape(20.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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
            shape = cardShape,
        )
    } else if (isDark) {
        // v0.33 facelift: OLED black has no usable drop shadow, so
        // non-BEST cards get a subtle light-catch gradient border
        // (top-left lighter, fading out) for a glassy, dimensional edge.
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    Color.Transparent,
                ),
            ),
            shape = cardShape,
        )
    } else Modifier
    // v0.33 facelift: light mode cards get a soft primary-tinted shadow
    // so they visually "float" off the paper background — BEST cards
    // float a bit more than regular ones.
    val shadowMod = if (!isDark) {
        Modifier.shadow(
            elevation = if (isBest) 6.dp else 3.dp,
            shape = cardShape,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        )
    } else Modifier

    Card(
        modifier = Modifier.fillMaxWidth().then(shadowMod).then(border),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark && isBest) 2.dp else 0.dp),
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
                            // v0.30.8: was maxLines=1/softWrap=false which
                            // forced "Aspora" → "A..." in Tamil/Malayalam
                            // where the verbose vs-mid line pushed the
                            // right column wide enough to leave <30 dp for
                            // the name column on 360 dp phones.  vs-mid is
                            // also being shortened in those locales (~70 dp
                            // recovered) but the 2-line wrap is the proper
                            // defense — provider names should NEVER clip,
                            // they're the identity the user is comparing.
                            maxLines = 2,
                            softWrap = true,
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
                            "stale" -> stringResource(R.string.status_stale)
                            "investigating" -> stringResource(R.string.status_investigating)
                            else -> stringResource(R.string.status_error)
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
                // v0.30: receive-amount as headline.  RateView now takes
                // `amount` so the right column can render "₹ 77,460" big
                // (the number users actually care about) instead of the
                // raw rate.  Rate stays available as a small detail line.
                // v0.31: also threads `history` into RateView so the
                // rate can carry a ▲/▼ trend arrow comparing today's
                // rate against the rolling 7-day average.
                RateView(p, midMarket = midMarket, amount = amount, history = history)
            }
            AnimatedVisibility(visible = p.status == "ok" && p.promoRate != null) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    if (p.promoRate != null) {
                        PromoBadge(p.promoRate, p.promoNote, p.quote)
                    }
                }
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
        // maxLines = 1 + softWrap = false: defends against the
        // "BES + T" wrapping bug seen on narrow phones when the
        // provider-name column was long enough to crush the badge.
        Text(
            text = stringResource(R.string.badge_best),
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
                MaterialTheme.colorScheme.tertiaryContainer,
                RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(R.string.badge_manual),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun RateView(
    p: ProviderQuote,
    midMarket: Double?,
    amount: Double,
    history: List<Double>,
) {
    val rate = p.effectiveRate ?: p.rate
    val symbol = CURRENCIES[p.quote]?.symbol ?: ""
    val semantic = LocalSemanticColors.current
    val positive = semantic.positive
    val negative = semantic.negative

    // v0.31.0: rolling-7-day trend arrow.  Computed against the
    // provider's 7-day rate history.
    // Threshold 0.1% (10 bps) matches the existing vs-mid threshold
    // (`delta > 0.001` on a rate around 26 = ~10 bps) so the two
    // indicators share a consistency: a ▲ here roughly corresponds in
    // magnitude to a visible "+0.0xxx vs mid" line.  Flat values get
    // no glyph (rather than a "▬" or similar) to avoid visual noise on
    // the ~half of cards where the rate is currently mid-band.
    val trendGlyph: Pair<String, Color>? = if (rate != null && history.size >= 2) {
        val avg = history.average()
        val delta = rate - avg
        val threshold = avg * 0.001
        when {
            delta > threshold  -> "▲" to positive
            delta < -threshold -> "▼" to negative
            else -> null
        }
    } else null

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.widthIn(min = 90.dp),
    ) {
        when (p.status) {
            "ok", "manual" -> {
                // v0.30.5: rate is the headline again (top), received
                // amount sits below at 16sp SemiBold full-contrast.  v0.30.0
                // had flipped this (₹ amount on top) but in practice users
                // scan the rate first to compare providers, then check
                // "for my AED, how many ₹?" once they've picked the winner.
                // So: 1) rate big (the comparison axis), 2) ₹ amount the
                // immediate consequence, 3) vs-mid the tertiary context.
                val received = if (rate != null) rate * amount else null
                Row(verticalAlignment = Alignment.Bottom) {
                    trendGlyph?.let { (glyph, color) ->
                        Text(
                            text = glyph,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = if (rate != null) "%.4f".format(rate) else "—",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (received != null) {
                    val receivedStr = if (received >= 100) "%,.0f".format(received)
                                      else "%,.2f".format(received)
                    Text(
                        text = "$symbol $receivedStr",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (rate != null && midMarket != null) {
                    val delta = rate - midMarket
                    // v0.31.0: `isDark`/`positive`/`negative` moved to the
                    // outer function scope so the trend arrow + vs-mid line
                    // share one palette decision per card.
                    val (label, color) = when {
                        delta > 0.001  -> "+%.4f".format(delta) to positive
                        delta < -0.001 -> "%.4f".format(delta) to negative
                        else -> stringResource(R.string.rate_equals_mid) to
                            MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val display = if (delta > 0.001 || delta < -0.001) {
                        stringResource(R.string.rate_vs_mid_format, label)
                    } else label
                    Text(
                        text = display,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        color = color,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            "stale" -> {
                val received = if (rate != null) rate * amount else null
                Text(
                    text = if (rate != null) "%.4f".format(rate) else "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = "tnum",
                    ),
                )
                if (received != null) {
                    Text(
                        text = "$symbol %,.0f".format(received),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Text(
                    stringResource(R.string.status_stale_short),
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
                        stringResource(R.string.status_estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                } else {
                    StatusDot(MaterialTheme.colorScheme.onSurfaceVariant)
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
