package com.lunary.app.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunary.app.domain.model.EventLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventLocationJsonTest {
    @Test
    fun encodeDecodeKeepsAtMostTenLocations() {
        val locations = (1..12).map {
            EventLocation("地点$it", "地址$it", 31.0 + it, 121.0 + it)
        }

        val encoded = EventLocationJson.encode(locations)!!
        val decoded = EventLocationJson.decode(encoded)

        assertEquals(10, decoded.size)
        assertEquals("地点1", decoded.first().name)
        assertEquals("地点10", decoded.last().name)
    }

    @Test
    fun sanitizeDropsBlankEntriesAndIncompleteCoordinates() {
        val locations = listOf(
            EventLocation("", "", null, null),
            EventLocation("杭州", "", 30.2741, null),
            EventLocation("上海", "外滩", 31.2304, 121.4737)
        )

        val decoded = EventLocationJson.fromJsonArray(EventLocationJson.toJsonArray(locations))

        assertEquals(2, decoded.size)
        assertEquals("杭州", decoded[0].name)
        assertNull(decoded[0].latitude)
        assertNull(decoded[0].longitude)
        assertEquals(31.2304, decoded[1].latitude!!, 0.00001)
        assertEquals(121.4737, decoded[1].longitude!!, 0.00001)
    }

    @Test
    fun legacyLocationFallsBackToSingleItem() {
        val decoded = EventLocationJson.fromLegacy("上海", null, null)

        assertEquals(1, decoded.size)
        assertEquals("上海", decoded.single().name)
        assertNull(decoded.single().latitude)
    }
}
