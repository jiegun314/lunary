package com.lunary.app.data.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lunary.app.R
import com.lunary.app.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
    private val secretStore: AutoBackupSecretStore,
    private val scheduler: AutoBackupScheduler,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.autoBackupSettings.first()
        val force = inputData.getBoolean("force", false)
        if (!settings.enabled && !force) return Result.success()
        val passphrase = secretStore.load() ?: run {
            settingsRepository.setAutoBackupResult(clock.millis(), "未设置自动备份密码", null)
            return Result.failure()
        }
        return try {
            runCatching { setForeground(createForegroundInfo()) }
            val tree = settings.treeUri?.let { DocumentFile.fromTreeUri(applicationContext, Uri.parse(it)) }
                ?: error("未选择自动备份文件夹")
            if (!tree.canWrite()) error("自动备份文件夹不可写，请重新选择")
            val name = "lunary-auto-" + ZonedDateTime.now(clock.withZone(ZoneId.systemDefault()))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".lunary"
            val document = tree.createFile("application/octet-stream", name)
                ?: error("无法创建备份文件")
            backupManager.writeEncrypted(document.uri, passphrase)
            prune(tree)
            settingsRepository.setAutoBackupResult(clock.millis(), null, document.uri.toString())
            Result.success()
        } catch (e: Exception) {
            settingsRepository.setAutoBackupResult(
                clock.millis(),
                e.message ?: "自动备份失败",
                null
            )
            Result.failure()
        } finally {
            passphrase.fill('\u0000')
            runCatching { scheduler.scheduleNext() }
        }
    }

    private fun prune(tree: DocumentFile) {
        tree.listFiles()
            .filter { it.name?.startsWith("lunary-auto-") == true }
            .sortedByDescending { it.lastModified() }
            .drop(14)
            .forEach { runCatching { it.delete() } }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "自动备份",
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("正在备份月念数据")
            .setContentText("大型附件可能需要一些时间")
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        return if (android.os.Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1001, notification)
        }
    }

    companion object {
        const val CHANNEL = "auto_backup"
    }
}
