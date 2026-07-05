package dev.tinnci.kanadojo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun ExerciseCard(
    exercise: Exercise,
    allItems: List<KanaItem>,
    onSpeak: (String) -> Unit,
    onEarcon: (KanaEarcon) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    feedback: AnswerFeedback?,
    onAnswer: (Boolean) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ExerciseHeader(exercise.kind)
            Box(modifier = Modifier.weight(1f)) {
                when (exercise.kind) {
                    ExerciseKind.KanaToRomaji -> ChoiceExercise(
                        exercise = exercise,
                        prompt = exercise.items.first().kana,
                        promptIsKana = true,
                        options = romajiOptions(exercise.items.first(), allItems),
                        correct = exercise.items.first().romaji,
                        reduceMotion = reduceMotion,
                        onAutoSpeak = {
                            onSpeak(exercise.items.first().kana)
                        },
                        onManualSpeak = {
                            onTaptic(KanaTaptic.Speak)
                            onSpeak(exercise.items.first().kana)
                        },
                        onAnswer = onAnswer
                    )

                    ExerciseKind.RomajiToKana -> ChoiceExercise(
                        exercise = exercise,
                        prompt = exercise.items.first().romaji,
                        promptIsKana = false,
                        options = kanaOptions(exercise.items.first(), allItems),
                        correct = exercise.items.first().kana,
                        reduceMotion = reduceMotion,
                        onAutoSpeak = {
                            onSpeak(exercise.items.first().kana)
                        },
                        onManualSpeak = {
                            onTaptic(KanaTaptic.Speak)
                            onSpeak(exercise.items.first().kana)
                        },
                        onAnswer = onAnswer
                    )

                    ExerciseKind.SoundToKana -> SoundChoiceExercise(
                        item = exercise.items.first(),
                        options = kanaOptions(exercise.items.first(), allItems),
                        onSpeak = onSpeak,
                        onTaptic = onTaptic,
                        reduceMotion = reduceMotion,
                        onAnswer = onAnswer
                    )

                    ExerciseKind.PairMatch -> PairMatchExercise(
                        items = exercise.items,
                        answered = feedback != null,
                        onSpeak = onSpeak,
                        onTaptic = onTaptic,
                        onAnswer = onAnswer
                    )

                    ExerciseKind.TraceKana -> TraceKanaExercise(
                        item = exercise.items.first(),
                        answered = feedback != null,
                        autoSpeakOnEnter = shouldAutoSpeakOnExerciseEnter(exercise),
                        manualSpeakEnabled = shouldOfferManualSpeak(exercise),
                        onEarcon = onEarcon,
                        onTaptic = onTaptic,
                        reduceMotion = reduceMotion,
                        onSpeak = onSpeak,
                        onAnswer = onAnswer
                    )
                }
            }
            AnimatedVisibility(
                visible = feedback != null,
                enter = if (reduceMotion) {
                    EnterTransition.None
                } else {
                    fadeIn() + expandVertically(expandFrom = Alignment.Top) + scaleIn(initialScale = 0.96f)
                },
                exit = if (reduceMotion) {
                    ExitTransition.None
                } else {
                    fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top) + scaleOut(targetScale = 0.96f)
                }
            ) {
                feedback?.let {
                    FeedbackBanner(feedback = it)
                }
            }
            AnimatedVisibility(
                visible = feedback != null,
                enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(stringResource(R.string.action_continue), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseHeader(kind: ExerciseKind) {
    val (title, subtitle) = when (kind) {
        ExerciseKind.KanaToRomaji -> stringResource(R.string.exercise_kana_to_romaji_title) to stringResource(R.string.exercise_kana_to_romaji_subtitle)
        ExerciseKind.RomajiToKana -> stringResource(R.string.exercise_romaji_to_kana_title) to stringResource(R.string.exercise_romaji_to_kana_subtitle)
        ExerciseKind.SoundToKana -> stringResource(R.string.exercise_sound_to_kana_title) to stringResource(R.string.exercise_sound_to_kana_subtitle)
        ExerciseKind.PairMatch -> stringResource(R.string.exercise_pair_match_title) to stringResource(R.string.exercise_pair_match_subtitle)
        ExerciseKind.TraceKana -> stringResource(R.string.exercise_trace_title) to stringResource(R.string.exercise_trace_subtitle)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (kind == ExerciseKind.TraceKana) Icons.Outlined.TouchApp else Icons.Outlined.School,
                contentDescription = null
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FeedbackBanner(feedback: AnswerFeedback) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (feedback.correct) Color(0xFFDCEBDD) else Color(0xFFFFDFD6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (feedback.correct) Icons.Outlined.CheckCircle else Icons.Outlined.Replay,
                contentDescription = null,
                tint = if (feedback.correct) Color(0xFF2F5D50) else Color(0xFF9B2D20)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    if (feedback.slow) {
                        stringResource(R.string.exercise_feedback_slow)
                    } else if (feedback.correct) {
                        stringResource(R.string.exercise_feedback_correct)
                    } else {
                        stringResource(R.string.exercise_feedback_answer, feedback.answer)
                    },
                    fontWeight = FontWeight.Bold
                )
                if (feedback.slow) {
                    Text(
                        stringResource(R.string.exercise_feedback_speed_target, (SpeedPracticeTargetMillis / 1000L).toInt()),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (!feedback.correct) {
                    Text(stringResource(R.string.exercise_feedback_retry), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChoiceExercise(
    exercise: Exercise,
    prompt: String,
    promptIsKana: Boolean,
    options: List<String>,
    correct: String,
    reduceMotion: Boolean,
    onAutoSpeak: () -> Unit,
    onManualSpeak: () -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    var selectedOption by rememberSaveable(prompt, correct) { mutableStateOf<String?>(null) }
    val autoSpeakOnEnter = shouldAutoSpeakOnExerciseEnter(exercise)
    val speakAfterCorrectChoice = shouldSpeakAfterCorrectChoice(exercise)
    val manualSpeakEnabled = shouldOfferManualSpeak(exercise)

    LaunchedEffect(prompt, correct, autoSpeakOnEnter) {
        if (autoSpeakOnEnter) {
            onAutoSpeak()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                prompt,
                fontSize = if (promptIsKana) 108.sp else 56.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (manualSpeakEnabled) {
                FilledTonalButton(onClick = onManualSpeak) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.exercise_hear_action))
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(options) { option ->
                val answered = selectedOption != null
                AnswerOptionButton(
                    option = option,
                    answered = answered,
                    correct = option == correct,
                    selected = option == selectedOption,
                    fontSize = if (option.length == 1) 34 else 22,
                    reduceMotion = reduceMotion,
                    onClick = {
                        if (selectedOption == null) {
                            selectedOption = option
                            if (speakAfterCorrectChoice && option == correct) {
                                onAutoSpeak()
                            }
                            onAnswer(option == correct)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SoundChoiceExercise(
    item: KanaItem,
    options: List<String>,
    onSpeak: (String) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    onAnswer: (Boolean) -> Unit
) {
    var selectedOption by rememberSaveable(item.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        onSpeak(item.kana)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = {
                    onTaptic(KanaTaptic.Speak)
                    onSpeak(item.kana)
                },
                shape = CircleShape,
                modifier = Modifier.size(116.dp)
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(R.string.exercise_play_sound_content_description),
                    modifier = Modifier.size(52.dp)
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(options) { option ->
                val answered = selectedOption != null
                AnswerOptionButton(
                    option = option,
                    answered = answered,
                    correct = option == item.kana,
                    selected = option == selectedOption,
                    fontSize = 34,
                    reduceMotion = reduceMotion,
                    onClick = {
                        if (selectedOption == null) {
                            selectedOption = option
                            onAnswer(option == item.kana)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AnswerOptionButton(
    option: String,
    answered: Boolean,
    correct: Boolean,
    selected: Boolean,
    fontSize: Int,
    reduceMotion: Boolean,
    onClick: () -> Unit
) {
    val targetColor = when {
        answered && correct -> Color(0xFFDCEBDD)
        answered && selected -> Color(0xFFFFDFD6)
        else -> MaterialTheme.colorScheme.surface
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "answerOptionColor"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            answered && correct -> 1.02f
            answered && selected -> 0.98f
            else -> 1f
        },
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "answerOptionScale"
    )

    FilledTonalButton(
        onClick = onClick,
        enabled = !answered,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Text(option, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PairMatchExercise(
    items: List<KanaItem>,
    answered: Boolean,
    onSpeak: (String) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    val matchKey = remember(items) { items.joinToString("|") { it.id } }
    var selectedKanaId by rememberSaveable(matchKey) { mutableStateOf<String?>(null) }
    var selectedRomajiId by rememberSaveable(matchKey) { mutableStateOf<String?>(null) }
    var matched by rememberSaveable(matchKey) { mutableStateOf(emptyList<String>()) }
    val itemsById = remember(items) { items.associateBy { it.id } }
    val selectedKana = selectedKanaId?.let { itemsById[it] }
    val selectedRomaji = selectedRomajiId?.let { itemsById[it] }
    val kanaColumn = remember(items) { items.shuffled(Random(lessonSeed(items))) }
    val romajiColumn = remember(items) { items.shuffled(Random(lessonSeed(items) + 9)) }
    val pairsLeft = (items.size - matched.size).coerceAtLeast(0)

    LaunchedEffect(selectedKana, selectedRomaji) {
        val kana = selectedKana
        val romaji = selectedRomaji
        if (kana != null && romaji != null) {
            if (kana.id == romaji.id) {
                val nextMatched = (matched + kana.id).distinct()
                val completesExercise = nextMatched.size == items.size
                matched = nextMatched
                if (completesExercise && !answered) {
                    onAnswer(true)
                } else {
                    onTaptic(KanaTaptic.Correct)
                }
            } else {
                onTaptic(KanaTaptic.Incorrect)
            }
            selectedKanaId = null
            selectedRomajiId = null
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.56f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.exercise_pair_match_pick_both), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.exercise_pair_match_progress, pairsLeft), style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MatchColumn(
                entries = kanaColumn,
                matched = matched,
                selected = selectedKana,
                label = { it.kana },
                onSelect = {
                    if (!answered) {
                        if (shouldSpeakForPairMatchSelection(it)) {
                            onSpeak(it.kana)
                        }
                        selectedKanaId = it.id
                    }
                },
                modifier = Modifier.weight(1f)
            )
            MatchColumn(
                entries = romajiColumn,
                matched = matched,
                selected = selectedRomaji,
                label = { it.romaji },
                onSelect = {
                    if (!answered) {
                        if (shouldSpeakForPairMatchSelection(it)) {
                            onSpeak(it.kana)
                        }
                        selectedRomajiId = it.id
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MatchColumn(
    entries: List<KanaItem>,
    matched: List<String>,
    selected: KanaItem?,
    label: (KanaItem) -> String,
    onSelect: (KanaItem) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        entries.forEach { item ->
            val done = item.id in matched
            val active = selected?.id == item.id
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = when {
                    done -> Color(0xFFDCEBDD)
                    active -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clickable(enabled = !done) { onSelect(item) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label(item), fontSize = if (label(item).length == 1) 30.sp else 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
