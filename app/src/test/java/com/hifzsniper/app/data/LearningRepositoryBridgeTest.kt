package com.hifzsniper.app.data

import com.hifzsniper.app.data.local.AudioTrackEntity
import com.hifzsniper.app.data.local.AyahEntity
import com.hifzsniper.app.data.local.ContentDao
import com.hifzsniper.app.data.local.ContentStatsEntity
import com.hifzsniper.app.data.local.DownloadItemEntity
import com.hifzsniper.app.data.local.LearningCardEntity
import com.hifzsniper.app.data.local.LearningDao
import com.hifzsniper.app.data.local.ReviewLogEntity
import com.hifzsniper.app.data.local.SurahEntity
import com.hifzsniper.app.data.local.StudySessionEntity
import com.hifzsniper.app.data.local.TafsirEntity
import com.hifzsniper.app.data.local.TranslationEntity
import com.hifzsniper.app.data.local.WeakPointEntity
import com.hifzsniper.app.data.local.WordEntity
import com.hifzsniper.app.data.local.WordTimingEntity
import com.hifzsniper.app.domain.CardType
import com.hifzsniper.app.domain.PlayerWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningRepositoryBridgeTest {

    @Test
    fun `createBridgeBetweenVerses creates a valid bridge card and saves it`() = runBlocking {
        val fakeContent = object : ContentDao {
            override fun observeStats(): Flow<ContentStatsEntity> = flowOf()
            override fun observeSurahs(): Flow<List<SurahEntity>> = flowOf()
            override fun observeAyahs(surahId: Int): Flow<List<AyahEntity>> = flowOf()
            override fun observeAyah(surahId: Int, ayahNumber: Int): Flow<AyahEntity?> = flowOf()
            override fun observeWords(verseKey: String): Flow<List<WordEntity>> = flowOf()
            override fun observeFrenchTranslation(verseKey: String): Flow<TranslationEntity?> = flowOf()
            override fun observeFrenchTafsir(verseKey: String): Flow<TafsirEntity?> = flowOf()
            override fun observeAlafasyTrack(verseKey: String): Flow<AudioTrackEntity?> = flowOf()
            override fun observeWordTimings(audioTrackId: String): Flow<List<WordTimingEntity>> = flowOf()
            override suspend fun contentVersion(): String? = "1.0"
            override suspend fun insertSurahs(items: List<SurahEntity>) {}
            override suspend fun insertAyahs(items: List<AyahEntity>) {}
            override suspend fun insertWords(items: List<WordEntity>) {}
            override suspend fun insertAudioTracks(items: List<AudioTrackEntity>) {}
            override suspend fun insertWordTimings(items: List<WordTimingEntity>) {}
            override suspend fun insertTranslations(items: List<TranslationEntity>) {}
            override suspend fun insertTafsirs(items: List<TafsirEntity>) {}
            override suspend fun insertDownloads(items: List<DownloadItemEntity>) {}
            override suspend fun deleteWordTimings() {}
            override suspend fun deleteAudioTracks() {}
            override suspend fun deleteWords() {}
            override suspend fun deleteTranslations() {}
            override suspend fun deleteTafsirs() {}
            override suspend fun deleteAyahs() {}
            override suspend fun deleteSurahs() {}
            override suspend fun deleteAudioDownloads() {}

            override fun observeSurah(id: Int): Flow<SurahEntity?> = flowOf(null)
            override suspend fun getSurah(id: Int): SurahEntity? = null
            override suspend fun getFirstAyahForPage(page: Int): AyahEntity? = null
            override fun observeAyahsForPage(page: Int): Flow<List<AyahEntity>> = flowOf(emptyList())
            override suspend fun getAyahsForPage(page: Int): List<AyahEntity> = emptyList()
            override fun observePageNumbers(): Flow<List<Int>> = flowOf(emptyList())
            override suspend fun getAyahsForSurah(surahId: Int): List<AyahEntity> = emptyList()
            override suspend fun getAllAyahs(): List<AyahEntity> = emptyList()

            override suspend fun getAyah(surahId: Int, ayahNumber: Int): AyahEntity? = null
            override suspend fun getWordsForVerse(verseKey: String): List<WordEntity> = when (verseKey) {
                "1:1" -> listOf(
                    WordEntity("1:1:1", "1:1", 1, "بِسْمِ", null, null),
                    WordEntity("1:1:2", "1:1", 2, "ٱللَّهِ", null, null),
                    WordEntity("1:1:3", "1:1", 3, "ٱلرَّحْمَـٰنِ", null, null),
                    WordEntity("1:1:4", "1:1", 4, "ٱلرَّحِيمِ", null, null)
                )
                "1:2" -> listOf(
                    WordEntity("1:2:1", "1:2", 1, "ٱلْحَمْدُ", null, null),
                    WordEntity("1:2:2", "1:2", 2, "لِلَّهِ", null, null),
                    WordEntity("1:2:3", "1:2", 3, "رَبِّ", null, null),
                    WordEntity("1:2:4", "1:2", 4, "ٱلْعَـٰلَمِينَ", null, null)
                )
                else -> emptyList()
            }
        }

        val savedCards = mutableListOf<LearningCardEntity>()
        val fakeLearning = object : LearningDao {
            override fun observeCards(): Flow<List<LearningCardEntity>> = flowOf(savedCards)
            override fun observeReviewLogs(): Flow<List<ReviewLogEntity>> = flowOf(emptyList())
            override fun observeStudySessions(): Flow<List<StudySessionEntity>> = flowOf(emptyList())
            override suspend fun card(id: String): LearningCardEntity? = savedCards.firstOrNull { it.id == id }
            override suspend fun insertCards(cards: List<LearningCardEntity>): List<Long> = cards.map { 1L }
            override suspend fun deleteTechnicalFixtures(): Int = 0
            override suspend fun activateAyahCards(surahId: Int, ayahNumber: Int, nextReviewAt: Long): Int = 1
            override suspend fun updateCard(card: LearningCardEntity) {}
            override suspend fun insertReviewLog(log: ReviewLogEntity) {}
            override suspend fun insertStudySession(session: StudySessionEntity) {}
            override suspend fun insertWeakPoint(weakPoint: WeakPointEntity): Long = 1L
            override fun observeActiveWeakPoints(): Flow<List<WeakPointEntity>> = flowOf(emptyList())
            override suspend fun resolveWeakPoint(id: String, resolvedAt: Long) {}
            override suspend fun upsertCard(card: LearningCardEntity) { savedCards.add(card) }
            override suspend fun upsertCards(cards: List<LearningCardEntity>) { savedCards.addAll(cards) }
            override suspend fun allCards(): List<LearningCardEntity> = savedCards
            override suspend fun allReviewLogs(): List<ReviewLogEntity> = emptyList()
            override suspend fun allWeakPoints(): List<WeakPointEntity> = emptyList()
            override suspend fun allStudySessions(): List<StudySessionEntity> = emptyList()
            override suspend fun insertReviewLogs(logs: List<ReviewLogEntity>) {}
            override suspend fun insertWeakPoints(items: List<WeakPointEntity>) {}
            override suspend fun insertStudySessions(items: List<StudySessionEntity>) {}
            override suspend fun clearAllCards() { savedCards.clear() }
            override suspend fun clearAllReviewLogs() {}
            override suspend fun clearAllWeakPoints() {}
            override suspend fun clearAllStudySessions() {}
        }

        val repository = LearningRepository(dao = fakeLearning, contentDao = fakeContent)
        val bridgeCard = repository.createBridgeBetweenVerses("1:1", "1:2", beforeCount = 3, afterCount = 3)

        assertNotNull(bridgeCard)
        assertEquals(CardType.BRIDGE, bridgeCard?.scheduling?.type)
        assertEquals("bridge-1-1-1-2", bridgeCard?.id)
        assertTrue(bridgeCard?.answer?.contains("ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ ٱلْحَمْدُ لِلَّهِ") == true)
        assertEquals(1, savedCards.size)
    }

    @Test
    fun `createChunkCard creates and saves a valid chunk reinforcement card`() = runBlocking {
        val savedCards = mutableListOf<LearningCardEntity>()
        val fakeLearning = object : LearningDao {
            override fun observeCards(): Flow<List<LearningCardEntity>> = flowOf()
            override fun observeReviewLogs(): Flow<List<ReviewLogEntity>> = flowOf()
            override fun observeStudySessions(): Flow<List<StudySessionEntity>> = flowOf()
            override suspend fun card(id: String): LearningCardEntity? = null
            override suspend fun insertCards(cards: List<LearningCardEntity>): List<Long> = emptyList()
            override suspend fun deleteTechnicalFixtures(): Int = 0
            override suspend fun activateAyahCards(surahId: Int, ayahNumber: Int, nextReviewAt: Long): Int = 0
            override suspend fun updateCard(card: LearningCardEntity) {}
            override suspend fun insertReviewLog(log: ReviewLogEntity) {}
            override suspend fun insertStudySession(session: StudySessionEntity) {}
            override suspend fun insertWeakPoint(weakPoint: WeakPointEntity): Long = 1L
            override fun observeActiveWeakPoints(): Flow<List<WeakPointEntity>> = flowOf()
            override suspend fun resolveWeakPoint(id: String, resolvedAt: Long) {}
            override suspend fun upsertCard(card: LearningCardEntity) { savedCards.add(card) }
            override suspend fun upsertCards(cards: List<LearningCardEntity>) {}
            override suspend fun allCards(): List<LearningCardEntity> = savedCards
            override suspend fun allReviewLogs(): List<ReviewLogEntity> = emptyList()
            override suspend fun allWeakPoints(): List<WeakPointEntity> = emptyList()
            override suspend fun allStudySessions(): List<StudySessionEntity> = emptyList()
            override suspend fun insertReviewLogs(logs: List<ReviewLogEntity>) {}
            override suspend fun insertWeakPoints(items: List<WeakPointEntity>) {}
            override suspend fun insertStudySessions(items: List<StudySessionEntity>) {}
            override suspend fun clearAllCards() {}
            override suspend fun clearAllReviewLogs() {}
            override suspend fun clearAllWeakPoints() {}
            override suspend fun clearAllStudySessions() {}
        }

        val repository = LearningRepository(dao = fakeLearning)
        val chunkWords = listOf(
            PlayerWord("w1", 1, "بِسْمِ", 0, 500),
            PlayerWord("w2", 2, "ٱللَّهِ", 500, 1000)
        )
        val card = repository.createChunkCard("1:1", chunkIndex = 1, totalChunks = 2, chunkWords = chunkWords)

        assertEquals("chunk-1-1-c1", card.id)
        assertEquals(CardType.CHUNK, card.scheduling.type)
        assertEquals("بِسْمِ ٱللَّهِ", card.answer)
        assertTrue(card.scheduling.isWeak)
        assertEquals(1, savedCards.size)
    }
}

