package com.hifzsniper.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query(
        """SELECT
            (SELECT COUNT(*) FROM surah) AS surahCount,
            (SELECT COUNT(*) FROM ayah) AS ayahCount,
            (SELECT COUNT(*) FROM word) AS wordCount,
            (SELECT COUNT(*) FROM audio_track) AS audioTrackCount,
            (SELECT COUNT(*) FROM word_timing) AS timingCount"""
    )
    fun observeStats(): Flow<ContentStatsEntity>

    @Query("SELECT * FROM surah ORDER BY id")
    fun observeSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surah WHERE id = :id LIMIT 1")
    fun observeSurah(id: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM surah WHERE id = :id LIMIT 1")
    suspend fun getSurah(id: Int): SurahEntity?

    @Query("SELECT * FROM ayah WHERE pageNumber = :page ORDER BY surahId ASC, ayahNumber ASC LIMIT 1")
    suspend fun getFirstAyahForPage(page: Int): AyahEntity?

    @Query("SELECT * FROM ayah WHERE pageNumber = :page ORDER BY id")
    fun observeAyahsForPage(page: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayah WHERE pageNumber = :page ORDER BY id")
    suspend fun getAyahsForPage(page: Int): List<AyahEntity>

    @Query("SELECT DISTINCT pageNumber FROM ayah WHERE pageNumber IS NOT NULL ORDER BY pageNumber")
    fun observePageNumbers(): Flow<List<Int>>

    @Query("SELECT * FROM ayah WHERE surahId = :surahId ORDER BY ayahNumber")
    fun observeAyahs(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayah WHERE surahId = :surahId ORDER BY ayahNumber")
    suspend fun getAyahsForSurah(surahId: Int): List<AyahEntity>

    @Query("SELECT * FROM ayah ORDER BY id")
    suspend fun getAllAyahs(): List<AyahEntity>

    @Query("SELECT * FROM ayah WHERE surahId = :surahId AND ayahNumber = :ayahNumber LIMIT 1")
    fun observeAyah(surahId: Int, ayahNumber: Int): Flow<AyahEntity?>

    @Query("SELECT * FROM ayah WHERE surahId = :surahId AND ayahNumber = :ayahNumber LIMIT 1")
    suspend fun getAyah(surahId: Int, ayahNumber: Int): AyahEntity?

    @Query("SELECT * FROM word WHERE verseKey = :verseKey ORDER BY positionInAyah")
    fun observeWords(verseKey: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM word WHERE verseKey = :verseKey ORDER BY positionInAyah")
    suspend fun getWordsForVerse(verseKey: String): List<WordEntity>

    @Query("SELECT * FROM translation WHERE verseKey = :verseKey AND languageCode = 'fr' LIMIT 1")
    fun observeFrenchTranslation(verseKey: String): Flow<TranslationEntity?>

    @Query("SELECT * FROM tafsir WHERE verseKey = :verseKey AND languageCode = 'fr' LIMIT 1")
    fun observeFrenchTafsir(verseKey: String): Flow<TafsirEntity?>

    @Query("SELECT * FROM audio_track WHERE verseKey = :verseKey AND reciterId = 'mishary-rashid-alafasy' LIMIT 1")
    fun observeAlafasyTrack(verseKey: String): Flow<AudioTrackEntity?>

    @Query("SELECT * FROM word_timing WHERE audioTrackId = :audioTrackId ORDER BY startMs")
    fun observeWordTimings(audioTrackId: String): Flow<List<WordTimingEntity>>

    @Query("SELECT dataVersion FROM surah ORDER BY id LIMIT 1")
    suspend fun contentVersion(): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSurahs(items: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAyahs(items: List<AyahEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWords(items: List<WordEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAudioTracks(items: List<AudioTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWordTimings(items: List<WordTimingEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTranslations(items: List<TranslationEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTafsirs(items: List<TafsirEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDownloads(items: List<DownloadItemEntity>)

    @Query("DELETE FROM word_timing")
    suspend fun deleteWordTimings()

    @Query("DELETE FROM audio_track")
    suspend fun deleteAudioTracks()

    @Query("DELETE FROM word")
    suspend fun deleteWords()

    @Query("DELETE FROM translation")
    suspend fun deleteTranslations()

    @Query("DELETE FROM tafsir")
    suspend fun deleteTafsirs()

    @Query("DELETE FROM ayah")
    suspend fun deleteAyahs()

    @Query("DELETE FROM surah")
    suspend fun deleteSurahs()

    @Query("DELETE FROM download_item WHERE resourceType = 'AUDIO_TRACK'")
    suspend fun deleteAudioDownloads()

    @Transaction
    suspend fun insertValidatedContent(bundle: PersistableContentBundle) {
        insertSurahs(bundle.surahs)
        insertAyahs(bundle.ayahs)
        insertWords(bundle.words)
        insertAudioTracks(bundle.audioTracks)
        insertWordTimings(bundle.wordTimings)
        insertTranslations(bundle.translations)
        insertTafsirs(bundle.tafsirs)
        insertDownloads(bundle.downloads)
    }

    @Transaction
    suspend fun replaceValidatedContent(bundle: PersistableContentBundle) {
        deleteWordTimings()
        deleteAudioTracks()
        deleteWords()
        deleteTranslations()
        deleteTafsirs()
        deleteAyahs()
        deleteSurahs()
        deleteAudioDownloads()
        insertValidatedContent(bundle)
    }
}

data class PersistableContentBundle(
    val surahs: List<SurahEntity>,
    val ayahs: List<AyahEntity>,
    val words: List<WordEntity>,
    val audioTracks: List<AudioTrackEntity>,
    val wordTimings: List<WordTimingEntity>,
    val translations: List<TranslationEntity>,
    val tafsirs: List<TafsirEntity>,
    val downloads: List<DownloadItemEntity>
)
