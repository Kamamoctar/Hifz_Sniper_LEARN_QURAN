package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifzsniper.app.presentation.AppUiState
import com.hifzsniper.app.presentation.theme.QuranTypography

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    state: AppUiState? = null,
    onComplete: (Int, String, Int, Int, Int, Boolean, Boolean, Boolean) -> Unit,
    onFinished: () -> Unit
) = PageColumn(modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) } // 0 = Vision, 1 = Rythme & Objectif, 2 = Point de départ, 3 = Aides & Lancement

    // Form state prefilled from existing preferences if available
    var minutes by rememberSaveable { mutableIntStateOf(state?.preferences?.dailyMinutes ?: 10) }
    var level by rememberSaveable { mutableStateOf(state?.preferences?.level ?: "Débutant") }
    var surahText by rememberSaveable { mutableStateOf(state?.preferences?.startSurah?.toString() ?: "1") }
    var ayahText by rememberSaveable { mutableStateOf(state?.preferences?.startAyah?.toString() ?: "1") }
    var goalText by rememberSaveable { mutableStateOf(state?.preferences?.dailyAyahGoal?.toString() ?: "1") }
    var transliteration by rememberSaveable { mutableStateOf(state?.preferences?.showTransliteration ?: false) }
    var translation by rememberSaveable { mutableStateOf(state?.preferences?.showTranslation ?: false) }
    var tafsir by rememberSaveable { mutableStateOf(state?.preferences?.showTafsir ?: false) }

    val surahs = state?.surahs.orEmpty()
    val surahsById = surahs.associateBy { it.id }

    val currentSurahId = surahText.toIntOrNull() ?: 1
    val currentAyahNum = ayahText.toIntOrNull() ?: 1
    val selectedSurahMeta = surahsById[currentSurahId]

    // Top Stepper Navigation Header
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Étape ${step + 1} sur 4",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (step) {
                    0 -> "Méthode & Vision"
                    1 -> "Rythme & Profil"
                    2 -> "Point de départ"
                    else -> "Aides & Lancement"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stepper dots / pills
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (0..3).forEach { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                index <= step -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    // STEP 0: VISION ET MÉTHODE DU PRODUIT
    if (step == 0) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📖 🎯", fontSize = 36.sp)
                Text(
                    "Bienvenue dans Hifz-Sniper",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "L'application de mémorisation coranique ciblée combinant la tradition de transmission orale et la science cognitive de la répétition espacée.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Text("Les 4 piliers de la méthode :", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🧠 1. Répétition Espacée (SRS)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Chaque verset est planifié au moment idéal d'oubli calculé par l'algorithme SRS (SuperMemo/Anki optimisé) pour ancrer le Coran dans votre mémoire à long terme.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🧩 2. Méthode par Chunks & Ponts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Découpage en petites unités faciles à assimiler, puis création automatique de « cartes ponts » pour maîtriser la liaison fluide entre la fin d'un verset et le début du suivant.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🎧 3. Récitateur & Boucles Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Récitation certifiée de Mishary Al-Afasy, répétitions en boucle (1× à ∞), enchaînement automatique et reconnaissance vocale avec feedback visuel mot à mot.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📖 4. Navigation par page du corpus", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Navigation selon les 604 métadonnées de pages du corpus, avec police Amiri Quran et symboles de versets ۝. La mise en page reste une lecture guidée, pas un fac-similé du Mushaf imprimé.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🛡️", fontSize = 24.sp)
                Text(
                    "Texte et progression hors-ligne · Audio hors-ligne après téléchargement · Zéro compte, publicité ou télémétrie. La reconnaissance vocale peut dépendre du service de parole installé sur l'appareil.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = { step = 1 },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Définir mes objectifs ▶")
        }
    }

    // STEP 1: RYTHME, NIVEAU & OBJECTIF
    if (step == 1) {
        Text("Votre rythme d'apprentissage", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Une séance quotidienne courte et régulière est la clé de la persévérance. Choisissez votre disponibilité réelle :",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("⏱️ Temps quotidien disponible", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20, 30).forEach { value ->
                        FilterChip(
                            selected = minutes == value,
                            onClick = { minutes = value },
                            label = { Text("$value min") }
                        )
                    }
                }
                Text(
                    text = when (minutes) {
                        5 -> "Séance éclair : 1 verset et révision rapide des cartes les plus urgentes."
                        10 -> "Rythme idéal pour débuter : apprentissage ciblé et consolidation durable."
                        15 -> "Rythme recommandé : mémorisation active + révision complète du jour."
                        20 -> "Progression soutenue : apprentissage de portions entières et renforcement."
                        else -> "Rythme intensif : mémorisation avancée et grand cycle de révision."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🌱 Votre niveau estimé", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Débutant" to "🌱 Débutant",
                        "Intermédiaire" to "🌿 Intermédiaire",
                        "Avancé" to "🌳 Avancé / Huffadh"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = level == key,
                            onClick = { level = key },
                            label = { Text(label) }
                        )
                    }
                }
                Text(
                    text = when (level) {
                        "Débutant" -> "Premiers pas ou reprise de la mémorisation : découpage fin et consolidation guidée."
                        "Intermédiaire" -> "Plusieurs sourates ou Juz mémorisés : équilibre entre nouvelles portions et révisions."
                        else -> "Répertoire étendu : révisions ciblées, traque des points faibles et versets jumeaux (Mutashabihat)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🎯 Objectif indicatif par séance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1", "2", "3", "5", "10").forEach { v ->
                        FilterChip(
                            selected = goalText == v,
                            onClick = { goalText = v },
                            label = { Text("$v verset(s)") }
                        )
                    }
                }
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter(Char::isDigit) },
                    label = { Text("Nombre de versets par jour") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f)) {
                Text("◀ Précédent")
            }
            Button(
                onClick = { step = 2 },
                enabled = (goalText.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Continuer ▶")
            }
        }
    }

    // STEP 2: POINT DE DÉPART DE MÉMORISATION
    if (step == 2) {
        Text("Où souhaitez-vous commencer ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Choisissez la sourate et le verset où débutera votre mémorisation. Vous pourrez changer à tout moment.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text("Raccourcis les plus populaires :", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        // Popular shortcuts chips
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(1, 1, "⭐ Al-Fatihah (1)"),
                Triple(78, 1, "🌙 Juz 'Amma (78 An-Naba)"),
                Triple(67, 1, "👑 Al-Mulk (67)"),
                Triple(36, 1, "🕊️ Ya-Sin (36)"),
                Triple(2, 1, "📖 Al-Baqarah (2)")
            ).forEach { (s, a, label) ->
                FilterChip(
                    selected = surahText == s.toString() && ayahText == a.toString(),
                    onClick = {
                        surahText = s.toString()
                        ayahText = a.toString()
                    },
                    label = { Text(label) }
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Saisie précise de la portion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (selectedSurahMeta != null) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${selectedSurahMeta.id}. ${selectedSurahMeta.nameTransliterated}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("${selectedSurahMeta.ayahCount} versets au total", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                selectedSurahMeta.nameArabic,
                                style = QuranTypography.surahBannerTitle.copy(fontSize = 18.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = surahText,
                    onValueChange = { surahText = it.filter(Char::isDigit) },
                    label = { Text("Numéro de sourate (1–114)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ayahText,
                    onValueChange = { ayahText = it.filter(Char::isDigit) },
                    label = { Text("Verset de départ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val validSurah = surahText.toIntOrNull()?.takeIf { it in 1..114 }
        val maxAyah = selectedSurahMeta?.ayahCount ?: 286
        val validAyah = ayahText.toIntOrNull()?.takeIf { it in 1..maxAyah }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) {
                Text("◀ Précédent")
            }
            Button(
                onClick = { step = 3 },
                enabled = validSurah != null && validAyah != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Continuer ▶")
            }
        }
    }

    // STEP 3: AIDES ET LANCEMENT
    if (step == 3) {
        Text("Aides & Lancement", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💡 Règle pédagogique de Hifz-Sniper :", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Les aides sont désactivées par défaut pour forcer le rappel actif et stimuler la mémoire. Vous pouvez cependant les activer selon vos besoins et les modifier à tout moment.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Préférences d'affichage des aides :", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🔤 Translittération (phonétique)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Transcription en alphabet latin", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = transliteration, onCheckedChange = { transliteration = it })
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🇫🇷 Traduction française", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Compréhension du sens des versets", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = translation, onCheckedChange = { translation = it })
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("📚 Notes explicatives QuranEnc", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Contexte et explications succinctes", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = tafsir, onCheckedChange = { tafsir = it })
                }
            }
        }

        // Summary Card
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📋 Récapitulatif de votre programme :", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("⏱️ Temps : $minutes minutes par jour · 🌱 Niveau : $level")
                Text("🎯 Objectif : ${goalText.toIntOrNull() ?: 1} verset(s) par jour")
                Text("📍 Point de départ : Sourate ${surahText.toIntOrNull() ?: 1} · Verset ${ayahText.toIntOrNull() ?: 1}")
            }
        }

        val validSurah = surahText.toIntOrNull()?.takeIf { it in 1..114 } ?: 1
        val validAyah = ayahText.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val validGoal = goalText.toIntOrNull()?.takeIf { it > 0 } ?: 1

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) {
                Text("◀ Précédent")
            }
            Button(
                onClick = {
                    onComplete(
                        minutes,
                        level,
                        validSurah,
                        validAyah,
                        validGoal,
                        transliteration,
                        translation,
                        tafsir
                    )
                    onFinished()
                },
                modifier = Modifier.weight(1.5f)
            ) {
                Text("Commencer 🚀", fontWeight = FontWeight.Bold)
            }
        }
    }
}
