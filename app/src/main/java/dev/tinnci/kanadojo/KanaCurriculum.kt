package dev.tinnci.kanadojo

import kotlin.random.Random

private const val PairMatchTargetSize = 4
private const val MinimumPairMatchSize = 2

fun buildLessonExercises(lesson: KanaLesson): List<Exercise> {
    val recognition = lesson.items.flatMap { item ->
        buildList {
            add(Exercise(ExerciseKind.RomajiToKana, listOf(item)))
            add(Exercise(ExerciseKind.KanaToRomaji, listOf(item)))
        }
    }
    val listening = lesson.items
        .filter { supportsAudioPrompt(it) }
        .map { Exercise(ExerciseKind.SoundToKana, listOf(it)) }
    val pairs = pairMatchExercisesFor(lesson.items)
    val writing = lesson.items.take(writingCountFor(lesson)).map { Exercise(ExerciseKind.TraceKana, listOf(it)) }
    val contrastItems = contrastItemsFor(lesson)
    val contrast = contrastItems
        .flatMap { listOf(Exercise(ExerciseKind.RomajiToKana, listOf(it)), Exercise(ExerciseKind.TraceKana, listOf(it))) }
    return recognition + listening + pairs + writing + contrast
}

fun lessonStartPreviewFor(lesson: KanaLesson): LessonStartPreview {
    val exercises = buildLessonExercises(lesson)
    val first = exercises.firstOrNull()
    val firstItem = first?.items?.firstOrNull() ?: lesson.items.first()
    val label = first?.kind?.lessonStartLabel().orEmpty()
    val prompt = when (first?.kind) {
        ExerciseKind.RomajiToKana -> "choose ${firstItem.kana} for ${firstItem.romaji}"
        ExerciseKind.KanaToRomaji -> "read ${firstItem.kana} as ${firstItem.romaji}"
        ExerciseKind.SoundToKana -> "listen for ${firstItem.romaji}"
        ExerciseKind.PairMatch -> "match ${first.items.size} kana pairs"
        ExerciseKind.TraceKana -> "trace ${firstItem.kana}"
        null -> "start the lesson"
    }
    return LessonStartPreview(
        title = "First: $label",
        message = "Begin with $prompt, then continue through ${exercises.size} short drills.",
        firstExerciseLabel = label,
        drillCount = exercises.size
    )
}

fun lessonResumeCueFor(lesson: KanaLesson, completed: Int, total: Int): LessonResumeCue? {
    val safeTotal = total.coerceAtLeast(1)
    val safeCompleted = completed.coerceIn(0, safeTotal)
    if (safeCompleted == 0 || safeCompleted >= safeTotal) return null
    return LessonResumeCue(
        lessonIndex = lesson.index,
        title = "Return to ${lesson.title}",
        message = "You left after $safeCompleted of $safeTotal drills. Return while the kana are still warm.",
        actionLabel = "Return",
        progress = safeCompleted / safeTotal.toFloat()
    )
}

fun pathCompletionFeedbackFor(
    completedLesson: KanaLesson,
    stats: LessonSessionStats,
    nextLesson: KanaLesson,
    practiceRecommendation: PracticeRecommendation
): PathCompletionFeedback =
    if (stats.missed > 0 || practiceRecommendation.mode == PracticeMode.Weak) {
        PathCompletionFeedback(
            title = if (stats.missed > 0) "Misses queued" else practiceRecommendation.title,
            message = if (stats.missed > 0) {
                "${stats.missed} misses changed the next action to ${practiceRecommendation.title.lowercase()}."
            } else {
                "Review moved ahead of the path so recall stays stable."
            },
            actionLabel = practiceRecommendation.actionLabel,
            action = PathFeedbackAction.OpenPractice,
            tone = if (stats.missed > 0 || "repair" in practiceRecommendation.title.lowercase()) {
                PathActionTone.Repair
            } else {
                PathActionTone.Review
            },
            practiceMode = practiceRecommendation.mode
        )
    } else {
        PathCompletionFeedback(
            title = "Next lesson ready",
            message = "${completedLesson.title} is warm. The path now points to ${nextLesson.title}.",
            actionLabel = "Start next",
            action = PathFeedbackAction.StartLesson,
            tone = PathActionTone.Advance,
            targetLessonIndex = nextLesson.index
        )
    }

fun lessonPhaseSummaryFor(lesson: KanaLesson): List<LessonPhaseCount> =
    listOf(
        LessonPhaseCount("Read", lesson.items.size * 2),
        LessonPhaseCount("Hear", lesson.items.count { supportsAudioPrompt(it) }),
        LessonPhaseCount("Match", pairMatchExercisesFor(lesson.items).size),
        LessonPhaseCount("Write", writingCountFor(lesson)),
        LessonPhaseCount("Contrast", contrastItemsFor(lesson).size * 2)
    ).filter { it.count > 0 }

