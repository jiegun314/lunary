package com.lunary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags WHERE deletedAt IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeActive(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TagEntity?

    @Upsert
    suspend fun upsert(tag: TagEntity)

    @Query(
        "UPDATE tags SET deletedAt = :deletedAt, updatedAt = :updatedAt, " +
            "revision = revision + 1 WHERE id = :id"
    )
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM tags WHERE deletedAt IS NULL")
    suspend fun maxSortOrder(): Int

    @Query("UPDATE tags SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Long)

    @Query("SELECT * FROM tags ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllForBackup(): List<TagEntity>

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}

@Dao
interface EventTagDao {

    @Query(
        "SELECT tags.* FROM tags INNER JOIN event_tags ON tags.id = event_tags.tagId " +
            "WHERE event_tags.eventId = :eventId AND tags.deletedAt IS NULL ORDER BY tags.name"
    )
    fun observeForEvent(eventId: String): Flow<List<TagEntity>>

    @Query(
        "SELECT tags.* FROM tags INNER JOIN event_tags ON tags.id = event_tags.tagId " +
            "WHERE event_tags.eventId = :eventId AND tags.deletedAt IS NULL ORDER BY tags.name"
    )
    suspend fun forEvent(eventId: String): List<TagEntity>

    @Upsert
    suspend fun insertAll(refs: List<EventTagCrossRef>)

    @Query("DELETE FROM event_tags WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: String)

    @Query("DELETE FROM event_tags WHERE tagId = :tagId")
    suspend fun deleteForTag(tagId: String)

    @Query("SELECT COUNT(*) FROM event_tags WHERE tagId = :tagId")
    suspend fun countForTag(tagId: String): Int

    @Query("SELECT * FROM event_tags")
    suspend fun getAllForBackup(): List<EventTagCrossRef>

    @Query("DELETE FROM event_tags")
    suspend fun deleteAll()
}
