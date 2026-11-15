package com.lunary.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lunary.app.R
import com.lunary.app.domain.date.DayMath
import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate

private val weekdayShortNames = listOf("一", "二", "三", "四", "五", "六", "日")
private val weekdayLongNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

fun weekdayShort(day: DayOfWeek): String = weekdayShortNames[day.value - 1]

fun weekdayLong(day: DayOfWeek): String = weekdayLongNames[day.value - 1]

fun formatMonthTitle(year: Int, month: Int): String = "$year 年 $month 月"

fun formatDate(date: LocalDate): String = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

fun formatMonthDay(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

fun formatSelectedDate(date: LocalDate): String =
    "${date.monthValue}月${date.dayOfMonth}日 · ${weekdayLong(date.dayOfWeek)}"

/** Interval / age label shown under a title. */
@Composable
fun intervalLabel(event: Event, policy: LeapDayPolicy, today: LocalDate = LocalDate.now()): String {
    if (event.dateMode == DateMode.FULL_DATE && event.recurrence == Recurrence.NONE) {
        val days = DayMath.daysBetween(today, event.date)
        return when {
            days > 0 -> stringResource(R.string.interval_future, days)
            days == 0L -> stringResource(R.string.interval_today)
            else -> stringResource(R.string.interval_past, -days)
        }
    }
    val next = DayMath.nextOccurrence(
        date = event.date,
        today = today,
        dateMode = event.dateMode,
        recurrence = event.recurrence,
        policy = policy
    )
    val days = DayMath.daysBetween(today, next)
    if (days == 0L && today.year > event.date.year) {
        val years = DayMath.yearsSince(event.date, today)
        if (years > 0) return stringResource(R.string.anniversary_years, years)
    }
    return when {
        days > 0 -> stringResource(R.string.interval_future, days)
        days == 0L -> stringResource(R.string.interval_today)
        else -> stringResource(R.string.interval_past, -days)
    }
}