private fun pairMatchExercisesFor(items: List<KanaItem>): List<Exercise> {
    if (items.size < MinimumPairMatchSize) return emptyList()

    val chunks = items.chunked(PairMatchTargetSize).toMutableList()
    if (chunks.size > 1 && chunks.last().size < MinimumPairMatchSize) {
        val tail = chunks.removeAt(chunks.lastIndex)
        chunks[chunks.lastIndex] = chunks.last() + tail
    }

    return chunks.map { Exercise(ExerciseKind.PairMatch, it) }
}

private fun ExerciseKind.lessonStartLabel(): String =
    when (this) {
        ExerciseKind.KanaToRomaji -> "Read it"
        ExerciseKind.RomajiToKana -> "Find the kana"
        ExerciseKind.SoundToKana -> "Hear it"
        ExerciseKind.PairMatch -> "Match pairs"
        ExerciseKind.TraceKana -> "Write it"
    }

private fun writingCountFor(lesson: KanaLesson): Int =
    when (lesson.stage) {
        LearningStage.Anchor -> 2
        LearningStage.RegularRows -> 3
        LearningStage.ShapeHeavy -> 4
        LearningStage.TailRows -> lesson.items.size
        LearningStage.Voiced -> lesson.items.size
        LearningStage.Combination -> lesson.items.size
        LearningStage.Special -> lesson.items.size
        LearningStage.Confusable -> lesson.items.size
    }

private fun contrastItemsFor(lesson: KanaLesson): List<KanaItem> =
    lesson.items.filter { it.confusable.isNotEmpty() }

fun supportsAudioPrompt(item: KanaItem): Boolean =
    item.row != "special"

fun buildMistakeExercise(item: KanaItem): Exercise {
    val kinds = buildList {
        add(ExerciseKind.RomajiToKana)
        add(ExerciseKind.KanaToRomaji)
        if (supportsAudioPrompt(item)) {
            add(ExerciseKind.SoundToKana)
        }
        add(ExerciseKind.TraceKana)
    }
    return Exercise(
        kind = kinds.random(Random(item.id.hashCode())),
        items = listOf(item)
    )
}

fun practiceItemsFor(
    mode: PracticeMode,
    scriptItems: List<KanaItem>,
    mistakeIds: List<String>,
    allItems: List<KanaItem>,
    mastery: Map<String, Int>,
    reviewDueEpochDays: Map<String, Long> = emptyMap(),
    currentEpochDay: Long = 0L
): List<KanaItem> {
    val byId = allItems.associateBy { it.id }
    return when (mode) {
        PracticeMode.Weak -> (
            mistakeIds
                .mapNotNull { byId[it] }
                .filter { it.script == scriptItems.firstOrNull()?.script } +
                dueReviewItemsFor(scriptItems, reviewDueEpochDays, currentEpochDay, mastery)
            )
            .distinctBy { it.id }
            .ifEmpty { scriptItems.sortedBy { mastery[it.id] ?: 0 }.take(6) }

        PracticeMode.Contrast -> scriptItems
            .filter { it.confusable.isNotEmpty() }
            .ifEmpty { scriptItems.sortedBy { mastery[it.id] ?: 0 }.take(6) }

        PracticeMode.Sound -> scriptItems
            .filter { supportsAudioPrompt(it) }
            .filter { (mastery[it.id] ?: 0) >= 1 }
            .ifEmpty { scriptItems.filter { supportsAudioPrompt(it) }.take(6) }

        PracticeMode.Writing -> scriptItems
            .sortedWith(compareBy<KanaItem> { mastery[it.id] ?: 0 }.thenBy { it.lesson })
            .take(8)

        PracticeMode.Speed -> scriptItems
            .filter { (mastery[it.id] ?: 0) >= 3 }
            .ifEmpty { scriptItems.take(8) }
            .shuffled(Random(scriptItems.firstOrNull()?.script?.name.hashCode() + 17))

        PracticeMode.Cross -> allItems
            .filter { (mastery[it.id] ?: 0) >= 2 }
            .ifEmpty { allItems.filter { it.lesson <= 2 }.take(12) }
            .shuffled(Random(41))

        PracticeMode.Mixed -> scriptItems
            .filter { (mastery[it.id] ?: 0) >= 2 }
            .ifEmpty { scriptItems.sortedBy { mastery[it.id] ?: 0 }.take(8) }
            .shuffled(Random(scriptItems.firstOrNull()?.script?.name.hashCode()))
    }
}

fun practiceExerciseFor(item: KanaItem, mode: PracticeMode, index: Int): Exercise =
    when (mode) {
        PracticeMode.Weak -> buildMistakeExercise(item)
        PracticeMode.Contrast -> Exercise(
            kind = if (index % 3 == 2) ExerciseKind.TraceKana else ExerciseKind.RomajiToKana,
            items = listOf(item)
        )

        PracticeMode.Sound -> Exercise(if (supportsAudioPrompt(item)) ExerciseKind.SoundToKana else ExerciseKind.RomajiToKana, listOf(item))
        PracticeMode.Writing -> Exercise(ExerciseKind.TraceKana, listOf(item))
        PracticeMode.Speed -> Exercise(
            kind = if (index % 2 == 0) ExerciseKind.KanaToRomaji else ExerciseKind.RomajiToKana,
            items = listOf(item)
        )

        PracticeMode.Cross -> Exercise(
            kind = if (index % 4 == 3) ExerciseKind.TraceKana else ExerciseKind.KanaToRomaji,
            items = listOf(item)
        )

        PracticeMode.Mixed -> Exercise(
            kind = when (index % 5) {
                0 -> ExerciseKind.KanaToRomaji
                1 -> ExerciseKind.RomajiToKana
                2 -> if (supportsAudioPrompt(item)) ExerciseKind.SoundToKana else ExerciseKind.RomajiToKana
                3 -> ExerciseKind.TraceKana
                else -> ExerciseKind.KanaToRomaji
            },
            items = listOf(item)
        )
    }

