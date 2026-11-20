package com.lunary.app.data.local

import android.content.Context
import androidx.room.Room
import com.lunary.app.data.local.dao.AttachmentDao
import com.lunary.app.data.local.dao.CategoryDao
import com.lunary.app.data.local.dao.EventDao
import com.lunary.app.data.local.dao.EventTagDao
import com.lunary.app.data.local.dao.LetterDao
import com.lunary.app.data.local.dao.ReminderDao
import com.lunary.app.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LunaryDatabase =
        Room.databaseBuilder(context, LunaryDatabase::class.java, LunaryDatabase.NAME)
            .addMigrations(
                LunaryDatabase.MIGRATION_1_2,
                LunaryDatabase.MIGRATION_2_3,
                LunaryDatabase.MIGRATION_3_4,
                LunaryDatabase.MIGRATION_4_5
            )
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    LunarySeed.seed(db)
                }
            })
            .build()

    @Provides
    fun provideEventDao(db: LunaryDatabase): EventDao = db.eventDao()

    @Provides
    fun provideCategoryDao(db: LunaryDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTagDao(db: LunaryDatabase): TagDao = db.tagDao()

    @Provides
    fun provideEventTagDao(db: LunaryDatabase): EventTagDao = db.eventTagDao()

    @Provides
    fun provideReminderDao(db: LunaryDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideAttachmentDao(db: LunaryDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun provideLetterDao(db: LunaryDatabase): LetterDao = db.letterDao()
}
