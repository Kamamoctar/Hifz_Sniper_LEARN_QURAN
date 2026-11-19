package com.hifzsniper.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BasmalaPolicyTest {
    @Test
    fun `bundled Tanzil opening is detected despite Uthmani marks`() {
        assertTrue(
            BasmalaPolicy.sourceContainsOpeningBasmala(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ الٓمٓ"
            )
        )
    }

    @Test
    fun `Al Fatiha never receives an extra standalone basmala`() {
        assertFalse(
            BasmalaPolicy.shouldRenderStandalone(
                surahId = 1,
                ayahNumber = 1,
                ayahText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
            )
        )
    }

    @Test
    fun `At Tawbah never receives a standalone basmala`() {
        assertFalse(BasmalaPolicy.shouldRenderStandalone(9, 1, "بَرَآءَةٌ مِّنَ ٱللَّهِ"))
    }

    @Test
    fun `embedded basmala is not duplicated in other surahs`() {
        assertFalse(
            BasmalaPolicy.shouldRenderStandalone(
                2,
                1,
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ الٓمٓ"
            )
        )
    }

    @Test
    fun `shouldRenderHeaderBasmala is true for Surah 2 and 114 but false for Surah 1 and 9`() {
        assertFalse(BasmalaPolicy.shouldRenderHeaderBasmala(1))
        assertTrue(BasmalaPolicy.shouldRenderHeaderBasmala(2))
        assertFalse(BasmalaPolicy.shouldRenderHeaderBasmala(9))
        assertTrue(BasmalaPolicy.shouldRenderHeaderBasmala(114))
    }

    @Test
    fun `formatAyahTextForMushaf strips opening basmala from verse 1 of Surah 2`() {
        val original = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ الٓمٓ"
        val formatted = BasmalaPolicy.formatAyahTextForMushaf(2, 1, original)
        org.junit.Assert.assertEquals("الٓمٓ", formatted)
    }

    @Test
    fun `formatAyahTextForMushaf preserves Al-Fatiha and At-Tawbah and non-first ayahs`() {
        val fatiha1 = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        org.junit.Assert.assertEquals(fatiha1, BasmalaPolicy.formatAyahTextForMushaf(1, 1, fatiha1))

        val tawbah1 = "بَرَآءَةٌ مِّنَ ٱللَّهِ"
        org.junit.Assert.assertEquals(tawbah1, BasmalaPolicy.formatAyahTextForMushaf(9, 1, tawbah1))

        val baqarah2 = "ذَٰلِكَ ٱلْكِتَـٰبُ لَا رَيْبَ"
        org.junit.Assert.assertEquals(baqarah2, BasmalaPolicy.formatAyahTextForMushaf(2, 2, baqarah2))
    }
}
