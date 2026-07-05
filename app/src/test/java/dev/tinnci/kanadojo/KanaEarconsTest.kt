package dev.tinnci.kanadojo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaEarconsTest {
    @Test
    fun frequentControlEarconsStaySinglePulseAndUnderFortyMilliseconds() {
        val frequentControls = listOf(
            KanaEarcon.Navigate to 28,
            KanaEarcon.Select to 30,
            KanaEarcon.Continue to 36
        )

        frequentControls.forEach { (earcon, expectedTotalMs) ->
            val timing = kanaEarconTimingFor(earcon)

            assertEquals(1, timing.pulseCount)
            assertEquals(expectedTotalMs, timing.totalMs)
            assertTrue(timing.totalMs < 40)
        }
    }

    @Test
    fun answerAndRepairEarconsStaySinglePulseAndUnderEightyMilliseconds() {
        val semanticResults = listOf(
            KanaEarcon.Correct to 54,
            KanaEarcon.Incorrect to 72,
            KanaEarcon.Review to 58,
            KanaEarcon.Reset to 64
        )

        semanticResults.forEach { (earcon, expectedTotalMs) ->
            val timing = kanaEarconTimingFor(earcon)

            assertEquals(1, timing.pulseCount)
            assertEquals(expectedTotalMs, timing.totalMs)
            assertTrue(timing.totalMs < 80)
        }
    }

    @Test
    fun startEarconIsBriefTransitionCueNotCelebration() {
        val timing = kanaEarconTimingFor(KanaEarcon.Start)

        assertEquals(2, timing.pulseCount)
        assertEquals(84, timing.activeMs)
        assertEquals(46, timing.longestPulseMs)
        assertEquals(110, timing.totalMs)
        assertTrue(timing.totalMs <= 120)
    }

    @Test
    fun completionEarconIsOnlyThreePulseCelebrationAndStaysBelowQuarterSecond() {
        val multiPulseEarcons = KanaEarcon.values()
            .filter { kanaEarconTimingFor(it).pulseCount > 1 }

        val timing = kanaEarconTimingFor(KanaEarcon.Complete)

        assertEquals(listOf(KanaEarcon.Start, KanaEarcon.Complete), multiPulseEarcons)
        assertEquals(3, timing.pulseCount)
        assertEquals(156, timing.activeMs)
        assertEquals(60, timing.longestPulseMs)
        assertEquals(212, timing.totalMs)
        assertTrue(timing.totalMs < 250)
    }
}
