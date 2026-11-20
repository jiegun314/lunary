package com.lunary.app.data.backup

import com.lunary.app.data.local.entity.AttachmentEntity
import com.lunary.app.data.local.entity.CategoryEntity
import com.lunary.app.domain.model.EmojiCatalog
import com.lunary.app.data.local.EventLocationJson
import com.lunary.app.data.local.entity.EventEntity
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.data.local.entity.LetterEntity
import com.lunary.app.data.local.entity.ReminderEntity
import com.lunary.app.data.local.entity.TagEntity
import org.json.JSONArray
import org.json.JSONObject

object BackupJson {

    fun encodeManifest(manifest: BackupManifest): ByteArray = JSONObject().apply {
        put("schemaVersion", manifest.schemaVersion)
        put("createdAt", manifest.createdAt)
        put("appVersion", manifest.appVersion)
        put("eventCount", manifest.eventCount)
        put("categoryCount", manifest.categoryCount)
        put("tagCount", manifest.tagCount)
        put("reminderCount", manifest.reminderCount)
        put("attachmentCount", manifest.attachmentCount)
        put("attachmentBytes", manifest.attachmentBytes)
        put("letterCount", manifest.letterCount)
        put("dataSha256", manifest.dataSha256)
        manifest.settings?.let { settings ->
            put(
                "settings",
                JSONObject().apply {
                    put("themeMode", settings.themeMode)
                    put("dynamicColor", settings.dynamicColor)
                    put("weekStart", settings.weekStart)
                    put("leapDayPolicy", settings.leapDayPolicy)
                    put("hideNotificationContent", settings.hideNotificationContent)
                    put("lockTimeoutMinutes", settings.lockTimeoutMinutes)
                }
            )
        }
        put("files", JSONArray().apply {
            manifest.files.forEach { file ->
                put(JSONObject().apply {
                    put("path", file.path)
                    put("size", file.size)
                    put("sha256", file.sha256)
                })
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decodeManifest(bytes: ByteArray): BackupManifest {
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        val files = json.getJSONArray("files")
        return BackupManifest(
            schemaVersion = json.getInt("schemaVersion"),
            createdAt = json.getLong("createdAt"),
            appVersion = json.optString("appVersion", "unknown"),
            eventCount = json.getInt("eventCount"),
            categoryCount = json.getInt("categoryCount"),
            tagCount = json.getInt("tagCount"),
            reminderCount = json.getInt("reminderCount"),
            attachmentCount = json.getInt("attachmentCount"),
            attachmentBytes = json.getLong("attachmentBytes"),
            letterCount = json.optInt("letterCount", 0),
            dataSha256 = json.getString("dataSha256"),
            settings = json.optJSONObject("settings")?.let { s ->
                BackupSettings(
                    themeMode = s.optString("themeMode", "SYSTEM"),
                    dynamicColor = s.optBoolean("dynamicColor", true),
                    weekStart = s.optString("weekStart", "SYSTEM"),
                    leapDayPolicy = s.optString("leapDayPolicy", "FEBRUARY_28"),
                    hideNotificationContent = s.optBoolean("hideNotificationContent", true),
                    lockTimeoutMinutes = s.optInt("lockTimeoutMinutes", 1)
                )
            },
            files = buildList {
                for (i in 0 until files.length()) {
                    val file = files.getJSONObject(i)
                    add(
                        BackupFileEntry(
                            path = file.getString("path"),
                            size = file.getLong("size"),
                            sha256 = file.getString("sha256")
                        )
                    )
                }
            }
        )
    }

    fun encodeSnapshot(data: SnapshotData): ByteArray = JSONObject().apply {
        put("schemaVersion", BACKUP_SCHEMA_VERSION)
        put("events", JSONArray().apply { data.events.forEach { put(it.toJson()) } })
        put("categories", JSONArray().apply { data.categories.forEach { put(it.toJson()) } })
        put("tags", JSONArray().apply { data.tags.forEach { put(it.toJson()) } })
        put("eventTags", JSONArray().apply {
            data.eventTags.forEach { put(JSONObject().put("eventId", it.eventId).put("tagId", it.tagId)) }
        })
        put("reminders", JSONArray().apply { data.reminders.forEach { put(it.toJson()) } })
        put("attachments", JSONArray().apply { data.attachments.forEach { put(it.toJson()) } })
        put("letters", JSONArray().apply { data.letters.forEach { put(it.toJson()) } })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decodeSnapshot(bytes: ByteArray): SnapshotData {
        val json = JSONObject(bytes.toString(Charsets.UTF_8))
        require(json.optInt("schemaVersion", -1) in 1..BACKUP_SCHEMA_VERSION) { "不支持的备份版本" }
        return SnapshotData(
            events = json.getJSONArray("events").mapObjects { it.toEvent() },
            categories = json.getJSONArray("categories").mapObjects { it.toCategory() },
            tags = json.getJSONArray("tags").mapObjects { it.toTag() },
            eventTags = json.getJSONArray("eventTags").mapObjects {
                EventTagCrossRef(it.getString("eventId"), it.getString("tagId"))
            },
            reminders = json.getJSONArray("reminders").mapObjects { it.toReminder() },
            attachments = json.getJSONArray("attachments").mapObjects { it.toAttachment() },
            letters = json.optJSONArray("letters")?.mapObjects { it.toLetter() } ?: emptyList()
        )
    }

    private fun EventEntity.toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("emoji", emoji)
        put("dateEpochDay", dateEpochDay)
        put("dateMode", dateMode); put("recurrenceRule", recurrenceRule)
        put("categoryId", categoryId); put("colorArgb", colorArgb)
        put("note", note); put("location", location)
        put("latitude", latitude); put("longitude", longitude)
        val locations = EventLocationJson.decode(locationsJson)
            .ifEmpty { EventLocationJson.fromLegacy(location, latitude, longitude) }
        put("locations", EventLocationJson.toJsonArray(locations))
        put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("deletedAt", deletedAt); put("revision", revision)
    }

    private fun JSONObject.toEvent(): EventEntity {
        val rawTitle = getString("title")
        val storedEmoji = optStringOrNull("emoji")
        val legacyEmoji = if (storedEmoji == null) EmojiCatalog.leading(rawTitle) else null
        val title = legacyEmoji?.let { rawTitle.removePrefix(it).trimStart() } ?: rawTitle
        return EventEntity(
            id = getString("id"), title = title, emoji = storedEmoji ?: legacyEmoji,
            dateEpochDay = getLong("dateEpochDay"),
            dateMode = optString("dateMode", "FULL_DATE"),
            recurrenceRule = optString("recurrenceRule", "NONE"),
            categoryId = optStringOrNull("categoryId"), colorArgb = getInt("colorArgb"),
            note = optStringOrNull("note"), location = optStringOrNull("location"),
            latitude = optDoubleOrNull("latitude"), longitude = optDoubleOrNull("longitude"),
            createdAt = getLong("createdAt"), updatedAt = getLong("updatedAt"),
            deletedAt = optLongOrNull("deletedAt"), revision = optLong("revision", 1),
            locationsJson = optJSONArray("locations")?.let { array ->
                EventLocationJson.encode(EventLocationJson.fromJsonArray(array))
            }
        )
    }

    private fun CategoryEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("colorArgb", colorArgb)
        put("sortOrder", sortOrder); put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("deletedAt", deletedAt); put("revision", revision)
    }

    private fun JSONObject.toCategory() = CategoryEntity(
        id = getString("id"), name = getString("name"), colorArgb = getInt("colorArgb"),
        sortOrder = optInt("sortOrder", 0), createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"), deletedAt = optLongOrNull("deletedAt"),
        revision = optLong("revision", 1)
    )

    private fun TagEntity.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("emoji", emoji); put("colorArgb", colorArgb)
        put("sortOrder", sortOrder); put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("deletedAt", deletedAt); put("revision", revision)
    }

    private fun JSONObject.toTag() = TagEntity(
        id = getString("id"), name = getString("name"), emoji = optStringOrNull("emoji"),
        colorArgb = if (isNull("colorArgb")) null else getInt("colorArgb"),
        sortOrder = optInt("sortOrder", 0), createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"), deletedAt = optLongOrNull("deletedAt"),
        revision = optLong("revision", 1)
    )

    private fun ReminderEntity.toJson() = JSONObject().apply {
        put("id", id); put("eventId", eventId); put("minutesBefore", minutesBefore)
        put("localTimeMinutes", localTimeMinutes); put("enabled", enabled)
        put("lastTriggeredAt", lastTriggeredAt)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun JSONObject.toReminder() = ReminderEntity(
        id = getString("id"), eventId = getString("eventId"),
        minutesBefore = optInt("minutesBefore", 0),
        localTimeMinutes = optInt("localTimeMinutes", 540),
        enabled = optBoolean("enabled", true),
        lastTriggeredAt = optLongOrNull("lastTriggeredAt"),
        createdAt = getLong("createdAt"), updatedAt = getLong("updatedAt")
    )

    private fun AttachmentEntity.toJson() = JSONObject().apply {
        put("id", id); put("eventId", eventId); put("displayName", displayName)
        put("mimeType", mimeType); put("sizeBytes", sizeBytes)
        put("storageKind", storageKind); put("relativePath", relativePath)
        put("persistedUri", persistedUri); put("checksum", checksum)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("deletedAt", deletedAt); put("revision", revision)
    }

    private fun JSONObject.toAttachment() = AttachmentEntity(
        id = getString("id"), eventId = getString("eventId"),
        displayName = getString("displayName"), mimeType = getString("mimeType"),
        sizeBytes = getLong("sizeBytes"), storageKind = optString("storageKind", "INTERNAL"),
        relativePath = optStringOrNull("relativePath"),
        persistedUri = optStringOrNull("persistedUri"), checksum = optStringOrNull("checksum"),
        createdAt = getLong("createdAt"), updatedAt = getLong("updatedAt"),
        deletedAt = optLongOrNull("deletedAt"), revision = optLong("revision", 1)
    )

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key)) null else optDouble(key)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        buildList { for (i in 0 until length()) add(transform(getJSONObject(i))) }
    private fun LetterEntity.toJson() = JSONObject().apply {
        put("id", id); put("dateEpochDay", dateEpochDay); put("timeMinutes", timeMinutes)
        put("sender", sender); put("recipient", recipient); put("subject", subject)
        put("content", content); put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("deletedAt", deletedAt); put("revision", revision)
    }

    private fun JSONObject.toLetter() = LetterEntity(
        id = getString("id"),
        dateEpochDay = getLong("dateEpochDay"),
        timeMinutes = optInt("timeMinutes", 0),
        sender = getString("sender"),
        recipient = getString("recipient"),
        subject = getString("subject"),
        content = getString("content"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"),
        deletedAt = optLongOrNull("deletedAt"),
        revision = optLong("revision", 1)
    )


}
