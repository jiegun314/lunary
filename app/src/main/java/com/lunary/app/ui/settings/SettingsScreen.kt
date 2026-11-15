package com.lunary.app.ui.settings

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.R
import com.lunary.app.data.lock.AppLockState
import com.lunary.app.data.settings.AppSettings
import com.lunary.app.data.settings.ThemeMode
import com.lunary.app.data.settings.WeekStart
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.ui.components.BrandLockup

private enum class SettingsGroup(@StringRes val titleRes: Int) {
    APPEARANCE(R.string.settings_appearance),
    CALENDAR(R.string.settings_calendar),
    REMINDERS(R.string.settings_reminders),
    PRIVACY(R.string.settings_privacy),
    DATA(R.string.settings_data_backup),
    TAXONOMY(R.string.settings_taxonomy),
    ABOUT(R.string.settings_help_about)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenTrash: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAutoBackup: () -> Unit,
    onManageCategories: () -> Unit,
    onManageTags: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lock by viewModel.lockState.collectAsStateWithLifecycle()
    var showPinSetup by rememberSaveable { mutableStateOf(false) }
    var showPinDisable by rememberSaveable { mutableStateOf(false) }
    var showPinChange by rememberSaveable { mutableStateOf(false) }
    var selectedGroup by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.0"
    }
    val alarmManager = remember(context) { context.getSystemService(AlarmManager::class.java) }
    var exactAlarmAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 31 || alarmManager?.canScheduleExactAlarms() == true
        )
    }
    LifecycleResumeEffect(Unit) {
        exactAlarmAllowed = Build.VERSION.SDK_INT < 31 ||
            alarmManager?.canScheduleExactAlarms() == true
        onPauseOrDispose { }
    }
    val onExactAlarmClick = {
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }.onFailure {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp && maxHeight >= 480.dp
        val selectedGroupEntry = SettingsGroup.entries[selectedGroup]
        val backAction = onBack
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    navigationIcon = {
                        if (!expanded && backAction != null) {
                            IconButton(onClick = backAction) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            if (expanded) {
                Row(Modifier.fillMaxSize().padding(padding)) {
                    SettingsCategoryNav(
                        groups = SettingsGroup.entries,
                        selectedIndex = selectedGroup,
                        onSelect = { selectedGroup = it },
                        modifier = Modifier.width(232.dp).fillMaxHeight()
                    )
                    VerticalDivider()
                    Box(
                        Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            state = rememberLazyListState(),
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 720.dp)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            item(key = selectedGroupEntry.name) {
                                SettingsGroupContent(
                                    group = selectedGroupEntry,
                                    showHeader = true,
                                    settings = settings,
                                    lock = lock,
                                    exactAlarmAllowed = exactAlarmAllowed,
                                    versionName = versionName,
                                    viewModel = viewModel,
                                    onExactAlarmClick = onExactAlarmClick,
                                    onOpenBackup = onOpenBackup,
                                    onOpenAutoBackup = onOpenAutoBackup,
                                    onOpenTrash = onOpenTrash,
                                    onManageCategories = onManageCategories,
                                    onManageTags = onManageTags,
                                    onRequestEnableLock = { showPinSetup = true },
                                    onRequestDisableLock = { showPinDisable = true },
                                    onRequestChangePin = { showPinChange = true }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 680.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 32.dp
                        )
                    ) {
                        SettingsGroup.entries.forEach { group ->
                            item(key = group.name) {
                                SettingsGroupContent(
                                    group = group,
                                    showHeader = true,
                                    settings = settings,
                                    lock = lock,
                                    exactAlarmAllowed = exactAlarmAllowed,
                                    versionName = versionName,
                                    viewModel = viewModel,
                                    onExactAlarmClick = onExactAlarmClick,
                                    onOpenBackup = onOpenBackup,
                                    onOpenAutoBackup = onOpenAutoBackup,
                                    onOpenTrash = onOpenTrash,
                                    onManageCategories = onManageCategories,
                                    onManageTags = onManageTags,
                                    onRequestEnableLock = { showPinSetup = true },
                                    onRequestDisableLock = { showPinDisable = true },
                                    onRequestChangePin = { showPinChange = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onSave = { pin, onResult -> viewModel.enableLock(pin, onResult) }
        )
    }
    if (showPinDisable) {
        VerifyPinDialog(
            title = stringResource(R.string.lock_disable_title),
            onDismiss = { showPinDisable = false },
            onVerify = { pin, onResult -> viewModel.disableLock(pin, onResult) }
        )
    }
    if (showPinChange) {
        ChangePinDialog(
            onDismiss = { showPinChange = false },
            onChange = { current, newPin, onResult ->
                viewModel.changePin(current, newPin, onResult)
            }
        )
    }
}

@Composable
private fun SettingsCategoryNav(
    groups: List<SettingsGroup>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_sections),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, end = 16.dp, bottom = 8.dp)
            )
        }
        itemsIndexed(groups, key = { _, group -> group.name }) { index, group ->
            NavigationDrawerItem(
                label = { Text(stringResource(group.titleRes)) },
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingsGroupContent(
    group: SettingsGroup,
    showHeader: Boolean,
    settings: AppSettings,
    lock: AppLockState,
    exactAlarmAllowed: Boolean,
    versionName: String,
    viewModel: SettingsViewModel,
    onExactAlarmClick: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAutoBackup: () -> Unit,
    onOpenTrash: () -> Unit,
    onManageCategories: () -> Unit,
    onManageTags: () -> Unit,
    onRequestEnableLock: () -> Unit,
    onRequestDisableLock: () -> Unit,
    onRequestChangePin: () -> Unit
) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    when (group) {
        SettingsGroup.APPEARANCE -> SettingsSection(
            R.string.settings_appearance,
            showHeader = showHeader
        ) {
            SettingsChoiceRow(
                title = stringResource(R.string.theme_mode),
                description = stringResource(R.string.settings_theme_desc),
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark)
                ),
                selected = settings.themeMode,
                onSelect = viewModel::setThemeMode
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.dynamic_color),
                description = stringResource(R.string.dynamic_color_hint),
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor
            )
        }

        SettingsGroup.CALENDAR -> SettingsSection(
            R.string.settings_calendar,
            showHeader = showHeader
        ) {
            SettingsChoiceRow(
                title = stringResource(R.string.week_start),
                description = stringResource(R.string.settings_week_start_desc),
                options = listOf(
                    WeekStart.SYSTEM to stringResource(R.string.week_system),
                    WeekStart.MONDAY to stringResource(R.string.week_monday),
                    WeekStart.SUNDAY to stringResource(R.string.week_sunday)
                ),
                selected = settings.weekStart,
                onSelect = viewModel::setWeekStart
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = stringResource(R.string.leap_day_policy),
                description = stringResource(R.string.settings_leap_day_desc),
                options = listOf(
                    LeapDayPolicy.FEBRUARY_28 to stringResource(R.string.leap_feb28),
                    LeapDayPolicy.MARCH_1 to stringResource(R.string.leap_mar1)
                ),
                selected = settings.leapDayPolicy,
                onSelect = viewModel::setLeapDayPolicy
            )
        }

        SettingsGroup.REMINDERS -> SettingsSection(
            R.string.settings_reminders,
            showHeader = showHeader
        ) {
            val status = when {
                Build.VERSION.SDK_INT < 31 -> stringResource(R.string.exact_alarm_status_not_required)
                exactAlarmAllowed -> stringResource(R.string.exact_alarm_status_granted)
                else -> stringResource(R.string.exact_alarm_status_denied)
            }
            val description = when {
                Build.VERSION.SDK_INT < 31 -> stringResource(R.string.exact_alarm_desc_not_required)
                exactAlarmAllowed -> stringResource(R.string.exact_alarm_desc_granted)
                else -> stringResource(R.string.exact_alarm_desc_denied)
            }
            SettingsNavigationRow(
                title = stringResource(R.string.settings_exact_alarm),
                description = description,
                trailingText = status,
                onClick = onExactAlarmClick
            )
        }

        SettingsGroup.PRIVACY -> SettingsSection(
            R.string.settings_privacy,
            showHeader = showHeader
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.app_lock),
                description = stringResource(R.string.app_lock_description),
                checked = lock.enabled,
                onCheckedChange = { enabled ->
                    if (enabled) onRequestEnableLock() else onRequestDisableLock()
                }
            )
            if (lock.enabled) {
                SettingsDivider()
                SettingsNavigationRow(
                    title = stringResource(R.string.lock_change_pin),
                    onClick = onRequestChangePin
                )
                SettingsDivider()
                SettingsChoiceRow(
                    title = stringResource(R.string.auto_lock),
                    options = listOf(
                        0 to stringResource(R.string.auto_lock_immediate),
                        1 to stringResource(R.string.auto_lock_1),
                        5 to stringResource(R.string.auto_lock_5),
                        15 to stringResource(R.string.auto_lock_15)
                    ),
                    selected = lock.timeoutMinutes,
                    onSelect = viewModel::setLockTimeoutMinutes
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.biometric_unlock),
                    description = if (!lock.biometricAvailable) {
                        stringResource(R.string.biometric_unavailable)
                    } else null,
                    checked = lock.biometricEnabled && lock.biometricAvailable,
                    enabled = lock.biometricAvailable,
                    onCheckedChange = viewModel::setBiometricUnlock
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.hide_notification_content),
                    description = stringResource(R.string.hide_notification_content_desc),
                    checked = settings.hideNotificationContent,
                    onCheckedChange = viewModel::setHideNotificationContent
                )
            }
        }

        SettingsGroup.DATA -> SettingsSection(
            R.string.settings_data_backup,
            showHeader = showHeader
        ) {
            SettingsNavigationRow(
                title = stringResource(R.string.settings_backup_restore),
                description = stringResource(R.string.settings_backup_restore_desc),
                onClick = onOpenBackup
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.auto_backup_title),
                description = stringResource(R.string.settings_auto_backup_desc),
                onClick = onOpenAutoBackup
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.settings_trash),
                description = stringResource(R.string.settings_trash_desc),
                onClick = onOpenTrash
            )
        }

        SettingsGroup.TAXONOMY -> SettingsSection(
            R.string.settings_taxonomy,
            showHeader = showHeader
        ) {
            SettingsNavigationRow(
                title = stringResource(R.string.manage_categories),
                description = stringResource(R.string.settings_categories_desc),
                onClick = onManageCategories
            )
            SettingsDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.manage_tags),
                description = stringResource(R.string.settings_tags_desc),
                onClick = onManageTags
            )
        }

        SettingsGroup.ABOUT -> {
            SettingsSection(
                R.string.settings_help_about,
                showHeader = showHeader
            ) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_help),
                    description = stringResource(R.string.settings_help_desc),
                    onClick = { showHelp = true }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_privacy_policy),
                    description = stringResource(R.string.settings_privacy_desc),
                    onClick = { showPrivacy = true }
                )
                SettingsDivider()
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    BrandLockup()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.version_label, versionName),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.offline_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showHelp) {
                AlertDialog(
                    onDismissRequest = { showHelp = false },
                    title = { Text(stringResource(R.string.settings_help)) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(stringResource(R.string.settings_help_content))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHelp = false }) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                )
            }
            if (showPrivacy) {
                AlertDialog(
                    onDismissRequest = { showPrivacy = false },
                    title = { Text(stringResource(R.string.settings_privacy_policy)) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Text(stringResource(R.string.settings_privacy_content))
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPrivacy = false }) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    @StringRes titleRes: Int,
    showHeader: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (showHeader) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
                    .semantics { heading() }
            )
        }
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun <T> SettingsChoiceRow(
    title: String,
    description: String? = null,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = description?.let { text -> { Text(text) } },
            colors = settingsItemColors()
        )
        Column(Modifier.selectableGroup()) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelect(value) }
                        )
                        .semantics(mergeDescendants = true) { }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { text -> { Text(text) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics { }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .semantics(mergeDescendants = true) { },
        colors = settingsItemColors()
    )
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    description: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { text -> { Text(text) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingText != null) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { },
        colors = settingsItemColors()
    )
}

