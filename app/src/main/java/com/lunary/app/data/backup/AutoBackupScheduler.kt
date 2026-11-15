package com.lunary.app.data.backup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lunary.app.data.settings.AutoBackupFrequency
import com.lunary.app.data.settings.AutoBackupSettings
import com.lunary.app.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class AutoBackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) {
    private val workManager get() = WorkManager.getInstance(context)

    suspend fun scheduleNext() {
        val settings = settingsRepository.autoBackupSettings.first()
        if (!settings.enabled || settings.treeUri == null) {
            cancel()
            return
        }
        val delay = delayUntilNext(settings, clock)
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
            .setInputData(androidx.work.workDataOf("force" to true))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(WORK)
    }

    companion object {
        const val WORK = "lunary_auto_backup"
        const val WORK_NOW = "lunary_auto_backup_now"
        const val TAG = "auto_backup"

        fun delayUntilNext(settings: AutoBackupSettings, clock: Clock): Long {
            val now = ZonedDateTime.now(clock.withZone(java.time.ZoneId.systemDefault()))
            var target = now.toLocalDate()
                .atTime(settings.hour, settings.minute)
                .atZone(now.zone)
            while (!target.isAfter(now)) {
                target = when (settings.frequency) {
                    AutoBackupFrequency.DAILY -> target.plusDays(1)
                    AutoBackupFrequency.WEEKLY -> target.plusWeeks(1)
                    AutoBackupFrequency.MONTHLY -> target.plusMonths(1)
                }
            }
            return java.time.Duration.between(now, target).toMillis()
        }
    }
}
