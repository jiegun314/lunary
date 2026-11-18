package com.lunary.app.ui

import java.time.DayOfWeek
import java.time.LocalDate

private val weekdayShortNames = listOf("一", "二", "三", "四", "五", "六", "日")
private val weekdayLongNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

fun weekdayShort(day: DayOfWeek): String = weekdayShortNames[day.value - 1]

fun weekdayLong(day: DayOfWeek): String = weekdayLongNames[day.value - 1]

fun formatMonthTitle(year: Int, month: Int): String = "$year 年 $month 月"

fun formatDate(date: LocalDate): String = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

fun formatMonthDay(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

fun formatTime(minutes: Int): String {
    val normalized = minutes.coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(normalized / 60, normalized % 60)
}

fun formatSelectedDate(date: LocalDate): String =
    "${date.monthValue}月${date.dayOfMonth}日 · ${weekdayLong(date.dayOfWeek)}"
