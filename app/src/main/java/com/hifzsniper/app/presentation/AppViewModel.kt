package com.hifzsniper.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hifzsniper.app.data.ContentImportResult
import com.hifzsniper.app.data.LearningRepository
import com.hifzsniper.app.data.PreferencesRepository
import com.hifzsniper.app.data.UserPreferences
import com.hifzsniper.app.data.ValidatedContentImporter
import com.hifzsniper.app.data.content.BundledTanzilPackageLoader
import com.hifzsniper.app.data.local.AppDatabase
import com.hifzsniper.app.data.local.AudioTrackEntity
import com.hifzsniper.app.data.local.AyahEntity
import com.hifzsniper.app.data.local.DownloadItemEntity
import com.hifzsniper.app.data.local.TafsirEntity
import com.hifzsniper.app.data.local.StudySessionEntity
import com.hifzsniper.app.data.local.TranslationEntity
import com.hifzsniper.app.data.local.WordEntity
import com.hifzsniper.app.data.local.WordTimingEntity
import com.hifzsniper.app.data.download.AudioDownloadScheduler
import com.hifzsniper.app.domain.AyahCompareCandidate
import com.hifzsniper.app.domain.CorpusStats
import com.hifzsniper.app.domain.DayActivity
import com.hifzsniper.app.domain.DownloadStatus
import com.hifzsniper.app.domain.MutashabihMatch
import com.hifzsniper.app.domain.MutashabihatEngine
import com.hifzsniper.app.domain.PageAyahView
import com.hifzsniper.app.domain.PlayerPassage
import com.hifzsniper.app.domain.PlayerWord
import com.hifzsniper.app.domain.Rating
import com.hifzsniper.app.domain.ReviewEvent
import com.hifzsniper.app.domain.ReviewStats
import com.hifzsniper.app.domain.SrsEngine
import com.hifzsniper.app.domain.StudyCard
import com.hifzsniper.app.domain.SurahSummary
import com.hifzsniper.app.domain.WeakPointSummary
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ContentInstallPhase { IDLE, IMPORTING, INSTALLED, ERROR }

data class ContentInstallState(
    val phase: ContentInstallPhase = ContentInstallPhase.IDLE,
    val message: String = "Le corpus Tanzil vérifié est prêt à être installé."
)

private data class PlayerTextEntities(
    val surah: com.hifzsniper.app.data.local.SurahEntity?,
    val ayah: AyahEntity?,
    val words: List<WordEntity>,
    val translation: TranslationEntity?,
    val tafsir: TafsirEntity?
)

private data class PlayerAudioEntities(
    val track: AudioTrackEntity?,
    val timings: List<WordTimingEntity>,
    val download: DownloadItemEntity?
)

private data class PassageData(
    val player: PlayerPassage?,
    val pageAyahs: List<PageAyahView>,
    val mutashabihat: List<MutashabihMatch>
)

private data class LearningOverviewData(
    val cards: List<StudyCard>,
    val reviews: List<ReviewEvent>,
    val weakPoints: List<com.hifzsniper.app.data.local.WeakPointEntity>,
    val sessions: List<StudySessionEntity>
)

