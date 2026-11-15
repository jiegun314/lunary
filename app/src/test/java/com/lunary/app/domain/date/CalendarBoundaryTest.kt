package com.lunary.app.domain.date

import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import com.lunary.app.ui.calendar.monthGridDates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarBoundaryTest {

    @Test
    fun weekStartHonoursFirstDayOfWeek() {
        val friday = LocalDate.of(2026, 11, 13)
        assertEquals(LocalDate.of(2026, 11, 9), DayMath.weekStart(friday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 8), DayMath.weekStart(friday, DayOfWeek.SUNDAY))
    }

    @Test
    fun weekStartAcrossYearBoundary() {
        val newYear = LocalDate.of(2027, 1, 1)
        assertEquals(LocalDate.of(2026, 12, 28), DayMath.weekStart(newYear, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 12, 27), DayMath.weekStart(newYear, DayOfWeek.SUNDAY))
    }

    @Test
    fun monthGridIsAlwaysSixWeeksAndCoversMonth() {
        val month = YearMonth.of(2026, 11)
        val monday = monthGridDates(month, DayOfWeek.MONDAY)
        val sunday = monthGridDates(month, DayOfWeek.SUNDAY)
        assertEquals(42, monday.size)
        assertEquals(42, sunday.size)
        assertEquals(LocalDate.of(2026, 10, 26), monday.first())
        assertEquals(LocalDate.of(2026, 12, 6), monday.last())
        assertEquals(LocalDate.of(2026, 11, 1), sunday.first())
        assertEquals(LocalDate.of(2026, 12, 12), sunday.last())
        assertTrue(monday.contains(LocalDate.of(2026, 11, 30)))
        assertFalse(monday.contains(LocalDate.of(2026, 12, 7)))
    }

    @Test
    fun dayOffsetsAcrossYearBoundary() {
        assertEquals(1L, DayMath.daysBetween(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)))
        assertEquals(-1L, DayMath.dayOffset(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun yearlyOccurrenceRollsIntoNextYear() {
        val eventDate = LocalDate.of(2020, 12, 31)
        val next = DayMath.nextOccurrence(
            date = eventDate,
            today = LocalDate.of(2026, 12, 31),
            dateMode = DateMode.FULL_DATE,
            recurrence = Recurrence.YEARLY
        )
        assertEquals(LocalDate.of(2026, 12, 31), next)
        val after = DayMath.nextOccurrence(
            date = eventDate,
            today = LocalDate.of(2027, 1, 1),
            dateMode = DateMode.FULL_DATE,
            recurrence = Recurrence.YEARLY
        )
        assertEquals(LocalDate.of(2027, 12, 31), after)
    }

    @Test
    fun leapDayBoundaryPolicies() {
        val leapDate = LocalDate.of(2024, 2, 29)
        val next28 = DayMath.nextOccurrence(
            date = leapDate,
            today = LocalDate.of(2026, 1, 1),
            dateMode = DateMode.MONTH_DAY,
            recurrence = Recurrence.YEARLY,
            policy = LeapDayPolicy.FEBRUARY_28
        )
        val next1 = DayMath.nextOccurrence(
            date = leapDate,
            today = LocalDate.of(2026, 1, 1),
            dateMode = DateMode.MONTH_DAY,
            recurrence = Recurrence.YEARLY,
            policy = LeapDayPolicy.MARCH_1
        )
        assertEquals(LocalDate.of(2026, 2, 28), next28)
        assertEquals(LocalDate.of(2026, 3, 1), next1)
    }

    @Test
    fun yearsSinceBirthdayBoundary() {
        val birthday = LocalDate.of(2000, 11, 13)
        assertEquals(25, DayMath.yearsSince(birthday, LocalDate.of(2026, 11, 12)))
        assertEquals(26, DayMath.yearsSince(birthday, LocalDate.of(2026, 11, 13)))
    }
}
