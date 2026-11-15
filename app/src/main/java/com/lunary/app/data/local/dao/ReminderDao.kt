package com.lunary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lunary.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE eventId = :eventId ORDER BY minutesBefore DESC")
    fun observeForEvent(eventId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE eventId = :eventId ORDER BY minutesBefore DESC")
    suspend fun getForEvent(eventId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: String)

    @Query("UPDATE reminders SET lastTriggeredAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun markTriggered(id: String, timestamp: Long)

    @Query("SELECT * FROM reminders")
    suspend fun getAllForBackup(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
