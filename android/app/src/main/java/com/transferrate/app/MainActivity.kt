package com.transferrate.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.transferrate.app.data.BillingRepository
import com.transferrate.app.data.NotificationCenter
import com.transferrate.app.data.NotificationPrefs
import com.transferrate.app.data.PrefetchScheduler
import com.transferrate.app.data.UserProfile
import com.transferrate.app.ui.AboutScreen
import com.transferrate.app.ui.RatesScreen
import com.transferrate.app.ui.RatesUiState
import com.transferrate.app.ui.DynamicAccentTheme
import com.transferrate.app.ui.RatesViewModel
import com.transferrate.app.ui.SplashScreen
import com.transferrate.app.ui.ThemeMode
import com.transferrate.app.ui.UpgradeScreen
import com.transferrate.app.ui.theme.TransferRateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Schedule the periodic prefetch worker. Idempotent — KEEP policy
        // means calling this every launch doesn't reset the schedule clock.
        PrefetchScheduler.schedule(this)

        // Register the daily-high notification channel up-front (idempotent)
        // so users see it under Android Settings → Apps → Transfer Rate →
        // Notifications even before any notification has fired. We do NOT
        // request POST_NOTIFICATIONS here — that prompt only appears when
        // the user explicitly enables the toggle in About → Notifications.
        NotificationCenter.ensureChannel(this)

        setContent {
            // Theme mode state — System (default), Light, Dark.
            // rememberSaveable persists across rotation and process death.
            var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }

            val isDark = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            TransferRateTheme(darkTheme = isDark) {
                // Cap user font scale at 1.15x within the app.  The dashboard
                // packs a provider name + BEST/MANUAL badge + rate + delta
                // into one row; at the platform default 1.0x scale the
                // layout fits comfortably, but Android's accessibility
                // settings let users set fontScale up to 2.0x — at which
                // point "Aspora" wraps to "Asp\nora" or ellipsizes to
                // "Asp..." in the rates list.  Capping at 1.15x preserves
                // a meaningful portion of the accessibility scale-up
                // (15% larger text is still helpful) while keeping the
                // dashboard layout intact across all phones.  Users who
                // need text larger than 1.15x can still use system-level
                // magnification gestures, which scale the whole UI rather
                // than just text.
                val baseDensity = LocalDensity.current
                val cappedDensity = Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale.coerceAtMost(1.15f),
                )
                CompositionLocalProvider(LocalDensity provides cappedDensity) {
                    AppRoot(themeMode = themeMode, onCycleThemeMode = { themeMode = themeMode.next() })
                }
            }
        }
    }
}

/**
 * Top-level composable that decides whether to show the splash screen or
 * the main rates UI.
 *
 * Splash policy (per user preference): show on every app launch, including
 * warm starts. We observe the Activity lifecycle and reset `splashDone`
 * each time the app comes back to the foreground (ON_START). On warm
 * starts the ViewModel already holds Ready state, so the splash dismisses
 * at the 1-second minimum without an additional network round-trip.
 *
 * splashDone is a plain `remember` (not `rememberSaveable`) so that
 * activity-recreation events (configuration changes, themed restoration)
 * don't carry the dismissed state over and skip the splash on resume.
 */
@Composable
private fun AppRoot(
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit,
) {
    val vm: RatesViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var splashDone by remember { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var showUpgrade by rememberSaveable { mutableStateOf(false) }

    val ctxForBilling = androidx.compose.ui.platform.LocalContext.current
    val billing = remember { BillingRepository(ctxForBilling) }
    val userProfile = remember { UserProfile(ctxForBilling) }
    val isProActive by billing.isProActive.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { billing.connect() }

    // First-launch POST_NOTIFICATIONS prompt for daily-high alerts.
    // v0.29.2: NotificationPrefs.dailyHighEnabled now defaults to true,
    // so on a fresh install we ask for notification permission once.
    // Three terminal states:
    //   * granted   → keep dailyHighEnabled=true; alerts work
    //   * denied    → flip dailyHighEnabled=false so the About toggle
    //                 reflects reality (the user can re-enable from
    //                 Android Settings → Apps → Transfer Rate)
    //   * permanently dismissed → same as denied
    // We set permissionRequested=true regardless so we never re-prompt
    // (Android's "permanently denied" path is harsh UX — one ask only).
    val ctxForPerms = androidx.compose.ui.platform.LocalContext.current
    val notifPrefs = remember { NotificationPrefs(ctxForPerms) }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notifPrefs.permissionRequested = true
        if (granted) {
            NotificationCenter.ensureChannel(ctxForPerms)
        } else {
            notifPrefs.dailyHighEnabled = false
        }
    }
    LaunchedEffect(Unit) {
        val alreadyAsked = notifPrefs.permissionRequested
        val wantsAlerts = notifPrefs.dailyHighEnabled
        val alreadyGranted = ContextCompat.checkSelfPermission(
            ctxForPerms, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyAsked && wantsAlerts && !alreadyGranted) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Reset the splash on every foreground transition so users see it
    // each time they (re)open the app, not just on cold start.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                splashDone = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // v0.42: app-wide accent follows the current BEST provider's brand colour.
    val bestProviderId = (state as? RatesUiState.Ready)?.bestProviderId
    DynamicAccentTheme(providerId = bestProviderId) {
    Box(modifier = Modifier.fillMaxSize().background(SPLASH_BG)) {
        if (!splashDone) {
            SplashScreen(
                minDurationMs = 3100L,
                isReady = state !is RatesUiState.Loading,
                onDone = { splashDone = true },
            )
        } else {
            Surface(modifier = Modifier.fillMaxSize()) {
                when {
                    showUpgrade -> UpgradeScreen(
                        isPro = isProActive,
                        priceString = billing.priceString(),
                        onUpgrade = {
                            val activity = ctxForBilling as? android.app.Activity
                            if (activity != null) billing.launchBillingFlow(activity)
                        },
                        onBack = { showUpgrade = false },
                    )
                    showAbout -> AboutScreen(
                        onBack = { showAbout = false },
                        isProActive = isProActive,
                        onShowUpgrade = { showUpgrade = true },
                    )
                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            RatesScreen(
                                vm = vm,
                                themeMode = themeMode,
                                onCycleThemeMode = onCycleThemeMode,
                                onShowAbout = { showAbout = true },
                            )
                            val ready = state as? RatesUiState.Ready
                            if (ready != null) {
                                FreshnessBanner(
                                    completedAt = ready.doc.completedAt,
                                    onRefresh = { vm.refresh() },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// Matches @color/splash_bg in resources so the OS splash and the in-app
// splash present an identical background; transition between them is
// invisible to the user. Transfer Rate brand soft white for the
// white-icon variant per the v0.17 brand refresh.
private val SPLASH_BG = Color(0xFFF1F4F8)
