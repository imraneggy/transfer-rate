package com.transferrate.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic rates prefetch worker. Idempotent — calling
 * this on every app start is fine; WorkManager dedupes via the unique
 * work name and KEEP policy.
 *
 * Cadence: every 1 hour minimum (WorkManager batches; actual interval
 * depends on OS Doze, battery, and other system constraints — usually
 * works out to ~once an hour while the device is awake).
 *
 * Constraints:
 *   - any network connection (Wi-Fi or cellular both fine — payload is
 *     small, ~3KB)
 *   - not on low battery (saver mode skips us)
 */
object PrefetchScheduler {

    private const val UNIQUE_WORK_NAME = "transfer-rate.prefetch"

    fun schedule(context: Context) {
        // UNMETERED (was CONNECTED) — match the doc-comment promise on
        // RatesPrefetchWorker that the worker won't run on metered
        // (cellular) networks by default.  Saves user data and matches
        // the public "$0 surprises" stance for an always-free app.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<RatesPrefetchWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .addTag("rates-prefetch")
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                // KEEP: if there's already a pending periodic work with
                // this name, don't replace it. Means re-launching the
                // app doesn't reset the schedule clock.
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }
}
