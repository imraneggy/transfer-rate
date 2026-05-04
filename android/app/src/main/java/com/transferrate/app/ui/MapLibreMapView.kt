package com.transferrate.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.transferrate.app.data.Mosque
import com.transferrate.app.data.formatDistance
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

/**
 * Compose wrapper around MapLibre Native MapView.
 *
 * MapLibre Native is the BSD-licensed community fork of mapbox-gl-native
 * after Mapbox v2 went paid in 2020.  We render OSM raster tiles through
 * an inline style JSON so no API key is needed and the "$0 ops" promise
 * holds.  Vector tiles would feel smoother but require a tile vendor
 * (MapTiler / Stadia / Versatiles) - we can swap the style URL later
 * without touching this file.
 *
 * Lifecycle handling is critical for MapLibre - the GLSurfaceView holds
 * GPU resources that must be released around onPause/onResume/onDestroy.
 *
 * @param centerLat   latitude to centre the camera at
 * @param centerLon   longitude to centre the camera at
 * @param zoom        camera zoom (3 = world, 14 = neighbourhood, 19 = building)
 * @param mosques     mosques + their distances from the user; rendered as
 *                    teal pins with the distance baked into the icon
 *                    bitmap (e.g. "350 m" / "1.2 km")
 * @param userLat     user's latitude; when non-null, a blue-dot marker
 *                    is rendered at this position
 * @param userLon     user's longitude
 * @param onMarkerTap fired when the user taps a mosque marker
 * @param modifier    standard Compose modifier
 */
@Composable
fun OsmMapView(
    centerLat: Double,
    centerLon: Double,
    zoom: Double = 14.0,
    mosques: List<MosqueWithDistance> = emptyList(),
    userLat: Double? = null,
    userLon: Double? = null,
    onMarkerTap: (Mosque) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { MapLibre.getInstance(ctx.applicationContext) }

    // We hold the SymbolManager + Style so subsequent recompositions
    // can register new icons + add/remove markers without rebuilding
    // the whole map (which would re-bind the GL texture atlas and flicker).
    val symbolManagerRef = remember { arrayOf<SymbolManager?>(null) }
    val styleRef         = remember { arrayOf<Style?>(null) }
    val mapViewRef       = remember { arrayOf<MapView?>(null) }

    val mapView = remember {
        MapLibre.getInstance(ctx.applicationContext)
        MapView(ctx).apply {
            mapViewRef[0] = this
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(centerLat, centerLon))
                    .zoom(zoom)
                    .build()
                map.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE)) { style ->
                    styleRef[0] = style
                    // Register the user-location bitmap once - it never changes.
                    style.addImage(USER_LOCATION_ICON_ID, makeUserLocationBitmap())
                    val sm = SymbolManager(this, map, style).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                        addClickListener { sym ->
                            val payloadId = sym.data?.asString?.toLongOrNull()
                            val match = mosqueIndex[payloadId]
                            if (match != null) onMarkerTap(match)
                            true
                        }
                    }
                    symbolManagerRef[0] = sm
                    syncMarkers(style, sm, mosques, userLat, userLon)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mv = mapViewRef[0] ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mv.onStart()
                Lifecycle.Event.ON_RESUME -> mv.onResume()
                Lifecycle.Event.ON_PAUSE -> mv.onPause()
                Lifecycle.Event.ON_STOP -> mv.onStop()
                Lifecycle.Event.ON_DESTROY -> mv.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef[0]?.onPause()
            mapViewRef[0]?.onStop()
            mapViewRef[0]?.onDestroy()
            mapViewRef[0] = null
            symbolManagerRef[0] = null
            styleRef[0] = null
        }
    }

    LaunchedEffect(centerLat, centerLon, zoom) {
        mapViewRef[0]?.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(centerLat, centerLon))
                .zoom(zoom)
                .build()
        }
    }

    LaunchedEffect(mosques, userLat, userLon) {
        val sm = symbolManagerRef[0]
        val style = styleRef[0]
        if (sm != null && style != null) {
            syncMarkers(style, sm, mosques, userLat, userLon)
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView })
    }
}

/**
 * Mosque marker icon with the distance text baked into the bitmap.
 *
 * Why bake the distance in instead of using a SymbolOptions text field?
 * MapLibre's text rendering needs a `glyphs` URL in the style JSON
 * (a PBF font endpoint).  Our OSM raster style doesn't have one, and
 * adding a glyphs vendor would re-introduce the API-key dependency
 * we deliberately avoided.  Baking text into the bitmap keeps the
 * style self-contained at the cost of one bitmap per mosque
 * (~80x60 ARGB = 19 KB each; 30 mosques = ~580 KB texture memory).
 */
