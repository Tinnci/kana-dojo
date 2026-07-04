package dev.tinnci.kanadojo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceScoringTest {
    @Test
    fun emptyTraceStartsAtZero() {
        val score = traceScoreFor(emptyList())

        assertEquals(0f, score.progress, 0.001f)
        assertFalse(score.ready)
        assertEquals("Trace over the ghost kana.", score.message)
    }

    @Test
    fun shortTraceIsNotReady() {
        val score = traceScoreFor(
            listOf(
                TracePoint(10f, 10f),
                TracePoint(24f, 18f),
                TracePoint(34f, 26f)
            )
        )

        assertFalse(score.ready)
        assertTrue(score.progress < 0.25f)
        assertEquals("Use more of the ghost shape.", score.message)
    }

    @Test
    fun broadTraceBecomesReady() {
        val points = listOf(
            TracePoint(20f, 20f),
            TracePoint(70f, 50f),
            TracePoint(120f, 90f),
            TracePoint(180f, 140f),
            TracePoint(240f, 200f),
            TracePoint(280f, 260f),
            TracePoint(220f, 280f),
            TracePoint(160f, 240f),
            TracePoint(110f, 190f),
            TracePoint(60f, 130f),
            TracePoint(25f, 70f),
            TracePoint(80f, 25f),
            TracePoint(140f, 60f),
            TracePoint(200f, 115f),
            TracePoint(260f, 180f)
        )

        val score = traceScoreFor(points)

        assertTrue(score.ready)
        assertTrue(score.progress >= 0.72f)
        assertEquals("Looks ready to check.", score.message)
    }

    @Test
    fun traceFeedbackStartsWithStartAndDirectionCues() {
        val cues = traceFeedbackCuesFor(emptyList())

        assertEquals("Start", cues[0].label)
        assertEquals("Direction", cues[1].label)
    }

    @Test
    fun traceFeedbackDescribesDirectionAndCoverage() {
        val cues = traceFeedbackCuesFor(
            listOf(
                TracePoint(20f, 20f),
                TracePoint(80f, 30f),
                TracePoint(140f, 40f)
            )
        )

        assertTrue(cues.any { it.label == "Direction" && "right" in it.message })
        assertTrue(cues.any { it.label == "Coverage" && "ghost shape" in it.message })
    }

    @Test
    fun traceGuidanceCoversConfusableKana() {
        val guidedKana = listOf("シ", "ツ", "ソ", "ン", "さ", "ち", "ぬ", "め")

        guidedKana.forEach { kana ->
            val item = (hiraganaItems + katakanaItems).first { it.kana == kana }
            val guidance = traceGuidanceFor(item)

            assertTrue("$kana should have trace guidance", guidance != null)
            assertTrue(guidance!!.cues.isNotEmpty())
        }
    }

    @Test
    fun highConfusionKatakanaHaveTraceOverlays() {
        val overlayKana = listOf("シ", "ツ", "ソ", "ン")

        overlayKana.forEach { kana ->
            val item = katakanaItems.first { it.kana == kana }
            val guidance = traceGuidanceFor(item)

            assertTrue("$kana should have overlay cues", guidance!!.overlays.isNotEmpty())
            assertTrue(
                guidance.overlays.all {
                    it.start.x in 0f..1f &&
                        it.start.y in 0f..1f &&
                        it.end.x in 0f..1f &&
                        it.end.y in 0f..1f
                }
            )
        }
    }

    @Test
    fun hiraganaTraceGuidanceDoesNotUseKatakanaOverlays() {
        val item = hiraganaItems.first { it.kana == "さ" }

        assertEquals(emptyList<TraceOverlayCue>(), traceGuidanceFor(item)?.overlays)
    }

    @Test
    fun traceGuidanceIgnoresNonConfusableKana() {
        val item = hiraganaItems.first { it.kana == "あ" }

        assertEquals(null, traceGuidanceFor(item))
    }
}
