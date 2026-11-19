package com.hifzsniper.app.data

import com.hifzsniper.app.data.local.LearningCardEntity
import com.hifzsniper.app.data.local.ReviewLogEntity
import com.hifzsniper.app.data.local.StudySessionEntity
import com.hifzsniper.app.data.local.WeakPointEntity
import com.hifzsniper.app.domain.CardType
import com.hifzsniper.app.domain.Rating
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

data class BackupSummary(
    val isValid: Boolean,
    val version: Int = 1,
    val cardsCount: Int = 0,
    val logsCount: Int = 0,
    val weakPointsCount: Int = 0,
    val sessionsCount: Int = 0,
    val exportedAt: String = "",
    val cards: List<LearningCardEntity> = emptyList(),
    val logs: List<ReviewLogEntity> = emptyList(),
    val weakPoints: List<WeakPointEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val preferences: UserPreferences? = null,
    val errorMessage: String? = null
)

object ProgressBackupService {
    const val CURRENT_BACKUP_VERSION = 1

    fun exportToJson(
        cards: List<LearningCardEntity>,
        logs: List<ReviewLogEntity>,
        weakPoints: List<WeakPointEntity>,
        sessions: List<StudySessionEntity> = emptyList(),
        preferences: UserPreferences,
        now: Instant = Instant.now()
    ): String {
        val root = JSONObject()
        root.put("version", CURRENT_BACKUP_VERSION)
        root.put("exportedAt", now.toString())
        root.put("app", "Hifz-Sniper")

        val prefJson = JSONObject().apply {
            put("dailyMinutes", preferences.dailyMinutes)
            put("level", preferences.level)
            put("startSurah", preferences.startSurah)
            put("startAyah", preferences.startAyah)
            put("endAyah", preferences.endAyah)
            put("currentAyah", preferences.currentAyah)
            put("dailyAyahGoal", preferences.dailyAyahGoal)
            put("showTransliteration", preferences.showTransliteration)
            put("showTranslation", preferences.showTranslation)
            put("showTafsir", preferences.showTafsir)
            put("onboardingComplete", preferences.onboardingComplete)
        }
        root.put("preferences", prefJson)

        val cardsArray = JSONArray()
        cards.forEach { card ->
            val item = JSONObject().apply {
                put("id", card.id)
                put("type", card.type)
                put("surahId", card.surahId)
                put("ayahStart", card.ayahStart)
                put("ayahEnd", card.ayahEnd)
                if (card.wordStart != null) put("wordStart", card.wordStart)
                if (card.wordEnd != null) put("wordEnd", card.wordEnd)
                put("prompt", card.prompt)
                put("answer", card.answer)
                put("createdAt", card.createdAt)
                if (card.lastReviewedAt != null) put("lastReviewedAt", card.lastReviewedAt)
                put("nextReviewAt", card.nextReviewAt)
                put("intervalDays", card.intervalDays)
                put("stability", card.stability)
                put("difficulty", card.difficulty)
                put("lapseCount", card.lapseCount)
                put("successCount", card.successCount)
                put("hesitationCount", card.hesitationCount)
                put("isWeak", card.isWeak)
                put("suspended", card.suspended)
                if (card.sourceCardId != null) put("sourceCardId", card.sourceCardId)
                if (card.userNote != null) put("userNote", card.userNote)
                put("estimatedDurationSeconds", card.estimatedDurationSeconds)
                if (card.lastResult != null) put("lastResult", card.lastResult)
                put("algorithmVersion", card.algorithmVersion)
                put("promptMode", card.promptMode)
                put("contextBeforeCount", card.contextBeforeCount)
                put("contextAfterCount", card.contextAfterCount)
            }
            cardsArray.put(item)
        }
        root.put("cards", cardsArray)

        val logsArray = JSONArray()
        logs.forEach { log ->
            val item = JSONObject().apply {
                put("cardId", log.cardId)
                put("reviewedAt", log.reviewedAt)
                put("rating", log.rating)
                put("previousInterval", log.previousInterval)
                put("newInterval", log.newInterval)
                put("previousStability", log.previousStability)
                put("newStability", log.newStability)
                put("wasOffline", log.wasOffline)
                if (log.responseTimeMs != null) put("responseTimeMs", log.responseTimeMs)
                if (log.sessionId != null) put("sessionId", log.sessionId)
            }
            logsArray.put(item)
        }
        root.put("reviewLogs", logsArray)

        val weakPointsArray = JSONArray()
        weakPoints.forEach { wp ->
            val item = JSONObject().apply {
                put("id", wp.id)
                put("verseKey", wp.verseKey)
                if (wp.wordStart != null) put("wordStart", wp.wordStart)
                if (wp.wordEnd != null) put("wordEnd", wp.wordEnd)
                put("reason", wp.reason)
                put("severity", wp.severity)
                put("createdAt", wp.createdAt)
                if (wp.resolvedAt != null) put("resolvedAt", wp.resolvedAt)
                if (wp.linkedCardId != null) put("linkedCardId", wp.linkedCardId)
            }
            weakPointsArray.put(item)
        }
        root.put("weakPoints", weakPointsArray)

        val sessionsArray = JSONArray()
        sessions.forEach { session ->
            sessionsArray.put(JSONObject().apply {
                put("id", session.id)
                put("startedAt", session.startedAt)
                if (session.endedAt != null) put("endedAt", session.endedAt)
                put("plannedMinutes", session.plannedMinutes)
                put("actualSeconds", session.actualSeconds)
                put("newCardsCount", session.newCardsCount)
                put("reviewCardsCount", session.reviewCardsCount)
                put("successCount", session.successCount)
                put("failureCount", session.failureCount)
                put("sessionType", session.sessionType)
            })
        }
        root.put("studySessions", sessionsArray)

        return root.toString(2)
    }

