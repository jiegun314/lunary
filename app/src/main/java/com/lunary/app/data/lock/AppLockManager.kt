package com.lunary.app.data.lock

import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import com.lunary.app.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppLockState(
    val initializing: Boolean = true,
    val enabled: Boolean = false,
    val locked: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val timeoutMinutes: Int = 1
)

@Singleton
class AppLockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = _state.asStateFlow()

    private var loaded = false
    private var backgroundAt = 0L
    private var failedAttempts = 0
    private var blockedUntil = 0L

    init {
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                val hasPin = settingsRepository.lockPinBlob() != null
                val enabled = settings.lockEnabled && hasPin
                val biometricAvailable = BiometricManager.from(context)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                    BiometricManager.BIOMETRIC_SUCCESS
                _state.update { current ->
                    val locked = when {
                        !enabled -> false
                        !loaded -> true
                        else -> current.locked
                    }
                    current.copy(
                        initializing = false,
                        enabled = enabled,
                        locked = locked,
                        biometricEnabled = enabled && settings.biometricUnlock,
                        biometricAvailable = biometricAvailable,
                        timeoutMinutes = settings.lockTimeoutMinutes
                    )
                }
                loaded = true
            }
        }
    }

    suspend fun setPin(pin: String): Boolean {
        if (!PIN_PATTERN.matches(pin)) return false
        val salt = LockCrypto.randomSalt()
        val verifier = derive(pin, salt)
        val blob = runCatching {
            LockCrypto.encrypt(salt + verifier)
        }.getOrNull() ?: return false
        settingsRepository.setLockPinBlob(blob)
        settingsRepository.setLockEnabled(true)
        failedAttempts = 0
        blockedUntil = 0
        _state.update { it.copy(enabled = true, locked = false) }
        return true
    }

    suspend fun verifyPin(pin: String): Boolean {
        if (SystemClock.elapsedRealtime() < blockedUntil) return false
        val blob = settingsRepository.lockPinBlob() ?: return false
        val payload = runCatching { LockCrypto.decrypt(blob) }.getOrNull() ?: return false
        if (payload.size <= 16) return false
        val salt = payload.copyOfRange(0, 16)
        val expected = payload.copyOfRange(16, payload.size)
        val actual = derive(pin, salt)
        return if (MessageDigest.isEqual(expected, actual)) {
            failedAttempts = 0
            blockedUntil = 0
            _state.update { it.copy(locked = false) }
            true
        } else {
            failedAttempts++
            val waitMs = min(30_000L, 1_000L shl min(failedAttempts, 5))
            blockedUntil = SystemClock.elapsedRealtime() + waitMs
            false
        }
    }

    suspend fun disableLock(pin: String): Boolean {
        if (!verifyPin(pin)) return false
        settingsRepository.setLockEnabled(false)
        settingsRepository.setLockPinBlob(null)
        settingsRepository.setBiometricUnlock(false)
        _state.update { it.copy(enabled = false, locked = false, biometricEnabled = false) }
        return true
    }

    suspend fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        return setPin(newPin)
    }

    fun unlockedWithBiometric() {
        val current = _state.value
        if (!current.enabled || !current.biometricEnabled) return
        failedAttempts = 0
        blockedUntil = 0
        _state.update { it.copy(locked = false) }
    }

    fun onBackground(changingConfigurations: Boolean = false) {
        if (changingConfigurations) return
        val current = _state.value
        if (!current.enabled || current.locked || current.initializing) return
        backgroundAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        val current = _state.value
        if (!current.enabled || current.locked || current.initializing) {
            backgroundAt = 0
            return
        }
        if (backgroundAt > 0L) {
            val elapsed = SystemClock.elapsedRealtime() - backgroundAt
            val timeoutMs = current.timeoutMinutes.coerceAtLeast(0) * 60_000L
            if (timeoutMs == 0L || elapsed >= timeoutMs) {
                _state.update { it.copy(locked = true) }
            }
        }
        backgroundAt = 0
    }

    fun lockNow() {
        if (_state.value.enabled) _state.update { it.copy(locked = true) }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val PBKDF2_ITERATIONS = 210_000
        private val PIN_PATTERN = Regex("\\d{6,12}")
    }
}
