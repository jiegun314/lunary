package com.lunary.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.lunary.app.data.local.LunaryDatabase
import com.lunary.app.data.local.entity.ReminderEntity
import com.lunary.app.data.repository.toDomain
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.domain.date.DayMath
import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Recurrence
import com.lunary.app.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: LunaryDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() {
        db.reminderDao().getEnabled().forEach { schedule(it.id) }
    }

    suspend fun schedule(reminderId: String) {
        val reminder = db.reminderDao().getById(reminderId)
        if (reminder == null || !reminder.enabled) {
            cancel(reminderId)
            return
        }
        val eventRow = db.eventDao().getById(reminder.eventId) ?: run {
            cancel(reminderId)
            return
        }
        if (eventRow.event.deletedAt != null) {
            cancel(reminderId)
            return
        }
        val policy = settingsRepository.settings.first().leapDayPolicy
        val event = eventRow.toDomain()
        val trigger = nextTrigger(reminder, event.date, event.dateMode, event.recurrence, policy)
        if (trigger == null) {
            cancel(reminderId)
            return
        }
        val pending = pendingIntent(reminder.id)
        val triggerMillis = trigger.toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pending
                )
                return
            } catch (_: SecurityException) {
                // Fall through to the inexact alarm when the permission is revoked.
            }
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pending
        )
    }

    fun cancel(reminderId: String) {
        alarmManager.cancel(pendingIntent(reminderId))
    }

    fun nextTrigger(reminder: Reminder, event: Event, policy: LeapDayPolicy): Instant? =
        nextTrigger(
            reminder.minutesBefore,
            reminder.localTimeMinutes,
            event.date,
            event.dateMode,
            event.recurrence,
            policy
        )

    private fun nextTrigger(
        reminder: ReminderEntity,
        eventDate: LocalDate,
        dateMode: DateMode,
        recurrence: Recurrence,
        policy: LeapDayPolicy
    ): Instant? = nextTrigger(
        reminder.minutesBefore,
        reminder.localTimeMinutes,
        eventDate,
        dateMode,
        recurrence,
        policy
    )

    private fun nextTrigger(
        minutesBefore: Int,
        localTimeMinutes: Int,
        eventDate: LocalDate,
        dateMode: DateMode,
        recurrence: Recurrence,
        policy: LeapDayPolicy
    ): Instant? {
        val now = LocalDateTime.now(clock)
        if (minutesBefore < 0) {
            // Count-up milestone: measured from the event's original date, e.g. 50th day or 1 year.
            val milestone = eventDate.atStartOfDay()
                .plusMinutes(localTimeMinutes.toLong())
                .plusMinutes((-minutesBefore).toLong())
            return if (milestone.isAfter(now)) {
                milestone.atZone(ZoneId.systemDefault()).toInstant()
            } else {
                null
            }
        }
        var from = now.toLocalDate()
        repeat(3) {
            val occurrence = DayMath.nextOccurrence(
                date = eventDate,
                today = from,
                dateMode = dateMode,
                recurrence = recurrence,
                policy = policy
            )
            val trigger = occurrence.atStartOfDay()
                .plusMinutes(localTimeMinutes.toLong())
                .minusMinutes(minutesBefore.toLong())
            if (trigger.isAfter(now)) {
                return trigger.atZone(ZoneId.systemDefault()).toInstant()
            }
            if (dateMode == DateMode.MONTH_DAY || recurrence == Recurrence.YEARLY) {
                from = from.plusDays(1)
            } else {
                return null
            }
        }
        return null
    }

    private fun pendingIntent(reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            data = Uri.parse("lunary://reminder/$reminderId")
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
