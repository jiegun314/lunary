package com.lunary.app.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lunary.app.data.local.entity.AttachmentEntity
import com.lunary.app.data.local.entity.CategoryEntity
import com.lunary.app.data.local.entity.EventEntity
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.data.local.entity.ReminderEntity
import com.lunary.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LunaryDatabaseTest {

    private lateinit var db: LunaryDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, LunaryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun softDeleteHidesEventAndRestoreBringsItBack() = runBlocking {
        seedEventWithTag()
        assertEquals(1, db.eventDao().observeActiveEvents().first().size)
        assertEquals(1, db.eventDao().getById(EVENT_ID)?.tags?.size)

        db.eventDao().markDeleted(EVENT_ID, deletedAt = 10L, updatedAt = 10L)
        assertTrue(db.eventDao().observeActiveEvents().first().isEmpty())
        assertEquals(1, db.eventDao().observeDeletedEvents().first().size)

        db.eventDao().restore(EVENT_ID, updatedAt = 11L)
        assertEquals(1, db.eventDao().observeActiveEvents().first().size)
        assertEquals(1, db.eventDao().getAllForBackup().size)
    }

    @Test
    fun permanentDeleteCascadesTagsRemindersAndAttachments() = runBlocking {
        seedEventWithTag()
        db.reminderDao().upsert(
            ReminderEntity(
                id = "r1", eventId = EVENT_ID, createdAt = 1L, updatedAt = 1L
            )
        )
        db.attachmentDao().upsert(
            AttachmentEntity(
                id = "a1", eventId = EVENT_ID, displayName = "photo.jpg",
                mimeType = "image/jpeg", sizeBytes = 3, createdAt = 1L, updatedAt = 1L
            )
        )

        db.eventDao().deletePermanently(EVENT_ID)

        assertNull(db.eventDao().getById(EVENT_ID))
        assertTrue(db.eventTagDao().getAllForBackup().isEmpty())
        assertTrue(db.reminderDao().getAllForBackup().isEmpty())
        assertTrue(db.attachmentDao().getAllForBackup().isEmpty())
    }

    @Test
    fun clearingCategoryLeavesEventUncategorised() = runBlocking {
        seedEventWithTag()
        db.eventDao().clearCategory(CATEGORY_ID, updatedAt = 20L)
        assertEquals(0, db.eventDao().countActiveInCategory(CATEGORY_ID))
        assertNull(db.eventDao().getById(EVENT_ID)?.event?.categoryId)
    }

    private suspend fun seedEventWithTag() {
        db.categoryDao().upsert(
            CategoryEntity(
                id = CATEGORY_ID, name = "纪念日", colorArgb = 0xFFFF0000.toInt(),
                createdAt = 1L, updatedAt = 1L
            )
        )
        db.tagDao().upsert(
            TagEntity(id = TAG_ID, name = "旅行", createdAt = 1L, updatedAt = 1L)
        )
        db.eventDao().upsert(
            EventEntity(
                id = EVENT_ID, title = "Test event", dateEpochDay = 20_000L,
                categoryId = CATEGORY_ID, colorArgb = 0xFF123456.toInt(),
                createdAt = 1L, updatedAt = 1L
            )
        )
        db.eventTagDao().insertAll(listOf(EventTagCrossRef(EVENT_ID, TAG_ID)))
    }

    private companion object {
        const val EVENT_ID = "event-1"
        const val CATEGORY_ID = "category-1"
        const val TAG_ID = "tag-1"
    }
}
