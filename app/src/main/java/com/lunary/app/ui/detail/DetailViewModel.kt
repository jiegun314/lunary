package com.lunary.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.attachment.AttachmentStore
import com.lunary.app.data.reminder.ReminderScheduler
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Reminder
import com.lunary.app.domain.repository.AttachmentRepository
import com.lunary.app.domain.repository.CategoryRepository
import com.lunary.app.domain.repository.EventRepository
import com.lunary.app.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val event: Event? = null,
    val category: Category? = null,
    val policy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28,
    val reminders: List<Reminder> = emptyList(),
    val activeReminderIds: Set<String> = emptySet(),
    val attachments: List<Attachment> = emptyList(),
    val loaded: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    private val reminderRepository: ReminderRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentStore: AttachmentStore,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val eventId: String = checkNotNull(savedStateHandle["eventId"])

    val uiState: StateFlow<DetailUiState> = combine(
        eventRepository.observeEvent(eventId),
        categoryRepository.observeCategories(),
        settingsRepository.settings,
        reminderRepository.observeForEvent(eventId),
        attachmentRepository.observeForEvent(eventId)
    ) { event, categories, settings, reminders, attachments ->
        val activeReminderIds = if (event != null) {
            reminders.asSequence()
                .filter {
                    it.enabled &&
                        reminderScheduler.nextTrigger(it, event, settings.leapDayPolicy) != null
                }
                .map { it.id }
                .toSet()
        } else {
            emptySet()
        }
        DetailUiState(
            event = event,
            category = event?.categoryId?.let { id -> categories.firstOrNull { it.id == id } },
            policy = settings.leapDayPolicy,
            reminders = reminders,
            activeReminderIds = activeReminderIds,
            attachments = attachments,
            loaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState()
    )

    fun addReminder(minutesBefore: Int, localTimeMinutes: Int = DEFAULT_REMINDER_TIME) {
        val now = Instant.ofEpochMilli(clock.millis())
        viewModelScope.launch {
            reminderRepository.save(
                Reminder(
                    id = UUID.randomUUID().toString(),
                    eventId = eventId,
                    minutesBefore = minutesBefore,
                    localTimeMinutes = localTimeMinutes,
                    enabled = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun updateReminder(reminder: Reminder, minutesBefore: Int, localTimeMinutes: Int) {
        viewModelScope.launch {
            reminderRepository.save(
                reminder.copy(
                    minutesBefore = minutesBefore,
                    localTimeMinutes = localTimeMinutes,
                    updatedAt = Instant.now()
                )
            )
        }
    }

    fun setReminderEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepository.save(reminder.copy(enabled = enabled, updatedAt = Instant.now()))
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch { reminderRepository.delete(id) }
    }

    fun fileFor(attachment: Attachment) = attachmentStore.fileFor(attachment)

    fun contentUri(attachment: Attachment) = attachmentStore.contentUri(attachment)

    companion object {
        const val DEFAULT_REMINDER_TIME = 9 * 60
    }
}
