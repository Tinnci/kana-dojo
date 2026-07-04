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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    reduceMotion: Boolean,
    onResult: (List<KanaItem>, Boolean) -> Unit,
    onExit: (LessonResumeCue?) -> Unit,
    onReviewMistakes: () -> Unit
) {
    val queue = remember(lesson) { mutableStateListOf<Exercise>().apply { addAll(buildLessonExercises(lesson)) } }
    var completed by remember(lesson) { mutableIntStateOf(0) }
    var sessionStats by remember(lesson) { mutableStateOf(LessonSessionStats()) }
    var feedback by remember(lesson) { mutableStateOf<AnswerFeedback?>(null) }
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
                Icon(Icons.Outlined.Close, contentDescription = "Close")
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
                reduceMotion = reduceMotion,
                onContinue = { onExit(null) },
                onRepeat = {
                    queue.clear()
                    queue.addAll(buildLessonExercises(lesson))
                    completed = 0
                    sessionStats = LessonSessionStats()
                    feedback = null
                },
                onReviewMistakes = onReviewMistakes
            )
        } else {
            ExerciseCard(
                exercise = current,
                allItems = allItems,
                onSpeak = onSpeak,
                reduceMotion = reduceMotion,
                feedback = feedback,
                onAnswer = { correct ->
                    if (feedback == null) {
                        feedback = AnswerFeedback(correct = correct, answer = correctAnswerFor(current))
                        sessionStats = if (correct) {
                            sessionStats.copy(correct = sessionStats.correct + 1)
                        } else {
                            sessionStats.copy(missed = sessionStats.missed + 1)
                        }
                        onResult(current.items, correct)
                    }
                },
                onContinue = {
                    feedback?.let { result ->
                        queue.removeAt(0)
                        completed += 1
                        if (!result.correct) {
                            queue.add(buildMistakeExercise(current.items.first()))
                        }
                        feedback = null
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
    reduceMotion: Boolean,
    onContinue: () -> Unit,
    onRepeat: () -> Unit,
    onReviewMistakes: () -> Unit
) {
    val accuracy by animateFloatAsState(targetValue = stats.accuracy, label = "lessonAccuracy")
    var entered by remember(lesson.index) { mutableStateOf(false) }
    LaunchedEffect(lesson.index) {
        entered = true
    }
    val celebrationColor by animateColorAsState(
        targetValue = when {
            stats.missed == 0 -> MaterialTheme.colorScheme.primaryContainer
            stats.accuracy >= 0.75f -> Color(0xFFFFF1BC)
            else -> Color(0xFFFFDFD6)
        },
        label = "completionColor"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.88f,
        label = "completionBadgeScale"
    )
    val summaryAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        label = "completionSummaryAlpha"
    )
    val summaryScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.96f,
        label = "completionSummaryScale"
    )
    val actionAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        label = "completionActionAlpha"
    )
    val message = when {
        stats.missed == 0 -> "Clean run. Keep the recall warm."
        stats.accuracy >= 0.75f -> "Good pass. Misses are queued for review."
        else -> "This row needs another repair pass."
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
        Text("Lesson complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        CompletionSparkRow(visible = entered && !reduceMotion, cleanRun = stats.missed == 0)
        Spacer(Modifier.height(8.dp))
        StageChip(lesson.stage)
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
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
                    ProgressStat("Correct", stats.correct, Color(0xFFDCEBDD), Modifier.weight(1f))
                    ProgressStat("Missed", stats.missed, Color(0xFFFFDFD6), Modifier.weight(1f))
                }
                LinearProgressIndicator(progress = { accuracy }, modifier = Modifier.fillMaxWidth())
                Text(lesson.items.joinToString(" ") { it.kana }, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(lesson.items.joinToString("  ") { it.romaji }, style = MaterialTheme.typography.titleMedium)
                Text("These will stay in review until they feel automatic.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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
            recommendation.message,
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
                    Text("Repeat row", fontWeight = FontWeight.Bold)
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
                            Text("Review")
                        }
                        OutlinedButton(
                            onClick = onContinue,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Path")
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
                        Text("Back to path")
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
                    Text("Review misses", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onContinue,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Back to path")
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
                    Text("Back to path", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
                Text("Up next", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
private fun CompletionSparkRow(visible: Boolean, cleanRun: Boolean) {
    val sparkScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        label = "completionSparkScale"
    )
    val sparkAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
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
