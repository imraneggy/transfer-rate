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

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C45),
    onPrimary = Color.White,
    secondary = Color(0xFF4F6354),
    background = Color(0xFFF7FBF6),
    surface = Color(0xFFF7FBF6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FDBA0),
    onPrimary = Color(0xFF003821),
    secondary = Color(0xFFB6CCBB),
    background = Color(0xFF101510),
    surface = Color(0xFF101510),
)

@Composable
fun TransferRateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You / dynamic color works on Android 12+; we're 14+ so always available.
    dynamicColor: Boolean = true,
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
