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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transferrate.app.data.PrefetchScheduler
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
                AppRoot(themeMode = themeMode, onCycleThemeMode = { themeMode = themeMode.next() })
            }
        }
    }
}

/**
 * Top-level composable that decides whether to show the splash screen or
 * the main rates UI. Splash holds until BOTH:
 *   - At least 1 second has elapsed (so the brand registers), AND
 *   - The first JSON fetch has resolved (success OR failure)
 *
 * On warm starts (process kept alive in background), the ViewModel still
 * holds Ready state, so the splash dismisses at the 1-second minimum
 * without waiting for a network round-trip.
 */
@Composable
private fun AppRoot(
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit,
) {
    val vm: RatesViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var splashDone by rememberSaveable { mutableStateOf(false) }

    // The teal brand background is shown beneath the splash so any
    // transition flicker matches the OS splash colour.
    Box(modifier = Modifier.fillMaxSize().background(SPLASH_BG)) {
        if (!splashDone) {
            SplashScreen(
                minDurationMs = 1000L,
                isReady = state !is RatesUiState.Loading,
                onDone = { splashDone = true },
            )
        } else {
            Surface(modifier = Modifier.fillMaxSize()) {
                RatesScreen(
                    vm = vm,
                    themeMode = themeMode,
                    onCycleThemeMode = onCycleThemeMode,
                )
            }
        }
    }
}

// Matches @color/splash_bg in resources so the OS splash and the in-app
// splash present an identical background; transition between them is
// invisible to the user.
private val SPLASH_BG = Color(0xFF00665B)
