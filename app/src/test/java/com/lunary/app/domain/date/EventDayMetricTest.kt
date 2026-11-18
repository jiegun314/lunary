package com.lunary.app.domain.date

import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDayMetricTest {

    private fun event(
        date: LocalDate,
        dateMode: DateMode = DateMode.FULL_DATE,
        recurrence: Recurrence = Recurrence.NONE
    ) = Event(
        id = "event",
        title = "Test",
        date = date,
        dateMode = dateMode,
        recurrence = recurrence,
        colorArgb = 0xFF6750A4.toInt()
    )

    @Test
    fun futureFixedDateCountsDaysForward() {
        val metric = eventDayMetric(
            event(LocalDate.of(2026, 11, 20)),
            LeapDayPolicy.FEBRUARY_28,
            LocalDate.of(2026, 11, 18)
        )

        assertEquals(2L, metric.days)
        assertFalse(metric.isPast)
        assertFalse(metric.isToday)
        assertEquals(0, metric.years)
        assertNull(metric.anniversaryYears)
    }

    @Test
    fun pastFixedDateIsMarkedAsPast() {
        val metric = eventDayMetric(
            event(LocalDate.of(2024, 1, 1)),
            LeapDayPolicy.FEBRUARY_28,
            LocalDate.of(2026, 11, 18)
        )

        assertTrue(metric.isPast)
        assertFalse(metric.isToday)
        assertEquals(2, metric.years)
        assertTrue(metric.days > 365)
    }

    @Test
    fun recurringEventUsesNextOccurrenceAndAnniversaryYear() {
        val today = LocalDate.of(2026, 11, 18)
        val metric = eventDayMetric(
            event(
                LocalDate.of(2010, 11, 18),
                dateMode = DateMode.MONTH_DAY,
                recurrence = Recurrence.YEARLY
            ),
            LeapDayPolicy.FEBRUARY_28,
            today
        )

        assertEquals(0L, metric.days)
        assertTrue(metric.isToday)
        assertFalse(metric.isPast)
        assertEquals(16, metric.anniversaryYears)
    }

    @Test
    fun futureRecurringEventCountsToNextOccurrence() {
        val metric = eventDayMetric(
            event(
                LocalDate.of(2010, 11, 20),
                dateMode = DateMode.MONTH_DAY,
                recurrence = Recurrence.YEARLY
            ),
            LeapDayPolicy.FEBRUARY_28,
            LocalDate.of(2026, 11, 18)
        )

        assertEquals(2L, metric.days)
        assertFalse(metric.isPast)
        assertNull(metric.anniversaryYears)
    }

    @Test
    fun futureDateOverOneYearUsesWholeYears() {
        val metric = eventDayMetric(
            event(LocalDate.of(2028, 1, 1)),
            LeapDayPolicy.FEBRUARY_28,
            LocalDate.of(2026, 11, 18)
        )

        assertFalse(metric.isPast)
        assertEquals(1, metric.years)
        assertTrue(metric.days > 365)
    }
}
