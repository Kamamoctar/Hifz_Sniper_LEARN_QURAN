package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MutashabihatEngineTest {

    @Test
    fun `normalizeArabic removes diacritics and unifies letter variants`() {
        val original = "وَاتَّقُوا يَوْمًا لَّا تَجْزِي نَفْسٌ عَن نَّفْسٍ"
        val normalized = MutashabihatEngine.normalizeArabic(original)
        assertFalse(normalized.contains('\u064E')) // Fatha
        assertFalse(normalized.contains('\u0651')) // Shadda
        assertFalse(normalized.contains('\u0652')) // Sukun
        assertTrue(normalized.startsWith("واتقوا يوما"))
    }

    @Test
    fun `findMatches detects famous prefix mirror in Al-Baqarah 48 and 123`() {
        val ayah48 = "وَاتَّقُوا يَوْمًا لَّا تَجْزِي نَفْسٌ عَن نَّفْسٍ شَيْئًا وَلَا يُقْبَلُ مِنْهَا شَفَاعَةٌ وَلَا يُؤْخَذُ مِنْهَا عَدْلٌ وَلَا هُمْ يُنصَرُونَ"
        val ayah123 = "وَاتَّقُوا يَوْمًا لَّا تَجْزِي نَفْسٌ عَن نَّفْسٍ شَيْئًا وَلَا يُقْبَلُ مِنْهَا عَدْلٌ وَلَا تَنفَعُهَا شَفَاعَةٌ وَلَا هُمْ يُنصَرُونَ"

        val candidates = listOf(
            AyahCompareCandidate(
                verseKey = "2:123",
                surahId = 2,
                ayahNumber = 123,
                surahNameArabic = "البقرة",
                surahNameTransliterated = "Al-Baqarah",
                textUthmani = ayah123
            ),
            AyahCompareCandidate(
                verseKey = "112:1",
                surahId = 112,
                ayahNumber = 1,
                surahNameArabic = "الإخلاص",
                surahNameTransliterated = "Al-Ikhlas",
                textUthmani = "قُلْ هُوَ اللَّهُ أَحَدٌ"
            )
        )

        val matches = MutashabihatEngine.findMatches(
            sourceVerseKey = "2:48",
            sourceTextUthmani = ayah48,
            allAyahs = candidates
        )

        assertEquals(1, matches.size)
        val match = matches.first()
        assertEquals("2:123", match.targetVerseKey)
        assertEquals(MutashabihType.PREFIX, match.matchType)
        assertTrue(match.similarityPercentage >= 80)
        assertTrue(match.divergenceDescription.contains("Même début"))
    }

    @Test
    fun `findMatches detects suffix matching endings`() {
        val verse1 = "ذَٰلِكَ بِأَنَّ اللَّهَ مَوْلَى الَّذِينَ آمَنُوا وَأَنَّ الْكَافِرِينَ لَا مَوْلَىٰ لَهُمْ"
        val verse2 = "إِنَّ الَّذِينَ كَفَرُوا وَصَدُّوا عَن سَبِيلِ اللَّهِ لَا مَوْلَىٰ لَهُمْ"

        val candidates = listOf(
            AyahCompareCandidate(
                verseKey = "47:11",
                surahId = 47,
                ayahNumber = 11,
                textUthmani = verse2
            )
        )

        val matches = MutashabihatEngine.findMatches(
            sourceVerseKey = "47:10",
            sourceTextUthmani = verse1,
            allAyahs = candidates
        )

        assertEquals(1, matches.size)
        assertEquals(MutashabihType.SUFFIX, matches.first().matchType)
    }

    @Test
    fun `findMatches returns empty on completely unrelated verses`() {
        val fatiha = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
        val ikhlas = listOf(
            AyahCompareCandidate(
                verseKey = "112:1",
                surahId = 112,
                ayahNumber = 1,
                textUthmani = "قُلْ هُوَ اللَّهُ أَحَدٌ"
            )
        )

        val matches = MutashabihatEngine.findMatches("1:2", fatiha, ikhlas)
        assertTrue(matches.isEmpty())
    }
}

