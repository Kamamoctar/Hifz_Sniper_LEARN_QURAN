package com.hifzsniper.app.data.content

import com.hifzsniper.app.domain.ContentValidator
import com.hifzsniper.app.domain.BasmalaPolicy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledTanzilPackageLoaderTest {
    private val assets = File("src/main/assets")

    @Test
    fun `bundled package is complete and passes domain validation`() {
        val content = loader().load()

        assertEquals(114, content.surahs.size)
        assertEquals(6236, content.ayahs.size)
        assertEquals("1:1", content.ayahs.first().verseKey)
        assertEquals("114:6", content.ayahs.last().verseKey)
        assertEquals(6236, content.surahs.sumOf { it.ayahCount })
        assertTrue(content.ayahs.all { it.pageNumber in 1..604 })
        assertTrue(content.ayahs.all { it.juzNumber in 1..30 })
        assertTrue(content.ayahs.all { it.hizbNumber in 1..60 })
        val validation = ContentValidator().validate(content)
        assertTrue(validation.issues.take(20).joinToString("\n"), validation.isValid)
    }

    @Test(expected = ContentPackageException::class)
    fun `tampered Quran text is rejected before parsing`() {
        BundledTanzilPackageLoader { path ->
            val bytes = File(assets, path).readBytes()
            if (path.endsWith("quran-uthmani.txt")) bytes.clone().also { it[0] = (it[0].toInt() xor 1).toByte() } else bytes
        }.load()
    }

    @Test
    fun `extended package contains licensed text layers and remote audio inventory`() {
        val content = loader().load()

        assertEquals(77_881, content.words.size)
        assertEquals(6_236, content.audioTracks.size)
        assertEquals(76_310, content.wordTimings.size)
        assertEquals(6_185, content.audioTracks.count { it.timingVersion != null })
        assertEquals(6_236, content.translations.size)
        assertEquals(1_330, content.tafsirs.size)
        assertTrue(content.audioTracks.all { it.remoteUrl.startsWith("https://everyayah.com/") })
    }

    @Test
    fun `bundled basmala model does not create duplicates for Al Fatiha Al Baqarah or At Tawbah`() {
        val content = loader().load()
        val fatiha = content.ayahs.single { it.verseKey == "1:1" }
        val baqarah = content.ayahs.single { it.verseKey == "2:1" }
        val tawbah = content.ayahs.single { it.verseKey == "9:1" }

        assertTrue(BasmalaPolicy.sourceContainsOpeningBasmala(fatiha.textUthmani))
        assertTrue(BasmalaPolicy.sourceContainsOpeningBasmala(baqarah.textUthmani))
        assertFalse(BasmalaPolicy.sourceContainsOpeningBasmala(tawbah.textUthmani))
        assertFalse(BasmalaPolicy.shouldRenderStandalone(1, 1, fatiha.textUthmani))
        assertFalse(BasmalaPolicy.shouldRenderStandalone(2, 1, baqarah.textUthmani))
        assertFalse(BasmalaPolicy.shouldRenderStandalone(9, 1, tawbah.textUthmani))
    }

    private fun loader() = BundledTanzilPackageLoader { path -> File(assets, path).readBytes() }
}
