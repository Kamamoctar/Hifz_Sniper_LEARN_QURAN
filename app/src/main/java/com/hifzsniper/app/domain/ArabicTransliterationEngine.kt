package com.hifzsniper.app.domain

/**
 * Moteur de translittération phonétique automatique du texte coranique Uthmani (Tanzil 1.1).
 * Convertit le texte arabe vocalisé avec tashkeel en transcription phonétique latine standard
 * (conforme aux normes académiques et pédagogiques de lecture du Coran).
 */
object ArabicTransliterationEngine {

    private val sunLetters = setOf('ت', 'ث', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ل', 'ن')

    /**
     * Translittère un verset ou une portion de texte Uthmani en caractères latins phonétiques.
     */
    fun transliterate(textUthmani: String): String {
        if (textUthmani.isBlank()) return ""

        // Nettoyer les ornements coraniques de fin de verset (۝, chiffres arabes, marques de waqf)
        val cleaned = textUthmani
            .replace(Regex("[\u06DD\u06DE\u06E9\u06DA\u06DB\u06DC\u06D6-\u06D9]"), "")
            .replace(Regex("[\u0660-\u0669]"), "")
            .trim()

        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val transliteratedWords = words.mapIndexed { index, word ->
            transliterateWord(word, isFirstWord = index == 0)
        }

        val result = transliteratedWords.joinToString(" ").trim()
        return if (result.isNotEmpty()) result.replaceFirstChar { it.uppercaseChar() } else ""
    }

    private fun transliterateWord(rawWord: String, isFirstWord: Boolean): String {
        val word = rawWord.trim()
        if (word.isEmpty()) return ""

        // Cas particuliers d'Allah et ses formes
        when {
            word == "ٱللَّهِ" || word == "اللَّهِ" -> return if (isFirstWord) "Allāhi" else "llāhi"
            word == "ٱللَّهُ" || word == "اللَّهُ" -> return if (isFirstWord) "Allāhu" else "llāhu"
            word == "ٱللَّهَ" || word == "اللَّهَ" -> return if (isFirstWord) "Allāha" else "llāha"
            word == "لِلَّهِ" -> return "Lillāhi"
        }

        val sb = StringBuilder()
        var i = 0
        val len = word.length

        // Détecter article défini Al- / ar- / etc.
        val hasDefiniteArticle = len >= 2 && (word[0] == 'ٱ' || word[0] == 'ا') && word[1] == 'ل'
        val isSunLetterFollower = hasDefiniteArticle && len >= 3 && word[2] in sunLetters

        while (i < len) {
            val c = word[i]

            // Traitement spécial de l'article défini "Al-"
            if (hasDefiniteArticle && i == 0) {
                if (isSunLetterFollower) {
                    // Lettre solaire : le lam est assimilé
                    val prefix = if (isFirstWord) "A" else "a"
                    sb.append(prefix)
                    i = 2 // Sauter 'ٱ' et 'ل'
                    continue
                } else {
                    // Lettre lunaire : le lam est prononcé "al-"
                    val prefix = if (isFirstWord) "Al-" else "al-"
                    sb.append(prefix)
                    i = 2 // Sauter 'ٱ' et 'ل'
                    continue
                }
            }

            // Vérifier si le caractère suivant est une shaddah ou un diacritique
            var hasShaddah = false
            var vowel: String? = null
            var nextIndex = i + 1

            // Examiner les diacritiques attachés à cette lettre
            while (nextIndex < len && isDiacritic(word[nextIndex])) {
                val d = word[nextIndex]
                when (d) {
                    '\u0651' -> hasShaddah = true // Shaddah
                    '\u064E' -> vowel = "a" // Fatha
                    '\u064F' -> vowel = "u" // Damma
                    '\u0650' -> vowel = "i" // Kasra
                    '\u064B' -> vowel = "an" // Tanwin Fatha
                    '\u064C' -> vowel = "un" // Tanwin Damma
                    '\u064D' -> vowel = "in" // Tanwin Kasra
                    '\u0670' -> vowel = "ā" // Dagger Alif
                    '\u06E5' -> vowel = "ū" // Small Waw
                    '\u06E6' -> vowel = "ī" // Small Ya
                    '\u0652', '\u06DF', '\u06E1' -> vowel = "" // Sukun
                }
                nextIndex++
            }

            // Conversion de la consonne
            val latinConsonant = when (c) {
                'ء', 'ؤ', 'ئ' -> "'"
                'أ' -> if (i == 0) "" else "'"
                'إ' -> if (i == 0) "" else "'"
                'آ' -> { vowel = "ā"; "" }
                'ا' -> {
                    // Alif sans diacritique suivant une fatha allonge en "ā"
                    if (sb.isNotEmpty() && sb.last() == 'a') {
                        sb.deleteCharAt(sb.length - 1)
                        "ā"
                    } else if (i == 0) "" else "a"
                }
                'ٱ' -> {
                    if (i == 0 && isFirstWord) "A" else ""
                }
                'ب' -> "b"
                'ت' -> "t"
                'ث' -> "th"
                'ج' -> "j"
                'ح' -> "ḥ"
                'خ' -> "kh"
                'د' -> "d"
                'ذ' -> "dh"
                'ر' -> "r"
                'ز' -> "z"
                'س' -> "s"
                'ش' -> "sh"
                'ص' -> "ṣ"
                'ض' -> "ḍ"
                'ط' -> "ṭ"
                'ظ' -> "ẓ"
                'ع' -> "‘"
                'غ' -> "gh"
                'ف' -> "f"
                'ق' -> "q"
                'ك' -> "k"
                'ل' -> "l"
                'م' -> "m"
                'ن' -> "n"
                'ه' -> "h"
                'ة' -> if (vowel != null && vowel.isNotEmpty()) "t" else "h"
                'و' -> {
                    if (vowel == null && sb.isNotEmpty() && sb.last() == 'u') {
                        sb.deleteCharAt(sb.length - 1)
                        "ū"
                    } else "w"
                }
                'ي' -> {
                    if (vowel == null && sb.isNotEmpty() && sb.last() == 'i') {
                        sb.deleteCharAt(sb.length - 1)
                        "ī"
                    } else "y"
                }
                'ى' -> "ā" // Alif Maqsura
                '\u0640' -> {
                    // Tatweel / Kashida (utilisé sous dagger alif dans الرحمن -> مَـٰن)
                    ""
                }
                else -> ""
            }

            if (latinConsonant.isNotEmpty()) {
                if (hasShaddah) {
                    sb.append(latinConsonant).append(latinConsonant)
                } else {
                    sb.append(latinConsonant)
                }
            }

            if (vowel != null) {
                // Éviter "aā", "uū", "iī" quand une voyelle longue suit une voyelle courte
                if (vowel == "ā" && sb.isNotEmpty() && sb.last() == 'a') {
                    sb.deleteCharAt(sb.length - 1)
                }
                if (vowel == "ū" && sb.isNotEmpty() && sb.last() == 'u') {
                    sb.deleteCharAt(sb.length - 1)
                }
                if (vowel == "ī" && sb.isNotEmpty() && sb.last() == 'i') {
                    sb.deleteCharAt(sb.length - 1)
                }
                sb.append(vowel)
            }

            i = nextIndex
        }

        return sb.toString()
    }

    private fun isDiacritic(c: Char): Boolean = c in '\u064B'..'\u065F' || c == '\u0670' || c == '\u06DF' || c in '\u06E1'..'\u06E6'
}
