package com.lunary.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local reminder. [minutesBefore] is relative to the event date's 00:00 local
 * time; [localTimeMinutes] is the preferred time of day for all-day events.
 * Actual scheduling is implemented in Stage 3.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("enabled")]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val minutesBefore: Int = 0,
    val localTimeMinutes: Int = 9 * 60,
    val enabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Attachment metadata. The bytes live either in the app's private files dir
 * ([storageKind] = INTERNAL, [relativePath]) or behind a persisted SAF/Photo
 * Picker URI ([storageKind] = SAF, [persistedUri]).
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("deletedAt")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storageKind: String = "INTERNAL",
    val relativePath: String? = null,
    val persistedUri: String? = null,
    val checksum: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val revision: Long = 1
)
