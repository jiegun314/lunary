package com.lunary.app.data.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.lunary.app.data.local.LunaryDatabase
import com.lunary.app.data.local.dao.AttachmentDao
import com.lunary.app.data.local.dao.CategoryDao
import com.lunary.app.data.local.dao.EventDao
import com.lunary.app.data.local.dao.EventTagDao
import com.lunary.app.data.local.dao.LetterDao
import com.lunary.app.data.local.dao.ReminderDao
import com.lunary.app.data.local.dao.TagDao
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.data.local.entity.CategoryEntity
import com.lunary.app.data.local.entity.TagEntity
import com.lunary.app.data.reminder.ReminderScheduler
import com.lunary.app.data.settings.SettingsRepository
import com.lunary.app.domain.model.LeapDayPolicy
import com.lunary.app.data.settings.ThemeMode
import com.lunary.app.data.settings.WeekStart
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

enum class RestoreMode { REPLACE, MERGE }

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LunaryDatabase,
    private val eventDao: EventDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val eventTagDao: EventTagDao,
    private val reminderDao: ReminderDao,
    private val attachmentDao: AttachmentDao,
    private val letterDao: LetterDao,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock
) {
    private val legacyMaxBytes = 60L * 1024 * 1024

    suspend fun writeEncrypted(uri: Uri, passphrase: CharArray) {
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: error("无法写入所选文件")
        writeEncryptedTo(output, passphrase)
    }

    suspend fun writeEncryptedToFile(target: File, passphrase: CharArray) {
        target.parentFile?.mkdirs()
        target.outputStream().use { writeEncryptedTo(it, passphrase) }
    }

    private suspend fun writeEncryptedTo(output: java.io.OutputStream, passphrase: CharArray) {
        val zip = buildZipFile()
        try {
            BackupV2.write(passphrase, zip, output)
        } finally {
            zip.delete()
        }
    }

    suspend fun readEncrypted(uri: Uri, passphrase: CharArray): BackupArchive {
        val input = context.contentResolver.openInputStream(uri) ?: error("无法读取所选文件")
        val header = ByteArray(BackupV2.MAGIC.length)
        val read = input.use { stream ->
            var off = 0
            while (off < header.size) {
                val n = stream.read(header, off, header.size - off)
                if (n <= 0) break
                off += n
            }
            off
        }
        require(read == header.size) { "备份文件不完整" }
        if (!BackupV2.isV2(header)) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("无法读取所选文件")
            require(bytes.size <= legacyMaxBytes) { "旧版备份文件过大" }
            return decodeLegacy(bytes, passphrase)
        }
        val temp = File(context.cacheDir, "backup-read-${clock.millis()}.zip")
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BackupV2.readToFile(passphrase, stream, temp)
            } ?: error("无法读取所选文件")
            return archiveFromZip(temp).also { it.zipFile }
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
    }

    private fun archiveFromZip(zipFile: File): BackupArchive {
        val zf = ZipFile(zipFile)
        try {
            val manifestEntry = zf.getEntry("manifest.json") ?: error("备份缺少 manifest.json")
            val dataEntry = zf.getEntry("data.json") ?: error("备份缺少 data.json")
            val manifest = BackupJson.decodeManifest(zf.getInputStream(manifestEntry).use { it.readBytes() })
            require(manifest.schemaVersion in 1..BACKUP_SCHEMA_VERSION) { "不支持的备份版本" }
            val dataBytes = zf.getInputStream(dataEntry).use { it.readBytes() }
            require(sha256(dataBytes) == manifest.dataSha256) { "备份数据校验失败" }
            val data = BackupJson.decodeSnapshot(dataBytes)
            require(data.events.size == manifest.eventCount) { "事项数量不一致" }
            require(data.categories.size == manifest.categoryCount) { "分类数量不一致" }
            require(data.tags.size == manifest.tagCount) { "标签数量不一致" }
            require(data.reminders.size == manifest.reminderCount) { "提醒数量不一致" }
            require(data.letters.size == manifest.letterCount) { "芳札数量不一致" }
            var total = 0L
            manifest.files.forEach { file ->
                require(!file.path.contains("..")) { "备份包含非法路径" }
                val entry = zf.getEntry("attachments/${file.path}")
                    ?: error("附件文件缺失：${file.path}")
                var size = 0L
                val digest = MessageDigest.getInstance("SHA-256")
                zf.getInputStream(entry).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        size += n
                        digest.update(buffer, 0, n)
                    }
                }
                require(size == file.size) { "附件大小不一致：${file.path}" }
                require(Base64.encodeToString(digest.digest(), Base64.NO_WRAP) == file.sha256) {
                    "附件校验失败：${file.path}"
                }
                total += size
            }
            require(manifest.files.size == manifest.attachmentCount) { "附件清单不完整" }
            require(total == manifest.attachmentBytes) { "附件总大小不一致" }
            return BackupArchive(manifest, data, zipFile = zipFile)
        } finally {
            zf.close()
        }
    }

    private fun decodeLegacy(bytes: ByteArray, passphrase: CharArray): BackupArchive {
        val zipBytes = BackupCrypto.decrypt(passphrase, bytes)
        val entries = readLegacyZip(zipBytes)
        val manifestBytes = entries["manifest.json"] ?: error("备份缺少 manifest.json")
        val dataBytes = entries["data.json"] ?: error("备份缺少 data.json")
        val manifest = BackupJson.decodeManifest(manifestBytes)
        require(manifest.schemaVersion in 1..BACKUP_SCHEMA_VERSION) { "不支持的备份版本" }
        require(sha256(dataBytes) == manifest.dataSha256) { "备份数据校验失败" }
        val data = BackupJson.decodeSnapshot(dataBytes)
        val files = entries.filterKeys { it.startsWith("attachments/") }
            .mapKeys { it.key.removePrefix("attachments/") }
        require(files.size == manifest.attachmentCount) { "附件数量不一致" }
        require(files.size == manifest.files.size) { "附件清单不完整" }
        var total = 0L
        files.forEach { (path, content) ->
            total += content.size
            val entry = manifest.files.firstOrNull { it.path == path }
            require(entry != null && sha256(content) == entry.sha256) { "附件校验失败：$path" }
        }
        require(total == manifest.attachmentBytes) { "附件总大小不一致" }
        return BackupArchive(manifest, data, attachmentFiles = files)
    }

    private suspend fun buildZipFile(): File {
        val data = SnapshotData(
            events = eventDao.getAllForBackup().map { it.event },
            categories = categoryDao.getAllForBackup(),
            tags = tagDao.getAllForBackup(),
            eventTags = eventTagDao.getAllForBackup(),
            reminders = reminderDao.getAllForBackup(),
            attachments = attachmentDao.getAllForBackup(),
            letters = letterDao.getAllForBackup()
        )
        val dataBytes = BackupJson.encodeSnapshot(data)
        val attachmentRoot = File(context.filesDir, "attachments")
        val files = mutableListOf<BackupFileEntry>()
        val fileMap = LinkedHashMap<String, File>()
        var attachmentBytes = 0L
        data.attachments.forEach { attachment ->
            val rawRelative = attachment.relativePath ?: return@forEach
            val relative = rawRelative.removePrefix("attachments/")
            val file = File(attachmentRoot, relative)
            if (!file.isFile) error("附件文件缺失：${attachment.displayName}")
            attachmentBytes += file.length()
            files += BackupFileEntry(relative, file.length(), sha256(file))
            fileMap[relative] = file
        }
        val settings = settingsRepository.settings.first()
        val manifest = BackupManifest(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            createdAt = clock.millis(),
            appVersion = appVersion(),
            eventCount = data.events.size,
            categoryCount = data.categories.size,
            tagCount = data.tags.size,
            reminderCount = data.reminders.size,
            letterCount = data.letters.size,
            attachmentCount = fileMap.size,
            attachmentBytes = attachmentBytes,
            dataSha256 = sha256(dataBytes),
            files = files,
            settings = BackupSettings(
                themeMode = settings.themeMode.name,
                dynamicColor = settings.dynamicColor,
                weekStart = settings.weekStart.name,
                leapDayPolicy = settings.leapDayPolicy.name,
                hideNotificationContent = settings.hideNotificationContent,
                lockTimeoutMinutes = settings.lockTimeoutMinutes
            )
        )
        val zip = File(context.cacheDir, "backup-build-${clock.millis()}.zip")
        ZipOutputStream(BufferedOutputStream(zip.outputStream())).use { out ->
            out.putNextEntry(ZipEntry("manifest.json"))
            out.write(BackupJson.encodeManifest(manifest))
            out.closeEntry()
            out.putNextEntry(ZipEntry("data.json"))
            out.write(dataBytes)
            out.closeEntry()
            fileMap.forEach { (path, file) ->
                out.putNextEntry(ZipEntry("attachments/$path"))
                file.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
        }
        return zip
    }

    private fun readLegacyZip(bytes: ByteArray): Map<String, ByteArray> {
        val result = LinkedHashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.name.contains("..")) { "备份包含非法路径" }
                val content = zip.readBytes()
                total += content.size
                require(total <= legacyMaxBytes) { "旧版备份解压后过大" }
                result[entry.name] = content
                zip.closeEntry()
            }
        }
        return result
    }

    suspend fun restore(
        archive: BackupArchive,
        passphrase: CharArray,
        mode: RestoreMode = RestoreMode.REPLACE
    ): RestoreResult {
        val autoName = "auto-" + clock.millis() + ".lunary"
        val autoDir = File(context.filesDir, "auto_backups").apply { mkdirs() }
        writeEncryptedToFile(File(autoDir, autoName), passphrase)
        pruneAutoBackups(autoDir)

        val staging = File(context.cacheDir, "restore-" + clock.millis()).apply { mkdirs() }
        try {
            val existingCategories = categoryDao.getAllForBackup()
            val existingTags = tagDao.getAllForBackup()

            val categories = archive.data.categories
            val tags = archive.data.tags
            var events = archive.data.events
            var refs = archive.data.eventTags
            var reminders = archive.data.reminders
            var attachments = archive.data.attachments
            var letters = archive.data.letters
            var categoriesToWrite = categories
            var tagsToWrite = tags
            val filesToWrite = mutableMapOf<String, String>()

            if (mode == RestoreMode.MERGE) {
                val catMap = mutableMapOf<String, String>()
                val activeCatByName = existingCategories
                    .asSequence()
                    .filter { it.deletedAt == null }
                    .associateBy { it.name }
                    .toMutableMap()
                val usedCatIds = existingCategories.mapTo(mutableSetOf()) { it.id }
                val newCategories = mutableListOf<CategoryEntity>()
                categories.forEach { cat ->
                    val existing = activeCatByName[cat.name]
                    when {
                        existing != null -> catMap[cat.id] = existing.id
                        cat.id in usedCatIds -> {
                            val newId = UUID.randomUUID().toString()
                            val copy = cat.copy(id = newId, updatedAt = clock.millis())
                            catMap[cat.id] = newId
                            usedCatIds += newId
                            if (copy.deletedAt == null) activeCatByName[cat.name] = copy
                            newCategories += copy
                        }
                        else -> {
                            catMap[cat.id] = cat.id
                            usedCatIds += cat.id
                            if (cat.deletedAt == null) activeCatByName[cat.name] = cat
                            newCategories += cat
                        }
                    }
                }
                categoriesToWrite = newCategories

                val tagMap = mutableMapOf<String, String>()
                val activeTagByName = existingTags
                    .asSequence()
                    .filter { it.deletedAt == null }
                    .associateBy { it.name }
                    .toMutableMap()
                val usedTagIds = existingTags.mapTo(mutableSetOf()) { it.id }
                val newTags = mutableListOf<TagEntity>()
                tags.forEach { tag ->
                    val existing = activeTagByName[tag.name]
                    when {
                        existing != null -> tagMap[tag.id] = existing.id
                        tag.id in usedTagIds -> {
                            val newId = UUID.randomUUID().toString()
                            val copy = tag.copy(id = newId, updatedAt = clock.millis())
                            tagMap[tag.id] = newId
                            usedTagIds += newId
                            if (copy.deletedAt == null) activeTagByName[tag.name] = copy
                            newTags += copy
                        }
                        else -> {
                            tagMap[tag.id] = tag.id
                            usedTagIds += tag.id
                            if (tag.deletedAt == null) activeTagByName[tag.name] = tag
                            newTags += tag
                        }
                    }
                }
                tagsToWrite = newTags

                val eventMap = mutableMapOf<String, String>()
                events = events.map { event ->
                    val id = UUID.randomUUID().toString()
                    val copied = event.copy(
                        id = id,
                        categoryId = event.categoryId?.let { catMap[it] },
                        updatedAt = clock.millis()
                    )
                    eventMap[event.id] = id
                    copied
                }
                refs = refs.mapNotNull { ref ->
                    val eventId = eventMap[ref.eventId] ?: return@mapNotNull null
                    val tagId = tagMap[ref.tagId] ?: return@mapNotNull null
                    EventTagCrossRef(eventId, tagId)
                }
                reminders = reminders.map { reminder ->
                    reminder.copy(
                        id = UUID.randomUUID().toString(),
                        eventId = eventMap[reminder.eventId] ?: reminder.eventId,
                        updatedAt = clock.millis()
                    )
                }
                letters = letters.map { letter ->
                    letter.copy(
                        id = UUID.randomUUID().toString(),
                        updatedAt = clock.millis()
                    )
                }
                attachments = attachments.map { attachment ->
                    val newEventId = eventMap[attachment.eventId] ?: attachment.eventId
                    val oldRelative = attachment.relativePath?.removePrefix("attachments/")
                    val fileName = oldRelative?.substringAfterLast('/') ?: "attachment.bin"
                    val newRelative = "$newEventId/$fileName"
                    if (oldRelative != null) filesToWrite[newRelative] = oldRelative
                    attachment.copy(
                        id = UUID.randomUUID().toString(),
                        eventId = newEventId,
                        relativePath = "attachments/$newRelative",
                        updatedAt = clock.millis()
                    )
                }
            } else {
                archive.data.attachments.forEach { attachment ->
                    val relative = attachment.relativePath?.removePrefix("attachments/") ?: return@forEach
                    filesToWrite[relative] = relative
                }
            }

            filesToWrite.forEach { (newRelative, oldRelative) ->
                val target = File(staging, newRelative)
                require(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                    "备份包含非法附件路径"
                }
                if (!archive.writeAttachmentTo(oldRelative, target)) {
                    error("附件文件缺失：$oldRelative")
                }
            }

            database.withTransaction {
                if (mode == RestoreMode.REPLACE) {
                    eventTagDao.deleteAll()
                    reminderDao.deleteAll()
                    attachmentDao.deleteAll()
                    letterDao.deleteAll()
                    eventDao.deleteAll()
                    categoryDao.deleteAll()
                    tagDao.deleteAll()
                }
                categoriesToWrite.forEach { categoryDao.upsert(it) }
                tagsToWrite.forEach { tagDao.upsert(it) }
                events.forEach { eventDao.upsert(it) }
                if (refs.isNotEmpty()) eventTagDao.insertAll(refs)
                reminders.forEach { reminderDao.upsert(it) }
                attachments.forEach { attachmentDao.upsert(it) }
                letters.forEach { letterDao.upsert(it) }
            }

            val attachmentsDir = File(context.filesDir, "attachments")
            if (mode == RestoreMode.REPLACE) {
                attachmentsDir.deleteRecursively()
            }
            if (filesToWrite.isNotEmpty()) {
                attachmentsDir.mkdirs()
                staging.copyRecursively(attachmentsDir, overwrite = true)
            }
            if (mode == RestoreMode.REPLACE) {
                archive.manifest.settings?.let { applySettings(it) }
            }
            reminderScheduler.rescheduleAll()
            return RestoreResult(
                events = events.size,
                categories = categories.size,
                tags = tags.size,
                reminders = reminders.size,
                attachments = attachments.size,
                autoBackupName = autoName,
                letters = letters.size
            )
        } finally {
            staging.deleteRecursively()
        }
    }

    private suspend fun applySettings(settings: BackupSettings) {
        runCatching { ThemeMode.valueOf(settings.themeMode) }
            .onSuccess { settingsRepository.setThemeMode(it) }
        runCatching { WeekStart.valueOf(settings.weekStart) }
            .onSuccess { settingsRepository.setWeekStart(it) }
        runCatching { LeapDayPolicy.valueOf(settings.leapDayPolicy) }
            .onSuccess { settingsRepository.setLeapDayPolicy(it) }
        settingsRepository.setDynamicColor(settings.dynamicColor)
        settingsRepository.setHideNotificationContent(settings.hideNotificationContent)
        settingsRepository.setLockTimeoutMinutes(settings.lockTimeoutMinutes)
    }

    private fun pruneAutoBackups(dir: File) {
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
    }

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }.getOrDefault("unknown")

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
    }
}
