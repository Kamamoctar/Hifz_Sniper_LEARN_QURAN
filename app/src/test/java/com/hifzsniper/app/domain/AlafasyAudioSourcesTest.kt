package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlafasyAudioSourcesTest {
    @Test
    fun verseKeyBuildsPaddedProviderUrls() {
        assertEquals("002255.mp3", AlafasyAudioSources.fileName("2:255"))
        assertEquals(
            listOf(
                "https://everyayah.com/data/Alafasy_128kbps/002255.mp3",
                "https://verses.quran.foundation/Alafasy/mp3/002255.mp3"
            ),
            AlafasyAudioSources.candidates(null, "2:255")
        )
    }

    @Test
    fun downloadResourceKeyCanResolveTheSameMirrors() {
        val candidates = AlafasyAudioSources.candidates(
            "https://everyayah.com/data/Alafasy_128kbps/114006.mp3",
            "alafasy-114-6"
        )
        assertEquals(2, candidates.size)
        assertTrue(candidates.last().endsWith("/Alafasy/mp3/114006.mp3"))
    }

    @Test
    fun invalidKeysDoNotInventRemoteAudio() {
        assertEquals(emptyList<String>(), AlafasyAudioSources.candidates(null, "not-a-verse"))
        assertEquals(null, AlafasyAudioSources.fileName("115:1"))
    }
}
