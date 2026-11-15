package com.lunary.app.ui.events

import org.junit.Assert.assertEquals
import org.junit.Test

class EventsFilterTest {

    @Test
    fun activeCountIncludesQuickAndAdvancedFilters() {
        assertEquals(0, EventsFilter().activeCount)
        assertEquals(1, EventsFilter(quick = EventFilter.UPCOMING).activeCount)
        assertEquals(
            4,
            EventsFilter(
                quick = EventFilter.PAST,
                categoryId = "category",
                tagIds = setOf("tag"),
                location = LocationFilter.HAS
            ).activeCount
        )
    }
}
