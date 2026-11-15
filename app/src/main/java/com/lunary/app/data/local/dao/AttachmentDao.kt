package com.lunary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lunary.app.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE eventId = :eventId AND deletedAt IS NULL ORDER BY createdAt")
    fun observeForEvent(eventId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE eventId = :eventId AND deletedAt IS NULL ORDER BY createdAt")
    suspend fun getForEvent(eventId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: String): AttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM attachments WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: String)

    @Query("SELECT * FROM attachments")
    suspend fun getAllForBackup(): List<AttachmentEntity>

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()
}
