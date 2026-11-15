package com.lunary.app.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunary.app.data.local.entity.LetterEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupJsonLetterTest {

    private val letter = LetterEntity(
        id = "letter-1",
        dateEpochDay = 20_000L,
        timeMinutes = 9 * 60 + 30,
        sender = "Alice",
        recipient = "Bob",
        subject = "Hello",
        content = "Long time no see.",
        createdAt = 1L,
        updatedAt = 2L,
        revision = 3L
    )

    @Test
    fun snapshotRoundTripsLetters() {
        val snapshot = SnapshotData(
            events = emptyList(),
            categories = emptyList(),
            tags = emptyList(),
            eventTags = emptyList(),
            reminders = emptyList(),
            attachments = emptyList(),
            letters = listOf(letter)
        )

        val decoded = BackupJson.decodeSnapshot(BackupJson.encodeSnapshot(snapshot))

        assertEquals(listOf(letter), decoded.letters)
    }

    @Test
    fun legacySnapshotWithoutLettersDecodesToEmptyList() {
        val snapshot = SnapshotData(
            events = emptyList(),
            categories = emptyList(),
            tags = emptyList(),
            eventTags = emptyList(),
            reminders = emptyList(),
            attachments = emptyList()
        )
        val json = JSONObject(String(BackupJson.encodeSnapshot(snapshot), Charsets.UTF_8))
            .apply { remove("letters") }

        val decoded = BackupJson.decodeSnapshot(json.toString().toByteArray(Charsets.UTF_8))

        assertTrue(decoded.letters.isEmpty())
    }
}
