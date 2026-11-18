package com.lunary.app.domain.date

import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Distance between an event and today, normalised for display.
 *
 * Fixed-date events can be in the past; recurring and month/day events always
 * resolve to their next occurrence. [days] is always non-negative and is the
 * value shown to the user, while [years] is set for distances over a year.
 */
data class EventDayMetric(
    val days: Long,
    val isPast: Boolean,
    val isToday: Boolean,
    val years: Int,
    val anniversaryYears: Int?
)

fun eventDayMetric(
    event: Event,
    policy: LeapDayPolicy,
    today: LocalDate = LocalDate.now()
): EventDayMetric {
    val fixed = event.dateMode == DateMode.FULL_DATE && event.recurrence == Recurrence.NONE
    val target = if (fixed) {
        event.date
    } else {
        DayMath.nextOccurrence(
            date = event.date,
            today = today,
            dateMode = event.dateMode,
            recurrence = event.recurrence,
            policy = policy
        )
    }
    val signedDays = DayMath.daysBetween(today, target)
    val days = abs(signedDays)
    val isToday = days == 0L
    val isPast = signedDays < 0

    val years = if (days > 365) {
        val completeYears = if (isPast) {
            ChronoUnit.YEARS.between(event.date, today)
        } else {
            ChronoUnit.YEARS.between(today, target)
        }
        abs(completeYears).toInt().coerceAtLeast(1)
    } else {
        0
    }

    val anniversaryYears = if (isToday && today.year > event.date.year) {
        DayMath.yearsSince(event.date, today).takeIf { it > 0 }
    } else {
        null
    }

    return EventDayMetric(
        days = days,
        isPast = isPast,
        isToday = isToday,
        years = years,
        anniversaryYears = anniversaryYears
    )
}
