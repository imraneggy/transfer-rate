package com.transferrate.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.transferrate.app.R

/**
 * Manrope is a modern open-source geometric sans-serif distributed under
 * the SIL Open Font License 1.1. Bundled in res/font/ at five weights.
 *
 * The license file is at NOTICE-fonts.txt at the repository root. Bundling
 * a copy of the OFL is the only attribution requirement.
 *
 * Why Manrope:
 *   * Excellent number rendering — critical for a rate-comparison app
 *     where columns of digits sit side by side.
 *   * Tabular figures aren't built-in but the proportional figures are
 *     visually balanced enough that we use `fontFeatureSettings = "tnum"`
 *     on rate displays to enforce monospace digits.
 *   * High x-height and open apertures stay legible at 12sp body text on
 *     small phones and at high DPI.
 */
val Manrope: FontFamily = FontFamily(
    Font(R.font.manrope_regular,    FontWeight.Normal),
    Font(R.font.manrope_medium,     FontWeight.Medium),
    Font(R.font.manrope_semibold,   FontWeight.SemiBold),
    Font(R.font.manrope_bold,       FontWeight.Bold),
    Font(R.font.manrope_extrabold,  FontWeight.ExtraBold),
)

/**
 * Space Grotesk is a distinctive geometric display sans by Florian
 * Karsten. Its quirky lower-case letter shapes (especially `g`, `k`,
 * `t`) give the brand a tech-forward identity at large sizes without
 * being illegible at body sizes. SIL OFL 1.1 licensed.
 *
 * Used for display headlines and the wordmark; body text remains
 * Manrope (better at small sizes, more conservative numerals).
 */
val SpaceGrotesk: FontFamily = FontFamily(
    Font(R.font.space_grotesk_regular,  FontWeight.Normal),
    Font(R.font.space_grotesk_medium,   FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold,     FontWeight.Bold),
)

/**
 * Material 3 Typography overridden to use Manrope at every level.
 *
 * Sizes follow the Material 3 type-scale defaults but with tightened
 * letter-spacing on display levels (geometric sans-serifs read better
 * with negative tracking on large text) and bumped headline weights so
 * the brand feels confident.
 */
val ExchangiaTypography: Typography = Typography(
    // Display levels: Space Grotesk for distinctive brand voice.
    displayLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.3).sp,
    ),

    // Headline levels: Space Grotesk SemiBold for confident hierarchy.
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)
