package com.hifzsniper.app.domain

data class RecitationWord(val id: String, val verseKey: String, val position: Int, val text: String)

data class BridgeDraft(
    val fromVerseKey: String,
    val toVerseKey: String,
    val before: List<RecitationWord>,
    val after: List<RecitationWord>
) {
    val recitableText: String get() = (before + after).joinToString(" ") { it.text }
    val beforeRange: IntRange get() = before.first().position..before.last().position
    val afterRange: IntRange get() = after.first().position..after.last().position
}

class BridgeBuilder(private val defaultContextWords: Int = 3) {
    fun build(
        previousVerseWords: List<RecitationWord>,
        nextVerseWords: List<RecitationWord>,
        beforeCount: Int = defaultContextWords,
        afterCount: Int = defaultContextWords
    ): BridgeDraft {
        require(previousVerseWords.isNotEmpty()) { "Le verset précédent doit contenir au moins un mot" }
        require(nextVerseWords.isNotEmpty()) { "Le verset suivant doit contenir au moins un mot" }
        require(beforeCount > 0 && afterCount > 0) { "Le contexte doit contenir au moins un mot de chaque côté" }
        val previousKey = previousVerseWords.first().verseKey
        val nextKey = nextVerseWords.first().verseKey
        require(previousVerseWords.all { it.verseKey == previousKey }) { "Tous les mots précédents doivent appartenir au même verset" }
        require(nextVerseWords.all { it.verseKey == nextKey }) { "Tous les mots suivants doivent appartenir au même verset" }
        val previous = previousVerseWords.sortedBy { it.position }
        val next = nextVerseWords.sortedBy { it.position }
        require(previous.map { it.position }.distinct().size == previous.size) { "Positions précédentes dupliquées" }
        require(next.map { it.position }.distinct().size == next.size) { "Positions suivantes dupliquées" }
        return BridgeDraft(
            fromVerseKey = previousKey,
            toVerseKey = nextKey,
            before = previous.takeLast(beforeCount),
            after = next.take(afterCount)
        )
    }
}
