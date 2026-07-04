package dev.tinnci.kanadojo

data class ProgressUpdate(
    val mastery: Int,
    val missStreak: Int,
    val inMistakes: Boolean
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
            inMistakes = nextMastery < 2
        )
    } else {
        val nextMissStreak = clampedMissStreak + 1
        val shouldDemote = clampedMastery < 5 || nextMissStreak >= 2
        ProgressUpdate(
            mastery = if (shouldDemote) (clampedMastery - 1).coerceAtLeast(0) else clampedMastery,
            missStreak = nextMissStreak,
            inMistakes = true
        )
    }
}
