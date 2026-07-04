package dev.tinnci.kanadojo

enum class Script(val label: String) {
    Hiragana("Hiragana"),
    Katakana("Katakana")
}

enum class ScreenTab(val label: String) {
    Lessons("Lessons"),
    Chart("Chart"),
    Mistakes("Practice")
}

enum class ExerciseKind {
    KanaToRomaji,
    RomajiToKana,
    SoundToKana,
    PairMatch,
    TraceKana
}

enum class PracticeMode(val label: String, val title: String, val subtitle: String) {
    Weak("Weak", "Weak repair", "Replay misses and low-mastery kana."),
    Contrast("Contrast", "Lookalike contrast", "Separate symbols that are easy to confuse."),
    Sound("Sound", "Sound recall", "Choose kana from Japanese audio first."),
    Writing("Write", "Writing reps", "Trace symbols until the shape feels familiar."),
    Speed("Speed", "Speed round", "Fast recognition with familiar kana."),
    Mixed("Mixed", "Mixed recall", "Keep familiar kana fast and automatic.")
}

enum class LearningStage(val label: String, val description: String) {
    Anchor("Anchor", "sound anchors"),
    RegularRows("Rows", "regular row rhythm"),
    ShapeHeavy("Shapes", "stroke-heavy symbols"),
    TailRows("Tail", "remaining base kana"),
    Voiced("Marks", "dakuten and handakuten"),
    Confusable("Contrast", "lookalike separation")
}

data class KanaItem(
    val id: String,
    val script: Script,
    val kana: String,
    val romaji: String,
    val row: String,
    val lesson: Int,
    val confusable: List<String> = emptyList()
)

data class KanaLesson(
    val index: Int,
    val title: String,
    val subtitle: String,
    val stage: LearningStage,
    val difficulty: Int,
    val items: List<KanaItem>
)

data class Exercise(
    val kind: ExerciseKind,
    val items: List<KanaItem>
)

data class AnswerFeedback(
    val correct: Boolean,
    val answer: String
)

data class ProgressSnapshot(
    val seen: Int,
    val recall: Int,
    val fluent: Int,
    val total: Int
) {
    val overall: Float = if (total == 0) 0f else fluent / total.toFloat()
}

data class LessonSessionStats(
    val correct: Int = 0,
    val missed: Int = 0
) {
    val attempts: Int = correct + missed
    val accuracy: Float = if (attempts == 0) 0f else correct / attempts.toFloat()
}
