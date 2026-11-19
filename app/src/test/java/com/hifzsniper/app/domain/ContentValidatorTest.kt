package com.hifzsniper.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {
    private val validator = ContentValidator()

    @Test fun validVersionedPackageIsAccepted() {
        val result = validator.validate(validPackage())
        assertTrue(result.issues.joinToString { it.code }, result.isValid)
    }

    @Test fun mismatchedVerseKeyIsRejected() {
        val content = validPackage().let { it.copy(ayahs = listOf(it.ayahs.single().copy(verseKey = "1:2"))) }
        val result = validator.validate(content)
        assertTrue(result.issues.any { it.code == "VERSE_KEY_MISMATCH" })
    }

    @Test fun nonContiguousWordPositionsAreRejected() {
        val content = validPackage().let {
            it.copy(words = listOf(it.words[0], it.words[1].copy(positionInAyah = 3)))
        }
        assertTrue(validator.validate(content).issues.any { it.code == "NON_CONTIGUOUS_WORDS" })
    }

    @Test fun anyReciterOtherThanAlAfasyIsRejected() {
        val content = validPackage().let {
            it.copy(audioTracks = listOf(it.audioTracks.single().copy(reciterId = "other", reciterName = "Other")))
        }
        assertTrue(validator.validate(content).issues.any { it.code == "UNSUPPORTED_RECITER" })
    }

    @Test fun overlappingOrMismatchedTimingsAreRejected() {
        val content = validPackage().let {
            it.copy(
                wordTimings = listOf(
                    it.wordTimings[0],
                    it.wordTimings[1].copy(startMs = 400, sourceVersion = "wrong-version")
                )
            )
        }
        val codes = validator.validate(content).issues.map { it.code }
        assertTrue("OVERLAPPING_TIMINGS" in codes)
        assertTrue("TIMING_VERSION_MISMATCH" in codes)
    }

    @Test fun missingTranslationLicenseIsRejected() {
        val content = validPackage().copy(
            translations = listOf(
                ImportTranslation("fr-1-1", "1:1", "fr", "Traducteur de test", "Fixture", "source", "", "1")
            )
        )
        assertTrue(validator.validate(content).issues.any { it.code == "INCOMPLETE_TRANSLATION_ATTRIBUTION" })
    }

    @Test fun checksumVerifierUsesRealSha256() {
        val bytes = "hello".encodeToByteArray()
        assertTrue(ChecksumVerifier.matches(bytes, "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))
        assertFalse(ChecksumVerifier.matches(bytes, "0".repeat(64)))
    }

    private fun validPackage(): ContentPackage {
        val textSource = SourceAttribution("Fixture technique", "https://example.test/text", "Test only", "1")
        val audioSource = SourceAttribution("Fixture audio", "https://example.test/audio", "Test only", "1")
        return ContentPackage(
            manifest = ContentManifest(1, "fixture-1", 1, 1, textSource, audioSource),
            surahs = listOf(ImportSurah(1, "اسم تجريبي", "Fixture", "Fixture", 1, null)),
            ayahs = listOf(ImportAyah(1, 1, 1, "1:1", "نص تقني غير قرآني", null, 1, 1, 1)),
            words = listOf(
                ImportWord("1:1:1", "1:1", 1, "نص", null, null),
                ImportWord("1:1:2", "1:1", 2, "تقني", null, null)
            ),
            audioTracks = listOf(
                ImportAudioTrack(
                    id = "track-1",
                    reciterId = ContentValidator.ALAFASY_ID,
                    reciterName = ContentValidator.ALAFASY_NAME,
                    verseKey = "1:1",
                    remoteUrl = "https://example.test/audio.mp3",
                    durationMs = 2_000,
                    checksumSha256 = "a".repeat(64),
                    sourceVersion = "audio-1",
                    timingVersion = "timing-1"
                )
            ),
            wordTimings = listOf(
                ImportWordTiming("timing-1", "track-1", "1:1:1", 0, 500, "timing-1"),
                ImportWordTiming("timing-2", "track-1", "1:1:2", 500, 1_000, "timing-1")
            )
        )
    }
}
