package com.transferrate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transferrate.app.data.HistoryDocument
import com.transferrate.app.data.ProviderQuote
import com.transferrate.app.data.RatesDocument
import com.transferrate.app.data.RatesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RatesUiState {
    data object Loading : RatesUiState
    data class Ready(
        val doc: RatesDocument,
        val selectedCurrency: String,
        val selectedAmount: Double = 1000.0,
        val refreshing: Boolean = false,
        val history: HistoryDocument? = null,
    ) : RatesUiState {
        /** Independent mid-market rate (provider_id == "mid_market"), shown in
         *  the header card. Sourced from open.er-api.com — NOT from Wise — so
         *  it is genuinely an objective external benchmark.
         *  Wise itself stays in the provider comparison list. */
        val midMarketRate: Double?
            get() = midMarketQuote?.rate

        /** Full mid-market ProviderQuote — used to open the history sheet
         *  when the user taps the MidMarketHeader. Returns null when the
         *  mid-market provider is missing or in an error state. */
        val midMarketQuote: ProviderQuote?
            get() = doc.corridors[selectedCurrency]
                ?.firstOrNull { it.providerId == "mid_market" && it.status == "ok" }

        /** Quotes for the selected corridor, EXCLUDING only the mid-market
         *  benchmark (which is promoted to the header). Wise and all other
         *  providers remain in the list.
         *  Sorted: ok by best rate, then stale, then investigating, then error. */
        val visibleQuotes: List<ProviderQuote>
            get() {
                val all = doc.corridors[selectedCurrency] ?: emptyList()
                return all
                    .filterNot { it.providerId == "mid_market" }
                    .sortedWith(
                        compareBy(
                            { statusOrder(it.status) },
                            { -(it.effectiveRate ?: it.rate ?: 0.0) },
                        )
                    )
            }

        /** The best (highest) rate among verified quotes (ok or manual), for the BEST badge. */
        val bestRate: Double?
            get() = visibleQuotes
                .filter { it.status == "ok" || it.status == "manual" }
                .mapNotNull { it.effectiveRate ?: it.rate }
                .maxOrNull()
    }
    data class Failed(val message: String) : RatesUiState
}

class RatesViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repo = RatesRepository(application.applicationContext)

    private val _state = MutableStateFlow<RatesUiState>(RatesUiState.Loading)
    val state: StateFlow<RatesUiState> = _state.asStateFlow()

    init {
        // Stale-while-revalidate: show cached data instantly on cold start,
        // then trigger a background refresh so the user sees fresh values
        // shortly after.
        viewModelScope.launch {
            repo.loadCached().fold(
                onSuccess = { doc ->
                    val selected = pickCurrency(doc, null)
                    _state.value = RatesUiState.Ready(doc, selected, refreshing = true)
                    fetchFresh()
                },
                onFailure = { fetchFresh() },
            )
        }
    }

    /**
     * User-initiated refresh — the Refresh button.
     *
     * Three-phase flow (v0.28.2 — stale-while-revalidate):
     *
     *   0. **Immediate fetch of the public rates.json.**  The cron tick
     *      may have produced a doc fresher than what the user sees on
     *      screen, in which case the new values land within ~1 second
     *      of tapping refresh — no spinner-and-wait UX.  This is the
     *      perceived-speed win; it doesn't change real freshness, but
     *      it makes the app feel instant.
     *
     *   1. **Upstream scrape trigger.**  POST the Cloudflare Worker so
     *      it dispatches a fresh workflow_dispatch (PAT held in the
     *      Worker's env).  Returns in well under a second.
     *
     *   2. **Poll until completed_at advances.**  After a short 2 s
     *      head-start (was 6 s pre-v0.28.2), poll rates.json every 2 s
     *      (was 4 s) for up to 24 attempts (was 12).  Same ~48 s total
     *      budget; finer-grained detection so a fresh scrape lands on
     *      the screen 0–4 s sooner than before.
     *
     * If the upstream trigger fails (no Worker configured, network
     * blip, Worker returned non-202), Phase 0 has already surfaced the
     * latest cron-published doc — we just clear the spinner.  Never
     * makes the refresh button worse than before.
     */
    fun refresh() {
        val current = _state.value
        if (current is RatesUiState.Ready) {
            _state.value = current.copy(refreshing = true)
        } else {
            _state.value = RatesUiState.Loading
        }
        viewModelScope.launch { performRefresh() }
    }

    private suspend fun performRefresh() {
        val initialState = _state.value as? RatesUiState.Ready
        val previousSelected = initialState?.selectedCurrency
        val previousHistory  = initialState?.history
        // Tracks the latest completed_at we've shown — advances after
        // Phase 0 if the cron-published doc is fresher than what was
        // on screen, then again in Phase 2 if the upstream scrape
        // produces something fresher still.
        var lastSeenCompleted = initialState?.doc?.completedAt

        // Phase 0: immediate fetch of the currently-published doc.  Most
        // refresh taps land while there's a doc on Pages that's already
        // a few minutes newer than the cached one on screen — surfacing
        // it within ~1 s gives the user instant feedback while the
        // upstream-trigger flow runs in the background.
        repo.fetch().fold(
            onSuccess = { doc ->
                val advanced = lastSeenCompleted == null
                    || doc.completedAt != lastSeenCompleted
                if (advanced) {
                    val selected = pickCurrency(doc, previousSelected)
                    _state.value = RatesUiState.Ready(
                        doc = doc,
                        selectedCurrency = selected,
                        history = previousHistory,
                        refreshing = true,   // keep spinner — more updates may follow
                    )
                    lastSeenCompleted = doc.completedAt
                    fetchHistory()
                }
            },
            onFailure = { /* don't fail fast — Phase 1 may still succeed */ },
        )

        // Phase 1: ask the Worker to dispatch a fresh scrape upstream.
        // Quick POST — should return in well under a second.
        val triggered = repo.triggerUpstreamRefresh()

        if (!triggered) {
            // No Worker, or Worker rejected.  Phase 0 already surfaced
            // the latest cron-published doc (or kept the previous one
            // if Phase 0 also failed); just clear the spinner.
            val current = _state.value
            if (current is RatesUiState.Ready) {
                _state.value = current.copy(refreshing = false)
            } else {
                performSingleFetch(previousSelected, previousHistory)
            }
            return
        }

        // Phase 2: poll rates.json until completed_at advances past
        // whatever Phase 0 surfaced.  Tightened from v0.28.1: 2 s
        // head-start (was 6 s), 2 s interval × 24 attempts (was 4 s ×
        // 12).  Same ~48 s total budget — finer detection grain.
        delay(2_000)
        val pollIntervalMs = 2_000L
        val maxAttempts = 24
        var settled = false
        for (attempt in 1..maxAttempts) {
            val result = repo.fetch()
            result.fold(
                onSuccess = { doc ->
                    val advanced = lastSeenCompleted == null
                        || doc.completedAt != lastSeenCompleted
                    if (advanced) {
                        val selected = pickCurrency(doc, previousSelected)
                        _state.value = RatesUiState.Ready(
                            doc = doc,
                            selectedCurrency = selected,
                            history = previousHistory,
                        )
                        lastSeenCompleted = doc.completedAt
                        fetchHistory()
                        settled = true
                    }
                },
                onFailure = { /* ignore intermediate failures during polling */ },
            )
            if (settled) return
            if (attempt < maxAttempts) delay(pollIntervalMs)
        }

        // Timeout: the upstream scrape likely failed or took longer
        // than the budget.  Phase 0 already showed the user the freshest
        // available cron-tick doc; just clear the spinner so the UI
        // stops spinning.
        val current = _state.value
        if (current is RatesUiState.Ready) {
            _state.value = current.copy(refreshing = false)
        } else {
            performSingleFetch(previousSelected, previousHistory)
        }
    }

    /** Single-shot fetch + state update.  Used as the fallback path
     *  when the upstream-trigger flow can't run, and as the final
     *  resync after a polling timeout. */
    private suspend fun performSingleFetch(
        previousSelected: String?,
        previousHistory: HistoryDocument?,
    ) {
        repo.fetch().fold(
            onSuccess = { doc ->
                val selected = pickCurrency(doc, previousSelected)
                _state.value = RatesUiState.Ready(
                    doc = doc,
                    selectedCurrency = selected,
                    history = previousHistory,
                )
                fetchHistory()
            },
            onFailure = { e ->
                val current = _state.value
                if (current is RatesUiState.Ready) {
                    _state.value = current.copy(refreshing = false)
                } else {
                    _state.value = RatesUiState.Failed(
                        e.message ?: e::class.simpleName ?: "unknown error"
                    )
                }
            },
        )
    }

    /** Cold-start path: no UI affordance, no upstream trigger — just
     *  load the cache or fetch.  Kept distinct from refresh() so the
     *  init flow stays snappy. */
    private fun fetchFresh() {
        viewModelScope.launch {
            val previousSelected = (_state.value as? RatesUiState.Ready)?.selectedCurrency
            val previousHistory  = (_state.value as? RatesUiState.Ready)?.history
            performSingleFetch(previousSelected, previousHistory)
        }
    }

    private fun fetchHistory() {
        viewModelScope.launch {
            repo.fetchHistory().fold(
                onSuccess = { hist ->
                    val current = _state.value
                    if (current is RatesUiState.Ready) {
                        _state.value = current.copy(history = hist)
                    }
                },
                onFailure = { /* non-fatal — sparklines simply absent */ },
            )
        }
    }

    private fun pickCurrency(doc: RatesDocument, previous: String?): String {
        return previous?.takeIf { doc.corridors.containsKey(it) }
            ?: doc.corridors.keys.firstOrNull { it == "INR" }
            ?: doc.corridors.keys.firstOrNull()
            ?: "INR"
    }

    fun selectCurrency(code: String) {
        val s = _state.value
        if (s is RatesUiState.Ready && s.selectedCurrency != code && doc(s).corridors.containsKey(code)) {
            _state.value = s.copy(selectedCurrency = code)
        }
    }

    /** Update the user's send amount. Bounded at the View layer; we just
     *  refuse implausible values defensively. */
    fun setAmount(amount: Double) {
        val s = _state.value
        if (s !is RatesUiState.Ready) return
        if (amount !in 1.0..1_000_000.0) return
        if (s.selectedAmount == amount) return
        _state.value = s.copy(selectedAmount = amount)
    }

    private fun doc(s: RatesUiState.Ready): RatesDocument = s.doc
}

private fun statusOrder(s: String): Int = when (s) {
    "ok" -> 0
    "manual" -> 1
    "stale" -> 2
    "investigating" -> 3
    else -> 4
}
