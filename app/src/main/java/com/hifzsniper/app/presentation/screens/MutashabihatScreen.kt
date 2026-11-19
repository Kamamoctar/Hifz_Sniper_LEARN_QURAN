package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifzsniper.app.domain.MutashabihMatch
import com.hifzsniper.app.domain.MutashabihType
import com.hifzsniper.app.presentation.AppUiState
import com.hifzsniper.app.presentation.theme.QuranTypography
import com.hifzsniper.app.presentation.theme.ayahEndSign

@Composable
fun MutashabihatScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onSelectAyah: (Int, Int) -> Unit,
    onChoosePassage: () -> Unit
) = PageColumn(modifier) {
    val passage = state.playerPassage

    Text("Versets Similaires (Mutashabihat)", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Détecteur automatique des versets miroirs et des confusions fréquentes.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (passage == null) {
        EmptyStateCard(
            title = "Aucun verset sélectionné",
            message = "Choisis un verset dans le lecteur pour analyser ses similitudes.",
            actionLabel = "Choisir un passage",
            onAction = onChoosePassage
        )
        return@PageColumn
    }

    // Source Verse Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Verset étudié : ${passage.verseKey}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Sourate ${passage.surahNameTransliterated}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "${passage.textUthmani}${ayahEndSign(passage.ayahNumber)}",
                style = QuranTypography.verseText,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val matches = state.mutashabihat

    if (matches.isEmpty()) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aucun verset jumeau détecté", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ce verset ne présente pas de similitude majeure (début ou fin identique) avec un autre verset du Coran.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = onChoosePassage, modifier = Modifier.fillMaxWidth()) {
                    Text("Tester un autre verset")
                }
            }
        }
    } else {
        Text(
            "${matches.size} verset(s) jumeau(x) identifié(s) :",
            style = MaterialTheme.typography.titleMedium
        )

        matches.forEach { match ->
            MutashabihMatchCard(match = match, onNavigate = {
                onSelectAyah(match.targetSurahId, match.targetAyahNumber)
            })
        }
    }
}

@Composable
fun MutashabihMatchCard(
    match: MutashabihMatch,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${match.targetVerseKey} · ${match.targetSurahNameTransliterated} (${match.targetSurahNameArabic})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Verset ${match.targetAyahNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val badgeText = when (match.matchType) {
                    MutashabihType.PREFIX -> "Même début"
                    MutashabihType.SUFFIX -> "Même fin"
                    MutashabihType.HIGH_SIMILARITY -> "Forte ressemblance"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Target verse text
            Text(
                text = "${match.targetTextUthmani}${ayahEndSign(match.targetAyahNumber)}",
                style = QuranTypography.verseText,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Divergence description
            Text(
                text = "💡 ${match.divergenceDescription}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aller à ce verset jumeau")
            }
        }
    }
}

