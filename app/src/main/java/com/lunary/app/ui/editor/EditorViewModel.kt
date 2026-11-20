package com.lunary.app.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.attachment.AttachmentDraft
import com.lunary.app.data.attachment.AttachmentStore
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.Event
import com.lunary.app.domain.model.EventLocation
import com.lunary.app.domain.model.PickedLocation
import com.lunary.app.domain.model.Recurrence
import com.lunary.app.domain.model.Tag
import com.lunary.app.domain.repository.AttachmentRepository
import com.lunary.app.domain.repository.CategoryRepository
import com.lunary.app.domain.repository.EventRepository
import com.lunary.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorForm(
    val title: String = "",
    val emoji: String? = null,
    val date: LocalDate = LocalDate.now(),
    val dateMode: DateMode = DateMode.FULL_DATE,
    val yearly: Boolean = false,
    val categoryId: String? = null,
    val colorArgb: Int = 0xFF7E57C2.toInt(),
    val note: String = "",
    val locations: List<EventLocation> = emptyList(),
    val selectedTagIds: Set<String> = emptySet()
)

data class EditorChromeState(
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList()
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    settingsRepository: SettingsRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentStore: AttachmentStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val eventId: String? = savedStateHandle["eventId"]
    val isEditing: Boolean = eventId != null

    private val initialDate: LocalDate = savedStateHandle.get<Long>("date")
        ?.takeIf { it != Long.MIN_VALUE }
        ?.let { LocalDate.ofEpochDay(it) }
        ?: LocalDate.now()

    private val _form = MutableStateFlow(
        EditorForm(date = initialDate, yearly = false)
    )
    val form: StateFlow<EditorForm> = _form.asStateFlow()

    private val _titleError = MutableStateFlow(false)
    val titleError: StateFlow<Boolean> = _titleError.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<AttachmentDraft>>(emptyList())
    val pendingAttachments: StateFlow<List<AttachmentDraft>> = _pendingAttachments.asStateFlow()

    private val _existingAttachments = MutableStateFlow<List<Attachment>>(emptyList())
    val existingAttachments: StateFlow<List<Attachment>> = _existingAttachments.asStateFlow()

    private val removedAttachments = linkedMapOf<String, Attachment>()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()

    private val savedChannel = Channel<String>(Channel.BUFFERED)
    val savedEventId = savedChannel.receiveAsFlow()

    private val deletedChannel = Channel<Unit>(Channel.BUFFERED)
    val deleted = deletedChannel.receiveAsFlow()

    val chrome: StateFlow<EditorChromeState> = combine(
        categoryRepository.observeCategories(),
        tagRepository.observeTags()
    ) { categories, tags -> EditorChromeState(categories, tags) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EditorChromeState()
        )

    init {
        if (eventId != null) {
            viewModelScope.launch {
                eventRepository.observeEvent(eventId)
                    .filterNotNull()
                    .first()
                    .let { event -> _form.value = event.toForm() }
            }
            viewModelScope.launch {
                attachmentRepository.observeForEvent(eventId).collect { attachments ->
                    _existingAttachments.value = attachments.filterNot { it.id in removedAttachments }
                }
            }
        }
    }

    fun setTitle(value: String) {
        _titleError.value = false
        _form.value = _form.value.copy(title = value)
    }

    fun setEmoji(value: String?) {
        _form.value = _form.value.copy(emoji = value?.takeIf { it.isNotBlank() })
    }

    fun setDate(date: LocalDate) {
        _form.value = _form.value.copy(date = date)
    }

    fun setDateMode(mode: DateMode) {
        _form.value = _form.value.copy(
            dateMode = mode,
            yearly = if (mode == DateMode.MONTH_DAY) true else _form.value.yearly
        )
    }

    fun setYearly(enabled: Boolean) {
        _form.value = _form.value.copy(yearly = enabled)
    }

    fun setCategory(categoryId: String?) {
        val categoryColor = categoryId
            ?.let { id -> chrome.value.categories.firstOrNull { it.id == id }?.colorArgb }
        _form.value = _form.value.copy(
            categoryId = categoryId,
            colorArgb = categoryColor ?: _form.value.colorArgb
        )
    }

    fun setColor(colorArgb: Int) {
        _form.value = _form.value.copy(colorArgb = colorArgb)
    }

    fun setNote(value: String) {
        _form.value = _form.value.copy(note = value)
    }

    fun addLocation() {
        val current = _form.value.locations
        if (current.size >= MAX_EDITOR_LOCATIONS) return
        _form.value = _form.value.copy(locations = current + EventLocation(name = ""))
    }

    fun removeLocation(index: Int) {
        val current = _form.value.locations
        if (index !in current.indices) return
        _form.value = _form.value.copy(locations = current.filterIndexed { i, _ -> i != index })
    }

    fun setLocationName(index: Int, value: String) {
        val current = _form.value.locations
        if (index !in current.indices) return
        _form.value = _form.value.copy(
            locations = current.mapIndexed { i, location ->
                if (i == index) location.copy(name = value) else location
            }
        )
    }

    fun setPickedLocation(index: Int, picked: PickedLocation) {
        val current = _form.value.locations
        if (index !in current.indices) return
        val label = picked.name.ifBlank { picked.address }
        _form.value = _form.value.copy(
            locations = current.mapIndexed { i, location ->
                if (i == index) {
                    location.copy(
                        name = label,
                        address = picked.address,
                        latitude = picked.latitude,
                        longitude = picked.longitude
                    )
                } else {
                    location
                }
            }
        )
    }

    fun toggleTag(tagId: String) {
        val current = _form.value.selectedTagIds
        _form.value = _form.value.copy(
            selectedTagIds = if (tagId in current) current - tagId else current + tagId
        )
    }

    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val occupied = _existingAttachments.value.size + _pendingAttachments.value.size
            val additions = mutableListOf<AttachmentDraft>()
            uris.forEach { uri ->
                if (_pendingAttachments.value.none { it.uri == uri } &&
                    occupied + additions.size < MAX_EDITOR_ATTACHMENTS
                ) {
                    additions += attachmentStore.describe(uri)
                }
            }
            if (additions.isNotEmpty()) {
                _pendingAttachments.value = _pendingAttachments.value + additions
            }
        }
    }

    fun removeAttachment(uri: Uri) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.uri == uri }
    }

    fun removeExistingAttachment(attachment: Attachment) {
        removedAttachments[attachment.id] = attachment
        _existingAttachments.value = _existingAttachments.value.filterNot { it.id == attachment.id }
    }

    fun save() {
        val form = _form.value
        if (form.title.isBlank()) {
            _titleError.value = true
            return
        }
        if (_saving.value || _deleting.value) return
        _saving.value = true
        viewModelScope.launch {
            try {
            val existing = eventId?.let { eventRepository.observeEvent(it).first() }
            val id = eventId ?: UUID.randomUUID().toString()
            val locations = form.locations
                .map { it.copy(name = it.name.trim(), address = it.address.trim()) }
                .filter { it.name.isNotBlank() || it.address.isNotBlank() }
                .take(MAX_EDITOR_LOCATIONS)
            val primaryLocation = locations.firstOrNull()
            val event = Event(
                id = id,
                title = form.title.trim(),
                emoji = form.emoji,
                date = form.date,
                dateMode = form.dateMode,
                recurrence = if (form.yearly) Recurrence.YEARLY else Recurrence.NONE,
                categoryId = form.categoryId,
                colorArgb = form.colorArgb,
                note = form.note.trim(),
                location = primaryLocation?.label.orEmpty(),
                latitude = primaryLocation?.latitude,
                longitude = primaryLocation?.longitude,
                locations = locations,
                tags = emptyList(),
                createdAt = existing?.createdAt ?: Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
            val tags = chrome.value.tags.filter { it.id in form.selectedTagIds }
            eventRepository.save(event.copy(tags = tags))
            removedAttachments.values.toList().forEach { attachment ->
                attachmentRepository.delete(attachment)
            }
            _pendingAttachments.value.forEach { draft ->
                attachmentRepository.add(id, draft.uri)
            }
            savedChannel.send(id)
            } finally {
                _saving.value = false
            }
        }
    }

    fun deleteEvent() {
        val id = eventId ?: return
        if (_deleting.value || _saving.value) return
        _deleting.value = true
        viewModelScope.launch {
            try {
                eventRepository.softDelete(id)
                deletedChannel.send(Unit)
            } finally {
                _deleting.value = false
            }
        }
    }

    private companion object {
        const val MAX_EDITOR_ATTACHMENTS = 20
        const val MAX_EDITOR_LOCATIONS = 10
    }

    private fun Event.toForm(): EditorForm = EditorForm(
        title = title,
        emoji = emoji,
        date = date,
        dateMode = dateMode,
        yearly = recurrence == Recurrence.YEARLY,
        categoryId = categoryId,
        colorArgb = colorArgb,
        note = note,
        locations = locations.ifEmpty {
            EventLocation(location, "", latitude, longitude)
                .takeIf { location.isNotBlank() || latitude != null || longitude != null }
                ?.let(::listOf)
                .orEmpty()
        },
        selectedTagIds = tagIds
    )
}
