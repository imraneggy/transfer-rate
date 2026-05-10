package com.transferrate.app.data

import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/**
 * Lightweight wrapper around SharedPreferences for the daily-high
 * notification feature. Two responsibilities:
 *
 *   1. Persist the user's opt-in toggle (default: false).
 *   2. Track per-local-day dedup state — the highest rate we've already
 *      notified about today — so the periodic worker doesn't fire ten
 *      notifications when a single afternoon spike pulls the rate up
 *      0.01 at a time.
 *
 * Storage uses its own SharedPreferences file (`transfer-rate-notifications`)
 * separate from the existing `transfer-rate` prefs (which holds the
 * onboarding-hint dismissal flag) so the two domains can be wiped or
 * inspected independently when debugging.
 */
class NotificationPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        FILE_NAME, Context.MODE_PRIVATE,
    )

    /** User-controlled opt-in.  v0.29.2: default flipped to **true** —
     *  daily-high alerts are the most useful feature in a remittance-rate
     *  app and most users miss the toggle in About.  MainActivity does a
     *  one-shot POST_NOTIFICATIONS permission request on first launch
     *  when this is true; if the user denies, the flag is flipped to
     *  false so the About toggle reflects reality.
     *
     *  Users who explicitly turned the toggle OFF in any prior version
     *  keep their `false` value (SharedPreferences stores the explicit
     *  choice; the default is only consulted when the key is absent). */
    var dailyHighEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    /** True once we've shown the system POST_NOTIFICATIONS prompt for
     *  this install, so we don't re-prompt on every cold start (Android's
     *  "permanently denied" path is a worse UX than a single ask).
     *  Default false — the first cold start with [dailyHighEnabled]=true
     *  triggers the prompt, then this flips. */
    var permissionRequested: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PERMISSION_REQUESTED, value).apply()
        }

    /** v0.30: custom rate-target alert.  When non-null, the periodic
     *  prefetch worker fires a notification on the first scrape where
     *  the best AED→INR rate >= this target.  Per-day dedup so users
     *  aren't spammed if the rate hovers above target all afternoon.
     *
     *  Stored as a Float to fit cleanly in SharedPreferences (Double
     *  isn't a primitive there); the small precision loss is fine — we
     *  only compare to 4 decimal places of rate data anyway. */
    var customAlertTargetInr: Double?
        get() {
            val v = prefs.getFloat(KEY_TARGET_INR, Float.NaN)
            return if (v.isNaN()) null else v.toDouble()
        }
        set(value) {
            val edit = prefs.edit()
            if (value == null) {
                edit.remove(KEY_TARGET_INR)
            } else {
                edit.putFloat(KEY_TARGET_INR, value.toFloat())
            }
            edit.apply()
        }

    /**
     * Should the worker fire a custom-target notification right now?
     *
     * Returns true on the first observation in the current local-day
     * where `currentBest >= customAlertTargetInr`, and atomically
     * records that we've notified for THIS target on THIS day so the
     * same observation can't trigger again.  A target change resets the
     * dedup so the user can lower their target mid-day and immediately
     * see a notification if the new target is already met.
     */
    fun shouldNotifyCustomTargetAndRecord(
        currentBest: Double,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val target = customAlertTargetInr ?: return false
        if (currentBest < target - EPSILON) return false  // not yet met

        val today = LocalDate.now(zone).toString()
        val recordedDate = prefs.getString(KEY_TARGET_LAST_DATE, null)
        val recordedTarget = prefs.getFloat(KEY_TARGET_LAST_NOTIFIED, Float.NaN)

        // Already notified today FOR THIS TARGET — don't re-fire.
        if (recordedDate == today && !recordedTarget.isNaN()
            && kotlin.math.abs(recordedTarget.toDouble() - target) < EPSILON) {
            return false
        }

        prefs.edit()
            .putString(KEY_TARGET_LAST_DATE, today)
            .putFloat(KEY_TARGET_LAST_NOTIFIED, target.toFloat())
            .apply()
        return true
    }

    /**
     * Decide whether to fire a notification for [candidate] (the live
     * "best now" rate) and, if yes, atomically record the new high so
     * the same observation can't trigger again.
     *
     * Logic:
     *   - If today's recorded peak is empty (new day, or first-ever
     *     check) → notify and record.
     *   - If [candidate] exceeds the recorded peak by more than [EPSILON]
     *     → notify and record.
     *   - Otherwise → no notification, no state change.
     *
     * EPSILON guards against float jitter (a rate of 25.83 vs. 25.8300001
     * would otherwise spam users with "new high" alerts on every refresh).
     */
    fun shouldNotifyAndRecord(
        candidate: Double,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val today = LocalDate.now(zone).toString()
        val recordedDate = prefs.getString(KEY_LAST_DATE, null)
        val recordedPeak = if (recordedDate == today) {
            prefs.getFloat(KEY_LAST_PEAK, 0f).toDouble()
        } else {
            // Different day → previous record is irrelevant; treat as 0.
            0.0
        }

        val isNewHigh = candidate > recordedPeak + EPSILON
        if (isNewHigh) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putFloat(KEY_LAST_PEAK, candidate.toFloat())
                .apply()
        }
        return isNewHigh
    }

    /** Test/debug helper: forget today's recorded peak so the next
     *  worker tick fires a notification regardless. Not exposed in UI. */
    @Suppress("unused")
    fun forgetTodaysPeak() {
        prefs.edit()
            .remove(KEY_LAST_DATE)
            .remove(KEY_LAST_PEAK)
            .apply()
    }

    companion object {
        private const val FILE_NAME = "transfer-rate-notifications"
        private const val KEY_ENABLED = "daily_high_enabled"
        private const val KEY_LAST_DATE = "last_notified_date"
        private const val KEY_LAST_PEAK = "last_notified_peak"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested_v1"
        private const val KEY_TARGET_INR = "custom_target_inr"
        private const val KEY_TARGET_LAST_DATE = "custom_target_last_date"
        private const val KEY_TARGET_LAST_NOTIFIED = "custom_target_last_notified"

        /** 0.005 ≈ half-a-paisa for AED→INR; below this we treat two
         *  observations as the same rate. Matches the 4-dp rounding the
         *  schema validates against in [HistoryDocument.validate]. */
        private const val EPSILON = 0.005
    }
}
