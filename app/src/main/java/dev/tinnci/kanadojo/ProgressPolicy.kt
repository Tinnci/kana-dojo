package dev.tinnci.kanadojo

import java.util.concurrent.TimeUnit

data class ProgressUpdate(
    val mastery: Int,
    val missStreak: Int,
    val inMistakes: Boolean,
    val reviewDelayDays: Int
)

fun progressUpdateFor(
    currentMastery: Int,
    currentMissStreak: Int,
    correct: Boolean
): ProgressUpdate {
    val clampedMastery = currentMastery.coerceIn(0, 5)
    val clampedMissStreak = currentMissStreak.coerceAtLeast(0)

    return if (correct) {
        val nextMastery = (clampedMastery + 1).coerceAtMost(5)
        ProgressUpdate(
            mastery = nextMastery,
            missStreak = 0,
            inMistakes = nextMastery < 2,
            reviewDelayDays = reviewDelayDaysFor(nextMastery, correct = true)
        )
    } else {
        val nextMissStreak = clampedMissStreak + 1
        val shouldDemote = clampedMastery < 5 || nextMissStreak >= 2
        val nextMastery = if (shouldDemote) (clampedMastery - 1).coerceAtLeast(0) else clampedMastery
        ProgressUpdate(
            mastery = nextMastery,
            missStreak = nextMissStreak,
            inMistakes = true,
            reviewDelayDays = reviewDelayDaysFor(nextMastery, correct = false)
        )
    }
}

fun reviewDelayDaysFor(mastery: Int, correct: Boolean): Int {
    if (!correct) return 0
    return when (mastery.coerceIn(0, 5)) {
        0, 1 -> 0
        2 -> 1
        3 -> 3
        4 -> 7
        else -> 14
    }
}

fun currentEpochDay(): Long =
    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
