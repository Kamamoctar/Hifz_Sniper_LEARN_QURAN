package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.presentation.AppUiState

@Composable
fun WeakPointsScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onResolve: (String) -> Unit,
    onOpenContext: (String) -> Unit = {}
) = PageColumn(modifier) {
    Text("Points faibles", style = MaterialTheme.typography.headlineMedium)
    if (state.weakPoints.isEmpty()) {
        EmptyStateCard("Aucun point faible actif", "Après deux échecs sur une carte, un point faible est créé automatiquement sans supprimer l’historique.")
    } else {
        state.weakPoints.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Passage ${item.verseKey}", style = MaterialTheme.typography.titleMedium)
                    Text(item.reason)
                    if (item.wordStart != null && item.wordEnd != null) {
                        Text("Mots ${item.wordStart} à ${item.wordEnd}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Sévérité ${item.severity}", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { onOpenContext(item.verseKey) }) { Text("Ouvrir dans le lecteur") }
                    OutlinedButton(onClick = { onResolve(item.id) }) { Text("Marquer comme consolidé") }
                }
            }
        }
    }
}