fun correctAnswerFor(exercise: Exercise): String =
    when (exercise.kind) {
        ExerciseKind.KanaToRomaji -> exercise.items.first().romaji
        ExerciseKind.RomajiToKana -> exercise.items.first().kana
        ExerciseKind.SoundToKana -> exercise.items.first().kana
        ExerciseKind.PairMatch -> exercise.items.joinToString("  ") { "${it.kana} ${it.romaji}" }
        ExerciseKind.TraceKana -> "${exercise.items.first().kana} ${exercise.items.first().romaji}"
    }

fun romajiOptions(target: KanaItem, allItems: List<KanaItem>): List<String> =
    (listOf(target.romaji) + allItems.filter { it.row == target.row && it.id != target.id }.map { it.romaji }.take(3) +
        allItems.filter { it.id != target.id }.shuffled(Random(target.id.hashCode())).map { it.romaji }.take(3))
        .distinct()
        .take(4)
        .shuffled(Random(target.romaji.hashCode()))

fun kanaOptions(target: KanaItem, allItems: List<KanaItem>): List<String> =
    (listOf(target.kana) + target.confusable + allItems.filter { it.row == target.row && it.id != target.id }.map { it.kana }.take(3) +
        allItems.filter { it.id != target.id }.shuffled(Random(target.id.hashCode())).map { it.kana }.take(3))
        .distinct()
        .take(4)
        .shuffled(Random(target.kana.hashCode()))

fun lessonsFor(script: Script): List<KanaLesson> {
    val allItems = itemsFor(script)
    return allItems.groupBy { it.lesson }.toSortedMap().map { (index, items) ->
        val stage = lessonStage(index, items, allItems)
        KanaLesson(
            index = index,
            title = lessonTitle(index, stage, items),
            subtitle = lessonSubtitle(stage, items),
            stage = stage,
            difficulty = lessonDifficulty(stage),
            items = items
        )
    }
}

private fun lessonTitle(index: Int, stage: LearningStage, items: List<KanaItem>): String =
    when (stage) {
        LearningStage.Anchor -> "Vowels"
        LearningStage.RegularRows,
        LearningStage.ShapeHeavy,
        LearningStage.TailRows,
        LearningStage.Confusable -> "${rowLabel(items)} row"
        LearningStage.Voiced -> "Marks: ${rowLabel(items)} row"
        LearningStage.Combination -> "Blends: ${blendLabel(index, items)}"
        LearningStage.Special -> "Special marks"
    }

private fun lessonSubtitle(stage: LearningStage, items: List<KanaItem>): String =
    "${stage.description}: ${items.joinToString(" ") { it.romaji }}"

private fun rowLabel(items: List<KanaItem>): String =
    when (val row = items.firstOrNull()?.row.orEmpty()) {
        "vowels" -> "Vowel"
        "w" -> "W/N"
        "special" -> "Special"
        else -> row.uppercase()
    }

private fun blendLabel(index: Int, items: List<KanaItem>): String =
    when (index) {
        16 -> "K/S"
        17 -> "T/N"
        18 -> "H/M"
        19 -> "R/G"
        20 -> "J/B/P"
        else -> rowLabel(items)
    }

private fun lessonStage(index: Int, items: List<KanaItem>, allItems: List<KanaItem>): LearningStage =
    when {
        index == 1 -> LearningStage.Anchor
        index >= 21 -> LearningStage.Special
        index >= 16 -> LearningStage.Combination
        index >= 11 -> LearningStage.Voiced
        items.any { item -> item.confusable.any { kana -> (allItems.firstOrNull { it.kana == kana }?.lesson ?: Int.MAX_VALUE) <= index } } ->
            LearningStage.Confusable
        index in 2..5 -> LearningStage.RegularRows
        index in 6..8 -> LearningStage.ShapeHeavy
        else -> LearningStage.TailRows
    }

private fun lessonDifficulty(stage: LearningStage): Int =
    when (stage) {
        LearningStage.Anchor -> 1
        LearningStage.RegularRows -> 2
        LearningStage.ShapeHeavy,
        LearningStage.TailRows,
        LearningStage.Voiced,
        LearningStage.Combination,
        LearningStage.Special,
        LearningStage.Confusable -> 3
    }

fun lessonAverageMastery(lesson: KanaLesson, mastery: Map<String, Int>): Float =
    if (lesson.items.isEmpty()) 0f else lesson.items.sumOf { (mastery[it.id] ?: 0).toDouble() }.toFloat() / lesson.items.size

