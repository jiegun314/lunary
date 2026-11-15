package com.lunary.app.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.lunary.app.R
import com.lunary.app.data.lock.AppLockManager
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    lockManager: AppLockManager,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricTitle = stringResource(R.string.lock_biometric_title)
    val biometricSubtitle = stringResource(R.string.lock_biometric_subtitle)
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var authenticating by remember { mutableStateOf(false) }

    fun submit(value: String) {
        if (value.length != PIN_LENGTH) return
        scope.launch {
            val ok = lockManager.verifyPin(value)
            if (!ok) {
                error = true
                pin = ""
            }
        }
    }

    fun authenticate() {
        if (activity == null || authenticating) return
        authenticating = true
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    lockManager.unlockedWithBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                }

                override fun onAuthenticationFailed() {
                    authenticating = false
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(biometricTitle)
            .setSubtitle(biometricSubtitle)
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }

    if (biometricEnabled && biometricAvailable) {
        LaunchedEffect(Unit) { authenticate() }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.lock_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(PIN_LENGTH) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (index < pin.length) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            if (error) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.lock_wrong_pin),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(28.dp))
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 0 until 3) {
                        val number = row * 3 + col + 1
                        if (number <= 9) {
                            PinKey(String.format("%d", number)) {
                                error = false
                                if (pin.length < PIN_LENGTH) pin += number
                                if (pin.length == PIN_LENGTH) submit(pin)
                            }
                        } else if (number == 10) {
                            Spacer(Modifier.size(64.dp))
                            PinKey("0") {
                                error = false
                                if (pin.length < PIN_LENGTH) pin += "0"
                                if (pin.length == PIN_LENGTH) submit(pin)
                            }
                        } else if (number == 11) {
                            IconButton(
                                onClick = { pin = pin.dropLast(1); error = false },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Text(
                                    text = "⌫",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
            }

            if (biometricEnabled && biometricAvailable && activity != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = { authenticate() }) {
                    Text(text = stringResource(R.string.lock_use_biometric))
                }
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(64.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

private const val PIN_LENGTH = 6
