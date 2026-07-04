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

        assertEquals(15, hiraganaLessons.size)
        assertEquals(15, katakanaLessons.size)
        assertEquals(LearningStage.Anchor, hiraganaLessons[0].stage)
        assertEquals(LearningStage.RegularRows, hiraganaLessons[1].stage)
        assertEquals(LearningStage.Confusable, hiraganaLessons[3].stage)
        assertEquals(LearningStage.Confusable, katakanaLessons[3].stage)
        assertEquals(LearningStage.Voiced, hiraganaLessons[10].stage)
        assertEquals(LearningStage.Voiced, katakanaLessons[14].stage)
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
    fun voicedKanaLessonsCoverDakutenAndHandakuten() {
        val hiraganaLessons = lessonsFor(Script.Hiragana)
        val voicedKana = hiraganaLessons.drop(10).flatMap { it.items }.map { it.kana }.toSet()

        assertTrue(setOf("が", "ざ", "だ", "ば", "ぱ").all { it in voicedKana })
        assertTrue(hiraganaLessons.drop(10).all { it.stage == LearningStage.Voiced })
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
    fun focusedPracticeModesUseDistinctExerciseKinds() {
        val item = itemsFor(Script.Katakana).first()

        assertEquals(ExerciseKind.SoundToKana, practiceExerciseFor(item, PracticeMode.Sound, 0).kind)
        assertEquals(ExerciseKind.TraceKana, practiceExerciseFor(item, PracticeMode.Writing, 0).kind)
        assertEquals(ExerciseKind.KanaToRomaji, practiceExerciseFor(item, PracticeMode.Speed, 0).kind)
        assertEquals(ExerciseKind.RomajiToKana, practiceExerciseFor(item, PracticeMode.Speed, 1).kind)
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
}
