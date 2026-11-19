package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.presentation.AppUiState

@Composable
fun HomeScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onStartReview: () -> Unit,
    onChoosePassage: () -> Unit,
    onOpenMushaf: () -> Unit = {},
    onOpenWeakPoints: () -> Unit,
    onOpenMutashabihat: () -> Unit = {},
    onInstallContent: () -> Unit
) = PageColumn(modifier) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text("Ta séance du jour", style = MaterialTheme.typography.headlineMedium)
        if (state.stats.currentStreak > 0) {
            Text(
                "🔥 ${state.stats.currentStreak} j",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
    Text("${state.preferences.dailyMinutes} minutes disponibles · ${state.stats.dueCards} carte(s) due(s)")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Plan du jour", style = MaterialTheme.typography.titleMedium)
            Text("${state.plannedCards.size} carte(s) planifiée(s) · ${state.stats.estimatedDueSeconds / 60} min estimées")
            Button(onClick = onStartReview, enabled = state.plannedCards.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text(if (state.plannedCards.isNotEmpty()) "Commencer ma séance" else "Aucune révision due")
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Portion coranique & Set de versets", style = MaterialTheme.typography.titleMedium)
            val passage = state.playerPassage
            val setStart = state.preferences.startAyah
            val setEnd = maxOf(setStart, state.preferences.endAyah)
            if (passage != null) {
                Text(
                    text = "${passage.surahNameTransliterated} (${passage.surahNameArabic}) · Set : Versets $setStart à $setEnd · Page ${passage.pageNumber ?: 1} / 604",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Point actuel : ${state.preferences.startSurah}:$setStart à $setEnd")
            }
            Button(onClick = onOpenMushaf, modifier = Modifier.fillMaxWidth()) {
                Text("📖 Ouvrir la Page ${state.playerPassage?.pageNumber ?: 1} du Mushaf")
            }
            OutlinedButton(onClick = onChoosePassage, modifier = Modifier.fillMaxWidth()) {
                Text("Sélectionner un Set de versets ou une page (1–604)")
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpenWeakPoints, modifier = Modifier.weight(1f)) {
            Text("Points faibles (${state.weakPoints.size})")
        }
        OutlinedButton(onClick = onOpenMutashabihat, modifier = Modifier.weight(1f)) {
            Text("Versets jumeaux (${state.mutashabihat.size})")
        }
    }
    if (!state.corpus.hasVerifiedContent) {
        EmptyStateCard(
            title = "Corpus non installé",
            message = "Le corpus Tanzil Uthmani 1.1 est inclus et prêt à être vérifié.",
            actionLabel = "Installer le corpus",
            onAction = onInstallContent
        )
    }
}