@Composable
private fun settingsItemColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer
)

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String, (Boolean) -> Unit) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_setup_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PinField(stringResource(R.string.pin), pin) { pin = it.filter(Char::isDigit).take(6) }
                Spacer(Modifier.height(8.dp))
                PinField(stringResource(R.string.lock_confirm_pin), confirm) {
                    confirm = it.filter(Char::isDigit).take(6)
                }
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pin_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length != 6 || confirm.length != 6) {
                    error = true
                } else if (pin != confirm) {
                    error = true
                } else {
                    onSave(pin) { ok -> if (ok) onDismiss() else error = true }
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun VerifyPinDialog(
    title: String,
    onDismiss: () -> Unit,
    onVerify: (String, (Boolean) -> Unit) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PinField(stringResource(R.string.lock_verify_pin), pin) {
                    pin = it.filter(Char::isDigit).take(6)
                }
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pin_wrong),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onVerify(pin) { ok -> if (ok) onDismiss() else error = true }
            }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onChange: (String, String, (Boolean) -> Unit) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_change_pin)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PinField(stringResource(R.string.lock_verify_pin), current) {
                    current = it.filter(Char::isDigit).take(6)
                }
                Spacer(Modifier.height(8.dp))
                PinField(stringResource(R.string.pin), newPin) {
                    newPin = it.filter(Char::isDigit).take(6)
                }
                Spacer(Modifier.height(8.dp))
                PinField(stringResource(R.string.lock_confirm_pin), confirm) {
                    confirm = it.filter(Char::isDigit).take(6)
                }
                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.pin_mismatch),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newPin.length != 6 || newPin != confirm) {
                    error = true
                } else {
                    onChange(current, newPin) { ok -> if (ok) onDismiss() else error = true }
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun PinField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
    )
}
