package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.presentation.AppUiState
import com.hifzsniper.app.presentation.ContentInstallPhase

@Composable
fun SourcesScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onInstallCorpus: () -> Unit
) = PageColumn(modifier) {
    Text("Sources et attributions", style = MaterialTheme.typography.headlineMedium)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Paquet de contenu vérifié", style = MaterialTheme.typography.titleLarge)
            Text(
                if (state.corpus.hasWordSynchronizedAudio && state.corpus.ayahCount == 6_236) {
                    "Installé : ${state.corpus.surahCount} sourates, ${state.corpus.ayahCount} versets, " +
                        "${state.corpus.wordCount} mots et ${state.corpus.audioTrackCount} pistes référencées."
                } else {
                    "Le paquet embarqué sera vérifié par SHA-256 avant une insertion atomique."
                }
            )
            if (state.contentInstall.phase == ContentInstallPhase.IMPORTING) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Text(state.contentInstall.message, style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onInstallCorpus,
                enabled = state.contentInstall.phase != ContentInstallPhase.IMPORTING &&
                    !state.corpus.hasWordSynchronizedAudio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.corpus.hasWordSynchronizedAudio) "Contenu installé" else "Vérifier et installer")
            }
        }
    }
    SourceCard(
        "Texte coranique",
        "Tanzil Uthmani 1.1, conservé sans modification. Licence CC BY 3.0 avec attribution et suivi des mises à jour.",
        "https://tanzil.net/docs/Text_License"
    )
    SourceCard(
        "Traduction française et notes",
        "Traduction de Rachid Maach 1.0.3 et ses notes explicatives, fournies par QuranEnc. " +
            "Le texte, la version et l’attribution sont conservés conformément aux conditions de republication.",
        "https://quranenc.com/fr/browse/french_rashid/"
    )
    SourceCard(
        "Audio Alafasy",
        "6 236 pistes HTTPS et leurs empreintes SHA-256 sont référencées. Aucun MP3 n’est inclus dans l’APK. " +
            "EveryAyah est utilisé en priorité, puis le miroir documenté Quran Foundation est essayé automatiquement. " +
            "Un téléchargement n’est accepté qu’après validation de son SHA-256.",
        "https://everyayah.com/data/Alafasy_128kbps/ · https://verses.quran.foundation/Alafasy/mp3/"
    )
    SourceCard(
        "Synchronisation mot à mot",
        "Timings quran-align, version du 24 novembre 2016, sous CC BY 4.0. " +
            "Les versets dont la segmentation ne correspond pas exactement utilisent le fallback au niveau du verset.",
        "https://github.com/cpfair/quran-align/releases/tag/release-2016-11-24"
    )
    SourceCard(
        "Notes explicatives",
        "Aucun tafsir français complet n’est présenté comme tel. L’option de lecture affiche uniquement les notes " +
            "explicatives attachées à la traduction QuranEnc.",
        "https://quranenc.com/fr/home/api/"
    )
}

@Composable
private fun SourceCard(title: String, description: String, source: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description)
            Text(source, style = MaterialTheme.typography.bodySmall)
        }
    }
}
