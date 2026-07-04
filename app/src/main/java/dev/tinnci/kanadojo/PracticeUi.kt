package dev.tinnci.kanadojo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MistakePracticeScreen(
    script: Script,
    allItems: List<KanaItem>,
    mistakeIds: List<String>,
    mastery: Map<String, Int>,
    reviewDueEpochDays: Map<String, Long>,
    currentEpochDay: Long,
    onSpeak: (String) -> Unit,
    reduceMotion: Boolean,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val scriptItems = remember(script) { itemsFor(script) }
    var selectedMode by remember(script) { mutableStateOf(PracticeMode.Weak) }
    val mistakeSnapshot = mistakeIds.toList()
    val masterySnapshot = mastery.toMap()
    val dueSnapshot = reviewDueEpochDays.toMap()
    val practiceItems = remember(script, selectedMode, mistakeSnapshot, masterySnapshot, dueSnapshot, currentEpochDay) {
        practiceItemsFor(
            mode = selectedMode,
            scriptItems = scriptItems,
            mistakeIds = mistakeSnapshot,
            allItems = allItems,
            mastery = masterySnapshot,
            reviewDueEpochDays = dueSnapshot,
            currentEpochDay = currentEpochDay
        )
    }
    val queueSignature = remember(practiceItems) { practiceItems.joinToString("|") { it.id } }
    var currentIndex by remember(selectedMode, queueSignature) { mutableIntStateOf(0) }
    var feedback by remember(selectedMode, practiceItems, currentIndex) { mutableStateOf<AnswerFeedback?>(null) }
    var sessionStats by remember(selectedMode, queueSignature) { mutableStateOf(LessonSessionStats()) }
    val current = if (practiceItems.isEmpty()) null else practiceItems[currentIndex % practiceItems.size]
    val exercise = current?.let { practiceExerciseFor(it, selectedMode, currentIndex) }
    val optionItems = if (selectedMode == PracticeMode.Cross) allItems else scriptItems
    val queueLabel = if (selectedMode == PracticeMode.Cross) "Both scripts" else script.label
    val weakCount = remember(mistakeSnapshot, scriptItems) {
        val scriptItemIds = scriptItems.map { it.id }.toSet()
        mistakeSnapshot.count { it in scriptItemIds }
    }
    val contrastCount = remember(practiceItems) { practiceItems.count { it.confusable.isNotEmpty() } }
    val dueCount = remember(scriptItems, dueSnapshot, currentEpochDay, masterySnapshot) {
        dueReviewCountFor(scriptItems, dueSnapshot, currentEpochDay, masterySnapshot)
    }
    var showIntro by remember(selectedMode, queueSignature) { mutableStateOf(true) }
    val intro = reviewIntroCopyFor(
        mode = selectedMode,
        dueCount = dueCount,
        weakCount = weakCount
    )

    if (current == null || exercise == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No kana to review yet.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PracticeHeroPanel(selectedMode.title, selectedMode.subtitle)
        PracticeModeTabs(selectedMode = selectedMode, onModeChange = { selectedMode = it })
        PracticeQueuePanel(
            mode = selectedMode,
            queueLabel = queueLabel,
            queueSize = practiceItems.size,
            weakCount = weakCount,
            dueCount = dueCount,
            contrastCount = contrastCount
        )
        if (showIntro) {
            PracticeIntroPanel(
                intro = intro,
                previewItems = practiceItems.take(8),
                onStart = { showIntro = false }
            )
            return@Column
        }
        PracticeSessionPanel(
            stats = sessionStats,
            completed = currentIndex,
            queueSize = practiceItems.size
        )
        ExerciseCard(
            exercise = exercise,
            allItems = optionItems,
            onSpeak = onSpeak,
            reduceMotion = reduceMotion,
            feedback = feedback,
            onAnswer = { correct ->
                if (feedback == null) {
                    feedback = AnswerFeedback(correct = correct, answer = correctAnswerFor(exercise))
                    sessionStats = if (correct) {
                        sessionStats.copy(correct = sessionStats.correct + 1)
                    } else {
                        sessionStats.copy(missed = sessionStats.missed + 1)
                    }
                    onResult(listOf(current), correct)
                }
            },
            onContinue = {
                currentIndex += 1
                feedback = null
            }
        )
    }
}

@Composable
private fun PracticeIntroPanel(
    intro: PracticeIntroCopy,
    previewItems: List<KanaItem>,
    onStart: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(intro.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(intro.subtitle, style = MaterialTheme.typography.bodyMedium)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(previewItems, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(item.kana, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text(item.romaji, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Button(onClick = onStart, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Text(intro.actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PracticeHeroPanel(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(Color(0xFFDCEBDD), Color(0xFFFFF1BC))))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("あ", color = MaterialTheme.colorScheme.onPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PracticeQueuePanel(
    mode: PracticeMode,
    queueLabel: String,
    queueSize: Int,
    weakCount: Int,
    dueCount: Int,
    contrastCount: Int
) {
    val accentColor = practiceModeColor(mode)
    val containerColor by animateColorAsState(
        targetValue = accentColor.copy(alpha = 0.28f),
        label = "practiceQueueColor"
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
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
                    .size(48.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (mode) {
                        PracticeMode.Weak -> Icons.Outlined.Replay
                        PracticeMode.Contrast -> Icons.Outlined.GridView
                        PracticeMode.Writing -> Icons.Outlined.TouchApp
                        else -> Icons.Outlined.School
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(queueLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(queueSize.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PracticeQueueMetric("Queue", queueSize, Modifier.weight(1f))
                    PracticeQueueMetric("Weak", weakCount, Modifier.weight(1f))
                    PracticeQueueMetric("Due", dueCount, Modifier.weight(1f))
                    PracticeQueueMetric("Contrast", contrastCount, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PracticeQueueMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun practiceModeColor(mode: PracticeMode): Color =
    when (mode) {
        PracticeMode.Weak -> Color(0xFFFFDFD6)
        PracticeMode.Contrast -> Color(0xFFE7DEFF)
        PracticeMode.Sound -> Color(0xFFE2EEF8)
        PracticeMode.Writing -> Color(0xFFDCEBDD)
        PracticeMode.Speed -> Color(0xFFFFF1BC)
        PracticeMode.Cross -> Color(0xFFE2EEF8)
        PracticeMode.Mixed -> MaterialTheme.colorScheme.primaryContainer
    }

@Composable
private fun PracticeSessionPanel(stats: LessonSessionStats, completed: Int, queueSize: Int) {
    val accuracy by animateFloatAsState(targetValue = stats.accuracy, label = "practiceAccuracy")
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Session", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${completed % queueSize + 1}/$queueSize",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(progress = { accuracy }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusMetric("Correct", stats.correct, Modifier.weight(1f))
                FocusMetric("Missed", stats.missed, Modifier.weight(1f))
                FocusMetric("Acc%", (accuracy * 100).toInt(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PracticeModeTabs(selectedMode: PracticeMode, onModeChange: (PracticeMode) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PracticeMode.entries.forEach { mode ->
            item {
                AssistChip(
                    onClick = { onModeChange(mode) },
                    label = { Text(mode.label, fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Medium) },
                    leadingIcon = {
                        if (selectedMode == mode) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    }
}
