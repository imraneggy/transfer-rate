package com.transferrate.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                    "The mid-market (or 'interbank') rate is the wholesale " +
                    "midpoint between the buy and sell prices in the global " +
                    "currency market. It is the most objective benchmark — " +
                    "every remittance provider charges some markup or fee " +
                    "on top.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "We pull it from public financial-data sources, with " +
                    "automatic fallbacks if the primary is unavailable. The " +
                    "rate is refreshed alongside provider rates on the " +
                    "regular cycle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = stringResource(R.string.about_section_what_is_best)) {
                Text(
                    "Among the providers with verified live rates, the one " +
                    "giving you the most rupees per AED. Updated each refresh.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Each card also shows '+0.02 vs mid-market' or '−0.05' so " +
                    "you can see at a glance how each provider compares to the " +
                    "objective benchmark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            SectionCard(title = stringResource(R.string.about_section_privacy)) {
                Text(
                    "This app collects nothing. No analytics, no telemetry, " +
                    "no advertising, no account, no cloud sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Outbound network access is allowlisted at the " +
                    "application layer to a small set of public data hosts " +
                    "needed for rate updates. Anything else is blocked " +
                    "regardless of what any future dependency tries to do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Permissions: INTERNET and ACCESS_NETWORK_STATE for the " +
                    "rate fetch. The optional POST_NOTIFICATIONS permission " +
                    "is requested once for daily-high alerts; you can deny " +
                    "or revoke it any time without breaking the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))

            LanguageCard()
            Spacer(Modifier.height(12.dp))

            ReshowWelcomeCard()
            Spacer(Modifier.height(12.dp))

            DailyHighToggleCard()
            Spacer(Modifier.height(12.dp))

            CustomTargetAlertCard()
            Spacer(Modifier.height(12.dp))

            // Privacy is the only outbound link — and even that is a
            // data: URI / in-app screen, not an external service. Source
            // code link removed at user request.

            Spacer(Modifier.height(24.dp))
            Text(
                "Rates shown are indicative and may differ at the provider. " +
                "Confirm at the provider's app or website before sending money. " +
                "Not financial advice. Not affiliated with any of the listed " +
                "remittance providers. Provider names and logos are trademarks " +
                "of their respective owners, used here for nominative " +
                "identification in a comparison context only.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Released under the MIT License.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * Toggle card for the optional daily-high notification.
 *
 * Behavior:
 *   - Switch reads current state from [NotificationPrefs] on first
 *     composition.
 *   - Flipping ON: if POST_NOTIFICATIONS is already granted, just save
 *     the flag. Otherwise launches the runtime permission request and
 *     saves the flag only on grant.
 *   - Flipping OFF: saves the flag immediately. We deliberately do NOT
 *     attempt to revoke the OS permission — that's a system-level
 *     setting the user controls, not something an app should toggle.
 *   - If the user denied permission, we surface a one-line hint
 *     pointing to system settings rather than re-prompting (re-prompts
 *     trigger Android's "permission permanently denied" path which is
 *     a worse UX than just telling them where to go).
 */
@Composable
private fun DailyHighToggleCard() {
    val ctx = LocalContext.current
    val prefs = remember { NotificationPrefs(ctx) }
    var enabled by remember { mutableStateOf(prefs.dailyHighEnabled) }
    var permissionPreviouslyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            // Make sure the channel exists before the worker tries to
            // post — registration is idempotent.
            NotificationCenter.ensureChannel(ctx)
            prefs.dailyHighEnabled = true
            enabled = true
            permissionPreviouslyDenied = false
        } else {
            prefs.dailyHighEnabled = false
            enabled = false
            permissionPreviouslyDenied = true
        }
    }

    SectionCard(title = stringResource(R.string.about_section_notifications)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    "Daily high alert",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Status-bar alert when a provider beats today's previous " +
                        "best AED→INR rate. At most a few notifications per day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        if (permissionPreviouslyDenied) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Permission was declined. To enable, open Android " +
                    "Settings → Apps → Transfer Rate → Notifications, " +
                    "allow them, then return here and toggle on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Custom rate-target alert (v0.30).
 *
 * Lets the user say "ping me when AED→INR ≥ 25.85" so they can act on
 * a peak without having to open the app to check.  Independent of the
 * daily-high toggle — those are two different alert flavours:
 *
 *   * daily-high: "tell me whenever today's previous best is beaten"
 *     (~few notifications/day; surfaces upward momentum)
 *   * custom target: "tell me when rate hits MY threshold" (one
 *     notification/day max per target; converts watching into action)
 *
 * Per-day dedup is handled by NotificationPrefs.  Empty input clears
 * the target (disables the alert).  Sane bounds (15.0..40.0 covers
 * any plausible AED→INR rate for the next decade).
 */
@Composable
private fun CustomTargetAlertCard() {
    val ctx = LocalContext.current
    val prefs = remember { NotificationPrefs(ctx) }
    val initial = prefs.customAlertTargetInr?.let { "%.2f".format(it) } ?: ""
    var fieldText by remember { mutableStateOf(initial) }
    var savedTarget by remember { mutableStateOf(prefs.customAlertTargetInr) }
    var inputError by remember { mutableStateOf<String?>(null) }

    val errInvalid = stringResource(R.string.about_target_invalid)
    val errOutOfRange = stringResource(R.string.about_target_out_of_range)
    SectionCard(title = stringResource(R.string.about_section_target_alert)) {
        Text(
            stringResource(R.string.about_target_blurb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        // Show whether a target is currently armed.
        savedTarget?.let { t ->
            Text(
                stringResource(R.string.about_target_armed, "%.2f".format(t)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = fieldText,
                onValueChange = {
                    fieldText = it
                    inputError = null
                },
                label = { Text(stringResource(R.string.about_target_label)) },
                singleLine = true,
                isError = inputError != null,
                supportingText = inputError?.let { { Text(it) } },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.Button(
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
                Text(stringResource(
                    if (fieldText.isBlank()) R.string.about_button_clear
                    else R.string.about_button_set,
                ))
            }
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

