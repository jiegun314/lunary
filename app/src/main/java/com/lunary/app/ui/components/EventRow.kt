package com.lunary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunary.app.R
import com.lunary.app.domain.date.eventDayMetric
import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.ui.formatDate
import com.lunary.app.ui.formatMonthDay
import java.time.LocalDate

@Composable
fun EventRow(
    event: Event,
    categoryName: String?,
    policy: LeapDayPolicy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    hasActiveReminder: Boolean = false
) {
    val metric = eventDayMetric(event, policy, today)
    val dateText = if (event.dateMode == DateMode.MONTH_DAY) {
        "每年 ${formatMonthDay(event.date)}"
    } else {
        formatDate(event.date)
    }
    val metaText = listOfNotNull(
        dateText,
        categoryName?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = eventCardContainerColor(metric.isPast)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(event.colorArgb))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    event.emoji?.let { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 20.sp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasActiveReminder) {
                Icon(
                    painter = painterResource(R.drawable.ic_alarm),
                    contentDescription = stringResource(R.string.reminder_active_indicator),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            EventDayMetricBlock(
                metric = metric,
                compact = true,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
