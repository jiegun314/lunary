package com.lunary.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lunary.app.domain.model.LeapDayPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "lunary_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class WeekStart { SYSTEM, MONDAY, SUNDAY }

enum class AutoBackupFrequency { DAILY, WEEKLY, MONTHLY }

data class AutoBackupSettings(
    val enabled: Boolean = false,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.DAILY,
    val hour: Int = 3,
    val minute: Int = 0,
    val treeUri: String? = null,
    val lastRunAt: Long? = null,
    val lastError: String? = null,
    val lastUri: String? = null
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val weekStart: WeekStart = WeekStart.SYSTEM,
    val leapDayPolicy: LeapDayPolicy = LeapDayPolicy.FEBRUARY_28,
    val lockEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 1,
    val biometricUnlock: Boolean = false,
    val hideNotificationContent: Boolean = true
) {
    fun firstDayOfWeek(locale: java.util.Locale = java.util.Locale.SIMPLIFIED_CHINESE): DayOfWeek =
        when (weekStart) {
            WeekStart.MONDAY -> DayOfWeek.MONDAY
            WeekStart.SUNDAY -> DayOfWeek.SUNDAY
            WeekStart.SYSTEM -> java.time.temporal.WeekFields.of(locale).firstDayOfWeek
        }
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val weekStart = stringPreferencesKey("week_start")
        val leapPolicy = stringPreferencesKey("leap_day_policy")
        val lockEnabled = booleanPreferencesKey("lock_enabled")
        val lockPinBlob = stringPreferencesKey("lock_pin_blob")
        val lockTimeoutMinutes = intPreferencesKey("lock_timeout_minutes")
        val biometricUnlock = booleanPreferencesKey("biometric_unlock")
        val hideNotificationContent = booleanPreferencesKey("hide_notification_content")
        val autoEnabled = booleanPreferencesKey("auto_backup_enabled")
        val autoFrequency = stringPreferencesKey("auto_backup_frequency")
        val autoHour = intPreferencesKey("auto_backup_hour")
        val autoMinute = intPreferencesKey("auto_backup_minute")
        val autoTreeUri = stringPreferencesKey("auto_backup_tree_uri")
        val autoLastRunAt = stringPreferencesKey("auto_backup_last_run_at")
        val autoLastError = stringPreferencesKey("auto_backup_last_error")
        val autoLastUri = stringPreferencesKey("auto_backup_last_uri")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.dynamicColor] ?: true,
            weekStart = prefs[Keys.weekStart]?.let { runCatching { WeekStart.valueOf(it) }.getOrNull() }
                ?: WeekStart.SYSTEM,
            leapDayPolicy = prefs[Keys.leapPolicy]
                ?.let { runCatching { LeapDayPolicy.valueOf(it) }.getOrNull() }
                ?: LeapDayPolicy.FEBRUARY_28,
            lockEnabled = prefs[Keys.lockEnabled] ?: false,
            lockTimeoutMinutes = prefs[Keys.lockTimeoutMinutes] ?: 1,
            biometricUnlock = prefs[Keys.biometricUnlock] ?: false,
            hideNotificationContent = prefs[Keys.hideNotificationContent] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }

    val autoBackupSettings: Flow<AutoBackupSettings> = context.settingsDataStore.data.map { prefs ->
        AutoBackupSettings(
            enabled = prefs[Keys.autoEnabled] ?: false,
            frequency = prefs[Keys.autoFrequency]
                ?.let { runCatching { AutoBackupFrequency.valueOf(it) }.getOrNull() }
                ?: AutoBackupFrequency.DAILY,
            hour = prefs[Keys.autoHour] ?: 3,
            minute = prefs[Keys.autoMinute] ?: 0,
            treeUri = prefs[Keys.autoTreeUri],
            lastRunAt = prefs[Keys.autoLastRunAt]?.toLongOrNull(),
            lastError = prefs[Keys.autoLastError],
            lastUri = prefs[Keys.autoLastUri]
        )
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) = edit { it[Keys.autoEnabled] = enabled }

    suspend fun setAutoBackupFrequency(frequency: AutoBackupFrequency) =
        edit { it[Keys.autoFrequency] = frequency.name }

    suspend fun setAutoBackupTime(hour: Int, minute: Int) = edit { prefs ->
        prefs[Keys.autoHour] = hour
        prefs[Keys.autoMinute] = minute
    }

    suspend fun setAutoBackupTreeUri(uri: String?) = edit { prefs ->
        if (uri == null) prefs.remove(Keys.autoTreeUri) else prefs[Keys.autoTreeUri] = uri
    }

    suspend fun setAutoBackupResult(lastRunAt: Long?, lastError: String?, lastUri: String?) = edit { prefs ->
        if (lastRunAt == null) prefs.remove(Keys.autoLastRunAt) else prefs[Keys.autoLastRunAt] = lastRunAt.toString()
        if (lastError == null) prefs.remove(Keys.autoLastError) else prefs[Keys.autoLastError] = lastError
        if (lastUri == null) prefs.remove(Keys.autoLastUri) else prefs[Keys.autoLastUri] = lastUri
    }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.dynamicColor] = enabled }

    suspend fun setWeekStart(start: WeekStart) = edit { it[Keys.weekStart] = start.name }

    suspend fun setLeapDayPolicy(policy: LeapDayPolicy) = edit { it[Keys.leapPolicy] = policy.name }

    suspend fun setLockEnabled(enabled: Boolean) = edit { it[Keys.lockEnabled] = enabled }

    suspend fun lockPinBlob(): String? =
        context.settingsDataStore.data.map { it[Keys.lockPinBlob] }.firstOrNull()

    suspend fun setLockPinBlob(blob: String?) = edit { prefs ->
        if (blob == null) prefs.remove(Keys.lockPinBlob) else prefs[Keys.lockPinBlob] = blob
    }

    suspend fun setLockTimeoutMinutes(minutes: Int) =
        edit { it[Keys.lockTimeoutMinutes] = minutes }

    suspend fun setBiometricUnlock(enabled: Boolean) =
        edit { it[Keys.biometricUnlock] = enabled }

    suspend fun setHideNotificationContent(enabled: Boolean) =
        edit { it[Keys.hideNotificationContent] = enabled }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit { prefs -> block(prefs) }
    }
}
