package com.lunary.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lunary.app.ui.LunarDate
import com.lunary.app.ui.components.LetterAccent
import androidx.compose.ui.unit.sp
import com.lunary.app.ui.weekdayShort
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

fun monthGridDates(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val start = first.minusDays(offset.toLong())
    return List(42) { start.plusDays(it.toLong()) }
}

@Composable
fun WeekdayHeader(firstDayOfWeek: DayOfWeek, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val day = DayOfWeek.of(((firstDayOfWeek.value - 1 + i) % 7) + 1)
            Text(
                text = weekdayShort(day),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventColorsByDate: Map<LocalDate, List<Int>>,
    firstDayOfWeek: DayOfWeek,
    cellSize: Dp,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    letterDates: Set<LocalDate> = emptySet()
) {
    val dates = monthGridDates(month, firstDayOfWeek)
    val lunarLabels = androidx.compose.runtime.remember(dates) {
        dates.associateWith { LunarDate.short(it) }
    }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val rowHeight = cellSize * fontScale
    Column(modifier = modifier.fillMaxWidth()) {
        dates.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().height(rowHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month,
                        selected = date == selectedDate,
                        today = date == today,
                        eventColors = eventColorsByDate[date].orEmpty(),
                        hasLetter = date in letterDates,
                        lunarText = lunarLabels[date].orEmpty(),
                        cellSize = rowHeight,
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    eventColors: List<Int>,
    hasLetter: Boolean,
    lunarText: String,
    cellSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val textScale = fontScale.coerceAtMost(1.3f)
    val cellWidth = cellSize / fontScale
    val circleSize = minOf(40.dp * fontScale, cellWidth - 4.dp)
    val touchHeight = maxOf(48.dp, cellSize)
    val description = androidx.compose.ui.res.stringResource(
        when {
            hasLetter && eventColors.isNotEmpty() ->
                com.lunary.app.R.string.day_cell_description_event_letter
            hasLetter -> com.lunary.app.R.string.day_cell_description_letter
            else -> com.lunary.app.R.string.day_cell_description
        },
        date.year,
        date.monthValue,
        date.dayOfMonth,
        eventColors.size
    )
    val contentColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        inMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Box(
        modifier = modifier
            .requiredHeight(touchHeight)
            .semantics {
                contentDescription = description
                this.selected = selected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.size(circleSize)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .then(
                        if (today && !selected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            )
                        } else Modifier
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = contentColor,
                    fontSize = (16f * textScale / fontScale).sp,
                    lineHeight = (18f * textScale / fontScale).sp,
                    fontWeight = if (selected || today) FontWeight.SemiBold else FontWeight.Normal
                )
                if (lunarText.isNotEmpty()) {
                    Text(
                        text = lunarText,
                        color = contentColor.copy(alpha = 0.78f),
                        fontSize = ((if (cellWidth < 44.dp) 7f else 9f) * textScale / fontScale).sp,
                        lineHeight = ((if (cellWidth < 44.dp) 8f else 10f) * textScale / fontScale).sp,
                        maxLines = 1
                    )
                }
                if (hasLetter) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                when {
                                    selected -> MaterialTheme.colorScheme.onPrimary
                                    !inMonth -> LetterAccent.copy(alpha = 0.45f)
                                    else -> LetterAccent
                                }
                            )
                    )
                }
            }
        }
        if (eventColors.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val dotSize = if (eventColors.size > 5) 4.dp else 5.dp
                eventColors.take(7).forEach { argb ->
                    val dotColor = if (argb != 0) Color(argb) else MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}
