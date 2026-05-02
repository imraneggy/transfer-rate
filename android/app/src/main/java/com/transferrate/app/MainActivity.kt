package com.transferrate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.transferrate.app.ui.RatesScreen
import com.transferrate.app.ui.ThemeMode
import com.transferrate.app.ui.theme.TransferRateTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
                RatesScreen(
                    themeMode = themeMode,
                    onCycleThemeMode = { themeMode = themeMode.next() },
                )
            }
        }
    }
}
