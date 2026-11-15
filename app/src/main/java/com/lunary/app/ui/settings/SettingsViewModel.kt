package com.lunary.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.settings.AppSettings
import com.lunary.app.data.lock.AppLockManager
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.data.settings.ThemeMode
import com.lunary.app.data.settings.WeekStart
import com.lunary.app.domain.model.LeapDayPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val appLockManager: AppLockManager
) : ViewModel() {

    val lockState = appLockManager.state

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repository.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        repository.setDynamicColor(enabled)
    }

    fun setWeekStart(start: WeekStart) = viewModelScope.launch {
        repository.setWeekStart(start)
    }

    fun setLeapDayPolicy(policy: LeapDayPolicy) = viewModelScope.launch {
        repository.setLeapDayPolicy(policy)
    }

    fun enableLock(pin: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(appLockManager.setPin(pin))
    }

    fun disableLock(pin: String, onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(appLockManager.disableLock(pin))
    }

    fun changePin(current: String, newPin: String, onResult: (Boolean) -> Unit) =
        viewModelScope.launch {
            onResult(appLockManager.changePin(current, newPin))
        }

    fun setLockTimeoutMinutes(minutes: Int) = viewModelScope.launch {
        repository.setLockTimeoutMinutes(minutes)
    }

    fun setBiometricUnlock(enabled: Boolean) = viewModelScope.launch {
        repository.setBiometricUnlock(enabled)
    }

    fun setHideNotificationContent(enabled: Boolean) = viewModelScope.launch {
        repository.setHideNotificationContent(enabled)
    }
}
