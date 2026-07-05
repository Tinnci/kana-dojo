package dev.tinnci.kanadojo

import kotlin.math.roundToInt

private const val TOKEN_SEPARATOR = "|"
private const val ITEM_SEPARATOR = ","
private const val POINT_SEPARATOR = ":"

fun exerciseSnapshotToken(exercise: Exercise): String =
    listOf(
        exercise.kind.name,
        exercise.items.joinToString(ITEM_SEPARATOR) { it.id }
    ).joinToString(TOKEN_SEPARATOR)

fun exerciseFromSnapshotToken(token: String, itemsById: Map<String, KanaItem>): Exercise? {
    val parts = token.split(TOKEN_SEPARATOR, limit = 2)
    val kind = parts.getOrNull(0)?.let { runCatching { ExerciseKind.valueOf(it) }.getOrNull() } ?: return null
    val itemIds = parts.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
        ?.split(ITEM_SEPARATOR)
        ?: return null
    val items = itemIds.mapNotNull { itemsById[it] }
    if (items.size != itemIds.size || items.isEmpty()) return null
    return Exercise(kind = kind, items = items)
}

fun exerciseSnapshotTokens(exercises: List<Exercise>): List<String> =
    exercises.map(::exerciseSnapshotToken)

fun exercisesFromSnapshotTokens(tokens: List<String>, itemsById: Map<String, KanaItem>): List<Exercise> =
    tokens.mapNotNull { exerciseFromSnapshotToken(it, itemsById) }

fun playableExercisesFromSnapshotTokens(tokens: List<String>, itemsById: Map<String, KanaItem>): List<Exercise> =
    exercisesFromSnapshotTokens(tokens, itemsById)
        .filterNot { it.kind == ExerciseKind.PairMatch && it.items.size < 2 }

fun countSnapshotTokens(counts: Map<String, Int>): List<String> =
    counts.entries
        .filter { it.value > 0 }
        .sortedBy { it.key }
        .map { (id, count) -> "$id$TOKEN_SEPARATOR$count" }

fun countMapFromSnapshotTokens(tokens: List<String>): Map<String, Int> =
    tokens.mapNotNull { token ->
        val parts = token.split(TOKEN_SEPARATOR, limit = 2)
        val id = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val count = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        id to count
    }.toMap()

fun incrementCountSnapshotToken(tokens: List<String>, id: String): List<String> {
    val counts = countMapFromSnapshotTokens(tokens).toMutableMap()
    counts[id] = (counts[id] ?: 0) + 1
    return countSnapshotTokens(counts)
}

fun tracePointSnapshotToken(point: TracePoint): String =
    buildString {
        append(point.x.roundToInt())
        append(POINT_SEPARATOR)
        append(point.y.roundToInt())
        if (point.startsStroke) {
            append(POINT_SEPARATOR)
            append("1")
        }
    }

fun tracePointFromSnapshotToken(token: String): TracePoint? {
    val parts = token.split(POINT_SEPARATOR)
    val x = parts.getOrNull(0)?.toFloatOrNull() ?: return null
    val y = parts.getOrNull(1)?.toFloatOrNull() ?: return null
    val startsStroke = parts.getOrNull(2) == "1"
    return TracePoint(x, y, startsStroke)
}

fun tracePointSnapshotTokens(points: List<TracePoint>): List<String> =
    points.map(::tracePointSnapshotToken)

fun tracePointsFromSnapshotTokens(tokens: List<String>): List<TracePoint> =
    tokens.mapNotNull(::tracePointFromSnapshotToken)
