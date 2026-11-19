package com.hifzsniper.app.domain

object RecitationMatcher {

    enum class WordMatchStatus {
        MATCHED,      // Mot correctement prononcé (Vert)
        APPROXIMATE,  // Prononciation approchante / légère hésitation (Orange)
        MISSED        // Mot manqué ou erroné (Rouge/Gris)
    }

    data class WordFeedback(
        val originalWord: String,
        val status: WordMatchStatus,
        val position: Int
    )

    data class RecitationEvaluation(
        val accuracyPercentage: Int,
        val wordFeedbacks: List<WordFeedback>,
        val recommendedRating: Rating,
        val matchedCount: Int,
        val approximateCount: Int,
        val missedCount: Int,
        val feedbackSummary: String
    )

    fun evaluate(expectedTextUthmani: String, spokenText: String): RecitationEvaluation {
        val expectedTokens = expectedTextUthmani
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.any(Char::isLetter) }

        if (expectedTokens.isEmpty()) {
            return RecitationEvaluation(
                accuracyPercentage = 0,
                wordFeedbacks = emptyList(),
                recommendedRating = Rating.FAILED,
                matchedCount = 0,
                approximateCount = 0,
                missedCount = 0,
                feedbackSummary = "Aucun texte à évaluer."
            )
        }

        val expectedNormalized = expectedTokens.map(MutashabihatEngine::normalizeArabic)
        val spokenNormalized = MutashabihatEngine.tokenizeWords(spokenText)

        if (spokenNormalized.isEmpty()) {
            val feedbacks = expectedTokens.mapIndexed { idx, token ->
                WordFeedback(token, WordMatchStatus.MISSED, idx + 1)
            }
            return RecitationEvaluation(
                accuracyPercentage = 0,
                wordFeedbacks = feedbacks,
                recommendedRating = Rating.FAILED,
                matchedCount = 0,
                approximateCount = 0,
                missedCount = expectedTokens.size,
                feedbackSummary = "Aucune parole détectée."
            )
        }

        // Align spoken words to expected words using greedy window search
        val feedbacks = mutableListOf<WordFeedback>()
        var spokenCursor = 0
        var matched = 0
        var approx = 0
        var missed = 0

        for (i in expectedNormalized.indices) {
            val expectedWord = expectedNormalized[i]
            val originalWord = expectedTokens[i]

            // Look ahead in spoken words (window of 3 words)
            var foundSpokenIndex = -1
            var bestDistance = Int.MAX_VALUE

            val searchLimit = minOf(spokenNormalized.size, spokenCursor + 4)
            for (j in spokenCursor until searchLimit) {
                val candidateSpoken = spokenNormalized[j]
                if (candidateSpoken == expectedWord) {
                    foundSpokenIndex = j
                    bestDistance = 0
                    break
                } else {
                    val dist = levenshtein(expectedWord, candidateSpoken)
                    if (dist <= 1 && dist < bestDistance && expectedWord.length >= 3) {
                        bestDistance = dist
                        foundSpokenIndex = j
                    }
                }
            }

            when {
                foundSpokenIndex != -1 && bestDistance == 0 -> {
                    feedbacks.add(WordFeedback(originalWord, WordMatchStatus.MATCHED, i + 1))
                    matched++
                    spokenCursor = foundSpokenIndex + 1
                }
                foundSpokenIndex != -1 && bestDistance <= 1 -> {
                    feedbacks.add(WordFeedback(originalWord, WordMatchStatus.APPROXIMATE, i + 1))
                    approx++
                    spokenCursor = foundSpokenIndex + 1
                }
                else -> {
                    feedbacks.add(WordFeedback(originalWord, WordMatchStatus.MISSED, i + 1))
                    missed++
                }
            }
        }

        val totalWords = expectedTokens.size
        val weightedScore = (matched.toDouble() + 0.6 * approx.toDouble()) / totalWords.toDouble()
        val accuracy = (weightedScore * 100).toInt().coerceIn(0, 100)

        val recommendedRating = when {
            accuracy >= 88 -> Rating.FLUENT
            accuracy >= 60 -> Rating.HESITANT
            else -> Rating.FAILED
        }

        val summary = when {
            accuracy >= 95 -> "Excellent ! Récitation quasi-parfaite sans hésitation."
            accuracy >= 85 -> "Très bonne récitation avec de légères nuances."
            accuracy >= 65 -> "Bonne récitation mais des mots ont été oubliés ou modifiés."
            else -> "Plusieurs mots manqués. Révise ce passage avant de valider."
        }

        return RecitationEvaluation(
            accuracyPercentage = accuracy,
            wordFeedbacks = feedbacks,
            recommendedRating = recommendedRating,
            matchedCount = matched,
            approximateCount = approx,
            missedCount = missed,
            feedbackSummary = summary
        )
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}

