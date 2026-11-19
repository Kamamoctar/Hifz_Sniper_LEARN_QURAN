package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hifzsniper.app.presentation.AppUiState

@Composable
fun DownloadsScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onStart: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDownloadCurrentPage: () -> Unit = {},
    onOpenSources: () -> Unit
) = PageColumn(modifier) {
    Text("Téléchargements", style = MaterialTheme.typography.headlineMedium)
    Text("Audio Al-Afasy récupéré à la demande et activé uniquement après vérification SHA-256.", style = MaterialTheme.typography.bodySmall)

    val completedCount = state.downloadedAudioCount
    val downloadingCount = state.activeDownloadCount
    val occupiedBytes = state.downloadedAudioBytes

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("État du cache hors-ligne", style = MaterialTheme.typography.titleMedium)
            Text(
                "$completedCount pistes téléchargées · $downloadingCount en cours · ${formatStorageSize(occupiedBytes)} utilisés",
                style = MaterialTheme.typography.bodySmall
            )
            state.playerPassage?.pageNumber?.let { page ->
                OutlinedButton(
                    onClick = onDownloadCurrentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📥 Télécharger tous les audios de la Page $page")
                }
            }
        }
    }
    if (state.downloads.isEmpty()) {
        EmptyStateCard(
            title = "Aucune piste validée",
            message = "Ouvre un passage dans le lecteur pour proposer sa piste audio.",
            actionLabel = "Voir les sources",
            onAction = onOpenSources
        )
    } else {
        state.downloads.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.resourceKey, style = MaterialTheme.typography.titleMedium)
                    Text(item.status)
                    val total = item.totalBytes
                    if (total != null && total > 0) {
                        LinearProgressIndicator(
                            progress = { (item.bytesDownloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${item.bytesDownloaded} / $total octets", style = MaterialTheme.typography.bodySmall)
                    }
                    item.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    when (item.status) {
                        "NOT_DOWNLOADED", "FAILED", "CANCELLED" -> OutlinedButton(onClick = { onStart(item.id) }) { Text("Télécharger") }
                        "QUEUED", "DOWNLOADING" -> OutlinedButton(onClick = { onCancel(item.id) }) { Text("Annuler") }
                        "COMPLETED" -> OutlinedButton(onClick = { onDelete(item.id) }) { Text("Supprimer du stockage") }
                    }
                }
            }
        }
    }
}

private fun formatStorageSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.2f Go".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f Mo".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f Ko".format(bytes / 1024.0)
    else -> "$bytes octets"
}
