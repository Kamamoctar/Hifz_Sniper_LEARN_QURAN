package com.hifzsniper.app.data.content

import android.content.Context
import com.hifzsniper.app.domain.ChecksumVerifier
import com.hifzsniper.app.domain.ContentManifest
import com.hifzsniper.app.domain.ContentPackage
import com.hifzsniper.app.domain.ImportAudioTrack
import com.hifzsniper.app.domain.ImportAyah
import com.hifzsniper.app.domain.ImportSurah
import com.hifzsniper.app.domain.ImportTafsir
import com.hifzsniper.app.domain.ImportTranslation
import com.hifzsniper.app.domain.ImportWord
import com.hifzsniper.app.domain.ImportWordTiming
import com.hifzsniper.app.domain.SourceAttribution
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.Properties
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.json.JSONArray
import org.w3c.dom.Element

class ContentPackageException(message: String) : IllegalStateException(message)

class BundledTanzilPackageLoader(
    private val readAsset: (String) -> ByteArray
) {
    constructor(context: Context) : this(
        readAsset = { path -> context.assets.open(path).use { it.readBytes() } }
    )

    fun load(): ContentPackage {
        val manifestBytes = readAsset("$ASSET_ROOT/content-manifest.properties")
        val properties = Properties().apply { ByteArrayInputStream(manifestBytes).use(::load) }
        val textBytes = verifiedAsset(properties, "textFile", "textSha256")
        val metadataBytes = verifiedAsset(properties, "metadataFile", "metadataSha256")
        val translationBytes = verifiedAsset(properties, "translationFile", "translationSha256")
        val audioManifestBytes = verifiedAsset(properties, "audioManifestFile", "audioManifestSha256")
        val timingBytes = verifiedAsset(properties, "timingFile", "timingSha256")
        val rawAyahs = parseAyahs(textBytes)
        val metadata = parseMetadata(metadataBytes, properties.required("metadataVersion"), rawAyahs)

        val expectedSurahs = properties.requiredInt("expectedSurahCount")
        val expectedAyahs = properties.requiredInt("expectedAyahCount")
        if (metadata.surahs.size != expectedSurahs) {
            throw ContentPackageException("Métadonnées Tanzil: $expectedSurahs sourates attendues, ${metadata.surahs.size} reçues")
        }
        if (rawAyahs.size != expectedAyahs) {
            throw ContentPackageException("Texte Tanzil: $expectedAyahs versets attendus, ${rawAyahs.size} reçus")
        }

        val surahsById = metadata.surahs.associateBy { it.id }
        val ayahs = rawAyahs.mapIndexed { index, raw ->
            val ordinal = index + 1
            val surah = surahsById[raw.surahId]
                ?: throw ContentPackageException("Sourate ${raw.surahId} absente des métadonnées")
            if (raw.ayahNumber !in 1..surah.ayahCount) {
                throw ContentPackageException("Verset hors limites: ${raw.surahId}:${raw.ayahNumber}")
            }
            ImportAyah(
                id = ordinal.toLong(),
                surahId = raw.surahId,
                ayahNumber = raw.ayahNumber,
                verseKey = "${raw.surahId}:${raw.ayahNumber}",
                textUthmani = raw.text,
                textSimple = null,
                pageNumber = metadata.pages.valueAt(ordinal),
                juzNumber = metadata.juzs.valueAt(ordinal)
                    ?: throw ContentPackageException("Juz absent pour ${raw.surahId}:${raw.ayahNumber}"),
                hizbNumber = metadata.hizbQuarters.valueAt(ordinal)?.let { ((it - 1) / 4) + 1 }
            )
        }
        val wordsByVerse = buildWords(ayahs)
        val translations = parseTranslations(translationBytes, ayahs.mapTo(hashSetOf()) { it.verseKey }, properties)
        val audioRows = parseAudioManifest(audioManifestBytes, ayahs.mapTo(hashSetOf()) { it.verseKey })
        val timingRows = parseTimingRows(timingBytes)
        val timingVersion = properties.required("timingVersion")
        val wordTimings = buildWordTimings(wordsByVerse, timingRows, timingVersion)
        val verseByWordId = wordsByVerse.values.flatten().associate { it.id to it.verseKey }
        val timedVerses = wordTimings.mapTo(hashSetOf()) { timing -> verseByWordId.getValue(timing.wordId) }
        val audioTracks = audioRows.map { row ->
            ImportAudioTrack(
                id = row.trackId,
                reciterId = "mishary-rashid-alafasy",
                reciterName = "Mishary Rashid Al-Afasy",
                verseKey = row.verseKey,
                remoteUrl = row.remoteUrl,
                durationMs = null,
                checksumSha256 = row.checksum,
                sourceVersion = properties.required("audioSourceVersion"),
                timingVersion = timingVersion.takeIf { row.verseKey in timedVerses }
            )
        }

        return ContentPackage(
            manifest = ContentManifest(
                schemaVersion = properties.requiredInt("schemaVersion"),
                contentVersion = properties.required("contentVersion"),
                expectedSurahCount = expectedSurahs,
                expectedAyahCount = expectedAyahs,
                textSource = SourceAttribution(
                    name = properties.required("textName"),
                    sourceUrl = properties.required("textSourceUrl"),
                    license = properties.required("textLicense"),
                    version = properties.required("textVersion")
                ),
                audioSource = SourceAttribution(
                    name = "Mishary Rashid Al-Afasy 128 kbps",
                    sourceUrl = properties.required("audioSourceUrl"),
                    license = properties.required("audioRights"),
                    version = properties.required("audioSourceVersion")
                )
            ),
            surahs = metadata.surahs,
            ayahs = ayahs,
            words = wordsByVerse.values.flatten(),
            audioTracks = audioTracks,
            wordTimings = wordTimings,
            translations = translations.first,
            tafsirs = translations.second
        )
    }

    private fun buildWords(ayahs: List<ImportAyah>): Map<String, List<ImportWord>> = ayahs.associate { ayah ->
        val tokens = ayah.textUthmani.split(WHITESPACE).filter { token -> token.any(Char::isLetter) }
        ayah.verseKey to tokens.mapIndexed { index, token ->
            ImportWord(
                id = "${ayah.verseKey}:w${index + 1}",
                verseKey = ayah.verseKey,
                positionInAyah = index + 1,
                textUthmani = token,
                transliteration = com.hifzsniper.app.domain.ArabicTransliterationEngine.transliterate(token),
                translationFrench = null
            )
        }
    }

    private fun parseTranslations(
        bytes: ByteArray,
        verseKeys: Set<String>,
        properties: Properties
    ): Pair<List<ImportTranslation>, List<ImportTafsir>> {
        val decoder = Base64.getDecoder()
        val translations = mutableListOf<ImportTranslation>()
        val notes = mutableListOf<ImportTafsir>()
        bytes.inputStream().bufferedReader(Charsets.US_ASCII).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.forEach { line ->
                val parts = line.split('|', limit = 3)
                if (parts.size != 3 || parts[0] !in verseKeys) {
                    throw ContentPackageException("Ligne QuranEnc invalide")
                }
                val translation = String(decoder.decode(parts[1]), Charsets.UTF_8)
                val footnotes = String(decoder.decode(parts[2]), Charsets.UTF_8)
                translations += ImportTranslation(
                    id = "quranenc-fr-${parts[0]}",
                    verseKey = parts[0],
                    languageCode = "fr",
                    translatorName = "Rachid Maach",
                    text = translation,
                    source = properties.required("translationSourceUrl"),
                    license = properties.required("translationLicense"),
                    version = properties.required("translationVersion")
                )
                if (footnotes.isNotBlank()) {
                    notes += ImportTafsir(
                        id = "quranenc-fr-note-${parts[0]}",
                        verseKey = parts[0],
                        languageCode = "fr",
                        authorOrCollection = "Notes explicatives de la traduction de Rachid Maach",
                        text = footnotes,
                        source = properties.required("translationSourceUrl"),
                        license = properties.required("translationLicense"),
                        version = properties.required("translationVersion")
                    )
                }
            }
        }
        if (translations.size != verseKeys.size) {
            throw ContentPackageException("QuranEnc: ${verseKeys.size} traductions attendues, ${translations.size} reçues")
        }
        return translations to notes
    }

    private fun parseAudioManifest(bytes: ByteArray, verseKeys: Set<String>): List<AudioRow> {
        val rows = bytes.inputStream().bufferedReader(Charsets.US_ASCII).useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                val parts = line.split('|', limit = 4)
                if (parts.size != 4 || parts[0] !in verseKeys) {
                    throw ContentPackageException("Ligne du manifeste audio invalide")
                }
                AudioRow(
                    verseKey = parts[0],
                    checksum = parts[2],
                    remoteUrl = parts[3]
                )
            }.toList()
        }
        if (rows.size != verseKeys.size || rows.map { it.verseKey }.toSet().size != verseKeys.size) {
            throw ContentPackageException("Le manifeste audio ne couvre pas exactement les 6 236 versets")
        }
        return rows
    }

    private fun parseTimingRows(bytes: ByteArray): Map<String, TimingRow> {
        val json = JSONArray(String(bytes, Charsets.UTF_8))
        val rows = buildMap {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                val key = "${item.getInt("surah")}:${item.getInt("ayah")}"
                val jsonSegments = item.getJSONArray("segments")
                val segments = buildList {
                    for (segmentIndex in 0 until jsonSegments.length()) {
                        val segment = jsonSegments.getJSONArray(segmentIndex)
                        add(TimingSegment(segment.getInt(0), segment.getInt(1), segment.getLong(2), segment.getLong(3)))
                    }
                }
                if (put(key, TimingRow(key, segments)) != null) {
                    throw ContentPackageException("Timing dupliqué: $key")
                }
            }
        }
        if (rows.size != 6236) throw ContentPackageException("6 236 timings audio attendus, ${rows.size} reçus")
        return rows
    }

    private fun buildWordTimings(
        wordsByVerse: Map<String, List<ImportWord>>,
        timingRows: Map<String, TimingRow>,
        version: String
    ): List<ImportWordTiming> = buildList {
        wordsByVerse.forEach { (verseKey, words) ->
            val row = timingRows[verseKey] ?: return@forEach
            if (row.segments.isEmpty() || row.segments.any { it.wordEnd - it.wordStart != 1 }) return@forEach
            if (row.segments.any { it.startMs < 0 || it.endMs <= it.startMs }) return@forEach
            if (row.segments.zipWithNext().any { (first, second) -> second.startMs < first.endMs }) return@forEach
            val maxEnd = row.segments.maxOf(TimingSegment::wordEnd)
            if (row.segments.map { it.wordStart } != (0 until maxEnd).toList()) return@forEach
            val (surah, ayah) = verseKey.split(':').map(String::toInt)
            val offset = if (ayah == 1 && surah !in setOf(1, 9) && words.size == maxEnd + 4) 4 else 0
            if (words.size != maxEnd + offset) return@forEach
            row.segments.forEach { segment ->
                val word = words[segment.wordStart + offset]
                add(
                    ImportWordTiming(
                        id = "alafasy-${verseKey.replace(':', '-')}-w${word.positionInAyah}",
                        audioTrackId = "alafasy-${verseKey.replace(':', '-')}",
                        wordId = word.id,
                        startMs = segment.startMs,
                        endMs = segment.endMs,
                        sourceVersion = version
                    )
                )
            }
        }
    }

    private fun verifiedAsset(properties: Properties, fileKey: String, checksumKey: String): ByteArray {
        val fileName = properties.required(fileKey)
        if (fileName.contains('/') || fileName.contains('\\')) {
            throw ContentPackageException("Nom de ressource non sûr: $fileName")
        }
        val bytes = readAsset("$ASSET_ROOT/$fileName")
        val expected = properties.required(checksumKey)
        if (!ChecksumVerifier.matches(bytes, expected)) {
            throw ContentPackageException("Échec SHA-256 pour $fileName")
        }
        return bytes
    }

    private fun parseAyahs(bytes: ByteArray): List<RawAyah> = bytes.inputStream()
        .bufferedReader(Charsets.UTF_8)
        .useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.mapIndexed { index, line ->
                val parts = line.split('|', limit = 3)
                if (parts.size != 3) throw ContentPackageException("Ligne Tanzil invalide: ${index + 1}")
                RawAyah(
                    surahId = parts[0].toIntOrNull()
                        ?: throw ContentPackageException("Numéro de sourate invalide à la ligne ${index + 1}"),
                    ayahNumber = parts[1].toIntOrNull()
                        ?: throw ContentPackageException("Numéro de verset invalide à la ligne ${index + 1}"),
                    text = parts[2].takeIf(String::isNotBlank)
                        ?: throw ContentPackageException("Texte vide à la ligne ${index + 1}")
                )
            }.toList()
        }

    private fun parseMetadata(bytes: ByteArray, expectedVersion: String, ayahs: List<RawAyah>): ParsedMetadata {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
            runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val root = document.documentElement
        if (root.tagName != "quran" || root.getAttribute("version") != expectedVersion) {
            throw ContentPackageException("Version des métadonnées Tanzil inattendue")
        }
        val orderByKey = ayahs.mapIndexed { index, ayah -> "${ayah.surahId}:${ayah.ayahNumber}" to index + 1 }.toMap()
        val surahs = root.getElementsByTagName("sura").asElements().map { element ->
            ImportSurah(
                id = element.requiredInt("index"),
                nameArabic = element.required("name"),
                nameTransliterated = element.required("tname"),
                nameFrench = element.required("ename"),
                ayahCount = element.requiredInt("ayas"),
                revelationType = element.required("type")
            )
        }
        return ParsedMetadata(
            surahs = surahs,
            juzs = boundaries(root, "juz", orderByKey),
            hizbQuarters = boundaries(root, "quarter", orderByKey),
            pages = boundaries(root, "page", orderByKey)
        )
    }

    private fun boundaries(root: Element, tagName: String, orderByKey: Map<String, Int>): BoundaryMap {
        val values = root.getElementsByTagName(tagName).asElements().map { element ->
            val key = "${element.requiredInt("sura")}:${element.requiredInt("aya")}"
            Boundary(
                ordinal = orderByKey[key] ?: throw ContentPackageException("Frontière $tagName inconnue: $key"),
                value = element.requiredInt("index")
            )
        }.sortedBy(Boundary::ordinal)
        if (values.firstOrNull()?.ordinal != 1) throw ContentPackageException("Première frontière $tagName invalide")
        return BoundaryMap(values)
    }

    private fun Properties.required(key: String): String = getProperty(key)?.trim().takeUnless { it.isNullOrEmpty() }
        ?: throw ContentPackageException("Clé de manifeste absente: $key")

    private fun Properties.requiredInt(key: String): Int = required(key).toIntOrNull()
        ?: throw ContentPackageException("Entier de manifeste invalide: $key")

    private fun Element.required(name: String): String = getAttribute(name).takeIf(String::isNotBlank)
        ?: throw ContentPackageException("Attribut XML absent: $tagName.$name")

    private fun Element.requiredInt(name: String): Int = required(name).toIntOrNull()
        ?: throw ContentPackageException("Attribut XML invalide: $tagName.$name")

    private fun org.w3c.dom.NodeList.asElements(): List<Element> = buildList {
        for (index in 0 until length) add(item(index) as Element)
    }

    private data class RawAyah(val surahId: Int, val ayahNumber: Int, val text: String)
    private data class AudioRow(val verseKey: String, val checksum: String, val remoteUrl: String) {
        val trackId: String get() = "alafasy-${verseKey.replace(':', '-')}"
    }
    private data class TimingRow(val verseKey: String, val segments: List<TimingSegment>)
    private data class TimingSegment(
        val wordStart: Int,
        val wordEnd: Int,
        val startMs: Long,
        val endMs: Long
    )
    private data class ParsedMetadata(
        val surahs: List<ImportSurah>,
        val juzs: BoundaryMap,
        val hizbQuarters: BoundaryMap,
        val pages: BoundaryMap
    )
    private data class Boundary(val ordinal: Int, val value: Int)

    private class BoundaryMap(private val boundaries: List<Boundary>) {
        fun valueAt(ordinal: Int): Int? = boundaries.lastOrNull { it.ordinal <= ordinal }?.value
    }

    companion object {
        const val ASSET_ROOT = "content/tanzil-uthmani-1.1"
        private val WHITESPACE = Regex("\\s+")
    }
}
