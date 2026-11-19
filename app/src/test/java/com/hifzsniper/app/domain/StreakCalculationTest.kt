package com.hifzsniper.app.domain

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakCalculationTest {

    private val now = Instant.parse("2026-11-12T12:00:00Z")

    @Test
    fun `empty reviews returns zero streak and 28 empty days`() {
        val result = StreakCalculator.computeStreaks(emptyList(), now)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.bestStreak)
        assertEquals(28, result.recentActivity.size)
        assertTrue(result.recentActivity.all { it.count == 0 })
    }

    @Test
    fun `reviewing today gives 1 day streak`() {
        val reviews = listOf(now.minus(1, ChronoUnit.HOURS))
        val result = StreakCalculator.computeStreaks(reviews, now)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.bestStreak)
        assertEquals(1, result.recentActivity.last().count)
        assertTrue(result.recentActivity.last().isToday)
    }

    @Test
    fun `reviewing yesterday and today gives 2 days streak`() {
        val reviews = listOf(
            now.minus(1, ChronoUnit.DAYS),
            now.minus(2, ChronoUnit.HOURS)
        )
        val result = StreakCalculator.computeStreaks(reviews, now)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.bestStreak)
    }

    @Test
    fun `reviewing yesterday but not yet today maintains current streak`() {
        val reviews = listOf(
            now.minus(2, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.DAYS)
        )
        val result = StreakCalculator.computeStreaks(reviews, now)
        assertEquals(2, result.currentStreak)
    }

    @Test
    fun `gap in reviews breaks current streak but preserves best streak`() {
        val reviews = listOf(
            now.minus(10, ChronoUnit.DAYS),
            now.minus(9, ChronoUnit.DAYS),
            now.minus(8, ChronoUnit.DAYS), // 3 days in a row in the past
            now.minus(1, ChronoUnit.DAYS),
            now.minus(1, ChronoUnit.HOURS)  // 2 days in a row now
        )
        val result = StreakCalculator.computeStreaks(reviews, now)
        assertEquals(2, result.currentStreak)
        assertEquals(3, result.bestStreak)
    }
}

