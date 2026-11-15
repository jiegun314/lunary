package com.lunary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lunary.app.data.local.entity.EventWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Transaction
    @Query(
        "SELECT * FROM events WHERE deletedAt IS NULL " +
            "ORDER BY dateEpochDay ASC, title COLLATE NOCASE ASC"
    )
    fun observeActiveEvents(): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<EventWithTags?>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventWithTags?

    @Upsert
    suspend fun upsert(event: com.lunary.app.data.local.entity.EventEntity)

    @Query(
        "UPDATE events SET deletedAt = :deletedAt, updatedAt = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id"
    )
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        "UPDATE events SET deletedAt = NULL, updatedAt = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id"
    )
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("SELECT COUNT(*) FROM events WHERE categoryId = :categoryId AND deletedAt IS NULL")
    suspend fun countActiveInCategory(categoryId: String): Int

    @Query(
        "UPDATE events SET categoryId = NULL, updatedAt = :updatedAt, " +
            "revision = revision + 1 WHERE categoryId = :categoryId"
    )
    suspend fun clearCategory(categoryId: String, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM events WHERE deletedAt IS NULL")
    fun observeActiveCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM events WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedEvents(): Flow<List<EventWithTags>>

    @Query("SELECT COUNT(*) FROM events WHERE deletedAt IS NOT NULL")
    fun observeDeletedCount(): Flow<Int>

    @Query("SELECT id FROM events WHERE deletedAt IS NOT NULL")
    suspend fun deletedEventIds(): List<String>

    @Query("SELECT id FROM events WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun deletedEventIdsBefore(before: Long): List<String>

    @Query("DELETE FROM events WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedBefore(before: Long): Int

    @Query("DELETE FROM events WHERE deletedAt IS NOT NULL")
    suspend fun clearTrash(): Int

    @Transaction
    @Query("SELECT * FROM events")
    suspend fun getAllForBackup(): List<EventWithTags>

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