    fun parseAndValidate(jsonString: String): BackupSummary {
        if (jsonString.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
            return BackupSummary(isValid = false, errorMessage = "Sauvegarde trop volumineuse (maximum 10 Mo)")
        }
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", -1)
            if (version != CURRENT_BACKUP_VERSION) {
                return BackupSummary(
                    isValid = false,
                    version = version,
                    errorMessage = "Version de sauvegarde incompatible : $version (attendue: $CURRENT_BACKUP_VERSION)"
                )
            }
            val exportedAt = root.optString("exportedAt", "Date inconnue")

            val prefObj = root.optJSONObject("preferences")
            val preferences = prefObj?.let {
                val startA = it.optInt("startAyah", 1)
                UserPreferences(
                    onboardingComplete = it.optBoolean("onboardingComplete", true),
                    dailyMinutes = it.optInt("dailyMinutes", 15),
                    level = it.optString("level", "Débutant"),
                    startSurah = it.optInt("startSurah", 1),
                    startAyah = startA,
                    endAyah = it.optInt("endAyah", startA),
                    currentAyah = it.optInt("currentAyah", startA),
                    dailyAyahGoal = it.optInt("dailyAyahGoal", 1),
                    showTransliteration = it.optBoolean("showTransliteration", false),
                    showTranslation = it.optBoolean("showTranslation", false),
                    showTafsir = it.optBoolean("showTafsir", false)
                )
            }

            val cardsJson = root.optJSONArray("cards") ?: JSONArray()
            if (cardsJson.length() > MAX_CARDS) {
                return BackupSummary(isValid = false, version = version, errorMessage = "Trop de cartes dans la sauvegarde")
            }
            val cards = mutableListOf<LearningCardEntity>()
            for (i in 0 until cardsJson.length()) {
                val item = cardsJson.getJSONObject(i)
                cards += LearningCardEntity(
                    id = item.getString("id"),
                    type = item.getString("type"),
                    surahId = item.getInt("surahId"),
                    ayahStart = item.getInt("ayahStart"),
                    ayahEnd = item.getInt("ayahEnd"),
                    wordStart = if (item.has("wordStart")) item.getInt("wordStart") else null,
                    wordEnd = if (item.has("wordEnd")) item.getInt("wordEnd") else null,
                    prompt = item.getString("prompt"),
                    answer = item.getString("answer"),
                    createdAt = item.getLong("createdAt"),
                    lastReviewedAt = if (item.has("lastReviewedAt")) item.getLong("lastReviewedAt") else null,
                    nextReviewAt = item.getLong("nextReviewAt"),
                    intervalDays = item.getDouble("intervalDays"),
                    stability = item.getDouble("stability"),
                    difficulty = item.getDouble("difficulty"),
                    lapseCount = item.getInt("lapseCount"),
                    successCount = item.getInt("successCount"),
                    hesitationCount = item.getInt("hesitationCount"),
                    isWeak = item.getBoolean("isWeak"),
                    suspended = item.getBoolean("suspended"),
                    sourceCardId = if (item.has("sourceCardId")) item.getString("sourceCardId") else null,
                    userNote = if (item.has("userNote")) item.getString("userNote") else null,
                    estimatedDurationSeconds = item.optInt("estimatedDurationSeconds", 30),
                    lastResult = if (item.has("lastResult")) item.getString("lastResult") else null,
                    algorithmVersion = item.optInt("algorithmVersion", 1),
                    promptMode = item.optString("promptMode", "RECITATION"),
                    contextBeforeCount = item.optInt("contextBeforeCount", 0),
                    contextAfterCount = item.optInt("contextAfterCount", 0)
                )
            }

            val logsJson = root.optJSONArray("reviewLogs") ?: JSONArray()
            if (logsJson.length() > MAX_LOGS) {
                return BackupSummary(isValid = false, version = version, errorMessage = "Trop de journaux de révision")
            }
            val logs = mutableListOf<ReviewLogEntity>()
            for (i in 0 until logsJson.length()) {
                val item = logsJson.getJSONObject(i)
                logs += ReviewLogEntity(
                    id = 0,
                    cardId = item.getString("cardId"),
                    reviewedAt = item.getLong("reviewedAt"),
                    rating = item.getString("rating"),
                    previousInterval = item.getDouble("previousInterval"),
                    newInterval = item.getDouble("newInterval"),
                    previousStability = item.getDouble("previousStability"),
                    newStability = item.getDouble("newStability"),
                    wasOffline = item.optBoolean("wasOffline", true),
                    responseTimeMs = if (item.has("responseTimeMs")) item.getLong("responseTimeMs") else null,
                    sessionId = if (item.has("sessionId")) item.getString("sessionId") else null
                )
            }

            val weakPointsJson = root.optJSONArray("weakPoints") ?: JSONArray()
            if (weakPointsJson.length() > MAX_WEAK_POINTS) {
                return BackupSummary(isValid = false, version = version, errorMessage = "Trop de points faibles")
            }
            val weakPoints = mutableListOf<WeakPointEntity>()
            for (i in 0 until weakPointsJson.length()) {
                val item = weakPointsJson.getJSONObject(i)
                weakPoints += WeakPointEntity(
                    id = item.getString("id"),
                    verseKey = item.getString("verseKey"),
                    wordStart = if (item.has("wordStart")) item.getInt("wordStart") else null,
                    wordEnd = if (item.has("wordEnd")) item.getInt("wordEnd") else null,
                    reason = item.getString("reason"),
                    severity = item.getInt("severity"),
                    createdAt = item.getLong("createdAt"),
                    resolvedAt = if (item.has("resolvedAt")) item.getLong("resolvedAt") else null,
                    linkedCardId = if (item.has("linkedCardId")) item.getString("linkedCardId") else null
                )
            }

            val sessionsJson = root.optJSONArray("studySessions") ?: JSONArray()
            if (sessionsJson.length() > MAX_SESSIONS) {
                return BackupSummary(isValid = false, version = version, errorMessage = "Trop de séances d'étude")
            }
            val sessions = mutableListOf<StudySessionEntity>()
            for (i in 0 until sessionsJson.length()) {
                val item = sessionsJson.getJSONObject(i)
                sessions += StudySessionEntity(
                    id = item.getString("id"),
                    startedAt = item.getLong("startedAt"),
                    endedAt = if (item.has("endedAt")) item.getLong("endedAt") else null,
                    plannedMinutes = item.optInt("plannedMinutes", 0),
                    actualSeconds = item.getInt("actualSeconds"),
                    newCardsCount = item.optInt("newCardsCount", 0),
                    reviewCardsCount = item.optInt("reviewCardsCount", 0),
                    successCount = item.optInt("successCount", 0),
                    failureCount = item.optInt("failureCount", 0),
                    sessionType = item.optString("sessionType", "CARD_REVIEW")
                )
            }

            val validationError = validateContent(cards, logs, weakPoints, sessions, preferences)
            if (validationError != null) {
                return BackupSummary(isValid = false, version = version, errorMessage = validationError)
            }

            BackupSummary(
                isValid = true,
                version = version,
                cardsCount = cards.size,
                logsCount = logs.size,
                weakPointsCount = weakPoints.size,
                sessionsCount = sessions.size,
                exportedAt = exportedAt,
                cards = cards,
                logs = logs,
                weakPoints = weakPoints,
                sessions = sessions,
                preferences = preferences
            )
        } catch (e: Exception) {
            BackupSummary(
                isValid = false,
                errorMessage = "Format de sauvegarde invalide : ${e.localizedMessage}"
            )
        }
    }

    private fun validateContent(
        cards: List<LearningCardEntity>,
        logs: List<ReviewLogEntity>,
        weakPoints: List<WeakPointEntity>,
        sessions: List<StudySessionEntity>,
        preferences: UserPreferences?
    ): String? {
        val allowedTypes = CardType.entries.mapTo(mutableSetOf()) { it.name }
        val allowedRatings = Rating.entries.mapTo(mutableSetOf()) { it.name }
        val cardIds = cards.map { it.id }
        if (cardIds.any { it.isBlank() || it.length > 200 } || cardIds.distinct().size != cardIds.size) {
            return "Identifiants de cartes vides, trop longs ou dupliqués"
        }
        cards.forEach { card ->
            if (card.type !in allowedTypes) return "Type de carte inconnu : ${card.type}"
            if (card.surahId !in 1..114 || card.ayahStart !in 1..286 || card.ayahEnd !in card.ayahStart..286) {
                return "Plage coranique invalide pour la carte ${card.id}"
            }
            if ((card.wordStart == null) != (card.wordEnd == null) ||
                (card.wordStart != null && (card.wordStart < 1 || card.wordEnd!! < card.wordStart))) {
                return "Plage de mots invalide pour la carte ${card.id}"
            }
            if (card.prompt.length > MAX_TEXT_LENGTH || card.answer.length > MAX_TEXT_LENGTH) {
                return "Texte excessivement long pour la carte ${card.id}"
            }
            if (!card.intervalDays.isFinite() || card.intervalDays < 0.0 ||
                !card.stability.isFinite() || card.stability < 0.0 ||
                !card.difficulty.isFinite() || card.difficulty !in 0.0..1.0) {
                return "Paramètres SRS invalides pour la carte ${card.id}"
            }
            if (card.createdAt < 0 || card.nextReviewAt < 0 || card.lastReviewedAt?.let { it < 0 } == true ||
                card.lapseCount < 0 || card.successCount < 0 || card.hesitationCount < 0 ||
                card.estimatedDurationSeconds !in 1..3600 || card.algorithmVersion !in 1..100) {
                return "Compteurs ou dates invalides pour la carte ${card.id}"
            }
            if (card.lastResult != null && card.lastResult !in allowedRatings) {
                return "Résultat SRS inconnu pour la carte ${card.id}"
            }
            if (card.promptMode !in ALLOWED_PROMPT_MODES || card.contextBeforeCount !in 0..20 || card.contextAfterCount !in 0..20) {
                return "Contexte de récitation invalide pour la carte ${card.id}"
            }
        }

        val knownCards = cardIds.toSet()
        val sessionIds = sessions.map { it.id }
        if (sessionIds.any { it.isBlank() || it.length > 200 } || sessionIds.distinct().size != sessionIds.size) {
            return "Identifiants de séances vides, trop longs ou dupliqués"
        }
        sessions.forEach { session ->
            if (session.startedAt < 0 || session.endedAt?.let { it < session.startedAt } == true ||
                session.plannedMinutes !in 0..1_440 || session.actualSeconds !in 0..86_400 ||
                session.newCardsCount < 0 || session.reviewCardsCount < 0 || session.successCount < 0 ||
                session.failureCount < 0 || session.sessionType !in ALLOWED_SESSION_TYPES) {
                return "Séance d'étude invalide : ${session.id}"
            }
        }
        val knownSessions = sessionIds.toSet()
        logs.forEach { log ->
            if (log.cardId !in knownCards) return "Journal sans carte correspondante : ${log.cardId}"
            if (log.rating !in allowedRatings) return "Évaluation inconnue : ${log.rating}"
            if (log.reviewedAt < 0 || !log.previousInterval.isFinite() || log.previousInterval < 0.0 ||
                !log.newInterval.isFinite() || log.newInterval < 0.0 ||
                !log.previousStability.isFinite() || log.previousStability < 0.0 ||
                !log.newStability.isFinite() || log.newStability < 0.0 ||
                log.responseTimeMs?.let { it !in 0..3_600_000 } == true ||
                log.sessionId?.let { it.isBlank() || it.length > 200 } == true) {
                return "Journal de révision invalide pour ${log.cardId}"
            }
            if (log.sessionId != null && log.sessionId !in knownSessions) {
                return "Journal sans séance correspondante : ${log.cardId}"
            }
        }

        val weakIds = weakPoints.map { it.id }
        if (weakIds.any { it.isBlank() || it.length > 200 } || weakIds.distinct().size != weakIds.size) {
            return "Identifiants de points faibles vides, trop longs ou dupliqués"
        }
        val verseKeyPattern = Regex("^(?:[1-9]|[1-9][0-9]|1[01][0-4]):(?:[1-9]|[1-9][0-9]|[12][0-9]{2})$")
        weakPoints.forEach { point ->
            if (!verseKeyPattern.matches(point.verseKey) || point.severity !in 1..5 || point.createdAt < 0 ||
                point.resolvedAt?.let { it < point.createdAt } == true || point.reason.isBlank() ||
                point.reason.length > 2_000 || (point.wordStart == null) != (point.wordEnd == null) ||
                (point.wordStart != null && (point.wordStart < 1 || point.wordEnd!! < point.wordStart))) {
                return "Point faible invalide : ${point.id}"
            }
            if (point.linkedCardId != null && point.linkedCardId !in knownCards) {
                return "Point faible sans carte correspondante : ${point.id}"
            }
        }

        preferences?.let { pref ->
            if (pref.dailyMinutes !in 1..180 || pref.level !in ALLOWED_LEVELS ||
                pref.startSurah !in 1..114 || pref.startAyah !in 1..286 ||
                pref.endAyah !in pref.startAyah..286 || pref.currentAyah !in 1..286 || pref.dailyAyahGoal !in 1..100) {
                return "Préférences utilisateur hors limites"
            }
        }
        return null
    }

    private const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
    private const val MAX_CARDS = 20_000
    private const val MAX_LOGS = 200_000
    private const val MAX_WEAK_POINTS = 20_000
    private const val MAX_SESSIONS = 200_000
    private const val MAX_TEXT_LENGTH = 20_000
    private val ALLOWED_LEVELS = setOf("Débutant", "Intermédiaire", "Avancé")
    private val ALLOWED_PROMPT_MODES = setOf("RECITATION", "LISTENING", "SEMANTIC", "BRIDGE")
    private val ALLOWED_SESSION_TYPES = setOf("CARD_REVIEW", "LEARNING", "MIXED")
}

