package dev.tinnci.kanadojo

import android.content.Context

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("kana_progress", Context.MODE_PRIVATE)

    fun loadMastery(items: List<KanaItem>): Map<String, Int> =
        items.associate { it.id to prefs.getInt("mastery:${it.id}", 0) }

    fun loadMistakes(): Set<String> =
        prefs.getStringSet("mistakes", emptySet()).orEmpty()

    fun loadReduceMotion(): Boolean =
        prefs.getBoolean("reduce_motion", false)

    fun setReduceMotion(enabled: Boolean) {
        prefs.edit().putBoolean("reduce_motion", enabled).apply()
    }

    fun mark(items: List<KanaItem>, correct: Boolean) {
        val editor = prefs.edit()
        val currentMistakes = loadMistakes().toMutableSet()
        items.forEach { item ->
            val key = "mastery:${item.id}"
            val next = (prefs.getInt(key, 0) + if (correct) 1 else -1).coerceIn(0, 5)
            editor.putInt(key, next)
            if (correct && next >= 2) currentMistakes.remove(item.id) else if (!correct) currentMistakes.add(item.id)
        }
        editor.putStringSet("mistakes", currentMistakes).apply()
    }
}
