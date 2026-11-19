package com.hifzsniper.app.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.data.BackupSummary
import com.hifzsniper.app.presentation.AppUiState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onMinutes: (Int) -> Unit,
    onLevel: (String) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onChoosePassage: () -> Unit,
    onTransliteration: (Boolean) -> Unit,
    onTranslation: (Boolean) -> Unit,
    onTafsir: (Boolean) -> Unit,
    onRestartOnboarding: () -> Unit = {},
    onDownloads: () -> Unit,
    onSources: () -> Unit,
    onWeakPoints: () -> Unit,
    onExportBackup: suspend () -> String = { "" },
    onInspectBackup: suspend (String) -> BackupSummary = { BackupSummary(false) },
    onRestoreBackup: suspend (BackupSummary) -> Result<String> = { Result.success("") }
) = PageColumn(modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = remember(context) { context.getSystemService(ClipboardManager::class.java) }

    var exportedJson by remember { mutableStateOf<String?>(null) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var inspectedSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var feedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Text("Réglages", style = MaterialTheme.typography.headlineMedium)

    Text("Temps quotidien", style = MaterialTheme.typography.titleMedium)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(5, 10, 15, 20, 30).forEach { value ->
            FilterChip(
                selected = state.preferences.dailyMinutes == value,
                onClick = { onMinutes(value) },
                label = { Text("$value min") }
            )
        }
    }

    Text("Niveau", style = MaterialTheme.typography.titleMedium)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("Débutant", "Intermédiaire", "Avancé").forEach { value ->
            FilterChip(
                selected = state.preferences.level == value,
                onClick = { onLevel(value) },
                label = { Text(value) }
            )
        }
    }

    Text("Objectif quotidien", style = MaterialTheme.typography.titleMedium)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(1, 2, 3, 5, 10).forEach { value ->
            FilterChip(
                selected = state.preferences.dailyAyahGoal == value,
                onClick = { onDailyGoal(value) },
                label = { Text("$value verset${if (value > 1) "s" else ""}") }
            )
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Passage actif", style = MaterialTheme.typography.titleSmall)
            Text("${state.preferences.startSurah}:${state.preferences.startAyah} à ${state.preferences.startSurah}:${state.preferences.endAyah}")
            OutlinedButton(onClick = onChoosePassage, modifier = Modifier.fillMaxWidth()) {
                Text("Modifier le passage")
            }
        }
    }

    Text("Aides indépendantes", style = MaterialTheme.typography.titleMedium)
    SettingRow("Translittération", state.preferences.showTransliteration, onTransliteration)
    SettingRow("Traduction française", state.preferences.showTranslation, onTranslation)
    SettingRow("Notes explicatives", state.preferences.showTafsir, onTafsir)

    Text("Contenu & Révisions", style = MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick = onDownloads, modifier = Modifier.fillMaxWidth()) {
        Text("Téléchargements (${state.downloads.size})")
    }
    OutlinedButton(onClick = onWeakPoints, modifier = Modifier.fillMaxWidth()) {
        Text("Points faibles (${state.weakPoints.size})")
    }
    OutlinedButton(onClick = onSources, modifier = Modifier.fillMaxWidth()) {
        Text("Sources et attributions")
    }

    Text("Guide d'accueil & Méthode", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Revoir la méthode Hifz-Sniper", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(
                "Relisez la présentation des 4 piliers de mémorisation ou reconfigurez votre rythme, votre niveau et votre sourate de départ.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onRestartOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text("🔄 Relancer le guide d'accueil (Onboarding)")
            }
        }
    }

    Text("Sauvegarde de la progression (Section 15)", style = MaterialTheme.typography.titleMedium)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Export & Restauration JSON", style = MaterialTheme.typography.titleSmall)
            Text(
                "Exporte tes cartes SRS, ton historique de révision et tes points faibles au format JSON documenté (sans audio) pour les conserver en lieu sûr.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val json = onExportBackup()
                            exportedJson = json
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Sauvegarde Hifz-Sniper", json))
                            feedbackMessage = "Progression exportée et copiée dans le presse-papier (${json.length} caractères)."
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Exporter JSON")
                }

                OutlinedButton(
                    onClick = {
                        showImportDialog = true
                        importInputText = ""
                        inspectedSummary = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restaurer JSON")
                }
            }

            feedbackMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (exportedJson != null) {
        AlertDialog(
            onDismissRequest = { exportedJson = null },
            title = { Text("Export de progression") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("La sauvegarde JSON a été copiée dans le presse-papier.")
                    Text("Nombre de caractères : ${exportedJson?.length}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { exportedJson = null }) { Text("Fermer") }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restaurer une progression") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Colle le contenu JSON de ta sauvegarde précédente :", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            inspectedSummary = null
                        },
                        placeholder = { Text("{\n  \"version\": 1,\n  ...") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    inspectedSummary?.let { summary ->
                        if (summary.isValid) {
                            Text("Résumé de la sauvegarde :", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text("• Cartes : ${summary.cardsCount}\n• Logs de révision : ${summary.logsCount}\n• Séances : ${summary.sessionsCount}\n• Points faibles : ${summary.weakPointsCount}\n• Date d'export : ${summary.exportedAt}")
                        } else {
                            Text(summary.errorMessage ?: "Format JSON invalide", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                val summary = inspectedSummary
                if (summary != null && summary.isValid) {
                    Button(onClick = {
                        scope.launch {
                            val result = onRestoreBackup(summary)
                            showImportDialog = false
                            feedbackMessage = result.getOrElse { it.message ?: "Échec de la restauration" }
                        }
                    }) {
                        Text("Confirmer la restauration")
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                inspectedSummary = onInspectBackup(importInputText)
                            }
                        },
                        enabled = importInputText.isNotBlank()
                    ) {
                        Text("Vérifier le fichier")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Annuler") }
            }
        )
    }

    Text("Récitateur unique : Mishary Rashid Al-Afasy", style = MaterialTheme.typography.bodySmall)
}
