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
    Cross("Both", "Both scripts", "Read hiragana and katakana in one queue."),
    Mixed("Mixed", "Mixed recall", "Keep familiar kana fast and automatic.")
}

data class PracticeIntroCopy(
    val title: String,
    val subtitle: String,
    val actionLabel: String
)

data class PracticeRecommendation(
    val mode: PracticeMode,
    val title: String,
    val message: String,
    val actionLabel: String
)

data class ReviewSessionOutcomes(
    val cleanIds: Set<String>,
    val repairedIds: Set<String>,
    val shakyIds: Set<String>
)

fun pathPracticeRecommendationFor(
    dueReviewCount: Int,
    weakCount: Int,
    stage: LearningStage
): PracticeRecommendation =
    when {
        dueReviewCount > 0 -> PracticeRecommendation(
            mode = PracticeMode.Weak,
            title = "Due review",
            message = "$dueReviewCount kana are ready for spaced recall.",
            actionLabel = "Review due"
        )

        weakCount > 0 -> PracticeRecommendation(
            mode = PracticeMode.Weak,
            title = "Repair weak kana",
            message = "$weakCount kana need a quick repair pass.",
            actionLabel = "Repair"
        )

        stage == LearningStage.Confusable -> PracticeRecommendation(
            mode = PracticeMode.Contrast,
            title = "Contrast drill",
            message = "Separate lookalike kana before the next lesson.",
            actionLabel = "Contrast"
        )

        stage == LearningStage.ShapeHeavy || stage == LearningStage.Special -> PracticeRecommendation(
            mode = PracticeMode.Writing,
            title = "Writing reps",
            message = "Trace the shapes until the stroke pattern feels stable.",
            actionLabel = "Write"
        )

        else -> PracticeRecommendation(
            mode = PracticeMode.Sound,
            title = "Sound recall",
            message = "Listen first so kana map directly to Japanese sound.",
            actionLabel = "Listen"
        )
    }

fun reviewIntroCopyFor(mode: PracticeMode, dueCount: Int, weakCount: Int): PracticeIntroCopy =
    when (mode) {
        PracticeMode.Weak -> when {
            dueCount > 0 -> PracticeIntroCopy(
                title = "Due recall",
                subtitle = "Start with kana whose spacing has matured today.",
                actionLabel = "Review due"
            )

            weakCount > 0 -> PracticeIntroCopy(
                title = "Mistake repair",
                subtitle = "Replay missed kana before they settle into the wrong shape.",
                actionLabel = "Repair mistakes"
            )

            else -> PracticeIntroCopy(
                title = "Low-mastery repair",
                subtitle = "Build shaky kana toward stable recall.",
                actionLabel = "Start repair"
            )
        }

        else -> PracticeIntroCopy(
            title = mode.title,
            subtitle = mode.subtitle,
            actionLabel = "Start"
        )
    }

enum class LearningStage(val label: String, val description: String) {
    Anchor("Anchor", "sound anchors"),
    RegularRows("Rows", "regular row rhythm"),
    ShapeHeavy("Shapes", "stroke-heavy symbols"),
    TailRows("Tail", "remaining base kana"),
    Voiced("Marks", "dakuten and handakuten"),
    Combination("Blend", "small ya yu yo combinations"),
    Special("Special", "small kana and length marks"),
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

data class LessonPhaseCount(
    val label: String,
    val count: Int
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

enum class CompletionRecommendation(val message: String) {
    RepeatRow("Recommended: repeat this row while it is fresh."),
    ReviewMisses("Recommended: repair the missed kana next."),
    BackToPath("Recommended: return to the path for the next lesson.")
}

enum class ReviewCompletionAction {
    ReturnToPath,
    RepeatQueue
}

fun completionRecommendationFor(stats: LessonSessionStats): CompletionRecommendation =
    when {
        stats.accuracy < 0.75f -> CompletionRecommendation.RepeatRow
        stats.missed > 0 -> CompletionRecommendation.ReviewMisses
        else -> CompletionRecommendation.BackToPath
    }

fun reviewCompletionActionFor(stats: LessonSessionStats): ReviewCompletionAction =
    if (stats.attempts > 0 && stats.missed == 0) {
        ReviewCompletionAction.ReturnToPath
    } else {
        ReviewCompletionAction.RepeatQueue
    }

fun reviewSessionOutcomesFor(correctCounts: Map<String, Int>, missCounts: Map<String, Int>): ReviewSessionOutcomes {
    val ids = correctCounts.keys + missCounts.keys
    return ReviewSessionOutcomes(
        cleanIds = ids.filter { id -> (correctCounts[id] ?: 0) > 0 && (missCounts[id] ?: 0) == 0 }.toSet(),
        repairedIds = ids.filter { id -> (correctCounts[id] ?: 0) > 0 && (missCounts[id] ?: 0) > 0 }.toSet(),
        shakyIds = ids.filter { id -> (correctCounts[id] ?: 0) == 0 && (missCounts[id] ?: 0) > 0 }.toSet()
    )
}
