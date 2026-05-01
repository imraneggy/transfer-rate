package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transferrate.app.R
import com.transferrate.app.data.ProviderQuote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatesScreen(vm: RatesViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringRes(R.string.title_aed_inr)) },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        // A small unicode glyph keeps us free of icon dependency bloat.
                        Text("↻", fontSize = 22.sp)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is RatesUiState.Loading -> CenteredSpinner()
                is RatesUiState.Failed -> ErrorView(s.message) { vm.refresh() }
                is RatesUiState.Ready -> RatesList(s)
            }
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringRes(R.string.error_loading))
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(24.dp))
        IconButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun RatesList(state: RatesUiState.Ready) {
    val ctx = LocalContext.current
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, vertical = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Quote for AED %.0f".format(state.doc.amountBase),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Updated ${state.doc.completedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        items(state.doc.providers, key = { it.providerId }) { p ->
            ProviderCard(p, onClick = {
                p.url?.let { url ->
                    runCatching {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url),
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    }
                }
            })
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringRes(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ProviderCard(p: ProviderQuote, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(p.status)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.providerName, fontWeight = FontWeight.SemiBold)
                if (p.deliveryEstimate != null) {
                    Text(
                        p.deliveryEstimate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (p.status != "ok" && p.note != null) {
                    Text(
                        p.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            RateView(p)
        }
    }
}

@Composable
private fun RateView(p: ProviderQuote) {
    val rate = p.effectiveRate ?: p.rate
    Column(horizontalAlignment = Alignment.End) {
        when (p.status) {
            "ok" -> {
                Text(
                    text = if (rate != null) "%.4f".format(rate) else "—",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    "₹ per AED",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            "stale" -> {
                Text(
                    text = if (rate != null) "%.4f".format(rate) else "—",
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringRes(R.string.status_stale),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            "investigating" -> Text(
                stringRes(R.string.status_investigating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            else -> Text(
                stringRes(R.string.status_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun StatusDot(status: String) {
    val color = when (status) {
        "ok" -> Color(0xFF2E7D32)
        "stale" -> Color(0xFFF57C00)
        "investigating" -> Color(0xFF455A64)
        else -> Color(0xFFC62828)
    }
    Box(
        Modifier.size(10.dp).background(color, CircleShape),
    )
}

@Composable
private fun stringRes(id: Int) = androidx.compose.ui.res.stringResource(id)
