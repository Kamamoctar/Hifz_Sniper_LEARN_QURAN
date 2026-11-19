package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AyahChunkerTest {

    @Test
    fun `empty word list produces empty chunk list`() {
        val result = AyahChunker.chunk(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `short ayah with 4 words produces a single chunk`() {
        val words = sampleWords(4)
        val chunks = AyahChunker.chunk(words)
        assertEquals(1, chunks.size)
        assertEquals(1, chunks[0].index)
        assertEquals(1, chunks[0].totalChunks)
        assertEquals(4, chunks[0].words.size)
        assertEquals("w1 w2 w3 w4", chunks[0].text)
    }

    @Test
    fun `ayah with 5 words stays as a single chunk to avoid single-word split`() {
        val words = sampleWords(5)
        val chunks = AyahChunker.chunk(words)
        assertEquals(1, chunks.size)
        assertEquals(5, chunks[0].words.size)
    }

    @Test
    fun `ayah with 8 words is split into two balanced chunks`() {
        val words = sampleWords(8)
        val chunks = AyahChunker.chunk(words, targetChunkSize = 4)
        assertEquals(2, chunks.size)
        assertEquals(4, chunks[0].words.size)
        assertEquals(4, chunks[1].words.size)
        assertEquals(1..4, chunks[0].positionRange)
        assertEquals(5..8, chunks[1].positionRange)
    }

    @Test
    fun `partial and full masking work as expected`() {
        val words = sampleWords(3)
        val chunk = AyahChunk(1, 1, words)
        assertEquals("w1 ··· ···", chunk.partiallyMaskedText())
        assertEquals("··· ··· ···", chunk.fullyMaskedText())
    }

    @Test
    fun `long ayah with 10 words does not leave a solitary orphan word`() {
        val words = sampleWords(10)
        val chunks = AyahChunker.chunk(words, targetChunkSize = 4)
        // With remaining <= target + 1, 10 words with target 4:
        // first take: 4 (6 remaining). Second take: 6 remaining <= 4+1 -> take remaining 6
        // or 4 then 6, avoiding [4, 4, 2] or [4, 5, 1]
        assertTrue(chunks.all { it.words.size >= 3 })
        assertEquals(10, chunks.sumOf { it.words.size })
    }

    private fun sampleWords(count: Int): List<PlayerWord> = (1..count).map {
        PlayerWord(id = "w$it", position = it, text = "w$it", startMs = (it - 1) * 500L, endMs = it * 500L)
    }
}

