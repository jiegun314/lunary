package com.lunary.app.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.domain.date.DayMath
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.domain.repository.CategoryRepository
import com.lunary.app.domain.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class CalendarViewMode { MONTH, WEEK, DAY }

data class CalendarUiState(
    val today: LocalDate,
    val selectedDate: LocalDate,
    val allEvents: List<Event>,
    val selectedEvents: List<Event>,
    val categories: List<Category> = emptyList(),
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val leapDayPolicy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28,
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH
) {
    val baseMonth: YearMonth get() = YearMonth.from(today)

    val weekStart: LocalDate
        get() = DayMath.weekStart(selectedDate, firstDayOfWeek)

    val weekEnd: LocalDate get() = weekStart.plusDays(6)
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    eventRepository: EventRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val today: LocalDate get() = LocalDate.now(clock)

    private val selectedEpochDay =
        savedStateHandle.getStateFlow(KEY_SELECTED_DATE, today.toEpochDay())

    private val viewMode = MutableStateFlow(CalendarViewMode.MONTH)

    val uiState: StateFlow<CalendarUiState> = combine(
        eventRepository.observeEvents(),
        categoryRepository.observeCategories(),
        selectedEpochDay,
        settingsRepository.settings,
        viewMode
    ) { events, categories, epochDay, settings, mode ->
        val date = LocalDate.ofEpochDay(epochDay)
        val selected = events
            .filter { DayMath.occursOn(it, date, settings.leapDayPolicy) }
            .sortedWith(compareBy({ it.dateMode }, { it.title }))
        CalendarUiState(
            today = today,
            selectedDate = date,
            allEvents = events,
            selectedEvents = selected,
            categories = categories,
            firstDayOfWeek = settings.firstDayOfWeek(),
            leapDayPolicy = settings.leapDayPolicy,
            viewMode = mode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            today = today,
            selectedDate = today,
            allEvents = emptyList(),
            selectedEvents = emptyList()
        )
    )

    fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED_DATE] = date.toEpochDay()
    }

    fun goToday() = selectDate(today)

    fun setViewMode(mode: CalendarViewMode) {
        viewMode.value = mode
    }

    /** Moves the selection by one month/week/day according to the current view. */
    fun shiftPeriod(delta: Long) {
        val current = LocalDate.ofEpochDay(selectedEpochDay.value)
        val target = when (viewMode.value) {
            CalendarViewMode.MONTH -> current.plusMonths(delta)
            CalendarViewMode.WEEK -> current.plusWeeks(delta)
            CalendarViewMode.DAY -> current.plusDays(delta)
        }
        selectDate(target)
    }

    companion object {
        const val KEY_SELECTED_DATE = "selectedDateEpochDay"
    }
}
