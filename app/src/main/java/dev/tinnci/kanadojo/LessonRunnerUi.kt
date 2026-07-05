package dev.tinnci.kanadojo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun LessonRunner(
    lesson: KanaLesson,
    allItems: List<KanaItem>,
    lessons: List<KanaLesson>,
    mastery: Map<String, Int>,
    onSpeak: (String) -> Unit,
    onEarcon: (KanaEarcon) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    onResult: (List<KanaItem>, Boolean) -> Unit,
    onExit: (LessonResumeCue?) -> Unit,
    onLessonComplete: (LessonSessionStats) -> Unit,
    onReviewMistakes: () -> Unit
) {
    val allItemsById = remember(allItems) { allItems.associateBy { it.id } }
    val initialQueueTokens = remember(lesson) { exerciseSnapshotTokens(buildLessonExercises(lesson)) }
    val lessonSaveKey = lesson.items.firstOrNull()?.id ?: lesson.index.toString()
    var queueTokens by rememberSaveable(lessonSaveKey) { mutableStateOf(initialQueueTokens) }
    var completed by rememberSaveable(lessonSaveKey) { mutableIntStateOf(0) }
    var correctCount by rememberSaveable(lessonSaveKey) { mutableIntStateOf(0) }
    var missedCount by rememberSaveable(lessonSaveKey) { mutableIntStateOf(0) }
    var feedbackCorrect by rememberSaveable(lessonSaveKey) { mutableStateOf<Boolean?>(null) }
    var feedbackAnswer by rememberSaveable(lessonSaveKey) { mutableStateOf<String?>(null) }
    val queue = remember(queueTokens, allItemsById) {
        playableExercisesFromSnapshotTokens(queueTokens, allItemsById)
    }
    val sessionStats = LessonSessionStats(correct = correctCount, missed = missedCount)
    val lessonRepairHint = stringResource(R.string.exercise_feedback_lesson_repair)
    val feedback = feedbackCorrect?.let { correct ->
        AnswerFeedback(
            correct = correct,
            answer = feedbackAnswer.orEmpty(),
            repairHint = if (correct) null else lessonRepairHint
        )
    }
    val current = queue.firstOrNull()
    val total = (completed + queue.size).coerceAtLeast(1)
    val masterySnapshot = mastery.toMap()
    val completionMastery = remember(lesson, masterySnapshot) {
        masterySnapshot.toMutableMap().apply { lesson.items.forEach { put(it.id, 4) } }
    }
    val nextPreview = remember(lesson, lessons, completionMastery) {
        nextPathLesson(lessons, completionMastery)?.takeIf { it.index != lesson.index }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onExit(lessonResumeCueFor(lesson, completed, total)) }) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.lesson_close_content_description))
            }
            LinearProgressIndicator(
                progress = { completed / total.toFloat() },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${sessionStats.correct}/${sessionStats.attempts.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (current == null) {
            LessonComplete(
                lesson = lesson,
                nextLesson = nextPreview,
                stats = sessionStats,
                onEarcon = onEarcon,
                onTaptic = onTaptic,
                reduceMotion = reduceMotion,
                onContinue = { onLessonComplete(sessionStats) },
                onRepeat = {
                    onEarcon(KanaEarcon.Reset)
                    onTaptic(KanaTaptic.Reset)
                    queueTokens = initialQueueTokens
                    completed = 0
                    correctCount = 0
                    missedCount = 0
                    feedbackCorrect = null
                    feedbackAnswer = null
                },
                onReviewMistakes = onReviewMistakes
            )
        } else {
            ExerciseCard(
                exercise = current,
                allItems = allItems,
                onSpeak = onSpeak,
                onEarcon = onEarcon,
                onTaptic = onTaptic,
                reduceMotion = reduceMotion,
                feedback = feedback,
                onAnswer = { correct ->
                    if (feedback == null) {
                        feedbackCorrect = correct
                        feedbackAnswer = correctAnswerFor(current)
                        if (correct) {
                            correctCount += 1
                        } else {
                            missedCount += 1
                        }
                        onResult(current.items, correct)
                    }
                },
                onContinue = {
                    onEarcon(KanaEarcon.Continue)
                    onTaptic(KanaTaptic.Continue)
                    feedback?.let { result ->
                        queueTokens = exerciseSnapshotTokens(
                            lessonQueueAfterAnswer(
                                current = current,
                                remaining = queue.drop(1),
                                correct = result.correct
                            )
                        )
                        completed += 1
                        feedbackCorrect = null
                        feedbackAnswer = null
                    }
                }
            )
        }
    }
}

