package com.hifzsniper.app.domain

import java.util.Locale

/** Trusted, byte-compatible hosts for the bundled Alafasy 128 kbps inventory. */
object AlafasyAudioSources {
    const val EVERYAYAH_BASE = "https://everyayah.com/data/Alafasy_128kbps/"
    const val QURAN_FOUNDATION_BASE = "https://verses.quran.foundation/Alafasy/mp3/"

    private val verseKeyPattern = Regex("^(\\d{1,3}):(\\d{1,3})$")
    private val resourceKeyPattern = Regex("(?:audio-)?alafasy-(\\d{1,3})-(\\d{1,3})")

    fun candidates(primaryUrl: String?, verseKeyOrResourceKey: String): List<String> {
        val fileName = fileName(verseKeyOrResourceKey) ?: return listOfNotNull(
            primaryUrl?.takeIf { it.startsWith("https://") }
        )
        return buildList {
            primaryUrl?.takeIf { it.startsWith("https://") }?.let(::add)
            add(EVERYAYAH_BASE + fileName)
            add(QURAN_FOUNDATION_BASE + fileName)
        }.distinct()
    }

    fun fileName(verseKeyOrResourceKey: String): String? {
        val match = verseKeyPattern.matchEntire(verseKeyOrResourceKey)
            ?: resourceKeyPattern.matchEntire(verseKeyOrResourceKey)
            ?: return null
        val surah = match.groupValues[1].toIntOrNull()?.takeIf { it in 1..114 } ?: return null
        val ayah = match.groupValues[2].toIntOrNull()?.takeIf { it in 1..286 } ?: return null
        return String.format(Locale.US, "%03d%03d.mp3", surah, ayah)
    }
}