fun progressSnapshot(items: List<KanaItem>, mastery: Map<String, Int>): ProgressSnapshot =
    ProgressSnapshot(
        seen = items.count { (mastery[it.id] ?: 0) >= 1 },
        recall = items.count { (mastery[it.id] ?: 0) >= 2 },
        fluent = items.count { (mastery[it.id] ?: 0) >= 4 },
        total = items.size
    )

fun reviewCountFor(scriptItems: List<KanaItem>, mistakeIds: List<String>, mastery: Map<String, Int>): Int {
    val scriptItemIds = scriptItems.map { it.id }.toSet()
    return (
        mistakeIds.filter { it in scriptItemIds } +
            scriptItems.filter { (mastery[it.id] ?: 0) in 1..3 }.map { it.id }
        ).toSet().size
}

fun dueReviewItemsFor(
    scriptItems: List<KanaItem>,
    reviewDueEpochDays: Map<String, Long>,
    currentEpochDay: Long,
    mastery: Map<String, Int>
): List<KanaItem> =
    scriptItems.filter { item ->
        val dueDay = reviewDueEpochDays[item.id] ?: return@filter false
        (mastery[item.id] ?: 0) >= 2 && dueDay <= currentEpochDay
    }

fun dueReviewCountFor(
    scriptItems: List<KanaItem>,
    reviewDueEpochDays: Map<String, Long>,
    currentEpochDay: Long,
    mastery: Map<String, Int>
): Int =
    dueReviewItemsFor(scriptItems, reviewDueEpochDays, currentEpochDay, mastery).size

fun isLessonUnlocked(lesson: KanaLesson, lessons: List<KanaLesson>, mastery: Map<String, Int>): Boolean {
    val previous = lessons.lastOrNull { it.index == lesson.index - 1 }
    return previous == null || lessonAverageMastery(previous, mastery) >= 2f
}

fun lessonLockCopyFor(lesson: KanaLesson, lessons: List<KanaLesson>, mastery: Map<String, Int>): LessonLockCopy? {
    val previous = lessons.lastOrNull { it.index == lesson.index - 1 } ?: return null
    if (lessonAverageMastery(previous, mastery) >= 2f) return null
    return LessonLockCopy(message = "Need ${previous.title} recall 2")
}

fun pathStageProgressCopyFor(
    selectedStage: LearningStage?,
    lessons: List<KanaLesson>,
    mastery: Map<String, Int>
): StageProgressCopy {
    val stageLessons = selectedStage?.let { stage -> lessons.filter { it.stage == stage } } ?: lessons
    val fluentCount = stageLessons.count { lessonAverageMastery(it, mastery) >= 4f }
    val prefix = selectedStage?.label?.let { "$it " }.orEmpty()
    return StageProgressCopy(message = "$prefix$fluentCount/${stageLessons.size} fluent")
}

fun pathStageEmptyStateCopyFor(
    selectedStage: LearningStage?,
    lessons: List<KanaLesson>,
    mastery: Map<String, Int>
): StageEmptyStateCopy? {
    val stage = selectedStage ?: return null
    val stageLessons = lessons.filter { it.stage == stage }
    val hasActionableLesson = stageLessons.any { isLessonUnlocked(it, lessons, mastery) && lessonAverageMastery(it, mastery) < 4f }
    if (hasActionableLesson) return null
    val allFluent = stageLessons.isNotEmpty() && stageLessons.all { lessonAverageMastery(it, mastery) >= 4f }
    return if (allFluent) {
        StageEmptyStateCopy(
            title = "${stage.label} fluent",
            message = "This stage is complete. Clear the filter to continue the path.",
            actionLabel = "Show all"
        )
    } else {
        StageEmptyStateCopy(
            title = "No open ${stage.label.lowercase()} lessons",
            message = "Earlier rows still control this stage. Clear the filter to return to the next open lesson.",
            actionLabel = "Show all"
        )
    }
}

fun chartProgressCopyFor(
    selectedRow: String?,
    items: List<KanaItem>,
    mastery: Map<String, Int>
): ChartProgressCopy {
    val visibleItems = selectedRow?.let { row -> items.filter { it.row == row } } ?: items
    val fluentCount = visibleItems.count { (mastery[it.id] ?: 0) >= 4 }
    val label = selectedRow?.let { chartRowLabelFor(it) } ?: "All"
    return ChartProgressCopy(message = "$label $fluentCount/${visibleItems.size} fluent")
}

fun chartRowGuidanceCopyFor(
    selectedRow: String?,
    items: List<KanaItem>,
    mastery: Map<String, Int>
): ChartRowGuidanceCopy? {
    val row = selectedRow ?: return null
    val rowItems = items.filter { it.row == row }
    if (rowItems.isEmpty() || rowItems.any { (mastery[it.id] ?: 0) >= 4 }) return null
    val label = chartRowLabelFor(row).lowercase()
    return ChartRowGuidanceCopy(
        title = "No fluent $label yet",
        message = "Use this row as reference, then return to lessons when you want it to count as recall."
    )
}

