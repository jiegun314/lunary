package com.lunary.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.reminder.ReminderScheduler
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.domain.date.DayMath
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.model.Tag
import com.lunary.app.domain.repository.CategoryRepository
import com.lunary.app.domain.repository.EventRepository
import com.lunary.app.domain.repository.ReminderRepository
import com.lunary.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class EventFilter { ALL, UPCOMING, PAST }

enum class LocationFilter { ANY, HAS, NONE }

data class EventsFilter(
    val query: String = "",
    val quick: EventFilter = EventFilter.ALL,
    val categoryId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val location: LocationFilter = LocationFilter.ANY
) {
    val advancedCount: Int
        get() = (if (categoryId != null) 1 else 0) +
            tagIds.size +
            (if (location != LocationFilter.ANY) 1 else 0)

    val activeCount: Int
        get() = advancedCount + if (quick != EventFilter.ALL) 1 else 0
}

data class EventsUiState(
    val today: LocalDate,
    val filter: EventsFilter,
    val events: List<Event>,
    val categories: Map<String, Category>,
    val tags: List<Tag>,
    val leapDayPolicy: LeapDayPolicy,
    val eventsWithActiveReminders: Set<String> = emptySet()
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    eventRepository: EventRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    settingsRepository: SettingsRepository,
    reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock
) : ViewModel() {

    private val filter = MutableStateFlow(EventsFilter())

    private val baseState = combine(
        eventRepository.observeEvents(),
        categoryRepository.observeCategories(),
        tagRepository.observeTags(),
        settingsRepository.settings,
        filter
    ) { events, categories, tags, settings, currentFilter ->
        val today = LocalDate.now(clock)
        val policy = settings.leapDayPolicy
        val normalized = currentFilter.query.trim()
        val filtered = events
            .filter { event ->
                normalized.isEmpty() ||
                    event.title.contains(normalized, ignoreCase = true) ||
                    event.note.contains(normalized, ignoreCase = true) ||
                    event.location.contains(normalized, ignoreCase = true)
            }
            .filter { event -> matchesQuickFilter(event, today, policy, currentFilter.quick) }
            .filter { event ->
                currentFilter.categoryId == null ||
                    event.categoryId == currentFilter.categoryId
            }
            .filter { event ->
                currentFilter.tagIds.isEmpty() ||
                    event.tags.any { it.id in currentFilter.tagIds }
            }
            .filter { event ->
                when (currentFilter.location) {
                    LocationFilter.ANY -> true
                    LocationFilter.HAS -> event.location.isNotBlank()
                    LocationFilter.NONE -> event.location.isBlank()
                }
            }
            .sortedWith(eventComparator(today, policy))
        EventsUiState(
            today = today,
            filter = currentFilter,
            events = filtered,
            categories = categories.associateBy { it.id },
            tags = tags,
            leapDayPolicy = policy
        )
    }

    val uiState: StateFlow<EventsUiState> = combine(
        baseState,
        reminderRepository.observeAll()
    ) { state, reminders ->
        val eventsById = state.events.associateBy { it.id }
        val activeEventIds = reminders.asSequence()
            .filter { it.enabled }
            .mapNotNull { reminder ->
                eventsById[reminder.eventId]
                    ?.takeIf { event ->
                        reminderScheduler.nextTrigger(reminder, event, state.leapDayPolicy) != null
                    }
                    ?.id
            }
            .toSet()
        state.copy(eventsWithActiveReminders = activeEventIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(
            today = LocalDate.now(clock),
            filter = EventsFilter(),
            events = emptyList(),
            categories = emptyMap(),
            tags = emptyList(),
            leapDayPolicy = LeapDayPolicy.FEBRUARY_28,
            eventsWithActiveReminders = emptySet()
        )
    )

    fun setQuery(value: String) {
        filter.value = filter.value.copy(query = value)
    }

    fun setQuickFilter(value: EventFilter) {
        filter.value = filter.value.copy(quick = value)
    }

    fun setCategory(categoryId: String?) {
        filter.value = filter.value.copy(categoryId = categoryId)
    }

    fun toggleTag(tagId: String) {
        val current = filter.value.tagIds
        filter.value = filter.value.copy(
            tagIds = if (tagId in current) current - tagId else current + tagId
        )
    }

    fun setLocation(location: LocationFilter) {
        filter.value = filter.value.copy(location = location)
    }

    fun clearFilters() {
        filter.value = EventsFilter(query = filter.value.query)
    }

    private fun matchesQuickFilter(
        event: Event,
        today: LocalDate,
        policy: LeapDayPolicy,
        filter: EventFilter
    ): Boolean {
        val next = DayMath.nextOccurrence(event.date, today, event.dateMode, event.recurrence, policy)
        return when (filter) {
            EventFilter.ALL -> true
            EventFilter.UPCOMING -> !next.isBefore(today)
            EventFilter.PAST -> next.isBefore(today)
        }
    }

    private fun eventComparator(today: LocalDate, policy: LeapDayPolicy): Comparator<Event> =
        Comparator { a, b ->
            val nextA = DayMath.nextOccurrence(a.date, today, a.dateMode, a.recurrence, policy)
            val nextB = DayMath.nextOccurrence(b.date, today, b.dateMode, b.recurrence, policy)
            val aUpcoming = !nextA.isBefore(today)
            val bUpcoming = !nextB.isBefore(today)
            when {
                aUpcoming && bUpcoming -> nextA.compareTo(nextB).takeIf { it != 0 }
                    ?: a.title.compareTo(b.title, ignoreCase = true)
                aUpcoming -> -1
                bUpcoming -> 1
                else -> b.date.compareTo(a.date)
            }
        }
}
