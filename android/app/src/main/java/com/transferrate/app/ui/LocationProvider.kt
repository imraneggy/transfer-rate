package com.transferrate.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import kotlin.coroutines.resume

/**
 * One-shot device location with explicit success/failure reporting.
 *
 * Uses Android's built-in LocationManager (NOT Google Play Services
 * FusedLocationProviderClient) so the APK works on de-Googled devices
 * (F-Droid users, GrapheneOS, Huawei). Modern API path:
 * `LocationManager.getCurrentLocation()` (API 30+) which is more
 * reliable than the deprecated `requestSingleUpdate`.
 *
 * Strategy:
 *   1. If a recent cached fix (under 2 min) exists, return it - fastest
 *      path, often instant.
 *   2. Otherwise, request a single fresh fix from the best enabled
 *      provider. Times out after 20s (give GPS time to acquire on
 *      cold start).
 *
 * Returns a `LocationResult` so the caller can distinguish between
 * "permission missing", "location services off", "no providers
 * enabled", and "timeout" - this matters because the UX response
 * differs (re-prompt vs send to settings vs wait).
 */
sealed interface LocationResult {
    data class Ok(val location: Location) : LocationResult
    data object NoPermission : LocationResult
    data object ServicesDisabled : LocationResult       // user has "Location" off in OS settings
    data object NoProvidersEnabled : LocationResult     // GPS + Network both off
    data object Timeout : LocationResult                // never got a fix within deadline
    data class Error(val message: String) : LocationResult
}

object LocationProvider {

    fun hasPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(
        ctx: Context,
        maxStaleMs: Long = 2 * 60 * 1000L,    // 2 minutes - tighter than before so we
                                              // don't show mosques 50km from where the
                                              // user actually is right now
        timeoutMs: Long = 20_000L,            // 20s gives cold-start GPS time to lock
    ): LocationResult {
        if (!hasPermission(ctx)) return LocationResult.NoPermission

        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Quick check: is the system Location toggle on at all?
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            return LocationResult.ServicesDisabled
        }

        // Step 1: best-recent fix from any provider that we have permission for.
        val now = System.currentTimeMillis()
        val cached = lm.allProviders
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.time <= maxStaleMs }
            .maxByOrNull { it.time }
        if (cached != null) return LocationResult.Ok(cached)

        // Step 2: pick the best enabled provider for a fresh fix.
        val provider = bestProvider(lm)
            ?: return LocationResult.NoProvidersEnabled

        // Step 3: getCurrentLocation (API 30+) - modern non-deprecated path.
        return suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            val executor = Executor { it.run() }   // run callbacks on the same thread
            val timeoutHandle = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutTask = Runnable {
                if (cont.isActive) {
                    signal.cancel()
                    cont.resume(LocationResult.Timeout)
                }
            }
            try {
                lm.getCurrentLocation(
                    provider,
                    signal,
                    executor,
                ) { loc ->
                    timeoutHandle.removeCallbacks(timeoutTask)
                    if (cont.isActive) {
                        cont.resume(
                            if (loc != null) LocationResult.Ok(loc)
                            else LocationResult.Timeout
                        )
                    }
                }
                timeoutHandle.postDelayed(timeoutTask, timeoutMs)
                cont.invokeOnCancellation {
                    timeoutHandle.removeCallbacks(timeoutTask)
                    signal.cancel()
                }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(LocationResult.NoPermission)
            } catch (e: Exception) {
                if (cont.isActive) {
                    cont.resume(
                        LocationResult.Error(
                            e.message ?: e::class.simpleName ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }

    /** Pick the most-accurate enabled provider; null if none enabled. */
    private fun bestProvider(lm: LocationManager): String? {
        // Order: GPS (best accuracy) -> NETWORK (cell+wifi triangulation).
        // FUSED is API 31+ and pulls from both, but only available where
        // Google Play Services have registered it - skip to keep the
        // dependency-free promise.
        val candidates = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        )
        return candidates.firstOrNull {
            runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
        }
    }
}
