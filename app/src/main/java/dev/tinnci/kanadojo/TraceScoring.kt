package dev.tinnci.kanadojo

import kotlin.math.hypot

data class TracePoint(
    val x: Float,
    val y: Float
)

data class TraceScore(
    val progress: Float,
    val ready: Boolean,
    val message: String
)

data class TraceFeedbackCue(
    val label: String,
    val message: String
)

data class TraceRemediationCopy(
    val title: String,
    val message: String,
    val actionLabel: String
)

fun traceScoreFor(points: List<TracePoint>): TraceScore {
    if (points.isEmpty()) {
        return TraceScore(0f, ready = false, message = "Trace over the ghost kana.")
    }
    val pathLength = points.zipWithNext().sumOf { (a, b) -> hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()) }.toFloat()
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val spread = ((maxX - minX).coerceAtMost(260f) / 260f) * ((maxY - minY).coerceAtMost(260f) / 260f)
    val pointScore = (points.size / 34f).coerceIn(0f, 1f)
    val lengthScore = (pathLength / 460f).coerceIn(0f, 1f)
    val progress = (pointScore * 0.32f + lengthScore * 0.38f + spread.coerceIn(0f, 1f) * 0.30f).coerceIn(0f, 1f)
    val ready = progress >= 0.72f && points.size > 12 && pathLength > 220f
    val message = when {
        ready -> "Looks ready to check."
        spread < 0.18f -> "Use more of the ghost shape."
        lengthScore < 0.55f -> "Add a little more stroke length."
        else -> "Keep tracing until the score fills."
    }
    return TraceScore(progress, ready, message)
}

fun traceRemediationFor(score: TraceScore): TraceRemediationCopy? =
    if (score.ready) {
        null
    } else {
        TraceRemediationCopy(
            title = "Shape needs one more pass",
            message = "${score.message} Compare the model, then retry the stroke.",
            actionLabel = "Retry trace"
        )
    }

fun traceFeedbackCuesFor(points: List<TracePoint>, score: TraceScore = traceScoreFor(points)): List<TraceFeedbackCue> {
    if (points.isEmpty()) {
        return listOf(
            TraceFeedbackCue("Start", "Place the first stroke on the ghost kana."),
            TraceFeedbackCue("Direction", "Move slowly enough that the stroke path is visible.")
        )
    }
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val width = maxX - minX
    val height = maxY - minY
    val start = points.first()
    val end = points.last()
    val direction = directionLabelFor(start, end)
    val coverage = when {
        score.ready -> "Coverage is broad enough to check."
        width < 90f || height < 90f -> "Use more of the ghost shape before checking."
        else -> "Coverage is close; add the missing edges."
    }
    return listOf(
        TraceFeedbackCue("Start", "Start marker is shown on your first touch."),
        TraceFeedbackCue("Direction", "Your main movement trends $direction."),
        TraceFeedbackCue("Coverage", coverage)
    )
}

private fun directionLabelFor(start: TracePoint, end: TracePoint): String {
    val dx = end.x - start.x
    val dy = end.y - start.y
    return when {
        kotlin.math.abs(dx) < 24f && kotlin.math.abs(dy) < 24f -> "in a small area"
        kotlin.math.abs(dx) > kotlin.math.abs(dy) && dx > 0f -> "to the right"
        kotlin.math.abs(dx) > kotlin.math.abs(dy) -> "to the left"
        dy > 0f -> "downward"
        else -> "upward"
    }
}
