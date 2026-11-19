package com.hifzsniper.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingStartsAndMovesToDailyRhythmSettings() {
        composeRule.waitUntil(timeoutMillis = 120_000) {
            composeRule.onAllNodesWithText("Bienvenue dans Hifz-Sniper")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Bienvenue dans Hifz-Sniper").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Définir mes objectifs", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Définir mes objectifs", substring = true).performScrollTo().performClick()
        composeRule.onNodeWithText("Votre rythme d'apprentissage").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Temps quotidien disponible", substring = true).performScrollTo().assertIsDisplayed()
    }
}
