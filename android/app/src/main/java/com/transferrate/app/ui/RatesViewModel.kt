package com.transferrate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transferrate.app.data.ProviderQuote
import com.transferrate.app.data.RatesDocument
import com.transferrate.app.data.RatesRepository
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
    ) : RatesUiState {
        /** Independent mid-market rate (provider_id == "mid_market"), shown in
         *  the header card. Sourced from open.er-api.com — NOT from Wise — so
         *  it is genuinely an objective external benchmark.
         *  Wise itself stays in the provider comparison list. */
        val midMarketRate: Double?
            get() = doc.corridors[selectedCurrency]
                ?.firstOrNull { it.providerId == "mid_market" && it.status == "ok" }
                ?.rate

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

    fun refresh() {
        val current = _state.value
        if (current is RatesUiState.Ready) {
            _state.value = current.copy(refreshing = true)
        } else {
            _state.value = RatesUiState.Loading
        }
        fetchFresh()
    }

    private fun fetchFresh() {
        viewModelScope.launch {
            val previousSelected = (_state.value as? RatesUiState.Ready)?.selectedCurrency
            repo.fetch().fold(
                onSuccess = { doc ->
                    val selected = pickCurrency(doc, previousSelected)
                    _state.value = RatesUiState.Ready(doc, selected)
                },
                onFailure = { e ->
                    // If we already have cached data showing, keep it visible
                    // and just clear the refreshing flag — DON'T blow it away.
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
