package com.lunary.app.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.model.DateMode
import com.lunary.app.domain.model.EmojiCatalog
import com.lunary.app.domain.model.PickedLocation
import com.lunary.app.domain.text.TextLimits
import com.lunary.app.ui.formatDate
import com.lunary.app.ui.weekdayLong
import com.lunary.app.ui.location.LocationMapPreview
import com.lunary.app.ui.location.LocationPickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val TITLE_MAX_GRAPHEMES = 120

private val palette = listOf(
    0xFF7E57C2, 0xFFE91E63, 0xFFFF9800, 0xFF4CAF50, 0xFF2196F3,
    0xFF9C27B0, 0xFF00BCD4, 0xFF795548, 0xFF607D8B
).map { it.toInt() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val chrome by viewModel.chrome.collectAsStateWithLifecycle()
    val titleError by viewModel.titleError.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()
    val existingAttachments by viewModel.existingAttachments.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addAttachments(uris)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editingLocationIndex by remember { mutableStateOf<Int?>(null) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val selectedEmoji = form.emoji
    val emojiAction = stringResource(
        if (selectedEmoji == null) R.string.editor_emoji_add
        else R.string.editor_emoji_change
    )
    val hasTitleText = form.title.isNotBlank()

    fun applyEmoji(emoji: String?) {
        viewModel.setEmoji(emoji)
    }

    LaunchedEffect(Unit) {
        viewModel.savedEventId.collect { onSaved() }
    }

    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onDeleted() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BoxWithConstraints {
                        val compactTitle = maxWidth < 200.dp
                        Text(
                            stringResource(
                                when {
                                    viewModel.isEditing && compactTitle -> R.string.editor_title_edit_short
                                    viewModel.isEditing -> R.string.editor_title_edit
                                    compactTitle -> R.string.editor_title_new_short
                                    else -> R.string.editor_title_new
                                }
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (form.title.isNotBlank() || form.emoji != null) showLeaveDialog = true else onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !saving && !deleting
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = viewModel::save,
                        enabled = hasTitleText && !saving && !deleting,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = {
                        viewModel.setTitle(TextLimits.truncateGraphemes(it, TITLE_MAX_GRAPHEMES))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.field_title)) },
                    placeholder = { Text(stringResource(R.string.hint_title)) },
                    leadingIcon = {
                        IconButton(
                            onClick = { showEmojiPicker = true },
                            modifier = Modifier.semantics {
                                contentDescription = emojiAction
                            }
                        ) {
                            if (selectedEmoji != null) {
                                Text(selectedEmoji, fontSize = 22.sp)
                            } else {
                                Icon(
                                    Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    isError = titleError,
                    supportingText = {
                        Text(
                            if (titleError) {
                                stringResource(R.string.error_title_required)
                            } else {
                                "${TextLimits.graphemeCount(form.title)}/$TITLE_MAX_GRAPHEMES"
                            }
                        )
                    },
                    singleLine = true
                )

                EditorSection(R.string.editor_section_schedule) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable(role = Role.Button) { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = if (form.dateMode == DateMode.MONTH_DAY) {
                                "${form.date.monthValue}月${form.date.dayOfMonth}日"
                            } else {
                                "${formatDate(form.date)} · ${weekdayLong(form.date.dayOfWeek)}"
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    EditorSwitchRow(
                        title = stringResource(R.string.editor_month_day_title),
                        description = stringResource(R.string.editor_hint_month_day),
                        checked = form.dateMode == DateMode.MONTH_DAY,
                        onCheckedChange = { checked ->
                            viewModel.setDateMode(
                                if (checked) DateMode.MONTH_DAY else DateMode.FULL_DATE
                            )
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    EditorSwitchRow(
                        title = stringResource(R.string.switch_yearly),
                        description = stringResource(R.string.editor_hint_yearly),
                        checked = form.yearly,
                        enabled = form.dateMode == DateMode.FULL_DATE,
                        onCheckedChange = viewModel::setYearly
                    )
                }

                EditorSection(R.string.editor_section_category) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = form.categoryId == null,
                                onClick = { viewModel.setCategory(null) },
                                label = { Text(stringResource(R.string.uncategorized)) }
                            )
                            chrome.categories.forEach { category ->
                                FilterChip(
                                    selected = form.categoryId == category.id,
                                    onClick = { viewModel.setCategory(category.id) },
                                    label = { Text(category.name) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(category.colorArgb))
                                        )
                                    }
                                )
                            }
                        }
                    }
                    if (chrome.tags.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            EditorSubLabel(stringResource(R.string.field_tags))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chrome.tags.forEach { tag ->
                                    FilterChip(
                                        selected = tag.id in form.selectedTagIds,
                                        onClick = { viewModel.toggleTag(tag.id) },
                                        label = {
                                            Text(
                                                listOfNotNull(tag.emoji, tag.name)
                                                    .joinToString(" ")
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                EditorSection(R.string.field_color) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        palette.forEachIndexed { index, color ->
                            EditorColorSwatch(
                                index = index,
                                color = color,
                                selected = form.colorArgb == color,
                                onClick = { viewModel.setColor(color) }
                            )
                        }
                    }
                }

                EditorSection(R.string.editor_section_note_location) {
                    OutlinedTextField(
                        value = form.note,
                        onValueChange = viewModel::setNote,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        label = { Text(stringResource(R.string.field_note)) },
                        minLines = 3
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    form.locations.forEachIndexed { index, location ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = location.name,
                                    onValueChange = { viewModel.setLocationName(index, it) },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            stringResource(
                                                if (index == 0) R.string.editor_primary_location
                                                else R.string.field_location
                                            )
                                        )
                                    },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = { viewModel.removeLocation(index) }
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.editor_remove_location)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { editingLocationIndex = index }) {
                                    Text(stringResource(R.string.editor_choose_on_map))
                                }
                                if (location.latitude != null && location.longitude != null) {
                                    Text(
                                        text = stringResource(R.string.editor_located),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            val previewLat = location.latitude
                            val previewLon = location.longitude
                            if (previewLat != null && previewLon != null) {
                                LocationMapPreview(
                                    latitude = previewLat,
                                    longitude = previewLon,
                                    title = location.label.ifBlank { stringResource(R.string.field_location) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .height(160.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = viewModel::addLocation,
                            enabled = form.locations.size < 10
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.editor_add_location))
                        }
                        if (form.locations.size >= 10) {
                            Text(
                                text = stringResource(R.string.editor_location_limit),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                EditorSection(R.string.attachments_title) {
                    val attachmentCount = existingAttachments.size + pendingAttachments.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                attachmentPicker.launch(
                                    arrayOf("image/*", "video/*", "audio/*")
                                )
                            },
                            enabled = attachmentCount < 20 && !saving && !deleting,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_attachment)
                            )
                        }
                    }
                    if (saving) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                    if (attachmentCount == 0) {
                        Text(
                            text = stringResource(R.string.editor_hint_attachments),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        )
                    } else {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        existingAttachments.forEachIndexed { index, attachment ->
                            EditableAttachmentRow(
                                name = attachment.displayName,
                                sizeBytes = attachment.sizeBytes,
                                enabled = !saving && !deleting,
                                onDelete = { viewModel.removeExistingAttachment(attachment) }
                            )
                            if (index != existingAttachments.lastIndex || pendingAttachments.isNotEmpty()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        pendingAttachments.forEachIndexed { index, draft ->
                            EditableAttachmentRow(
                                name = draft.displayName,
                                sizeBytes = draft.sizeBytes,
                                enabled = !saving && !deleting,
                                onDelete = { viewModel.removeAttachment(draft.uri) }
                            )
                            if (index != pendingAttachments.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            selected = selectedEmoji,
            onSelect = { emoji ->
                applyEmoji(emoji)
                showEmojiPicker = false
            },
            onRemove = {
                applyEmoji(null)
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.date
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.setDate(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    val pickerIndex = editingLocationIndex
    if (pickerIndex != null) {
        val location = form.locations.getOrNull(pickerIndex)
        LocationPickerDialog(
            initial = location?.latitude?.let { lat ->
                location.longitude?.let { lon ->
                    PickedLocation(
                        name = location.label.ifBlank { location.address },
                        address = location.address,
                        latitude = lat,
                        longitude = lon
                    )
                }
            },
            onDismiss = { editingLocationIndex = null },
            onConfirm = { picked ->
                viewModel.setPickedLocation(pickerIndex, picked)
                editingLocationIndex = null
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, form.title)) },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteEvent()
                    }
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.cancel)) },
            text = { Text(stringResource(R.string.editor_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    onClose()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun EmojiPickerDialog(
    selected: String?,
    onSelect: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.editor_emoji_picker_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.editor_emoji_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(48.dp),
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(EmojiCatalog.all, key = { it }) { emoji ->
                            Surface(
                                onClick = { onSelect(emoji) },
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                color = if (selected == emoji) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 24.sp)
                                    if (selected == emoji) {
                                        Text(
                                            "✓",
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selected != null) {
                            TextButton(onClick = onRemove) {
                                Text(stringResource(R.string.editor_emoji_remove))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024f / 1024f)
    bytes >= 1024 -> String.format(java.util.Locale.ROOT, "%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}

@Composable
private fun EditableAttachmentRow(
    name: String,
    sizeBytes: Long,
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = formatFileSize(sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onDelete,
            enabled = enabled
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.editor_remove_attachment)
            )
        }
    }
}

@Composable
private fun EditorSection(
    @StringRes titleRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier
                .padding(start = 4.dp, bottom = 8.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun EditorSubLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun EditorSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}

@Composable
private fun EditorColorSwatch(
    index: Int,
    color: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val label = stringResource(R.string.editor_color_option, index + 1)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(color))
        )
        if (selected) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
