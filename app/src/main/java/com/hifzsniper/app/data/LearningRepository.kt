package com.hifzsniper.app.data

import com.hifzsniper.app.data.local.ContentDao
import com.hifzsniper.app.data.local.LearningCardEntity
import com.hifzsniper.app.data.local.LearningDao
import com.hifzsniper.app.data.local.ReviewLogEntity
import com.hifzsniper.app.data.local.StudySessionEntity
import com.hifzsniper.app.data.local.WeakPointEntity
import com.hifzsniper.app.domain.BridgeBuilder
import com.hifzsniper.app.domain.CardType
import com.hifzsniper.app.domain.ContentPackage
import com.hifzsniper.app.domain.LearningCard
import com.hifzsniper.app.domain.PlayerWord
import com.hifzsniper.app.domain.Rating
import com.hifzsniper.app.domain.RecitationWord
import com.hifzsniper.app.domain.ReviewEvent
import com.hifzsniper.app.domain.SrsEngine
import com.hifzsniper.app.domain.StudyCard
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LearningRepository(
    private val dao: LearningDao,
    private val contentDao: ContentDao? = null,
    private val engine: SrsEngine = SrsEngine(),
    private val clock: Clock = Clock.systemUTC()
) {
    val cards: Flow<List<StudyCard>> = dao.observeCards().map { list -> list.map { it.toDomain() } }
    val reviewEvents: Flow<List<ReviewEvent>> = dao.observeReviewLogs().map { list ->
        list.map { ReviewEvent(it.cardId, Instant.ofEpochMilli(it.reviewedAt), Rating.valueOf(it.rating)) }
    }

    suspend fun seedCorpusCards(content: ContentPackage, selectedSurah: Int, selectedAyah: Int): Int {
        val now = clock.instant().toEpochMilli()
        val names = content.surahs.associate { it.id to it.nameTransliterated }
        val entities = content.ayahs.map { ayah ->
            val selected = ayah.surahId == selectedSurah && ayah.ayahNumber == selectedAyah
            LearningCardEntity(
                id = "ayah-${ayah.surahId}-${ayah.ayahNumber}",
                type = CardType.AYAH.name,
                surahId = ayah.surahId,
                ayahStart = ayah.ayahNumber,
                ayahEnd = ayah.ayahNumber,
                wordStart = null,
                wordEnd = null,
                prompt = "Récite ${ayah.verseKey} — ${names[ayah.surahId].orEmpty()}",
                answer = ayah.textUthmani,
                createdAt = now,
                lastReviewedAt = null,
                nextReviewAt = if (selected) now else FAR_FUTURE_MILLIS,
                intervalDays = 0.0,
                stability = 1.0,
                difficulty = 0.5,
                lapseCount = 0,
                successCount = 0,
                hesitationCount = 0,
                isWeak = false,
                suspended = !selected,
                sourceCardId = null,
                userNote = "Tanzil Quran Text 1.1 · CC BY 3.0 · https://tanzil.net",
                estimatedDurationSeconds = 45,
                lastResult = null,
                algorithmVersion = 1
            )
        }
        dao.deleteTechnicalFixtures()
        return dao.insertCards(entities).count { it != -1L }
    }

    suspend fun activatePassage(surah: Int, ayah: Int): Boolean =
        dao.activateAyahCards(surah, ayah, clock.instant().toEpochMilli()) > 0

    suspend fun review(cardId: String, rating: Rating, wasOffline: Boolean = true): Boolean {
        val previous = dao.card(cardId) ?: return false
        val previousDomain = previous.toDomain().scheduling
        val now = clock.instant()
        val updated = engine.review(previousDomain, rating, now)
        val sessionId = UUID.randomUUID().toString()
        val estimatedSeconds = previous.estimatedDurationSeconds.coerceAtLeast(1)
        dao.persistReview(
            previous.fromDomain(updated),
            ReviewLogEntity(
                cardId = cardId,
                reviewedAt = now.toEpochMilli(),
                rating = rating.name,
                previousInterval = previous.intervalDays,
                newInterval = updated.intervalDays,
                previousStability = previous.stability,
                newStability = updated.stability,
                wasOffline = wasOffline,
                responseTimeMs = null,
                sessionId = sessionId
            ),
            weakPoint = if (updated.isWeak && !previous.isWeak) {
                WeakPointEntity(
                    id = UUID.randomUUID().toString(),
                    verseKey = "${updated.surahId}:${updated.ayahStart}",
                    wordStart = updated.wordStart,
                    wordEnd = updated.wordEnd,
                    reason = "Échecs répétés",
                    severity = 2,
                    createdAt = now.toEpochMilli(),
                    resolvedAt = null,
                    linkedCardId = updated.id
                )
            } else null,
            session = StudySessionEntity(
                id = sessionId,
                startedAt = now.minusSeconds(estimatedSeconds.toLong()).toEpochMilli(),
                endedAt = now.toEpochMilli(),
                plannedMinutes = 0,
                actualSeconds = estimatedSeconds,
                newCardsCount = if (previous.lastReviewedAt == null) 1 else 0,
                reviewCardsCount = if (previous.lastReviewedAt == null) 0 else 1,
                successCount = if (rating == Rating.FLUENT) 1 else 0,
                failureCount = if (rating == Rating.FAILED) 1 else 0,
                sessionType = "CARD_REVIEW"
            )
        )
        return true
    }

    suspend fun createBridgeBetweenVerses(
        fromVerseKey: String,
        toVerseKey: String,
        beforeCount: Int = 3,
        afterCount: Int = 3
    ): StudyCard? {
        val content = contentDao ?: return null
        val fromWords = content.getWordsForVerse(fromVerseKey)
        val toWords = content.getWordsForVerse(toVerseKey)
        if (fromWords.isEmpty() || toWords.isEmpty()) return null

        val bridgeBuilder = BridgeBuilder(defaultContextWords = 3)
        val draft = bridgeBuilder.build(
            previousVerseWords = fromWords.map { RecitationWord(it.id, it.verseKey, it.positionInAyah, it.textUthmani) },
            nextVerseWords = toWords.map { RecitationWord(it.id, it.verseKey, it.positionInAyah, it.textUthmani) },
            beforeCount = beforeCount.coerceAtMost(fromWords.size),
            afterCount = afterCount.coerceAtMost(toWords.size)
        )

        val (fromSurah, fromAyah) = fromVerseKey.split(':').map(String::toInt)
        val (toSurah, toAyah) = toVerseKey.split(':').map(String::toInt)
        val now = clock.instant().toEpochMilli()

        val entity = LearningCardEntity(
            id = "bridge-${fromVerseKey.replace(':', '-')}-${toVerseKey.replace(':', '-')}",
            type = CardType.BRIDGE.name,
            surahId = fromSurah,
            ayahStart = fromAyah,
            ayahEnd = toAyah,
            wordStart = draft.beforeRange.first,
            wordEnd = draft.afterRange.last,
            prompt = "Pont de continuité : transition $fromVerseKey → $toVerseKey",
            answer = draft.recitableText,
            createdAt = now,
            lastReviewedAt = null,
            nextReviewAt = now,
            intervalDays = 0.0,
            stability = 1.0,
            difficulty = 0.5,
            lapseCount = 0,
            successCount = 0,
            hesitationCount = 0,
            isWeak = false,
            suspended = false,
            sourceCardId = "ayah-$fromSurah-$fromAyah",
            userNote = "Pont généré automatiquement",
            estimatedDurationSeconds = 25,
            lastResult = null,
            algorithmVersion = 1
        )
        dao.upsertCard(entity)
        return entity.toDomain()
    }

    suspend fun createChunkCard(
        verseKey: String,
        chunkIndex: Int,
        totalChunks: Int,
        chunkWords: List<PlayerWord>
    ): StudyCard {
        val (surah, ayah) = verseKey.split(':').map(String::toInt)
        val now = clock.instant().toEpochMilli()
        val id = "chunk-$surah-$ayah-c$chunkIndex"
        val entity = LearningCardEntity(
            id = id,
            type = CardType.CHUNK.name,
            surahId = surah,
            ayahStart = ayah,
            ayahEnd = ayah,
            wordStart = chunkWords.firstOrNull()?.position ?: 1,
            wordEnd = chunkWords.lastOrNull()?.position ?: 1,
            prompt = "Fragment $chunkIndex/$totalChunks — Verset $verseKey",
            answer = chunkWords.joinToString(" ") { it.text },
            createdAt = now,
            lastReviewedAt = null,
            nextReviewAt = now,
            intervalDays = 0.0,
            stability = 1.0,
            difficulty = 0.5,
            lapseCount = 0,
            successCount = 0,
            hesitationCount = 0,
            isWeak = true,
            suspended = false,
            sourceCardId = "ayah-$surah-$ayah",
            userNote = "Fragment à renforcer",
            estimatedDurationSeconds = 20,
            lastResult = null,
            algorithmVersion = 1
        )
        dao.upsertCard(entity)
        return entity.toDomain()
    }

    suspend fun recordWeakPoint(
        verseKey: String,
        reason: String,
        severity: Int = 1,
        wordStart: Int? = null,
        wordEnd: Int? = null,
        linkedCardId: String? = null
    ): Boolean {
        val entity = WeakPointEntity(
            id = UUID.randomUUID().toString(),
            verseKey = verseKey,
            wordStart = wordStart,
            wordEnd = wordEnd,
            reason = reason,
            severity = severity,
            createdAt = clock.instant().toEpochMilli(),
            resolvedAt = null,
            linkedCardId = linkedCardId
        )
        return dao.insertWeakPoint(entity) > 0
    }

    suspend fun exportBackup(preferences: UserPreferences): String {
        val cards = dao.allCards()
        val logs = dao.allReviewLogs()
        val weakPoints = dao.allWeakPoints()
        val sessions = dao.allStudySessions()
        return ProgressBackupService.exportToJson(cards, logs, weakPoints, sessions, preferences, clock.instant())
    }

    suspend fun restoreBackup(summary: BackupSummary): Boolean {
        if (!summary.isValid) return false
        dao.restoreUserData(summary.cards, summary.logs, summary.weakPoints, summary.sessions)
        return true
    }

    private fun LearningCardEntity.toDomain(): StudyCard = StudyCard(
        scheduling = LearningCard(
            id = id,
            type = CardType.valueOf(type),
            surahId = surahId,
            ayahStart = ayahStart,
            ayahEnd = ayahEnd,
            wordStart = wordStart,
            wordEnd = wordEnd,
            createdAt = Instant.ofEpochMilli(createdAt),
            lastReviewedAt = lastReviewedAt?.let(Instant::ofEpochMilli),
            nextReviewAt = Instant.ofEpochMilli(nextReviewAt),
            intervalDays = intervalDays,
            stability = stability,
            difficulty = difficulty,
            lapseCount = lapseCount,
            successCount = successCount,
            hesitationCount = hesitationCount,
            isWeak = isWeak,
            suspended = suspended,
            sourceCardId = sourceCardId,
            userNote = userNote,
            estimatedDurationSeconds = estimatedDurationSeconds,
            lastResult = lastResult?.let(Rating::valueOf),
            algorithmVersion = algorithmVersion
        ),
        prompt = prompt,
        answer = answer
    )

    private fun LearningCardEntity.fromDomain(card: LearningCard): LearningCardEntity = copy(
        lastReviewedAt = card.lastReviewedAt?.toEpochMilli(),
        nextReviewAt = card.nextReviewAt.toEpochMilli(),
        intervalDays = card.intervalDays,
        stability = card.stability,
        difficulty = card.difficulty,
        lapseCount = card.lapseCount,
        successCount = card.successCount,
        hesitationCount = card.hesitationCount,
        isWeak = card.isWeak,
        suspended = card.suspended,
        lastResult = card.lastResult?.name,
        algorithmVersion = card.algorithmVersion
    )

    companion object {
        private const val FAR_FUTURE_MILLIS = 253_402_300_799_000L
    }
}
