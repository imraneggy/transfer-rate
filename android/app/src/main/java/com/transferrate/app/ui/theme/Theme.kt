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
 * Exchangia brand palette (per design brief 2026-05-02).
 *
 * Brand swatches:
 *   - Primary teal:       #00B49E  (the E logo + active controls)
 *   - Deep navy:          #0E1F3A  (high-contrast text + dark icon variant)
 *   - Mid blue:           #1E3A5F  (secondary surfaces in dark mode)
 *   - Accent gold:        #F4B940  (BEST badge / promo accents)
 *   - Light cyan:         #9DEAD0  (light primaryContainer / soft fills)
 *   - Off-white surface:  #F4F4F0  (warm-leaning background)
 *
 * Both schemes are AAA-contrast for body text on background and AA on
 * surfaceVariant, verified with the official WCAG calculator.
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF00B49E),         // brand teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9DEAD0),
    onPrimaryContainer = Color(0xFF00261F),

    secondary = Color(0xFF7E5A00),       // gold for BEST badge (deepened for AA contrast on white)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3A8),
    onSecondaryContainer = Color(0xFF281A00),

    tertiary = Color(0xFF1E3A5F),        // mid blue used for promo badge accents
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E3FA),
    onTertiaryContainer = Color(0xFF051A33),

    background = Color(0xFFF4F4F0),      // brand off-white
    onBackground = Color(0xFF0E1F3A),    // brand deep navy as body text
    surface = Color(0xFFFAFAF7),
    onSurface = Color(0xFF0E1F3A),
    surfaceVariant = Color(0xFFEAEAE2),
    onSurfaceVariant = Color(0xFF45495A),
    outline = Color(0xFF767870),
    outlineVariant = Color(0xFFCBCEC4),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BD8C0),         // lifted teal for dark surface readability
    onPrimary = Color(0xFF003830),
    primaryContainer = Color(0xFF005A4D),
    onPrimaryContainer = Color(0xFFB6F2DE),

    secondary = Color(0xFFFFD069),       // brighter gold so BEST pops on dark
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF604200),
    onSecondaryContainer = Color(0xFFFFE3A8),

    tertiary = Color(0xFFA8C8F0),        // mid-blue lifted for dark
    onTertiary = Color(0xFF002046),
    tertiaryContainer = Color(0xFF1E3A5F),
    onTertiaryContainer = Color(0xFFD3E3FA),

    // Hierarchy: bg darkest navy, surface a touch lighter, surfaceVariant
    // lifted again — gives provider cards a clearer elevation feel without
    // explicit shadows.
    background = Color(0xFF0A1424),      // deep navy
    onBackground = Color(0xFFE5E8F0),
    surface = Color(0xFF101D33),
    onSurface = Color(0xFFE5E8F0),
    surfaceVariant = Color(0xFF1E2A40),
    onSurfaceVariant = Color(0xFFC9CDD8),
    outline = Color(0xFF8A92A1),
    outlineVariant = Color(0xFF3A445A),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF410E0B),
    errorContainer = Color(0xFF601410),
    onErrorContainer = Color(0xFFF9DEDC),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExchangiaTypography,
        content = content,
    )
}
