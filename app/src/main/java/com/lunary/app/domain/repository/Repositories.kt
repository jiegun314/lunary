package com.lunary.app.domain.repository

import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.Letter
import com.lunary.app.domain.model.Reminder
import com.lunary.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    fun observeEvent(id: String): Flow<Event?>
    fun observeActiveCount(): Flow<Int>
    fun observeTrashedEvents(): Flow<List<Event>>
    fun observeTrashCount(): Flow<Int>

    /** Creates or updates [event] together with its tag links. */
    suspend fun save(event: Event)

    suspend fun softDelete(id: String)
    suspend fun restore(id: String)
    suspend fun deletePermanently(id: String)

    /** Permanently removes trash entries older than [retentionDays]. */
    suspend fun purgeExpiredTrash(retentionDays: Int = 30)
    suspend fun emptyTrash()
}

interface LetterRepository {
    fun observeLetters(): Flow<List<Letter>>
    fun observeLetter(id: String): Flow<Letter?>
    fun observeTrashedLetters(): Flow<List<Letter>>
    fun observeTrashCount(): Flow<Int>

    suspend fun save(letter: Letter)
    suspend fun softDelete(id: String)
    suspend fun restore(id: String)
    suspend fun deletePermanently(id: String)
    suspend fun purgeExpiredTrash(retentionDays: Int = 30)
    suspend fun emptyTrash()
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun save(category: Category)
    suspend fun delete(id: String)
    suspend fun reorder(ids: List<String>)
}

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun save(tag: Tag)
    suspend fun delete(id: String)
    suspend fun reorder(ids: List<String>)
}

interface ReminderRepository {
    fun observeForEvent(eventId: String): Flow<List<Reminder>>
    fun observeAll(): Flow<List<Reminder>>
    suspend fun save(reminder: Reminder)
    suspend fun delete(id: String)
    suspend fun rescheduleAll()
}

interface AttachmentRepository {
    fun observeForEvent(eventId: String): Flow<List<Attachment>>
    suspend fun add(eventId: String, uri: android.net.Uri): Attachment?
    suspend fun delete(attachment: Attachment)
    suspend fun deleteForEvent(eventId: String)
}
