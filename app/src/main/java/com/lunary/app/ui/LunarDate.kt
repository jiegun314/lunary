package com.lunary.app.ui

import android.icu.util.ChineseCalendar
import java.time.LocalDate
import java.time.ZoneId

object LunarDate {
    private val months = arrayOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
    private val days = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    private fun fields(date: LocalDate): Triple<Int, Int, Boolean> {
        val calendar = ChineseCalendar()
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        calendar.timeInMillis = millis
        val month = calendar.get(ChineseCalendar.MONTH) + 1
        val day = calendar.get(ChineseCalendar.DAY_OF_MONTH)
        val leap = calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        return Triple(month, day, leap)
    }

    fun short(date: LocalDate): String {
        val (month, day, leap) = fields(date)
        return if (day == 1) (if (leap) "闰" else "") + months[month - 1] else days[day - 1]
    }

    fun full(date: LocalDate): String {
        val (month, day, leap) = fields(date)
        return "农历" + (if (leap) "闰" else "") + months[month - 1] + days[day - 1]
    }
}
