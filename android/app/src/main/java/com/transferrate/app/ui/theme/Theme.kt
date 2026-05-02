package com.transferrate.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Custom palette tuned for a financial comparison app.
 * The accent (primary) is a deep teal — evokes trust/banking without
 * looking generically corporate. Secondary is a warm gold for the
 * "Best rate" highlight. Neutrals are warm-leaning for readability.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF00665B),         // deep teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB0F0E2),
    onPrimaryContainer = Color(0xFF002019),

    secondary = Color(0xFF815400),       // warm gold (BEST badge)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF291800),

    tertiary = Color(0xFF8B4789),         // accent for promo badge
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8F4),
    onTertiaryContainer = Color(0xFF370041),

    background = Color(0xFFFCFAF7),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFCFAF7),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFF1EFEA),
    onSurfaceVariant = Color(0xFF44483F),
    outline = Color(0xFF767870),
    outlineVariant = Color(0xFFC6C7BE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5C5),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005046),
    onPrimaryContainer = Color(0xFFB0F0E2),

    secondary = Color(0xFFFFB960),
    onSecondary = Color(0xFF442B00),
    secondaryContainer = Color(0xFF624000),
    onSecondaryContainer = Color(0xFFFFDDB0),

    tertiary = Color(0xFFFFAEEE),
    onTertiary = Color(0xFF54155E),
    tertiaryContainer = Color(0xFF6F2F75),
    onTertiaryContainer = Color(0xFFFFD8F4),

    background = Color(0xFF131312),
    onBackground = Color(0xFFE5E2DA),
    surface = Color(0xFF131312),
    onSurface = Color(0xFFE5E2DA),
    surfaceVariant = Color(0xFF2C2A26),
    onSurfaceVariant = Color(0xFFC9C7BE),
    outline = Color(0xFF92938A),
    outlineVariant = Color(0xFF44483F),
)

@Composable
fun TransferRateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled by default so our crafted palette is the consistent visual identity.
    // Set to true to opt into Material You dynamic theming on Android 12+.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
