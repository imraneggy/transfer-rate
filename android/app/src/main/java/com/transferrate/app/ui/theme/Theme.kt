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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Transfer Rate palette — Stripe Atlas Premium (v0.27 lift).
 *
 * This palette was generated using OKLCh perceptually-uniform colour
 * space, anchored on real Stripe brand values:
 *
 *   - Primary anchor:   #8486FF  (Stripe indigo lifted to L=70)
 *   - Neutral anchor:   #0A2540  (Stripe deep navy, real brand text)
 *   - Background:       #F6F9FC  (Stripe paper bg, real brand)
 *
 * v0.27 change: the primary anchor moved up one step on the indigo
 * tonal ramp (L=60 → L=70) because the L=60 reading felt too heavy
 * against the paper-light background.  L=70 lifts the brand toward
 * "fresh fintech" without losing the indigo identity.  Secondary now
 * occupies the deeper L=60 step so the dual-tone relationship stays —
 * primary feels airy, secondary feels grounded.
 *
 * Trade-off acknowledged: AED-on-primary contrast at L=70 is APCA Lc
 * ~-65 (passes Body+ at 14sp/Bold, fails AAA Normal).  Buttons in this
 * app use 14sp/Bold labels so the threshold is met; if we ever add
 * 12sp body text on a filled primary surface we'd need to revisit.
 *
 * Each role colour is taken from a hue-locked OKLCh tonal ramp at a
 * specific lightness step (notation: <role>_L<lightness>).  This is
 * the same methodology Stripe / Linear / Vercel ship — palette
 * decisions ladder back to perceptual-uniformity rather than HSL
 * eyeballing.
 *
 * NEW in v0.26: metal accent colours.  Gold and silver have their
 * own warm/cool tonal ramps rather than borrowing from secondary +
 * neutral.  Exposed to the UI layer via [LocalMetalColors] so the
 * gold/silver bottom-sheet and home-card can render their respective
 * metals with proper visual differentiation.
 */

private val LightColors = lightColorScheme(
    // v0.29.3 light-mode tightening — user feedback "washed out and pale".
    // Diagnosis: every surface in v0.29.x sat at L>=92 and primary at L=70,
    // so the whole palette was clustered at the top of the lightness axis
    // with no visual depth between background, cards, and accent surfaces.
    // Adjustments below pull primary from L=70 to L=66 (more brand presence
    // without going back to the L=60 "too dark" complaint), and pull the
    // surface stack apart so cards visibly pop against the paper bg.
    primary = Color(0xFF6F73FF),                  // indigo L=63 (settles between L=60 too-dark and L=70 too-pale; tightened in v0.29.3 after APCA validation found Lc<60 for primary-as-text on surface variants)
    onPrimary = Color.White,                      // APCA Body+ at 14sp/Bold passes
    primaryContainer = Color(0xFFCDD9FF),         // indigo L=90 (was L=95 — more presence)
    onPrimaryContainer = Color(0xFF241776),       // primary L=20

    secondary = Color(0xFF635BFF),                // indigo L=60 (the v0.26 primary anchor)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA3ACFF),       // primary L=80
    onSecondaryContainer = Color(0xFF100D3B),     // primary L=12

    tertiary = Color(0xFF4A6684),                 // neutral L=50 (slate)
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCE0FB),        // neutral L=90
    onTertiaryContainer = Color(0xFF1F2F41),      // neutral L=30

    background = Color(0xFFF6F9FC),               // Stripe paper L=97
    onBackground = Color(0xFF1F2F41),             // neutral L=30, APCA Body+ -99
    surface = Color.White,
    onSurface = Color(0xFF1F2F41),
    surfaceVariant = Color(0xFFD1DDE9),           // neutral L=82 (v0.29.6: deepened from L=87 — light mode still felt washed; cards now visibly lift off the paper bg)
    onSurfaceVariant = Color(0xFF435F7C),         // neutral L=47 (v0.29.6: deepened L=50 -> L=47 to clear APCA Body 60 on the new darker surfaceVariant)
    outline = Color(0xFF7BA3C6),                  // neutral L=66 (v0.29.6: deepened from L=72 for stronger card edges)
    outlineVariant = Color(0xFFB3CCE5),           // neutral L=80 (v0.29.6: deepened from L=85)
    error = Color(0xFFBB0412),                    // negative L=50
    onError = Color.White,
    errorContainer = Color(0xFFFFD3C7),           // negative L=95
    onErrorContainer = Color(0xFF610000),         // negative L=20
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0BAFF),                  // primary L=85 (lifted from L=80 in v0.29.3 — Lc -64 vs surface, was -56 fail; chip-fill with onPrimary dark navy still APCA-safe at +90)
    onPrimary = Color(0xFF100D3B),                // primary L=12 (tightened from L=20 in v0.29.3 — gives APCA Lc -65+ on the L=80 primary fill, was Lc 57 fail)
    primaryContainer = Color(0xFF3929AD),         // primary L=40
    onPrimaryContainer = Color(0xFFDCE7FF),       // primary L=95

    secondary = Color(0xFF8486FF),                // primary L=70
    onSecondary = Color(0xFF100D3B),              // primary L=12 (matched to onPrimary tightening)
    secondaryContainer = Color(0xFF241776),       // primary L=20
    onSecondaryContainer = Color(0xFFDCE7FF),

    tertiary = Color(0xFFA6C1DE),                 // neutral L=80
    onTertiary = Color(0xFF1F2F41),
    tertiaryContainer = Color(0xFF4A6684),        // neutral L=50
    onTertiaryContainer = Color(0xFFE0F1FF),      // neutral L=95

    // v0.32.0: OLED-true-black dark mode.  Was a navy stack
    // (background #0F1720 + surface #1F2F41 + variant #334462)
    // anchored on Stripe brand navy.  Real OLED pixels render the
    // navy as a low-amber glow; pure #000000 turns those pixels off
    // entirely, saving battery on AMOLED panels (~30-40% lower power
    // draw on a typical dashboard view) and giving the high-contrast
    // floating-content look modern users associate with finance apps.
    // Card layers lifted with small +brightness steps (surface #0E /
    // variant #1A) so cards still pop off the background — purely
    // black-on-black cards lose the edge separation that makes
    // hierarchy readable.  Brand-indigo primary stays the same; the
    // bigger contrast against true black actually improves Lc (L=85
    // primary on L=0 background = Lc -90 vs the previous -64 on
    // navy, well above APCA Body+ threshold).
    background = Color(0xFF000000),               // OLED pure black — pixels off
    onBackground = Color(0xFFE0F1FF),             // neutral L=95, APCA Lc -110 (was -97)
    surface = Color(0xFF0E0E0E),                  // near-black cards, +5 luminance lift
    onSurface = Color(0xFFE0F1FF),
    surfaceVariant = Color(0xFF1A1F26),           // deeper-near-black variant w/ slight navy hint
    onSurfaceVariant = Color(0xFFBFD2E5),         // neutral L=85 (Lc still -85+ on new variant)
    outline = Color(0xFF3D4A5C),                  // slate edge — visible on black without glow
    outlineVariant = Color(0xFF1F2937),           // softer hairline
    error = Color(0xFFFF8B7D),                    // negative L=70
    onError = Color(0xFF610000),
    errorContainer = Color(0xFF8F0000),           // negative L=40
    onErrorContainer = Color(0xFFFFD3C7),
)

