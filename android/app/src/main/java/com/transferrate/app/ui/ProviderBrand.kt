package com.transferrate.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Provider brand-colour mapping used to tint the BEST card so the
 * winning provider's identity reads at a glance.
 *
 * Each entry is the *display* tint we render at — already toned down
 * from the raw brand hex so that text on top stays legible without
 * needing per-provider on-colour calculation.  These are NOT exact
 * brand specs (we don't claim to use trademark-protected pantones);
 * they're "in the right family" tints picked to:
 *
 *   * read distinctively against the surface card stack,
 *   * leave room for `MaterialTheme.colorScheme.onSurface` text to
 *     remain accessible (APCA Body+ at the displayed size),
 *   * match the visual identity a UAE-Indian remittance user would
 *     associate with each brand from its app icon / website.
 *
 * Trademark posture: provider names and logos are trademarks of their
 * respective owners.  This map is used here for nominative identification
 * in a comparison context, not branding or endorsement.
 */
@Composable
internal fun bestCardTintFor(providerId: String): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (providerId.lowercase()) {
        // Light-mode tints are pale enough that onSurface (deep navy)
        // text stays readable; dark-mode tints are deeper saturated
        // variants so the BEST card stays distinguishable from the
        // dark surface stack.
        "wise"             -> if (isDark) Color(0xFF2A4F2E) else Color(0xFFD7F0CC)
        "remitly"          -> if (isDark) Color(0xFF5C2A14) else Color(0xFFFFE0CC)
        "aspora"           -> if (isDark) Color(0xFF124E47) else Color(0xFFC8EFE9)
        "lulu"             -> if (isDark) Color(0xFF5C1414) else Color(0xFFFFD0D0)
        "transfergo"       -> if (isDark) Color(0xFF1B2A55) else Color(0xFFD1DAF5)
        "al_ansari"        -> if (isDark) Color(0xFF4F1027) else Color(0xFFF7CCDC)
        "al_dahab"         -> if (isDark) Color(0xFF4A3B0F) else Color(0xFFFAEAB1)
        "ahalia"           -> if (isDark) Color(0xFF143A55) else Color(0xFFCFE2F2)
        "federal_exchange" -> if (isDark) Color(0xFF1A4F31) else Color(0xFFCFEFD8)
        "gcc_exchange"     -> if (isDark) Color(0xFF103D5C) else Color(0xFFCFE5F5)
        "index_exchange"   -> if (isDark) Color(0xFF12483D) else Color(0xFFCFEDDF)
        "lari"             -> if (isDark) Color(0xFF4A2050) else Color(0xFFE9D2F0)
        "joyalukkas"       -> if (isDark) Color(0xFF5A2418) else Color(0xFFF5D5C8)
        "western_union"    -> if (isDark) Color(0xFF5C3A12) else Color(0xFFFADBB0)
        "moneygram"        -> if (isDark) Color(0xFF1A3D5C) else Color(0xFFCFE2F2)
        "instarem"         -> if (isDark) Color(0xFF3D1F4F) else Color(0xFFE0CFEC)
        "xoom"             -> if (isDark) Color(0xFF103465) else Color(0xFFCFD8F5)
        "worldremit"       -> if (isDark) Color(0xFF124A40) else Color(0xFFCFEDE3)
        // Fallback: the existing v0.27.x dual-tone secondary container
        // (slightly more presence than surface, less than primary).
        else               -> if (isDark) Color(0xFF241776) else Color(0xFFA3ACFF)
    }
}
