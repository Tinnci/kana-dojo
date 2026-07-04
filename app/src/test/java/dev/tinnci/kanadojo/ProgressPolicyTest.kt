package dev.tinnci.kanadojo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPolicyTest {
    @Test
    fun correctAnswersPromoteMasteryAndClearMissStreak() {
        val update = progressUpdateFor(currentMastery = 2, currentMissStreak = 1, correct = true)

        assertEquals(3, update.mastery)
        assertEquals(0, update.missStreak)
        assertFalse(update.inMistakes)
    }

    @Test
    fun correctAnswersStayInMistakesUntilRecallLevel() {
        val update = progressUpdateFor(currentMastery = 0, currentMissStreak = 0, correct = true)

        assertEquals(1, update.mastery)
        assertTrue(update.inMistakes)
    }

    @Test
    fun incorrectAnswersDemoteNonMasteredKanaImmediately() {
        val update = progressUpdateFor(currentMastery = 3, currentMissStreak = 0, correct = false)

        assertEquals(2, update.mastery)
        assertEquals(1, update.missStreak)
        assertTrue(update.inMistakes)
    }

    @Test
    fun masteredKanaNeedsRepeatedMissBeforeDemotion() {
        val firstMiss = progressUpdateFor(currentMastery = 5, currentMissStreak = 0, correct = false)
        val secondMiss = progressUpdateFor(currentMastery = firstMiss.mastery, currentMissStreak = firstMiss.missStreak, correct = false)

        assertEquals(5, firstMiss.mastery)
        assertEquals(1, firstMiss.missStreak)
        assertTrue(firstMiss.inMistakes)
        assertEquals(4, secondMiss.mastery)
        assertEquals(2, secondMiss.missStreak)
        assertTrue(secondMiss.inMistakes)
    }

    @Test
    fun masteryBoundsStayWithinStoredRange() {
        assertEquals(5, progressUpdateFor(currentMastery = 5, currentMissStreak = 0, correct = true).mastery)
        assertEquals(0, progressUpdateFor(currentMastery = 0, currentMissStreak = 0, correct = false).mastery)
    }
}
