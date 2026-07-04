package dev.tinnci.kanadojo

data class TraceGuidance(
    val title: String,
    val cues: List<String>
)

fun traceGuidanceFor(item: KanaItem): TraceGuidance? =
    when (item.kana) {
        "シ" -> TraceGuidance(
            title = "Separate from ツ / ン",
            cues = listOf("Short strokes sit flatter.", "Long stroke rises from lower-left to upper-right.")
        )

        "ツ" -> TraceGuidance(
            title = "Separate from シ",
            cues = listOf("Short strokes stand more upright.", "Long stroke falls from upper-left to lower-right.")
        )

        "ソ" -> TraceGuidance(
            title = "Separate from ン",
            cues = listOf("Starts with a short upper-left stroke.", "Long stroke falls downward.")
        )

        "ン" -> TraceGuidance(
            title = "Separate from ソ / シ",
            cues = listOf("First stroke is small and high.", "Long stroke rises toward the upper-right.")
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
