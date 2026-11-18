package com.lunary.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lunary.app.R
import com.lunary.app.domain.date.EventDayMetric

private val FutureContainerLight = Color(0xFFE4EFFB)
private val PastContainerLight = Color(0xFFEDE6F4)
private val FutureContainerDark = Color(0xFF182836)
private val PastContainerDark = Color(0xFF2A2236)

private val FutureMetricLight = Color(0xFF24557D)
private val PastMetricLight = Color(0xFF5A3E76)
private val FutureMetricDark = Color(0xFFA8C9E8)
private val PastMetricDark = Color(0xFFCFB8EA)

@Composable
fun eventCardContainerColor(isPast: Boolean): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when {
        dark && isPast -> PastContainerDark
        dark -> FutureContainerDark
        isPast -> PastContainerLight
        else -> FutureContainerLight
    }
}

@Composable
fun eventMetricColor(isPast: Boolean): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when {
        dark && isPast -> PastMetricDark
        dark -> FutureMetricDark
        isPast -> PastMetricLight
        else -> FutureMetricLight
    }
}

/**
 * The distance-to-today block. Compact is used at the trailing edge of a list
 * row; the larger variant is used in the detail header.
 */
@Composable
fun EventDayMetricBlock(
    metric: EventDayMetric,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val numberStyle = if (compact) {
        MaterialTheme.typography.headlineSmall
    } else {
        MaterialTheme.typography.displaySmall
    }
    val numberColor = eventMetricColor(metric.isPast)
    val horizontalAlignment = if (compact) Alignment.End else Alignment.Start
    val textAlign = if (compact) TextAlign.End else TextAlign.Start

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (metric.isToday) {
            Text(
                text = stringResource(R.string.interval_today),
                style = if (compact) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                color = numberColor,
                textAlign = textAlign,
                maxLines = 2
            )
            metric.anniversaryYears?.let { years ->
                Text(
                    text = stringResource(R.string.anniversary_years, years),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = stringResource(
                    if (metric.isPast) R.string.metric_past else R.string.metric_future
                ),
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (metric.years > 0) {
                        metric.years.toString()
                    } else {
                        metric.days.toString()
                    },
                    style = numberStyle.copy(fontFeatureSettings = "tnum"),
                    color = numberColor,
                    textAlign = textAlign,
                    maxLines = 1
                )
                Text(
                    text = stringResource(
                        if (metric.years > 0) R.string.metric_year_unit else R.string.metric_day_unit
                    ),
                    style = if (compact) {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    color = numberColor.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = if (compact) 2.dp else 6.dp)
                )
            }
            if (metric.years > 0) {
                Text(
                    text = stringResource(R.string.metric_total_days, metric.days),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = textAlign,
                    maxLines = 1
                )
            }
        }
    }
}
