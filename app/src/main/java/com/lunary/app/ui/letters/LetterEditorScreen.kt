package com.lunary.app.ui.letters

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.ui.formatDate
import com.lunary.app.ui.formatTime
import com.lunary.app.ui.weekdayLong
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterEditorScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: LetterEditorViewModel = hiltViewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val deleting by viewModel.deleting.collectAsStateWithLifecycle()
    val showErrors by viewModel.showErrors.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saved.collect { onSaved() }
    }
    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onDeleted() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isEditing) R.string.letter_title_edit
                            else R.string.letter_title_new
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (form.hasAnyInput) showLeaveDialog = true else onClose()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
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
                        enabled = !saving && !deleting,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = form.subject,
                    onValueChange = viewModel::setSubject,
                    label = { Text(stringResource(R.string.letter_subject)) },
                    placeholder = { Text(stringResource(R.string.letter_subject_hint)) },
                    singleLine = true,
                    isError = showErrors && form.subject.isBlank(),
                    supportingText = {
                        if (showErrors && form.subject.isBlank()) {
                            Text(
                                stringResource(
                                    R.string.letter_field_required,
                                    stringResource(R.string.letter_subject)
                                )
                            )
                        } else {
                            Text("${form.subject.length}/${LetterEditorViewModel.MAX_SUBJECT_GRAPHEMES}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = form.sender,
                        onValueChange = viewModel::setSender,
                        label = { Text(stringResource(R.string.letter_sender)) },
                        placeholder = { Text(stringResource(R.string.letter_sender_hint)) },
                        singleLine = true,
                        isError = showErrors && form.sender.isBlank(),
                        supportingText = if (showErrors && form.sender.isBlank()) {
                            {
                                Text(
                                    stringResource(
                                        R.string.letter_field_required,
                                        stringResource(R.string.letter_sender)
                                    )
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = form.recipient,
                        onValueChange = viewModel::setRecipient,
                        label = { Text(stringResource(R.string.letter_recipient)) },
                        placeholder = { Text(stringResource(R.string.letter_recipient_hint)) },
                        singleLine = true,
                        isError = showErrors && form.recipient.isBlank(),
                        supportingText = if (showErrors && form.recipient.isBlank()) {
                            {
                                Text(
                                    stringResource(
                                        R.string.letter_field_required,
                                        stringResource(R.string.letter_recipient)
                                    )
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                LetterSection(title = stringResource(R.string.letter_section_datetime)) {
                    LetterDateTimeRow(
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = stringResource(R.string.letter_date),
                        value = "${formatDate(form.date)} · ${weekdayLong(form.date.dayOfWeek)}",
                        onClick = { showDatePicker = true }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    LetterDateTimeRow(
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_schedule),
                                contentDescription = null
                            )
                        },
                        label = stringResource(R.string.letter_time),
                        value = formatTime(form.timeMinutes),
                        onClick = { showTimePicker = true }
                    )
                }

                OutlinedTextField(
                    value = form.content,
                    onValueChange = viewModel::setContent,
                    label = { Text(stringResource(R.string.letter_content)) },
                    placeholder = { Text(stringResource(R.string.letter_content_hint)) },
                    minLines = 6,
                    maxLines = 12,
                    isError = showErrors && form.content.isBlank(),
                    supportingText = {
                        if (showErrors && form.content.isBlank()) {
                            Text(
                                stringResource(
                                    R.string.letter_field_required,
                                    stringResource(R.string.letter_content)
                                )
                            )
                        } else {
                            Text(
                                stringResource(
                                    R.string.letter_content_count,
                                    form.content.length,
                                    LetterEditorViewModel.MAX_CONTENT_GRAPHEMES
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.date
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
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
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        LetterTimePickerDialog(
            initialTimeMinutes = form.timeMinutes,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.setTime(it)
                showTimePicker = false
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.letter_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.letter_delete_message,
                        form.subject
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun LetterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun LetterDateTimeRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LetterTimePickerDialog(
    initialTimeMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val timeState = rememberTimePickerState(
        initialHour = initialTimeMinutes / 60,
        initialMinute = initialTimeMinutes % 60,
        is24Hour = is24Hour
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.select_time),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                TimePicker(state = timeState)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    TextButton(onClick = {
                        onConfirm(timeState.hour * 60 + timeState.minute)
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
