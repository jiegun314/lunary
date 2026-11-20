package com.lunary.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.domain.date.eventDayMetric
import com.lunary.app.domain.model.DateMode
import com.lunary.app.ui.components.EventDayMetricBlock
import com.lunary.app.ui.components.eventCardContainerColor
import com.lunary.app.ui.formatDate
import com.lunary.app.ui.weekdayLong
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import android.text.format.DateFormat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.model.Reminder
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    embedded: Boolean = false,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showReminderPanel by remember { mutableStateOf(false) }
    var editingReminderTime by remember { mutableStateOf<Reminder?>(null) }
    var showNotificationRationale by remember { mutableStateOf(false) }
    var attachmentViewerIndex by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    attachmentViewerIndex?.let { index ->
        AttachmentViewerDialog(
            attachments = state.attachments,
            initialIndex = index,
            fileFor = viewModel::fileFor,
            contentUri = viewModel::contentUri,
            onDismiss = { attachmentViewerIndex = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val currentEvent = state.event
                    if (currentEvent != null) {
                        Text(
                            text = listOfNotNull(currentEvent.emoji, currentEvent.title)
                                .joinToString(" "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        BoxWithConstraints {
                            val compactTitle = maxWidth < 180.dp
                            Text(
                                stringResource(
                                    when {
                                        embedded -> R.string.events_title
                                        compactTitle -> R.string.detail_details
                                        else -> R.string.detail_title
                                    }
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (!embedded) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                actions = {
                    val event = state.event
                    if (event != null) {
                        val activeReminderCount = state.activeReminderIds.size
                        val alarmDescription = if (activeReminderCount > 0) {
                            stringResource(
                                R.string.reminder_panel_action_active,
                                activeReminderCount
                            )
                        } else {
                            stringResource(R.string.reminder_panel_action)
                        }
                        IconButton(
                            onClick = {
                                val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    showNotificationRationale = true
                                } else {
                                    showReminderPanel = true
                                }
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .then(
                                    if (activeReminderCount > 0) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            BadgedBox(
                                badge = {
                                    if (activeReminderCount > 0) {
                                        Badge {
                                            Text(
                                                text = activeReminderCount.toString(),
                                                modifier = Modifier.clearAndSetSemantics {}
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_alarm),
                                    contentDescription = alarmDescription,
                                    tint = if (activeReminderCount > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { onEdit(event.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            !state.loaded -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.event == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.empty_search)) }

            else -> {
                val event = state.event!!
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val metric = eventDayMetric(event, state.policy)
                        val dateText = if (event.dateMode == DateMode.MONTH_DAY) {
                            "每年 ${event.date.monthValue}月${event.date.dayOfMonth}日"
                        } else {
                            "${formatDate(event.date)} · " +
                                weekdayLong(event.date.dayOfWeek)
                        }
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = eventCardContainerColor(metric.isPast),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(
                                        modifier = Modifier.height(IntrinsicSize.Min),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(event.colorArgb))
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        EventDayMetricBlock(
                                            metric = metric,
                                            compact = false,
                                            modifier = Modifier.widthIn(
                                                min = 112.dp,
                                                max = 160.dp
                                            )
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    BasicText(
                                        text = dateText,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End
                                        ),
                                        maxLines = 1,
                                        autoSize = TextAutoSize.StepBased(
                                            minFontSize = 7.sp,
                                            maxFontSize = 16.sp,
                                            stepSize = 0.5.sp
                                        )
                                    )
                                }
                                if (state.category != null || event.tags.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (state.category != null) {
                                            Row(
                                                modifier = Modifier.widthIn(max = 160.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Color(state.category!!.colorArgb)
                                                        )
                                                )
                                                Text(
                                                    text = state.category!!.name,
                                                    modifier = Modifier.padding(start = 8.dp),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (event.tags.isNotEmpty()) {
                                            if (state.category != null) {
                                                Spacer(Modifier.width(12.dp))
                                            }
                                            FlowRow(
                                                modifier = Modifier.weight(1f),
                                                horizontalArrangement = Arrangement.spacedBy(
                                                    6.dp,
                                                    Alignment.End
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                event.tags.forEach { tag ->
                                                    DetailTag(
                                                        text = listOfNotNull(tag.emoji, tag.name)
                                                            .joinToString(" ")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Column(Modifier.fillMaxWidth()) {
                            DetailPrimaryHeader(R.string.detail_details)

                            DetailSubsection(R.string.field_note) {
                                if (event.note.isBlank()) {
                                    DetailEmptyText(R.string.detail_no_note)
                                } else {
                                    Column(
                                        Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        Text(
                                            text = event.note,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }

                            DetailSubsection(R.string.field_location) {
                                if (event.location.isBlank()) {
                                    DetailEmptyText(R.string.detail_no_location)
                                } else {
                                    Column(
                                        Modifier.fillMaxWidth().padding(16.dp)
                                    ) {
                                        Text(
                                            text = event.location,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        val lat = event.latitude
                                        val lon = event.longitude
                                        if (lat != null && lon != null) {
                                            Spacer(Modifier.height(12.dp))
                                            com.lunary.app.ui.location.LocationMapPreview(
                                                latitude = lat,
                                                longitude = lon,
                                                title = event.location,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(160.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            DetailSubsection(R.string.attachments_title) {
                                if (state.attachments.isEmpty()) {
                                    DetailEmptyText(R.string.no_attachments)
                                } else {
                                    AttachmentGallery(
                                        attachments = state.attachments,
                                        viewModel = viewModel,
                                        onOpen = { attachment ->
                                            attachmentViewerIndex = state.attachments
                                                .indexOfFirst { it.id == attachment.id }
                                                .takeIf { it >= 0 }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = {
                showNotificationRationale = false
                showReminderPanel = true
            },
            title = { Text(stringResource(R.string.reminder_channel_name)) },
            text = { Text(stringResource(R.string.reminder_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    showReminderPanel = true
                }) { Text(stringResource(R.string.reminder_permission_button)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    showReminderPanel = true
                }) { Text(stringResource(R.string.reminder_permission_skip)) }
            }
        )
    }

    if (showReminderPanel) {
        ReminderPanel(
            reminders = state.reminders,
            activeCount = state.activeReminderIds.size,
            onDismiss = { showReminderPanel = false },
            onAddPreset = { minutesBefore, timeMinutes ->
                viewModel.addReminder(minutesBefore, timeMinutes)
            },
            onTogglePreset = { minutesBefore, timeMinutes ->
                val matching = state.reminders.filter { it.minutesBefore == minutesBefore }
                when {
                    matching.isEmpty() -> viewModel.addReminder(minutesBefore, timeMinutes)
                    matching.any { it.enabled } ->
                        matching.filter { it.enabled }.forEach {
                            viewModel.setReminderEnabled(it, false)
                        }
                    else -> matching.forEach { viewModel.setReminderEnabled(it, true) }
                }
            },
            onToggleReminder = { reminder, enabled ->
                viewModel.setReminderEnabled(reminder, enabled)
            },
            onDeleteReminder = { reminder -> viewModel.deleteReminder(reminder.id) },
            onEditTime = { reminder -> editingReminderTime = reminder }
        )
    }

    editingReminderTime?.let { reminder ->
        ReminderTimePickerDialog(
            initialTimeMinutes = reminder.localTimeMinutes,
            onDismiss = { editingReminderTime = null },
            onConfirm = { timeMinutes ->
                viewModel.updateReminder(reminder, reminder.minutesBefore, timeMinutes)
                editingReminderTime = null
            }
        )
    }

}


@Composable
private fun DetailPrimaryHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DetailSubsection(
    @StringRes titleRes: Int,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            action?.invoke()
        }
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
private fun DetailEmptyText(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
private fun DetailTag(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private data class ReminderPreset(
    val minutesBefore: Int,
    @StringRes val titleRes: Int
)

private val milestonePresets = listOf(
    ReminderPreset(-50 * 1440, R.string.reminder_milestone_50),
    ReminderPreset(-100 * 1440, R.string.reminder_milestone_100),
    ReminderPreset(-365 * 1440, R.string.reminder_milestone_year)
)

private val advancePresets = listOf(
    ReminderPreset(0, R.string.reminder_offset_same_day),
    ReminderPreset(1440, R.string.reminder_offset_one_day),
    ReminderPreset(4320, R.string.reminder_offset_three_days),
    ReminderPreset(10080, R.string.reminder_offset_seven_days)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPanel(
    reminders: List<Reminder>,
    activeCount: Int,
    onDismiss: () -> Unit,
    onAddPreset: (minutesBefore: Int, timeMinutes: Int) -> Unit,
    onTogglePreset: (minutesBefore: Int, timeMinutes: Int) -> Unit,
    onToggleReminder: (Reminder, Boolean) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
    onEditTime: (Reminder) -> Unit
) {
    var defaultTime by remember { mutableIntStateOf(DetailViewModel.DEFAULT_REMINDER_TIME) }
    var showDefaultTimePicker by remember { mutableStateOf(false) }
    val sortedReminders = reminders.sortedWith(
        compareByDescending<Reminder> { it.enabled }
            .thenBy { it.minutesBefore }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val wide = maxWidth >= 600.dp
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                modifier = if (wide) {
                    Modifier.widthIn(max = 560.dp).fillMaxHeight(0.92f)
                } else {
                    Modifier.fillMaxSize()
                }
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reminder_panel_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = if (activeCount > 0) {
                                    stringResource(R.string.reminder_panel_active_count, activeCount)
                                } else {
                                    stringResource(R.string.reminder_panel_no_active)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reminder_panel_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ReminderPanelSection(R.string.reminder_panel_default_time) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reminder_panel_default_time_desc),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(onClick = { showDefaultTimePicker = true }) {
                                    Text(reminderTimeLabel(defaultTime))
                                }
                            }
                        }

                        ReminderPanelSection(R.string.reminder_panel_milestones) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.reminder_panel_milestones_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    milestonePresets.forEach { preset ->
                                        val selected = reminders.any {
                                            it.minutesBefore == preset.minutesBefore && it.enabled
                                        }
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                onTogglePreset(preset.minutesBefore, defaultTime)
                                            },
                                            label = { Text(stringResource(preset.titleRes)) },
                                            leadingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        ReminderPanelSection(R.string.reminder_panel_advance) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.reminder_panel_advance_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    advancePresets.forEach { preset ->
                                        val selected = reminders.any {
                                            it.minutesBefore == preset.minutesBefore && it.enabled
                                        }
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                onTogglePreset(preset.minutesBefore, defaultTime)
                                            },
                                            label = { Text(stringResource(preset.titleRes)) },
                                            leadingIcon = if (selected) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        ReminderPanelSection(R.string.reminder_panel_configured) {
                            if (sortedReminders.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.reminder_panel_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                sortedReminders.forEachIndexed { index, reminder ->
                                    ReminderSettingRow(
                                        reminder = reminder,
                                        onEditTime = { onEditTime(reminder) },
                                        onEnabledChange = {
                                            onToggleReminder(reminder, it)
                                        },
                                        onDelete = { onDeleteReminder(reminder) }
                                    )
                                    if (index != sortedReminders.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showDefaultTimePicker) {
        ReminderTimePickerDialog(
            initialTimeMinutes = defaultTime,
            onDismiss = { showDefaultTimePicker = false },
            onConfirm = {
                defaultTime = it
                showDefaultTimePicker = false
            }
        )
    }
}

@Composable
private fun ReminderPanelSection(
    @StringRes titleRes: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier
                .fillMaxWidth()
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
private fun ReminderSettingRow(
    reminder: Reminder,
    onEditTime: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onEditTime)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = reminderLabel(reminder.minutesBefore),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = reminderTimeLabel(reminder.localTimeMinutes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Switch(checked = reminder.enabled, onCheckedChange = onEnabledChange)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
        }
    }
}

private fun reminderLabel(minutesBefore: Int): String = when {
    minutesBefore < 0 -> {
        val days = -minutesBefore / 1440
        when (days) {
            50 -> "50 天纪念日"
            100 -> "100 天纪念日"
            365 -> "一年纪念日"
            else -> "纪念日后 $days 天"
        }
    }
    minutesBefore == 0 -> "当天"
    minutesBefore == 1440 -> "提前 1 天"
    minutesBefore == 4320 -> "提前 3 天"
    minutesBefore == 10080 -> "提前 7 天"
    else -> if (minutesBefore % 1440 == 0) {
        "提前 ${minutesBefore / 1440} 天"
    } else {
        "提前 $minutesBefore 分钟"
    }
}

private fun reminderTimeLabel(localTimeMinutes: Int): String {
    val hour = (localTimeMinutes / 60).toString().padStart(2, '0')
    val minute = (localTimeMinutes % 60).toString().padStart(2, '0')
    return "$hour:$minute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
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
                horizontalAlignment = CenterHorizontally
            ) {
                Text(stringResource(R.string.select_time), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                TimePicker(state = timeState)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = {
                        onConfirm(timeState.hour * 60 + timeState.minute)
                    }) { Text(stringResource(R.string.confirm)) }
                }
            }
        }
    }
}

@Composable
private fun AttachmentGallery(
    attachments: List<Attachment>,
    viewModel: DetailViewModel,
    onOpen: (Attachment) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(attachments, key = { it.id }) { attachment ->
            AttachmentGalleryCard(
                attachment = attachment,
                viewModel = viewModel,
                onOpen = { onOpen(attachment) }
            )
        }
    }
}

@Composable
private fun AttachmentGalleryCard(
    attachment: Attachment,
    viewModel: DetailViewModel,
    onOpen: () -> Unit
) {
    val isImage = attachment.mimeType.startsWith("image/")
    val file = remember(attachment.id) { viewModel.fileFor(attachment) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .width(200.dp)
            .clickable(role = Role.Button, onClick = onOpen)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (isImage && file != null) {
                    ImageThumbnail(file)
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = attachment.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatBytes(attachment.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnail(file: File) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, file.path) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            decodeOrientedBitmap(file, inSampleSize = 4)?.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}


