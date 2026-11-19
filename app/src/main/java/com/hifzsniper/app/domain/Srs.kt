package com.hifzsniper.app.domain

import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

enum class CardType { AYAH, CHUNK, BRIDGE, WEAK_WORD, LISTEN_ONLY, SEMANTIC_CUE }
enum class Rating { FAILED, HESITANT, FLUENT }

data class LearningCard(
    val id: String,
    val type: CardType,
    val surahId: Int = 0,
    val ayahStart: Int = 0,
    val ayahEnd: Int = 0,
    val wordStart: Int? = null,
    val wordEnd: Int? = null,
    val createdAt: Instant = Instant.EPOCH,
    val lastReviewedAt: Instant? = null,
    val nextReviewAt: Instant,
    val intervalDays: Double = 0.0,
    val stability: Double = 1.0,
    val difficulty: Double = 0.5,
    val lapseCount: Int = 0,
    val successCount: Int = 0,
    val hesitationCount: Int = 0,
    val isWeak: Boolean = false,
    val suspended: Boolean = false,
    val sourceCardId: String? = null,
    val userNote: String? = null,
    val estimatedDurationSeconds: Int = 30,
    val lastResult: Rating? = null,
    val algorithmVersion: Int = 1
)

data class SrsConfig(
    val firstSuccessMinutes: Long = 10,
    val successIntervalsDays: List<Double> = listOf(1.0, 3.0, 7.0),
    val weakLapseThreshold: Int = 2,
    val bridgeWeight: Double = 2.0,
    val overdueWeight: Double = 1.0,
    val weaknessWeight: Double = 3.0,
    val lapseWeight: Double = 1.5,
    val stabilityWeight: Double = 1.0,
    val durationPenalty: Double = 0.01
)

class SrsEngine(private val config: SrsConfig = SrsConfig()) {
    fun review(card: LearningCard, rating: Rating, now: Instant): LearningCard {
        return when (rating) {
            Rating.FAILED -> card.copy(
                lastReviewedAt = now,
                nextReviewAt = now.plusSeconds(5 * 60),
                intervalDays = max(0.0, card.intervalDays * 0.35),
                stability = max(0.5, card.stability * 0.65),
                lapseCount = card.lapseCount + 1,
                isWeak = card.isWeak || card.lapseCount + 1 >= config.weakLapseThreshold,
                suspended = false,
                lastResult = rating
            )
            Rating.HESITANT -> card.copy(
                lastReviewedAt = now,
                nextReviewAt = now.plusSeconds(30 * 60),
                intervalDays = max(0.25, card.intervalDays * 0.6),
                stability = max(0.75, card.stability * 0.85),
                hesitationCount = card.hesitationCount + 1,
                suspended = false,
                lastResult = rating
            )
            Rating.FLUENT -> {
                val nextInterval = when (card.successCount) {
                    0 -> config.firstSuccessMinutes / (24.0 * 60.0)
                    1 -> config.successIntervalsDays[0]
                    2 -> config.successIntervalsDays[1]
                    3 -> config.successIntervalsDays[2]
                    else -> max(1.0, card.intervalDays * (1.8 + card.stability * 0.1))
                }
                card.copy(
                    lastReviewedAt = now,
                    nextReviewAt = now.plusSeconds((nextInterval * 24 * 60 * 60).toLong()),
                    intervalDays = nextInterval,
                    stability = min(100.0, card.stability + 0.8),
                    successCount = card.successCount + 1,
                    suspended = false,
                    lastResult = rating
                )
            }
        }
    }

    fun priority(card: LearningCard, now: Instant): Double {
        val overdueDays = max(0.0, Duration.between(card.nextReviewAt, now).toMinutes() / (60.0 * 24.0))
        return overdueDays * config.overdueWeight +
            (if (card.isWeak) config.weaknessWeight else 0.0) +
            card.lapseCount * config.lapseWeight +
            (if (card.type == CardType.BRIDGE) config.bridgeWeight else 0.0) +
            (1.0 / max(0.1, card.stability)) * config.stabilityWeight -
            card.estimatedDurationSeconds * config.durationPenalty
    }

    fun planSession(cards: List<LearningCard>, now: Instant, budgetSeconds: Int): List<LearningCard> {
        if (budgetSeconds <= 0) return emptyList()
        var remaining = budgetSeconds
        val selected = mutableListOf<LearningCard>()
        cards.asSequence()
            .filterNot { it.suspended }
            .distinctBy { it.id }
            .sortedByDescending { priority(it, now) }
            .forEach { card ->
                if (card.estimatedDurationSeconds <= remaining) {
                    selected += card
                    remaining -= card.estimatedDurationSeconds
                }
            }
        return selected
    }
}
