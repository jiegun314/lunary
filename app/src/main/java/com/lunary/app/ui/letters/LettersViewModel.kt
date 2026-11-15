package com.lunary.app.ui.letters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.domain.model.Letter
import com.lunary.app.domain.repository.LetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class LettersFilter(
    val query: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val sender: String = "",
    val recipient: String = ""
) {
    val advancedCount: Int
        get() = listOf(
            startDate != null || endDate != null,
            sender.isNotBlank(),
            recipient.isNotBlank()
        ).count { it }

    fun matches(letter: Letter): Boolean {
        val term = query.trim()
        if (term.isNotEmpty()) {
            val hit = listOf(letter.subject, letter.content, letter.sender, letter.recipient)
                .any { it.contains(term, ignoreCase = true) }
            if (!hit) return false
        }
        if (startDate != null && letter.date.isBefore(startDate)) return false
        if (endDate != null && letter.date.isAfter(endDate)) return false
        val from = sender.trim()
        if (from.isNotEmpty() && !letter.sender.contains(from, ignoreCase = true)) return false
        val to = recipient.trim()
        if (to.isNotEmpty() && !letter.recipient.contains(to, ignoreCase = true)) return false
        return true
    }
}

data class LettersUiState(
    val letters: List<Letter> = emptyList(),
    val filter: LettersFilter = LettersFilter(),
    val totalCount: Int = 0
) {
    val hasActiveFilter: Boolean
        get() = filter.query.isNotBlank() || filter.advancedCount > 0
}

@HiltViewModel
class LettersViewModel @Inject constructor(
    letterRepository: LetterRepository
) : ViewModel() {

    private val filter = MutableStateFlow(LettersFilter())

    val uiState: StateFlow<LettersUiState> = combine(
        letterRepository.observeLetters(),
        filter
    ) { letters, currentFilter ->
        LettersUiState(
            letters = letters.filter(currentFilter::matches),
            filter = currentFilter,
            totalCount = letters.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LettersUiState()
    )

    fun setQuery(value: String) = filter.update { it.copy(query = value) }

    fun setStartDate(value: LocalDate?) = filter.update {
        val end = it.endDate
        it.copy(
            startDate = value,
            endDate = if (value != null && end != null && value.isAfter(end)) value else end
        )
    }

    fun setEndDate(value: LocalDate?) = filter.update {
        val start = it.startDate
        it.copy(
            endDate = value,
            startDate = if (value != null && start != null && value.isBefore(start)) value else start
        )
    }

    fun setSender(value: String) = filter.update { it.copy(sender = value) }

    fun setRecipient(value: String) = filter.update { it.copy(recipient = value) }

    fun setDateRange(start: LocalDate?, end: LocalDate?) = filter.update {
        it.copy(startDate = start, endDate = end)
    }

    fun clearAdvancedFilters() = filter.update {
        it.copy(startDate = null, endDate = null, sender = "", recipient = "")
    }

    fun clearAllFilters() = filter.update { LettersFilter() }
}
