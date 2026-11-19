package com.hifzsniper.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userDataStore by preferencesDataStore("user_preferences")

data class UserPreferences(
    val onboardingComplete: Boolean = false,
    val dailyMinutes: Int = 10,
    val level: String = "Débutant",
    val startSurah: Int = 1,
    val startAyah: Int = 1,
    val endAyah: Int = 1,
    val currentAyah: Int = startAyah,
    val dailyAyahGoal: Int = 1,
    val showTransliteration: Boolean = false,
    val showTranslation: Boolean = false,
    val showTafsir: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val minutes = intPreferencesKey("daily_minutes")
        val level = stringPreferencesKey("level")
        val surah = intPreferencesKey("start_surah")
        val ayah = intPreferencesKey("start_ayah")
        val endAyah = intPreferencesKey("end_ayah")
        val currentAyah = intPreferencesKey("current_ayah")
        val goal = intPreferencesKey("daily_ayah_goal")
        val transliteration = booleanPreferencesKey("show_transliteration")
        val translation = booleanPreferencesKey("show_translation")
        val tafsir = booleanPreferencesKey("show_tafsir")
    }

    val preferences: Flow<UserPreferences> = context.userDataStore.data.map { value ->
        val startA = value[Keys.ayah] ?: 1
        val endA = value[Keys.endAyah] ?: startA
        val currentA = value[Keys.currentAyah] ?: startA
        UserPreferences(
            onboardingComplete = value[Keys.onboarding] ?: false,
            dailyMinutes = value[Keys.minutes] ?: 10,
            level = value[Keys.level] ?: "Débutant",
            startSurah = value[Keys.surah] ?: 1,
            startAyah = startA,
            endAyah = endA,
            currentAyah = currentA,
            dailyAyahGoal = value[Keys.goal] ?: 1,
            showTransliteration = value[Keys.transliteration] ?: false,
            showTranslation = value[Keys.translation] ?: false,
            showTafsir = value[Keys.tafsir] ?: false
        )
    }

    suspend fun completeOnboarding(
        minutes: Int,
        level: String,
        surah: Int,
        ayah: Int,
        goal: Int,
        transliteration: Boolean = false,
        translation: Boolean = false,
        tafsir: Boolean = false
    ) {
        context.userDataStore.edit {
            it[Keys.onboarding] = true
            it[Keys.minutes] = minutes
            it[Keys.level] = level
            it[Keys.surah] = surah
            it[Keys.ayah] = ayah
            it[Keys.endAyah] = ayah
            it[Keys.currentAyah] = ayah
            it[Keys.goal] = goal
            it[Keys.transliteration] = transliteration
            it[Keys.translation] = translation
            it[Keys.tafsir] = tafsir
        }
    }

    suspend fun resetOnboarding() {
        context.userDataStore.edit {
            it[Keys.onboarding] = false
        }
    }

    suspend fun setDailyMinutes(value: Int) = context.userDataStore.edit { it[Keys.minutes] = value.coerceIn(1, 180) }
    suspend fun setLevel(value: String) = context.userDataStore.edit {
        if (value in setOf("Débutant", "Intermédiaire", "Avancé")) it[Keys.level] = value
    }
    suspend fun setDailyAyahGoal(value: Int) = context.userDataStore.edit { it[Keys.goal] = value.coerceIn(1, 100) }

    suspend fun setStartPoint(surah: Int, ayah: Int) = context.userDataStore.edit {
        it[Keys.surah] = surah
        it[Keys.ayah] = ayah
        it[Keys.endAyah] = ayah
        it[Keys.currentAyah] = ayah
    }

    suspend fun setVerseSet(surah: Int, startAyah: Int, endAyah: Int) = context.userDataStore.edit {
        it[Keys.surah] = surah
        it[Keys.ayah] = startAyah
        val clampedEnd = endAyah.coerceAtLeast(startAyah)
        it[Keys.endAyah] = clampedEnd
        it[Keys.currentAyah] = startAyah
    }

    suspend fun setCurrentAyah(surah: Int, ayah: Int) = context.userDataStore.edit {
        val previousSurah = it[Keys.surah]
        if (previousSurah != surah) {
            it[Keys.surah] = surah
            it[Keys.ayah] = ayah
            it[Keys.endAyah] = ayah
            it[Keys.currentAyah] = ayah
        } else {
            it[Keys.currentAyah] = ayah
        }
    }

    suspend fun setTransliteration(value: Boolean) = context.userDataStore.edit { it[Keys.transliteration] = value }
    suspend fun setTranslation(value: Boolean) = context.userDataStore.edit { it[Keys.translation] = value }
    suspend fun setTafsir(value: Boolean) = context.userDataStore.edit { it[Keys.tafsir] = value }

    suspend fun restoreAll(value: UserPreferences) = context.userDataStore.edit {
        it[Keys.onboarding] = value.onboardingComplete
        it[Keys.minutes] = value.dailyMinutes
        it[Keys.level] = value.level
        it[Keys.surah] = value.startSurah
        it[Keys.ayah] = value.startAyah
        it[Keys.endAyah] = value.endAyah
        it[Keys.currentAyah] = value.currentAyah
        it[Keys.goal] = value.dailyAyahGoal
        it[Keys.transliteration] = value.showTransliteration
        it[Keys.translation] = value.showTranslation
        it[Keys.tafsir] = value.showTafsir
    }
}
