package com.transferrate.app.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One mosque returned from an Overpass API query.
 *
 * The Overpass API returns OSM elements with `tags` containing whatever
 * the volunteer mapper added: name, denomination, address fragments,
 * opening_hours, etc.  We extract the most useful fields with sensible
 * fallbacks; everything is nullable except the coordinate pair (which
 * the API guarantees).
 */
data class Mosque(
    val id: Long,                    // OSM element ID (stable across queries)
    val name: String,                // displayable label, falls back to "Mosque" when name missing
    val lat: Double,
    val lon: Double,
    val denomination: String? = null, // sunni / shia / ibadi / etc, when tagged
    val address: String? = null,      // best-effort joined from addr:* tags
    val openingHours: String? = null, // raw OSM opening_hours expression
    val operator: String? = null,     // mosque trust/management body, when tagged
)

/**
 * Great-circle distance between two lat/lon pairs in metres.
 * Haversine formula - accurate enough for sorting nearby mosques and
 * cheap enough to call once per result without batching.
 */
fun haversineMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double,
): Double {
    val r = 6371000.0 // Earth radius in metres
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2.0) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

/** Format a metres distance for compact display ("450 m" / "3.2 km"). */
fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m"
    else "%.1f km".format(meters / 1000)
