package com.lunary.app.ui.letters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.domain.model.Letter
import com.lunary.app.domain.repository.LetterRepository
import com.lunary.app.domain.text.TextLimits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

data class LetterForm(
    val date: LocalDate = LocalDate.now(),
    val timeMinutes: Int = LocalTime.now().let { it.hour * 60 + it.minute },
    val sender: String = "",
    val recipient: String = "",
    val subject: String = "",
    val content: String = ""
) {
    val hasAnyInput: Boolean
        get() = sender.isNotBlank() || recipient.isNotBlank() ||
            subject.isNotBlank() || content.isNotBlank()
}

@HiltViewModel
class LetterEditorViewModel @Inject constructor(
    private val letterRepository: LetterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val letterId: String? = savedStateHandle.get<String>("letterId")
    val isEditing: Boolean = letterId != null

    private val initialDate: LocalDate = savedStateHandle.get<Long>("date")
        ?.takeIf { it != Long.MIN_VALUE }
        ?.let(LocalDate::ofEpochDay)
        ?: LocalDate.now()

    private val _form = MutableStateFlow(LetterForm(date = initialDate))
    val form: StateFlow<LetterForm> = _form.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val _showErrors = MutableStateFlow(false)
    val showErrors: StateFlow<Boolean> = _showErrors.asStateFlow()

    private val savedChannel = Channel<Unit>(Channel.BUFFERED)
    val saved = savedChannel.receiveAsFlow()

    private val deletedChannel = Channel<Unit>(Channel.BUFFERED)
    val deleted = deletedChannel.receiveAsFlow()

    init {
        if (letterId != null) {
            viewModelScope.launch {
                val letter = letterRepository.observeLetter(letterId).first() ?: return@launch
                _form.value = LetterForm(
                    date = letter.date,
                    timeMinutes = letter.timeMinutes,
                    sender = letter.sender,
                    recipient = letter.recipient,
                    subject = letter.subject,
                    content = letter.content
                )
            }
        }
    }

    fun setDate(date: LocalDate) {
        _form.value = _form.value.copy(date = date)
    }

    fun setTime(minutes: Int) {
        _form.value = _form.value.copy(timeMinutes = minutes.coerceIn(0, MINUTES_PER_DAY - 1))
    }

    fun setSender(value: String) {
        _form.value = _form.value.copy(
            sender = TextLimits.truncateGraphemes(value, MAX_PARTY_GRAPHEMES)
        )
    }

    fun setRecipient(value: String) {
        _form.value = _form.value.copy(
            recipient = TextLimits.truncateGraphemes(value, MAX_PARTY_GRAPHEMES)
        )
    }

    fun setSubject(value: String) {
        _form.value = _form.value.copy(
            subject = TextLimits.truncateGraphemes(value, MAX_SUBJECT_GRAPHEMES)
        )
    }

    fun setContent(value: String) {
        _form.value = _form.value.copy(
            content = TextLimits.truncateGraphemes(value, MAX_CONTENT_GRAPHEMES)
        )
    }

    fun save() {
        if (_saving.value || _deleting.value) return
        val current = _form.value
        if (current.sender.isBlank() || current.recipient.isBlank() ||
            current.subject.isBlank() || current.content.isBlank()
        ) {
            _showErrors.value = true
            return
        }
        _saving.value = true
        viewModelScope.launch {
            try {
                val now = Instant.now()
                letterRepository.save(
                    Letter(
                        id = letterId ?: UUID.randomUUID().toString(),
                        date = current.date,
                        timeMinutes = current.timeMinutes,
                        sender = current.sender,
                        recipient = current.recipient,
                        subject = current.subject,
                        content = current.content,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null
                    )
                )
                savedChannel.send(Unit)
            } finally {
                _saving.value = false
            }
        }
    }

    fun delete() {
        val id = letterId ?: return
        if (_saving.value || _deleting.value) return
        _deleting.value = true
        viewModelScope.launch {
            try {
                letterRepository.softDelete(id)
                deletedChannel.send(Unit)
            } finally {
                _deleting.value = false
            }
        }
    }

    companion object {
        const val MAX_PARTY_GRAPHEMES = 80
        const val MAX_SUBJECT_GRAPHEMES = 120
        const val MAX_CONTENT_GRAPHEMES = 5000
        private const val MINUTES_PER_DAY = 24 * 60
    }
}
