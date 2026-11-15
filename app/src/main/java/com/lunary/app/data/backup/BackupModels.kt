package com.lunary.app.data.backup

import com.lunary.app.data.local.entity.AttachmentEntity
import com.lunary.app.data.local.entity.CategoryEntity
import com.lunary.app.data.local.entity.EventEntity
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.data.local.entity.LetterEntity
import com.lunary.app.data.local.entity.ReminderEntity
import com.lunary.app.data.local.entity.TagEntity
import java.io.File
import java.util.zip.ZipFile

const val BACKUP_SCHEMA_VERSION = 3
const val BACKUP_MAGIC = "LUNARYBK1"

data class BackupManifest(
    val schemaVersion: Int,
    val createdAt: Long,
    val appVersion: String,
    val eventCount: Int,
    val categoryCount: Int,
    val tagCount: Int,
    val reminderCount: Int,
    val attachmentCount: Int,
    val attachmentBytes: Long,
    val dataSha256: String,
    val files: List<BackupFileEntry>,
    val settings: BackupSettings? = null,
    val letterCount: Int = 0
)

data class BackupSettings(
    val themeMode: String,
    val dynamicColor: Boolean,
    val weekStart: String,
    val leapDayPolicy: String,
    val hideNotificationContent: Boolean,
    val lockTimeoutMinutes: Int
)

data class BackupFileEntry(
    val path: String,
    val size: Long,
    val sha256: String
)

data class SnapshotData(
    val events: List<EventEntity>,
    val categories: List<CategoryEntity>,
    val tags: List<TagEntity>,
    val eventTags: List<EventTagCrossRef>,
    val reminders: List<ReminderEntity>,
    val attachments: List<AttachmentEntity>,
    val letters: List<LetterEntity> = emptyList()
)

data class BackupArchive(
    val manifest: BackupManifest,
    val data: SnapshotData,
    val attachmentFiles: Map<String, ByteArray> = emptyMap(),
    val zipFile: File? = null
) {
    fun hasAttachment(relativePath: String): Boolean =
        attachmentFiles.containsKey(relativePath) ||
            zipFile?.let { zip ->
                ZipFile(zip).use { it.getEntry("attachments/$relativePath") != null }
            } ?: false

    fun writeAttachmentTo(relativePath: String, target: java.io.File): Boolean {
        target.parentFile?.mkdirs()
        attachmentFiles[relativePath]?.let { bytes ->
            target.writeBytes(bytes)
            return true
        }
        val zip = zipFile ?: return false
        return ZipFile(zip).use { zf ->
            val entry = zf.getEntry("attachments/$relativePath") ?: return false
            zf.getInputStream(entry).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            true
        }
    }

    fun close() {
        zipFile?.delete()
    }
    val eventCount: Int get() = data.events.size
    val categoryCount: Int get() = data.categories.size
    val tagCount: Int get() = data.tags.size
    val reminderCount: Int get() = data.reminders.size
    val attachmentCount: Int get() = data.attachments.size
    val letterCount: Int get() = data.letters.size
}

data class RestoreResult(
    val events: Int,
    val categories: Int,
    val tags: Int,
    val reminders: Int,
    val attachments: Int,
    val autoBackupName: String,
    val letters: Int = 0
)
