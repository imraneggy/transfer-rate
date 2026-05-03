package com.transferrate.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transferrate.app.data.formatDistance
import kotlinx.coroutines.launch

/**
 * Mosque Finder screen.
 *
 * Layout (top -> bottom):
 *   1. TopAppBar with back button and title
 *   2. Map taking ~55% of remaining height
 *   3. Bottom panel: status / actions / list of mosques sorted by distance
 *
 * State machine:
 *   Idle              - first entry, prompts user to find their location
 *   LocationNeeded    - permission denied; show CTA + rationale
 *   Loading           - location resolved, fetching from Overpass
 *   Ready             - map populated, list scrollable
 *   Failed            - error message + retry
 *
 * Default centre when no fix available: Dubai (25.2048N, 55.2708E) -
 * the largest UAE remittance corridor we serve, so most users see
 * something familiar even before granting permission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueScreen(
    onBack: () -> Unit,
    vm: MosqueViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            scope.launch {
                val loc = LocationProvider.currentLocation(ctx)
                if (loc != null) vm.searchAt(loc.latitude, loc.longitude)
                else vm.searchAt(DEFAULT_LAT, DEFAULT_LON)  // fall back to Dubai
            }
        } else {
            vm.markLocationNeeded()
        }
    }

    fun requestLocation() {
        if (LocationProvider.hasPermission(ctx)) {
            scope.launch {
                val loc = LocationProvider.currentLocation(ctx)
                if (loc != null) vm.searchAt(loc.latitude, loc.longitude)
                else vm.searchAt(DEFAULT_LAT, DEFAULT_LON)
            }
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ))
        }
    }

    // On first composition, auto-request location if we already have permission.
    LaunchedEffect(Unit) {
        if (LocationProvider.hasPermission(ctx) && state is MosqueUiState.Idle) {
            requestLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Mosques nearby",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            "OpenStreetMap data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            "<",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            // Map - takes ~55% of the screen below the app bar.
            val (centerLat, centerLon, mosques) = when (val s = state) {
                is MosqueUiState.Ready -> Triple(s.userLat, s.userLon, s.mosques.map { it.mosque })
                else -> Triple(DEFAULT_LAT, DEFAULT_LON, emptyList())
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
                    .weight(0.55f),
            ) {
                OsmMapView(
                    centerLat = centerLat,
                    centerLon = centerLon,
                    zoom = if (state is MosqueUiState.Ready) 14.5 else 11.0,
                    mosques = mosques,
                    onMarkerTap = { /* future: scroll list to this mosque */ },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bottom panel - status / list.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                when (val s = state) {
                    MosqueUiState.Idle -> EmptyPanel(
                        title = "Find mosques near you",
                        subtitle = "We'll search OpenStreetMap for the closest mosques " +
                                   "within a ${vm.radiusMeters / 1000} km radius.",
                        ctaLabel = "Use my location",
                        onCta = { requestLocation() },
                    )
                    MosqueUiState.LocationNeeded -> EmptyPanel(
                        title = "Location permission needed",
                        subtitle = "Grant location access to find mosques near you, " +
                                   "or skip and view a sample area (Dubai Marina).",
                        ctaLabel = "Try again",
                        onCta = { requestLocation() },
                        secondaryLabel = "Show Dubai sample",
                        onSecondary = { vm.searchAt(DEFAULT_LAT, DEFAULT_LON) },
                    )
                    MosqueUiState.Loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Querying OpenStreetMap...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is MosqueUiState.Failed -> EmptyPanel(
                        title = "Couldn't load mosques",
                        subtitle = s.message,
                        ctaLabel = "Retry",
                        onCta = { requestLocation() },
                    )
                    is MosqueUiState.Ready -> MosqueList(
                        state = s,
                        onMosqueTap = { m ->
                            // Open in the user's preferred maps app for directions.
                            val uri = Uri.parse(
                                "geo:${m.mosque.lat},${m.mosque.lon}" +
                                "?q=${m.mosque.lat},${m.mosque.lon}" +
                                "(${Uri.encode(m.mosque.name)})"
                            )
                            runCatching {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, uri)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPanel(
    title: String,
    subtitle: String,
    ctaLabel: String,
    onCta: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onCta) { Text(ctaLabel) }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.TextButton(onClick = onSecondary) {
                Text(secondaryLabel)
            }
        }
    }
}

@Composable
private fun MosqueList(
    state: MosqueUiState.Ready,
    onMosqueTap: (com.transferrate.app.ui.MosqueWithDistance) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${state.mosques.size} mosques within ${state.radiusMeters / 1000} km",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        if (state.mosques.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No mosques found nearby. Try increasing the search radius.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.mosques, key = { it.mosque.id }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMosqueTap(item) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.mosque.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false,
                            )
                            val secondary = item.mosque.address
                                ?: item.mosque.denomination?.replaceFirstChar { it.uppercase() }
                                ?: "Tap to open in Maps"
                            Text(
                                secondary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            formatDistance(item.distanceMeters),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// Default map centre when no location available - Dubai Marina.
// Picked because (1) largest concentration of UAE remittance users
// who'd land on this app, (2) dense mosque coverage in OSM, (3)
// instantly recognisable to most of our user base.
private const val DEFAULT_LAT = 25.0772
private const val DEFAULT_LON = 55.1390
