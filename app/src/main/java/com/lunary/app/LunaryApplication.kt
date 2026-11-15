package com.lunary.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import androidx.work.WorkManager
import com.lunary.app.data.backup.AutoBackupScheduler
import com.lunary.app.data.backup.AutoBackupWorker
import org.osmdroid.config.Configuration
import java.io.File
import com.lunary.app.data.reminder.ReminderReceiver
import com.lunary.app.data.attachment.AttachmentStore
import com.lunary.app.data.reminder.ReminderScheduler
import com.lunary.app.domain.repository.EventRepository
import com.lunary.app.domain.repository.LetterRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LunaryApplication : Application() {

    @Inject
    lateinit var eventRepository: EventRepository

    @Inject
    lateinit var letterRepository: LetterRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var attachmentStore: AttachmentStore

    @Inject
    lateinit var autoBackupScheduler: AutoBackupScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        WorkManager.initialize(
            this,
            WorkConfiguration.Builder().setWorkerFactory(workerFactory).build()
        )
        ReminderReceiver.createChannel(this)
        createAutoBackupChannel()
        applicationScope.launch {
            // Housekeeping: recycle-bin entries live for 30 days.
            eventRepository.purgeExpiredTrash()
            letterRepository.purgeExpiredTrash()
            reminderScheduler.rescheduleAll()
            autoBackupScheduler.scheduleNext()
        }
    }

    private fun createAutoBackupChannel() {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                AutoBackupWorker.CHANNEL,
                "自动备份",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
        )
    }
}
