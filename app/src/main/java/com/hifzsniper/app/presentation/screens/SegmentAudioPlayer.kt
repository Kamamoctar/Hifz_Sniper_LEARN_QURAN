package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Plays only the timestamped word range used by the active Hifz chunk. */
@Composable
fun SegmentAudioPlayer(
    sources: List<String>,
    startMs: Long,
    endMs: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val availableSources = sources.filter(String::isNotBlank).distinct()
    require(availableSources.isNotEmpty()) { "Au moins une source audio est requise" }
    val sourceKey = availableSources.joinToString("|")
    var repeatTarget by remember { mutableIntStateOf(3) }
    var completedRepeats by remember(sourceKey, startMs, endMs) { mutableIntStateOf(0) }
    var isPlaying by remember(sourceKey) { mutableStateOf(false) }
    var sourceIndex by remember(sourceKey) { mutableIntStateOf(0) }
    var statusMessage by remember(sourceKey) { mutableStateOf<String?>(null) }
    var errorMessage by remember(sourceKey) { mutableStateOf<String?>(null) }

    val player = remember(sourceKey) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            setHandleAudioBecomingNoisy(true)
            setMediaItem(MediaItem.fromUri(availableSources.first().segmentAudioUri()))
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(value: Boolean) {
                    isPlaying = value
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val nextIndex = sourceIndex + 1
                    if (nextIndex < availableSources.size) {
                        val shouldResume = playWhenReady || isPlaying
                        sourceIndex = nextIndex
                        errorMessage = null
                        statusMessage = "Source indisponible · utilisation automatique du miroir ${nextIndex + 1}/${availableSources.size}."
                        setMediaItem(MediaItem.fromUri(availableSources[nextIndex].segmentAudioUri()))
                        prepare()
                        seekTo(startMs)
                        playWhenReady = shouldResume
                    } else {
                        errorMessage = "Audio indisponible après ${availableSources.size} source(s)."
                    }
                }
            })
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(player, isPlaying, startMs, endMs, repeatTarget) {
        while (isActive && isPlaying) {
            delay(40)
            if (player.currentPosition >= endMs) {
                completedRepeats += 1
                if (completedRepeats < repeatTarget) {
                    player.seekTo(startMs)
                    player.play()
                } else {
                    player.pause()
                    player.seekTo(startMs)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else {
                        completedRepeats = 0
                        errorMessage = null
                        if (player.playerError != null) {
                            sourceIndex = 0
                            statusMessage = null
                            player.setMediaItem(MediaItem.fromUri(availableSources.first().segmentAudioUri()))
                            player.prepare()
                        }
                        player.seekTo(startMs)
                        player.play()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isPlaying) "Pause" else "Écouter le groupe")
            }
            listOf(1, 3, 5).forEach { count ->
                FilterChip(
                    selected = repeatTarget == count,
                    onClick = { repeatTarget = count },
                    label = { Text("$count×", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall) }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun String.segmentAudioUri() = if (startsWith("/")) File(this).toUri() else toUri()
