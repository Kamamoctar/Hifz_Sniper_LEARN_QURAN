package com.hifzsniper.app.presentation.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hifzsniper.app.domain.Rating
import com.hifzsniper.app.domain.RecitationMatcher
import com.hifzsniper.app.presentation.theme.QuranTypography

@Composable
fun VoiceRecitationCard(
    expectedTextUthmani: String,
    onRate: (Rating) -> Unit,
    modifier: Modifier = Modifier,
    prompt: String = "Récitation vocale intelligente"
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by rememberSaveable { mutableStateOf("") }
    var manualTextInput by rememberSaveable { mutableStateOf("") }
    var showManualInput by rememberSaveable { mutableStateOf(false) }
    var evaluation by remember { mutableStateOf<RecitationMatcher.RecitationEvaluation?>(null) }
    var isTextMasked by rememberSaveable { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    val startListeningNow: () -> Unit = {
        errorMessage = null
        evaluation = null
        recognizedText = ""
        runCatching { speechRecognizer?.startListening(recognitionIntent) }
            .onFailure {
                errorMessage = "Le micro n'a pas pu démarrer. Utilisez la saisie manuelle ou vérifiez les permissions."
                showManualInput = true
            }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startListeningNow()
        } else {
            errorMessage = "Permission micro refusée. Autorisez l'accès micro dans les paramètres ou utilisez la saisie manuelle ci-dessous."
            showManualInput = true
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; errorMessage = null }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Aucune parole détectée. Rapprochez le micro et réessayez."
                    SpeechRecognizer.ERROR_NETWORK -> "Connexion ou pack de langue arabe requis."
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur d'enregistrement audio."
                    else -> "Reconnaissance vocale indisponible ($error). Utilisez la saisie manuelle ci-dessous."
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spoken = matches?.firstOrNull() ?: ""
                recognizedText = spoken
                if (spoken.isNotBlank()) {
                    evaluation = RecitationMatcher.evaluate(expectedTextUthmani, spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { partial ->
                    recognizedText = partial
                    evaluation = RecitationMatcher.evaluate(expectedTextUthmani, partial)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { isTextMasked = !isTextMasked }) {
                    Text(if (isTextMasked) "👁 Révéler" else "🙈 Masquer")
                }
            }

            // Expected Verse Text (can be masked for active recall)
            if (!isTextMasked) {
                Text(
                    text = expectedTextUthmani,
                    style = QuranTypography.verseText,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Texte masqué pour tester ta mémorisation.\nRécite à voix haute au micro !",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Microphone Button & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (speechRecognizer != null) {
                    Button(
                        onClick = {
                            if (isListening) {
                                speechRecognizer.stopListening()
                                isListening = false
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    startListeningNow()
                                } else {
                                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(if (isListening) "⏹ Arrêter l'écoute" else "🎙️ Réciter au micro")
                    }
                } else {
                    Text("Reconnaissance vocale indisponible sur cet appareil.", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (isListening) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("  Écoute en cours… Récitez le verset", style = MaterialTheme.typography.bodySmall)
                }
            }

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Recognized speech display
            if (recognizedText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("Paroles détectées :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(recognizedText, style = QuranTypography.pageText, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Recitation Evaluation Results
            evaluation?.let { eval ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (eval.recommendedRating) {
                            Rating.FLUENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            Rating.HESITANT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            Rating.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Score : ${eval.accuracyPercentage} %",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when (eval.recommendedRating) {
                                    Rating.FLUENT -> "🟢 Fluide"
                                    Rating.HESITANT -> "🟡 Hésitant"
                                    Rating.FAILED -> "🔴 À consolider"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(eval.feedbackSummary, style = MaterialTheme.typography.bodyMedium)

                        // Word-by-word visual feedback
                        Text("Analyse mot par mot :", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            eval.wordFeedbacks.forEach { wordFb ->
                                val (bg, fg) = when (wordFb.status) {
                                    RecitationMatcher.WordMatchStatus.MATCHED -> Color(0xFF2E7D32) to Color.White
                                    RecitationMatcher.WordMatchStatus.APPROXIMATE -> Color(0xFFE65100) to Color.White
                                    RecitationMatcher.WordMatchStatus.MISSED -> Color(0xFFC62828) to Color.White
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(bg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(wordFb.originalWord, color = fg, style = QuranTypography.wordText.copy(fontSize = 18.sp))
                                }
                            }
                        }

                        // Submit rating button based on speech recommendation
                        Button(
                            onClick = { onRate(eval.recommendedRating) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Valider l'évaluation (${eval.recommendedRating.name})")
                        }
                    }
                }
            }

            // Fallback manual evaluation
            if (evaluation == null) {
                OutlinedButton(
                    onClick = { showManualInput = !showManualInput },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showManualInput) "Fermer la saisie texte" else "Optionnel : tester par saisie texte")
                }

                if (showManualInput) {
                    OutlinedTextField(
                        value = manualTextInput,
                        onValueChange = { manualTextInput = it },
                        label = { Text("Tape ou colle ta récitation en arabe") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (manualTextInput.isNotBlank()) {
                                evaluation = RecitationMatcher.evaluate(expectedTextUthmani, manualTextInput)
                            }
                        },
                        enabled = manualTextInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Évaluer ma saisie")
                    }
                }
            }
        }
    }
}

