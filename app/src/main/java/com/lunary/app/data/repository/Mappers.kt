package com.lunary.app.data.repository

import com.lunary.app.data.local.entity.AttachmentEntity
import com.lunary.app.data.local.entity.CategoryEntity
import com.lunary.app.data.local.entity.EventEntity
import com.lunary.app.data.local.entity.EventWithTags
import com.lunary.app.data.local.entity.LetterEntity
import com.lunary.app.data.local.entity.ReminderEntity
import com.lunary.app.data.local.entity.TagEntity
import com.lunary.app.data.local.EventLocationJson
import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.EmojiCatalog
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.Letter
import com.lunary.app.domain.model.Recurrence
import com.lunary.app.domain.model.Reminder
import com.lunary.app.domain.model.Tag
import java.time.Instant
import java.time.LocalDate

fun EventWithTags.toDomain(): Event {
    val storedEmoji = event.emoji?.takeIf { it.isNotBlank() }
    val legacyEmoji = if (storedEmoji == null) EmojiCatalog.leading(event.title) else null
    val title = legacyEmoji?.let { event.title.removePrefix(it).trimStart() } ?: event.title
    val locations = EventLocationJson.decode(event.locationsJson)
        .ifEmpty { EventLocationJson.fromLegacy(event.location, event.latitude, event.longitude) }
    val primaryLocation = locations.firstOrNull()
    return Event(
        id = event.id,
        title = title,
        emoji = storedEmoji ?: legacyEmoji,
        date = LocalDate.ofEpochDay(event.dateEpochDay),
        dateMode = runCatching { DateMode.valueOf(event.dateMode) }.getOrDefault(DateMode.FULL_DATE),
        recurrence = runCatching { Recurrence.valueOf(event.recurrenceRule) }
            .getOrDefault(Recurrence.NONE),
        categoryId = event.categoryId,
        colorArgb = event.colorArgb,
        note = event.note.orEmpty(),
        location = primaryLocation?.label ?: event.location.orEmpty(),
        latitude = primaryLocation?.latitude ?: event.latitude,
        longitude = primaryLocation?.longitude ?: event.longitude,
        locations = locations,
        tags = tags.map { it.toDomain() },
        createdAt = Instant.ofEpochMilli(event.createdAt),
        updatedAt = Instant.ofEpochMilli(event.updatedAt),
        deletedAt = event.deletedAt?.let(Instant::ofEpochMilli)
    )
}

fun Event.toEntity(): EventEntity {
    val normalizedLocations = EventLocationJson.normalize(locations)
        .ifEmpty { EventLocationJson.fromLegacy(location, latitude, longitude) }
    val primaryLocation = normalizedLocations.firstOrNull()
    return EventEntity(
        id = id,
        title = title,
        emoji = emoji,
        dateEpochDay = date.toEpochDay(),
        dateMode = dateMode.name,
        recurrenceRule = recurrence.name,
        categoryId = categoryId,
        colorArgb = colorArgb,
        note = note.ifBlank { null },
        location = (primaryLocation?.label ?: location).ifBlank { null },
        latitude = primaryLocation?.latitude ?: latitude,
        longitude = primaryLocation?.longitude ?: longitude,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        deletedAt = deletedAt?.toEpochMilli(),
        locationsJson = EventLocationJson.encode(normalizedLocations)
    )
}

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli)
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    deletedAt = deletedAt?.toEpochMilli()
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    emoji = emoji,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli)
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    emoji = emoji,
    colorArgb = colorArgb,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    deletedAt = deletedAt?.toEpochMilli()
)

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    eventId = eventId,
    minutesBefore = minutesBefore,
    localTimeMinutes = localTimeMinutes,
    enabled = enabled,
    lastTriggeredAt = lastTriggeredAt?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    eventId = eventId,
    minutesBefore = minutesBefore,
    localTimeMinutes = localTimeMinutes,
    enabled = enabled,
    lastTriggeredAt = lastTriggeredAt?.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

fun AttachmentEntity.toDomain(): Attachment = Attachment(
    id = id,
    eventId = eventId,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    storageKind = storageKind,
    relativePath = relativePath,
    persistedUri = persistedUri,
    checksum = checksum,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli)
)

fun Attachment.toEntity(): AttachmentEntity = AttachmentEntity(
    id = id,
    eventId = eventId,
    displayName = displayName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    storageKind = storageKind,
    relativePath = relativePath,
    persistedUri = persistedUri,
    checksum = checksum,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    deletedAt = deletedAt?.toEpochMilli()
)

fun LetterEntity.toDomain(): Letter = Letter(
    id = id,
    date = LocalDate.ofEpochDay(dateEpochDay),
    timeMinutes = timeMinutes,
    sender = sender,
    recipient = recipient,
    subject = subject,
    content = content,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli)
)

fun Letter.toEntity(revision: Long = 1): LetterEntity = LetterEntity(
    id = id,
    dateEpochDay = date.toEpochDay(),
    timeMinutes = timeMinutes,
    sender = sender,
    recipient = recipient,
    subject = subject,
    content = content,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    deletedAt = deletedAt?.toEpochMilli(),
    revision = revision
)
