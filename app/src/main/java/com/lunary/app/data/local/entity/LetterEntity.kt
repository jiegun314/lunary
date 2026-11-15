package com.lunary.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recorded email between two people.
 *
 * [dateEpochDay] is a local calendar date and [timeMinutes] is minutes from
 * local midnight, so a letter can be shown on the calendar without a timezone
 * conversion.
 */
@Entity(
    tableName = "letters",
    indices = [
        Index("dateEpochDay"),
        Index("deletedAt"),
        Index("updatedAt")
    ]
)
data class LetterEntity(
    @PrimaryKey val id: String,
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val sender: String,
    val recipient: String,
    val subject: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val revision: Long = 1
)
