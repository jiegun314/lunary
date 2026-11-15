package com.lunary.app.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.backup.AutoBackupScheduler
import com.lunary.app.data.backup.AutoBackupSecretStore
import com.lunary.app.data.settings.AutoBackupFrequency
import com.lunary.app.data.settings.AutoBackupSettings
import com.lunary.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AutoBackupViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val secretStore: AutoBackupSecretStore,
    private val scheduler: AutoBackupScheduler
) : ViewModel() {

    val settings: StateFlow<AutoBackupSettings> = repository.autoBackupSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutoBackupSettings()
    )

    private val _hasSecret = MutableStateFlow(secretStore.hasSecret())
    val hasSecret: StateFlow<Boolean> = _hasSecret.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        if (enabled && (!_hasSecret.value || settings.value.treeUri == null)) return
        viewModelScope.launch {
            repository.setAutoBackupEnabled(enabled)
            if (enabled) scheduler.scheduleNext() else scheduler.cancel()
        }
    }

    fun setFrequency(frequency: AutoBackupFrequency) = viewModelScope.launch {
        repository.setAutoBackupFrequency(frequency)
        scheduler.scheduleNext()
    }

    fun setTime(hour: Int, minute: Int) = viewModelScope.launch {
        repository.setAutoBackupTime(hour, minute)
        scheduler.scheduleNext()
    }

    fun setFolder(uri: String) = viewModelScope.launch {
        repository.setAutoBackupTreeUri(uri)
        scheduler.scheduleNext()
    }

    fun savePassword(passphrase: String) {
        secretStore.save(passphrase.toCharArray())
        _hasSecret.value = true
    }

    fun clearPassword() {
        secretStore.clear()
        _hasSecret.value = false
    }

    fun runNow() = scheduler.runNow()

    fun refreshSecret() {
        _hasSecret.value = secretStore.hasSecret()
    }
}