private fun makeMosqueMarkerWithDistance(distanceLabel: String): Bitmap {
    val w = 90
    val h = 64
    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bm)
    // Pin (top of bitmap)
    val cx = w / 2f
    val cy = 18f
    val outer = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.WHITE
    }
    val inner = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.rgb(0, 180, 158)  // brand teal
    }
    c.drawCircle(cx, cy, 16f, outer)
    c.drawCircle(cx, cy, 12f, inner)

    // Distance label background pill
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = AColor.rgb(14, 31, 58)  // brand deep navy
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val labelBounds = Rect()
    textPaint.getTextBounds(distanceLabel, 0, distanceLabel.length, labelBounds)
    val padH = 7f
    val padV = 4f
    val rectLeft = cx - labelBounds.width() / 2f - padH
    val rectRight = cx + labelBounds.width() / 2f + padH
    val rectTop = 38f
    val rectBottom = rectTop + labelBounds.height() + padV * 2
    val bgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.WHITE
    }
    val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = AColor.rgb(184, 187, 177)  // brand outlineVariant light
    }
    val cornerR = (rectBottom - rectTop) / 2f
    c.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, cornerR, cornerR, bgPaint)
    c.drawRoundRect(rectLeft, rectTop, rectRight, rectBottom, cornerR, cornerR, borderPaint)

    // Centre the text vertically inside the pill
    val textY = rectTop + padV - labelBounds.top.toFloat()
    c.drawText(distanceLabel, cx, textY, textPaint)
    return bm
}

/**
 * "You are here" marker for the user's own position.
 *
 * Material-blue dot on a white ring with a translucent blue accuracy
 * halo around it - the universal phone-app convention.
 */
private fun makeUserLocationBitmap(): Bitmap {
    val size = 56
    val bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bm)
    val cx = size / 2f
    val cy = size / 2f
    // Translucent halo (suggests accuracy radius without being literal)
    val halo = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.argb(60, 33, 150, 243)  // material blue 500 @ 24%
    }
    c.drawCircle(cx, cy, size / 2f - 2f, halo)
    // White ring
    val ring = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.WHITE
    }
    c.drawCircle(cx, cy, 14f, ring)
    // Solid blue centre
    val dot = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.rgb(33, 150, 243)
    }
    c.drawCircle(cx, cy, 10f, dot)
    return bm
}

/**
 * Track currently-displayed mosques by id so the symbol tap handler
 * can look them up.  Updated synchronously inside syncMarkers.
 */
private val mosqueIndex = HashMap<Long, Mosque>()

private const val USER_LOCATION_ICON_ID = "user-location"
private const val USER_LOCATION_SYMBOL_KEY = "__user__"

/** Per-mosque icon ID = "mosque-<osmId>" so each pin's text label is
 *  rendered into its own bitmap. */
private fun mosqueIconId(osmId: Long): String = "mosque-$osmId"

private fun syncMarkers(
    style: Style,
    sm: SymbolManager,
    mosques: List<MosqueWithDistance>,
    userLat: Double?,
    userLon: Double?,
) {
    sm.deleteAll()
    mosqueIndex.clear()

    // User location marker first (sits below mosque pins in z-order
    // because we add it earlier; SymbolManager renders in insertion order).
    if (userLat != null && userLon != null) {
        sm.create(
            SymbolOptions()
                .withLatLng(LatLng(userLat, userLon))
                .withIconImage(USER_LOCATION_ICON_ID)
                .withIconSize(1.0f)
                .withData(com.google.gson.JsonPrimitive(USER_LOCATION_SYMBOL_KEY))
        )
    }

    // Each mosque gets its own icon image registered with the distance
    // baked in; idempotent on re-render because addImage replaces by key.
    mosques.forEach { mwd ->
        val m = mwd.mosque
        mosqueIndex[m.id] = m
        val iconId = mosqueIconId(m.id)
        style.addImage(iconId, makeMosqueMarkerWithDistance(formatDistance(mwd.distanceMeters)))
        sm.create(
            SymbolOptions()
                .withLatLng(LatLng(m.lat, m.lon))
                .withIconImage(iconId)
                .withIconSize(1.0f)
                // The icon is anchored at its centre by default; shift
                // it up so the pin (which sits at the top of our 90x64
                // bitmap) lands on the actual coordinate, with the
                // distance label appearing below.
                .withIconOffset(arrayOf(0f, -16f))
                .withData(com.google.gson.JsonPrimitive(m.id.toString()))
        )
    }
}

/**
 * Inline OSM raster tile style.  Same data, same servers, same $0 cost.
 *
 * Future upgrade path (no code change here):
 *   * Vector tiles via MapTiler free tier (`https://api.maptiler.com/maps/streets/style.json?key=...`)
 *   * Versatiles community vector tiles (`https://tiles.versatiles.org/styles/colorful.json`)
 *   * Self-hosted PMTiles via Cloudflare R2
 *
 * OSM tile usage policy requires our app's User-Agent identifies us;
 * MapLibre uses the package name + version automatically.
 */
private const val OSM_RASTER_STYLE = """
{
  "version": 8,
  "name": "OSM Raster",
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors",
      "maxzoom": 19
    }
  },
  "layers": [
    {"id": "osm", "type": "raster", "source": "osm"}
  ]
}
"""
