package com.lunary.app.ui.letters

import com.lunary.app.domain.model.Letter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LettersFilterTest {

    private val letters = listOf(
        letter(
            id = "1",
            date = LocalDate.of(2026, 11, 15),
            sender = "Alice",
            recipient = "Bob",
            subject = "Autumn Letter",
            content = "Hello Bob"
        ),
        letter(
            id = "2",
            date = LocalDate.of(2026, 10, 1),
            sender = "Carol",
            recipient = "Alice",
            subject = "Summer Note",
            content = "A summer note"
        ),
        letter(
            id = "3",
            date = LocalDate.of(2025, 12, 31),
            sender = "Bob",
            recipient = "Carol",
            subject = "Old Postcard",
            content = "An old postcard"
        )
    )

    @Test
    fun queryMatchesSubjectContentSenderAndRecipient() {
        assertEquals(listOf("1", "2"), ids(LettersFilter(query = "alice")))
        assertEquals(listOf("1"), ids(LettersFilter(query = "hello")))
        assertEquals(listOf("3"), ids(LettersFilter(query = "postcard")))
    }

    @Test
    fun senderAndRecipientAreContainsAndCaseInsensitive() {
        assertEquals(listOf("1"), ids(LettersFilter(sender = "ali")))
        assertEquals(listOf("2"), ids(LettersFilter(recipient = "ali")))
        assertEquals(listOf("3"), ids(LettersFilter(sender = "BOB")))
    }

    @Test
    fun dateRangeIsInclusiveAndOpenEnded() {
        assertEquals(
            listOf("1", "2"),
            ids(
                LettersFilter(
                    startDate = LocalDate.of(2026, 10, 1),
                    endDate = LocalDate.of(2026, 11, 15)
                )
            )
        )
        assertEquals(
            listOf("3"),
            ids(LettersFilter(endDate = LocalDate.of(2025, 12, 31)))
        )
        assertEquals(
            listOf("1", "2"),
            ids(LettersFilter(startDate = LocalDate.of(2026, 1, 1)))
        )
    }

    @Test
    fun allConditionsCombine() {
        val filter = LettersFilter(
            query = "letter",
            startDate = LocalDate.of(2026, 1, 1),
            sender = "Alice",
            recipient = "Bob"
        )
        assertEquals(listOf("1"), ids(filter))
        assertEquals(3, filter.advancedCount)
    }

    @Test
    fun emptyFilterMatchesEverythingAndHasNoAdvancedCount() {
        val filter = LettersFilter()
        assertTrue(letters.all(filter::matches))
        assertEquals(0, filter.advancedCount)
        assertFalse(filter.copy(sender = "nobody").matches(letters.first()))
    }

    private fun ids(filter: LettersFilter): List<String> =
        letters.filter(filter::matches).map { it.id }

    private fun letter(
        id: String,
        date: LocalDate,
        sender: String,
        recipient: String,
        subject: String,
        content: String
    ) = Letter(
        id = id,
        date = date,
        timeMinutes = 12 * 60,
        sender = sender,
        recipient = recipient,
        subject = subject,
        content = content
    )
}
