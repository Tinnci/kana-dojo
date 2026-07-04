package dev.tinnci.kanadojo

import kotlin.random.Random

fun buildLessonExercises(lesson: KanaLesson): List<Exercise> {
    val intro = lesson.items.flatMap {
        listOf(
            Exercise(ExerciseKind.RomajiToKana, listOf(it)),
            Exercise(ExerciseKind.KanaToRomaji, listOf(it)),
            Exercise(ExerciseKind.SoundToKana, listOf(it))
        )
    }
    val pairs = lesson.items.chunked(4).map { Exercise(ExerciseKind.PairMatch, it) }
    val writingCount = when (lesson.stage) {
        LearningStage.Anchor -> 2
        LearningStage.RegularRows -> 3
        LearningStage.ShapeHeavy -> 4
        LearningStage.TailRows -> lesson.items.size
        LearningStage.Confusable -> lesson.items.size
    }
    val writing = lesson.items.take(writingCount).map { Exercise(ExerciseKind.TraceKana, listOf(it)) }
    val contrast = lesson.items
        .filter { it.confusable.isNotEmpty() }
        .flatMap { listOf(Exercise(ExerciseKind.RomajiToKana, listOf(it)), Exercise(ExerciseKind.TraceKana, listOf(it))) }
    return (intro + pairs + writing + contrast).shuffled(Random(lesson.index))
}

fun buildMistakeExercise(item: KanaItem): Exercise =
    Exercise(
        kind = listOf(ExerciseKind.RomajiToKana, ExerciseKind.KanaToRomaji, ExerciseKind.SoundToKana, ExerciseKind.TraceKana)
            .random(Random(item.id.hashCode())),
        items = listOf(item)
    )

fun practiceItemsFor(
    mode: PracticeMode,
    scriptItems: List<KanaItem>,
    mistakeIds: List<String>,
    allItems: List<KanaItem>,
    mastery: Map<String, Int>
): List<KanaItem> {
    val byId = allItems.associateBy { it.id }
    return when (mode) {
        PracticeMode.Weak -> mistakeIds
            .mapNotNull { byId[it] }
            .filter { it.script == scriptItems.firstOrNull()?.script }
            .ifEmpty { scriptItems.sortedBy { mastery[it.id] ?: 0 }.take(6) }

        PracticeMode.Contrast -> scriptItems
            .filter { it.confusable.isNotEmpty() }
            .ifEmpty { scriptItems.sortedBy { mastery[it.id] ?: 0 }.take(6) }

        PracticeMode.Sound -> scriptItems
            .filter { (mastery[it.id] ?: 0) >= 1 }
            .ifEmpty { scriptItems.take(6) }

        PracticeMode.Writing -> scriptItems
            .sortedWith(compareBy<KanaItem> { mastery[it.id] ?: 0 }.thenBy { it.lesson })
            .take(8)

        PracticeMode.Speed -> scriptItems
            .filter { (mastery[it.id] ?: 0) >= 2 }
            .ifEmpty { scriptItems.take(8) }
            .shuffled(Random(scriptItems.firstOrNull()?.script?.name.hashCode() + 17))

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

        PracticeMode.Sound -> Exercise(ExerciseKind.SoundToKana, listOf(item))
        PracticeMode.Writing -> Exercise(ExerciseKind.TraceKana, listOf(item))
        PracticeMode.Speed -> Exercise(
            kind = if (index % 2 == 0) ExerciseKind.KanaToRomaji else ExerciseKind.RomajiToKana,
            items = listOf(item)
        )

        PracticeMode.Mixed -> Exercise(
            kind = when (index % 5) {
                0 -> ExerciseKind.KanaToRomaji
                1 -> ExerciseKind.RomajiToKana
                2 -> ExerciseKind.SoundToKana
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
            title = "Lesson $index",
            subtitle = "${stage.description}: ${items.joinToString(" ") { it.romaji }}",
            stage = stage,
            difficulty = lessonDifficulty(stage),
            items = items
        )
    }
}

private fun lessonStage(index: Int, items: List<KanaItem>, allItems: List<KanaItem>): LearningStage =
    when {
        index == 1 -> LearningStage.Anchor
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

fun isLessonUnlocked(lesson: KanaLesson, lessons: List<KanaLesson>, mastery: Map<String, Int>): Boolean {
    val previous = lessons.lastOrNull { it.index == lesson.index - 1 }
    return previous == null || lessonAverageMastery(previous, mastery) >= 2f
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
    confusable: List<String> = emptyList()
) = KanaItem("${script.name.lowercase()}-$romaji", script, kana, romaji, row, lesson, confusable)

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
    kana(Script.Hiragana, "ん", "n", "w", 10)
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
    kana(Script.Katakana, "ン", "n", "w", 10, listOf("ソ", "シ"))
)
