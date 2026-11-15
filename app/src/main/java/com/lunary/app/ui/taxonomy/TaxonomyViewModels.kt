package com.lunary.app.ui.taxonomy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.domain.model.Category
import com.lunary.app.domain.model.Tag
import com.lunary.app.domain.repository.CategoryRepository
import com.lunary.app.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.observeCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun save(existing: Category?, name: String, colorArgb: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val nextOrder = (categories.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            repository.save(
                Category(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    colorArgb = colorArgb,
                    sortOrder = existing?.sortOrder ?: nextOrder,
                    createdAt = existing?.createdAt ?: Instant.EPOCH,
                    updatedAt = Instant.EPOCH
                )
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val current = categories.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        if (fromIndex == toIndex) return
        val reordered = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch { repository.reorder(reordered.map { it.id }) }
    }
}

@HiltViewModel
class TagViewModel @Inject constructor(
    private val repository: TagRepository
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = repository.observeTags().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun save(existing: Tag?, name: String, emoji: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val nextOrder = (tags.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            repository.save(
                Tag(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    emoji = emoji?.trim()?.ifBlank { null },
                    colorArgb = existing?.colorArgb,
                    sortOrder = existing?.sortOrder ?: nextOrder,
                    createdAt = existing?.createdAt ?: Instant.EPOCH,
                    updatedAt = Instant.EPOCH
                )
            )
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun move(fromIndex: Int, toIndex: Int) {
        val current = tags.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        if (fromIndex == toIndex) return
        val reordered = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch { repository.reorder(reordered.map { it.id }) }
    }
}
