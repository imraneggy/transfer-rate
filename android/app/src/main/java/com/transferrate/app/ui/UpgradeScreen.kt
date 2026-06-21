package com.transferrate.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transferrate.app.R
import com.transferrate.app.ui.theme.LocalBrandColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    isPro: Boolean,
    priceString: String?,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Transfer Rate Pro", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroCard()
            Spacer(Modifier.height(24.dp))
            FeatureRow(
                emoji = "🎯",
                title = "Up to 3 rate-target alerts",
                subtitle = "Free plan: 1 alert. Pro: set targets for INR, AED and more simultaneously.",
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                emoji = "🔔",
                title = "Priority daily-high alerts",
                subtitle = "Get notified the moment any provider beats today's best — no missed spikes.",
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                emoji = "📊",
                title = "Full 30-day history",
                subtitle = "Browse every provider's historical rates for any of the supported corridors.",
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                emoji = "❤️",
                title = "Support independent development",
                subtitle = "Transfer Rate is ad-free and open source. Pro keeps it that way.",
            )
            Spacer(Modifier.height(28.dp))
            if (isPro) {
                ProActiveCard()
            } else {
                UpgradeButton(priceString = priceString, onUpgrade = onUpgrade)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cancel anytime in Google Play subscriptions. No refunds for partial months.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCard() {
    val brand = LocalBrandColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(brand.navy, Color(0xFF0D2B40)),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "✦",
                fontSize = 40.sp,
                color = brand.gold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Transfer Rate Pro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "More alerts. Deeper insights. Always ad-free.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UpgradeButton(priceString: String?, onUpgrade: () -> Unit) {
    val brand = LocalBrandColors.current
    val label = if (priceString != null) {
        "Upgrade to Pro — $priceString / month"
    } else {
        "Upgrade to Pro"
    }
    Button(
        onClick = onUpgrade,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = brand.teal),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun ProActiveCard() {
    val brand = LocalBrandColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = brand.teal.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("✦", fontSize = 20.sp, color = brand.teal)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Pro active",
                    fontWeight = FontWeight.SemiBold,
                    color = brand.teal,
                )
                Text(
                    text = "All Pro features are unlocked on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
