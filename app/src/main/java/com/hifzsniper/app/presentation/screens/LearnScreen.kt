package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.domain.AyahChunker
import com.hifzsniper.app.domain.PlayerWord
import com.hifzsniper.app.domain.Rating
import com.hifzsniper.app.presentation.AppUiState
import com.hifzsniper.app.presentation.theme.QuranTypography
import kotlinx.coroutines.delay

enum class HifzStep(val title: String, val subtitle: String) {
    PRE_LISTEN("Phase A · Pré-écoute", "Écoute attentive et repérage du texte"),
    CHUNK_STUDY("Phases B & C · Talaqqi assisté", "Découpage en petits groupes et masquage progressif"),
    CHAINING("Phase D · Chaînage", "Liaison progressive des groupes du verset"),
    DISTRACTION("Phase E · Délai de rappel", "Courte interruption avant le rappel sans assistance"),
    FINAL_RECALL("Phase E · Rappel différé", "Récitation du verset complet sans assistance"),
    COMPLETED("Phase F · Pont de continuité", "Mémorisation validée et liaison au verset suivant")
}

@Composable
fun LearnScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onChoosePassage: () -> Unit,
    onOpenPlayer: () -> Unit,
    onAddPassage: () -> Unit,
    onOpenSources: () -> Unit,
    onRate: (String, Rating) -> Unit,
    onCreateBridge: (String, String) -> Unit = { _, _ -> },
    onCreateChunkCard: (String, Int, Int, List<PlayerWord>) -> Unit = { _, _, _, _ -> },
    onToggleTransliteration: (Boolean) -> Unit = {},
    onToggleTranslation: (Boolean) -> Unit = {},
    onToggleTafsir: (Boolean) -> Unit = {},
    onFlagWeakPoint: (String, String) -> Unit = { _, _ -> },
    onNextPassage: () -> Unit = {}
) = PageColumn(modifier) {
    val passage = state.playerPassage
    val surahId = state.preferences.startSurah
    val ayahNumber = state.preferences.startAyah
    val currentVerseKey = "$surahId:$ayahNumber"
    val selectedCard = state.cards.firstOrNull { it.id == "ayah-$surahId-$ayahNumber" }

    Text("Protocole Hifz-Time", style = MaterialTheme.typography.headlineMedium)

    if (!state.corpus.hasVerifiedContent) {
        EmptyStateCard(
            title = "Corpus coranique requis",
            message = "Installe et valide d'abord le corpus Tanzil Uthmani 1.1 pour démarrer les séances.",
            actionLabel = "Ouvrir les sources",
            onAction = onOpenSources
        )
        return@PageColumn
    }

    if (passage == null) {
        EmptyStateCard(
            title = "Passage non chargé",
            message = "Choisis un verset pour lancer la séance d'apprentissage.",
            actionLabel = "Choisir un passage",
            onAction = onChoosePassage
        )
        return@PageColumn
    }

    val chunks = remember(passage.words) { AyahChunker.chunk(passage.words) }
    var currentStep by rememberSaveable(currentVerseKey) { mutableStateOf(HifzStep.PRE_LISTEN) }
    var activeChunkIndex by rememberSaveable(currentVerseKey) { mutableIntStateOf(0) }
    var chainLevel by rememberSaveable(currentVerseKey) { mutableIntStateOf(minOf(2, chunks.size).coerceAtLeast(1)) }
    var chunkMaskMode by rememberSaveable(currentVerseKey, activeChunkIndex) { mutableIntStateOf(1) } // 1=full, 2=partial, 3=hidden
    var chunkRevealed by rememberSaveable(currentVerseKey, activeChunkIndex, chunkMaskMode) { mutableStateOf(false) }
    var preListenCount by rememberSaveable(currentVerseKey) { mutableIntStateOf(0) }
    var bridgeCreated by rememberSaveable(currentVerseKey) { mutableStateOf(false) }

    // Step progress indicator
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(currentStep.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Verset $currentVerseKey", style = MaterialTheme.typography.bodyMedium)
            }
            Text(currentStep.subtitle, style = MaterialTheme.typography.bodySmall)
            val stepFraction = when (currentStep) {
                HifzStep.PRE_LISTEN -> 0.2f
                HifzStep.CHUNK_STUDY -> 0.4f
                HifzStep.CHAINING -> 0.65f
                HifzStep.DISTRACTION -> 0.78f
                HifzStep.FINAL_RECALL -> 0.9f
                HifzStep.COMPLETED -> 1.0f
            }
            LinearProgressIndicator(progress = { stepFraction }, modifier = Modifier.fillMaxWidth())
        }
    }

    // Aides rapides (Phonétique, Traduction, Tafsir)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Aides :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        FilterChip(
            selected = state.preferences.showTransliteration,
            onClick = { onToggleTransliteration(!state.preferences.showTransliteration) },
            label = { Text(if (state.preferences.showTransliteration) "✓ 🔤 Phonétique" else "🔤 Phonétique") }
        )
        FilterChip(
            selected = state.preferences.showTranslation,
            onClick = { onToggleTranslation(!state.preferences.showTranslation) },
            label = { Text(if (state.preferences.showTranslation) "✓ 🇫🇷 Traduction" else "🇫🇷 Traduction") }
        )
        FilterChip(
            selected = state.preferences.showTafsir,
            onClick = { onToggleTafsir(!state.preferences.showTafsir) },
            label = { Text(if (state.preferences.showTafsir) "✓ 📚 Notes" else "📚 Notes") }
        )
    }

    when (currentStep) {
        HifzStep.PRE_LISTEN -> {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Texte coranique (Écriture Uthmanie)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = passage.textUthmani,
                        style = QuranTypography.verseText,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.preferences.showTransliteration) {
                        val translit = passage.effectiveTransliteration
                        if (translit.isNotBlank()) {
                            Text(
                                text = "Phonétique : $translit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (state.preferences.showTranslation && passage.translationFrench != null) {
                        Text("Traduction : ${passage.translationFrench}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.preferences.showTafsir && passage.explanatoryNotesFrench != null) {
                        Text("Notes : ${passage.explanatoryNotesFrench}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenPlayer, modifier = Modifier.weight(1f)) {
                    Text("Écouter l'audio")
                }
                Button(
                    onClick = {
                        currentStep = HifzStep.CHUNK_STUDY
                        activeChunkIndex = 0
                        chunkMaskMode = 1
                    },
                    enabled = preListenCount > 0 && chunks.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Passer au découpage")
                }
            }
            OutlinedButton(
                onClick = { preListenCount += 1 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (preListenCount == 0) "Confirmer une écoute attentive" else "Écoute confirmée ✓ ($preListenCount)")
            }
        }

        HifzStep.CHUNK_STUDY -> {
            val safeChunkIndex = activeChunkIndex.coerceIn(0, (chunks.size - 1).coerceAtLeast(0))
            val currentChunk = chunks.getOrNull(safeChunkIndex)

            // Selector tabs for chunks
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunks.forEachIndexed { idx, _ ->
                    FilterChip(
                        selected = idx == safeChunkIndex,
                        onClick = {
                            activeChunkIndex = idx
                            chunkMaskMode = 1
                            chunkRevealed = false
                        },
                        label = { Text("Groupe ${idx + 1}/${chunks.size}") }
                    )
                }
            }

            if (currentChunk != null) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Groupe ${currentChunk.index} sur ${currentChunk.totalChunks}", style = MaterialTheme.typography.titleMedium)
                            Text(when (chunkMaskMode) {
                                1 -> "1/3 : Texte complet"
                                2 -> "2/3 : Masquage partiel"
                                else -> "3/3 : Texte masqué"
                            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        val displayText = when {
                            chunkRevealed -> currentChunk.text
                            chunkMaskMode == 1 -> currentChunk.text
                            chunkMaskMode == 2 -> currentChunk.partiallyMaskedText()
                            else -> currentChunk.fullyMaskedText()
                        }

                        Text(
                            text = displayText,
                            style = QuranTypography.verseText,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val segmentStart = currentChunk.words.mapNotNull { it.startMs }.minOrNull()
                        val segmentEnd = currentChunk.words.mapNotNull { it.endMs }.maxOrNull()
                        val playableSources = passage.playableUris
                        if (playableSources.isNotEmpty() && segmentStart != null && segmentEnd != null && segmentEnd > segmentStart) {
                            SegmentAudioPlayer(
                                sources = playableSources,
                                startMs = segmentStart,
                                endMs = segmentEnd
                            )
                        } else {
                            OutlinedButton(onClick = onOpenPlayer, modifier = Modifier.fillMaxWidth()) {
                                Text("Écouter le verset complet (timings de groupe indisponibles)")
                            }
                        }

                        if (!chunkRevealed && chunkMaskMode > 1) {
                            Text("Récite de mémoire, puis vérifie.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { chunkRevealed = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Révéler la réponse")
                            }
                        } else if (chunkMaskMode == 1) {
                            Button(onClick = {
                                chunkMaskMode = 2
                                chunkRevealed = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("J'ai répété · Masquer partiellement")
                            }
                        } else {
                            Text("Auto-évaluation de ce groupe :", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        onCreateChunkCard(currentVerseKey, currentChunk.index, currentChunk.totalChunks, currentChunk.words)
                                        onFlagWeakPoint(currentVerseKey, "Hésitation sur groupe ${currentChunk.index}")
                                        chunkMaskMode = 1
                                        chunkRevealed = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Raté") }

                                Button(
                                    onClick = {
                                        chunkMaskMode = 2
                                        chunkRevealed = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Hésitant") }

                                Button(
                                    onClick = {
                                        if (chunkMaskMode < 3) {
                                            chunkMaskMode = 3
                                            chunkRevealed = false
                                        } else {
                                            // Chunk mastered
                                            if (safeChunkIndex + 1 < chunks.size) {
                                                activeChunkIndex = safeChunkIndex + 1
                                                chunkMaskMode = 1
                                                chunkRevealed = false
                                            } else {
                                                // All chunks studied -> Chaining
                                                currentStep = HifzStep.CHAINING
                                                chainLevel = 2
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (chunkMaskMode < 3) "Continuer" else "Fluide ✓") }
                            }
                        }
                    }
                }
            }
        }

        HifzStep.CHAINING -> {
            val chainedWords = chunks.take(chainLevel).flatMap { it.words }
            val chainedText = chainedWords.joinToString(" ") { it.text }
            var chainRevealed by rememberSaveable(chainLevel) { mutableStateOf(false) }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chaînage cumulatif : Groupes 1 à $chainLevel", style = MaterialTheme.typography.titleMedium)
                    Text("Récite les groupes enchaînés pour assurer la continuité orale.")

                    Text(
                        text = if (chainRevealed) chainedText else chainedWords.joinToString(" ") { "···" },
                        style = QuranTypography.verseText,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(onClick = { chainRevealed = !chainRevealed }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (chainRevealed) "Masquer" else "Révéler pour vérifier")
                    }

                    if (chainRevealed) {
                        Button(
                            onClick = {
                                if (chainLevel < chunks.size) {
                                    chainLevel += 1
                                    chainRevealed = false
                                } else {
                                    currentStep = HifzStep.DISTRACTION
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (chainLevel < chunks.size) "Enchaîner le groupe suivant" else "Passer au rappel du verset complet")
                        }
                    }
                }
            }
        }

        HifzStep.DISTRACTION -> {
            var secondsRemaining by rememberSaveable(currentVerseKey) { mutableIntStateOf(15) }
            LaunchedEffect(currentVerseKey) {
                while (secondsRemaining > 0) {
                    delay(1_000)
                    secondsRemaining -= 1
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Interruption active", style = MaterialTheme.typography.titleMedium)
                    Text("Pendant cette courte pause, rappelle-toi le sens général du verset sans regarder son texte.")
                    Text(
                        if (secondsRemaining > 0) "Rappel disponible dans $secondsRemaining s" else "Le rappel différé est prêt.",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { currentStep = HifzStep.FINAL_RECALL },
                        enabled = secondsRemaining == 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Commencer le rappel différé")
                    }
                }
            }
        }

        HifzStep.FINAL_RECALL -> {
            VoiceRecitationCard(
                expectedTextUthmani = passage.textUthmani,
                prompt = "Phase E · Rappel actif au micro",
                onRate = { rating ->
                    onAddPassage()
                    selectedCard?.id?.let { onRate(it, rating) }
                    if (rating == Rating.FAILED) {
                        onFlagWeakPoint(currentVerseKey, "Échec lors du rappel final")
                        currentStep = HifzStep.CHUNK_STUDY
                        activeChunkIndex = 0
                        chunkMaskMode = 1
                    } else {
                        currentStep = HifzStep.COMPLETED
                    }
                }
            )
        }

        HifzStep.COMPLETED -> {
            val currentSurahSummary = state.surahs.firstOrNull { it.id == surahId }
            val nextAyah = if (currentSurahSummary != null && ayahNumber < currentSurahSummary.ayahCount) ayahNumber + 1 else 1
            val nextSurah = if (currentSurahSummary != null && ayahNumber < currentSurahSummary.ayahCount) surahId else (surahId + 1).coerceAtMost(114)
            val nextVerseKey = "$nextSurah:$nextAyah"

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mémorisation validée !", style = MaterialTheme.typography.titleLarge)
                    Text("Le verset $currentVerseKey est désormais intégré à ton cycle de révision espacée.")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pont de continuité vers $nextVerseKey", style = MaterialTheme.typography.titleMedium)
                    Text("Un pont relie les derniers mots de ce verset aux premiers mots du suivant pour ancrer la transition.")

                    Button(
                        onClick = {
                            onCreateBridge(currentVerseKey, nextVerseKey)
                            bridgeCreated = true
                        },
                        enabled = !bridgeCreated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (bridgeCreated) "Pont de continuité créé ✓" else "Créer le pont de continuité ($currentVerseKey → $nextVerseKey)")
                    }
                }
            }

            Button(
                onClick = {
                    onNextPassage()
                    currentStep = HifzStep.PRE_LISTEN
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Passer au verset suivant ($nextVerseKey)")
            }

            OutlinedButton(onClick = onChoosePassage, modifier = Modifier.fillMaxWidth()) {
                Text("Choisir un autre passage")
            }
        }
    }

    OutlinedButton(onClick = onChoosePassage, modifier = Modifier.fillMaxWidth()) {
        Text("Changer de passage (${surahId}:${ayahNumber})")
    }
}
