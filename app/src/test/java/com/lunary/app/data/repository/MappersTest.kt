package com.lunary.app.data.repository

import com.lunary.app.data.local.entity.EventEntity
import com.lunary.app.data.local.entity.EventWithTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {
    private fun entity(title: String, emoji: String? = null) = EventEntity(
        id = "e1",
        title = title,
        emoji = emoji,
        dateEpochDay = 0L,
        colorArgb = 0,
        createdAt = 0L,
        updatedAt = 0L
    )

    @Test
    fun legacyLeadingEmojiBecomesLabel() {
        val event = EventWithTags(entity("😄Party"), emptyList()).toDomain()

        assertEquals("Party", event.title)
        assertEquals("😄", event.emoji)
    }

    @Test
    fun storedEmojiKeepsTitleUntouched() {
        val event = EventWithTags(entity("Party", "❤️"), emptyList()).toDomain()

        assertEquals("Party", event.title)
        assertEquals("❤️", event.emoji)
    }

    @Test
    fun plainTitleHasNoLabel() {
        val event = EventWithTags(entity("Party"), emptyList()).toDomain()

        assertEquals("Party", event.title)
        assertNull(event.emoji)
    }
}
