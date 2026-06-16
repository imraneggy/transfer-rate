package com.transferrate.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.transferrate.app.BuildConfig
import com.transferrate.app.R
import com.transferrate.app.data.NotificationCenter
import com.transferrate.app.data.NotificationPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            // Hero block: logo on a NEUTRAL near-white coin (both light
            // and dark modes) so the brand mark's own navy + teal
            // palette reads as the logo.  Tinting the coin (as in
            // v0.29.2) muted the original colours.
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFFFFFFF)),
                    contentAlignment = Alignment.Center,
                ) {
                    TransferRateLogo(size = 96.dp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            SectionCard(title = stringResource(R.string.about_section_what_is_mid)) {
                Text(
                    stringResource(R.string.about_midmarket_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_midmarket_body_sources),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = stringResource(R.string.about_section_what_is_best)) {
                Text(
                    stringResource(R.string.about_best_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_best_body_delta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = stringResource(R.string.about_section_privacy)) {
                Text(
                    stringResource(R.string.about_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_privacy_body_network),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.about_privacy_body_permissions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            LanguageCard()
            Spacer(Modifier.height(12.dp))

            ReshowWelcomeCard()
            Spacer(Modifier.height(12.dp))

            // Notifications: both alert types in a single branded card.
            NotificationsCard()
            Spacer(Modifier.height(12.dp))

            // Privacy is the only outbound link — and even that is a
            // data: URI / in-app screen, not an external service. Source
            // code link removed at user request.

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.about_footer_disclaimer),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.about_footer_license),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * Unified notification-settings card replacing the two separate
 * DailyHighToggleCard / CustomTargetAlertCard that used to render as
 * generic SectionCards.
 *
 * Design decisions:
 *  - Branded deep-navy header ties it visually to the toolbar / splash.
 *  - Icon circles use per-section accent colours: teal for Daily High
 *    (live/active feel) and amber for Rate Target (goal/aim feel).
 *  - Switch checked-track uses the brand teal so the "on" state reads
 *    as distinctly enabled rather than just another indigo accent.
 *  - An animated notification-preview bubble appears when Daily High is
 *    enabled, showing the user exactly what the notification will look
 *    like in the shade — reduces uncertainty about what "on" means.
 *  - Armed-target chip shows the current threshold at a glance without
 *    the user having to look at the text field.
 *  - All logic, permission flow, and preference writes are identical to
 *    the previous two composables; only the visual presentation changed.
 */
@Composable
private fun NotificationsCard() {
    val ctx = LocalContext.current
    val prefs = remember { NotificationPrefs(ctx) }

    // ── Daily-high state ──────────────────────────────────────────────
    var enabled by remember { mutableStateOf(prefs.dailyHighEnabled) }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            NotificationCenter.ensureChannel(ctx)
            prefs.dailyHighEnabled = true
            enabled = true
            permissionDenied = false
        } else {
            prefs.dailyHighEnabled = false
            enabled = false
            permissionDenied = true
        }
    }

    // ── Rate-target state ─────────────────────────────────────────────
    val initial = prefs.customAlertTargetInr?.let { "%.2f".format(it) } ?: ""
    var fieldText by remember { mutableStateOf(initial) }
    var savedTarget by remember { mutableStateOf(prefs.customAlertTargetInr) }
    var inputError by remember { mutableStateOf<String?>(null) }
    val errInvalid = stringResource(R.string.about_target_invalid)
    val errOutOfRange = stringResource(R.string.about_target_out_of_range)

    // ── Brand palette ─────────────────────────────────────────────────
    val brandNavy = Color(0xFF071827)
    val brandTeal = Color(0xFF14BBA6)
    val amberAccent = Color(0xFFF4A900)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {

        // ── Header band ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(brandNavy, Color(0xFF0C2336)),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(brandTeal.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔔", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.about_section_notifications),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                    )
                    Text(
                        "Smart rate alerts · No spam",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.58f),
                    )
                }
                Spacer(Modifier.weight(1f))
                // Active-alerts count badge
                val activeCount = (if (enabled) 1 else 0) + (if (savedTarget != null) 1 else 0)
                if (activeCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(brandTeal, RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$activeCount active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandNavy,
                        )
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {

            // ── Daily High Alert ──────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (enabled) brandTeal.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (enabled) "📈" else "📊", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.about_dailyhigh_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.about_dailyhigh_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = { wantsOn ->
                        if (wantsOn) {
                            val granted = ContextCompat.checkSelfPermission(
                                ctx, Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                NotificationCenter.ensureChannel(ctx)
                                prefs.dailyHighEnabled = true
                                enabled = true
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            prefs.dailyHighEnabled = false
                            enabled = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = brandTeal,
                        checkedThumbColor = Color.White,
                        checkedBorderColor = Color.Transparent,
                    ),
                )
            }

            // Notification preview — animated, appears when switch is ON.
            // Shows the user exactly what will appear in the notification shade.
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, bottom = 5.dp),
                    )
                    NotificationPreviewBubble(brandTeal = brandTeal)
                }
            }

            // Permission-denied hint — styled as an inline warning chip.
            AnimatedVisibility(
                visible = permissionDenied,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        "⚠",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.about_permission_denied_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Rate Target Alert ─────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (savedTarget != null) amberAccent.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (savedTarget != null) "🎯" else "🔕", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.about_section_target_alert),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.about_target_blurb),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Armed-target chip — shows the current threshold when set.
            savedTarget?.let { t ->
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(brandTeal.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(brandTeal, CircleShape),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        stringResource(R.string.about_target_armed, "%.2f".format(t)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Input row
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = fieldText,
                    onValueChange = {
                        fieldText = it
                        inputError = null
                    },
                    label = { Text(stringResource(R.string.about_target_label)) },
                    prefix = { Text("≥ ") },
                    singleLine = true,
                    isError = inputError != null,
                    supportingText = inputError?.let { msg -> { Text(msg) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = fieldText.trim()
                        if (text.isEmpty()) {
                            prefs.customAlertTargetInr = null
                            savedTarget = null
                            inputError = null
                            return@Button
                        }
                        val n = text.toDoubleOrNull()
                        if (n == null) {
                            inputError = errInvalid
                            return@Button
                        }
                        if (n !in 15.0..40.0) {
                            inputError = errOutOfRange
                            return@Button
                        }
                        prefs.customAlertTargetInr = n
                        savedTarget = n
                        inputError = null
                        fieldText = "%.2f".format(n)
                    },
                ) {
                    Text(
                        stringResource(
                            if (fieldText.isBlank()) R.string.about_button_clear
                            else R.string.about_button_set,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * A mock-up of an Android notification shade entry, showing the user
 * exactly what the daily-high alert will look like.  Uses illustrative
 * values (Wise, ₹25.87) — the real notification is generated dynamically
 * by NotificationCenter when a new high is detected.
 *
 * Styled to match Material 3's notification appearance: rounded card,
 * app-name eyebrow row, bold title, single-line body.  The teal left-
 * dot echoes the Transfer Rate brand mark the user sees in the shade.
 */
@Composable
private fun NotificationPreviewBubble(brandTeal: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        border = BorderStroke(1.dp, brandTeal.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            // Eyebrow row: app icon dot · app name · timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(brandTeal, CircleShape),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.app_name),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "just now",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(5.dp))
            // Notification title
            Text(
                "New daily high — AED→INR",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            // Notification body
            Text(
                "Wise now offering ₹25.87. Tap to compare providers.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Lets the user re-trigger the first-launch welcome modal — handy for
 * sharing the app with someone, or revisiting the feature tour.  Just
 * clears the dismissal flag in SharedPreferences; the modal will
 * appear next time RatesScreen is shown.
 */
@Composable
private fun ReshowWelcomeCard() {
    val ctx = LocalContext.current
    val prefs = remember {
        ctx.getSharedPreferences("transfer-rate", android.content.Context.MODE_PRIVATE)
    }
    var triggered by remember { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.about_section_reshow_welcome)) {
        Text(
            stringResource(R.string.about_reshow_blurb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = {
                prefs.edit().putBoolean("welcome_dismissed_v2", false).apply()
                triggered = true
            },
        ) {
            Text(stringResource(
                if (triggered) R.string.about_reshow_done
                else R.string.about_reshow_button,
            ))
        }
    }
}

/**
 * Shortcut into Android's per-app language picker (Settings → Apps →
 * Transfer Rate → App language).  Available on API 33+; our minSdk is
 * 34 so the action *should* always resolve, but a defensive
 * resolveActivity check guards against OEM ROMs that strip the picker
 * (a few old custom Android skins did).  When unavailable we fall back
 * to opening the device's general language settings.
 */
@Composable
private fun LanguageCard() {
    val ctx = LocalContext.current
    var unavailable by remember { mutableStateOf(false) }
    SectionCard(title = stringResource(R.string.about_section_language)) {
        Text(
            stringResource(R.string.about_language_blurb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = {
                val perApp = android.content.Intent(
                    android.provider.Settings.ACTION_APP_LOCALE_SETTINGS,
                    android.net.Uri.fromParts("package", ctx.packageName, null),
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                val deviceWide = android.content.Intent(
                    android.provider.Settings.ACTION_LOCALE_SETTINGS,
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(perApp) }
                    .recoverCatching { ctx.startActivity(deviceWide) }
                    .onFailure { unavailable = true }
            },
        ) {
            Text(stringResource(R.string.about_language_button))
        }
        if (unavailable) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.about_language_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
