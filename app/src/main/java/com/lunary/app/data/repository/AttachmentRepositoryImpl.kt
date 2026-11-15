package com.lunary.app.data.repository

import android.net.Uri
import com.lunary.app.data.attachment.AttachmentStore
import com.lunary.app.data.local.dao.AttachmentDao
import com.lunary.app.domain.model.Attachment
import com.lunary.app.domain.repository.AttachmentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    private val attachmentDao: AttachmentDao,
    private val store: AttachmentStore
) : AttachmentRepository {

    override fun observeForEvent(eventId: String): Flow<List<Attachment>> =
        attachmentDao.observeForEvent(eventId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun add(eventId: String, uri: Uri): Attachment? {
        val attachment = store.import(eventId, uri) ?: return null
        attachmentDao.upsert(attachment.toEntity())
        return attachment
    }

    override suspend fun delete(attachment: Attachment) {
        attachmentDao.delete(attachment.id)
        store.deleteFile(attachment)
    }

    override suspend fun deleteForEvent(eventId: String) {
        attachmentDao.deleteForEvent(eventId)
        store.deleteEventFiles(eventId)
    }
}
