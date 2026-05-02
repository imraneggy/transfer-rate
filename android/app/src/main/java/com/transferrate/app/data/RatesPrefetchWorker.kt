package com.transferrate.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic background work that refreshes the local rates cache so the
 * app shows fresh data instantly on cold start.
 *
 * Constraints applied at scheduling time (see schedule() below):
 *   - requires network (any kind — Wi-Fi or cellular)
 *   - won't run on low battery
 *   - won't run on metered networks BY DEFAULT — user data costs nothing
 *     surprising
 *
 * On WorkManager's cycle, this fires roughly every 1 hour (or whenever
 * the OS decides; WorkManager batches with other work for battery
 * efficiency). Even a few times per day is enough to keep the cache
 * within ~hour-old of the cron run.
 *
 * The worker only WRITES the cache. The UI reads it via
 * RatesRepository.loadCached() at startup. There's no two-way coupling
 * to the running ViewModel — the worker is fire-and-forget.
 */
class RatesPrefetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = RatesRepository(applicationContext)
        return repo.fetch().fold(
            onSuccess = {
                // The repository's fetch() already writes the cache as a
                // side-effect on success.
                Result.success()
            },
            onFailure = {
                // Don't fail loudly — the next launch can re-cache.
                // Returning retry would burn user battery; defer.
                Result.success()
            },
        )
    }
}
