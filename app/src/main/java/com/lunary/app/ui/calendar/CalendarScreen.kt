package com.lunary.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.domain.date.DayMath
import com.lunary.app.ui.components.LunaryTopAppBar
import com.lunary.app.ui.components.EventRow
import com.lunary.app.ui.components.LetterRow
import com.lunary.app.ui.formatMonthDay
import com.lunary.app.ui.formatSelectedDate
import com.lunary.app.ui.LunarDate
import com.lunary.app.ui.weekdayShort
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAddEvent: (LocalDate) -> Unit,
    onEventClick: (String) -> Unit,
    onAddLetter: (LocalDate) -> Unit,
    onLetterClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LunaryTopAppBar(
                title = stringResource(R.string.nav_calendar),
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add)
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val expanded = maxWidth >= 760.dp && maxHeight >= 560.dp
            Column(Modifier.fillMaxSize()) {
                if (expanded) {
                    Row(Modifier.weight(1f)) {
                        CalendarPane(
                            state = state,
                            onShift = viewModel::shiftPeriod,
                            onSelect = viewModel::selectDate,
                            onModeChange = viewModel::setViewMode,
                            onToday = viewModel::goToday,
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                        )
                        VerticalDivider()
                        DayEventsPane(
                            state = state,
                            onEventClick = onEventClick,
                            onLetterClick = onLetterClick,
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    CompactCalendar(
                        state = state,
                        onShift = viewModel::shiftPeriod,
                        onSelect = viewModel::selectDate,
                        onModeChange = viewModel::setViewMode,
                        onToday = viewModel::goToday,
                        onEventClick = onEventClick,
                        onLetterClick = onLetterClick
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.add_to_date,
                        formatSelectedDate(state.selectedDate)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.add_event)) },
                    supportingContent = { Text(stringResource(R.string.add_event_hint)) },
                    leadingContent = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        onAddEvent(state.selectedDate)
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.add_letter)) },
                    supportingContent = { Text(stringResource(R.string.add_letter_hint)) },
                    leadingContent = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.clickable {
                        showAddSheet = false
                        onAddLetter(state.selectedDate)
                    }
                )
            }
        }
    }
}


@Composable
private fun CalendarPane(
    state: CalendarUiState,
    onShift: (Long) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onModeChange: (CalendarViewMode) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        CalendarControls(
            mode = state.viewMode,
            onModeChange = onModeChange,
            onToday = onToday
        )
        PeriodHeader(state = state, onShift = onShift)
        when (state.viewMode) {
            CalendarViewMode.MONTH -> MonthView(state = state, onSelect = onSelect)
            CalendarViewMode.WEEK -> WeekView(state = state, onSelect = onSelect)
            CalendarViewMode.DAY -> DaySummary(state = state)
        }
    }
}

@Composable
private fun CompactCalendar(
    state: CalendarUiState,
    onShift: (Long) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onModeChange: (CalendarViewMode) -> Unit,
    onToday: () -> Unit,
    onEventClick: (String) -> Unit,
    onLetterClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            CalendarPane(
                state = state,
                onShift = onShift,
                onSelect = onSelect,
                onModeChange = onModeChange,
                onToday = onToday,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.viewMode != CalendarViewMode.DAY) {
            item { SelectedDateHeader(state) }
        }
        dayEntries(state, onEventClick, onLetterClick)
    }
}

@Composable
private fun DayEventsPane(
    state: CalendarUiState,
    onEventClick: (String) -> Unit,
    onLetterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { SelectedDateHeader(state) }
        dayEntries(state, onEventClick, onLetterClick)
    }
}

