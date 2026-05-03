package com.transferrate.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Overpass API client - queries OpenStreetMap for mosques near a point.
 *
 * Free, public, community-run service. Rate-limited to "be reasonable"
 * (a handful of queries per minute is fine; bulk extracts go to Geofabrik).
 *
 * Why Overpass + OSM?
 *   * $0 cost, no API key, matches Exchangia's $0-ops promise
 *   * Mosque coverage in UAE/India/Pakistan/Egypt/Indonesia is excellent
 *     (volunteer-mapped streets dense in Muslim-majority regions)
 *   * Returns rich tags: denomination, opening_hours, addr:*, operator
 */
class OverpassService(
    private val client: OkHttpClient = DEFAULT_CLIENT,
) {

    /**
     * Find mosques within `radiusMeters` of (lat, lon).
     *
     * Overpass QL query:
     *   * `node["amenity"="place_of_worship"]["religion"="muslim"]
     *      (around:RADIUS,LAT,LON);`
     *   * `way`  same selectors - mosque buildings as polygons
     *   * `relation` same - large mosque complexes
     *   * `out center;` - emit centroids for ways and relations so
     *     every result has a single (lat, lon)
     *
     * Results are converted to Mosque objects.  No clientside sort -
     * caller can sort by haversine distance from the user.
     */
    suspend fun nearbyMosques(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 5000,
    ): List<Mosque> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:25];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
              relation["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$lat,$lon);
            );
            out center tags;
        """.trimIndent()

        val req = Request.Builder()
            .url(OVERPASS_URL)
            .header("User-Agent", USER_AGENT)
            .post(("data=" + java.net.URLEncoder.encode(query, "UTF-8"))
                .toRequestBody(FORM_TYPE))
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw RuntimeException("Overpass HTTP ${resp.code}: ${resp.message}")
            }
            val body = resp.body?.string()
                ?: throw RuntimeException("Overpass: empty response body")
            parseOverpassResponse(body)
        }
    }

    private fun parseOverpassResponse(body: String): List<Mosque> {
        val root = JSONObject(body)
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<Mosque>(elements.length())
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)

            // For nodes, lat/lon are top-level. For ways and relations,
            // they live under a `center` object (because we asked for
            // `out center`).
            val lat = e.optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
                ?: e.optJSONObject("center")?.optDouble("lat", Double.NaN)
                ?: continue
            val lon = e.optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
                ?: e.optJSONObject("center")?.optDouble("lon", Double.NaN)
                ?: continue
            if (lat.isNaN() || lon.isNaN()) continue

            val tags = e.optJSONObject("tags")
            val name = tags?.optString("name")?.takeIf { it.isNotBlank() }
                ?: tags?.optString("name:en")?.takeIf { it.isNotBlank() }
                ?: "Mosque"

            val denomination = tags?.optString("denomination")?.takeIf { it.isNotBlank() }
            val openingHours = tags?.optString("opening_hours")?.takeIf { it.isNotBlank() }
            val operator = tags?.optString("operator")?.takeIf { it.isNotBlank() }

            // Best-effort address from OSM addr:* tags
            val addr = listOfNotNull(
                tags?.optString("addr:housenumber")?.takeIf { it.isNotBlank() },
                tags?.optString("addr:street")?.takeIf { it.isNotBlank() },
                tags?.optString("addr:city")?.takeIf { it.isNotBlank() },
            ).joinToString(", ").takeIf { it.isNotBlank() }

            out.add(
                Mosque(
                    id = e.optLong("id"),
                    name = name,
                    lat = lat,
                    lon = lon,
                    denomination = denomination,
                    address = addr,
                    openingHours = openingHours,
                    operator = operator,
                )
            )
        }
        return out
    }

    companion object {
        // Public mirror.  Use the .de mirror because of better global
        // routing than the original .ru endpoint.
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

        // OSM volunteer policy: every Overpass client must self-identify
        // with an honest User-Agent so abuse is traceable.  We include
        // a contact path so admins can reach us if our usage causes a
        // problem.
        private const val USER_AGENT =
            "Exchangia/1.0 (https://github.com/imraneggy/transfer-rate)"

        private val FORM_TYPE = "application/x-www-form-urlencoded".toMediaType()

        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