@Composable
private fun LessonComplete(
    lesson: KanaLesson,
    nextLesson: KanaLesson?,
    stats: LessonSessionStats,
    onEarcon: (KanaEarcon) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    onContinue: () -> Unit,
    onRepeat: () -> Unit,
    onReviewMistakes: () -> Unit
) {
    val accuracy by animateFloatAsState(
        targetValue = stats.accuracy,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "lessonAccuracy"
    )
    var entered by remember(lesson.index) { mutableStateOf(false) }
    LaunchedEffect(lesson.index) {
        entered = true
        onEarcon(KanaEarcon.Complete)
        onTaptic(KanaTaptic.Complete)
    }
    val celebrationColor by animateColorAsState(
        targetValue = when {
            stats.missed == 0 -> MaterialTheme.colorScheme.primaryContainer
            stats.accuracy >= 0.75f -> Color(0xFFFFF1BC)
            else -> Color(0xFFFFDFD6)
        },
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionColor"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.88f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionBadgeScale"
    )
    val summaryAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionSummaryAlpha"
    )
    val summaryScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.96f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionSummaryScale"
    )
    val actionAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionActionAlpha"
    )
    val message = when {
        stats.missed == 0 -> stringResource(R.string.lesson_completion_clean_message)
        stats.accuracy >= 0.75f -> stringResource(R.string.lesson_completion_good_message)
        else -> stringResource(R.string.lesson_completion_retry_message)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LessonCompleteBadge(
            kana = lesson.items.firstOrNull()?.kana.orEmpty(),
            color = celebrationColor,
            scale = badgeScale
        )
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.lesson_completion_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        CompletionSparkRow(visible = entered && !reduceMotion, cleanRun = stats.missed == 0, reduceMotion = reduceMotion)
        Spacer(Modifier.height(8.dp))
        StageChip(lesson.stage)
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = KanaElevation.Focused,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(alpha = summaryAlpha, scaleX = summaryScale, scaleY = summaryScale)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProgressStat(stringResource(R.string.metric_correct), stats.correct, Color(0xFFDCEBDD), Modifier.weight(1f))
                    ProgressStat(stringResource(R.string.metric_missed), stats.missed, Color(0xFFFFDFD6), Modifier.weight(1f))
                }
                LinearProgressIndicator(progress = { accuracy }, modifier = Modifier.fillMaxWidth())
                Text(lesson.items.joinToString(" ") { it.kana }, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(lesson.items.joinToString("  ") { it.romaji }, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.lesson_completion_review_hint), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.graphicsLayer(alpha = actionAlpha)) {
            CompletionActions(
                stats = stats,
                nextLesson = nextLesson,
                onContinue = onContinue,
                onRepeat = onRepeat,
                onReviewMistakes = onReviewMistakes
            )
        }
    }
}

@Composable
private fun CompletionActions(
    stats: LessonSessionStats,
    nextLesson: KanaLesson?,
    onContinue: () -> Unit,
    onRepeat: () -> Unit,
    onReviewMistakes: () -> Unit
) {
    val recommendation = completionRecommendationFor(stats)
    val needsRepeat = recommendation == CompletionRecommendation.RepeatRow
    val hasMisses = stats.missed > 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            localizedCompletionRecommendation(recommendation),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        when {
            needsRepeat -> {
                Button(
                    onClick = onRepeat,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(stringResource(R.string.lesson_action_repeat_row), fontWeight = FontWeight.Bold)
                }
                if (hasMisses) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onReviewMistakes,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text(stringResource(R.string.lesson_action_review))
                        }
                        OutlinedButton(
                            onClick = onContinue,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text(stringResource(R.string.lesson_action_path_short))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onContinue,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(stringResource(R.string.lesson_action_back_to_path))
                    }
                }
            }

            hasMisses -> {
                Button(
                    onClick = onReviewMistakes,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(stringResource(R.string.lesson_action_review_misses), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onContinue,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.lesson_action_back_to_path))
                }
            }

            else -> {
                nextLesson?.let { lesson ->
                    NextLessonPreview(lesson = lesson)
                }
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(stringResource(R.string.lesson_action_back_to_path), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun localizedCompletionRecommendation(recommendation: CompletionRecommendation): String =
    when (recommendation) {
        CompletionRecommendation.RepeatRow -> stringResource(R.string.lesson_recommendation_repeat)
        CompletionRecommendation.ReviewMisses -> stringResource(R.string.lesson_recommendation_review)
        CompletionRecommendation.BackToPath -> stringResource(R.string.lesson_recommendation_path)
    }

@Composable
private fun NextLessonPreview(lesson: KanaLesson) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(lesson.items.firstOrNull()?.kana.orEmpty(), fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.lesson_up_next), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(lesson.items.joinToString(" ") { it.kana }, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LessonCompleteBadge(kana: String, color: Color, scale: Float) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer(alpha = 0.42f, scaleX = scale, scaleY = scale)
                .background(color, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(3.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(kana.ifBlank { "済" }, fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CompletionSparkRow(visible: Boolean, cleanRun: Boolean, reduceMotion: Boolean) {
    val sparkScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionSparkScale"
    )
    val sparkAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "completionSparkAlpha"
    )
    val colors = if (cleanRun) {
        listOf(Color(0xFFDCEBDD), Color(0xFFFFF1BC), Color(0xFFE7DEFF))
    } else {
        listOf(Color(0xFFFFF1BC), Color(0xFFFFDFD6), Color(0xFFE7DEFF))
    }
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .graphicsLayer(alpha = sparkAlpha),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(if (index == 1) 12.dp else 9.dp)
                    .graphicsLayer(scaleX = sparkScale, scaleY = sparkScale)
                    .background(color, CircleShape)
            )
        }
    }
}