fun chartContrastSummaryCopyFor(selectedRow: String?, items: List<KanaItem>): ChartContrastSummaryCopy? {
    val visibleItems = selectedRow?.let { row -> items.filter { it.row == row } } ?: items
    val contrastCount = visibleItems.count { it.confusable.isNotEmpty() }
    if (contrastCount == 0) return null
    val label = selectedRow?.let { chartRowLabelFor(it).lowercase() } ?: "chart"
    return ChartContrastSummaryCopy(
        title = "$contrastCount contrast kana",
        message = "Outlined $label tiles have known lookalikes; compare shape and stroke direction."
    )
}

fun chartRowLabelFor(row: String): String =
    when (row) {
        "vowels" -> "Vowels"
        "w" -> "W/N row"
        "special" -> "Special marks"
        else -> if (row.endsWith("-y")) {
            "${row.substringBefore("-").uppercase()} blends"
        } else {
            "${row.uppercase()} row"
        }
    }

fun chartCardTagFor(item: KanaItem): ChartCardTag? {
    if (item.row != "special") return null
    return ChartCardTag(
        label = when {
            item.romaji == "long mark" -> "long mark"
            item.romaji.startsWith("small ") -> "small"
            else -> "special"
        }
    )
}

fun chartTapFeedbackFor(item: KanaItem): ChartTapFeedback =
    ChartTapFeedback(
        title = "Audio cue: ${item.kana}",
        message = "${item.romaji} · ${chartRowLabelFor(item.row)}"
    )

fun chartMasteryCopyFor(level: Int): ChartMasteryCopy {
    val clamped = level.coerceIn(0, 5)
    val label = when (clamped) {
        0 -> "new"
        1 -> "familiar"
        2 -> "recall"
        3 -> "contrast"
        4 -> "fluent"
        else -> "mastered"
    }
    return ChartMasteryCopy("$clamped/5 $label")
}

fun chartLegendCopyFor(): ChartLegendCopy =
    ChartLegendCopy("Fluent is stable recall; mastered is long-spaced maintenance.")

fun nextPathLesson(lessons: List<KanaLesson>, mastery: Map<String, Int>): KanaLesson? =
    lessons.firstOrNull { lesson ->
        isLessonUnlocked(lesson, lessons, mastery) && lessonAverageMastery(lesson, mastery) < 4f
    }

fun masteryLabel(averageMastery: Float): String =
    when {
        averageMastery >= 4f -> "fluent"
        averageMastery >= 3f -> "contrast"
        averageMastery >= 2f -> "recall"
        averageMastery >= 1f -> "familiar"
        else -> "new"
    }

fun itemsFor(script: Script): List<KanaItem> =
    if (script == Script.Hiragana) hiraganaItems else katakanaItems

fun lessonSeed(items: List<KanaItem>): Int =
    items.joinToString("") { it.id }.hashCode()

private fun kana(
    script: Script,
    kana: String,
    romaji: String,
    row: String,
    lesson: Int,
    confusable: List<String> = emptyList(),
    idSuffix: String = romaji
) = KanaItem("${script.name.lowercase()}-$idSuffix", script, kana, romaji, row, lesson, confusable)

