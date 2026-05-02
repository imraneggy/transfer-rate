package com.transferrate.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for fetching rates. Holds no state — the ViewModel
 * keeps the cached document.
 *
 * Security choices:
 *   * Strict TLS (OkHttp default). No certificate pinning because the
 *     publisher (GitHub Pages) rotates certs frequently and a hard-pin
 *     would brick the app on every rotation. Network Security Config
 *     restricts the domain instead, which is a stronger control here.
 *   * 10s call timeout. App should fail fast and the user can retry.
 *   * `ignoreUnknownKeys = true` so future schema additions on the server
 *     don't break older clients in the field.
 */
class RatesRepository(
    private val context: Context? = null,
    // Override in tests. Production uses the GitHub Pages URL.
    private val ratesUrl: String =
        "https://imraneggy.github.io/transfer-rate/rates.json",
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        explicitNulls = false
        coerceInputValues = false
    }

    private val cacheFile: File?
        get() = context?.filesDir?.let { File(it, "rates-cache.json") }

    /**
     * Stale-while-revalidate: read the on-disk cache (if any), return it
     * immediately if it exists and is younger than 24h. The caller can
     * then trigger fetch() to revalidate in the background.
     */
    suspend fun loadCached(): Result<RatesDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val f = cacheFile ?: error("no context, cannot load cache")
            if (!f.exists()) error("no cache yet")
            val ageMs = System.currentTimeMillis() - f.lastModified()
            if (ageMs > CACHE_MAX_AGE_MS) {
                error("cache too old (${ageMs / 60_000} min)")
            }
            val text = f.readText(Charsets.UTF_8)
            json.decodeFromString<RatesDocument>(text).validate()
        }
    }

    /** Fetch the rolling 7-day history for sparkline rendering. */
    suspend fun fetchHistory(): Result<HistoryDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val historyUrl = ratesUrl.removeSuffix("rates.json") + "history.json"
            val req = Request.Builder()
                .url(historyUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "TransferRateApp/0.8.0 Android")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string() ?: error("Empty body")
                require(body.length < 5_000_000) { "History too large: ${body.length}" }
                json.decodeFromString<HistoryDocument>(body).validate()
            }
        }
    }

    suspend fun fetch(): Result<RatesDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(ratesUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "TransferRateApp/0.1.0 Android")
                .cacheControl(okhttp3.CacheControl.Builder().noCache().build())
                .get()
                .build()

            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}")
                }
                val body = resp.body?.string()
                    ?: error("Empty body")
                require(body.length < 1_000_000) {
                    "Document larger than 1 MB — refusing"
                }
                val doc = json.decodeFromString<RatesDocument>(body).validate()
                // Persist for next cold start
                cacheFile?.runCatching { writeText(body, Charsets.UTF_8) }
                doc
            }
        }
    }

    companion object {
        private const val CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L  // 24 hours
    }
}
