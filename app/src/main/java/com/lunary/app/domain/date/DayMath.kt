package com.lunary.app.domain.date

import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit

/**
 * Pure calendar-date helpers. All computations use [LocalDate] and natural
 * calendar days; no UTC timestamps or 24-hour rolling is involved.
 */
object DayMath {

    fun daysBetween(from: LocalDate, to: LocalDate): Long =
        ChronoUnit.DAYS.between(from, to)

    /** First day of the week containing [date], honouring [firstDayOfWeek]. */
    fun weekStart(date: LocalDate, firstDayOfWeek: java.time.DayOfWeek): LocalDate {
        val shift = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return date.minusDays(shift.toLong())
    }

    /** Signed day offset relative to the user's local "today". */
    fun dayOffset(date: LocalDate, today: LocalDate): Long = daysBetween(today, date)

    /**
     * Resolves the occurrence of a month/day pair in [year].
     *
     * Feb 29 is mapped according to [policy] when [year] is not a leap year.
     */
    fun monthDayInYear(
        month: Int,
        day: Int,
        year: Int,
        policy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28
    ): LocalDate {
        return try {
            LocalDate.of(year, month, day)
        } catch (e: DateTimeException) {
            if (month == 2 && day == 29 && !isLeapYear(year)) {
                when (policy) {
                    LeapDayPolicy.FEBRUARY_28 -> LocalDate.of(year, 2, 28)
                    LeapDayPolicy.MARCH_1 -> LocalDate.of(year, 3, 1)
                }
            } else {
                throw e
            }
        }
    }

    fun isLeapYear(year: Int): Boolean = java.time.Year.isLeap(year.toLong())

    /**
     * The next occurrence on/after [today] for an anniversary-style event.
     * For [DateMode.FULL_DATE] + [Recurrence.NONE] the exact date is returned.
     */
    fun nextOccurrence(
        date: LocalDate,
        today: LocalDate,
        dateMode: DateMode,
        recurrence: Recurrence,
        policy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28
    ): LocalDate {
        if (dateMode == DateMode.FULL_DATE && recurrence == Recurrence.NONE) return date
        if (dateMode == DateMode.FULL_DATE && recurrence == Recurrence.YEARLY) {
            if (!date.isBefore(today)) return date
            return monthDayInYear(date.monthValue, date.dayOfMonth, today.year, policy)
                .let { if (it.isBefore(today)) it.plusYears(1) else it }
        }
        // MONTH_DAY (with or without yearly recurrence): the year is ignored.
        val thisYear = monthDayInYear(date.monthValue, date.dayOfMonth, today.year, policy)
        return if (thisYear.isBefore(today)) {
            monthDayInYear(date.monthValue, date.dayOfMonth, today.year + 1, policy)
        } else {
            thisYear
        }
    }

    /** Age / years since the original date, or null when the year is unknown. */
    fun yearsSince(original: LocalDate, today: LocalDate): Int =
        if (today.monthValue < original.monthValue ||
            (today.monthValue == original.monthValue && today.dayOfMonth < original.dayOfMonth)
        ) {
            today.year - original.year - 1
        } else {
            today.year - original.year
        }

    /**
     * Whether [event] (with its date mode / recurrence) falls on [date].
     * Feb 29 anniversaries respect [policy] in non-leap years.
     */
    fun occursOn(
        event: com.lunary.app.domain.model.Event,
        date: LocalDate,
        policy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28
    ): Boolean {
        val original = event.date
        if (event.dateMode == DateMode.FULL_DATE && event.recurrence == Recurrence.NONE) {
            return original == date
        }
        if (date.isBefore(original)) return false
        return monthDayInYear(original.monthValue, original.dayOfMonth, date.year, policy) == date
    }

    fun formatMonthDay(month: Int, day: Int): String = "%d月%d日".format(month, day)

    fun monthName(month: Month): String = "${month.value}月"
}