val hiraganaItems = listOf(
    kana(Script.Hiragana, "あ", "a", "vowels", 1),
    kana(Script.Hiragana, "い", "i", "vowels", 1),
    kana(Script.Hiragana, "う", "u", "vowels", 1),
    kana(Script.Hiragana, "え", "e", "vowels", 1),
    kana(Script.Hiragana, "お", "o", "vowels", 1),
    kana(Script.Hiragana, "か", "ka", "k", 2),
    kana(Script.Hiragana, "き", "ki", "k", 2),
    kana(Script.Hiragana, "く", "ku", "k", 2),
    kana(Script.Hiragana, "け", "ke", "k", 2),
    kana(Script.Hiragana, "こ", "ko", "k", 2),
    kana(Script.Hiragana, "さ", "sa", "s", 3, listOf("ち")),
    kana(Script.Hiragana, "し", "shi", "s", 3),
    kana(Script.Hiragana, "す", "su", "s", 3),
    kana(Script.Hiragana, "せ", "se", "s", 3),
    kana(Script.Hiragana, "そ", "so", "s", 3),
    kana(Script.Hiragana, "た", "ta", "t", 4),
    kana(Script.Hiragana, "ち", "chi", "t", 4, listOf("さ")),
    kana(Script.Hiragana, "つ", "tsu", "t", 4),
    kana(Script.Hiragana, "て", "te", "t", 4),
    kana(Script.Hiragana, "と", "to", "t", 4),
    kana(Script.Hiragana, "な", "na", "n", 5),
    kana(Script.Hiragana, "に", "ni", "n", 5),
    kana(Script.Hiragana, "ぬ", "nu", "n", 5, listOf("め")),
    kana(Script.Hiragana, "ね", "ne", "n", 5),
    kana(Script.Hiragana, "の", "no", "n", 5),
    kana(Script.Hiragana, "は", "ha", "h", 6),
    kana(Script.Hiragana, "ひ", "hi", "h", 6),
    kana(Script.Hiragana, "ふ", "fu", "h", 6),
    kana(Script.Hiragana, "へ", "he", "h", 6),
    kana(Script.Hiragana, "ほ", "ho", "h", 6),
    kana(Script.Hiragana, "ま", "ma", "m", 7),
    kana(Script.Hiragana, "み", "mi", "m", 7),
    kana(Script.Hiragana, "む", "mu", "m", 7),
    kana(Script.Hiragana, "め", "me", "m", 7, listOf("ぬ")),
    kana(Script.Hiragana, "も", "mo", "m", 7),
    kana(Script.Hiragana, "や", "ya", "y", 8),
    kana(Script.Hiragana, "ゆ", "yu", "y", 8),
    kana(Script.Hiragana, "よ", "yo", "y", 8),
    kana(Script.Hiragana, "ら", "ra", "r", 9),
    kana(Script.Hiragana, "り", "ri", "r", 9),
    kana(Script.Hiragana, "る", "ru", "r", 9),
    kana(Script.Hiragana, "れ", "re", "r", 9),
    kana(Script.Hiragana, "ろ", "ro", "r", 9),
    kana(Script.Hiragana, "わ", "wa", "w", 10),
    kana(Script.Hiragana, "を", "wo", "w", 10),
    kana(Script.Hiragana, "ん", "n", "w", 10),
    kana(Script.Hiragana, "が", "ga", "g", 11),
    kana(Script.Hiragana, "ぎ", "gi", "g", 11),
    kana(Script.Hiragana, "ぐ", "gu", "g", 11),
    kana(Script.Hiragana, "げ", "ge", "g", 11),
    kana(Script.Hiragana, "ご", "go", "g", 11),
    kana(Script.Hiragana, "ざ", "za", "z", 12),
    kana(Script.Hiragana, "じ", "ji", "z", 12, listOf("ぢ")),
    kana(Script.Hiragana, "ず", "zu", "z", 12, listOf("づ")),
    kana(Script.Hiragana, "ぜ", "ze", "z", 12),
    kana(Script.Hiragana, "ぞ", "zo", "z", 12),
    kana(Script.Hiragana, "だ", "da", "d", 13),
    kana(Script.Hiragana, "ぢ", "ji", "d", 13, listOf("じ"), idSuffix = "di"),
    kana(Script.Hiragana, "づ", "zu", "d", 13, listOf("ず"), idSuffix = "du"),
    kana(Script.Hiragana, "で", "de", "d", 13),
    kana(Script.Hiragana, "ど", "do", "d", 13),
    kana(Script.Hiragana, "ば", "ba", "b", 14),
    kana(Script.Hiragana, "び", "bi", "b", 14),
    kana(Script.Hiragana, "ぶ", "bu", "b", 14),
    kana(Script.Hiragana, "べ", "be", "b", 14),
    kana(Script.Hiragana, "ぼ", "bo", "b", 14),
    kana(Script.Hiragana, "ぱ", "pa", "p", 15),
    kana(Script.Hiragana, "ぴ", "pi", "p", 15),
    kana(Script.Hiragana, "ぷ", "pu", "p", 15),
    kana(Script.Hiragana, "ぺ", "pe", "p", 15),
    kana(Script.Hiragana, "ぽ", "po", "p", 15),
    kana(Script.Hiragana, "きゃ", "kya", "k-y", 16),
    kana(Script.Hiragana, "きゅ", "kyu", "k-y", 16),
    kana(Script.Hiragana, "きょ", "kyo", "k-y", 16),
    kana(Script.Hiragana, "しゃ", "sha", "s-y", 16),
    kana(Script.Hiragana, "しゅ", "shu", "s-y", 16),
    kana(Script.Hiragana, "しょ", "sho", "s-y", 16),
    kana(Script.Hiragana, "ちゃ", "cha", "t-y", 17),
    kana(Script.Hiragana, "ちゅ", "chu", "t-y", 17),
    kana(Script.Hiragana, "ちょ", "cho", "t-y", 17),
    kana(Script.Hiragana, "にゃ", "nya", "n-y", 17),
    kana(Script.Hiragana, "にゅ", "nyu", "n-y", 17),
    kana(Script.Hiragana, "にょ", "nyo", "n-y", 17),
    kana(Script.Hiragana, "ひゃ", "hya", "h-y", 18),
    kana(Script.Hiragana, "ひゅ", "hyu", "h-y", 18),
    kana(Script.Hiragana, "ひょ", "hyo", "h-y", 18),
    kana(Script.Hiragana, "みゃ", "mya", "m-y", 18),
    kana(Script.Hiragana, "みゅ", "myu", "m-y", 18),
    kana(Script.Hiragana, "みょ", "myo", "m-y", 18),
    kana(Script.Hiragana, "りゃ", "rya", "r-y", 19),
    kana(Script.Hiragana, "りゅ", "ryu", "r-y", 19),
    kana(Script.Hiragana, "りょ", "ryo", "r-y", 19),
    kana(Script.Hiragana, "ぎゃ", "gya", "g-y", 19),
    kana(Script.Hiragana, "ぎゅ", "gyu", "g-y", 19),
    kana(Script.Hiragana, "ぎょ", "gyo", "g-y", 19),
    kana(Script.Hiragana, "じゃ", "ja", "j-y", 20),
    kana(Script.Hiragana, "じゅ", "ju", "j-y", 20),
    kana(Script.Hiragana, "じょ", "jo", "j-y", 20),
    kana(Script.Hiragana, "びゃ", "bya", "b-y", 20),
    kana(Script.Hiragana, "びゅ", "byu", "b-y", 20),
    kana(Script.Hiragana, "びょ", "byo", "b-y", 20),
    kana(Script.Hiragana, "ぴゃ", "pya", "p-y", 20),
    kana(Script.Hiragana, "ぴゅ", "pyu", "p-y", 20),
    kana(Script.Hiragana, "ぴょ", "pyo", "p-y", 20),
    kana(Script.Hiragana, "っ", "small tsu", "special", 21, idSuffix = "small-tsu"),
    kana(Script.Hiragana, "ゃ", "small ya", "special", 21, idSuffix = "small-ya"),
    kana(Script.Hiragana, "ゅ", "small yu", "special", 21, idSuffix = "small-yu"),
    kana(Script.Hiragana, "ょ", "small yo", "special", 21, idSuffix = "small-yo")
)

