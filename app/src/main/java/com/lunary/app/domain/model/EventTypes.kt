package com.lunary.app.domain.model

enum class DateMode {
    /** A normal calendar date; the year matters. */
    FULL_DATE,

    /** Birthday / anniversary: the year is only a reference, the month-day repeats. */
    MONTH_DAY
}

enum class Recurrence {
    NONE,
    YEARLY
}

enum class LeapDayPolicy {
    /** Feb 29 events fall on Feb 28 in non-leap years. */
    FEBRUARY_28,

    /** Feb 29 events fall on Mar 1 in non-leap years. */
    MARCH_1
}
