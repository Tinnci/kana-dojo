package dev.tinnci.kanadojo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaHapticsTest {
    @Test
    fun frequentControlTapticsStaySinglePulse() {
        val frequentControls = listOf(
            KanaTaptic.Navigate,
            KanaTaptic.Select,
            KanaTaptic.Continue,
            KanaTaptic.Speak,
            KanaTaptic.ToggleOn,
            KanaTaptic.ToggleOff
        )

        frequentControls.forEach { taptic ->
            val timing = kanaTapticTimingFor(taptic)

            assertEquals("$taptic should be a single pulse", 1, timing.pulseCount)
            assertEquals("$taptic should not schedule delayed follow-up pulses", 0L, timing.lastDelayMs)
        }
    }

    @Test
    fun answerAndRepairTapticsStaySinglePulse() {
        val resultTaptics = listOf(
            KanaTaptic.Correct,
            KanaTaptic.Incorrect,
            KanaTaptic.Review,
            KanaTaptic.Reset,
            KanaTaptic.Start
        )

        resultTaptics.forEach { taptic ->
            val timing = kanaTapticTimingFor(taptic)

            assertEquals("$taptic should be a single semantic haptic", 1, timing.pulseCount)
            assertEquals("$taptic should feel immediate", 0L, timing.lastDelayMs)
        }
    }

    @Test
    fun completionTapticIsOnlyMultiPulseCelebrationAndStaysBrief() {
        val multiPulseTaptics = KanaTaptic.values()
            .filter { kanaTapticTimingFor(it).pulseCount > 1 }
        val timing = kanaTapticTimingFor(KanaTaptic.Complete)

        assertEquals(listOf(KanaTaptic.Complete), multiPulseTaptics)
        assertEquals(3, timing.pulseCount)
        assertEquals(180L, timing.lastDelayMs)
        assertTrue(timing.lastDelayMs < 220L)
    }
}
