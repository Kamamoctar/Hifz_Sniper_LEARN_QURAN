package com.hifzsniper.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

object StreakCalculator {
    fun computeStreaks(
        reviewedInstants: List<Instant>,
        now: Instant,
        zone: ZoneOffset = ZoneOffset.UTC
    ): StreakResult {
        val today = now.atZone(zone).toLocalDate()
        val reviewsByDate = reviewedInstants.groupBy { it.atZone(zone).toLocalDate() }

        val recentActivity = (27 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            DayActivity(
                dateString = date.toString(),
                count = reviewsByDate[date]?.size ?: 0,
                isToday = daysAgo == 0
            )
        }

        var currentStreak = 0
        var checkDate = if (reviewsByDate.containsKey(today) && (reviewsByDate[today]?.isNotEmpty() == true)) today else today.minusDays(1)
        while (reviewsByDate.containsKey(checkDate) && (reviewsByDate[checkDate]?.isNotEmpty() == true)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        val activeDates = reviewsByDate.filter { it.value.isNotEmpty() }.keys.sorted()
        var bestStreak = 0
        var running = 0
        var prevDate: LocalDate? = null
        for (d in activeDates) {
            if (prevDate != null && d == prevDate.plusDays(1)) {
                running++
            } else {
                running = 1
            }
            if (running > bestStreak) bestStreak = running
            prevDate = d
        }

        return StreakResult(
            currentStreak = currentStreak,
            bestStreak = maxOf(bestStreak, currentStreak),
            recentActivity = recentActivity
        )
    }
}

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int,
    val recentActivity: List<DayActivity>
)

