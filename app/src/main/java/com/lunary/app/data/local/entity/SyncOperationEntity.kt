package com.lunary.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Durable outbox for future cloud sync (Stage 5). Kept in the schema now so
 * enabling sync later does not require a destructive migration.
 */
@Entity(
    tableName = "sync_operations",
    indices = [Index("createdAt"), Index("entityType", "entityId")]
)
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String? = null,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long
)
