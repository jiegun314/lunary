package com.lunary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lunary.app.data.local.entity.LetterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {

    @Query(
        "SELECT * FROM letters WHERE deletedAt IS NULL " +
            "ORDER BY dateEpochDay DESC, timeMinutes DESC, createdAt DESC"
    )
    fun observeActive(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<LetterEntity?>

    @Query("SELECT * FROM letters WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LetterEntity?

    @Upsert
    suspend fun upsert(letter: LetterEntity)

    @Query(
        "UPDATE letters SET deletedAt = :deletedAt, updatedAt = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id"
    )
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        "UPDATE letters SET deletedAt = NULL, updatedAt = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id"
    )
    suspend fun restore(id: String, updatedAt: Long)

    @Query("DELETE FROM letters WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("SELECT * FROM letters WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedLetters(): Flow<List<LetterEntity>>

    @Query("SELECT COUNT(*) FROM letters WHERE deletedAt IS NOT NULL")
    fun observeDeletedCount(): Flow<Int>

    @Query("SELECT id FROM letters WHERE deletedAt IS NOT NULL")
    suspend fun deletedLetterIds(): List<String>

    @Query("SELECT id FROM letters WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun deletedLetterIdsBefore(before: Long): List<String>

    @Query("DELETE FROM letters WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedBefore(before: Long): Int

    @Query("DELETE FROM letters WHERE deletedAt IS NOT NULL")
    suspend fun clearTrash(): Int

    @Query("SELECT * FROM letters ORDER BY dateEpochDay, timeMinutes, createdAt")
    suspend fun getAllForBackup(): List<LetterEntity>

    @Query("DELETE FROM letters")
    suspend fun deleteAll()
}
