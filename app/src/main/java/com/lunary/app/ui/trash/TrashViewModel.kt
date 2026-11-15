package com.lunary.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.Letter
import com.lunary.app.domain.repository.EventRepository
import com.lunary.app.domain.repository.LetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashItem(
    val event: Event,
    val remainingDays: Int
)

data class TrashLetterItem(
    val letter: Letter,
    val remainingDays: Int
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: EventRepository,
    private val letterRepository: LetterRepository,
    private val clock: Clock
) : ViewModel() {

    val items: StateFlow<List<TrashItem>> = repository.observeTrashedEvents()
        .map { events ->
            val now = Instant.now(clock)
            events.map { event ->
                val deletedAt = event.deletedAt ?: now
                val elapsed = ChronoUnit.DAYS.between(deletedAt, now).toInt()
                TrashItem(
                    event = event,
                    remainingDays = (RETENTION_DAYS - elapsed).coerceIn(0, RETENTION_DAYS)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val letterItems: StateFlow<List<TrashLetterItem>> = letterRepository.observeTrashedLetters()
        .map { letters ->
            val now = Instant.now(clock)
            letters.map { letter ->
                val deletedAt = letter.deletedAt ?: now
                val elapsed = ChronoUnit.DAYS.between(deletedAt, now).toInt()
                TrashLetterItem(
                    letter = letter,
                    remainingDays = (RETENTION_DAYS - elapsed).coerceIn(0, RETENTION_DAYS)
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun restore(id: String) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun deletePermanently(id: String) {
        viewModelScope.launch { repository.deletePermanently(id) }
    }

    fun restoreLetter(id: String) {
        viewModelScope.launch { letterRepository.restore(id) }
    }

    fun deleteLetterPermanently(id: String) {
        viewModelScope.launch { letterRepository.deletePermanently(id) }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            letterRepository.emptyTrash()
        }
    }

    companion object {
        const val RETENTION_DAYS = 30
    }
}
