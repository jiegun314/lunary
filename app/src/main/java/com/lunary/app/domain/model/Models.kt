package com.lunary.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class Category(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val deletedAt: Instant? = null
)

data class Tag(
    val id: String,
    val name: String,
    val emoji: String? = null,
    val colorArgb: Int? = null,
    val sortOrder: Int = 0,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val deletedAt: Instant? = null
)

data class Event(
    val id: String,
    val title: String,
    val emoji: String? = null,
    val date: LocalDate,
    val dateMode: DateMode = DateMode.FULL_DATE,
    val recurrence: Recurrence = Recurrence.NONE,
    val categoryId: String? = null,
    val colorArgb: Int,
    val note: String = "",
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val deletedAt: Instant? = null
) {
    val tagIds: Set<String> get() = tags.mapTo(mutableSetOf()) { it.id }
}

data class Letter(
    val id: String,
    val date: LocalDate,
    val timeMinutes: Int,
    val sender: String,
    val recipient: String,
    val subject: String,
    val content: String,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val deletedAt: Instant? = null
) {
    val time: LocalTime get() = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
}

data class PickedLocation(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

data class Reminder(
    val id: String,
    val eventId: String,
    val minutesBefore: Int = 0,
    val localTimeMinutes: Int = 9 * 60,
    val enabled: Boolean = true,
    val lastTriggeredAt: Instant? = null,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH
)

data class Attachment(
    val id: String,
    val eventId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storageKind: String,
    val relativePath: String? = null,
    val persistedUri: String? = null,
    val checksum: String? = null,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val deletedAt: Instant? = null
)
