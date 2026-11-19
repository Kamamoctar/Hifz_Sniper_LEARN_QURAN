package com.hifzsniper.app.domain

import com.hifzsniper.app.domain.PlaybackLoopEngine.SetDecision
import com.hifzsniper.app.domain.PlaybackLoopEngine.TrackDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackLoopEngineTest {
    @Test
    fun finiteVerseRepeatAdvancesOnlyAfterTarget() {
        assertEquals(TrackDecision.Repeat(2), PlaybackLoopEngine.afterTrackEnded(1, 3))
        assertEquals(TrackDecision.Repeat(3), PlaybackLoopEngine.afterTrackEnded(2, 3))
        assertEquals(TrackDecision.VerseCompleted, PlaybackLoopEngine.afterTrackEnded(3, 3))
    }

    @Test
    fun infiniteVerseRepeatKeepsAnAccurateCounter() {
        assertEquals(TrackDecision.Repeat(2), PlaybackLoopEngine.afterTrackEnded(1, -1))
        assertEquals(TrackDecision.Repeat(42), PlaybackLoopEngine.afterTrackEnded(41, -1))
    }

    @Test
    fun setAdvancesWithinCurrentCycle() {
        assertEquals(
            SetDecision.Continue(nextAyah = 4, nextSetCycle = 2),
            PlaybackLoopEngine.afterVerseCompleted(3, 2, 5, 2, 3)
        )
    }

    @Test
    fun finiteSetRestartsAtBeginningUntilTargetCycle() {
        assertEquals(
            SetDecision.Continue(nextAyah = 2, nextSetCycle = 2),
            PlaybackLoopEngine.afterVerseCompleted(5, 2, 5, 1, 3)
        )
        assertEquals(
            SetDecision.Completed,
            PlaybackLoopEngine.afterVerseCompleted(5, 2, 5, 3, 3)
        )
    }

    @Test
    fun infiniteSetAlwaysRestartsAndIncrementsCycle() {
        assertEquals(
            SetDecision.Continue(nextAyah = 2, nextSetCycle = 12),
            PlaybackLoopEngine.afterVerseCompleted(5, 2, 5, 11, -1)
        )
    }

    @Test
    fun singleVerseSetCanRepeatAndComplete() {
        assertEquals(
            SetDecision.Continue(nextAyah = 7, nextSetCycle = 2),
            PlaybackLoopEngine.afterVerseCompleted(7, 7, 7, 1, 3)
        )
        assertEquals(
            SetDecision.Completed,
            PlaybackLoopEngine.afterVerseCompleted(7, 7, 7, 3, 3)
        )
    }

    @Test
    fun unsupportedTargetsFailSafeToOnePass() {
        assertEquals(TrackDecision.VerseCompleted, PlaybackLoopEngine.afterTrackEnded(1, 99))
        assertEquals(
            SetDecision.Completed,
            PlaybackLoopEngine.afterVerseCompleted(1, 1, 1, 1, 99)
        )
    }
}
