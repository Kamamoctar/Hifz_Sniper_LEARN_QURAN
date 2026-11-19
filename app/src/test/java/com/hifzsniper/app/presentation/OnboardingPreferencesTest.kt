package com.hifzsniper.app.presentation

import com.hifzsniper.app.data.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPreferencesTest {

    @Test
    fun `default user preferences have onboarding incomplete and all aides disabled`() {
        val prefs = UserPreferences()

        assertFalse("L'onboarding doit être incomplet par défaut", prefs.onboardingComplete)
        assertEquals(10, prefs.dailyMinutes)
        assertEquals("Débutant", prefs.level)
        assertEquals(1, prefs.startSurah)
        assertEquals(1, prefs.startAyah)
        assertEquals(1, prefs.dailyAyahGoal)

        // Superprompt section 4 & 16.5: Toutes les aides doivent être désactivées par défaut
        assertFalse("Translittération désactivée par défaut", prefs.showTransliteration)
        assertFalse("Traduction française désactivée par défaut", prefs.showTranslation)
        assertFalse("Tafsir désactivé par défaut", prefs.showTafsir)
    }

    @Test
    fun `completed onboarding configuration sets custom parameters and flags completion`() {
        val completed = UserPreferences(
            onboardingComplete = true,
            dailyMinutes = 20,
            level = "Avancé",
            startSurah = 78,
            startAyah = 1,
            dailyAyahGoal = 3,
            showTransliteration = true,
            showTranslation = false,
            showTafsir = true
        )

        assertTrue(completed.onboardingComplete)
        assertEquals(20, completed.dailyMinutes)
        assertEquals("Avancé", completed.level)
        assertEquals(78, completed.startSurah)
        assertEquals(1, completed.startAyah)
        assertEquals(3, completed.dailyAyahGoal)
        assertTrue(completed.showTransliteration)
        assertFalse(completed.showTranslation)
        assertTrue(completed.showTafsir)
    }

    @Test
    fun `resetting onboarding returns onboardingComplete to false`() {
        val active = UserPreferences(
            onboardingComplete = true,
            dailyMinutes = 15,
            level = "Intermédiaire",
            startSurah = 67,
            startAyah = 1,
            dailyAyahGoal = 5
        )

        val reset = active.copy(onboardingComplete = false)
        assertFalse(reset.onboardingComplete)
        // Les préférences utilisateur sont conservées lors d'un simple reset d'onboarding
        assertEquals(15, reset.dailyMinutes)
        assertEquals("Intermédiaire", reset.level)
        assertEquals(67, reset.startSurah)
    }

    @Test
    fun `verse set preserves set boundaries when currentAyah advances`() {
        val setPrefs = UserPreferences(
            startSurah = 1,
            startAyah = 1,
            endAyah = 7,
            currentAyah = 1
        )

        assertEquals("Le début du set doit être 1", 1, setPrefs.startAyah)
        assertEquals("La fin du set doit être 7", 7, setPrefs.endAyah)
        assertEquals("Le verset en cours initial est 1", 1, setPrefs.currentAyah)

        // Simulation de l'avancement dans le set : currentAyah passe à 2, puis 3
        val advanced = setPrefs.copy(currentAyah = 3)
        assertEquals("Le début du set ne doit PAS être écrasé lors de l'avancement", 1, advanced.startAyah)
        assertEquals("La fin du set ne doit PAS changer lors de l'avancement", 7, advanced.endAyah)
        assertEquals("Le verset en cours doit refléter la progression", 3, advanced.currentAyah)

        // Arrivée au dernier verset puis rebouclage vers startAyah
        val atEnd = advanced.copy(currentAyah = advanced.endAyah)
        assertEquals(7, atEnd.currentAyah)
        val looped = atEnd.copy(currentAyah = atEnd.startAyah)
        assertEquals("Le rebouclage doit revenir au début du set (1)", 1, looped.currentAyah)
        assertEquals("Les bornes du set doivent rester intactes", 1, looped.startAyah)
        assertEquals("Les bornes du set doivent rester intactes", 7, looped.endAyah)
    }

    @Test
    fun `default currentAyah equals startAyah`() {
        val prefs = UserPreferences(startAyah = 5, endAyah = 10)
        assertEquals(5, prefs.currentAyah)
    }
}

