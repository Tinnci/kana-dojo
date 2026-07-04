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

data class DailyRhythmDay(
    val offsetFromToday: Int,
    val active: Boolean,
    val isToday: Boolean
)

data class DailyRhythm(
    val title: String,
    val message: String,
    val activeDays: Int,
    val days: List<DailyRhythmDay>
)

data class PathStartGuidance(
    val title: String,
    val message: String
)

data class LessonStartPreview(
    val title: String,
    val message: String,
    val firstExerciseLabel: String,
    val drillCount: Int
)

data class LessonResumeCue(
    val lessonIndex: Int,
    val title: String,
    val message: String,
    val actionLabel: String,
    val progress: Float
)

data class LessonLockCopy(
    val message: String
)

data class StageProgressCopy(
    val message: String
)

data class StageEmptyStateCopy(
    val title: String,
    val message: String,
    val actionLabel: String
)

data class ChartProgressCopy(
    val message: String
)

data class ChartRowGuidanceCopy(
    val title: String,
    val message: String
)

data class ChartContrastSummaryCopy(
    val title: String,
    val message: String
)

data class ChartCardTag(
    val label: String
)

data class ChartTapFeedback(
    val title: String,
    val message: String
)

data class ChartMasteryCopy(
    val label: String
)

data class ChartLegendCopy(
    val message: String
)

enum class PathFeedbackAction {
    StartLesson,
    OpenPractice
}

enum class PathActionTone {
    Advance,
    Review,
    Repair
}

data class PathCompletionFeedback(
    val title: String,
    val message: String,
    val actionLabel: String,
    val action: PathFeedbackAction,
    val tone: PathActionTone,
    val targetLessonIndex: Int? = null,
    val practiceMode: PracticeMode? = null
)

data class PracticeQueueExplanation(
    val title: String,
    val message: String
)

data class PracticeSessionGoal(
    val title: String,
    val message: String
)

data class PracticeModeTabAffordance(
    val label: String,
    val badge: String?
)

data class PracticeQueueSourceCue(
    val title: String,
    val message: String
)

data class ReviewSessionOutcomes(
    val cleanIds: Set<String>,
    val repairedIds: Set<String>,
    val shakyIds: Set<String>
)

