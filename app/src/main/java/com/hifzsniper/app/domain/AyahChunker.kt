package com.hifzsniper.app.domain

data class AyahChunk(
    val index: Int,
    val totalChunks: Int,
    val words: List<PlayerWord>
) {
    val text: String get() = words.joinToString(" ") { it.text }
    val positionRange: IntRange get() = (words.firstOrNull()?.position ?: 1)..(words.lastOrNull()?.position ?: 1)
    val wordCount: Int get() = words.size

    fun partiallyMaskedText(): String {
        if (words.isEmpty()) return ""
        if (words.size == 1) return words.first().text
        val first = words.first().text
        val hidden = words.drop(1).joinToString(" ") { "···" }
        return "$first $hidden"
    }

    fun fullyMaskedText(): String = words.joinToString(" ") { "···" }
}

object AyahChunker {
    fun chunk(words: List<PlayerWord>, targetChunkSize: Int = 4): List<AyahChunk> {
        if (words.isEmpty()) return emptyList()
        val total = words.size
        val k = when {
            total <= 5 -> 1
            else -> ((total + targetChunkSize - 1) / targetChunkSize).coerceAtLeast(1)
        }
        val baseSize = total / k
        val remainder = total % k

        val chunks = mutableListOf<List<PlayerWord>>()
        var cursor = 0
        for (i in 0 until k) {
            val size = baseSize + if (i < remainder) 1 else 0
            chunks += words.subList(cursor, cursor + size)
            cursor += size
        }

        return chunks.mapIndexed { i, chunkWords ->
            AyahChunk(index = i + 1, totalChunks = chunks.size, words = chunkWords)
        }
    }
}
