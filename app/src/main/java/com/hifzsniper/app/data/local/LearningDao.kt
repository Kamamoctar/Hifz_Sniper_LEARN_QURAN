package com.hifzsniper.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_card ORDER BY nextReviewAt ASC")
    fun observeCards(): Flow<List<LearningCardEntity>>

    @Query("SELECT * FROM review_log ORDER BY reviewedAt DESC")
    fun observeReviewLogs(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM study_session ORDER BY startedAt DESC")
    fun observeStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM learning_card WHERE id = :id")
    suspend fun card(id: String): LearningCardEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCards(cards: List<LearningCardEntity>): List<Long>

    @Query("DELETE FROM learning_card WHERE userNote = 'Donnée de test, pas un verset.'")
    suspend fun deleteTechnicalFixtures(): Int

    @Query(
        """UPDATE learning_card
            SET suspended = 0, nextReviewAt = :nextReviewAt
            WHERE type = 'AYAH' AND surahId = :surahId
              AND ayahStart = :ayahNumber AND ayahEnd = :ayahNumber"""
    )
    suspend fun activateAyahCards(surahId: Int, ayahNumber: Int, nextReviewAt: Long): Int

    @Update
    suspend fun updateCard(card: LearningCardEntity)

    @Insert
    suspend fun insertReviewLog(log: ReviewLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeakPoint(weakPoint: WeakPointEntity): Long

    @Query("SELECT * FROM weak_point WHERE resolvedAt IS NULL ORDER BY severity DESC, createdAt DESC")
    fun observeActiveWeakPoints(): Flow<List<WeakPointEntity>>

    @Query("UPDATE weak_point SET resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun resolveWeakPoint(id: String, resolvedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: LearningCardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(cards: List<LearningCardEntity>)

    @Query("SELECT * FROM learning_card")
    suspend fun allCards(): List<LearningCardEntity>

    @Query("SELECT * FROM review_log")
    suspend fun allReviewLogs(): List<ReviewLogEntity>

    @Query("SELECT * FROM weak_point")
    suspend fun allWeakPoints(): List<WeakPointEntity>

    @Query("SELECT * FROM study_session")
    suspend fun allStudySessions(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLogs(logs: List<ReviewLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakPoints(items: List<WeakPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(items: List<StudySessionEntity>)

    @Query("DELETE FROM learning_card")
    suspend fun clearAllCards()

    @Query("DELETE FROM review_log")
    suspend fun clearAllReviewLogs()

    @Query("DELETE FROM weak_point")
    suspend fun clearAllWeakPoints()

    @Query("DELETE FROM study_session")
    suspend fun clearAllStudySessions()

    @Transaction
    suspend fun restoreUserData(
        cards: List<LearningCardEntity>,
        logs: List<ReviewLogEntity>,
        weakPoints: List<WeakPointEntity>,
        sessions: List<StudySessionEntity>
    ) {
        clearAllCards()
        clearAllReviewLogs()
        clearAllWeakPoints()
        clearAllStudySessions()
        upsertCards(cards)
        insertReviewLogs(logs)
        insertWeakPoints(weakPoints)
        insertStudySessions(sessions)
    }

    @Transaction
    suspend fun persistReview(
        card: LearningCardEntity,
        log: ReviewLogEntity,
        weakPoint: WeakPointEntity?,
        session: StudySessionEntity
    ) {
        updateCard(card)
        insertReviewLog(log)
        insertStudySession(session)
        if (weakPoint != null) insertWeakPoint(weakPoint)
    }
}
