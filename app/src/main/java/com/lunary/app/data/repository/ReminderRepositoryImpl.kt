package com.lunary.app.data.repository

import com.lunary.app.data.local.dao.ReminderDao
import com.lunary.app.data.reminder.ReminderScheduler
import com.lunary.app.domain.model.Reminder
import com.lunary.app.domain.repository.ReminderRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val scheduler: ReminderScheduler,
    private val clock: Clock
) : ReminderRepository {

    override fun observeForEvent(eventId: String): Flow<List<Reminder>> =
        reminderDao.observeForEvent(eventId).map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Reminder>> =
        reminderDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(reminder: Reminder) {
        val now = clock.millis()
        val existing = reminderDao.getById(reminder.id)
        reminderDao.upsert(
            reminder.copy(
                createdAt = existing?.createdAt?.let(Instant::ofEpochMilli)
                    ?: Instant.ofEpochMilli(now),
                updatedAt = Instant.ofEpochMilli(now)
            ).toEntity()
        )
        scheduler.schedule(reminder.id)
    }

    override suspend fun delete(id: String) {
        scheduler.cancel(id)
        reminderDao.delete(id)
    }

    override suspend fun rescheduleAll() = scheduler.rescheduleAll()
}
