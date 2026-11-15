package com.lunary.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.domain.date.DayMath
import com.lunary.app.ui.components.BrandLockup
import com.lunary.app.ui.components.EventRow
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
    onOpenSettings: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { BrandLockup() },
                actions = {
                    ViewModeMenu(
                        mode = state.viewMode,
                        onModeChange = viewModel::setViewMode
                    )
                    TextButton(onClick = viewModel::goToday) {
                        Text(stringResource(R.string.today))
                    }
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
            FloatingActionButton(onClick = { onAddEvent(state.selectedDate) }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_event)
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
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                        )
                        VerticalDivider()
                        DayEventsPane(
                            state = state,
                            onEventClick = onEventClick,
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
                        onEventClick = onEventClick
                    )
                }
            }
        }
    }
}


@Composable
private fun CalendarPane(
    state: CalendarUiState,
    onShift: (Long) -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
    onEventClick: (String) -> Unit
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
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.viewMode != CalendarViewMode.DAY) {
            item { SelectedDateHeader(state) }
        }
        if (state.selectedEvents.isEmpty()) {
            item { EmptyDay() }
        } else {
            items(state.selectedEvents, key = { it.id }) { event ->
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
    }
}

@Composable
private fun DayEventsPane(
    state: CalendarUiState,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { SelectedDateHeader(state) }
        if (state.selectedEvents.isEmpty()) {
            item { EmptyDay() }
        } else {
            items(state.selectedEvents, key = { it.id }) { event ->
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
    }
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
        if (state.selectedEvents.isNotEmpty()) {
            Text(
                text = stringResource(R.string.event_count, state.selectedEvents.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
            val description = stringResource(
                R.string.day_cell_description,
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
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (count > 0) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(minOf(colors.size, 4)) { index ->
                            val argb = colors[index]
                            BoxDot(
                                color = if (argb != 0) androidx.compose.ui.graphics.Color(argb)
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun BoxDot(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(color)
    )
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
                text = if (state.selectedEvents.isEmpty()) {
                    stringResource(R.string.no_event_count)
                } else {
                    stringResource(R.string.event_count, state.selectedEvents.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

