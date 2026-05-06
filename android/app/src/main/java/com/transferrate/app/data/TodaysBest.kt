package com.transferrate.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Compact summary used by the "Today's Best" banner above the rates list.
 *
 * Holds the two complementary signals a user actually wants to see at the
 * top of the screen:
 *
 *   - [currentBest] — the highest live provider rate *right now*. Drives
 *     the immediate decision: "if I send right now, this is the leader."
 *   - [todaysPeak]  — the highest rate seen across providers since
 *     local-midnight. Drives the patience signal: "is right now actually
 *     a good moment, or did I miss the high earlier?"
 *
 * Both fields are nullable independently — we may have current data but
 * no history yet (cold start before history.json arrives), or vice
 * versa, and the banner is built to degrade gracefully.
 */
data class TodaysBestSummary(
    val currentBest: ProviderSnapshot?,
    val todaysPeak: ProviderSnapshot?,
) {
    /** Helper for UI: did the current best just hit / match today's high?
     *  We round both to 4 dp because the underlying schema validates rates
     *  to that precision and float equality is otherwise misleading. */
    val isAtPeak: Boolean
        get() = currentBest != null
            && todaysPeak != null
            && roundTo4(currentBest.rate) >= roundTo4(todaysPeak.rate)

    /** Difference between today's high and right-now. Positive = right
     *  now is below today's peak. Zero / negative = at-or-above the peak.
     *  Null when either signal is missing. */
    val deltaFromPeak: Double?
        get() = if (currentBest != null && todaysPeak != null)
            todaysPeak.rate - currentBest.rate
        else null
}

/**
 * Single point reference into a provider's data — used by the banner and
 * (deliberately) carries the timestamp so the UI can render
 * "11:24 — 3h ago" against it without re-walking the history list.
 */
data class ProviderSnapshot(
    val providerId: String,
    val providerName: String,
    val rate: Double,
    /** Wall-clock timestamp of this rate observation. For the *current*
     *  best this is the document's `completed_at`; for *today's peak*
     *  it's the timestamp of the historical point. */
    val observedAt: Instant,
)

/**
 * Compute the banner inputs from the live rates document and the
 * (optional) rolling history document.
 *
 * @param doc        The just-fetched live rates document.
 * @param history    The rolling-7-day history. May be null while the
 *                   parallel history fetch is still in flight.
 * @param corridor   The currency code currently selected by the user
 *                   (e.g. "INR"). The banner is corridor-scoped — peaks
 *                   for AED→PHP have nothing to do with AED→INR.
 * @param zone       Timezone defining "today". Defaults to the device's
 *                   system zone so a Dubai user at 23:30 sees a Dubai
 *                   "today" not a UTC "today".
 * @param now        Override for tests; defaults to wall clock.
 */
fun computeTodaysBest(
    doc: RatesDocument,
    history: HistoryDocument?,
    corridor: String,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
): TodaysBestSummary {

    // --- 1. Current best (from the live document) ----------------------
    //
    // Mirror the same filter used in RatesViewModel.bestRate: only "ok"
    // and "manual" quotes count, mid_market is excluded (it's a benchmark,
    // not a transferable rate).
    val liveQuotes = doc.corridors[corridor].orEmpty()
        .asSequence()
        .filter { it.providerId != "mid_market" }
        .filter { it.status == "ok" || it.status == "manual" }
        .mapNotNull { q ->
            val r = q.effectiveRate ?: q.rate ?: return@mapNotNull null
            ProviderSnapshot(
                providerId = q.providerId,
                providerName = q.providerName,
                rate = r,
                observedAt = parseInstantOrNull(doc.completedAt) ?: now,
            )
        }

    val currentBest = liveQuotes.maxByOrNull { it.rate }

    // --- 2. Today's peak (from the rolling history) --------------------
    //
    // History is keyed by provider_id but is NOT corridor-scoped at the
    // top level — the published history.json today only carries the
    // primary corridor (INR). For other corridors history may be empty;
    // we degrade gracefully by returning null for todaysPeak so the
    // banner falls back to a single-line "current best" presentation.
    val today: LocalDate = ZonedDateTime.ofInstant(now, zone).toLocalDate()

    val todaysPeak: ProviderSnapshot? = history
        ?.providers
        ?.asSequence()
        ?.filter { (providerId, _) -> providerId != "mid_market" }
        ?.flatMap { (providerId, points) ->
            points.asSequence()
                .mapNotNull { p ->
                    val instant = parseInstantOrNull(p.t) ?: return@mapNotNull null
                    val pointDate = ZonedDateTime.ofInstant(instant, zone).toLocalDate()
                    if (pointDate != today) return@mapNotNull null
                    Triple(providerId, instant, p.rate)
                }
        }
        ?.maxByOrNull { it.third }
        ?.let { (pid, instant, rate) ->
            // Resolve the friendly provider_name from the live document
            // (history only has IDs). If the provider has dropped out of
            // the live doc we fall back to the ID.
            val name = doc.corridors[corridor].orEmpty()
                .firstOrNull { it.providerId == pid }
                ?.providerName
                ?: pid.replace('_', ' ').replaceFirstChar(Char::titlecase)
            ProviderSnapshot(
                providerId = pid,
                providerName = name,
                rate = rate,
                observedAt = instant,
            )
        }

    return TodaysBestSummary(
        currentBest = currentBest,
        todaysPeak = todaysPeak,
    )
}

/** Defensive ISO-8601 parse — returns null instead of throwing so a
 *  single malformed history point can't blank the entire banner. */
private fun parseInstantOrNull(s: String): Instant? =
    runCatching { Instant.parse(s) }.getOrNull()

private fun roundTo4(d: Double): Double =
    kotlin.math.round(d * 10_000.0) / 10_000.0
