package com.hifzsniper.app.domain

import java.time.Instant

data class StudyCard(
    val scheduling: LearningCard,
    val prompt: String,
    val answer: String
) {
    val id: String get() = scheduling.id
}

data class DayActivity(
    val dateString: String,
    val count: Int,
    val isToday: Boolean
)

data class ReviewStats(
    val totalReviews: Int = 0,
    val fluentReviews: Int = 0,
    val failedReviews: Int = 0,
    val weakCards: Int = 0,
    val dueCards: Int = 0,
    val estimatedDueSeconds: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val estimatedSecondsToday: Int = 0,
    val estimatedSecondsLast7Days: Int = 0,
    val estimatedSecondsLast30Days: Int = 0,
    val learnedAyahs: Int = 0,
    val reviewedAyahs: Int = 0,
    val dailyCompletedAyahs: Int = 0,
    val averageStability: Double = 0.0,
    val dueNext24Hours: Int = 0,
    val dueNext7Days: Int = 0,
    val recentActivity: List<DayActivity> = emptyList()
) {
    val successRate: Int
        get() = if (totalReviews == 0) 0 else (fluentReviews * 100) / totalReviews
}

data class ReviewEvent(
    val cardId: String,
    val reviewedAt: Instant,
    val rating: Rating
)

data class CorpusStats(
    val surahCount: Int = 0,
    val ayahCount: Int = 0,
    val wordCount: Int = 0,
    val audioTrackCount: Int = 0,
    val timingCount: Int = 0
) {
    val hasVerifiedContent: Boolean get() = surahCount > 0 && ayahCount > 0
    val hasWordSynchronizedAudio: Boolean get() = audioTrackCount > 0 && timingCount > 0
}

data class DownloadStatus(
    val id: String,
    val resourceKey: String,
    val status: String,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val errorMessage: String?
)

data class SurahSummary(
    val id: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameFrench: String,
    val ayahCount: Int
)

data class WeakPointSummary(
    val id: String,
    val verseKey: String,
    val reason: String,
    val severity: Int,
    val createdAt: Long,
    val linkedCardId: String?,
    val wordStart: Int? = null,
    val wordEnd: Int? = null
)

data class PlayerWord(
    val id: String,
    val position: Int,
    val text: String,
    val startMs: Long?,
    val endMs: Long?
)

data class PageSummary(
    val pageNumber: Int,
    val startSurahId: Int,
    val startAyahNumber: Int,
    val surahName: String = "",
    val juzNumber: Int = 1
)

data class PlayerPassage(
    val verseKey: String,
    val surahId: Int = 1,
    val ayahNumber: Int = 1,
    val surahNameArabic: String = "",
    val surahNameTransliterated: String = "",
    val pageNumber: Int? = 1,
    val juzNumber: Int? = 1,
    val hizbNumber: Int? = 1,
    val textUthmani: String,
    val transliteration: String? = null,
    val translationFrench: String?,
    val explanatoryNotesFrench: String?,
    val words: List<PlayerWord>,
    val audioTrackId: String?,
    val remoteUrl: String?,
    val localPath: String?,
    val downloadId: String?,
    val downloadStatus: String?
) {
    val playableUris: List<String>
        get() = buildList {
            localPath?.takeIf(String::isNotBlank)?.let(::add)
            if (!audioTrackId.isNullOrBlank() || !remoteUrl.isNullOrBlank()) {
                addAll(AlafasyAudioSources.candidates(remoteUrl, verseKey))
            }
        }.distinct()
    val playableUri: String? get() = playableUris.firstOrNull()
    val hasWordTimings: Boolean get() = words.any { it.startMs != null && it.endMs != null }
    val effectiveTransliteration: String get() = transliteration?.takeIf(String::isNotBlank) ?: ArabicTransliterationEngine.transliterate(textUthmani)
}

data class PageAyahView(
    val surahId: Int,
    val ayahNumber: Int,
    val verseKey: String,
    val textUthmani: String,
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbNumber: Int? = null,
    val surahNameArabic: String = "",
    val surahNameTransliterated: String = ""
)
