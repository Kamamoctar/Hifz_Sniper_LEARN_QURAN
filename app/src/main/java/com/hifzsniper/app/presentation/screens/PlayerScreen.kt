package com.hifzsniper.app.presentation.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hifzsniper.app.domain.PlaybackLoopEngine
import com.hifzsniper.app.domain.PlayerPassage
import com.hifzsniper.app.presentation.AppUiState
import com.hifzsniper.app.presentation.ContentInstallPhase
import com.hifzsniper.app.presentation.theme.QuranTypography
import com.hifzsniper.app.presentation.theme.ayahEndSign
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PlayerScreen(
    state: AppUiState,
    modifier: Modifier = Modifier,
    onChoosePassage: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onGoToPage: (Int) -> Unit = {},
    onSelectAyah: (Int, Int) -> Unit = { _, _ -> },
    onSelectVerseSet: (Int, Int, Int) -> Unit = { _, _, _ -> },
    onDownloadAudio: () -> Unit,
    onDownloadPageAudio: (Int) -> Unit = {},
    onDownloadSurahAudio: (Int) -> Unit = {},
    onToggleTransliteration: (Boolean) -> Unit = {},
    onToggleTranslation: (Boolean) -> Unit = {},
    onToggleTafsir: (Boolean) -> Unit = {},
    onOpenDownloads: () -> Unit,
    onOpenMutashabihat: () -> Unit = {},
    onFlagWeakRange: (String, Int?, Int?, String) -> Unit = { _, _, _, _ -> }
) = PageColumn(modifier) {
    val passage = state.playerPassage
    var showPageDialog by rememberSaveable { mutableStateOf(false) }
    var pageInputText by rememberSaveable { mutableStateOf("") }
    var viewMode by rememberSaveable { mutableIntStateOf(1) } // 1 = page Mushaf guidée, 0 = verset et écoute

    // Synchronization state & Set Playback state
    var playbackPositionMs by remember(passage?.verseKey) { mutableLongStateOf(0L) }
    var isAudioPlaying by rememberSaveable(passage?.verseKey) { mutableStateOf(false) }
    var keepPlayingNextVerse by rememberSaveable { mutableStateOf(false) }
    var verseRepeatTarget by rememberSaveable { mutableIntStateOf(1) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }
    var setRepeatTarget by rememberSaveable { mutableIntStateOf(1) } // 1x, 3x, 5x, 10x, -1 (infinie)
    var currentSetCycle by rememberSaveable { mutableIntStateOf(1) }
    var playbackRestartToken by rememberSaveable { mutableIntStateOf(0) }

    // Set dialog
    var showSetDialog by rememberSaveable { mutableStateOf(false) }
    var setStartText by rememberSaveable { mutableStateOf("") }
    var setEndText by rememberSaveable { mutableStateOf("") }

    Text("Mushaf & Lecteur", style = MaterialTheme.typography.headlineMedium)

    if (state.contentInstall.phase == ContentInstallPhase.IMPORTING) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chargement du Coran en cours…", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Les 114 sourates, 604 pages et les pistes audio sont en cours d'initialisation.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (passage == null) {
        EmptyStateCard(
            title = "Passage indisponible",
            message = if (state.contentInstall.phase == ContentInstallPhase.IMPORTING)
                "Import en cours… les versets et l'audio arrivent dans quelques instants."
            else
                "Le corpus est en attente d'initialisation.",
            actionLabel = "Choisir un passage",
            onAction = onChoosePassage
        )
        return@PageColumn
    }

    val setStartAyah = state.preferences.startAyah
    val setEndAyah = maxOf(setStartAyah, state.preferences.endAyah)
    val totalInSet = setEndAyah - setStartAyah + 1
    val currentSurahMeta = state.surahs.firstOrNull { it.id == passage.surahId }
    val maxAyahInSurah = currentSurahMeta?.ayahCount ?: 286

    // Carte Set de versets sélectionné
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "📦 Set en cours : Versets $setStartAyah à $setEndAyah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Sourate ${passage.surahNameTransliterated} (${passage.surahNameArabic}) · $totalInSet verset(s)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = {
                        setStartText = setStartAyah.toString()
                        setEndText = setEndAyah.toString()
                        showSetDialog = true
                    }
                ) {
                    Text("Changer le Set ✎")
                }
            }

            // Boutons de boucle du Set
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Boucle du Set :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                listOf(
                    1 to "1×",
                    3 to "3×",
                    5 to "5×",
                    10 to "10×",
                    -1 to "∞ Boucle"
                ).forEach { (cnt, lbl) ->
                    FilterChip(
                        selected = setRepeatTarget == cnt,
                        onClick = {
                            setRepeatTarget = cnt
                            currentSetCycle = 1
                            autoAdvance = true
                            if (verseRepeatTarget == PlaybackLoopEngine.INFINITE) {
                                verseRepeatTarget = 1
                            }
                            playbackRestartToken++
                            if (passage.ayahNumber != setStartAyah) {
                                onSelectAyah(passage.surahId, setStartAyah)
                            }
                        },
                        label = { Text(lbl) }
                    )
                }
            }

            if (verseRepeatTarget == PlaybackLoopEngine.INFINITE) {
                Text(
                    "La répétition infinie du verset met l’enchaînement du Set en pause.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Progression au sein du Set
            if (totalInSet > 1) {
                val progressInSet = ((passage.ayahNumber - setStartAyah + 1).toFloat() / totalInSet.toFloat()).coerceIn(0f, 1f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Récitation : verset ${passage.ayahNumber} (verset ${passage.ayahNumber - setStartAyah + 1}/$totalInSet)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (setRepeatTarget != 1) {
                        Text(
                            if (setRepeatTarget == -1) "Cycle $currentSetCycle (∞)" else "Cycle $currentSetCycle / $setRepeatTarget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                LinearProgressIndicator(progress = { progressInSet }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // View Mode Switcher: page Mushaf guidée vs verset et audio.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = viewMode == 1,
            onClick = { viewMode = 1 },
            label = { Text("📖 Mushaf par page") },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = viewMode == 0,
            onClick = { viewMode = 0 },
            label = { Text("🎧 Verset & Écoute") },
            modifier = Modifier.weight(1f)
        )
    }

    if (viewMode == 1) {
        // Mode page Mushaf guidée. Les métadonnées de page/juz viennent du corpus.
        MushafPageView(
            pageNumber = passage.pageNumber ?: 1,
            ayahs = state.pageAyahs,
            surahs = state.surahs,
            selectedVerseKey = passage.verseKey,
            startAyahInSet = setStartAyah,
            endAyahInSet = setEndAyah,
            currentSetSurahId = passage.surahId,
            isPlaying = isAudioPlaying,
            onSelectAyah = onSelectAyah,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            onOpenPageDialog = {
                pageInputText = (passage.pageNumber ?: 1).toString()
                showPageDialog = true
            },
            onDownloadPageAudio = { onDownloadPageAudio(passage.pageNumber ?: 1) }
        )

        // Aides rapides en mode Mushaf
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Aides (${passage.verseKey}) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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

        if (state.preferences.showTransliteration) {
            val translit = passage.effectiveTransliteration
            if (translit.isNotBlank()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Translittération · Verset ${passage.ayahNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = translit,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        if (state.preferences.showTranslation && passage.translationFrench != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Traduction française · Verset ${passage.ayahNumber}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(passage.translationFrench, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    } else {
        // Mode Verset unique & Écoute détaillée
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (passage.surahNameTransliterated.isNotBlank())
                            "${passage.surahId}. ${passage.surahNameTransliterated}"
                        else
                            "Sourate ${passage.surahId}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (passage.surahNameArabic.isNotBlank()) {
                        Text(passage.surahNameArabic, style = QuranTypography.surahBannerTitle.copy(fontSize = 18.sp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Verset ${passage.ayahNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Page ${passage.pageNumber ?: 1} / 604 · Juz ${passage.juzNumber ?: 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Navigation buttons: Previous/Next verse AND Previous/Next page
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f)) { Text("◀ Verset préc.") }
                OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Verset suiv. ▶") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreviousPage, modifier = Modifier.weight(1f)) { Text("◀ Page préc.") }
                OutlinedButton(onClick = { showPageDialog = true; pageInputText = (passage.pageNumber ?: 1).toString() }, modifier = Modifier.weight(1f)) {
                    Text("Page ${passage.pageNumber ?: 1} ✎")
                }
                OutlinedButton(onClick = onNextPage, modifier = Modifier.weight(1f)) { Text("Page suiv. ▶") }
            }
        }

        // Quran Arabic text card with Amiri Quran font & isolated word-by-word highlight
        VerseWordHighlightCard(
            passage = passage,
            isAudioPlaying = isAudioPlaying,
            positionProvider = { playbackPositionMs }
        )

        // Quick Aides Toggle Chips (Phonétique, Traduction, Notes)
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

        // 1. Translittération phonétique
        if (state.preferences.showTransliteration) {
            val translit = passage.effectiveTransliteration
            if (translit.isNotBlank()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Translittération phonétique",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Aide à la prononciation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = translit,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }

        // 2. Translations & footnotes
        if (state.preferences.showTranslation) {
            passage.translationFrench?.let {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Traduction française", style = MaterialTheme.typography.titleMedium)
                        Text(it)
                        Text("Rachid Maach · QuranEnc 1.0.3", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (state.preferences.showTafsir) {
            passage.explanatoryNotesFrench?.let {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Notes explicatives", style = MaterialTheme.typography.titleMedium)
                        Text(it)
                        Text("Notes de la traduction · QuranEnc 1.0.3", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Mutashabihat warning banner if twin verses exist
        if (state.mutashabihat.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚠️ ${state.mutashabihat.size} verset(s) jumeau(x) détecté(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        OutlinedButton(onClick = onOpenMutashabihat) {
                            Text("Comparer")
                        }
                    }
                    Text(
                        "Attention à la confusion avec ${state.mutashabihat.first().targetVerseKey} (${state.mutashabihat.first().targetSurahNameTransliterated}) : ${state.mutashabihat.first().divergenceDescription}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        var weakMarked by rememberSaveable(passage.verseKey) { mutableStateOf(false) }
        var showWeakDialog by rememberSaveable(passage.verseKey) { mutableStateOf(false) }
        var weakStartText by rememberSaveable(passage.verseKey) { mutableStateOf("") }
        var weakEndText by rememberSaveable(passage.verseKey) { mutableStateOf("") }

        // Actions: Offline audio & Point faible & Téléchargements groupés
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (passage.downloadStatus) {
                "COMPLETED" -> OutlinedButton(onClick = onOpenDownloads, modifier = Modifier.weight(1f)) {
                    Text("✓ Audio hors-ligne")
                }
                "QUEUED", "DOWNLOADING" -> OutlinedButton(onClick = onOpenDownloads, modifier = Modifier.weight(1f)) {
                    Text(if (passage.downloadStatus == "QUEUED") "En file d'attente…" else "Téléchargement…")
                }
                else -> OutlinedButton(onClick = onDownloadAudio, modifier = Modifier.weight(1f)) {
                    Text("Télécharger verset")
                }
            }

            OutlinedButton(
                onClick = {
                    val currentPos = playbackPositionMs
                    val currentActiveWord = passage.words.firstOrNull { word ->
                        word.startMs != null && word.endMs != null && currentPos in word.startMs until word.endMs
                    }
                    val activePosition = currentActiveWord?.position
                    weakStartText = activePosition?.toString().orEmpty()
                    weakEndText = activePosition?.toString().orEmpty()
                    showWeakDialog = true
                },
                enabled = !weakMarked,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (weakMarked) "✓ Point signalé" else "Isoler une difficulté")
            }
        }

        if (showWeakDialog) {
            val start = weakStartText.toIntOrNull()
            val end = weakEndText.toIntOrNull()
            val wholeVerse = weakStartText.isBlank() && weakEndText.isBlank()
            val validRange = wholeVerse || (start != null && end != null && start in 1..passage.words.size && end in start..passage.words.size)
            AlertDialog(
                onDismissRequest = { showWeakDialog = false },
                title = { Text("Isoler le point faible") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Laissez les deux champs vides pour signaler le verset entier, ou indiquez une plage de mots.")
                        OutlinedTextField(
                            value = weakStartText,
                            onValueChange = { weakStartText = it.filter(Char::isDigit) },
                            label = { Text("Premier mot (1–${passage.words.size})") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weakEndText,
                            onValueChange = { weakEndText = it.filter(Char::isDigit) },
                            label = { Text("Dernier mot (1–${passage.words.size})") },
                            singleLine = true
                        )
                        if (!validRange) {
                            Text("Plage invalide.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val reason = if (wholeVerse) {
                                "Verset complet signalé depuis le lecteur"
                            } else {
                                "Difficulté sur les mots $start à $end"
                            }
                            onFlagWeakRange(passage.verseKey, start, end, reason)
                            weakMarked = true
                            showWeakDialog = false
                        },
                        enabled = validRange
                    ) { Text("Enregistrer") }
                },
                dismissButton = {
                    TextButton(onClick = { showWeakDialog = false }) { Text("Annuler") }
                }
            )
        }

        // Batch download buttons for Page & Surah
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onDownloadPageAudio(passage.pageNumber ?: 1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("📥 Télécharger Page ${passage.pageNumber ?: 1}")
            }
            OutlinedButton(
                onClick = { onDownloadSurahAudio(passage.surahId) },
                modifier = Modifier.weight(1f)
            ) {
                Text("📥 Télécharger Sourate")
            }
        }
    }

    // Audio Player Bar (available in both view modes!)
    passage.playableUris.takeIf(List<String>::isNotEmpty)?.let { sources ->
        NativeComposeAudioPlayer(
            sources = sources,
            playbackCycleToken = playbackRestartToken,
            autoPlay = keepPlayingNextVerse,
            repeatTarget = verseRepeatTarget,
            autoAdvance = autoAdvance,
            onPosition = { pos -> playbackPositionMs = pos },
            onPlayStateChanged = { playing ->
                isAudioPlaying = playing
            },
            onPlaybackIntentChanged = { shouldContinue -> keepPlayingNextVerse = shouldContinue },
            onRepeatTargetChange = { target ->
                verseRepeatTarget = target
                if (target == PlaybackLoopEngine.INFINITE) {
                    setRepeatTarget = 1
                    currentSetCycle = 1
                    autoAdvance = false
                }
            },
            onAutoAdvanceChange = { enabled ->
                autoAdvance = enabled
                if (!enabled) {
                    setRepeatTarget = 1
                    currentSetCycle = 1
                } else if (verseRepeatTarget == PlaybackLoopEngine.INFINITE) {
                    verseRepeatTarget = 1
                }
            },
            onAutoAdvance = {
                val currentAyah = passage.ayahNumber
                val startAyah = state.preferences.startAyah
                val endAyah = maxOf(startAyah, state.preferences.endAyah)
                val surahId = passage.surahId

                when (val decision = PlaybackLoopEngine.afterVerseCompleted(
                    currentAyah = currentAyah,
                    startAyah = startAyah,
                    endAyah = endAyah,
                    currentSetCycle = currentSetCycle,
                    setRepeatTarget = setRepeatTarget
                )) {
                    is PlaybackLoopEngine.SetDecision.Continue -> {
                        currentSetCycle = decision.nextSetCycle
                        if (decision.nextAyah == currentAyah) playbackRestartToken++
                        onSelectAyah(surahId, decision.nextAyah)
                    }
                    PlaybackLoopEngine.SetDecision.Completed -> {
                        currentSetCycle = 1
                        keepPlayingNextVerse = false
                        if (currentAyah == startAyah) {
                            playbackRestartToken++
                        } else {
                            onSelectAyah(surahId, startAyah)
                        }
                    }
                }
            }
        )
    } ?: EmptyStateCard("Audio indisponible", "Aucune piste Al-Afasy n’est associée à ce verset.")

    // Set dialog
    if (showSetDialog) {
        AlertDialog(
            onDismissRequest = { showSetDialog = false },
            title = { Text("Sélectionner un Set de versets") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Sourate ${passage.surahNameTransliterated} (${passage.surahNameArabic}) - $maxAyahInSurah versets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Choisissez la plage de versets à mémoriser :", style = MaterialTheme.typography.bodySmall)

                    // Presets rapides
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "1–3" to (1 to minOf(3, maxAyahInSurah)),
                            "1–5" to (1 to minOf(5, maxAyahInSurah)),
                            "1–7" to (1 to minOf(7, maxAyahInSurah)),
                            "1–10" to (1 to minOf(10, maxAyahInSurah)),
                            "Toute" to (1 to maxAyahInSurah)
                        ).forEach { (lbl, range) ->
                            FilterChip(
                                selected = setStartText == range.first.toString() && setEndText == range.second.toString(),
                                onClick = {
                                    setStartText = range.first.toString()
                                    setEndText = range.second.toString()
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = setStartText,
                            onValueChange = { setStartText = it.filter(Char::isDigit) },
                            label = { Text("Verset début") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = setEndText,
                            onValueChange = { setEndText = it.filter(Char::isDigit) },
                            label = { Text("Verset fin (max $maxAyahInSurah)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val startVal = setStartText.toIntOrNull()?.takeIf { it in 1..maxAyahInSurah }
                    val rawEnd = setEndText.toIntOrNull()?.takeIf { it in 1..maxAyahInSurah } ?: startVal
                    val endVal = if (startVal != null && rawEnd != null) maxOf(startVal, rawEnd) else null
                    val count = if (startVal != null && endVal != null) endVal - startVal + 1 else 1

                    Text(
                        "Set : versets ${startVal ?: 1} à ${endVal ?: 1} ($count verset(s))",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                val startVal = setStartText.toIntOrNull()?.takeIf { it in 1..maxAyahInSurah }
                val rawEnd = setEndText.toIntOrNull()?.takeIf { it in 1..maxAyahInSurah } ?: startVal
                val endVal = if (startVal != null && rawEnd != null) maxOf(startVal, rawEnd) else null
                Button(
                    onClick = {
                        if (startVal != null && endVal != null) {
                            onSelectVerseSet(passage.surahId, startVal, endVal)
                            currentSetCycle = 1
                            playbackRestartToken++
                            showSetDialog = false
                        }
                    },
                    enabled = startVal != null && endVal != null
                ) {
                    Text("Appliquer le Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetDialog = false }) { Text("Annuler") }
            }
        )
    }

    // Jump to page dialog
    if (showPageDialog) {
        AlertDialog(
            onDismissRequest = { showPageDialog = false },
            title = { Text("Aller à la page du Mushaf") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Entrez un numéro de page entre 1 et 604 :")
                    OutlinedTextField(
                        value = pageInputText,
                        onValueChange = { pageInputText = it.filter(Char::isDigit) },
                        label = { Text("Page (1–604)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = pageInputText.toIntOrNull()?.coerceIn(1, 604)
                        if (target != null) {
                            onGoToPage(target)
                            showPageDialog = false
                        }
                    },
                    enabled = pageInputText.toIntOrNull() in 1..604
                ) {
                    Text("Aller")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPageDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
internal fun NativeComposeAudioPlayer(
    sources: List<String>,
    playbackCycleToken: Int = 1,
    autoPlay: Boolean = false,
    repeatTarget: Int,
    autoAdvance: Boolean,
    onPosition: (Long) -> Unit,
    onAutoAdvance: () -> Unit = {},
    onPlayStateChanged: (Boolean) -> Unit = {},
    onPlaybackIntentChanged: (Boolean) -> Unit = {},
    onRepeatTargetChange: (Int) -> Unit = {},
    onAutoAdvanceChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val availableSources = sources.filter(String::isNotBlank).distinct()
    require(availableSources.isNotEmpty()) { "Au moins une source audio est requise" }
    val playerKey = "${availableSources.joinToString("|")}-$playbackCycleToken"
    var isPlaying by remember(playerKey) { mutableStateOf(autoPlay) }
    var currentPositionMs by remember(playerKey) { mutableLongStateOf(0L) }
    var durationMs by remember(playerKey) { mutableLongStateOf(0L) }
    var speed by rememberSaveable { mutableFloatStateOf(1f) }
    var repeatDelayMs by rememberSaveable { mutableIntStateOf(700) }

    var currentRepeatCount by remember(playerKey) { mutableIntStateOf(1) }

    var isBuffering by remember(playerKey) { mutableStateOf(false) }
    var errorMsg by remember(playerKey) { mutableStateOf<String?>(null) }
    var fallbackMsg by remember(playerKey) { mutableStateOf<String?>(null) }
    var sourceIndex by remember(playerKey) { mutableIntStateOf(0) }
    val repeatHandler = remember(playerKey) { Handler(Looper.getMainLooper()) }

    val player = remember(playerKey) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(availableSources.first().asAudioUri()))
            playWhenReady = autoPlay
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    onPlayStateChanged(playing)
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = (playbackState == Player.STATE_BUFFERING)
                    if (playbackState == Player.STATE_READY && duration > 0) durationMs = duration
                    if (playbackState == Player.STATE_ENDED) {
                        when (val decision = PlaybackLoopEngine.afterTrackEnded(
                            currentRepeatCount = currentRepeatCount,
                            repeatTarget = repeatTarget
                        )) {
                            is PlaybackLoopEngine.TrackDecision.Repeat -> {
                                currentRepeatCount = decision.nextRepeatCount
                                pause()
                                repeatHandler.postDelayed({
                                    seekTo(0)
                                    play()
                                }, repeatDelayMs.toLong())
                            }
                            PlaybackLoopEngine.TrackDecision.VerseCompleted -> {
                                currentRepeatCount = 1
                                if (autoAdvance) {
                                    onAutoAdvance()
                                } else {
                                    pause()
                                    onPlaybackIntentChanged(false)
                                    onPlayStateChanged(false)
                                }
                            }
                        }
                    }
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val nextIndex = sourceIndex + 1
                    if (nextIndex < availableSources.size) {
                        val shouldResume = playWhenReady || isPlaying
                        sourceIndex = nextIndex
                        durationMs = 0L
                        currentPositionMs = 0L
                        errorMsg = null
                        fallbackMsg = "Source audio indisponible · bascule automatique sur le miroir ${nextIndex + 1}/${availableSources.size}."
                        setMediaItem(MediaItem.fromUri(availableSources[nextIndex].asAudioUri()))
                        prepare()
                        playWhenReady = shouldResume
                    } else {
                        errorMsg = "Erreur audio après ${availableSources.size} source(s) : ${error.localizedMessage ?: "vérifiez votre connexion"}"
                        onPlaybackIntentChanged(false)
                    }
                }
            })
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose {
            repeatHandler.removeCallbacksAndMessages(null)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            val pos = player.currentPosition.coerceAtLeast(0L)
            currentPositionMs = pos
            if (player.duration > 0) durationMs = player.duration
            onPosition(pos)
            delay(200)
        }
    }

    LaunchedEffect(speed) { player.playbackParameters = PlaybackParameters(speed) }
    LaunchedEffect(repeatTarget) { currentRepeatCount = 1 }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Récitateur : Mishary Rashid Al-Afasy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (repeatTarget == -1) {
                        Text("Répétition : $currentRepeatCount (∞)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    } else if (repeatTarget > 1) {
                        Text("Répétition : $currentRepeatCount sur $repeatTarget", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                if (isBuffering) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isPlaying) "En écoute ●" else "En pause", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Seek slider
            val sliderValue = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
            Slider(
                value = sliderValue,
                onValueChange = { fraction ->
                    val targetMs = (fraction * durationMs).toLong()
                    player.seekTo(targetMs)
                    currentPositionMs = targetMs
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Timestamps
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(currentPositionMs), style = MaterialTheme.typography.bodySmall)
                Text(formatDuration(durationMs), style = MaterialTheme.typography.bodySmall)
            }

            // Central control buttons: Seek -3s, Big Play/Pause, Seek +3s
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { player.seekTo((player.currentPosition - 3000L).coerceAtLeast(0L)) }) {
                    Text("⏪ 3s")
                }

                Button(
                    onClick = {
                        if (isPlaying) {
                            player.pause()
                            onPlaybackIntentChanged(false)
                        } else {
                            errorMsg = null
                            if (player.playerError != null) {
                                sourceIndex = 0
                                fallbackMsg = null
                                player.setMediaItem(MediaItem.fromUri(availableSources.first().asAudioUri()))
                                player.prepare()
                            }
                            if (player.playbackState == Player.STATE_ENDED) {
                                player.seekTo(0)
                                currentRepeatCount = 1
                            }
                            onPlaybackIntentChanged(true)
                            player.play()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(if (isPlaying) "⏸ Pause" else "▶ Lecture", style = MaterialTheme.typography.titleMedium)
                }

                OutlinedButton(onClick = { player.seekTo((player.currentPosition + 3000L).coerceAtMost(durationMs)) }) {
                    Text("3s ⏩")
                }
            }

            // Repeat Counts: 1x, 3x, 5x, 10x, ∞
            Text("Répétition du verset :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    1 to "1×",
                    3 to "3×",
                    5 to "5×",
                    10 to "10×",
                    -1 to "∞ Boucle"
                ).forEach { (count, label) ->
                    FilterChip(
                        selected = repeatTarget == count,
                        onClick = {
                            currentRepeatCount = 1
                            onRepeatTargetChange(count)
                        },
                        label = { Text(label) }
                    )
                }
            }

            Text("Pause entre répétitions :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0 to "Aucune", 500 to "0,5 s", 700 to "0,7 s", 1_000 to "1 s").forEach { (delayMs, label) ->
                    FilterChip(
                        selected = repeatDelayMs == delayMs,
                        onClick = { repeatDelayMs = delayMs },
                        label = { Text(label) }
                    )
                }
            }

            // Auto-advance toggle chip & Speed selector
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = autoAdvance,
                    onClick = { onAutoAdvanceChange(!autoAdvance) },
                    label = { Text(if (autoAdvance) "✓ Enchaîner (Auto-advance)" else "Enchaîner auto ⏭") }
                )

                listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                    FilterChip(
                        selected = speed == spd,
                        onClick = { speed = spd },
                        label = { Text("${"%.2f".format(spd)}×") }
                    )
                }
            }

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            fallbackMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun String.asAudioUri() = if (startsWith("/")) File(this).toUri() else toUri()

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
}

/**
 * Isolated Composable for word-by-word highlighted Quran verse text.
 * By receiving the playback position via a provider lambda, only this card
 * recomposes on audio pulse updates, avoiding large screen recompositions.
 */
@Composable
private fun VerseWordHighlightCard(
    passage: PlayerPassage,
    isAudioPlaying: Boolean,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier
) {
    val position = positionProvider()
    val activeWord = passage.words.firstOrNull { word ->
        word.startMs != null && word.endMs != null && position in word.startMs until word.endMs
    }
    val highlightColor = MaterialTheme.colorScheme.primaryContainer

    val verse = buildAnnotatedString {
        passage.words.forEachIndexed { index, word ->
            val isWordActive = word.id == activeWord?.id
            if (isWordActive) pushStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold))
            append(word.text)
            if (isWordActive) pop()
            if (index != passage.words.lastIndex) append(' ')
        }
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
            append(ayahEndSign(passage.ayahNumber))
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        border = if (isAudioPlaying) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (passage.hasWordTimings) "● Surlignage synchronisé mot par mot (quran-align)"
                    else "● Synchronisation au niveau du verset",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAudioPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isAudioPlaying) {
                    Text("▶ Récitation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = verse,
                modifier = Modifier.fillMaxWidth(),
                style = QuranTypography.verseText,
                textAlign = TextAlign.Right
            )
        }
    }
}