data class PracticeCompletionNextStep(
    val title: String,
    val message: String
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

fun dailyRhythmFor(practiceEpochDays: Set<Long>, currentEpochDay: Long): DailyRhythm {
    val days = (6 downTo 0).map { offset ->
        val epochDay = currentEpochDay - offset
        DailyRhythmDay(
            offsetFromToday = offset,
            active = epochDay in practiceEpochDays,
            isToday = offset == 0
        )
    }
    val activeDays = days.count { it.active }
    val practicedToday = days.last().active
    val practicedYesterday = days.getOrNull(days.lastIndex - 1)?.active == true
    val title = when {
        practicedToday -> "Today touched"
        activeDays >= 4 -> "Steady rhythm"
        practicedYesterday -> "Warm start"
        else -> "Fresh start"
    }
    val message = when {
        practicedToday -> "Enough for today; review more only if it feels light."
        activeDays >= 4 -> "$activeDays of 7 days active without chasing a streak."
        practicedYesterday -> "One short lesson keeps yesterday's recall warm."
        else -> "Start with a tiny queue; consistency can stay low-pressure."
    }

    return DailyRhythm(
        title = title,
        message = message,
        activeDays = activeDays,
        days = days
    )
}

fun pathStartGuidanceFor(seenCount: Int, nextStage: LearningStage): PathStartGuidance? {
    if (seenCount > 0) return null
    return when (nextStage) {
        LearningStage.Anchor -> PathStartGuidance(
            title = "Start with sound anchors",
            message = "Vowels make every row easier because each kana maps to one clear Japanese sound."
        )

        else -> PathStartGuidance(
            title = "Start at the first open node",
            message = "The path keeps new kana small, then brings mistakes back before they settle."
        )
    }
}

fun practiceQueueExplanationFor(
    mode: PracticeMode,
    queueSize: Int,
    dueCount: Int,
    weakCount: Int,
    contrastCount: Int,
    soundReadyCount: Int
): PracticeQueueExplanation =
    when {
        queueSize == 0 -> PracticeQueueExplanation(
            title = "Nothing queued",
            message = "Start or finish a lesson first, then practice will have kana to work with."
        )

        mode == PracticeMode.Weak && dueCount > 0 -> PracticeQueueExplanation(
            title = "Due recall first",
            message = "This queue mixes due spaced-review kana with any recent mistakes."
        )

        mode == PracticeMode.Weak && weakCount > 0 -> PracticeQueueExplanation(
            title = "Mistake repair",
            message = "This queue starts from recent misses, then keeps low-mastery kana warm."
        )

        mode == PracticeMode.Weak -> PracticeQueueExplanation(
            title = "Low-mastery fallback",
            message = "No due or missed kana, so this queue uses the lowest-mastery symbols."
        )

        mode == PracticeMode.Sound && soundReadyCount > 0 -> PracticeQueueExplanation(
            title = "Seen kana audio",
            message = "Sound recall uses kana you have already seen at least once."
        )

        mode == PracticeMode.Sound -> PracticeQueueExplanation(
            title = "Sound-safe fallback",
            message = "No seen kana yet, so this queue previews kana that support audio prompts."
        )

        mode == PracticeMode.Contrast && contrastCount > 0 -> PracticeQueueExplanation(
            title = "Lookalike focus",
            message = "This queue targets kana with known visual confusions."
        )

        mode == PracticeMode.Contrast -> PracticeQueueExplanation(
            title = "Contrast fallback",
            message = "No lookalikes are available here, so this queue uses low-mastery kana."
        )

        mode == PracticeMode.Writing -> PracticeQueueExplanation(
            title = "Writing priority",
            message = "Trace practice starts with the least stable kana first."
        )

        mode == PracticeMode.Speed -> PracticeQueueExplanation(
            title = "Fast recall",
            message = "Speed rounds prefer recall-ready kana, with early kana as fallback."
        )

        mode == PracticeMode.Cross -> PracticeQueueExplanation(
            title = "Both scripts",
            message = "Cross-script practice mixes recall-ready hiragana and katakana."
        )

        else -> PracticeQueueExplanation(
            title = "Mixed recall",
            message = "Mixed practice rotates reading, sound, and writing prompts."
        )
    }

fun practiceSessionGoalFor(
    mode: PracticeMode,
    queueSize: Int,
    dueCount: Int,
    weakCount: Int,
    contrastCount: Int
): PracticeSessionGoal =
    when {
        queueSize == 0 -> PracticeSessionGoal(
            title = "Build a queue",
            message = "Complete a lesson first so practice has kana to measure."
        )

        mode == PracticeMode.Weak && dueCount > 0 -> PracticeSessionGoal(
            title = "Clear due recall",
            message = "Finish $queueSize reps with no missed due kana."
        )

        mode == PracticeMode.Weak && weakCount > 0 -> PracticeSessionGoal(
            title = "Repair misses",
            message = "Turn shaky kana into clean or repaired by the end."
        )

        mode == PracticeMode.Weak -> PracticeSessionGoal(
            title = "Lift low mastery",
            message = "Move the least stable kana toward recall."
        )

        mode == PracticeMode.Contrast && contrastCount > 0 -> PracticeSessionGoal(
            title = "Separate lookalikes",
            message = "Keep confusable kana distinct through every prompt."
        )

        mode == PracticeMode.Contrast -> PracticeSessionGoal(
            title = "Sharpen contrast",
            message = "Use the fallback queue to find the next visual weakness."
        )

        mode == PracticeMode.Sound -> PracticeSessionGoal(
            title = "Hear before reading",
            message = "Choose kana from sound before relying on written hints."
        )

        mode == PracticeMode.Writing -> PracticeSessionGoal(
            title = "Stabilize shapes",
            message = "Trace each kana only after the stroke path feels deliberate."
        )

        mode == PracticeMode.Speed -> PracticeSessionGoal(
            title = "Keep recall fast",
            message = "Answer familiar kana without slowing down for labels."
        )

        mode == PracticeMode.Cross -> PracticeSessionGoal(
            title = "Switch scripts cleanly",
            message = "Read hiragana and katakana in one rhythm."
        )

        else -> PracticeSessionGoal(
            title = "Stay flexible",
            message = "Rotate reading, sound, and writing without losing accuracy."
        )
    }

fun practicePreviewReasonFor(
    item: KanaItem,
    mode: PracticeMode,
    mastery: Map<String, Int>,
    mistakeIds: List<String>,
    reviewDueEpochDays: Map<String, Long>,
    currentEpochDay: Long
): String {
    val level = mastery[item.id] ?: 0
    val due = (reviewDueEpochDays[item.id] ?: Long.MAX_VALUE) <= currentEpochDay && level >= 2
    return when (mode) {
        PracticeMode.Weak -> when {
            due -> "due"
            item.id in mistakeIds -> "miss"
            else -> "m$level"
        }

        PracticeMode.Sound -> if (level >= 1) "seen" else "audio"
        PracticeMode.Contrast -> if (item.confusable.isNotEmpty()) "look" else "m$level"
        PracticeMode.Writing -> "m$level"
        PracticeMode.Speed -> if (level >= 2) "recall" else "early"
        PracticeMode.Cross -> if (level >= 2) item.script.label.take(4) else "early"
        PracticeMode.Mixed -> if (level >= 2) "recall" else "m$level"
    }
}

fun practiceModeTabAffordanceFor(
    mode: PracticeMode,
    selectedMode: PracticeMode,
    recommendedMode: PracticeMode,
    fallbackMode: PracticeMode?
): PracticeModeTabAffordance =
    PracticeModeTabAffordance(
        label = mode.label,
        badge = when {
            mode == selectedMode && mode == fallbackMode -> "Fallback"
            mode == selectedMode -> "Now"
            mode == recommendedMode -> "Rec"
            else -> null
        }
    )

fun practiceQueueSourceCueFor(
    script: Script,
    selectedMode: PracticeMode,
    recommendedMode: PracticeMode
): PracticeQueueSourceCue =
    when {
        selectedMode == PracticeMode.Cross -> PracticeQueueSourceCue(
            title = "Both scripts",
            message = "This queue can pull hiragana and katakana together."
        )

        selectedMode == recommendedMode -> PracticeQueueSourceCue(
            title = "${script.label} recommended",
            message = "This queue follows the path recommendation for the selected script."
        )

        else -> PracticeQueueSourceCue(
            title = script.label,
            message = "This queue uses the currently selected script."
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

fun practiceCompletionNextStepFor(mode: PracticeMode, stats: LessonSessionStats): PracticeCompletionNextStep =
    when {
        stats.missed > 0 -> PracticeCompletionNextStep(
            title = "Repeat while fresh",
            message = "Missed kana are still in working memory; run the queue again before switching tasks."
        )

        mode == PracticeMode.Writing -> PracticeCompletionNextStep(
            title = "Shapes are stable",
            message = "Return to the path and let reading or sound recall test the same kana."
        )

        else -> PracticeCompletionNextStep(
            title = "Ready for path",
            message = "Clean queue. Continue while recall is warm."
        )
    }

fun reviewSessionOutcomesFor(correctCounts: Map<String, Int>, missCounts: Map<String, Int>): ReviewSessionOutcomes {
    val ids = correctCounts.keys + missCounts.keys
    return ReviewSessionOutcomes(
        cleanIds = ids.filter { id -> (correctCounts[id] ?: 0) > 0 && (missCounts[id] ?: 0) == 0 }.toSet(),
        repairedIds = ids.filter { id -> (correctCounts[id] ?: 0) > 0 && (missCounts[id] ?: 0) > 0 }.toSet(),
        shakyIds = ids.filter { id -> (correctCounts[id] ?: 0) == 0 && (missCounts[id] ?: 0) > 0 }.toSet()
    )
}