private fun LazyListScope.dayEntries(
    state: CalendarUiState,
    onEventClick: (String) -> Unit,
    onLetterClick: (String) -> Unit
) {
    val hasEvents = state.selectedEvents.isNotEmpty()
    val hasLetters = state.selectedLetters.isNotEmpty()
    if (!hasEvents && !hasLetters) {
        item { EmptyDay() }
        return
    }
    if (hasEvents) {
        if (hasLetters) {
            item { DaySectionHeader(stringResource(R.string.events_title)) }
        }
        items(state.selectedEvents, key = { "event-${it.id}" }) { event ->
            EventRow(
                event = event,
                categoryName = state.categories.find { it.id == event.categoryId }?.name,
                policy = state.leapDayPolicy,
                onClick = { onEventClick(event.id) },
                today = state.today,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
    if (hasLetters) {
        item { DaySectionHeader(stringResource(R.string.letters_title)) }
        items(state.selectedLetters, key = { "letter-${it.id}" }) { letter ->
            LetterRow(
                letter = letter,
                onClick = { onLetterClick(letter.id) },
                showDate = false,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DaySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SelectedDateHeader(state: CalendarUiState) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatSelectedDate(state.selectedDate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = LunarDate.full(state.selectedDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .alignByBaseline()
            )
        }
        val countText = dayCountText(state)
        if (countText != null) {
            Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun dayCountText(state: CalendarUiState): String? {
    val eventCount = state.selectedEvents.size
    val letterCount = state.selectedLetters.size
    return when {
        eventCount > 0 && letterCount > 0 -> stringResource(
            R.string.event_and_letter_count,
            eventCount,
            letterCount
        )
        letterCount > 0 -> stringResource(R.string.letter_count, letterCount)
        eventCount > 0 -> stringResource(R.string.event_count, eventCount)
        else -> null
    }
}

@Composable
private fun EmptyDay() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.empty_day_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.empty_day_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ViewModeMenu(
    mode: CalendarViewMode,
    onModeChange: (CalendarViewMode) -> Unit
) {
    val labels = listOf(
        CalendarViewMode.MONTH to R.string.view_month,
        CalendarViewMode.WEEK to R.string.view_week,
        CalendarViewMode.DAY to R.string.view_day
    )
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(stringResource(labels.first { it.first == mode }.second))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            labels.forEach { (item, labelRes) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onModeChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarControls(
    mode: CalendarViewMode,
    onModeChange: (CalendarViewMode) -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ViewModeMenu(mode = mode, onModeChange = onModeChange)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onToday) {
            Text(stringResource(R.string.today))
        }
    }
}

@Composable
private fun PeriodHeader(
    state: CalendarUiState,
    onShift: (Long) -> Unit
) {
    val label = when (state.viewMode) {
        CalendarViewMode.MONTH -> stringResource(
            R.string.month_title,
            state.selectedDate.year,
            state.selectedDate.monthValue
        )
        CalendarViewMode.WEEK -> stringResource(
            R.string.week_range,
            formatMonthDay(state.weekStart),
            formatMonthDay(state.weekEnd)
        )
        CalendarViewMode.DAY -> formatSelectedDate(state.selectedDate)
    }
    val previousLabel = when (state.viewMode) {
        CalendarViewMode.MONTH -> stringResource(R.string.previous_month)
        CalendarViewMode.WEEK -> stringResource(R.string.previous_week)
        CalendarViewMode.DAY -> stringResource(R.string.previous_day)
    }
    val nextLabel = when (state.viewMode) {
        CalendarViewMode.MONTH -> stringResource(R.string.next_month)
        CalendarViewMode.WEEK -> stringResource(R.string.next_week)
        CalendarViewMode.DAY -> stringResource(R.string.next_day)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = previousLabel)
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = nextLabel)
        }
    }
}

@Composable
private fun MonthView(
    state: CalendarUiState,
    onSelect: (LocalDate) -> Unit
) {
    val month = YearMonth.from(state.selectedDate)
    val dates = remember(month, state.firstDayOfWeek) {
        monthGridDates(month, state.firstDayOfWeek)
    }
    val eventColors = remember(dates, state.allEvents, state.leapDayPolicy) {
        dates.associateWith { date ->
            state.allEvents
                .filter { DayMath.occursOn(it, date, state.leapDayPolicy) }
                .map { it.colorArgb }
        }
    }
    val letterDates = remember(dates, state.allLetters) {
        state.allLetters.mapTo(mutableSetOf()) { it.date }
    }
    WeekdayHeader(
        firstDayOfWeek = state.firstDayOfWeek,
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 2.dp)
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val landscape = LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val maxCell = when {
            landscape -> 36.dp
            maxWidth >= 600.dp -> 44.dp
            else -> 48.dp
        }
        val cellSize = minOf(maxWidth / 7, maxCell)
        MonthGrid(
            month = month,
            selectedDate = state.selectedDate,
            today = state.today,
            eventColorsByDate = eventColors,
            letterDates = letterDates,
            firstDayOfWeek = state.firstDayOfWeek,
            cellSize = cellSize,
            onDateClick = onSelect
        )
    }
}

@Composable
private fun WeekView(
    state: CalendarUiState,
    onSelect: (LocalDate) -> Unit
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val textScale = fontScale.coerceAtMost(1.3f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (offset in 0..6) {
            val date = state.weekStart.plusDays(offset.toLong())
            val selected = date == state.selectedDate
            val colors = state.allEvents
                .filter { DayMath.occursOn(it, date, state.leapDayPolicy) }
                .map { it.colorArgb }
            val count = colors.size
            val hasLetter = state.allLetters.any { it.date == date }
            val description = stringResource(
                when {
                    hasLetter && count > 0 -> R.string.day_cell_description_event_letter
                    hasLetter -> R.string.day_cell_description_letter
                    else -> R.string.day_cell_description
                },
                date.year,
                date.monthValue,
                date.dayOfMonth,
                count
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                    .semantics {
                        contentDescription = description
                        this.selected = selected
                    }
                    .clickable { onSelect(date) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = weekdayShort(date.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = (11f * textScale / fontScale).sp,
                    lineHeight = (16f * textScale / fontScale).sp,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                DateMarkers(
                    selected = selected,
                    muted = false,
                    hasLetter = hasLetter,
                    eventColors = colors,
                    maxDots = 4
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = (16f * textScale / fontScale).sp,
                        lineHeight = (24f * textScale / fontScale).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummary(state: CalendarUiState) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.month_title,
                    state.selectedDate.year,
                    state.selectedDate.monthValue
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.selectedDate.dayOfMonth.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = dayCountText(state) ?: stringResource(R.string.no_entry_count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

