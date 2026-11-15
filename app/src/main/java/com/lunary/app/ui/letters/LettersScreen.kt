package com.lunary.app.ui.letters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.ui.components.LetterRow
import com.lunary.app.ui.components.SearchFilterBar
import com.lunary.app.ui.components.LunaryTopAppBar
import com.lunary.app.ui.formatDate
import java.time.LocalDate

private enum class DateField { START, END }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LettersScreen(
    onAddLetter: () -> Unit,
    onLetterClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LettersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf<DateField?>(null) }

    Scaffold(
        topBar = {
            LunaryTopAppBar(
                title = stringResource(R.string.letters_title),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLetter) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_letter)
                )
            }
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
                    .fillMaxHeight()
            ) {
                SearchFilterBar(
                    query = state.filter.query,
                    onQueryChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.search_letters),
                    filterCount = state.filter.advancedCount,
                    onFilterClick = { showFilters = true }
                )

                when {
                    state.totalCount == 0 -> LettersEmptyState(
                        modifier = Modifier.weight(1f)
                    )

                    state.letters.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.letters_no_results),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.letters_no_results_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            TextButton(
                                onClick = viewModel::clearAllFilters,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.clear_filters))
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = stringResource(
                                        R.string.letter_count,
                                        state.letters.size
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                            items(state.letters, key = { it.id }) { letter ->
                                LetterRow(
                                    letter = letter,
                                    onClick = { onLetterClick(letter.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LetterFilterSheet(
                filter = state.filter,
                onStartDate = viewModel::setStartDate,
                onEndDate = viewModel::setEndDate,
                onSender = viewModel::setSender,
                onRecipient = viewModel::setRecipient,
                onDateRange = viewModel::setDateRange,
                onPickStart = { pickingDate = DateField.START },
                onPickEnd = { pickingDate = DateField.END },
                onReset = viewModel::clearAdvancedFilters,
                onDone = { showFilters = false }
            )
        }
    }

    when (pickingDate) {
        DateField.START -> LetterDatePickerDialog(
            initialDate = state.filter.startDate,
            title = stringResource(R.string.filter_start_date),
            onDismiss = { pickingDate = null },
            onConfirm = { date ->
                viewModel.setStartDate(date)
                pickingDate = null
            }
        )

        DateField.END -> LetterDatePickerDialog(
            initialDate = state.filter.endDate,
            title = stringResource(R.string.filter_end_date),
            onDismiss = { pickingDate = null },
            onConfirm = { date ->
                viewModel.setEndDate(date)
                pickingDate = null
            }
        )

        null -> Unit
    }
}

@Composable
private fun LettersEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.letters_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.letters_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LetterFilterSheet(
    filter: LettersFilter,
    onStartDate: (LocalDate?) -> Unit,
    onEndDate: (LocalDate?) -> Unit,
    onSender: (String) -> Unit,
    onRecipient: (String) -> Unit,
    onDateRange: (LocalDate?, LocalDate?) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val last7Start = today.minusDays(6)
    val last30Start = today.minusDays(29)
    val yearStart = today.withDayOfYear(1)
    val yearEnd = today.withDayOfYear(today.lengthOfYear())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.filter_title),
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = stringResource(R.string.filter_time),
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter.startDate == null && filter.endDate == null,
                onClick = { onDateRange(null, null) },
                leadingIcon = if (filter.startDate == null && filter.endDate == null) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                label = { Text(stringResource(R.string.filter_all_time)) }
            )
            FilterChip(
                selected = filter.startDate == last7Start && filter.endDate == today,
                onClick = { onDateRange(last7Start, today) },
                leadingIcon = if (filter.startDate == last7Start && filter.endDate == today) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                label = { Text(stringResource(R.string.filter_last_7_days)) }
            )
            FilterChip(
                selected = filter.startDate == last30Start && filter.endDate == today,
                onClick = { onDateRange(last30Start, today) },
                leadingIcon = if (filter.startDate == last30Start && filter.endDate == today) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                label = { Text(stringResource(R.string.filter_last_30_days)) }
            )
            FilterChip(
                selected = filter.startDate == yearStart && filter.endDate == yearEnd,
                onClick = { onDateRange(yearStart, yearEnd) },
                leadingIcon = if (filter.startDate == yearStart && filter.endDate == yearEnd) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                label = { Text(stringResource(R.string.filter_this_year)) }
            )
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column {
                DateFilterRow(
                    label = stringResource(R.string.filter_start_date),
                    date = filter.startDate,
                    onClick = onPickStart,
                    onClear = { onStartDate(null) }
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                DateFilterRow(
                    label = stringResource(R.string.filter_end_date),
                    date = filter.endDate,
                    onClick = onPickEnd,
                    onClear = { onEndDate(null) }
                )
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.letter_sender),
            style = MaterialTheme.typography.titleSmall
        )
        OutlinedTextField(
            value = filter.sender,
            onValueChange = onSender,
            label = { Text(stringResource(R.string.filter_contains)) },
            placeholder = { Text(stringResource(R.string.filter_sender_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.letter_recipient),
            style = MaterialTheme.typography.titleSmall
        )
        OutlinedTextField(
            value = filter.recipient,
            onValueChange = onRecipient,
            label = { Text(stringResource(R.string.filter_contains)) },
            placeholder = { Text(stringResource(R.string.filter_recipient_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.filter_reset))
            }
            Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.filter_done))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DateFilterRow(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date?.let(::formatDate) ?: stringResource(R.string.filter_any_date),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (date != null) {
            IconButton(onClick = onClear) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.filter_clear_date)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LetterDatePickerDialog(
    initialDate: LocalDate?,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toEpochDay()?.times(MILLIS_PER_DAY)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                    }
                },
                enabled = state.selectedDateMillis != null
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp)
            )
            DatePicker(state = state)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L
