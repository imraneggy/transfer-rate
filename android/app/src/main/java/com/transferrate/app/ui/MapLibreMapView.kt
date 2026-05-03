package com.transferrate.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Paint
import android.os.Bundle
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
 * @param mosques     markers to draw; cleared and re-added on each invocation
 * @param onMarkerTap fired when the user taps a mosque marker
 * @param modifier    standard Compose modifier
 */
@Composable
fun OsmMapView(
    centerLat: Double,
    centerLon: Double,
    zoom: Double = 14.0,
    mosques: List<Mosque> = emptyList(),
    onMarkerTap: (Mosque) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MapLibre.getInstance() must be called BEFORE any MapView constructor.
    // It is idempotent so calling on every recomposition is safe.
    // Use applicationContext to avoid leaking the Activity through the
    // singleton's internal storage path.
    LaunchedEffect(Unit) { MapLibre.getInstance(ctx.applicationContext) }

    // SymbolManager is created once after the style loads; we keep a
    // mutable reference so subsequent recompositions can clear/re-add
    // markers without rebuilding the manager (which would re-bind GL
    // texture atlas and flicker).
    val symbolManagerRef = remember { arrayOf<SymbolManager?>(null) }
    val mapViewRef       = remember { arrayOf<MapView?>(null) }

    val mapView = remember {
        MapLibre.getInstance(ctx.applicationContext)
        MapView(ctx).apply {
            mapViewRef[0] = this
            // MapView itself does not have an onCreate observer hook;
            // we have to drive its full lifecycle from our DisposableEffect.
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(centerLat, centerLon))
                    .zoom(zoom)
                    .build()
                map.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE)) { style ->
                    // Register the marker icon ONCE per style load.
                    style.addImage(MARKER_ICON_ID, makeMarkerBitmap())
                    val sm = SymbolManager(this, map, style).apply {
                        iconAllowOverlap = true
                        iconIgnorePlacement = true
                        // Tap handler — find the corresponding Mosque
                        // by symbol data field we set when adding it.
                        addClickListener { sym ->
                            val payloadId = sym.data?.asString?.toLongOrNull()
                            val match = mosqueIndex[payloadId]
                            if (match != null) onMarkerTap(match)
                            true
                        }
                    }
                    symbolManagerRef[0] = sm
                    syncMarkers(sm, mosques)
                }
            }
        }
    }

    // Lifecycle bridge: MapLibre's MapView demands every callback in the
    // canonical Activity sequence; missing onDestroy in particular leaks
    // ~30MB of GL textures.
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
        }
    }

    // Re-centre when caller changes coordinates (location resolved, list-tap).
    LaunchedEffect(centerLat, centerLon, zoom) {
        mapViewRef[0]?.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(centerLat, centerLon))
                .zoom(zoom)
                .build()
        }
    }

    // Refresh markers when list changes (after style loaded).
    LaunchedEffect(mosques) {
        symbolManagerRef[0]?.let { syncMarkers(it, mosques) }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView })
    }
}

/** Build a small green dot bitmap to use as the mosque marker. */
private fun makeMarkerBitmap(): Bitmap {
    val sizePx = 36
    val bm = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bm)
    val outer = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = AColor.WHITE
    }
    val inner = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        // Brand teal #00B49E - matches Exchangia primary
        color = AColor.rgb(0, 180, 158)
    }
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    c.drawCircle(cx, cy, sizePx / 2f - 2f, outer)
    c.drawCircle(cx, cy, sizePx / 2f - 6f, inner)
    return bm
}

/**
 * Track a snapshot of currently-displayed mosques by id so the symbol
 * tap handler can look them up.  Keyed on OSM element id (Long).
 *
 * Updated synchronously inside syncMarkers so it always matches what
 * SymbolManager has on the map - no stale references during async
 * style reloads.
 */
private val mosqueIndex = HashMap<Long, Mosque>()

private const val MARKER_ICON_ID = "mosque-marker"

private fun syncMarkers(sm: SymbolManager, mosques: List<Mosque>) {
    sm.deleteAll()
    mosqueIndex.clear()
    mosques.forEach { m ->
        mosqueIndex[m.id] = m
        sm.create(
            SymbolOptions()
                .withLatLng(LatLng(m.lat, m.lon))
                .withIconImage(MARKER_ICON_ID)
                .withIconSize(1.0f)
                .withData(com.google.gson.JsonPrimitive(m.id.toString()))
        )
    }
}

/**
 * Inline OSM raster tile style.  Equivalent to the previous osmdroid
 * MAPNIK source - same data, same servers, same zero-cost.
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
