package com.hifzsniper.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hifzsniper.app.domain.PlaybackLoopEngine
import com.hifzsniper.app.presentation.screens.NativeComposeAudioPlayer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AudioLoopIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<AudioLoopTestActivity>()

    @Test
    fun threeVerseRepeatsEmitOneCompletionAfterRealPlaybackEnds() {
        val audioFile = composeRule.activity.cacheDir.resolve("loop-test.wav")
        audioFile.writeBytes(createPcmWave(durationMs = 200))
        val completions = AtomicInteger(0)

        composeRule.setContent {
            MaterialTheme {
                NativeComposeAudioPlayer(
                    sources = listOf(audioFile.absolutePath),
                    repeatTarget = 3,
                    autoAdvance = true,
                    onPosition = {},
                    onAutoAdvance = { completions.incrementAndGet() }
                )
            }
        }

        composeRule.onNodeWithText("▶ Lecture").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { completions.get() == 1 }
        assertEquals(1, completions.get())
    }

    @Test
    fun unavailablePrimarySourceFallsBackToLocalMirror() {
        val mirror = composeRule.activity.cacheDir.resolve("fallback-mirror.wav")
        mirror.writeBytes(createPcmWave(durationMs = 200))
        val missingPrimary = composeRule.activity.cacheDir.resolve("missing-primary.wav")
        val completions = AtomicInteger(0)

        composeRule.setContent {
            MaterialTheme {
                NativeComposeAudioPlayer(
                    sources = listOf(missingPrimary.absolutePath, mirror.absolutePath),
                    repeatTarget = 1,
                    autoAdvance = true,
                    onPosition = {},
                    onAutoAdvance = { completions.incrementAndGet() }
                )
            }
        }

        composeRule.onNodeWithText("▶ Lecture").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) { completions.get() == 1 }
        assertEquals(1, completions.get())
    }

    @Test
    fun twoVerseSetRepeatsEachVerseAndCompletesTwoCycles() {
        val verseFiles = (1..2).map { index ->
            composeRule.activity.cacheDir.resolve("set-verse-$index.wav").apply {
                writeBytes(createPcmWave(durationMs = 200))
            }
        }
        val setCompletions = AtomicInteger(0)

        composeRule.setContent {
            MaterialTheme {
                var currentAyah by remember { mutableIntStateOf(1) }
                var currentCycle by remember { mutableIntStateOf(1) }
                var continuePlayback by remember { mutableStateOf(false) }

                NativeComposeAudioPlayer(
                    sources = listOf(verseFiles[currentAyah - 1].absolutePath),
                    autoPlay = continuePlayback,
                    repeatTarget = 2,
                    autoAdvance = true,
                    onPosition = {},
                    onPlaybackIntentChanged = { continuePlayback = it },
                    onAutoAdvance = {
                        when (val decision = PlaybackLoopEngine.afterVerseCompleted(
                            currentAyah = currentAyah,
                            startAyah = 1,
                            endAyah = 2,
                            currentSetCycle = currentCycle,
                            setRepeatTarget = 2
                        )) {
                            is PlaybackLoopEngine.SetDecision.Continue -> {
                                currentAyah = decision.nextAyah
                                currentCycle = decision.nextSetCycle
                            }
                            PlaybackLoopEngine.SetDecision.Completed -> {
                                continuePlayback = false
                                setCompletions.incrementAndGet()
                            }
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithText("▶ Lecture").performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) { setCompletions.get() == 1 }
        assertEquals(1, setCompletions.get())
    }

    private fun createPcmWave(durationMs: Int): ByteArray {
        val sampleRate = 8_000
        val channels = 1
        val bitsPerSample = 16
        val sampleCount = sampleRate * durationMs / 1_000
        val dataSize = sampleCount * channels * (bitsPerSample / 8)
        return ByteBuffer.allocate(44 + dataSize)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".encodeToByteArray())
                putInt(36 + dataSize)
                put("WAVE".encodeToByteArray())
                put("fmt ".encodeToByteArray())
                putInt(16)
                putShort(1.toShort())
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(sampleRate * channels * (bitsPerSample / 8))
                putShort((channels * (bitsPerSample / 8)).toShort())
                putShort(bitsPerSample.toShort())
                put("data".encodeToByteArray())
                putInt(dataSize)
                repeat(sampleCount) { putShort(0) }
            }
            .array()
    }
}
