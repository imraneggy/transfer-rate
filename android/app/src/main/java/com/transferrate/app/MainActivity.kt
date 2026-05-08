package com.transferrate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transferrate.app.data.NotificationCenter
import com.transferrate.app.data.PrefetchScheduler
import com.transferrate.app.ui.AboutScreen
import com.transferrate.app.ui.RatesScreen
import com.transferrate.app.ui.RatesUiState
import com.transferrate.app.ui.RatesViewModel
import com.transferrate.app.ui.SplashScreen
import com.transferrate.app.ui.ThemeMode
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

    Box(modifier = Modifier.fillMaxSize().background(SPLASH_BG)) {
        if (!splashDone) {
            SplashScreen(
                minDurationMs = 1000L,
                isReady = state !is RatesUiState.Loading,
                onDone = { splashDone = true },
            )
        } else {
            Surface(modifier = Modifier.fillMaxSize()) {
                when {
                    showAbout -> AboutScreen(onBack = { showAbout = false })
                    else -> RatesScreen(
                        vm = vm,
                        themeMode = themeMode,
                        onCycleThemeMode = onCycleThemeMode,
                        onShowAbout = { showAbout = true },
                    )
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
