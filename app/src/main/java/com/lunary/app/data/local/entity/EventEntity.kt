package com.lunary.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A single life event / anniversary.
 *
 * [dateEpochDay] is always a local calendar date; for month-day-only events
 * ([dateMode] = "MONTH_DAY") the stored year is the year the event was created
 * and the UI ignores it.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dateEpochDay"),
        Index("deletedAt"),
        Index("categoryId"),
        Index("updatedAt")
    ]
)
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val emoji: String? = null,
    val dateEpochDay: Long,
    val dateMode: String = "FULL_DATE",
    val recurrenceRule: String = "NONE",
    val categoryId: String? = null,
    val colorArgb: Int,
    val note: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val revision: Long = 1,
    val locationsJson: String? = null
)

data class EventWithTags(
    @Embedded val event: EventEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EventTagCrossRef::class,
            parentColumn = "eventId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)
