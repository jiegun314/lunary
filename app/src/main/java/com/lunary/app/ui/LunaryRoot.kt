package com.lunary.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.LocalActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.data.lock.AppLockManager
import com.lunary.app.ui.lock.LockScreen

@Composable
fun LunaryRoot(
    lockManager: AppLockManager,
    initialEventId: String?,
    onInitialEventConsumed: () -> Unit
) {
    val lock by lockManager.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    LifecycleEventEffect(Lifecycle.Event.ON_START) { lockManager.onForeground() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        lockManager.onBackground(activity?.isChangingConfigurations == true)
    }

    when {
        lock.initializing -> {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        lock.locked -> LockScreen(
            lockManager = lockManager,
            biometricEnabled = lock.biometricEnabled,
            biometricAvailable = lock.biometricAvailable
        )

        else -> LunaryApp(
            initialEventId = initialEventId,
            onInitialEventConsumed = onInitialEventConsumed
        )
    }
}
