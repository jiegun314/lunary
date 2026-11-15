package com.lunary.app.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lunary.app.data.backup.BackupArchive
import com.lunary.app.data.backup.BackupManager
import com.lunary.app.data.backup.RestoreMode
import com.lunary.app.data.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val preview: BackupArchive? = null,
    val restored: RestoreResult? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private var pendingPassphrase: CharArray? = null

    fun export(uri: Uri, passphrase: CharArray) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            runCatching { backupManager.writeEncrypted(uri, passphrase) }
                .onSuccess { _state.update { it.copy(busy = false, message = "备份已导出") } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.friendlyMessage()) } }
        }
    }

    fun inspect(uri: Uri, passphrase: CharArray) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            runCatching { backupManager.readEncrypted(uri, passphrase) }
                .onSuccess { archive ->
                    pendingPassphrase = passphrase
                    _state.update { it.copy(busy = false, preview = archive) }
                }
                .onFailure { e ->
                    pendingPassphrase?.fill('\u0000')
                    pendingPassphrase = null
                    _state.update { it.copy(busy = false, error = e.friendlyMessage()) }
                }
        }
    }

    fun confirmRestore(mode: RestoreMode) {
        val archive = _state.value.preview ?: return
        val passphrase = pendingPassphrase ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            runCatching { backupManager.restore(archive, passphrase, mode) }
                .onSuccess { result ->
                    passphrase.fill('\u0000')
                    pendingPassphrase = null
                    archive.close()
                    _state.update {
                        it.copy(busy = false, preview = null, restored = result, message = "恢复完成")
                    }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.friendlyMessage()) } }
        }
    }

    fun dismissPreview() {
        pendingPassphrase?.fill('\u0000')
        pendingPassphrase = null
        _state.value.preview?.close()
        _state.update { it.copy(preview = null) }
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    fun clearRestored() = _state.update { it.copy(restored = null) }

    private fun Throwable.friendlyMessage(): String = when (this) {
        is javax.crypto.AEADBadTagException -> "密码错误或备份文件已损坏"
        else -> message ?: "操作失败"
    }
}
