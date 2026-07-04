package dev.tinnci.kanadojo

import android.content.Context

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("kana_progress", Context.MODE_PRIVATE)

    fun loadMastery(items: List<KanaItem>): Map<String, Int> =
        items.associate { it.id to prefs.getInt("mastery:${it.id}", 0) }

    fun loadMistakes(): Set<String> =
        prefs.getStringSet("mistakes", emptySet()).orEmpty()

    fun loadReviewDueEpochDays(items: List<KanaItem>): Map<String, Long> =
        items.associate { it.id to prefs.getLong("review_due_epoch_day:${it.id}", 0L) }

    fun loadPracticeEpochDays(currentEpochDay: Long = currentEpochDay()): Set<Long> =
        prefs.getStringSet("practice_epoch_days", emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .filter { it in (currentEpochDay - 6)..currentEpochDay }
            .toSet()

    fun loadReduceMotion(): Boolean =
        prefs.getBoolean("reduce_motion", false)

    fun setReduceMotion(enabled: Boolean) {
        prefs.edit().putBoolean("reduce_motion", enabled).apply()
    }

    fun loadSoundEnabled(): Boolean =
        prefs.getBoolean("sound_enabled", true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun loadHapticsEnabled(): Boolean =
        prefs.getBoolean("haptics_enabled", true)

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun mark(items: List<KanaItem>, correct: Boolean) {
        val editor = prefs.edit()
        val currentMistakes = loadMistakes().toMutableSet()
        val today = currentEpochDay()
        items.forEach { item ->
            val masteryKey = "mastery:${item.id}"
            val missStreakKey = "miss_streak:${item.id}"
            val reviewDueKey = "review_due_epoch_day:${item.id}"
            val update = progressUpdateFor(
                currentMastery = prefs.getInt(masteryKey, 0),
                currentMissStreak = prefs.getInt(missStreakKey, 0),
                correct = correct
            )
            editor.putInt(masteryKey, update.mastery)
            editor.putInt(missStreakKey, update.missStreak)
            editor.putLong(reviewDueKey, today + update.reviewDelayDays)
            if (update.inMistakes) {
                currentMistakes.add(item.id)
            } else {
                currentMistakes.remove(item.id)
            }
        }
        val practiceDays = (loadPracticeEpochDays(today) + today)
            .filter { it in (today - 6)..today }
            .map { it.toString() }
            .toSet()
        editor
            .putStringSet("mistakes", currentMistakes)
            .putStringSet("practice_epoch_days", practiceDays)
            .apply()
    }

}
