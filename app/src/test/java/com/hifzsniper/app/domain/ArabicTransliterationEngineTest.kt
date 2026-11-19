package com.hifzsniper.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTransliterationEngineTest {

    @Test
    fun `transliterates basmala accurately`() {
        val basmala = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ"
        val translit = ArabicTransliterationEngine.transliterate(basmala)

        assertFalse(translit.isBlank())
        assertTrue("Devrait commencer par Bismi: $translit", translit.startsWith("Bismi", ignoreCase = true))
        assertTrue("Devrait contenir llāhi: $translit", translit.contains("llāh", ignoreCase = true))
        assertTrue("Devrait contenir raḥmān: $translit", translit.contains("raḥmān", ignoreCase = true))
        assertTrue("Devrait contenir raḥīm: $translit", translit.contains("raḥīm", ignoreCase = true))
    }

    @Test
    fun `transliterates Al-Fatihah verse 2`() {
        val ayah = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"
        val translit = ArabicTransliterationEngine.transliterate(ayah)

        assertFalse(translit.isBlank())
        assertTrue("Devrait contenir ḥamd: $translit", translit.contains("ḥamd", ignoreCase = true))
        assertTrue("Devrait contenir lillāh: $translit", translit.contains("lillāh", ignoreCase = true))
        assertTrue("Devrait contenir rabb: $translit", translit.contains("rabb", ignoreCase = true))
        assertTrue("Devrait contenir ‘ālamīn: $translit", translit.contains("‘ālamīn", ignoreCase = true))
    }

    @Test
    fun `transliterates Al-Ikhlas verse 1`() {
        val ayah = "قُلْ هُوَ اللَّهُ أَحَدٌ"
        val translit = ArabicTransliterationEngine.transliterate(ayah)

        assertFalse(translit.isBlank())
        assertTrue("Devrait contenir Qul: $translit", translit.contains("Qul", ignoreCase = true))
        assertTrue("Devrait contenir huwa: $translit", translit.contains("huwa", ignoreCase = true))
        assertTrue("Devrait contenir Allāh ou llāh: $translit", translit.contains("llāh", ignoreCase = true))
        assertTrue("Devrait contenir aḥad: $translit", translit.contains("aḥad", ignoreCase = true))
    }

    @Test
    fun `handles empty and blank strings safely`() {
        val empty = ArabicTransliterationEngine.transliterate("")
        assertTrue(empty.isEmpty())

        val blanks = ArabicTransliterationEngine.transliterate("   ")
        assertTrue(blanks.isEmpty())
    }
}

