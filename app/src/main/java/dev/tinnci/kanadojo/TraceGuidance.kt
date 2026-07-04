package dev.tinnci.kanadojo

data class TraceGuidance(
    val title: String,
    val cues: List<String>,
    val overlays: List<TraceOverlayCue> = emptyList()
)

data class TraceGuidePoint(
    val x: Float,
    val y: Float
)

data class TraceOverlayCue(
    val label: String,
    val start: TraceGuidePoint,
    val end: TraceGuidePoint,
    val cue: String
)

fun traceGuidanceFor(item: KanaItem): TraceGuidance? =
    when (item.kana) {
        "シ" -> TraceGuidance(
            title = "Separate from ツ / ン",
            cues = listOf("Short strokes sit flatter.", "Long stroke rises from lower-left to upper-right."),
            overlays = listOf(
                TraceOverlayCue("Main", TraceGuidePoint(0.28f, 0.70f), TraceGuidePoint(0.76f, 0.42f), "Rise toward upper-right.")
            )
        )

        "ツ" -> TraceGuidance(
            title = "Separate from シ",
            cues = listOf("Short strokes stand more upright.", "Long stroke falls from upper-left to lower-right."),
            overlays = listOf(
                TraceOverlayCue("Main", TraceGuidePoint(0.36f, 0.34f), TraceGuidePoint(0.72f, 0.72f), "Fall toward lower-right.")
            )
        )

        "ソ" -> TraceGuidance(
            title = "Separate from ン",
            cues = listOf("Starts with a short upper-left stroke.", "Long stroke falls downward."),
            overlays = listOf(
                TraceOverlayCue("Main", TraceGuidePoint(0.64f, 0.30f), TraceGuidePoint(0.42f, 0.76f), "Drop down-left.")
            )
        )

        "ン" -> TraceGuidance(
            title = "Separate from ソ / シ",
            cues = listOf("First stroke is small and high.", "Long stroke rises toward the upper-right."),
            overlays = listOf(
                TraceOverlayCue("Main", TraceGuidePoint(0.30f, 0.72f), TraceGuidePoint(0.76f, 0.42f), "Rise toward upper-right.")
            )
        )

        "さ" -> TraceGuidance(
            title = "Separate from ち",
            cues = listOf("Top stroke is separate.", "Lower curve stays compact.")
        )

        "ち" -> TraceGuidance(
            title = "Separate from さ",
            cues = listOf("Starts with a top stroke.", "Main stroke drops then curves wide.")
        )

        "ぬ" -> TraceGuidance(
            title = "Separate from め",
            cues = listOf("Look for the loop and finishing tail.", "Keep the crossing clear.")
        )

        "め" -> TraceGuidance(
            title = "Separate from ぬ",
            cues = listOf("No final loop tail.", "Keep the curve simple and open.")
        )

        else -> null
    }
