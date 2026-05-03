package com.transferrate.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * One-shot device location.
 *
 * Uses Android's built-in LocationManager (NOT Google Play Services
 * FusedLocationProviderClient) so the APK works on de-Googled devices
 * (F-Droid users, GrapheneOS, Huawei). Trade-off: on stock Android the
 * fix is slightly slower (no fused-sensor hint), but for a "find a
 * mosque now" UX a few-hundred-ms latency is invisible.
 *
 * Strategy:
 *   1. Try the most recent cached fix from any provider - usually
 *      good enough and instant.
 *   2. If no cache or stale (>5 min), request a single fresh fix
 *      from the best available provider (GPS, then network).
 *
 * Returns null if no fix can be obtained within `timeoutMs`.
 *
 * Caller must verify ACCESS_FINE_LOCATION (or COARSE) permission.
 * The @SuppressLint is intentional: we already check explicitly.
 */
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
        maxStaleMs: Long = 5 * 60 * 1000L, // 5 minutes
        timeoutMs: Long = 10_000L,
    ): Location? {
        if (!hasPermission(ctx)) return null

        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Step 1: best-recent fix from any provider.
        val now = System.currentTimeMillis()
        val cached = lm.allProviders
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { now - it.time <= maxStaleMs }
            .maxByOrNull { it.time }
        if (cached != null) return cached

        // Step 2: request a single fresh fix from the best provider.
        val provider = bestProvider(lm) ?: return null

        return suspendCancellableCoroutine { cont ->
            val handler = android.os.Handler(Looper.getMainLooper())
            val timeoutToken = Runnable {
                if (cont.isActive) cont.resume(null)
                runCatching { lm.removeUpdates(NoOpListener) }
            }
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(loc: Location) {
                    handler.removeCallbacks(timeoutToken)
                    runCatching { lm.removeUpdates(this) }
                    if (cont.isActive) cont.resume(loc)
                }
                @Deprecated("Required for older API levels but unused")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            try {
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                handler.postDelayed(timeoutToken, timeoutMs)
                cont.invokeOnCancellation {
                    handler.removeCallbacks(timeoutToken)
                    runCatching { lm.removeUpdates(listener) }
                }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

    private fun bestProvider(lm: LocationManager): String? {
        // Prefer GPS for accuracy when available; fall back to network.
        val candidates = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return candidates.firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    }

    // Sentinel listener for cleanup-by-reference (we never actually
    // register this; it exists just so removeUpdates can be called
    // safely even if registration failed).
    private val NoOpListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {}
        @Deprecated("Required for older API levels but unused")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
}
