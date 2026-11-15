package com.lunary.app.data.repository

import androidx.room.withTransaction
import com.lunary.app.data.local.LunaryDatabase
import com.lunary.app.data.attachment.AttachmentStore
import com.lunary.app.data.local.dao.CategoryDao
import com.lunary.app.data.local.dao.EventDao
import com.lunary.app.data.local.dao.EventTagDao
import com.lunary.app.data.local.dao.TagDao
import com.lunary.app.data.reminder.ReminderScheduler
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.Tag
import com.lunary.app.domain.repository.CategoryRepository
import com.lunary.app.domain.repository.EventRepository
import com.lunary.app.domain.repository.TagRepository
import com.lunary.app.domain.text.TextLimits
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val db: LunaryDatabase,
    private val eventDao: EventDao,
    private val eventTagDao: EventTagDao,
    private val attachmentStore: AttachmentStore,
    private val scheduler: ReminderScheduler,
    private val clock: Clock
) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> =
        eventDao.observeActiveEvents().map { rows -> rows.map { it.toDomain() } }

    override fun observeEvent(id: String): Flow<Event?> =
        eventDao.observeById(id).map { it?.toDomain() }

    override fun observeActiveCount(): Flow<Int> = eventDao.observeActiveCount()

    override fun observeTrashedEvents(): Flow<List<Event>> =
        eventDao.observeDeletedEvents().map { rows -> rows.map { it.toDomain() } }

    override fun observeTrashCount(): Flow<Int> = eventDao.observeDeletedCount()

    override suspend fun save(event: Event) {
        require(event.title.isNotBlank()) { "title must not be blank" }
        val now = clock.millis()
        db.withTransaction {
            val existing = eventDao.getById(event.id)
            val entity = event.copy(
                title = TextLimits.truncateGraphemes(event.title.trim(), MAX_TITLE_GRAPHEMES),
                createdAt = existing?.event?.createdAt?.let(Instant::ofEpochMilli)
                    ?: Instant.ofEpochMilli(now),
                updatedAt = Instant.ofEpochMilli(now)
            ).toEntity()
            eventDao.upsert(entity)
            eventTagDao.deleteForEvent(event.id)
            if (event.tags.isNotEmpty()) {
                eventTagDao.insertAll(
                    event.tags.map { EventTagCrossRef(eventId = event.id, tagId = it.id) }
                )
            }
        }
    }

    override suspend fun softDelete(id: String) {
        val now = clock.millis()
        eventDao.markDeleted(id = id, deletedAt = now, updatedAt = now)
        db.reminderDao().getForEvent(id).forEach { scheduler.cancel(it.id) }
    }

    override suspend fun restore(id: String) {
        eventDao.restore(id = id, updatedAt = clock.millis())
        db.reminderDao().getForEvent(id).forEach { scheduler.schedule(it.id) }
    }

    override suspend fun deletePermanently(id: String) {
        purgeEvent(id)
    }

    override suspend fun purgeExpiredTrash(retentionDays: Int) {
        val cutoff = clock.millis() - retentionDays * 24L * 60L * 60L * 1000L
        eventDao.deletedEventIdsBefore(cutoff).forEach { purgeEvent(it) }
        eventDao.purgeDeletedBefore(cutoff)
    }

    override suspend fun emptyTrash() {
        eventDao.deletedEventIds().forEach { purgeEvent(it) }
        eventDao.clearTrash()
    }

    private suspend fun purgeEvent(id: String) {
        db.reminderDao().getForEvent(id).forEach { scheduler.cancel(it.id) }
        attachmentStore.deleteEventFiles(id)
        db.attachmentDao().deleteForEvent(id)
        eventDao.deletePermanently(id)
    }

    companion object {
        const val MAX_TITLE_GRAPHEMES = 120
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val db: LunaryDatabase,
    private val categoryDao: CategoryDao,
    private val clock: Clock
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(category: Category) {
        require(category.name.isNotBlank())
        val now = clock.millis()
        val existing = categoryDao.getById(category.id)
        categoryDao.upsert(
            category.copy(
                name = TextLimits.truncateGraphemes(category.name.trim(), MAX_NAME_GRAPHEMES),
                createdAt = existing?.createdAt?.let(Instant::ofEpochMilli)
                    ?: Instant.ofEpochMilli(now),
                updatedAt = Instant.ofEpochMilli(now)
            ).toEntity()
        )
    }

    override suspend fun delete(id: String) {
        val now = clock.millis()
        db.withTransaction {
            categoryDao.markDeleted(id = id, deletedAt = now, updatedAt = now)
            eventDao.clearCategory(id, now)
        }
    }

    override suspend fun reorder(ids: List<String>) {
        val now = clock.millis()
        db.withTransaction {
            ids.forEachIndexed { index, id ->
                categoryDao.updateSortOrder(id = id, sortOrder = index, updatedAt = now)
            }
        }
    }

    private val eventDao: EventDao
        get() = db.eventDao()

    companion object {
        const val MAX_NAME_GRAPHEMES = 20
    }
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val db: LunaryDatabase,
    private val tagDao: TagDao,
    private val eventTagDao: EventTagDao,
    private val clock: Clock
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(tag: Tag) {
        require(tag.name.isNotBlank())
        val now = clock.millis()
        val existing = tagDao.getById(tag.id)
        tagDao.upsert(
            tag.copy(
                name = TextLimits.truncateGraphemes(tag.name.trim(), MAX_NAME_GRAPHEMES),
                createdAt = existing?.createdAt?.let(Instant::ofEpochMilli)
                    ?: Instant.ofEpochMilli(now),
                updatedAt = Instant.ofEpochMilli(now)
            ).toEntity()
        )
    }

    override suspend fun delete(id: String) {
        val now = clock.millis()
        db.withTransaction {
            tagDao.markDeleted(id = id, deletedAt = now, updatedAt = now)
            eventTagDao.deleteForTag(id)
        }
    }

    override suspend fun reorder(ids: List<String>) {
        val now = clock.millis()
        db.withTransaction {
            ids.forEachIndexed { index, id ->
                tagDao.updateSortOrder(id = id, sortOrder = index, updatedAt = now)
            }
        }
    }

    companion object {
        const val MAX_NAME_GRAPHEMES = 20
    }
}
