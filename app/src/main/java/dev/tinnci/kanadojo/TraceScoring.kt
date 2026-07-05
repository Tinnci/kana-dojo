package dev.tinnci.kanadojo

import kotlin.math.hypot
import kotlin.math.sqrt

data class TracePoint(
    val x: Float,
    val y: Float
)

data class TraceBounds(
    val width: Float,
    val height: Float
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

private data class TraceCheckpoint(
    val x: Float,
    val y: Float,
    val radius: Float = 0.14f
)

private data class TraceShapeProfile(
    val checkpoints: List<TraceCheckpoint>,
    val requiredCoverage: Float = 0.78f
)

fun traceScoreFor(points: List<TracePoint>): TraceScore =
    calculateTraceScore(points = points, item = null, bounds = null)

fun traceScoreFor(points: List<TracePoint>, item: KanaItem, bounds: TraceBounds): TraceScore =
    calculateTraceScore(points = points, item = item, bounds = bounds)

private fun calculateTraceScore(points: List<TracePoint>, item: KanaItem?, bounds: TraceBounds?): TraceScore {
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
    val cellScore = occupiedCellScore(points, bounds)
    val turnScore = significantTurnScore(points)
    val profile = item?.let { traceShapeProfileFor(it) }
    val profileScore = profile?.let { traceProfileScore(points, bounds, it) }
    val baseProgress = (
        pointScore * 0.18f +
            lengthScore * 0.24f +
            spread.coerceIn(0f, 1f) * 0.18f +
            cellScore * 0.20f +
            turnScore * 0.20f
        ).coerceIn(0f, 1f)
    val progress = if (profileScore == null) {
        baseProgress
    } else {
        (baseProgress * 0.62f + profileScore * 0.38f).coerceIn(0f, 1f)
    }
    val profileReady = profile == null || (profileScore ?: 0f) >= profile.requiredCoverage
    val ready = progress >= 0.72f &&
        points.size > 12 &&
        pathLength > 220f &&
        cellScore >= 0.45f &&
        turnScore >= 0.20f &&
        profileReady
    val message = when {
        ready -> "Looks ready to check."
        !profileReady -> "Use more of the ghost shape."
        spread < 0.18f -> "Use more of the ghost shape."
        lengthScore < 0.55f -> "Add a little more stroke length."
        else -> "Keep tracing until the score fills."
    }
    return TraceScore(progress, ready, message)
}

private fun occupiedCellScore(points: List<TracePoint>, bounds: TraceBounds?): Float {
    val normalized = normalizedTracePoints(points, bounds)
    val occupied = normalized
        .map { point ->
            val x = (point.x * 4).toInt().coerceIn(0, 3)
            val y = (point.y * 4).toInt().coerceIn(0, 3)
            x to y
        }
        .toSet()
    return (occupied.size / 7f).coerceIn(0f, 1f)
}

private fun significantTurnScore(points: List<TracePoint>): Float {
    if (points.size < 4) return 0f
    val turns = points.windowed(3).count { (a, b, c) ->
        val abx = b.x - a.x
        val aby = b.y - a.y
        val bcx = c.x - b.x
        val bcy = c.y - b.y
        val abLength = hypot(abx.toDouble(), aby.toDouble())
        val bcLength = hypot(bcx.toDouble(), bcy.toDouble())
        if (abLength < 18.0 || bcLength < 18.0) {
            false
        } else {
            val cosine = ((abx * bcx + aby * bcy) / (abLength * bcLength)).coerceIn(-1.0, 1.0)
            cosine < 0.55
        }
    }
    return (turns / 3f).coerceIn(0f, 1f)
}

private fun traceProfileScore(points: List<TracePoint>, bounds: TraceBounds?, profile: TraceShapeProfile): Float {
    val normalized = normalizedTracePoints(points, bounds)
    if (normalized.isEmpty()) return 0f
    val covered = profile.checkpoints.count { checkpoint ->
        normalized.any { point ->
            val dx = point.x - checkpoint.x
            val dy = point.y - checkpoint.y
            sqrt(dx * dx + dy * dy) <= checkpoint.radius
        }
    }
    return covered / profile.checkpoints.size.toFloat()
}

private fun normalizedTracePoints(points: List<TracePoint>, bounds: TraceBounds?): List<TracePoint> {
    if (points.isEmpty()) return emptyList()
    val width = bounds?.width?.takeIf { it > 1f }
    val height = bounds?.height?.takeIf { it > 1f }
    return if (width != null && height != null) {
        points.map { TracePoint((it.x / width).coerceIn(0f, 1f), (it.y / height).coerceIn(0f, 1f)) }
    } else {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val sourceWidth = (maxX - minX).coerceAtLeast(1f)
        val sourceHeight = (maxY - minY).coerceAtLeast(1f)
        points.map { TracePoint((it.x - minX) / sourceWidth, (it.y - minY) / sourceHeight) }
    }
}

private fun traceShapeProfileFor(item: KanaItem): TraceShapeProfile? =
    when (item.kana) {
        "か" -> TraceShapeProfile(
            checkpoints = listOf(
                TraceCheckpoint(0.32f, 0.36f),
                TraceCheckpoint(0.50f, 0.36f),
                TraceCheckpoint(0.43f, 0.58f),
                TraceCheckpoint(0.36f, 0.74f),
                TraceCheckpoint(0.62f, 0.72f),
                TraceCheckpoint(0.76f, 0.36f),
                TraceCheckpoint(0.82f, 0.52f)
            ),
            requiredCoverage = 0.82f
        )

        else -> null
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
