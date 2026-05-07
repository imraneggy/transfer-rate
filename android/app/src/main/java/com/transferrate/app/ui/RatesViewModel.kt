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
     * Two-phase flow so the user actually sees freshly-scraped data:
     *   1. Trigger the upstream scrape via the Cloudflare Worker
     *      (the Worker proxies workflow_dispatch to GitHub Actions
     *      with a PAT held in its env).  Returns in <1 sec.
     *   2. Poll rates.json (cache-busted on every call) until
     *      `completed_at` advances past what we had before, or until
     *      we time out at ~50 sec (slightly above the typical 30-35 s
     *      scrape duration).
     *
     * If the upstream trigger fails (no Worker configured, network
     * blip, Worker returned non-202), we fall through to a single
     * snappy fetch — same UX as the pre-v0.22 refresh.  Never makes
     * the refresh button worse than before.
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
        val previousSelected = (_state.value as? RatesUiState.Ready)?.selectedCurrency
        val previousHistory  = (_state.value as? RatesUiState.Ready)?.history
        val previousCompleted = (_state.value as? RatesUiState.Ready)?.doc?.completedAt

        // Phase 1: ask the Worker to dispatch a fresh scrape upstream.
        // Quick POST — should return in well under a second.
        val triggered = repo.triggerUpstreamRefresh()

        if (!triggered) {
            // No Worker, or Worker rejected.  Fall back to the simple
            // single-fetch behaviour the app had before v0.22.
            performSingleFetch(previousSelected, previousHistory)
            return
        }

        // Phase 2: poll rates.json until completed_at advances.  Initial
        // 6 s headstart absorbs the dispatch + GHA queue latency before
        // we waste cycles on requests that can only re-fetch the same
        // stale doc.  Then poll every 4 s up to ~12 attempts (50 s
        // total budget).
        delay(6_000)
        val pollIntervalMs = 4_000L
        val maxAttempts = 12
        var settled = false
        for (attempt in 1..maxAttempts) {
            val result = repo.fetch()
            result.fold(
                onSuccess = { doc ->
                    val advanced = previousCompleted == null
                        || doc.completedAt != previousCompleted
                    if (advanced) {
                        val selected = pickCurrency(doc, previousSelected)
                        _state.value = RatesUiState.Ready(
                            doc = doc,
                            selectedCurrency = selected,
                            history = previousHistory,
                        )
                        fetchHistory()
                        settled = true
                    }
                },
                onFailure = { /* ignore intermediate failures during polling */ },
            )
            if (settled) return
            if (attempt < maxAttempts) delay(pollIntervalMs)
        }

        // Timeout: the scrape likely failed or took longer than 50 s.
        // Surface whatever the latest fetch produced, even if it's the
        // same doc we already had — at least the spinner stops.
        performSingleFetch(previousSelected, previousHistory)
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
