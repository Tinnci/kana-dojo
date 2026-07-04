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

        assertEquals(10, hiraganaLessons.size)
        assertEquals(10, katakanaLessons.size)
        assertEquals(LearningStage.Anchor, hiraganaLessons[0].stage)
        assertEquals(LearningStage.RegularRows, hiraganaLessons[1].stage)
        assertEquals(LearningStage.Confusable, hiraganaLessons[3].stage)
        assertEquals(LearningStage.Confusable, katakanaLessons[3].stage)
        assertEquals(listOf("a", "i", "u", "e", "o"), hiraganaLessons[0].items.map { it.romaji })
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
}
