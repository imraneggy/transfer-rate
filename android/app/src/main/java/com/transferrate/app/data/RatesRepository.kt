package com.transferrate.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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
        .retryOnConnectionFailure(false)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false        // strict parsing — no quirks
        explicitNulls = false
        coerceInputValues = false
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
                json.decodeFromString<RatesDocument>(body).validate()
            }
        }
    }
}
