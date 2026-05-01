package com.transferrate.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.transferrate.app.data.RatesDocument
import com.transferrate.app.data.RatesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RatesUiState {
    data object Loading : RatesUiState
    data class Ready(val doc: RatesDocument, val refreshing: Boolean = false) : RatesUiState
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
                    // Sort providers: ok by best effective rate desc, then stale,
                    // then investigating, then error. Keeps the most useful
                    // information at the top of the list.
                    val ordered = doc.providers.sortedWith(
                        compareBy(
                            { statusOrder(it.status) },
                            { -(it.effectiveRate ?: it.rate ?: 0.0) },
                        )
                    )
                    _state.value = RatesUiState.Ready(doc.copy(providers = ordered))
                },
                onFailure = { e ->
                    _state.value = RatesUiState.Failed(
                        e.message ?: e::class.simpleName ?: "unknown error"
                    )
                },
            )
        }
    }

    private fun statusOrder(s: String): Int = when (s) {
        "ok" -> 0
        "stale" -> 1
        "investigating" -> 2
        else -> 3
    }
}
