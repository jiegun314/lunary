package com.lunary.app.domain.date

import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayMathTest {

    @Test
    fun feb29UsesPolicyInNonLeapYear() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            DayMath.monthDayInYear(2, 29, 2025, LeapDayPolicy.FEBRUARY_28)
        )
        assertEquals(
            LocalDate.of(2025, 3, 1),
            DayMath.monthDayInYear(2, 29, 2025, LeapDayPolicy.MARCH_1)
        )
        assertEquals(
            LocalDate.of(2024, 2, 29),
            DayMath.monthDayInYear(2, 29, 2024, LeapDayPolicy.FEBRUARY_28)
        )
    }

    @Test
    fun nextOccurrenceOfMonthDayStaysInCurrentYearOrRolls() {
        val date = LocalDate.of(2000, 12, 25)
        assertEquals(
            LocalDate.of(2026, 12, 25),
            DayMath.nextOccurrence(
                date, LocalDate.of(2026, 11, 13),
                DateMode.MONTH_DAY, Recurrence.YEARLY, LeapDayPolicy.FEBRUARY_28
            )
        )
        assertEquals(
            LocalDate.of(2027, 1, 1),
            DayMath.nextOccurrence(
                LocalDate.of(2000, 1, 1), LocalDate.of(2026, 11, 13),
                DateMode.MONTH_DAY, Recurrence.YEARLY, LeapDayPolicy.FEBRUARY_28
            )
        )
    }

    @Test
    fun occursOnMatchesMonthDayForYearlyEvent() {
        val event = Event(
            id = "e1",
            title = "生日",
            date = LocalDate.of(1990, 5, 1),
            dateMode = DateMode.MONTH_DAY,
            recurrence = Recurrence.YEARLY,
            colorArgb = 0
        )
        assertTrue(DayMath.occursOn(event, LocalDate.of(2026, 5, 1)))
        assertFalse(DayMath.occursOn(event, LocalDate.of(2026, 5, 2)))
    }

    @Test
    fun fullDateWithoutRecurrenceOnlyMatchesExactDate() {
        val event = Event(
            id = "e2",
            title = "一次性",
            date = LocalDate.of(2026, 11, 13),
            dateMode = DateMode.FULL_DATE,
            recurrence = Recurrence.NONE,
            colorArgb = 0
        )
        assertTrue(DayMath.occursOn(event, LocalDate.of(2026, 11, 13)))
        assertFalse(DayMath.occursOn(event, LocalDate.of(2027, 11, 13)))
    }

    @Test
    fun daysBetweenIsSigned() {
        val today = LocalDate.of(2026, 11, 13)
        assertEquals(0L, DayMath.daysBetween(today, today))
        assertEquals(18L, DayMath.daysBetween(today, LocalDate.of(2026, 12, 1)))
        assertEquals(-1L, DayMath.daysBetween(today, LocalDate.of(2026, 11, 12)))
    }

    @Test
    fun yearsSinceHandlesUpcomingBirthday() {
        val born = LocalDate.of(1990, 5, 1)
        assertEquals(36, DayMath.yearsSince(born, LocalDate.of(2026, 11, 13)))
        assertEquals(35, DayMath.yearsSince(born, LocalDate.of(2026, 4, 30)))
    }
}
