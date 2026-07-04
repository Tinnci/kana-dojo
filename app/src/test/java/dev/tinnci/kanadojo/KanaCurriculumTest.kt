package dev.tinnci.kanadojo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KanaCurriculumTest {
    @Test
    fun lessonsFollowExpectedBaseProgression() {
        val hiraganaLessons = lessonsFor(Script.Hiragana)
        val katakanaLessons = lessonsFor(Script.Katakana)

        assertEquals(21, hiraganaLessons.size)
        assertEquals(21, katakanaLessons.size)
        assertEquals(LearningStage.Anchor, hiraganaLessons[0].stage)
        assertEquals(LearningStage.RegularRows, hiraganaLessons[1].stage)
        assertEquals(LearningStage.Confusable, hiraganaLessons[3].stage)
        assertEquals(LearningStage.Confusable, katakanaLessons[3].stage)
        assertEquals(LearningStage.Voiced, hiraganaLessons[10].stage)
        assertEquals(LearningStage.Voiced, katakanaLessons[14].stage)
        assertEquals(LearningStage.Combination, hiraganaLessons[15].stage)
        assertEquals(LearningStage.Combination, katakanaLessons[19].stage)
        assertEquals(LearningStage.Special, hiraganaLessons[20].stage)
        assertEquals(LearningStage.Special, katakanaLessons[20].stage)
        assertEquals(listOf("a", "i", "u", "e", "o"), hiraganaLessons[0].items.map { it.romaji })
    }

    @Test
    fun kanaIdsAreUniqueAcrossDuplicateRomaji() {
        val allItems = hiraganaItems + katakanaItems

        assertEquals(allItems.size, allItems.map { it.id }.toSet().size)
        assertTrue(hiraganaItems.any { it.kana == "ぢ" && it.romaji == "ji" })
        assertTrue(hiraganaItems.any { it.kana == "づ" && it.romaji == "zu" })
        assertTrue(katakanaItems.any { it.kana == "ヂ" && it.romaji == "ji" })
        assertTrue(katakanaItems.any { it.kana == "ヅ" && it.romaji == "zu" })
    }

    @Test
    fun lessonsUseReadableProgressionTitles() {
        val lessons = lessonsFor(Script.Hiragana)

        assertEquals("Vowels", lessons[0].title)
        assertEquals("K row", lessons[1].title)
        assertEquals("Marks: G row", lessons[10].title)
        assertEquals("Blends: K/S", lessons[15].title)
        assertEquals("Special marks", lessons[20].title)
    }

    @Test
    fun voicedKanaLessonsCoverDakutenAndHandakuten() {
        val hiraganaLessons = lessonsFor(Script.Hiragana)
        val voicedKana = hiraganaLessons.slice(10..14).flatMap { it.items }.map { it.kana }.toSet()

        assertTrue(setOf("が", "ざ", "だ", "ば", "ぱ").all { it in voicedKana })
        assertTrue(hiraganaLessons.slice(10..14).all { it.stage == LearningStage.Voiced })
    }

    @Test
    fun combinationLessonsCoverSmallYaYuYoBlends() {
        val hiraganaLessons = lessonsFor(Script.Hiragana)
        val katakanaLessons = lessonsFor(Script.Katakana)
        val hiraganaBlends = hiraganaLessons.slice(15..19).flatMap { it.items }.map { it.kana }.toSet()
        val katakanaBlends = katakanaLessons.slice(15..19).flatMap { it.items }.map { it.kana }.toSet()

        assertTrue(setOf("きゃ", "しゃ", "ちゃ", "にゃ", "ひゃ", "みゃ", "りゃ", "ぎゃ", "じゃ", "びゃ", "ぴゃ").all { it in hiraganaBlends })
        assertTrue(setOf("キャ", "シャ", "チャ", "ニャ", "ヒャ", "ミャ", "リャ", "ギャ", "ジャ", "ビャ", "ピャ").all { it in katakanaBlends })
        assertTrue(hiraganaLessons.slice(15..19).all { it.stage == LearningStage.Combination })
    }

    @Test
    fun specialLessonsCoverSmallKanaAndLengthMark() {
        val hiraganaSpecial = lessonsFor(Script.Hiragana).last()
        val katakanaSpecial = lessonsFor(Script.Katakana).last()

        assertEquals(LearningStage.Special, hiraganaSpecial.stage)
        assertEquals(LearningStage.Special, katakanaSpecial.stage)
        assertTrue(setOf("っ", "ゃ", "ゅ", "ょ").all { it in hiraganaSpecial.items.map { item -> item.kana } })
        assertTrue(setOf("ッ", "ー", "ァ", "ィ", "ゥ", "ェ", "ォ", "ャ", "ュ", "ョ").all { it in katakanaSpecial.items.map { item -> item.kana } })
        assertTrue((hiraganaSpecial.items + katakanaSpecial.items).none { supportsAudioPrompt(it) })
    }

    @Test
    fun nextLessonUnlocksAtRecallLevelTwo() {
        val lessons = lessonsFor(Script.Hiragana)
        val lessonOne = lessons[0]
        val lessonTwo = lessons[1]

        assertTrue(isLessonUnlocked(lessonOne, lessons, emptyMap()))
        assertFalse(isLessonUnlocked(lessonTwo, lessons, emptyMap()))

        val familiarOnly = lessonOne.items.associate { it.id to 1 }
        val recallReady = lessonOne.items.associate { it.id to 2 }

        assertFalse(isLessonUnlocked(lessonTwo, lessons, familiarOnly))
        assertTrue(isLessonUnlocked(lessonTwo, lessons, recallReady))
    }

    @Test
    fun nextPathLessonReturnsFirstUnfinishedUnlockedLesson() {
        val lessons = lessonsFor(Script.Hiragana)
        val lessonOneFluent = lessons[0].items.associate { it.id to 4 }

        val next = nextPathLesson(lessons, lessonOneFluent)

        assertEquals(lessons[1].index, next?.index)
    }

    @Test
    fun nextPathLessonDoesNotSkipLockedLessons() {
        val lessons = lessonsFor(Script.Hiragana)
        val lessonOneFamiliar = lessons[0].items.associate { it.id to 1 }

        val next = nextPathLesson(lessons, lessonOneFamiliar)

        assertEquals(lessons[0].index, next?.index)
    }

    @Test
    fun progressSnapshotCountsThresholds() {
        val items = itemsFor(Script.Hiragana).take(5)
        val mastery = mapOf(
            items[0].id to 1,
            items[1].id to 2,
            items[2].id to 4,
            items[3].id to 5
        )

        val snapshot = progressSnapshot(items, mastery)

        assertEquals(4, snapshot.seen)
        assertEquals(3, snapshot.recall)
        assertEquals(2, snapshot.fluent)
        assertEquals(5, snapshot.total)
        assertEquals(0.4f, snapshot.overall, 0.001f)
    }

    @Test
    fun reviewCountCombinesMistakesAndLearningKanaWithoutDuplicates() {
        val hiraganaItems = itemsFor(Script.Hiragana)
        val katakanaMistake = itemsFor(Script.Katakana).first()
        val weak = hiraganaItems[0]
        val learning = hiraganaItems[1]
        val fluent = hiraganaItems[2]

        val count = reviewCountFor(
            scriptItems = hiraganaItems,
            mistakeIds = listOf(weak.id, katakanaMistake.id),
            mastery = mapOf(
                weak.id to 1,
                learning.id to 3,
                fluent.id to 4
            )
        )

        assertEquals(2, count)
    }

    @Test
    fun weakPracticeFiltersMistakesToSelectedScript() {
        val allItems = hiraganaItems + katakanaItems
        val hiraganaMistake = hiraganaItems.first { it.romaji == "a" }
        val katakanaMistake = katakanaItems.first { it.romaji == "a" }

        val weakItems = practiceItemsFor(
            mode = PracticeMode.Weak,
            scriptItems = itemsFor(Script.Hiragana),
            mistakeIds = listOf(hiraganaMistake.id, katakanaMistake.id),
            allItems = allItems,
            mastery = emptyMap()
        )

        assertEquals(listOf(hiraganaMistake.id), weakItems.map { it.id })
    }

    @Test
    fun weakPracticeUsesCurrentMistakeList() {
        val allItems = hiraganaItems + katakanaItems
        val firstMistake = hiraganaItems.first { it.romaji == "a" }
        val nextMistake = hiraganaItems.first { it.romaji == "i" }

        val firstQueue = practiceItemsFor(
            mode = PracticeMode.Weak,
            scriptItems = itemsFor(Script.Hiragana),
            mistakeIds = listOf(firstMistake.id),
            allItems = allItems,
            mastery = emptyMap()
        )
        val nextQueue = practiceItemsFor(
            mode = PracticeMode.Weak,
            scriptItems = itemsFor(Script.Hiragana),
            mistakeIds = listOf(nextMistake.id),
            allItems = allItems,
            mastery = emptyMap()
        )

        assertEquals(listOf(firstMistake.id), firstQueue.map { it.id })
        assertEquals(listOf(nextMistake.id), nextQueue.map { it.id })
    }

    @Test
    fun weakPracticeFallsBackToLowestMasteryKanaWhenNoMistakesExist() {
        val scriptItems = itemsFor(Script.Hiragana)
        val low = scriptItems[3]
        val lower = scriptItems[4]
        val mastery = scriptItems.associate { item ->
            item.id to when (item.id) {
                low.id -> 1
                lower.id -> 0
                else -> 5
            }
        }

        val weakItems = practiceItemsFor(
            mode = PracticeMode.Weak,
            scriptItems = scriptItems,
            mistakeIds = emptyList(),
            allItems = hiraganaItems + katakanaItems,
            mastery = mastery
        )

        assertEquals(listOf(lower.id, low.id), weakItems.take(2).map { it.id })
    }

    @Test
    fun contrastPracticeTargetsConfusableKana() {
        val contrastItems = practiceItemsFor(
            mode = PracticeMode.Contrast,
            scriptItems = itemsFor(Script.Katakana),
            mistakeIds = emptyList(),
            allItems = hiraganaItems + katakanaItems,
            mastery = emptyMap()
        )

        assertTrue(contrastItems.isNotEmpty())
        assertTrue(contrastItems.all { it.confusable.isNotEmpty() })
        assertTrue(contrastItems.any { it.kana == "シ" })
        assertTrue(contrastItems.any { it.kana == "ン" })
    }

    @Test
    fun kanaOptionsIncludeConfusableChoices() {
        val target = katakanaItems.first { it.kana == "シ" }
        val options = kanaOptions(target, hiraganaItems + katakanaItems)

        assertTrue("ツ" in options)
    }

    @Test
    fun mixedPracticeUsesRecallReadyKanaWhenAvailable() {
        val scriptItems = itemsFor(Script.Hiragana)
        val recallReady = scriptItems.take(3).associate { it.id to 2 }

        val mixedItems = practiceItemsFor(
            mode = PracticeMode.Mixed,
            scriptItems = scriptItems,
            mistakeIds = emptyList(),
            allItems = hiraganaItems + katakanaItems,
            mastery = recallReady
        )

        assertEquals(recallReady.keys.toSet(), mixedItems.map { it.id }.toSet())
    }

    @Test
    fun lessonsIncludeSoundRecallExercises() {
        val lesson = lessonsFor(Script.Hiragana).first()
        val exercises = buildLessonExercises(lesson)

        assertTrue(exercises.any { it.kind == ExerciseKind.SoundToKana })
    }

    @Test
    fun specialLessonsDoNotUseStandaloneSoundPrompts() {
        val lesson = lessonsFor(Script.Katakana).last()
        val exercises = buildLessonExercises(lesson)

        assertFalse(exercises.any { it.kind == ExerciseKind.SoundToKana })
        assertTrue(exercises.any { it.kind == ExerciseKind.TraceKana })
    }

    @Test
    fun focusedPracticeModesUseDistinctExerciseKinds() {
        val item = itemsFor(Script.Katakana).first()

        assertEquals(ExerciseKind.SoundToKana, practiceExerciseFor(item, PracticeMode.Sound, 0).kind)
        assertEquals(ExerciseKind.TraceKana, practiceExerciseFor(item, PracticeMode.Writing, 0).kind)
        assertEquals(ExerciseKind.KanaToRomaji, practiceExerciseFor(item, PracticeMode.Speed, 0).kind)
        assertEquals(ExerciseKind.RomajiToKana, practiceExerciseFor(item, PracticeMode.Speed, 1).kind)
        assertEquals(ExerciseKind.KanaToRomaji, practiceExerciseFor(item, PracticeMode.Cross, 0).kind)
        assertEquals(ExerciseKind.TraceKana, practiceExerciseFor(item, PracticeMode.Cross, 3).kind)
    }

    @Test
    fun soundPracticePrefersSeenKana() {
        val scriptItems = itemsFor(Script.Hiragana)
        val seen = scriptItems.drop(2).take(2).associate { it.id to 1 }

        val soundItems = practiceItemsFor(
            mode = PracticeMode.Sound,
            scriptItems = scriptItems,
            mistakeIds = emptyList(),
            allItems = hiraganaItems + katakanaItems,
            mastery = seen
        )

        assertEquals(seen.keys.toSet(), soundItems.map { it.id }.toSet())
    }

    @Test
    fun soundPracticeFallbackExcludesStandaloneSpecialMarks() {
        val soundItems = practiceItemsFor(
            mode = PracticeMode.Sound,
            scriptItems = itemsFor(Script.Katakana),
            mistakeIds = emptyList(),
            allItems = hiraganaItems + katakanaItems,
            mastery = emptyMap()
        )

        assertTrue(soundItems.isNotEmpty())
        assertTrue(soundItems.all { supportsAudioPrompt(it) })
    }

    @Test
    fun crossPracticeMixesBothScripts() {
        val allItems = hiraganaItems + katakanaItems
        val mastery = mapOf(
            hiraganaItems.first { it.romaji == "a" }.id to 2,
            katakanaItems.first { it.romaji == "a" }.id to 2
        )

        val crossItems = practiceItemsFor(
            mode = PracticeMode.Cross,
            scriptItems = itemsFor(Script.Hiragana),
            mistakeIds = emptyList(),
            allItems = allItems,
            mastery = mastery
        )

        assertTrue(crossItems.any { it.script == Script.Hiragana })
        assertTrue(crossItems.any { it.script == Script.Katakana })
    }

    @Test
    fun completionRecommendationReturnsToPathForCleanRuns() {
        val recommendation = completionRecommendationFor(LessonSessionStats(correct = 8, missed = 0))

        assertEquals(CompletionRecommendation.BackToPath, recommendation)
    }

    @Test
    fun completionRecommendationReviewsMissesForGoodPassesWithMisses() {
        val recommendation = completionRecommendationFor(LessonSessionStats(correct = 8, missed = 2))

        assertEquals(CompletionRecommendation.ReviewMisses, recommendation)
    }

    @Test
    fun completionRecommendationRepeatsLowAccuracyRows() {
        val recommendation = completionRecommendationFor(LessonSessionStats(correct = 2, missed = 2))

        assertEquals(CompletionRecommendation.RepeatRow, recommendation)
    }
}
