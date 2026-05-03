package com.transferrate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.transferrate.app.data.Mosque
import com.transferrate.app.data.OverpassService
import com.transferrate.app.data.haversineMeters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the Mosque Finder screen.
 */
sealed interface MosqueUiState {
    data object Idle : MosqueUiState
    data object LocationNeeded : MosqueUiState        // permission not yet granted
    data object Loading : MosqueUiState
    data class Ready(
        val userLat: Double,
        val userLon: Double,
        val mosques: List<MosqueWithDistance>,
        val radiusMeters: Int,
    ) : MosqueUiState
    data class Failed(val message: String) : MosqueUiState
}

/** A mosque enriched with the great-circle distance from the user. */
data class MosqueWithDistance(
    val mosque: Mosque,
    val distanceMeters: Double,
)

class MosqueViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val service = OverpassService()

    private val _state = MutableStateFlow<MosqueUiState>(MosqueUiState.Idle)
    val state: StateFlow<MosqueUiState> = _state.asStateFlow()

    /** Search radius in metres.  3km is a reasonable city default
     *  - typical walking range in dense urban Muslim-majority areas.
     *  Users in low-density regions need a larger radius via a future
     *  selector. */
    var radiusMeters: Int = 3000
        private set

    fun setRadius(meters: Int) {
        if (meters in 500..20000 && meters != radiusMeters) {
            radiusMeters = meters
            // If we already have a location, re-query with the new radius.
            val s = _state.value
            if (s is MosqueUiState.Ready) {
                searchAt(s.userLat, s.userLon)
            }
        }
    }

    /** Mark that we need permission - usually called before the
     *  permission request flow opens. */
    fun markLocationNeeded() {
        _state.value = MosqueUiState.LocationNeeded
    }

    /** Mark that we tried to get a location but failed (services off,
     *  no providers, timed out, etc.).  UI shows the message + retry +
     *  "show Dubai sample" actions. */
    fun markLocationFailed(message: String) {
        _state.value = MosqueUiState.Failed(message)
    }

    /** Spinner state, used while we wait on the OS for a fresh fix. */
    fun markLoading() {
        _state.value = MosqueUiState.Loading
    }

    /** Search for mosques near a known coordinate (called once we
     *  successfully resolved the user's location, or when the user
     *  manually picks a location). */
    fun searchAt(lat: Double, lon: Double) {
        _state.value = MosqueUiState.Loading
        viewModelScope.launch {
            runCatching { service.nearbyMosques(lat, lon, radiusMeters) }
                .onSuccess { raw ->
                    val withDist = raw
                        .map { m ->
                            MosqueWithDistance(
                                mosque = m,
                                distanceMeters = haversineMeters(
                                    lat, lon, m.lat, m.lon,
                                ),
                            )
                        }
                        .sortedBy { it.distanceMeters }
                    _state.value = MosqueUiState.Ready(
                        userLat = lat,
                        userLon = lon,
                        mosques = withDist,
                        radiusMeters = radiusMeters,
                    )
                }
                .onFailure { e ->
                    _state.value = MosqueUiState.Failed(
                        e.message ?: e::class.simpleName ?: "Unknown error"
                    )
                }
        }
    }

    fun reset() {
        _state.value = MosqueUiState.Idle
    }
}
