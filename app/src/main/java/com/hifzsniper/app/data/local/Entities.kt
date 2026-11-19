package com.hifzsniper.app.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learning_card",
    indices = [Index("nextReviewAt"), Index(value = ["surahId", "ayahStart", "ayahEnd", "type"])]
)
data class LearningCardEntity(
    @PrimaryKey val id: String,
    val type: String,
    val surahId: Int,
    val ayahStart: Int,
    val ayahEnd: Int,
    val wordStart: Int?,
    val wordEnd: Int?,
    val prompt: String,
    val answer: String,
    val createdAt: Long,
    val lastReviewedAt: Long?,
    val nextReviewAt: Long,
    val intervalDays: Double,
    val stability: Double,
    val difficulty: Double,
    val lapseCount: Int,
    val successCount: Int,
    val hesitationCount: Int,
    val isWeak: Boolean,
    val suspended: Boolean,
    val sourceCardId: String?,
    val userNote: String?,
    val estimatedDurationSeconds: Int,
    val lastResult: String?,
    val algorithmVersion: Int,
    @ColumnInfo(defaultValue = "'RECITATION'") val promptMode: String = "RECITATION",
    @ColumnInfo(defaultValue = "0") val contextBeforeCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val contextAfterCount: Int = 0
)

@Entity(
    tableName = "review_log",
    indices = [Index("cardId"), Index("reviewedAt")]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val reviewedAt: Long,
    val rating: String,
    val previousInterval: Double,
    val newInterval: Double,
    val previousStability: Double,
    val newStability: Double,
    val wasOffline: Boolean,
    val responseTimeMs: Long? = null,
    val sessionId: String? = null
)

@Entity(tableName = "surah")
data class SurahEntity(
    @PrimaryKey val id: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameFrench: String,
    val ayahCount: Int,
    val revelationType: String?,
    val dataVersion: String
)

@Entity(
    tableName = "ayah",
    indices = [Index(value = ["verseKey"], unique = true), Index("surahId")]
)
data class AyahEntity(
    @PrimaryKey val id: Long,
    val surahId: Int,
    val ayahNumber: Int,
    val verseKey: String,
    val textUthmani: String,
    val textSimple: String?,
    val pageNumber: Int?,
    val juzNumber: Int,
    val hizbNumber: Int?,
    val dataVersion: String
)

@Entity(
    tableName = "word",
    indices = [Index(value = ["verseKey", "positionInAyah"], unique = true)]
)
data class WordEntity(
    @PrimaryKey val id: String,
    val verseKey: String,
    val positionInAyah: Int,
    val textUthmani: String,
    val transliteration: String?,
    val translationFrench: String?
)

@Entity(
    tableName = "audio_track",
    indices = [Index("verseKey"), Index("reciterId")]
)
data class AudioTrackEntity(
    @PrimaryKey val id: String,
    val reciterId: String,
    val reciterName: String,
    val verseKey: String,
    val remoteUrl: String,
    val localPath: String?,
    val durationMs: Long?,
    val checksum: String,
    val downloadStatus: String,
    val sourceVersion: String,
    val timingVersion: String?
)

@Entity(
    tableName = "word_timing",
    indices = [Index(value = ["audioTrackId", "wordId"], unique = true), Index(value = ["audioTrackId", "startMs"])]
)
data class WordTimingEntity(
    @PrimaryKey val id: String,
    val audioTrackId: String,
    val wordId: String,
    val startMs: Long,
    val endMs: Long,
    val sourceVersion: String
)

@Entity(
    tableName = "translation",
    indices = [Index(value = ["verseKey", "languageCode", "translatorName"], unique = true)]
)
data class TranslationEntity(
    @PrimaryKey val id: String,
    val verseKey: String,
    val languageCode: String,
    val translatorName: String,
    val text: String,
    val source: String,
    val license: String,
    val version: String
)

@Entity(
    tableName = "tafsir",
    indices = [Index(value = ["verseKey", "languageCode", "authorOrCollection"], unique = true)]
)
data class TafsirEntity(
    @PrimaryKey val id: String,
    val verseKey: String,
    val languageCode: String,
    val authorOrCollection: String,
    val text: String,
    val source: String,
    val license: String,
    val version: String
)

@Entity(tableName = "study_session", indices = [Index("startedAt")])
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val plannedMinutes: Int,
    val actualSeconds: Int,
    val newCardsCount: Int,
    val reviewCardsCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val sessionType: String
)

@Entity(tableName = "weak_point", indices = [Index("verseKey"), Index("linkedCardId")])
data class WeakPointEntity(
    @PrimaryKey val id: String,
    val verseKey: String,
    val wordStart: Int?,
    val wordEnd: Int?,
    val reason: String,
    val severity: Int,
    val createdAt: Long,
    val resolvedAt: Long?,
    val linkedCardId: String?
)

@Entity(
    tableName = "download_item",
    indices = [Index(value = ["resourceType", "resourceKey"], unique = true), Index("status")]
)
data class DownloadItemEntity(
    @PrimaryKey val id: String,
    val resourceType: String,
    val resourceKey: String,
    val remoteUrl: String,
    val localPath: String,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val status: String,
    val checksum: String,
    val errorMessage: String?,
    val updatedAt: Long
)

data class ContentStatsEntity(
    val surahCount: Int,
    val ayahCount: Int,
    val wordCount: Int,
    val audioTrackCount: Int,
    val timingCount: Int
)
