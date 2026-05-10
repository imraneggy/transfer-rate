package com.transferrate.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.transferrate.app.R
import java.util.Locale

/**
 * Local notification plumbing for the "new daily high" alert.
 *
 * No Firebase, no FCM — these are platform-local notifications fired
 * from [RatesPrefetchWorker] when a refresh detects a rate that beats
 * today's previously-notified peak. The privacy posture stays intact:
 * nothing leaves the device, no token registers with any server.
 *
 * Channel registration is idempotent and safe to call from any thread,
 * so we register both at app launch (so the user can see it under
 * Android Settings → Apps → Transfer Rate → Notifications even before
 * any notification has fired) and defensively from the worker.
 */
object NotificationCenter {

    /** Channel ID — versioned so future cadence/importance changes can
     *  ship a new channel without retro-modifying user-customised
     *  importance levels (Android disallows that). */
    const val CHANNEL_ID_DAILY_HIGH = "daily_high_v1"

    /** Single notification ID for the daily-high alert. We re-use it so
     *  successive intra-day highs replace the previous notification
     *  rather than stacking — most users want "what's the best right
     *  now", not a chronological audit log in their shade. */
    private const val NOTIFICATION_ID_DAILY_HIGH = 1001
    private const val NOTIFICATION_ID_CUSTOM_TARGET = 1002

    /** Register (or re-register; idempotent) the daily-high channel. */
    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID_DAILY_HIGH,
            "Daily best rate",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description =
                "Alerts when a provider beats today's previous best AED→INR rate. Fires at most a few times per day."
            // No sound override — user-customisable per channel via
            // system settings. We respect their choice.
            enableVibration(false)
            setShowBadge(false)
        }
        mgr.createNotificationChannel(channel)
    }

    /**
     * Post (or replace) the daily-high notification.
     *
     * Returns true on success, false if the runtime permission was not
     * granted (the worker will silently swallow this case — the user
     * can re-grant via the in-app toggle or system settings).
     */
    fun postDailyHigh(
        context: Context,
        providerName: String,
        rate: Double,
        currencyCode: String,
        currencySymbol: String,
    ): Boolean {
        // POST_NOTIFICATIONS gate (Android 13+, which is everything for
        // our minSdk 34). Calling notify() without the grant logs a
        // SecurityException; we'd rather check up-front and exit clean.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, "android.permission.POST_NOTIFICATIONS",
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        // Defensive — the channel is registered at app launch but the
        // worker can run after a system-driven app process death where
        // the channel registration would not have run again. Cheap to
        // re-register; idempotent.
        ensureChannel(context)

        val launchIntent = Intent().apply {
            // Use class-name lookup rather than a direct MainActivity
            // import so this file has no UI-package dependency.
            setClassName(context, "com.transferrate.app.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = "New daily high — AED→$currencyCode"
        val short = String.format(
            Locale.US,
            "%s now offering %s%.2f. Tap to compare providers.",
            providerName, currencySymbol, rate,
        )
        val long = String.format(
            Locale.US,
            "%s is offering %s%.2f for AED→%s — best rate so far today. " +
                "Tap to compare against other providers and lock in your transfer.",
            providerName, currencySymbol, rate, currencyCode,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_HIGH)
            // Monochrome white-on-transparent vector — Android tints to
            // the system accent color in the status bar. Re-uses the
            // launcher monochrome glyph so brand identity stays
            // consistent across icon & notification.
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(short)
            .setStyle(NotificationCompat.BigTextStyle().bigText(long))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_DAILY_HIGH, notification)
            true
        } catch (_: SecurityException) {
            // Permission revoked between checkSelfPermission and
            // notify(). Race is rare; just bail.
            false
        }
    }

    /**
     * Post the custom rate-target notification (v0.30).
     *
     * Fired by [RatesPrefetchWorker] when the user has set a target via
     * About → Notifications → "Notify when rate ≥ X" and the current
     * best AED→INR rate has crossed it.  Per-day dedup is handled by
     * [NotificationPrefs.shouldNotifyCustomTargetAndRecord].
     *
     * Uses the same daily-high channel since the runtime UX (status-bar
     * alert at IMPORTANCE_DEFAULT) is identical from the user's
     * perspective; only the body copy differs.  Returns true on
     * successful post, false if POST_NOTIFICATIONS is not granted.
     */
    fun postCustomTargetHit(
        context: Context,
        providerName: String,
        rate: Double,
        target: Double,
        currencyCode: String,
        currencySymbol: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, "android.permission.POST_NOTIFICATIONS",
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        ensureChannel(context)

        val launchIntent = Intent().apply {
            setClassName(context, "com.transferrate.app.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            1,                              // distinct request code per notification kind
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = "Target hit — AED→$currencyCode"
        val short = String.format(
            Locale.US,
            "%s%.2f hit (target %s%.2f). %s leads — tap to send.",
            currencySymbol, rate, currencySymbol, target, providerName,
        )
        val long = String.format(
            Locale.US,
            "Your alert target of %s%.2f for AED→%s has been reached. " +
                "%s is offering %s%.2f right now — tap to compare and lock in.",
            currencySymbol, target, currencyCode,
            providerName, currencySymbol, rate,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_HIGH)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(short)
            .setStyle(NotificationCompat.BigTextStyle().bigText(long))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        return try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_CUSTOM_TARGET, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
