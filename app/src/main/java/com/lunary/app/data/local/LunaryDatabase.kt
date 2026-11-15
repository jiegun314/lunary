package com.lunary.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lunary.app.data.local.dao.AttachmentDao
import com.lunary.app.data.local.dao.CategoryDao
import com.lunary.app.data.local.dao.EventDao
import com.lunary.app.data.local.dao.EventTagDao
import com.lunary.app.data.local.dao.LetterDao
import com.lunary.app.data.local.dao.ReminderDao
import com.lunary.app.data.local.dao.TagDao
import com.lunary.app.data.local.entity.AttachmentEntity
import com.lunary.app.data.local.entity.CategoryEntity
import com.lunary.app.data.local.entity.EventEntity
import com.lunary.app.data.local.entity.EventTagCrossRef
import com.lunary.app.data.local.entity.LetterEntity
import com.lunary.app.data.local.entity.ReminderEntity
import com.lunary.app.data.local.entity.SyncOperationEntity
import com.lunary.app.data.local.entity.TagEntity
import com.lunary.app.domain.model.EmojiCatalog

@Database(
    entities = [
        EventEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        EventTagCrossRef::class,
        ReminderEntity::class,
        AttachmentEntity::class,
        SyncOperationEntity::class,
        LetterEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class LunaryDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun eventTagDao(): EventTagDao
    abstract fun reminderDao(): ReminderDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun letterDao(): LetterDao

    companion object {
        const val NAME = "lunary.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE events ADD COLUMN longitude REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `letters` (" +
                        "`id` TEXT NOT NULL, " +
                        "`dateEpochDay` INTEGER NOT NULL, " +
                        "`timeMinutes` INTEGER NOT NULL, " +
                        "`sender` TEXT NOT NULL, " +
                        "`recipient` TEXT NOT NULL, " +
                        "`subject` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`deletedAt` INTEGER, " +
                        "`revision` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_letters_dateEpochDay` ON `letters` (`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_letters_deletedAt` ON `letters` (`deletedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_letters_updatedAt` ON `letters` (`updatedAt`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN emoji TEXT")
                db.query("SELECT id, title FROM events").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val titleIndex = cursor.getColumnIndexOrThrow("title")
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val title = cursor.getString(titleIndex) ?: continue
                        val emoji = EmojiCatalog.leading(title) ?: continue
                        db.execSQL(
                            "UPDATE events SET emoji = ?, title = ? WHERE id = ?",
                            arrayOf(emoji, title.removePrefix(emoji).trimStart(), id)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Seeds the starter categories on first creation only.
 */
object LunarySeed {

    private data class SeedCategory(val id: String, val name: String, val color: Long, val order: Int)

    private val categories = listOf(
        SeedCategory("cat-anniversary", "纪念日", 0xFFE91E63, 0),
        SeedCategory("cat-birthday", "生日", 0xFFFF9800, 1),
        SeedCategory("cat-festival", "节日", 0xFF4CAF50, 2),
        SeedCategory("cat-travel", "旅行", 0xFF2196F3, 3),
        SeedCategory("cat-other", "其他", 0xFF9E9E9E, 4)
    )

    fun seed(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        categories.forEach { category ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories " +
                    "(id, name, colorArgb, sortOrder, createdAt, updatedAt, deletedAt, revision) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NULL, 1)",
                arrayOf<Any>(category.id, category.name, category.color.toInt(), category.order, now, now)
            )
        }
    }
}