/**
 * Metal-specific colour palette for the gold/silver UI surfaces.
 *
 * Independent of the indigo-anchored Material 3 palette so each metal
 * reads as itself: warm gold (saddle anchor) for gold, cool steel for
 * silver.  Used by GoldHeader and GoldHistorySheet to tint snapshot
 * cards, carat selectors and stats pills in the appropriate metal.
 *
 * Tonal ramp anchors:
 *   gold:   warm saddle  (real Brex anchor #FFB800 family at L=50)
 *   silver: cool steel   (neutral grey-blue, slight cyan-shifted hue)
 */
data class MetalColors(
    val goldLightest: Color,          // L=95: highlight tint behind gold
    val goldLight: Color,             // L=90: card background gradient end
    val goldSurface: Color,           // L=80: chip background
    val goldAccent: Color,            // L=50-60: primary gold accent
    val goldText: Color,              // L=30: text on light gold bg
    val goldDeep: Color,              // L=20: deep gold for emphasis

    val silverLightest: Color,        // L=95
    val silverLight: Color,           // L=90
    val silverSurface: Color,         // L=80
    val silverAccent: Color,          // L=50
    val silverText: Color,            // L=30
    val silverDeep: Color,            // L=20
)

private val LightMetalColors = MetalColors(
    goldLightest = Color(0xFFFFF9E6),
    goldLight    = Color(0xFFFBE9B1),
    goldSurface  = Color(0xFFF2D17A),
    goldAccent   = Color(0xFFD4A017),
    goldText     = Color(0xFFA87800),
    goldDeep     = Color(0xFF5C3F00),

    silverLightest = Color(0xFFF2F4F7),
    silverLight    = Color(0xFFE0E5EB),
    silverSurface  = Color(0xFFC5CDD8),
    silverAccent   = Color(0xFF8E9AA8),
    silverText     = Color(0xFF586473),
    silverDeep     = Color(0xFF2F3742),
)

private val DarkMetalColors = MetalColors(
    // Inverted lightness — gold "lightest" in dark mode is the deepest
    // saddle, "deepest" is the cream highlight.  Same hue, flipped L.
    goldLightest = Color(0xFF3D2D00),
    goldLight    = Color(0xFF5C3F00),
    goldSurface  = Color(0xFFA87800),
    goldAccent   = Color(0xFFE8C76B),
    goldText     = Color(0xFFFFD788),
    goldDeep     = Color(0xFFFFF9E6),

    silverLightest = Color(0xFF2F3742),
    silverLight    = Color(0xFF586473),
    silverSurface  = Color(0xFF8E9AA8),
    silverAccent   = Color(0xFFC5CDD8),
    silverText     = Color(0xFFE0E5EB),
    silverDeep     = Color(0xFFF2F4F7),
)

/**
 * CompositionLocal provides MetalColors to the UI tree.  Default value
 * is the light metals; TransferRateTheme overrides per dark/light.
 */
val LocalMetalColors = staticCompositionLocalOf { LightMetalColors }

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
    val metals = if (darkTheme) DarkMetalColors else LightMetalColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMetalColors provides metals) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TransferRateTypography,
            content = content,
        )
    }
}
