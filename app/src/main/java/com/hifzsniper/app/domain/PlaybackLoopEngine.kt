package com.hifzsniper.app.domain

/** Pure loop decisions shared by the audio player and its unit tests. */
object PlaybackLoopEngine {
    const val INFINITE = -1
    private val supportedTargets = setOf(1, 3, 5, 10, INFINITE)

    sealed interface TrackDecision {
        data class Repeat(val nextRepeatCount: Int) : TrackDecision
        data object VerseCompleted : TrackDecision
    }

    sealed interface SetDecision {
        data class Continue(val nextAyah: Int, val nextSetCycle: Int) : SetDecision
        data object Completed : SetDecision
    }

    fun afterTrackEnded(currentRepeatCount: Int, repeatTarget: Int): TrackDecision {
        val count = currentRepeatCount.coerceAtLeast(1)
        val target = repeatTarget.takeIf { it in supportedTargets } ?: 1
        return if (target == INFINITE || count < target) {
            TrackDecision.Repeat(count + 1)
        } else {
            TrackDecision.VerseCompleted
        }
    }

    fun afterVerseCompleted(
        currentAyah: Int,
        startAyah: Int,
        endAyah: Int,
        currentSetCycle: Int,
        setRepeatTarget: Int
    ): SetDecision {
        val start = startAyah.coerceAtLeast(1)
        val end = endAyah.coerceAtLeast(start)
        val ayah = currentAyah.coerceIn(start, end)
        val cycle = currentSetCycle.coerceAtLeast(1)
        val target = setRepeatTarget.takeIf { it in supportedTargets } ?: 1

        if (ayah < end) return SetDecision.Continue(ayah + 1, cycle)
        if (target == INFINITE || cycle < target) {
            return SetDecision.Continue(start, cycle + 1)
        }
        return SetDecision.Completed
    }
}
