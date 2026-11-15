package com.lunary.app.data.attachment

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.lunary.app.domain.model.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AttachmentDraft(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)

/**
 * Copies picked files into the app-private attachments directory so they
 * survive the source URI being revoked. Files are scoped per event.
 */
@Singleton
class AttachmentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock
) {
    private val root: File get() = File(context.filesDir, "attachments")

    suspend fun describe(uri: Uri): AttachmentDraft = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name: String? = null
        var size = 0L
        runCatching {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
        }
        AttachmentDraft(
            uri = uri,
            displayName = name ?: uri.lastPathSegment ?: "附件",
            mimeType = resolver.getType(uri) ?: "application/octet-stream",
            sizeBytes = size
        )
    }

    suspend fun import(eventId: String, uri: Uri): Attachment? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val displayName = queryDisplayName(uri) ?: uri.lastPathSegment ?: "附件"
        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime)
            ?.let { ".$it" }
            ?: displayName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { ".$it" }
            ?: ""
        val dir = File(root, eventId).apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}$extension")
        runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
        }.getOrElse {
            target.delete()
            return@withContext null
        }
        if (!target.exists() || target.length() == 0L) {
            target.delete()
            return@withContext null
        }
        val now = clock.millis()
        Attachment(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            displayName = displayName,
            mimeType = mime,
            sizeBytes = target.length(),
            storageKind = "INTERNAL",
            relativePath = target.path.removePrefix(context.filesDir.path + "/"),
            checksum = sha256(target),
            createdAt = Instant.ofEpochMilli(now),
            updatedAt = Instant.ofEpochMilli(now)
        )
    }

    fun fileFor(attachment: Attachment): File? =
        attachment.relativePath?.let { File(context.filesDir, it) }?.takeIf { it.exists() }

    fun contentUri(attachment: Attachment): Uri? {
        val file = fileFor(attachment) ?: return null
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    suspend fun deleteFile(attachment: Attachment) = withContext(Dispatchers.IO) {
        val file = fileFor(attachment)
        file?.delete()
        file?.parentFile?.takeIf { it.listFiles().isNullOrEmpty() }?.delete()
        Unit
    }

    suspend fun deleteEventFiles(eventId: String) = withContext(Dispatchers.IO) {
        File(root, eventId).deleteRecursively()
        Unit
    }

    suspend fun pruneEmptyDirs() = withContext(Dispatchers.IO) {
        root.listFiles()?.filter { it.isDirectory && it.listFiles().isNullOrEmpty() }?.forEach { it.delete() }
        Unit
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
