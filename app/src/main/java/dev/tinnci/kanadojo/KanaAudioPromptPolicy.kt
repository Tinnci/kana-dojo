package dev.tinnci.kanadojo

fun shouldAutoSpeakOnExerciseEnter(exercise: Exercise): Boolean {
    val item = exercise.items.firstOrNull() ?: return false
    if (!supportsAudioPrompt(item)) return false
    return when (exercise.kind) {
        ExerciseKind.RomajiToKana,
        ExerciseKind.SoundToKana,
        ExerciseKind.TraceKana -> true
        ExerciseKind.KanaToRomaji,
        ExerciseKind.PairMatch -> false
    }
}

fun shouldSpeakAfterCorrectChoice(exercise: Exercise): Boolean {
    val item = exercise.items.firstOrNull() ?: return false
    if (!supportsAudioPrompt(item)) return false
    return exercise.kind == ExerciseKind.KanaToRomaji
}

fun shouldOfferManualSpeak(exercise: Exercise): Boolean {
    val item = exercise.items.firstOrNull() ?: return false
    return supportsAudioPrompt(item) && exercise.kind != ExerciseKind.PairMatch
}

fun shouldSpeakForPairMatchSelection(item: KanaItem): Boolean =
    supportsAudioPrompt(item)

fun shouldSpeakForChartTap(item: KanaItem): Boolean =
    supportsAudioPrompt(item)
