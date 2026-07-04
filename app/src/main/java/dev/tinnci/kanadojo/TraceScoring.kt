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
