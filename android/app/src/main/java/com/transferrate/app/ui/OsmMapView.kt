package com.transferrate.app.ui

import android.content.Context
import android.preference.PreferenceManager
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * One-time osmdroid configuration.  Must be called before any MapView
 * is inflated; idempotent so calling multiple times is harmless.
 *
 * The User-Agent is required by the OSM tile policy
 * (https://operations.osmfoundation.org/policies/tiles/) - bulk
 * downloaders without a UA are throttled or banned outright.
 */
fun configureOsmdroidOnce(ctx: Context) {
    val cfg = Configuration.getInstance()
    cfg.load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
    cfg.userAgentValue = "Exchangia/1.0 (https://github.com/imraneggy/transfer-rate)"
    cfg.osmdroidBasePath = ctx.cacheDir
    cfg.osmdroidTileCache = ctx.cacheDir
}

/**
 * Compose wrapper for an osmdroid MapView.
 *
 * AndroidView interop with explicit lifecycle handling - osmdroid's
 * MapView holds tile-loader threads that must be paused/resumed
 * around the surrounding lifecycle to avoid crashes on background
 * transitions.
 *
 * @param centerLat   latitude to centre the map at
 * @param centerLon   longitude to centre the map at
 * @param zoom        OSM zoom level (3 = world, 15 = neighbourhood, 19 = building)
 * @param mosques     markers to draw on the map (cleared and re-added on each invocation)
 * @param onMarkerTap callback when the user taps a mosque marker
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

    // Initialise osmdroid configuration once - cheap if already loaded.
    LaunchedEffect(Unit) { configureOsmdroidOnce(ctx) }

    val mapView = remember {
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(centerLat, centerLon))
        }
    }

    // Pause/resume the MapView with the host lifecycle to avoid leaking
    // tile-loader threads when the screen is backgrounded.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Re-centre when caller changes the centre coordinates (e.g. user
    // location resolved or list-tap).
    LaunchedEffect(centerLat, centerLon, zoom) {
        mapView.controller.setZoom(zoom)
        mapView.controller.animateTo(GeoPoint(centerLat, centerLon))
    }

    // Refresh markers when the mosque list changes.
    LaunchedEffect(mosques) {
        mapView.overlays.clear()
        mosques.forEach { m ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(m.lat, m.lon)
                title = m.name
                snippet = m.address ?: m.denomination ?: ""
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onMarkerTap(m)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier,
        )
    }
}