val katakanaItems = listOf(
    kana(Script.Katakana, "ア", "a", "vowels", 1),
    kana(Script.Katakana, "イ", "i", "vowels", 1),
    kana(Script.Katakana, "ウ", "u", "vowels", 1),
    kana(Script.Katakana, "エ", "e", "vowels", 1),
    kana(Script.Katakana, "オ", "o", "vowels", 1),
    kana(Script.Katakana, "カ", "ka", "k", 2),
    kana(Script.Katakana, "キ", "ki", "k", 2),
    kana(Script.Katakana, "ク", "ku", "k", 2),
    kana(Script.Katakana, "ケ", "ke", "k", 2),
    kana(Script.Katakana, "コ", "ko", "k", 2),
    kana(Script.Katakana, "サ", "sa", "s", 3),
    kana(Script.Katakana, "シ", "shi", "s", 3, listOf("ツ", "ン")),
    kana(Script.Katakana, "ス", "su", "s", 3),
    kana(Script.Katakana, "セ", "se", "s", 3),
    kana(Script.Katakana, "ソ", "so", "s", 3, listOf("ン")),
    kana(Script.Katakana, "タ", "ta", "t", 4),
    kana(Script.Katakana, "チ", "chi", "t", 4),
    kana(Script.Katakana, "ツ", "tsu", "t", 4, listOf("シ")),
    kana(Script.Katakana, "テ", "te", "t", 4),
    kana(Script.Katakana, "ト", "to", "t", 4),
    kana(Script.Katakana, "ナ", "na", "n", 5),
    kana(Script.Katakana, "ニ", "ni", "n", 5),
    kana(Script.Katakana, "ヌ", "nu", "n", 5),
    kana(Script.Katakana, "ネ", "ne", "n", 5),
    kana(Script.Katakana, "ノ", "no", "n", 5),
    kana(Script.Katakana, "ハ", "ha", "h", 6),
    kana(Script.Katakana, "ヒ", "hi", "h", 6),
    kana(Script.Katakana, "フ", "fu", "h", 6),
    kana(Script.Katakana, "ヘ", "he", "h", 6),
    kana(Script.Katakana, "ホ", "ho", "h", 6),
    kana(Script.Katakana, "マ", "ma", "m", 7),
    kana(Script.Katakana, "ミ", "mi", "m", 7),
    kana(Script.Katakana, "ム", "mu", "m", 7),
    kana(Script.Katakana, "メ", "me", "m", 7),
    kana(Script.Katakana, "モ", "mo", "m", 7),
    kana(Script.Katakana, "ヤ", "ya", "y", 8),
    kana(Script.Katakana, "ユ", "yu", "y", 8),
    kana(Script.Katakana, "ヨ", "yo", "y", 8),
    kana(Script.Katakana, "ラ", "ra", "r", 9),
    kana(Script.Katakana, "リ", "ri", "r", 9),
    kana(Script.Katakana, "ル", "ru", "r", 9),
    kana(Script.Katakana, "レ", "re", "r", 9),
    kana(Script.Katakana, "ロ", "ro", "r", 9),
    kana(Script.Katakana, "ワ", "wa", "w", 10),
    kana(Script.Katakana, "ヲ", "wo", "w", 10),
    kana(Script.Katakana, "ン", "n", "w", 10, listOf("ソ", "シ")),
    kana(Script.Katakana, "ガ", "ga", "g", 11),
    kana(Script.Katakana, "ギ", "gi", "g", 11),
    kana(Script.Katakana, "グ", "gu", "g", 11),
    kana(Script.Katakana, "ゲ", "ge", "g", 11),
    kana(Script.Katakana, "ゴ", "go", "g", 11),
    kana(Script.Katakana, "ザ", "za", "z", 12),
    kana(Script.Katakana, "ジ", "ji", "z", 12, listOf("ヂ")),
    kana(Script.Katakana, "ズ", "zu", "z", 12, listOf("ヅ")),
    kana(Script.Katakana, "ゼ", "ze", "z", 12),
    kana(Script.Katakana, "ゾ", "zo", "z", 12),
    kana(Script.Katakana, "ダ", "da", "d", 13),
    kana(Script.Katakana, "ヂ", "ji", "d", 13, listOf("ジ"), idSuffix = "di"),
    kana(Script.Katakana, "ヅ", "zu", "d", 13, listOf("ズ"), idSuffix = "du"),
    kana(Script.Katakana, "デ", "de", "d", 13),
    kana(Script.Katakana, "ド", "do", "d", 13),
    kana(Script.Katakana, "バ", "ba", "b", 14),
    kana(Script.Katakana, "ビ", "bi", "b", 14),
    kana(Script.Katakana, "ブ", "bu", "b", 14),
    kana(Script.Katakana, "ベ", "be", "b", 14),
    kana(Script.Katakana, "ボ", "bo", "b", 14),
    kana(Script.Katakana, "パ", "pa", "p", 15),
    kana(Script.Katakana, "ピ", "pi", "p", 15),
    kana(Script.Katakana, "プ", "pu", "p", 15),
    kana(Script.Katakana, "ペ", "pe", "p", 15),
    kana(Script.Katakana, "ポ", "po", "p", 15),
    kana(Script.Katakana, "キャ", "kya", "k-y", 16),
    kana(Script.Katakana, "キュ", "kyu", "k-y", 16),
    kana(Script.Katakana, "キョ", "kyo", "k-y", 16),
    kana(Script.Katakana, "シャ", "sha", "s-y", 16),
    kana(Script.Katakana, "シュ", "shu", "s-y", 16),
    kana(Script.Katakana, "ショ", "sho", "s-y", 16),
    kana(Script.Katakana, "チャ", "cha", "t-y", 17),
    kana(Script.Katakana, "チュ", "chu", "t-y", 17),
    kana(Script.Katakana, "チョ", "cho", "t-y", 17),
    kana(Script.Katakana, "ニャ", "nya", "n-y", 17),
    kana(Script.Katakana, "ニュ", "nyu", "n-y", 17),
    kana(Script.Katakana, "ニョ", "nyo", "n-y", 17),
    kana(Script.Katakana, "ヒャ", "hya", "h-y", 18),
    kana(Script.Katakana, "ヒュ", "hyu", "h-y", 18),
    kana(Script.Katakana, "ヒョ", "hyo", "h-y", 18),
    kana(Script.Katakana, "ミャ", "mya", "m-y", 18),
    kana(Script.Katakana, "ミュ", "myu", "m-y", 18),
    kana(Script.Katakana, "ミョ", "myo", "m-y", 18),
    kana(Script.Katakana, "リャ", "rya", "r-y", 19),
    kana(Script.Katakana, "リュ", "ryu", "r-y", 19),
    kana(Script.Katakana, "リョ", "ryo", "r-y", 19),
    kana(Script.Katakana, "ギャ", "gya", "g-y", 19),
    kana(Script.Katakana, "ギュ", "gyu", "g-y", 19),
    kana(Script.Katakana, "ギョ", "gyo", "g-y", 19),
    kana(Script.Katakana, "ジャ", "ja", "j-y", 20),
    kana(Script.Katakana, "ジュ", "ju", "j-y", 20),
    kana(Script.Katakana, "ジョ", "jo", "j-y", 20),
    kana(Script.Katakana, "ビャ", "bya", "b-y", 20),
    kana(Script.Katakana, "ビュ", "byu", "b-y", 20),
    kana(Script.Katakana, "ビョ", "byo", "b-y", 20),
    kana(Script.Katakana, "ピャ", "pya", "p-y", 20),
    kana(Script.Katakana, "ピュ", "pyu", "p-y", 20),
    kana(Script.Katakana, "ピョ", "pyo", "p-y", 20),
    kana(Script.Katakana, "ッ", "small tsu", "special", 21, idSuffix = "small-tsu"),
    kana(Script.Katakana, "ー", "long mark", "special", 21, idSuffix = "long-mark"),
    kana(Script.Katakana, "ァ", "small a", "special", 21, idSuffix = "small-a"),
    kana(Script.Katakana, "ィ", "small i", "special", 21, idSuffix = "small-i"),
    kana(Script.Katakana, "ゥ", "small u", "special", 21, idSuffix = "small-u"),
    kana(Script.Katakana, "ェ", "small e", "special", 21, idSuffix = "small-e"),
    kana(Script.Katakana, "ォ", "small o", "special", 21, idSuffix = "small-o"),
    kana(Script.Katakana, "ャ", "small ya", "special", 21, idSuffix = "small-ya"),
    kana(Script.Katakana, "ュ", "small yu", "special", 21, idSuffix = "small-yu"),
    kana(Script.Katakana, "ョ", "small yo", "special", 21, idSuffix = "small-yo")
)
