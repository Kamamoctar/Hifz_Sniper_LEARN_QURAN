package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecitationMatcherTest {

    @Test
    fun `exact recitation gives 100 percent accuracy and FLUENT rating`() {
        val expected = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
        val spoken = "الحمد لله رب العالمين"

        val result = RecitationMatcher.evaluate(expected, spoken)

        assertEquals(100, result.accuracyPercentage)
        assertEquals(Rating.FLUENT, result.recommendedRating)
        assertEquals(4, result.matchedCount)
        assertEquals(0, result.missedCount)
        assertTrue(result.wordFeedbacks.all { it.status == RecitationMatcher.WordMatchStatus.MATCHED })
    }

    @Test
    fun `empty spoken text gives 0 percent accuracy and FAILED rating`() {
        val expected = "قُلْ هُوَ اللَّهُ أَحَدٌ"
        val result = RecitationMatcher.evaluate(expected, "")

        assertEquals(0, result.accuracyPercentage)
        assertEquals(Rating.FAILED, result.recommendedRating)
        assertEquals(4, result.missedCount)
    }

    @Test
    fun `recitation with minor hesitation gives HESITANT rating`() {
        // 4 words, 2 matched, 2 missed
        val expected = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ"
        val spoken = "اياك نعبد"

        val result = RecitationMatcher.evaluate(expected, spoken)

        assertTrue(result.accuracyPercentage in 40..60)
        assertEquals(Rating.FAILED, result.recommendedRating)
        assertEquals(2, result.matchedCount)
        assertEquals(2, result.missedCount)
    }

    @Test
    fun `recitation with 5 of 6 words gives HESITANT rating`() {
        val expected = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ مِن شَرِّ"
        val spoken = "قل اعوذ برب الفلق من" // 5 of 6

        val result = RecitationMatcher.evaluate(expected, spoken)

        assertTrue(result.accuracyPercentage >= 70)
        assertEquals(Rating.HESITANT, result.recommendedRating)
        assertEquals(5, result.matchedCount)
        assertEquals(1, result.missedCount)
    }

    @Test
    fun `recitation with approximate pronunciation is scored appropriately`() {
        val expected = "الرَّحْمَٰنِ الرَّحِيمِ"
        val spoken = "الرحمن الرحيم"

        val result = RecitationMatcher.evaluate(expected, spoken)

        assertEquals(100, result.accuracyPercentage)
        assertEquals(Rating.FLUENT, result.recommendedRating)
    }
}

