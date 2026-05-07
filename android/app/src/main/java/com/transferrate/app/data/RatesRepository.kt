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
    // Cloudflare Worker that proxies a workflow_dispatch to GitHub
    // Actions (held PAT lives in the Worker's encrypted env). When
    // the user taps Refresh, we ping this to force a fresh scrape;
    // null disables the upstream-trigger phase (useful in tests).
    private val refreshTriggerUrl: String? =
        com.transferrate.app.BuildConfig.REFRESH_TRIGGER_URL.takeIf { it.isNotBlank() },
    private val refreshTriggerSecret: String? =
        com.transferrate.app.BuildConfig.REFRESH_TRIGGER_SECRET.takeIf { it.isNotBlank() },
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
            val historyBase = ratesUrl.removeSuffix("rates.json") + "history.json"
            val req = Request.Builder()
                .url(bustCache(historyBase))
                .header("Accept", "application/json")
                .header("User-Agent", "TransferRateApp/0.8.0 Android")
                .header("Pragma", "no-cache")
                .cacheControl(NO_CACHE_NO_STORE)
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
                .url(bustCache(ratesUrl))
                .header("Accept", "application/json")
                .header("User-Agent", "TransferRateApp/0.1.0 Android")
                // Pragma is the HTTP/1.0 spelling; some legacy proxies
                // honour it where Cache-Control would be ignored.
                .header("Pragma", "no-cache")
                .cacheControl(NO_CACHE_NO_STORE)
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

    /**
     * Ask the Cloudflare Worker to dispatch a fresh scrape upstream.
     * The Worker forwards the request to GitHub's workflow_dispatch
     * endpoint with the PAT held in its encrypted env; we never
     * touch the PAT from the client.
     *
     * Returns true if the Worker accepted the request (HTTP 202),
     * false otherwise. Callers should NOT block waiting for the
     * scrape itself to finish — that takes ~25-35 seconds. Pattern:
     *   1. triggerUpstreamRefresh() returns quickly
     *   2. Caller polls fetch() until completedAt advances or it
     *      times out, whichever first.
     *
     * Quietly returns false if no Worker is configured, so feature
     * flags / dev builds without the secret degrade to the previous
     * behaviour (just refetch existing rates.json).
     */
    suspend fun triggerUpstreamRefresh(): Boolean = withContext(Dispatchers.IO) {
        val url = refreshTriggerUrl ?: return@withContext false
        runCatching {
            val builder = Request.Builder()
                .url("$url/refresh")
                .header("Accept", "application/json")
                .header("User-Agent", "TransferRateApp/0.22.0 Android")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
            refreshTriggerSecret?.let {
                builder.header("Authorization", "Bearer $it")
            }
            http.newCall(builder.build()).execute().use { resp ->
                resp.code == 202
            }
        }.getOrDefault(false)
    }

    /**
     * Append a unique-per-request cache-busting query parameter so every
     * refresh hits origin (or at least gets a fresh cache key).
     *
     * GitHub Pages serves rates.json with `Cache-Control: max-age=600`,
     * which means a Fastly edge node CAN legitimately hold up to a
     * 10-minute-old copy of the document. Our `Cache-Control: no-cache`
     * request header is supposed to force revalidation but real-world
     * CDNs, ISP transparent caches, and corporate proxies inconsistently
     * honour it — particularly on cellular networks where carrier-grade
     * caching is common.
     *
     * A unique query string forces a distinct cache key per request,
     * which every well-behaved cache (Fastly, Cloudflare, Squid, default
     * Apache mod_cache, ISP boxes) treats as a brand-new URL. Pages
     * still serves the same underlying file on a hit; we just bypass
     * the intermediate caching layer entirely.
     *
     * Origin staleness is a separate problem (the scrape workflow
     * actually has to run for rates.json itself to update). This helper
     * only addresses the "between origin and device" caching surface.
     */
    private fun bustCache(url: String): String {
        val sep = if ('?' in url) '&' else '?'
        return "$url${sep}_t=${System.currentTimeMillis()}"
    }

    companion object {
        private const val CACHE_MAX_AGE_MS = 24L * 60L * 60L * 1000L  // 24 hours

        private val NO_CACHE_NO_STORE = okhttp3.CacheControl.Builder()
            .noCache()
            .noStore()
            .build()
    }
}
