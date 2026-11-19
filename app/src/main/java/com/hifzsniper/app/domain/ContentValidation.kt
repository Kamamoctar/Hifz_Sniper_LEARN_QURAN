package com.hifzsniper.app.domain

import java.net.URI
import java.security.MessageDigest

data class SourceAttribution(
    val name: String,
    val sourceUrl: String,
    val license: String,
    val version: String
)

data class ContentManifest(
    val schemaVersion: Int,
    val contentVersion: String,
    val expectedSurahCount: Int,
    val expectedAyahCount: Int,
    val textSource: SourceAttribution,
    val audioSource: SourceAttribution? = null
)

data class ImportSurah(
    val id: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameFrench: String,
    val ayahCount: Int,
    val revelationType: String?
)

data class ImportAyah(
    val id: Long,
    val surahId: Int,
    val ayahNumber: Int,
    val verseKey: String,
    val textUthmani: String,
    val textSimple: String?,
    val pageNumber: Int?,
    val juzNumber: Int,
    val hizbNumber: Int?
)

data class ImportWord(
    val id: String,
    val verseKey: String,
    val positionInAyah: Int,
    val textUthmani: String,
    val transliteration: String?,
    val translationFrench: String?
)

data class ImportAudioTrack(
    val id: String,
    val reciterId: String,
    val reciterName: String,
    val verseKey: String,
    val remoteUrl: String,
    val durationMs: Long?,
    val checksumSha256: String,
    val sourceVersion: String,
    val timingVersion: String?
)

data class ImportWordTiming(
    val id: String,
    val audioTrackId: String,
    val wordId: String,
    val startMs: Long,
    val endMs: Long,
    val sourceVersion: String
)

data class ImportTranslation(
    val id: String,
    val verseKey: String,
    val languageCode: String,
    val translatorName: String,
    val text: String,
    val source: String,
    val license: String,
    val version: String
)

data class ImportTafsir(
    val id: String,
    val verseKey: String,
    val languageCode: String,
    val authorOrCollection: String,
    val text: String,
    val source: String,
    val license: String,
    val version: String
)

data class ContentPackage(
    val manifest: ContentManifest,
    val surahs: List<ImportSurah>,
    val ayahs: List<ImportAyah>,
    val words: List<ImportWord> = emptyList(),
    val audioTracks: List<ImportAudioTrack> = emptyList(),
    val wordTimings: List<ImportWordTiming> = emptyList(),
    val translations: List<ImportTranslation> = emptyList(),
    val tafsirs: List<ImportTafsir> = emptyList()
)

data class ValidationIssue(val code: String, val location: String, val message: String)

data class ContentValidationResult(val issues: List<ValidationIssue>) {
    val isValid: Boolean get() = issues.isEmpty()
}

