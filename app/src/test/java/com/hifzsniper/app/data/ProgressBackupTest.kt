package com.hifzsniper.app.data

import com.hifzsniper.app.data.local.LearningCardEntity
import com.hifzsniper.app.data.local.ReviewLogEntity
import com.hifzsniper.app.data.local.WeakPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ProgressBackupTest {

    @Test
    fun `export and parse roundtrip preserves all entities and preferences`() {
        val now = Instant.parse("2026-05-10T14:30:00Z")
        val card = LearningCardEntity(
            id = "ayah-1-1",
            type = "AYAH",
            surahId = 1,
            ayahStart = 1,
            ayahEnd = 1,
            wordStart = null,
            wordEnd = null,
            prompt = "Récite 1:1",
            answer = "بِسْمِ ٱللَّهِ",
            createdAt = 1000L,
            lastReviewedAt = 2000L,
            nextReviewAt = 3000L,
            intervalDays = 1.0,
            stability = 2.0,
            difficulty = 0.5,
            lapseCount = 1,
            successCount = 3,
            hesitationCount = 0,
            isWeak = false,
            suspended = false,
            sourceCardId = null,
            userNote = "Note test",
            estimatedDurationSeconds = 45,
            lastResult = "FLUENT",
            algorithmVersion = 1
        )
        val log = ReviewLogEntity(
            id = 1,
            cardId = "ayah-1-1",
            reviewedAt = 2000L,
            rating = "FLUENT",
            previousInterval = 0.5,
            newInterval = 1.0,
            previousStability = 1.2,
            newStability = 2.0,
            wasOffline = true
        )
        val weakPoint = WeakPointEntity(
            id = "wp-1",
            verseKey = "1:1",
            wordStart = 1,
            wordEnd = 2,
            reason = "Hésitation sur basmala",
            severity = 1,
            createdAt = 1500L,
            resolvedAt = null,
            linkedCardId = "ayah-1-1"
        )
        val preferences = UserPreferences(
            onboardingComplete = true,
            dailyMinutes = 20,
            level = "Intermédiaire",
            startSurah = 2,
            startAyah = 255,
            endAyah = 255,
            dailyAyahGoal = 5,
            showTransliteration = true,
            showTranslation = false,
            showTafsir = true
        )

        val json = ProgressBackupService.exportToJson(
            cards = listOf(card),
            logs = listOf(log),
            weakPoints = listOf(weakPoint),
            preferences = preferences,
            now = now
        )

        assertNotNull(json)
        assertTrue(json.contains("\"app\": \"Hifz-Sniper\""))

        val summary = ProgressBackupService.parseAndValidate(json)
        assertTrue(summary.isValid)
        assertEquals(1, summary.version)
        assertEquals(1, summary.cardsCount)
        assertEquals(1, summary.logsCount)
        assertEquals(1, summary.weakPointsCount)
        assertEquals("2026-05-10T14:30:00Z", summary.exportedAt)

        val parsedCard = summary.cards.single()
        assertEquals(card.id, parsedCard.id)
        assertEquals(card.prompt, parsedCard.prompt)
        assertEquals(card.answer, parsedCard.answer)
        assertEquals(card.intervalDays, parsedCard.intervalDays, 0.001)

        val parsedLog = summary.logs.single()
        assertEquals(log.cardId, parsedLog.cardId)
        assertEquals(log.rating, parsedLog.rating)

        val parsedWp = summary.weakPoints.single()
        assertEquals(weakPoint.id, parsedWp.id)
        assertEquals(weakPoint.reason, parsedWp.reason)

        val parsedPref = summary.preferences
        assertNotNull(parsedPref)
        assertEquals(20, parsedPref?.dailyMinutes)
        assertEquals(2, parsedPref?.startSurah)
        assertEquals(255, parsedPref?.startAyah)
        assertTrue(parsedPref?.showTransliteration == true)
        assertTrue(parsedPref?.showTranslation == false)
        assertTrue(parsedPref?.showTafsir == true)
    }

    @Test
    fun `incompatible version is rejected`() {
        val corruptedJson = """{"version": 99, "cards": []}"""
        val summary = ProgressBackupService.parseAndValidate(corruptedJson)
        assertFalse(summary.isValid)
        assertTrue(summary.errorMessage?.contains("incompatible") == true)
    }

    @Test
    fun `malformed json is safely caught and returns invalid summary`() {
        val badJson = "not a valid json string"
        val summary = ProgressBackupService.parseAndValidate(badJson)
        assertFalse(summary.isValid)
        assertNotNull(summary.errorMessage)
    }

    @Test
    fun `unknown card type is rejected before Room restore`() {
        val invalid = """{
          "version": 1,
          "cards": [{
            "id":"x", "type":"INJECTED", "surahId":1, "ayahStart":1, "ayahEnd":1,
            "prompt":"p", "answer":"a", "createdAt":1, "nextReviewAt":1,
            "intervalDays":0, "stability":1, "difficulty":0.5,
            "lapseCount":0, "successCount":0, "hesitationCount":0,
            "isWeak":false, "suspended":false, "estimatedDurationSeconds":30,
            "algorithmVersion":1
          }]
        }"""
        val summary = ProgressBackupService.parseAndValidate(invalid)
        assertFalse(summary.isValid)
        assertTrue(summary.errorMessage?.contains("Type de carte inconnu") == true)
    }

    @Test
    fun `review log without matching card is rejected`() {
        val invalid = """{
          "version": 1,
          "cards": [],
          "reviewLogs": [{
            "cardId":"missing", "reviewedAt":1, "rating":"FLUENT",
            "previousInterval":0, "newInterval":1,
            "previousStability":1, "newStability":2, "wasOffline":true
          }]
        }"""
        val summary = ProgressBackupService.parseAndValidate(invalid)
        assertFalse(summary.isValid)
        assertTrue(summary.errorMessage?.contains("sans carte") == true)
    }
}

