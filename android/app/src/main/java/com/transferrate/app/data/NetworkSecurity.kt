package com.transferrate.app.data

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Outbound host allowlist enforced at the OkHttp layer.
 *
 * Why this exists in addition to `network_security_config.xml`:
 *
 *   Android's `<network-security-config>` does NOT actually restrict
 *   *which* hosts the app can talk to.  Listing a host inside a
 *   `<domain-config>` block only overrides the trust-anchor / cleartext
 *   policy for that host.  Any HTTPS connection to any other host still
 *   succeeds via the `<base-config>`'s system-CA trust.
 *
 *   The "allowlist" promise the app makes to its users (and that the
 *   Play Store description states) therefore cannot be enforced by the
 *   platform alone.  This interceptor is the actual enforcement: any
 *   OkHttpClient that includes it will throw on a request to a host
 *   outside [ALLOWED_HOSTS], regardless of whether a code path or a
 *   future dependency tried to construct one.
 *
 * Both [com.transferrate.app.data.RatesRepository] and
 * [com.transferrate.app.data.OverpassService] share this interceptor so
 * the policy is single-sourced.  Adding a new outbound host means
 * adding it here AND to the `<domain-config>` block in
 * `res/xml/network_security_config.xml` so the platform-level cleartext
 * + cert-pinning policy stays consistent with the application-level
 * allowlist.
 */
internal val ALLOWED_HOSTS: Set<String> = setOf(
    // Static rates / history JSON, served by GitHub Pages.
    "imraneggy.github.io",
    // Cloudflare Worker proxying workflow_dispatch (refresh button).
    "transfer-rate-refresh.imranbatchait.workers.dev",
    // OpenStreetMap Overpass mirror (mosque finder).
    "overpass-api.de",
    // OpenStreetMap raster tiles (mosque finder map background).
    "tile.openstreetmap.org",
    // Common subdomains for OSM tile load-balancing (a.tile, b.tile, c.tile).
    "a.tile.openstreetmap.org",
    "b.tile.openstreetmap.org",
    "c.tile.openstreetmap.org",
)

/**
 * OkHttp [Interceptor] that throws on any request whose host is not in
 * [ALLOWED_HOSTS].  Add to any [okhttp3.OkHttpClient.Builder] via
 * `addInterceptor(HostAllowlistInterceptor)`.
 *
 * The check is intentionally on the *resolved* request (post-redirect)
 * because OkHttp invokes interceptors at every redirect hop when
 * `followRedirects(true)`.  In the production clients we set
 * `followRedirects(false)` so a redirect to a disallowed host fails as
 * a 3xx the caller can inspect, rather than being followed silently.
 */
internal object HostAllowlistInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host.lowercase()
        if (host !in ALLOWED_HOSTS) {
            throw IOException(
                "Blocked outbound HTTPS to disallowed host: $host. " +
                "Add to ALLOWED_HOSTS in NetworkSecurity.kt and to " +
                "<domain-config> in network_security_config.xml if intended.",
            )
        }
        return chain.proceed(chain.request())
    }
}
