package com.hifzsniper.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LearningCardEntity::class,
        ReviewLogEntity::class,
        SurahEntity::class,
        AyahEntity::class,
        WordEntity::class,
        AudioTrackEntity::class,
        WordTimingEntity::class,
        TranslationEntity::class,
        TafsirEntity::class,
        StudySessionEntity::class,
        WeakPointEntity::class,
        DownloadItemEntity::class
    ],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningDao(): LearningDao
    abstract fun contentDao(): ContentDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hifz-sniper.db"
            ).build().also { instance = it }
        }
    }
}
