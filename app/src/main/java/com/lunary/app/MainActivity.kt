package com.lunary.app

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lunary.app.data.settings.AppSettings
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.data.settings.ThemeMode
import com.lunary.app.data.lock.AppLockManager
import com.lunary.app.ui.LunaryRoot
import com.lunary.app.ui.theme.LunaryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var lockManager: AppLockManager

    private val pendingEventId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingEventId.value = intent?.getStringExtra(EXTRA_EVENT_ID)

        setContent {
            val settings by settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val systemDark = isSystemInDarkTheme()
            val darkTheme = remember(settings.themeMode, systemDark) {
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
            }
            val deepLinkEventId by pendingEventId.collectAsStateWithLifecycle()
            LunaryTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                LunaryRoot(
                    lockManager = lockManager,
                    initialEventId = deepLinkEventId,
                    onInitialEventConsumed = { pendingEventId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingEventId.value = intent.getStringExtra(EXTRA_EVENT_ID)
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }
}
