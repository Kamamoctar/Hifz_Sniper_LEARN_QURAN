package com.hifzsniper.app.domain

object MutashabihatEngine {

    private val ARABIC_DIACRITICS = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    fun normalizeArabic(text: String): String {
        return text
            .replace(ARABIC_DIACRITICS, "")
            .replace('ٱ', 'ا')
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace('ؤ', 'ء')
            .replace('ئ', 'ء')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun tokenizeWords(text: String): List<String> {
        return normalizeArabic(text)
            .split(' ')
            .filter { it.isNotBlank() && it.any(Char::isLetter) }
    }

    fun findMatches(
        sourceVerseKey: String,
        sourceTextUthmani: String,
        allAyahs: List<AyahCompareCandidate>,
        maxResults: Int = 5
    ): List<MutashabihMatch> {
        val sourceWords = tokenizeWords(sourceTextUthmani)
        if (sourceWords.size < 3) return emptyList()

        val sourceWordSet = sourceWords.toSet()
        val results = mutableListOf<MutashabihMatch>()

        for (candidate in allAyahs) {
            if (candidate.verseKey == sourceVerseKey) continue
            val candidateWords = tokenizeWords(candidate.textUthmani)
            if (candidateWords.size < 3) continue

            // 1. Prefix Match (Début identique sur au moins 3 mots)
            val commonPrefix = commonPrefixLength(sourceWords, candidateWords)
            val isPrefixMatch = commonPrefix >= 3 && (sourceWords != candidateWords)

            // 2. Suffix Match (Fin identique sur au moins 3 mots)
            val commonSuffix = commonSuffixLength(sourceWords, candidateWords)
            val isSuffixMatch = commonSuffix >= 3 && (sourceWords != candidateWords)

            // 3. Jaccard word similarity
            val candidateWordSet = candidateWords.toSet()
            val intersection = sourceWordSet.intersect(candidateWordSet).size
            val union = sourceWordSet.union(candidateWordSet).size
            val jaccard = if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
            val jaccardPercent = (jaccard * 100).toInt()

            val matchType = when {
                isPrefixMatch -> MutashabihType.PREFIX
                isSuffixMatch -> MutashabihType.SUFFIX
                jaccardPercent >= 60 -> MutashabihType.HIGH_SIMILARITY
                else -> null
            }

            if (matchType != null) {
                val divergenceDescription = when (matchType) {
                    MutashabihType.PREFIX -> "Même début sur $commonPrefix mots, mais la suite diffère."
                    MutashabihType.SUFFIX -> "Même fin sur $commonSuffix mots, mais le début diffère."
                    MutashabihType.HIGH_SIMILARITY -> "Structure globale très similaire ($jaccardPercent % de mots communs)."
                }

                val score = maxOf(
                    jaccardPercent,
                    if (isPrefixMatch) (commonPrefix * 20).coerceAtMost(95) else 0,
                    if (isSuffixMatch) (commonSuffix * 20).coerceAtMost(90) else 0
                )

                results.add(
                    MutashabihMatch(
                        sourceVerseKey = sourceVerseKey,
                        targetVerseKey = candidate.verseKey,
                        targetSurahId = candidate.surahId,
                        targetAyahNumber = candidate.ayahNumber,
                        targetSurahNameArabic = candidate.surahNameArabic,
                        targetSurahNameTransliterated = candidate.surahNameTransliterated,
                        targetTextUthmani = candidate.textUthmani,
                        similarityPercentage = score,
                        matchType = matchType,
                        divergenceDescription = divergenceDescription
                    )
                )
            }
        }

        return results.sortedByDescending { it.similarityPercentage }.take(maxResults)
    }

    private fun commonPrefixLength(a: List<String>, b: List<String>): Int {
        var count = 0
        val limit = minOf(a.size, b.size)
        while (count < limit && a[count] == b[count]) {
            count++
        }
        return count
    }

    private fun commonSuffixLength(a: List<String>, b: List<String>): Int {
        var count = 0
        val limit = minOf(a.size, b.size)
        while (count < limit && a[a.size - 1 - count] == b[b.size - 1 - count]) {
            count++
        }
        return count
    }
}

data class AyahCompareCandidate(
    val verseKey: String,
    val surahId: Int,
    val ayahNumber: Int,
    val surahNameArabic: String = "",
    val surahNameTransliterated: String = "",
    val textUthmani: String
)

data class MutashabihMatch(
    val sourceVerseKey: String,
    val targetVerseKey: String,
    val targetSurahId: Int,
    val targetAyahNumber: Int,
    val targetSurahNameArabic: String,
    val targetSurahNameTransliterated: String,
    val targetTextUthmani: String,
    val similarityPercentage: Int,
    val matchType: MutashabihType,
    val divergenceDescription: String
)

enum class MutashabihType {
    PREFIX,
    SUFFIX,
    HIGH_SIMILARITY
}

