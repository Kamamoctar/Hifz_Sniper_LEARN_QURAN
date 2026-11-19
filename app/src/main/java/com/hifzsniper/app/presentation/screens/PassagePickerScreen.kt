package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifzsniper.app.presentation.AppUiState

@Composable
fun PassagePickerScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onSelect: (Int, Int) -> Unit,
    onSelectSet: (Int, Int, Int) -> Unit = { s, start, _ -> onSelect(s, start) },
    onSelectPage: (Int) -> Unit = {},
    onSelected: () -> Unit
) {
    var pickerMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Par Sourate / Set, 1 = Par Page
    var selectedSurah by rememberSaveable { mutableIntStateOf(state.preferences.startSurah) }
    var startAyahText by rememberSaveable { mutableStateOf(state.preferences.startAyah.toString()) }
    var endAyahText by rememberSaveable { mutableStateOf(state.preferences.endAyah.toString()) }
    var pageInputText by rememberSaveable { mutableStateOf((state.playerPassage?.pageNumber ?: 1).toString()) }
    val selected = state.surahs.firstOrNull { it.id == selectedSurah }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Choisir un passage & Set", style = MaterialTheme.typography.headlineMedium)

        // Mode switch tabs: Par Sourate vs Par Page (1 à 604)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pickerMode == 0,
                onClick = { pickerMode = 0 },
                label = { Text("Par Sourate & Set") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = pickerMode == 1,
                onClick = { pickerMode = 1 },
                label = { Text("Par Page (1–604)") },
                modifier = Modifier.weight(1f)
            )
        }

        if (pickerMode == 0) {
            // Mode Par Sourate & Set de versets
            if (state.surahs.isEmpty()) {
                EmptyStateCard(
                    title = "Corpus en cours de chargement",
                    message = "L'inventaire des sourates se charge automatiquement."
                )
            } else {
                Text(
                    "Sourate : ${selected?.nameTransliterated ?: selectedSurah} (${selected?.nameArabic.orEmpty()})",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.surahs, key = { it.id }) { surah ->
                        Card(
                            onClick = {
                                selectedSurah = surah.id
                                startAyahText = "1"
                                endAyahText = minOf(7, surah.ayahCount).toString()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${surah.id}. ${surah.nameTransliterated}", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        surah.nameArabic,
                                        style = com.hifzsniper.app.presentation.theme.QuranTypography.surahBannerTitle.copy(fontSize = 18.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text("${surah.ayahCount} versets", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            val maxAyah = selected?.ayahCount ?: 286

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📦 Définir un Set de versets :", style = MaterialTheme.typography.titleSmall)

                    // Quick presets
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "1 v." to 1,
                            "3 v." to 3,
                            "5 v." to 5,
                            "7 v." to 7,
                            "10 v." to 10
                        ).forEach { (lbl, count) ->
                            val startNum = startAyahText.toIntOrNull() ?: 1
                            FilterChip(
                                selected = (endAyahText.toIntOrNull() ?: 1) == minOf(startNum + count - 1, maxAyah),
                                onClick = {
                                    endAyahText = minOf(startNum + count - 1, maxAyah).toString()
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startAyahText,
                            onValueChange = { startAyahText = it.filter(Char::isDigit) },
                            label = { Text("Verset début") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endAyahText,
                            onValueChange = { endAyahText = it.filter(Char::isDigit) },
                            label = { Text("Verset fin (max $maxAyah)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val startVal = startAyahText.toIntOrNull()?.takeIf { it in 1..maxAyah }
                    val rawEnd = endAyahText.toIntOrNull()?.takeIf { it in 1..maxAyah } ?: startVal
                    val endVal = if (startVal != null && rawEnd != null) maxOf(startVal, rawEnd) else null
                    val count = if (startVal != null && endVal != null) endVal - startVal + 1 else 1

                    Text(
                        "Set : versets ${startVal ?: 1} à ${endVal ?: 1} ($count verset(s))",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Button(
                        onClick = {
                            if (startVal != null && endVal != null) {
                                onSelectSet(selectedSurah, startVal, endVal)
                                onSelected()
                            }
                        },
                        enabled = selectedSurah in 1..114 && startVal != null && endVal != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ouvrir le Set (${startVal ?: 1}–${endVal ?: 1}) 📖")
                    }
                }
            }
        } else {
            // Mode Par Page (1 à 604)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Accès direct par page du Mushaf", style = MaterialTheme.typography.titleMedium)
                    Text("Le Coran de Médine comporte 604 pages.", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pageInputText,
                            onValueChange = { pageInputText = it.filter(Char::isDigit) },
                            label = { Text("Page (1–604)") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val pageNum = pageInputText.toIntOrNull()?.coerceIn(1, 604)
                                if (pageNum != null) {
                                    onSelectPage(pageNum)
                                    onSelected()
                                }
                            },
                            enabled = pageInputText.toIntOrNull() in 1..604,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Aller")
                        }
                    }
                }
            }

            Text("Liste des pages du Mushaf :", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items((1..604).toList(), key = { it }) { pageNum ->
                    val juzNum = when {
                        pageNum <= 21 -> 1
                        pageNum <= 41 -> 2
                        pageNum <= 61 -> 3
                        pageNum <= 81 -> 4
                        pageNum <= 101 -> 5
                        pageNum <= 121 -> 6
                        pageNum <= 141 -> 7
                        pageNum <= 161 -> 8
                        pageNum <= 181 -> 9
                        pageNum <= 201 -> 10
                        pageNum <= 221 -> 11
                        pageNum <= 241 -> 12
                        pageNum <= 261 -> 13
                        pageNum <= 281 -> 14
                        pageNum <= 301 -> 15
                        pageNum <= 321 -> 16
                        pageNum <= 341 -> 17
                        pageNum <= 361 -> 18
                        pageNum <= 381 -> 19
                        pageNum <= 401 -> 20
                        pageNum <= 421 -> 21
                        pageNum <= 441 -> 22
                        pageNum <= 461 -> 23
                        pageNum <= 481 -> 24
                        pageNum <= 501 -> 25
                        pageNum <= 521 -> 26
                        pageNum <= 541 -> 27
                        pageNum <= 561 -> 28
                        pageNum <= 581 -> 29
                        else -> 30
                    }
                    Card(
                        onClick = {
                            onSelectPage(pageNum)
                            onSelected()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Page $pageNum", style = MaterialTheme.typography.titleMedium)
                            Text("Juz $juzNum", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
