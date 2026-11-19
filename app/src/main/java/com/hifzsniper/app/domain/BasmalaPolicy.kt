package com.hifzsniper.app.domain

/**
 * Keeps the UI aligned with the bundled Tanzil model: when the opening
 * basmala is already part of ayah 1, it must never be rendered a second time.
 */
object BasmalaPolicy {
    private val diacritics = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06EDـ]")
    private val nonLetters = Regex("[^ء-يٱ]+")

    fun sourceContainsOpeningBasmala(text: String): Boolean {
        val normalized = text
            .replace(diacritics, "")
            .replace('ٱ', 'ا')
            .replace(nonLetters, " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        return normalized.startsWith("بسم الله الرحمن الرحيم")
    }

    fun shouldRenderStandalone(surahId: Int, ayahNumber: Int, ayahText: String): Boolean =
        ayahNumber == 1 &&
            surahId != 1 &&
            surahId != 9 &&
            !sourceContainsOpeningBasmala(ayahText)

    /**
     * Medina Mushaf standard:
     * - Surah 1 (Al-Fatiha): Basmala IS verse 1 itself (numbered). No standalone unnumbered header.
     * - Surahs 2..114 (except 9): An unnumbered calligraphic Basmala header is rendered before verse 1.
     * - Surah 9 (At-Tawbah): No Basmala at all.
     */
    fun shouldRenderHeaderBasmala(surahId: Int): Boolean = surahId != 1 && surahId != 9

    /**
     * When displaying verse 1 in the continuous Mushaf page view (where an unnumbered
     * Basmala header is rendered above), strip the opening Basmala from the verse text
     * so it is never displayed twice.
     * e.g. for Surah 2:1 "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ الٓمٓ" -> "الٓمٓ".
     */
    fun formatAyahTextForMushaf(surahId: Int, ayahNumber: Int, textUthmani: String): String {
        if (surahId == 1 || surahId == 9 || ayahNumber != 1) return textUthmani
        if (!sourceContainsOpeningBasmala(textUthmani)) return textUthmani
        val words = textUthmani.trim().split(Regex("\\s+"))
        return if (words.size > 4) {
            words.drop(4).joinToString(" ")
        } else {
            textUthmani
        }
    }
}
