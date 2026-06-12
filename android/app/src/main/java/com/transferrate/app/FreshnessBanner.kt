package com.transferrate.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transferrate.app.R
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Home-screen freshness guard.
 *
 * The rate feed is useful only when users can immediately trust its age.
 * The normal "Updated X ago" text inside the header is enough for fresh data,
 * but it is too quiet when the GitHub Pages artifact or scraper workflow is
 * significantly delayed. This banner appears only when the loaded document is
 * older than the thresholds below, so the normal happy path stays uncluttered.
 */
@Composable
fun FreshnessBanner(
    completedAt: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember(completedAt) { freshnessState(completedAt) }
    if (state.level == FreshnessLevel.Fresh) return

    val colors = MaterialTheme.colorScheme
    val containerColor = when (state.level) {
        FreshnessLevel.Delayed -> colors.tertiaryContainer
        FreshnessLevel.Critical -> colors.errorContainer
        FreshnessLevel.Unknown -> colors.errorContainer
        FreshnessLevel.Fresh -> colors.surface
    }
    val contentColor = when (state.level) {
        FreshnessLevel.Delayed -> colors.onTertiaryContainer
        FreshnessLevel.Critical -> colors.onErrorContainer
        FreshnessLevel.Unknown -> colors.onErrorContainer
        FreshnessLevel.Fresh -> colors.onSurface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
                Text(
                    text = state.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.width(10.dp))
            TextButton(onClick = onRefresh) {
                Text(
                    text = stringResource(R.string.toolbar_refresh),
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
        }
    }
}

private enum class FreshnessLevel { Fresh, Delayed, Critical, Unknown }

private data class FreshnessUiState(
    val level: FreshnessLevel,
    val title: String,
    val body: String,
)

private fun freshnessState(completedAt: String): FreshnessUiState {
    val completed = try {
        Instant.parse(completedAt)
    } catch (_: DateTimeParseException) {
        return FreshnessUiState(
            level = FreshnessLevel.Unknown,
            title = "Rate age unknown",
            body = "The feed timestamp could not be parsed. Refresh before relying on these rates.",
        )
    }

    val age = Duration.between(completed, Instant.now())
    if (age.isNegative || age < DELAYED_AFTER) {
        return FreshnessUiState(
            level = FreshnessLevel.Fresh,
            title = "",
            body = "",
        )
    }

    val ageText = formatAge(age)
    return if (age >= CRITICAL_AFTER) {
        FreshnessUiState(
            level = FreshnessLevel.Critical,
            title = "Rates may be stale",
            body = "Last successful update was $ageText. Confirm in the provider app before sending money.",
        )
    } else {
        FreshnessUiState(
            level = FreshnessLevel.Delayed,
            title = "Rates are delayed",
            body = "Last successful update was $ageText. Tap refresh to check for a newer scrape.",
        )
    }
}

private fun formatAge(duration: Duration): String {
    val totalMinutes = duration.toMinutes().coerceAtLeast(0)
    return when {
        totalMinutes < 60 -> "$totalMinutes min ago"
        totalMinutes < 48 * 60 -> {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes == 0L) "$hours h ago" else "$hours h $minutes m ago"
        }
        else -> {
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            if (hours == 0L) "$days d ago" else "$days d $hours h ago"
        }
    }
}

private val DELAYED_AFTER: Duration = Duration.ofHours(3)
private val CRITICAL_AFTER: Duration = Duration.ofHours(24)
