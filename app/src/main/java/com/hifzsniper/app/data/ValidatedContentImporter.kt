package com.hifzsniper.app.data

import com.hifzsniper.app.data.local.AudioTrackEntity
import com.hifzsniper.app.data.local.AyahEntity
import com.hifzsniper.app.data.local.ContentDao
import com.hifzsniper.app.data.local.DownloadItemEntity
import com.hifzsniper.app.data.local.PersistableContentBundle
import com.hifzsniper.app.data.local.SurahEntity
import com.hifzsniper.app.data.local.TafsirEntity
import com.hifzsniper.app.data.local.TranslationEntity
import com.hifzsniper.app.data.local.WordEntity
import com.hifzsniper.app.data.local.WordTimingEntity
import com.hifzsniper.app.domain.ContentPackage
import com.hifzsniper.app.domain.ContentValidator
import com.hifzsniper.app.domain.ValidationIssue

sealed interface ContentImportResult {
    data class Imported(
        val contentVersion: String,
        val surahCount: Int,
        val ayahCount: Int,
        val wordCount: Int,
        val audioTrackCount: Int
    ) : ContentImportResult

    data class Rejected(val issues: List<ValidationIssue>) : ContentImportResult
}

class ValidatedContentImporter(
    private val contentDao: ContentDao,
    private val validator: ContentValidator = ContentValidator()
) {
    suspend fun importValidated(content: ContentPackage): ContentImportResult {
        val validation = validator.validate(content)
        if (!validation.isValid) return ContentImportResult.Rejected(validation.issues)
        val version = content.manifest.contentVersion
        val installedVersion = contentDao.contentVersion()
        if (installedVersion == version) return content.importedResult()
        val bundle = PersistableContentBundle(
                surahs = content.surahs.map {
                    SurahEntity(it.id, it.nameArabic, it.nameTransliterated, it.nameFrench, it.ayahCount, it.revelationType, version)
                },
                ayahs = content.ayahs.map {
                    AyahEntity(it.id, it.surahId, it.ayahNumber, it.verseKey, it.textUthmani, it.textSimple, it.pageNumber, it.juzNumber, it.hizbNumber, version)
                },
                words = content.words.map {
                    WordEntity(it.id, it.verseKey, it.positionInAyah, it.textUthmani, it.transliteration, it.translationFrench)
                },
                audioTracks = content.audioTracks.map {
                    AudioTrackEntity(
                        id = it.id,
                        reciterId = it.reciterId,
                        reciterName = it.reciterName,
                        verseKey = it.verseKey,
                        remoteUrl = it.remoteUrl,
                        localPath = null,
                        durationMs = it.durationMs,
                        checksum = it.checksumSha256,
                        downloadStatus = "NOT_DOWNLOADED",
                        sourceVersion = it.sourceVersion,
                        timingVersion = it.timingVersion
                    )
                },
                wordTimings = content.wordTimings.map {
                    WordTimingEntity(it.id, it.audioTrackId, it.wordId, it.startMs, it.endMs, it.sourceVersion)
                },
                translations = content.translations.map {
                    TranslationEntity(it.id, it.verseKey, it.languageCode, it.translatorName, it.text, it.source, it.license, it.version)
                },
                tafsirs = content.tafsirs.map {
                    TafsirEntity(it.id, it.verseKey, it.languageCode, it.authorOrCollection, it.text, it.source, it.license, it.version)
                },
                downloads = content.audioTracks.map {
                    DownloadItemEntity(
                        id = "audio-${it.id}",
                        resourceType = "AUDIO_TRACK",
                        resourceKey = it.id,
                        remoteUrl = it.remoteUrl,
                        localPath = "",
                        bytesDownloaded = 0,
                        totalBytes = null,
                        status = "NOT_DOWNLOADED",
                        checksum = it.checksumSha256,
                        errorMessage = null,
                        updatedAt = 0
                    )
                }
            )
        if (installedVersion == null) contentDao.insertValidatedContent(bundle)
        else contentDao.replaceValidatedContent(bundle)
        return content.importedResult()
    }

    private fun ContentPackage.importedResult() = ContentImportResult.Imported(
            contentVersion = manifest.contentVersion,
            surahCount = surahs.size,
            ayahCount = ayahs.size,
            wordCount = words.size,
            audioTrackCount = audioTracks.size
        )
}
