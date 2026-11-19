package com.hifzsniper.app.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranTypographyTest {

    @Test
    fun `formatArabicDigits converts Latin digits to Eastern Arabic digits`() {
        assertEquals("٠", formatArabicDigits(0))
        assertEquals("١", formatArabicDigits(1))
        assertEquals("٧", formatArabicDigits(7))
        assertEquals("٤٢", formatArabicDigits(42))
        assertEquals("١١٤", formatArabicDigits(114))
        assertEquals("٦٢٣٦", formatArabicDigits(6236))
    }

    @Test
    fun `ayahEndSign wraps Arabic digits with authentic Quranic ayah end symbol`() {
        val signForAyah1 = ayahEndSign(1)
        assertTrue(signForAyah1.contains("\u06DD"))
        assertTrue(signForAyah1.contains("١"))

        val signForAyah7 = ayahEndSign(7)
        assertTrue(signForAyah7.contains("\u06DD"))
        assertTrue(signForAyah7.contains("٧"))
    }
}

