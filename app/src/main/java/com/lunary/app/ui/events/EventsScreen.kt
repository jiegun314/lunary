package com.lunary.app.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.ui.components.EventRow
import com.lunary.app.ui.components.SearchFilterBar
import com.lunary.app.ui.components.LunaryTopAppBar
import com.lunary.app.ui.detail.DetailScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onAddEvent: () -> Unit,
    onEventClick: (String) -> Unit,
    onEditEvent: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LunaryTopAppBar(
                title = stringResource(R.string.events_title),
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
            FloatingActionButton(onClick = onAddEvent) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val contentWidth = maxWidth
            val twoPane = false // list+detail pane deferred; avoid empty right pane
            var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (twoPane) Arrangement.Start else Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(
                            if (twoPane) contentWidth * 0.46f
                            else contentWidth.coerceAtMost(720.dp)
                        )
                        .fillMaxHeight()
                ) {
            SearchFilterBar(
                query = state.filter.query,
                onQueryChange = viewModel::setQuery,
                placeholder = stringResource(R.string.search_events),
                filterCount = state.filter.activeCount,
                onFilterClick = { showFilters = true }
            )
            if (state.events.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val filtering = state.filter.activeCount > 0
                    Text(
                        text = when {
                            state.filter.query.isNotBlank() -> stringResource(R.string.empty_search)
                            filtering -> stringResource(R.string.no_matching_events)
                            else -> stringResource(R.string.no_events)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (filtering) {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text(stringResource(R.string.clear_filters))
                        }
                    } else if (state.filter.query.isBlank()) {
                        Text(
                            text = stringResource(R.string.no_events_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.event_count_total, state.events.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                    items(state.events, key = { it.id }) { event ->
                        EventRow(
                            event = event,
                            categoryName = event.categoryId?.let { state.categories[it]?.name },
                            policy = state.leapDayPolicy,
                            today = state.today,
                            hasActiveReminder = event.id in state.eventsWithActiveReminders,
                            onClick = {
                                if (twoPane) selectedId = event.id else onEventClick(event.id)
                            }
                        )
                    }
                }
            }
            if (twoPane) {
                VerticalDivider()
                EventsDetailPane(
                    selectedId = selectedId,
                    onEdit = onEditEvent,
                    modifier = Modifier.width(contentWidth * 0.54f)
                )
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
            EventFilterSheet(
                state = state,
                onCategory = viewModel::setCategory,
                onToggleTag = viewModel::toggleTag,
                onLocation = viewModel::setLocation,
                onQuick = viewModel::setQuickFilter,
                onReset = viewModel::clearFilters,
                onDone = { showFilters = false }
            )
        }
    }
}

@Composable
private fun EventsDetailPane(
    selectedId: String?,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nav = rememberNavController()
    LaunchedEffect(selectedId) {
        if (selectedId == null) {
            nav.popBackStack("empty", inclusive = false)
        } else {
            nav.navigate("detail/$selectedId") {
                launchSingleTop = true
                popUpTo("empty")
            }
        }
    }
    NavHost(
        navController = nav,
        startDestination = "empty",
        modifier = modifier.fillMaxHeight()
    ) {
        composable("empty") {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择一个纪念日查看详情",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        composable(
            route = "detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) {
            DetailScreen(
                onBack = {},
                onEdit = onEdit,
                embedded = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventFilterSheet(
    state: EventsUiState,
    onCategory: (String?) -> Unit,
    onToggleTag: (String) -> Unit,
    onLocation: (LocationFilter) -> Unit,
    onQuick: (EventFilter) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter.quick == filter,
                    onClick = { onQuick(filter) },
                    leadingIcon = if (state.filter.quick == filter) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            when (filter) {
                                EventFilter.ALL -> stringResource(R.string.filter_all)
                                EventFilter.UPCOMING -> stringResource(R.string.filter_upcoming)
                                EventFilter.PAST -> stringResource(R.string.filter_past)
                            }
                        )
                    }
                )
            }
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.filter_category),
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter.categoryId == null,
                onClick = { onCategory(null) },
                leadingIcon = if (state.filter.categoryId == null) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                label = { Text(stringResource(R.string.filter_all_categories)) }
            )
            state.categories.values.sortedBy { it.sortOrder }.forEach { category ->
                FilterChip(
                    selected = state.filter.categoryId == category.id,
                    onClick = { onCategory(category.id) },
                    leadingIcon = if (state.filter.categoryId == category.id) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    label = { Text(category.name) }
                )
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.filter_tags),
            style = MaterialTheme.typography.titleSmall
        )
        if (state.tags.isEmpty()) {
            Text(
                text = stringResource(R.string.filter_no_tags),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tags.forEach { tag ->
                    FilterChip(
                        selected = tag.id in state.filter.tagIds,
                        onClick = { onToggleTag(tag.id) },
                        leadingIcon = if (tag.id in state.filter.tagIds) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        label = {
                            Text(listOfNotNull(tag.emoji, tag.name).joinToString(" "))
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.filter_location),
            style = MaterialTheme.typography.titleSmall
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LocationFilter.entries.forEach { location ->
                FilterChip(
                    selected = state.filter.location == location,
                    onClick = { onLocation(location) },
                    leadingIcon = if (state.filter.location == location) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            when (location) {
                                LocationFilter.ANY -> stringResource(R.string.filter_location_any)
                                LocationFilter.HAS -> stringResource(R.string.filter_location_has)
                                LocationFilter.NONE -> stringResource(R.string.filter_location_none)
                            }
                        )
                    }
                )
            }
        }

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