class ContentValidator {
    fun validate(content: ContentPackage): ContentValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        validateManifest(content.manifest, issues)
        validateSurahsAndAyahs(content, issues)
        validateWords(content, issues)
        validateAudio(content, issues)
        validateLocalizedContent(content, issues)
        return ContentValidationResult(issues)
    }

    private fun validateManifest(manifest: ContentManifest, issues: MutableList<ValidationIssue>) {
        if (manifest.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            issues += issue("UNSUPPORTED_SCHEMA", "manifest.schemaVersion", "Version attendue: $SUPPORTED_SCHEMA_VERSION")
        }
        if (manifest.contentVersion.isBlank()) issues += issue("MISSING_VERSION", "manifest.contentVersion", "Version obligatoire")
        if (manifest.expectedSurahCount <= 0) issues += issue("INVALID_COUNT", "manifest.expectedSurahCount", "Valeur positive requise")
        if (manifest.expectedAyahCount <= 0) issues += issue("INVALID_COUNT", "manifest.expectedAyahCount", "Valeur positive requise")
        validateAttribution(manifest.textSource, "manifest.textSource", issues)
        manifest.audioSource?.let { validateAttribution(it, "manifest.audioSource", issues) }
    }

    private fun validateAttribution(source: SourceAttribution, location: String, issues: MutableList<ValidationIssue>) {
        if (source.name.isBlank()) issues += issue("MISSING_SOURCE", "$location.name", "Nom obligatoire")
        if (!isHttps(source.sourceUrl)) issues += issue("INSECURE_SOURCE_URL", "$location.sourceUrl", "URL HTTPS obligatoire")
        if (source.license.isBlank()) issues += issue("MISSING_LICENSE", "$location.license", "Licence obligatoire")
        if (source.version.isBlank()) issues += issue("MISSING_VERSION", "$location.version", "Version obligatoire")
    }

    private fun validateSurahsAndAyahs(content: ContentPackage, issues: MutableList<ValidationIssue>) {
        if (content.surahs.size != content.manifest.expectedSurahCount) {
            issues += issue("SURAH_COUNT_MISMATCH", "surahs", "Nombre déclaré ${content.manifest.expectedSurahCount}, reçu ${content.surahs.size}")
        }
        if (content.ayahs.size != content.manifest.expectedAyahCount) {
            issues += issue("AYAH_COUNT_MISMATCH", "ayahs", "Nombre déclaré ${content.manifest.expectedAyahCount}, reçu ${content.ayahs.size}")
        }
        duplicateValues(content.surahs.map { it.id }).forEach { issues += issue("DUPLICATE_SURAH", "surah[$it]", "Identifiant dupliqué") }
        duplicateValues(content.ayahs.map { it.id }).forEach { issues += issue("DUPLICATE_AYAH_ID", "ayah[$it]", "Identifiant dupliqué") }
        duplicateValues(content.ayahs.map { it.verseKey }).forEach { issues += issue("DUPLICATE_VERSE_KEY", "ayah[$it]", "Clé dupliquée") }
        val surahs = content.surahs.associateBy { it.id }
        content.surahs.forEach { surah ->
            if (surah.id !in 1..114) issues += issue("INVALID_SURAH_ID", "surah[${surah.id}]", "Identifiant hors limites")
            if (surah.ayahCount <= 0) issues += issue("INVALID_AYAH_COUNT", "surah[${surah.id}]", "Nombre de versets invalide")
            val actual = content.ayahs.count { it.surahId == surah.id }
            if (actual != surah.ayahCount) issues += issue("SURAH_AYAH_COUNT_MISMATCH", "surah[${surah.id}]", "Déclaré ${surah.ayahCount}, reçu $actual")
        }
        content.ayahs.forEach { ayah ->
            if (surahs[ayah.surahId] == null) issues += issue("UNKNOWN_SURAH", "ayah[${ayah.verseKey}]", "Sourate absente")
            if (ayah.ayahNumber <= 0) issues += issue("INVALID_AYAH_NUMBER", "ayah[${ayah.verseKey}]", "Numéro positif requis")
            val expectedKey = "${ayah.surahId}:${ayah.ayahNumber}"
            if (ayah.verseKey != expectedKey) issues += issue("VERSE_KEY_MISMATCH", "ayah[${ayah.verseKey}]", "Clé attendue: $expectedKey")
            if (ayah.textUthmani.isBlank()) issues += issue("EMPTY_QURAN_TEXT", "ayah[${ayah.verseKey}]", "Le texte ne peut pas être vide")
            if (ayah.juzNumber !in 1..30) issues += issue("INVALID_JUZ", "ayah[${ayah.verseKey}]", "Juz hors limites")
        }
    }

    private fun validateWords(content: ContentPackage, issues: MutableList<ValidationIssue>) {
        val ayahKeys = content.ayahs.mapTo(hashSetOf()) { it.verseKey }
        duplicateValues(content.words.map { it.id }).forEach { issues += issue("DUPLICATE_WORD_ID", "word[$it]", "Identifiant dupliqué") }
        content.words.forEach { word ->
            if (word.verseKey !in ayahKeys) issues += issue("UNKNOWN_VERSE", "word[${word.id}]", "Verset absent")
            if (word.positionInAyah <= 0) issues += issue("INVALID_WORD_POSITION", "word[${word.id}]", "Position positive requise")
            if (word.textUthmani.isBlank()) issues += issue("EMPTY_WORD_TEXT", "word[${word.id}]", "Texte vide")
        }
        content.words.groupBy { it.verseKey }.forEach { (verseKey, words) ->
            val actual = words.map { it.positionInAyah }.sorted()
            val expected = (1..words.size).toList()
            if (actual != expected) issues += issue("NON_CONTIGUOUS_WORDS", "words[$verseKey]", "Positions attendues: $expected")
        }
    }

    private fun validateAudio(content: ContentPackage, issues: MutableList<ValidationIssue>) {
        val ayahKeys = content.ayahs.mapTo(hashSetOf()) { it.verseKey }
        val words = content.words.associateBy { it.id }
        val tracks = content.audioTracks.associateBy { it.id }
        duplicateValues(content.audioTracks.map { it.id }).forEach { issues += issue("DUPLICATE_AUDIO_ID", "audio[$it]", "Identifiant dupliqué") }
        duplicateValues(content.wordTimings.map { it.id }).forEach { issues += issue("DUPLICATE_TIMING_ID", "timing[$it]", "Identifiant dupliqué") }
        content.audioTracks.forEach { track ->
            if (track.reciterId != ALAFASY_ID || track.reciterName != ALAFASY_NAME) {
                issues += issue("UNSUPPORTED_RECITER", "audio[${track.id}]", "Seul $ALAFASY_NAME est autorisé")
            }
            if (track.verseKey !in ayahKeys) issues += issue("UNKNOWN_VERSE", "audio[${track.id}]", "Verset absent")
            if (!isHttps(track.remoteUrl)) issues += issue("INSECURE_AUDIO_URL", "audio[${track.id}]", "URL HTTPS obligatoire")
            if (!SHA_256.matches(track.checksumSha256)) issues += issue("INVALID_CHECKSUM", "audio[${track.id}]", "SHA-256 hexadécimal requis")
            if (track.durationMs != null && track.durationMs <= 0) issues += issue("INVALID_DURATION", "audio[${track.id}]", "Durée positive requise")
        }
        content.wordTimings.forEach { timing ->
            val track = tracks[timing.audioTrackId]
            val word = words[timing.wordId]
            if (track == null) issues += issue("UNKNOWN_AUDIO_TRACK", "timing[${timing.id}]", "Piste absente")
            if (word == null) issues += issue("UNKNOWN_WORD", "timing[${timing.id}]", "Mot absent")
            if (timing.startMs < 0 || timing.endMs <= timing.startMs) issues += issue("INVALID_TIMING_RANGE", "timing[${timing.id}]", "Intervalle invalide")
            if (track != null && word != null && track.verseKey != word.verseKey) issues += issue("TIMING_VERSE_MISMATCH", "timing[${timing.id}]", "Piste et mot appartiennent à des versets différents")
            if (track?.durationMs != null && timing.endMs > track.durationMs) issues += issue("TIMING_AFTER_AUDIO", "timing[${timing.id}]", "Le timing dépasse la piste")
            if (track?.timingVersion == null || track.timingVersion != timing.sourceVersion) issues += issue("TIMING_VERSION_MISMATCH", "timing[${timing.id}]", "Versions piste/timing différentes")
        }
        content.wordTimings.groupBy { it.audioTrackId }.forEach { (trackId, timings) ->
            val sorted = timings.sortedBy { it.startMs }
            sorted.zipWithNext().forEach { (first, second) ->
                if (second.startMs < first.endMs) issues += issue("OVERLAPPING_TIMINGS", "timings[$trackId]", "${first.id} chevauche ${second.id}")
            }
        }
    }

    private fun validateLocalizedContent(content: ContentPackage, issues: MutableList<ValidationIssue>) {
        val ayahKeys = content.ayahs.mapTo(hashSetOf()) { it.verseKey }
        content.translations.forEach { item ->
            if (item.verseKey !in ayahKeys) issues += issue("UNKNOWN_VERSE", "translation[${item.id}]", "Verset absent")
            if (item.text.isBlank() || item.license.isBlank() || item.source.isBlank() || item.version.isBlank()) {
                issues += issue("INCOMPLETE_TRANSLATION_ATTRIBUTION", "translation[${item.id}]", "Texte, source, licence et version obligatoires")
            }
        }
        content.tafsirs.forEach { item ->
            if (item.verseKey !in ayahKeys) issues += issue("UNKNOWN_VERSE", "tafsir[${item.id}]", "Verset absent")
            if (item.text.isBlank() || item.license.isBlank() || item.source.isBlank() || item.version.isBlank()) {
                issues += issue("INCOMPLETE_TAFSIR_ATTRIBUTION", "tafsir[${item.id}]", "Texte, source, licence et version obligatoires")
            }
        }
    }

    private fun isHttps(value: String): Boolean = runCatching { URI(value).scheme.equals("https", ignoreCase = true) }.getOrDefault(false)
    private fun issue(code: String, location: String, message: String) = ValidationIssue(code, location, message)
    private fun <T> duplicateValues(values: List<T>): Set<T> = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val ALAFASY_ID = "mishary-rashid-alafasy"
        const val ALAFASY_NAME = "Mishary Rashid Al-Afasy"
        private val SHA_256 = Regex("^[0-9a-fA-F]{64}$")
    }
}

object ChecksumVerifier {
    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    fun matches(bytes: ByteArray, expectedSha256: String): Boolean =
        sha256(bytes).equals(expectedSha256, ignoreCase = true)
}