data class AppUiState(
    val loaded: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val cards: List<StudyCard> = emptyList(),
    val plannedCards: List<StudyCard> = emptyList(),
    val corpus: CorpusStats = CorpusStats(),
    val downloads: List<DownloadStatus> = emptyList(),
    val downloadedAudioCount: Int = 0,
    val downloadedAudioBytes: Long = 0,
    val activeDownloadCount: Int = 0,
    val surahs: List<SurahSummary> = emptyList(),
    val weakPoints: List<WeakPointSummary> = emptyList(),
    val stats: ReviewStats = ReviewStats(),
    val contentInstall: ContentInstallState = ContentInstallState(),
    val playerPassage: PlayerPassage? = null,
    val pageAyahs: List<PageAyahView> = emptyList(),
    val mutashabihat: List<MutashabihMatch> = emptyList(),
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)
    private val database = AppDatabase.get(application)
    private val learningRepository = LearningRepository(database.learningDao(), database.contentDao())
    private val contentImporter = ValidatedContentImporter(database.contentDao())
    private val bundledContentLoader = BundledTanzilPackageLoader(application)
    private val contentInstall = MutableStateFlow(ContentInstallState())
    private val contentStats = database.contentDao().observeStats()
    private val downloads = database.downloadDao().observeAll()
    private val contentOverview = combine(
        contentStats,
        database.contentDao().observeSurahs(),
        contentInstall
    ) { stats, surahs, install -> Triple(stats, surahs, install) }
    private val selectedPassage = preferencesRepository.preferences.flatMapLatest { preferences ->
        val activeAyah = preferences.currentAyah
        val verseKey = "${preferences.startSurah}:$activeAyah"
        val audioTrackId = "alafasy-${preferences.startSurah}-$activeAyah"
        val textLayers = combine(
            database.contentDao().observeSurah(preferences.startSurah),
            database.contentDao().observeAyah(preferences.startSurah, activeAyah),
            database.contentDao().observeWords(verseKey),
            database.contentDao().observeFrenchTranslation(verseKey),
            database.contentDao().observeFrenchTafsir(verseKey)
        ) { surah, ayah, words, translation, tafsir -> PlayerTextEntities(surah, ayah, words, translation, tafsir) }
        val audioLayers = combine(
            database.contentDao().observeAlafasyTrack(verseKey),
            database.contentDao().observeWordTimings(audioTrackId),
            database.downloadDao().observeByResourceKey(audioTrackId)
        ) { track, timings, download -> PlayerAudioEntities(track, timings, download) }
        combine(textLayers, audioLayers) { text, audio -> text to audio }.flatMapLatest { (text, audio) ->
            val page = text.ayah?.pageNumber ?: 1
            database.contentDao().observeAyahsForPage(page).map { pageAyahEntities ->
                val player = text.ayah?.let { ayah ->
                    val timingsByWord = audio.timings.associateBy(WordTimingEntity::wordId)
                    PlayerPassage(
                        verseKey = ayah.verseKey,
                        surahId = ayah.surahId,
                        ayahNumber = ayah.ayahNumber,
                        surahNameArabic = text.surah?.nameArabic.orEmpty(),
                        surahNameTransliterated = text.surah?.nameTransliterated.orEmpty(),
                        pageNumber = ayah.pageNumber,
                        juzNumber = ayah.juzNumber,
                        hizbNumber = ayah.hizbNumber,
                        textUthmani = ayah.textUthmani,
                        transliteration = com.hifzsniper.app.domain.ArabicTransliterationEngine.transliterate(ayah.textUthmani),
                        translationFrench = text.translation?.text,
                        explanatoryNotesFrench = text.tafsir?.text,
                        words = text.words.map { word ->
                            val timing = timingsByWord[word.id]
                            PlayerWord(word.id, word.positionInAyah, word.textUthmani, timing?.startMs, timing?.endMs)
                        },
                        audioTrackId = audio.track?.id,
                        remoteUrl = audio.track?.remoteUrl,
                        localPath = audio.download?.localPath?.takeIf { audio.download.status == "COMPLETED" },
                        downloadId = audio.download?.id,
                        downloadStatus = audio.download?.status
                    )
                }
                val pageAyahs = pageAyahEntities.map {
                    PageAyahView(
                        surahId = it.surahId,
                        ayahNumber = it.ayahNumber,
                        verseKey = it.verseKey,
                        textUthmani = it.textUthmani,
                        pageNumber = it.pageNumber ?: page,
                        juzNumber = it.juzNumber,
                        hizbNumber = it.hizbNumber
                    )
                }
                val cand = allAyahCandidates ?: emptyList()
                val mutashabihat = if (player != null && cand.isNotEmpty()) {
                    MutashabihatEngine.findMatches(
                        sourceVerseKey = player.verseKey,
                        sourceTextUthmani = player.textUthmani,
                        allAyahs = cand,
                        maxResults = 6
                    )
                } else emptyList()
                PassageData(player, pageAyahs, mutashabihat)
            }
        }
    }.flowOn(Dispatchers.Default)
    private val learningOverview = combine(
        learningRepository.cards,
        learningRepository.reviewEvents,
        database.learningDao().observeActiveWeakPoints(),
        database.learningDao().observeStudySessions()
    ) { cards, reviews, weakPoints, sessions -> LearningOverviewData(cards, reviews, weakPoints, sessions) }
    private val clock = Clock.systemUTC()
    private val planner = SrsEngine()
    private val downloadScheduler = AudioDownloadScheduler(application)
    private var allAyahCandidates: List<AyahCompareCandidate>? = null

    private suspend fun loadCandidates(): List<AyahCompareCandidate> {
        allAyahCandidates?.let { return it }
        val surahs = database.contentDao().observeSurahs().first().associateBy { it.id }
        val ayahs = database.contentDao().getAllAyahs()
        val candidates = ayahs.map {
            val surah = surahs[it.surahId]
            AyahCompareCandidate(
                verseKey = it.verseKey,
                surahId = it.surahId,
                ayahNumber = it.ayahNumber,
                surahNameArabic = surah?.nameArabic.orEmpty(),
                surahNameTransliterated = surah?.nameTransliterated.orEmpty(),
                textUthmani = it.textUthmani
            )
        }
        allAyahCandidates = candidates
        return candidates
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
            val stats = database.contentDao().observeStats().first()
            if (stats.surahCount == 0 || stats.ayahCount == 0) {
                importBundledCorpus()
            }
            loadCandidates()
        }
    }

    val state = combine(
        preferencesRepository.preferences,
        learningOverview,
        contentOverview,
        downloads,
        selectedPassage
    ) { preferences, learning, contentData, downloadItems, passageData ->
        val player = passageData.player
        val pageAyahs = passageData.pageAyahs
        val mutashabihat = passageData.mutashabihat
        val cards = learning.cards
        val reviews = learning.reviews
        val weakPointEntities = learning.weakPoints
        val studySessions = learning.sessions
        val (content, surahEntities, install) = contentData
        val now = clock.instant()
        val due = cards.filter { !it.scheduling.suspended && !it.scheduling.nextReviewAt.isAfter(now) }
        val cardsById = cards.associateBy(StudyCard::id)
        val planned = planner.planSession(
            cards = due.map(StudyCard::scheduling),
            now = now,
            budgetSeconds = preferences.dailyMinutes * 60
        ).mapNotNull { cardsById[it.id] }

        val streakInfo = com.hifzsniper.app.domain.StreakCalculator.computeStreaks(
            reviewedInstants = reviews.map { it.reviewedAt },
            now = now
        )
        val startOfToday = LocalDate.now(clock).atStartOfDay().toInstant(ZoneOffset.UTC)
        val startOfLast7Days = now.minus(Duration.ofDays(7))
        val startOfLast30Days = now.minus(Duration.ofDays(30))
        val cardsByReviewId = cards.associateBy { it.id }
        fun estimatedSecondsSince(start: java.time.Instant): Int {
            val recorded = studySessions.asSequence()
                .filter { it.startedAt >= start.toEpochMilli() }
                .sumOf { it.actualSeconds }
            return if (recorded > 0) recorded else reviews
                .asSequence()
                .filter { !it.reviewedAt.isBefore(start) }
                .sumOf { cardsByReviewId[it.cardId]?.scheduling?.estimatedDurationSeconds ?: 30 }
        }
        val activeCards = cards.filterNot { it.scheduling.suspended }
        val tomorrow = now.plus(Duration.ofDays(1))
        val nextWeek = now.plus(Duration.ofDays(7))
        val todayAyahReviews = reviews.asSequence()
            .filter { !it.reviewedAt.isBefore(startOfToday) }
            .mapNotNull { cardsByReviewId[it.cardId] }
            .filter { it.scheduling.type == com.hifzsniper.app.domain.CardType.AYAH }
            .map { it.id }
            .distinct()
            .count()

        AppUiState(
            loaded = true,
            preferences = preferences,
            cards = cards,
            plannedCards = planned,
            corpus = CorpusStats(
                surahCount = content.surahCount,
                ayahCount = content.ayahCount,
                wordCount = content.wordCount,
                audioTrackCount = content.audioTrackCount,
                timingCount = content.timingCount
            ),
            downloads = downloadItems.filter {
                it.status != "NOT_DOWNLOADED" || it.resourceKey == player?.audioTrackId
            }.take(100).map {
                DownloadStatus(it.id, it.resourceKey, it.status, it.bytesDownloaded, it.totalBytes, it.errorMessage)
            },
            downloadedAudioCount = downloadItems.count { it.status == "COMPLETED" },
            downloadedAudioBytes = downloadItems.filter { it.status == "COMPLETED" }.sumOf { it.bytesDownloaded },
            activeDownloadCount = downloadItems.count { it.status == "DOWNLOADING" || it.status == "QUEUED" },
            surahs = surahEntities.map {
                SurahSummary(it.id, it.nameArabic, it.nameTransliterated, it.nameFrench, it.ayahCount)
            },
            weakPoints = weakPointEntities.map {
                WeakPointSummary(it.id, it.verseKey, it.reason, it.severity, it.createdAt, it.linkedCardId, it.wordStart, it.wordEnd)
            },
            stats = ReviewStats(
                totalReviews = reviews.size,
                fluentReviews = reviews.count { it.rating == Rating.FLUENT },
                failedReviews = reviews.count { it.rating == Rating.FAILED },
                weakCards = cards.count { it.scheduling.isWeak },
                dueCards = due.size,
                estimatedDueSeconds = planned.sumOf { it.scheduling.estimatedDurationSeconds },
                currentStreak = streakInfo.currentStreak,
                bestStreak = streakInfo.bestStreak,
                estimatedSecondsToday = estimatedSecondsSince(startOfToday),
                estimatedSecondsLast7Days = estimatedSecondsSince(startOfLast7Days),
                estimatedSecondsLast30Days = estimatedSecondsSince(startOfLast30Days),
                learnedAyahs = activeCards.count { it.scheduling.type == com.hifzsniper.app.domain.CardType.AYAH },
                reviewedAyahs = reviews.asSequence()
                    .mapNotNull { cardsByReviewId[it.cardId] }
                    .filter { it.scheduling.type == com.hifzsniper.app.domain.CardType.AYAH }
                    .map { it.id }
                    .distinct()
                    .count(),
                dailyCompletedAyahs = todayAyahReviews,
                averageStability = activeCards.map { it.scheduling.stability }.average().takeIf { !it.isNaN() } ?: 0.0,
                dueNext24Hours = activeCards.count { it.scheduling.nextReviewAt.isAfter(now) && !it.scheduling.nextReviewAt.isAfter(tomorrow) },
                dueNext7Days = activeCards.count { it.scheduling.nextReviewAt.isAfter(now) && !it.scheduling.nextReviewAt.isAfter(nextWeek) },
                recentActivity = streakInfo.recentActivity
            ),
            contentInstall = if (
                content.surahCount == 114 && content.ayahCount == 6236 &&
                content.wordCount > 0 && content.audioTrackCount == 6236 && content.timingCount > 0 &&
                install.phase == ContentInstallPhase.IDLE
            ) {
                ContentInstallState(ContentInstallPhase.INSTALLED, "Corpus enrichi installé et vérifié.")
            } else install,
            playerPassage = player,
            pageAyahs = pageAyahs,
            mutashabihat = mutashabihat
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    fun completeOnboarding(
        minutes: Int,
        level: String,
        surah: Int,
        ayah: Int,
        goal: Int,
        transliteration: Boolean = false,
        translation: Boolean = false,
        tafsir: Boolean = false
    ) {
        viewModelScope.launch {
            preferencesRepository.completeOnboarding(
                minutes, level, surah, ayah, goal,
                transliteration, translation, tafsir
            )
            learningRepository.activatePassage(surah, ayah)
            if (!state.value.corpus.hasVerifiedContent) {
                importBundledCorpus()
            }
        }
    }

    fun restartOnboarding() = viewModelScope.launch {
        preferencesRepository.resetOnboarding()
    }

    fun review(cardId: String, rating: Rating) {
        viewModelScope.launch { learningRepository.review(cardId, rating) }
    }

    fun setDailyMinutes(value: Int) = viewModelScope.launch { preferencesRepository.setDailyMinutes(value) }
    fun setStartPoint(surah: Int, ayah: Int) = viewModelScope.launch {
        preferencesRepository.setStartPoint(surah, ayah)
        learningRepository.activatePassage(surah, ayah)
    }
    fun setCurrentAyah(surah: Int, ayah: Int) = viewModelScope.launch {
        preferencesRepository.setCurrentAyah(surah, ayah)
        learningRepository.activatePassage(surah, ayah)
    }
    fun setVerseSet(surah: Int, startAyah: Int, endAyah: Int) = viewModelScope.launch {
        val s = surah.coerceIn(1, 114)
        val start = startAyah.coerceAtLeast(1)
        val end = endAyah.coerceAtLeast(start)
        preferencesRepository.setVerseSet(s, start, end)
        learningRepository.activatePassage(s, start)
    }
    fun activateSelectedPassage() = viewModelScope.launch {
        val preferences = state.value.preferences
        val activeAyah = preferences.currentAyah
        val activated = learningRepository.activatePassage(preferences.startSurah, activeAyah)
        contentInstall.value = if (activated) {
            ContentInstallState(ContentInstallPhase.INSTALLED, "${preferences.startSurah}:$activeAyah ajouté aux révisions.")
        } else {
            ContentInstallState(ContentInstallPhase.ERROR, "Installe d’abord le corpus Tanzil.")
        }
    }
    fun movePassage(delta: Int) = viewModelScope.launch {
        if (delta == 0) return@launch
        val current = state.value.preferences
        val surahs = state.value.surahs
        val currentSurah = surahs.firstOrNull { it.id == current.startSurah } ?: return@launch
        val activeAyah = current.currentAyah
        val target = when {
            delta < 0 && activeAyah > 1 -> current.startSurah to activeAyah - 1
            delta < 0 && current.startSurah > 1 -> {
                val previous = surahs.first { it.id == current.startSurah - 1 }
                previous.id to previous.ayahCount
            }
            delta > 0 && activeAyah < currentSurah.ayahCount -> current.startSurah to activeAyah + 1
            delta > 0 && current.startSurah < 114 -> current.startSurah + 1 to 1
            else -> current.startSurah to activeAyah
        }
        preferencesRepository.setCurrentAyah(target.first, target.second)
    }

    fun goToPage(page: Int) = viewModelScope.launch {
        val clamped = page.coerceIn(1, 604)
        val firstAyah = database.contentDao().getFirstAyahForPage(clamped)
        if (firstAyah != null) {
            preferencesRepository.setCurrentAyah(firstAyah.surahId, firstAyah.ayahNumber)
            learningRepository.activatePassage(firstAyah.surahId, firstAyah.ayahNumber)
        }
    }

    fun movePage(delta: Int) = viewModelScope.launch {
        val current = state.value.playerPassage?.pageNumber ?: 1
        goToPage(current + delta)
    }
    fun importBundledCorpus() = viewModelScope.launch {
        if (contentInstall.value.phase == ContentInstallPhase.IMPORTING) return@launch
        contentInstall.value = ContentInstallState(ContentInstallPhase.IMPORTING, "Vérification SHA-256 et import en cours…")
        val preferences = state.value.preferences
        val outcome = runCatching {
            withContext(Dispatchers.IO) {
                val content = bundledContentLoader.load()
                when (val result = contentImporter.importValidated(content)) {
                    is ContentImportResult.Imported -> {
                        val insertedCards = learningRepository.seedCorpusCards(
                            content,
                            selectedSurah = preferences.startSurah,
                            selectedAyah = preferences.currentAyah
                        )
                        learningRepository.activatePassage(preferences.startSurah, preferences.currentAyah)
                        "${result.surahCount} sourates et ${result.ayahCount} versets vérifiés. $insertedCards cartes initialisées."
                    }
                    is ContentImportResult.Rejected -> throw IllegalStateException(
                        result.issues.take(3).joinToString(" · ") { "${it.code}: ${it.message}" }
                    )
                }
            }
        }
        contentInstall.value = outcome.fold(
            onSuccess = { ContentInstallState(ContentInstallPhase.INSTALLED, it) },
            onFailure = { ContentInstallState(ContentInstallPhase.ERROR, it.message ?: "Échec de l’import") }
        )
    }
    fun setTransliteration(value: Boolean) = viewModelScope.launch { preferencesRepository.setTransliteration(value) }
    fun setTranslation(value: Boolean) = viewModelScope.launch { preferencesRepository.setTranslation(value) }
    fun setTafsir(value: Boolean) = viewModelScope.launch { preferencesRepository.setTafsir(value) }
    fun setLevel(value: String) = viewModelScope.launch { preferencesRepository.setLevel(value) }
    fun setDailyAyahGoal(value: Int) = viewModelScope.launch { preferencesRepository.setDailyAyahGoal(value) }
    fun startDownload(id: String) = viewModelScope.launch(Dispatchers.IO) { queueDownload(id) }
    fun downloadCurrentAudio() = state.value.playerPassage?.downloadId?.let { id ->
        viewModelScope.launch(Dispatchers.IO) { queueDownload(id) }
    }
    fun downloadPageAudio(page: Int) = viewModelScope.launch(Dispatchers.IO) {
        val ayahs = database.contentDao().getAyahsForPage(page)
        ayahs.forEach { ayah ->
            val downloadId = "audio-alafasy-${ayah.surahId}-${ayah.ayahNumber}"
            queueDownload(downloadId)
        }
    }
    fun downloadSurahAudio(surahId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val ayahs = database.contentDao().getAyahsForSurah(surahId)
        ayahs.forEach { ayah ->
            val downloadId = "audio-alafasy-${ayah.surahId}-${ayah.ayahNumber}"
            queueDownload(downloadId)
        }
    }
    private suspend fun queueDownload(id: String) {
        val item = database.downloadDao().get(id) ?: return
        if (item.status == "COMPLETED" && File(item.localPath).isFile) return
        database.downloadDao().update(
            item.copy(status = "QUEUED", errorMessage = null, updatedAt = System.currentTimeMillis())
        )
        downloadScheduler.enqueue(id)
    }

    fun cancelDownload(id: String) = viewModelScope.launch(Dispatchers.IO) {
        downloadScheduler.cancel(id)
        database.downloadDao().get(id)?.let { item ->
            database.downloadDao().update(
                item.copy(status = "CANCELLED", errorMessage = null, updatedAt = System.currentTimeMillis())
            )
        }
    }
    fun deleteDownload(id: String) = viewModelScope.launch(Dispatchers.IO) {
        downloadScheduler.cancel(id)
        val item = database.downloadDao().get(id) ?: return@launch
        val audioDirectory = File(getApplication<Application>().filesDir, "audio")
        val target = File(item.localPath)
        val partial = File(audioDirectory, ".${com.hifzsniper.app.domain.DownloadSafety.safeFileName(id, ".part")}")
        listOf(target, partial).forEach { file ->
            if (com.hifzsniper.app.domain.DownloadSafety.isInside(audioDirectory, file) && file.exists()) {
                file.delete()
            }
        }
        database.downloadDao().update(
            item.copy(
                localPath = "",
                bytesDownloaded = 0,
                totalBytes = null,
                status = "NOT_DOWNLOADED",
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    fun resolveWeakPoint(id: String) = viewModelScope.launch {
        database.learningDao().resolveWeakPoint(id, System.currentTimeMillis())
    }

    fun recordWeakPoint(verseKey: String, reason: String = "Difficulté à la récitation") = viewModelScope.launch {
        learningRepository.recordWeakPoint(verseKey, reason)
    }

    fun recordWeakRange(verseKey: String, wordStart: Int?, wordEnd: Int?, reason: String) = viewModelScope.launch {
        learningRepository.recordWeakPoint(
            verseKey = verseKey,
            reason = reason,
            wordStart = wordStart,
            wordEnd = wordEnd
        )
    }

    fun createBridgeToNextVerse(fromVerseKey: String, toVerseKey: String) = viewModelScope.launch {
        learningRepository.createBridgeBetweenVerses(fromVerseKey, toVerseKey)
    }

    fun createChunkReinforcementCard(verseKey: String, chunkIndex: Int, totalChunks: Int, words: List<PlayerWord>) = viewModelScope.launch {
        learningRepository.createChunkCard(verseKey, chunkIndex, totalChunks, words)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        learningRepository.exportBackup(state.value.preferences)
    }

    suspend fun inspectBackup(jsonString: String): com.hifzsniper.app.data.BackupSummary = withContext(Dispatchers.Default) {
        com.hifzsniper.app.data.ProgressBackupService.parseAndValidate(jsonString)
    }

    suspend fun restoreBackup(summary: com.hifzsniper.app.data.BackupSummary): Result<String> = withContext(Dispatchers.IO) {
        if (!summary.isValid) return@withContext Result.failure(IllegalArgumentException(summary.errorMessage ?: "Fichier invalide"))
        val previous = runCatching {
            val previousJson = learningRepository.exportBackup(state.value.preferences)
            com.hifzsniper.app.data.ProgressBackupService.parseAndValidate(previousJson)
        }.getOrElse { return@withContext Result.failure(IllegalStateException("Impossible de créer le point de restauration", it)) }
        try {
            if (!learningRepository.restoreBackup(summary)) error("Échec de la restauration Room")
            summary.preferences?.let { preferencesRepository.restoreAll(it) }
            Result.success("${summary.cardsCount} cartes, ${summary.logsCount} révisions, ${summary.sessionsCount} séances et ${summary.weakPointsCount} points faibles restaurés.")
        } catch (error: Exception) {
            // Compensating rollback keeps Room and DataStore aligned if the second store fails.
            runCatching {
                if (previous.isValid) {
                    learningRepository.restoreBackup(previous)
                    previous.preferences?.let { preferencesRepository.restoreAll(it) }
                }
            }
            Result.failure(IllegalStateException("Restauration annulée et état précédent rétabli : ${error.message}", error))
        }
    }
}
