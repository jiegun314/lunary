package com.lunary.app.ui.letters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.domain.model.Letter
import com.lunary.app.domain.repository.LetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LetterDetailViewModel @Inject constructor(
    letterRepository: LetterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val letterId: String = checkNotNull(savedStateHandle["letterId"])

    val letter: StateFlow<Letter?> = letterRepository.observeLetter(letterId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
