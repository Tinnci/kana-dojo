package dev.tinnci.kanadojo

private const val TOKEN_SEPARATOR = "|"
private const val ITEM_SEPARATOR = ","

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
