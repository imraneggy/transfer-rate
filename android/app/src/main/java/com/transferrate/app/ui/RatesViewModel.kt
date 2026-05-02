package com.transferrate.app.ui

import androidx.lifecycle.ViewModel
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
        val refreshing: Boolean = false,
    ) : RatesUiState {
        /** Quotes for the currently selected corridor, sorted: ok by best rate, then stale, then investigating, then error. */
        val visibleQuotes: List<ProviderQuote>
            get() = (doc.corridors[selectedCurrency] ?: emptyList())
                .sortedWith(
                    compareBy(
                        { statusOrder(it.status) },
                        { -(it.effectiveRate ?: it.rate ?: 0.0) },
                    )
                )

        /** The best (highest) rate among the OK quotes, used to highlight the winner. */
        val bestRate: Double?
            get() = visibleQuotes
                .filter { it.status == "ok" }
                .mapNotNull { it.effectiveRate ?: it.rate }
                .maxOrNull()
    }
    data class Failed(val message: String) : RatesUiState
}

class RatesViewModel(
    private val repo: RatesRepository = RatesRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<RatesUiState>(RatesUiState.Loading)
    val state: StateFlow<RatesUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        val current = _state.value
        if (current is RatesUiState.Ready) {
            _state.value = current.copy(refreshing = true)
        } else {
            _state.value = RatesUiState.Loading
        }
        viewModelScope.launch {
            repo.fetch().fold(
                onSuccess = { doc ->
                    val previousSelected = (current as? RatesUiState.Ready)?.selectedCurrency
                    val selected = previousSelected
                        ?.takeIf { doc.corridors.containsKey(it) }
                        ?: doc.corridors.keys.firstOrNull { it == "INR" }
                        ?: doc.corridors.keys.firstOrNull()
                        ?: "INR"
                    _state.value = RatesUiState.Ready(doc, selected)
                },
                onFailure = { e ->
                    _state.value = RatesUiState.Failed(
                        e.message ?: e::class.simpleName ?: "unknown error"
                    )
                },
            )
        }
    }

    fun selectCurrency(code: String) {
        val s = _state.value
        if (s is RatesUiState.Ready && s.selectedCurrency != code && doc(s).corridors.containsKey(code)) {
            _state.value = s.copy(selectedCurrency = code)
        }
    }

    private fun doc(s: RatesUiState.Ready): RatesDocument = s.doc
}

private fun statusOrder(s: String): Int = when (s) {
    "ok" -> 0
    "stale" -> 1
    "investigating" -> 2
    else -> 3
}
