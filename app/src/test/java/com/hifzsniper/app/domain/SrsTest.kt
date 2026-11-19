package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SrsTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val card = LearningCard(id = "demo", type = CardType.AYAH, nextReviewAt = now)
    private val engine = SrsEngine()

    @Test fun firstFluentReviewSchedulesWithinSameDay() {
        val updated = engine.review(card, Rating.FLUENT, now)
        assertEquals(1, updated.successCount)
        assertEquals(now.plusSeconds(10 * 60), updated.nextReviewAt)
    }

    @Test fun failedCardBecomesWeakAfterThreshold() {
        val once = engine.review(card, Rating.FAILED, now)
        val twice = engine.review(once, Rating.FAILED, now)
        assertTrue(twice.isWeak)
        assertEquals(2, twice.lapseCount)
    }

    @Test fun bridgeWeakCardHasHigherPriority() {
        val weakBridge = card.copy(type = CardType.BRIDGE, isWeak = true)
        assertTrue(engine.priority(weakBridge, now) > engine.priority(card, now))
    }

    @Test fun hesitantReviewRemainsShortAndTracksHesitation() {
        val updated = engine.review(card, Rating.HESITANT, now)
        assertEquals(now.plusSeconds(30 * 60), updated.nextReviewAt)
        assertEquals(1, updated.hesitationCount)
        assertEquals(Rating.HESITANT, updated.lastResult)
    }

    @Test fun consecutiveSuccessesUseTenMinutesThenOneThreeAndSevenDays() {
        val first = engine.review(card, Rating.FLUENT, now)
        val second = engine.review(first, Rating.FLUENT, now)
        val third = engine.review(second, Rating.FLUENT, now)
        val fourth = engine.review(third, Rating.FLUENT, now)
        assertEquals(1.0, second.intervalDays, 0.0)
        assertEquals(3.0, third.intervalDays, 0.0)
        assertEquals(7.0, fourth.intervalDays, 0.0)
    }

    @Test fun lapseAfterStablePeriodReducesButDoesNotEraseStability() {
        val stable = card.copy(intervalDays = 30.0, stability = 20.0, successCount = 8)
        val updated = engine.review(stable, Rating.FAILED, now)
        assertEquals(10.5, updated.intervalDays, 0.0)
        assertEquals(13.0, updated.stability, 0.0)
    }

    @Test fun planSessionHonoursBudgetAndRemovesDuplicateIds() {
        val cards = listOf(
            card.copy(id = "a", estimatedDurationSeconds = 180, isWeak = true),
            card.copy(id = "a", estimatedDurationSeconds = 180),
            card.copy(id = "b", estimatedDurationSeconds = 150),
            card.copy(id = "c", estimatedDurationSeconds = 60)
        )
        val planned = engine.planSession(cards, now, budgetSeconds = 300)
        assertTrue(planned.sumOf { it.estimatedDurationSeconds } <= 300)
        assertEquals(planned.map { it.id }.distinct(), planned.map { it.id })
    }

    @Test fun suspendedCardsAreNeverPlanned() {
        val planned = engine.planSession(listOf(card.copy(suspended = true)), now, 300)
        assertTrue(planned.isEmpty())
    }

    @Test fun firstActiveRecallUnsuspendsTheLearnedCard() {
        val learned = engine.review(card.copy(suspended = true), Rating.HESITANT, now)
        assertTrue(!learned.suspended)
    }
}
