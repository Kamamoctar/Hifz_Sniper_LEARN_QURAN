package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.domain.Rating
import com.hifzsniper.app.presentation.AppUiState

@Composable
fun ReviewScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onRate: (String, Rating) -> Unit,
    onChoosePassage: () -> Unit
) = PageColumn(modifier) {
    var reviewMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Récitation Vocale, 1 = Cartes

    Text("Révision intelligente", style = MaterialTheme.typography.headlineMedium)

    if (state.plannedCards.isEmpty()) {
        EmptyStateCard(
            title = "Aucune carte planifiée",
            message = "Toutes les révisions sont à jour pour aujourd'hui ! Reviens demain pour prolonger ta série.",
            actionLabel = "Choisir une nouvelle portion",
            onAction = onChoosePassage
        )
    } else {
        // Mode Selector: Vocal Speech vs Classic Cards
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = reviewMode == 0,
                onClick = { reviewMode = 0 },
                label = { Text("🎙️ Récitation Vocale") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = reviewMode == 1,
                onClick = { reviewMode = 1 },
                label = { Text("🃏 Cartes classiques") },
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            "${state.plannedCards.size} carte(s) due(s) · ~${state.stats.estimatedDueSeconds / 60} min",
            style = MaterialTheme.typography.bodySmall
        )

        if (reviewMode == 0) {
            // Mode Vocal Interactif (carte par carte au micro)
            val currentCard = state.plannedCards.firstOrNull()
            if (currentCard != null) {
                LinearProgressIndicator(
                    progress = { 1f / state.plannedCards.size.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )

                VoiceRecitationCard(
                    expectedTextUthmani = currentCard.answer,
                    prompt = currentCard.prompt,
                    onRate = { rating -> onRate(currentCard.id, rating) }
                )
            }
        } else {
            // Mode Classique : liste des cartes d'auto-évaluation
            state.plannedCards.forEach { card ->
                ActiveRecallCard(card, onRate, showTransliteration = state.preferences.showTransliteration)
            }
        }
    }
}
