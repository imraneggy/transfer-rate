package com.transferrate.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic background work that refreshes the local rates cache so the
 * app shows fresh data instantly on cold start, AND — when the user has
 * opted in — fires a status-bar notification when a provider beats the
 * day's previous best AED→INR rate.
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
        val fetched = repo.fetch()
        if (fetched.isFailure) {
            // Don't fail loudly — the next launch can re-cache.
            // Returning retry would burn user battery; defer.
            return Result.success()
        }

        // ----- Daily-high notification (opt-in) -----
        //
        // Only fires when (a) the user has explicitly enabled the
        // toggle in the About screen, AND (b) the live "best now" rate
        // for INR exceeds today's previously-notified peak by more
        // than a half-paisa epsilon.
        //
        // Wrapped in runCatching so any notification path failure
        // (revoked permission, channel not yet registered, OEM quirks)
        // never affects the worker's primary cache-refresh job.
        runCatching {
            val prefs = NotificationPrefs(applicationContext)
            if (!prefs.dailyHighEnabled) return@runCatching

            val doc = fetched.getOrNull() ?: return@runCatching
            val currentBest = pickCurrentBest(doc, corridor = "INR")
                ?: return@runCatching

            val rate = currentBest.effectiveRate ?: currentBest.rate
            ?: return@runCatching

            if (prefs.shouldNotifyAndRecord(rate)) {
                val info = CURRENCIES["INR"]
                NotificationCenter.postDailyHigh(
                    context = applicationContext,
                    providerName = currentBest.providerName,
                    rate = rate,
                    currencyCode = "INR",
                    currencySymbol = info?.symbol ?: "₹",
                )
            }
        }

        return Result.success()
    }

    /** Same filter as RatesViewModel.bestRate — exclude mid_market and
     *  any non-OK status, then take the highest effective rate. */
    private fun pickCurrentBest(doc: RatesDocument, corridor: String): ProviderQuote? {
        val quotes = doc.corridors[corridor].orEmpty()
        return quotes.asSequence()
            .filter { it.providerId != "mid_market" }
            .filter { it.status == "ok" || it.status == "manual" }
            .filter { (it.effectiveRate ?: it.rate) != null }
            .maxByOrNull { it.effectiveRate ?: it.rate ?: 0.0 }
    }
}
