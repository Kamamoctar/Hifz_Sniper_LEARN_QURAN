package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BridgeBuilderTest {
    private val builder = BridgeBuilder()

    @Test fun defaultBridgeTakesThreeWordsOnEachSideWithoutReordering() {
        val bridge = builder.build(words("1:1", 5), words("1:2", 5))
        assertEquals(3..5, bridge.beforeRange)
        assertEquals(1..3, bridge.afterRange)
        assertEquals("1:1", bridge.fromVerseKey)
        assertEquals("1:2", bridge.toVerseKey)
    }

    @Test fun shortVersesUseAllAvailableWords() {
        val bridge = builder.build(words("1:1", 2), words("1:2", 1))
        assertEquals(2, bridge.before.size)
        assertEquals(1, bridge.after.size)
    }

    @Test fun contextCanBeAdaptedForARecitableGroup() {
        val bridge = builder.build(words("1:1", 8), words("1:2", 8), beforeCount = 5, afterCount = 4)
        assertEquals(4..8, bridge.beforeRange)
        assertEquals(1..4, bridge.afterRange)
    }

    @Test fun mixedVerseInputIsRejected() {
        val mixed = words("1:1", 2) + RecitationWord("x", "1:9", 3, "x")
        assertThrows(IllegalArgumentException::class.java) { builder.build(mixed, words("1:2", 2)) }
    }

    private fun words(verseKey: String, count: Int): List<RecitationWord> =
        (1..count).map { RecitationWord("$verseKey:$it", verseKey, it, "w$it") }
}
