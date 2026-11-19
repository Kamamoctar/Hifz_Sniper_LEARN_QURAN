package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.domain.Rating
import com.hifzsniper.app.domain.StudyCard

@Composable
fun PageColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
fun EmptyStateCard(title: String, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(message)
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun ActiveRecallCard(
    card: StudyCard,
    onRate: (String, Rating) -> Unit,
    showTransliteration: Boolean = false
) {
    var revealed by rememberSaveable(card.id) { mutableStateOf(false) }
    val isArabic = card.answer.any { it in '\u0600'..'\u06FF' }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(card.prompt, style = MaterialTheme.typography.titleMedium)
            if (revealed) {
                if (isArabic) {
                    Text(
                        card.answer,
                        style = com.hifzsniper.app.presentation.theme.QuranTypography.verseText,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showTransliteration) {
                        val translit = com.hifzsniper.app.domain.ArabicTransliterationEngine.transliterate(card.answer)
                        if (translit.isNotBlank()) {
                            Text(
                                text = "Phonétique : $translit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Text(card.answer)
                }
            } else {
                Text("Récite sans aide, puis révèle la réponse.", textAlign = TextAlign.Center)
            }
            OutlinedButton(onClick = { revealed = !revealed }, modifier = Modifier.fillMaxWidth()) {
                Text(if (revealed) "Masquer" else "Révéler")
            }
            if (revealed) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { onRate(card.id, Rating.FAILED) }, modifier = Modifier.weight(1f)) { Text("Raté") }
                    Button(onClick = { onRate(card.id, Rating.HESITANT) }, modifier = Modifier.weight(1f)) { Text("Hésitant") }
                    Button(onClick = { onRate(card.id, Rating.FLUENT) }, modifier = Modifier.weight(1f)) { Text("Fluide") }
                }
            }
        }
    }
}

@Composable
fun SettingRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
