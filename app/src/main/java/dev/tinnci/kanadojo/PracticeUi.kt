package dev.tinnci.kanadojo

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MistakePracticeScreen(
    script: Script,
    initialMode: PracticeMode,
    allItems: List<KanaItem>,
    mistakeIds: List<String>,
    mastery: Map<String, Int>,
    reviewDueEpochDays: Map<String, Long>,
    currentEpochDay: Long,
    onSpeak: (String) -> Unit,
    reduceMotion: Boolean,
    onResult: (List<KanaItem>, Boolean) -> Unit,
    onReturnToPath: () -> Unit
) {
    val scriptItems = remember(script) { itemsFor(script) }
    var selectedMode by remember(script, initialMode) { mutableStateOf(initialMode) }
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
    var correctCounts by remember(selectedMode, queueSignature) { mutableStateOf(emptyMap<String, Int>()) }
    var missCounts by remember(selectedMode, queueSignature) { mutableStateOf(emptyMap<String, Int>()) }
    val queueComplete = currentIndex >= practiceItems.size
    val current = if (practiceItems.isEmpty() || queueComplete) null else practiceItems[currentIndex]
    val exercise = current?.let { practiceExerciseFor(it, selectedMode, currentIndex) }
    val optionItems = if (selectedMode == PracticeMode.Cross) allItems else scriptItems
    val sourceCue = remember(script, selectedMode, initialMode) {
        practiceQueueSourceCueFor(
            script = script,
            selectedMode = selectedMode,
            recommendedMode = initialMode
        )
    }
    val weakCount = remember(mistakeSnapshot, scriptItems) {
        val scriptItemIds = scriptItems.map { it.id }.toSet()
        mistakeSnapshot.count { it in scriptItemIds }
    }
    val contrastCount = remember(practiceItems) { practiceItems.count { it.confusable.isNotEmpty() } }
    val dueCount = remember(scriptItems, dueSnapshot, currentEpochDay, masterySnapshot) {
        dueReviewCountFor(scriptItems, dueSnapshot, currentEpochDay, masterySnapshot)
    }
    val soundReadyCount = remember(scriptItems, masterySnapshot) {
        scriptItems.count { supportsAudioPrompt(it) && (masterySnapshot[it.id] ?: 0) >= 1 }
    }
    val queueExplanation = remember(selectedMode, practiceItems, dueCount, weakCount, contrastCount, soundReadyCount) {
        practiceQueueExplanationFor(
            mode = selectedMode,
            queueSize = practiceItems.size,
            dueCount = dueCount,
            weakCount = weakCount,
            contrastCount = contrastCount,
            soundReadyCount = soundReadyCount
        )
    }
    val sessionGoal = remember(selectedMode, practiceItems, dueCount, weakCount, contrastCount) {
        practiceSessionGoalFor(
            mode = selectedMode,
            queueSize = practiceItems.size,
            dueCount = dueCount,
            weakCount = weakCount,
            contrastCount = contrastCount
        )
    }
    val fallbackMode = if ("fallback" in queueExplanation.title.lowercase()) selectedMode else null
    var showIntro by remember(selectedMode, queueSignature) { mutableStateOf(true) }
    val intro = reviewIntroCopyFor(
        mode = selectedMode,
        dueCount = dueCount,
        weakCount = weakCount
    )
    val previewReasons = remember(selectedMode, practiceItems, masterySnapshot, mistakeSnapshot, dueSnapshot, currentEpochDay) {
        practiceItems.take(8).associate { item ->
            item.id to practicePreviewReasonFor(
                item = item,
                mode = selectedMode,
                mastery = masterySnapshot,
                mistakeIds = mistakeSnapshot,
                reviewDueEpochDays = dueSnapshot,
                currentEpochDay = currentEpochDay
            )
        }
    }

    if (practiceItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            PracticeEmptyState(explanation = queueExplanation)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PracticeHeroPanel(selectedMode.title, selectedMode.subtitle)
        PracticeModeTabs(
            selectedMode = selectedMode,
            recommendedMode = initialMode,
            fallbackMode = fallbackMode,
            onModeChange = { selectedMode = it }
        )
        PracticeQueueIntent(
            state = PracticeQueueIntentState(
                mode = selectedMode,
                sourceCue = sourceCue,
                queueSize = practiceItems.size,
                weakCount = weakCount,
                dueCount = dueCount,
                contrastCount = contrastCount,
                explanation = queueExplanation
            ),
            reduceMotion = reduceMotion
        )
        if (showIntro) {
            PracticeIntroPanel(
                intro = intro,
                goal = sessionGoal,
                previewItems = practiceItems.take(8),
                previewReasons = previewReasons,
                onStart = { showIntro = false }
            )
            return@Column
        }
        if (queueComplete) {
            val outcomes = reviewSessionOutcomesFor(correctCounts, missCounts)
            PracticeCompletionPanel(
                mode = selectedMode,
                stats = sessionStats,
                outcomes = outcomes,
                cleanItems = practiceItems.filter { it.id in outcomes.cleanIds },
                repairedItems = practiceItems.filter { it.id in outcomes.repairedIds },
                shakyItems = practiceItems.filter { it.id in outcomes.shakyIds },
                queueSize = practiceItems.size,
                reduceMotion = reduceMotion,
                onReturnToPath = onReturnToPath,
                onRepeat = {
                    currentIndex = 0
                    feedback = null
                    sessionStats = LessonSessionStats()
                    correctCounts = emptyMap()
                    missCounts = emptyMap()
                    showIntro = true
                }
            )
            return@Column
        }
        if (current == null || exercise == null) {
            return@Column
        }
        PracticeSessionPanel(
            stats = sessionStats,
            completed = currentIndex,
            queueSize = practiceItems.size,
            goal = sessionGoal
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
                    if (correct) {
                        correctCounts = correctCounts + (current.id to ((correctCounts[current.id] ?: 0) + 1))
                    } else {
                        missCounts = missCounts + (current.id to ((missCounts[current.id] ?: 0) + 1))
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

private data class PracticeQueueIntentState(
    val mode: PracticeMode,
    val sourceCue: PracticeQueueSourceCue,
    val queueSize: Int,
    val weakCount: Int,
    val dueCount: Int,
    val contrastCount: Int,
    val explanation: PracticeQueueExplanation
)

@Composable
private fun PracticeCompletionPanel(
    mode: PracticeMode,
    stats: LessonSessionStats,
    outcomes: ReviewSessionOutcomes,
    cleanItems: List<KanaItem>,
    repairedItems: List<KanaItem>,
    shakyItems: List<KanaItem>,
    queueSize: Int,
    reduceMotion: Boolean,
    onReturnToPath: () -> Unit,
    onRepeat: () -> Unit
) {
    val accuracy by animateFloatAsState(targetValue = stats.accuracy, label = "practiceCompletionAccuracy")
    val action = reviewCompletionActionFor(stats)
    val stable = action == ReviewCompletionAction.ReturnToPath
    val nextStep = practiceCompletionNextStepFor(mode, stats)
    val repeatActionLabel = practiceRepeatActionLabelFor(mode)
    val compactRepeatActionLabel = practiceRepeatActionLabelFor(mode, compact = true)
    val completionMetrics = practiceCompletionMetricsFor(outcomes, queueSize)
    val accuracyTone = practiceAccuracyToneCopyFor(stats)
    val outcomeGuidance = practiceOutcomeGuidanceCopyFor(outcomes)
    val actionRationale = practiceActionRationaleCopyFor(action, stats)
    val showActionRationale = shouldShowPracticeActionRationale(action, stats)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Review complete",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        PracticeAccuracyToneChip(accuracyTone)
                    }
                    Text(accuracyTone.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
            LinearProgressIndicator(progress = { accuracy }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                completionMetrics.forEach { metric ->
                    PracticeCompletionMetricTile(metric, Modifier.weight(1f))
                }
            }
            PracticeCompletionSectionDivider("Outcome")
            PracticeOutcomeGuidancePanel(copy = outcomeGuidance, reduceMotion = reduceMotion)
            if (cleanItems.isNotEmpty()) {
                CompletionKanaGroup("Clean", cleanItems)
            }
            if (repairedItems.isNotEmpty()) {
                CompletionKanaGroup("Repaired", repairedItems)
            }
            if (shakyItems.isNotEmpty()) {
                CompletionKanaGroup("Still shaky", shakyItems)
            }
            PracticeCompletionNextStepPanel(
                mode = mode,
                nextStep = nextStep,
                repeatRequired = !stable,
                reduceMotion = reduceMotion
            )
            PracticeCompletionSectionDivider("Action")
            if (showActionRationale) {
                PracticeActionRationalePanel(copy = actionRationale, action = action, reduceMotion = reduceMotion)
            }
            if (stable) {
                PracticeCompletionActionGroup(
                    repeatActionLabel = repeatActionLabel,
                    compactRepeatActionLabel = compactRepeatActionLabel,
                    reduceMotion = reduceMotion,
                    onReturnToPath = onReturnToPath,
                    onRepeat = onRepeat
                )
            } else {
                PracticeRepeatRequiredActionGroup(
                    repeatActionLabel = repeatActionLabel,
                    compactRepeatActionLabel = compactRepeatActionLabel,
                    reduceMotion = reduceMotion,
                    onRepeat = onRepeat
                )
            }
        }
    }
}

@Composable
private fun PracticeCompletionActionGroup(
    repeatActionLabel: String,
    compactRepeatActionLabel: String,
    reduceMotion: Boolean,
    onReturnToPath: () -> Unit,
    onRepeat: () -> Unit
) {
    val animationModifier = practiceCompletionActionGroupEntranceModifier(
        key = "clean-$repeatActionLabel",
        reduceMotion = reduceMotion
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDCEBDD).copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .then(animationModifier)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            PracticeActionRoleChip(practiceActionRoleLabelFor(ReviewCompletionAction.ReturnToPath))
            Button(onClick = onReturnToPath, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.School, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                PracticeActionButtonLabel(practiceReturnActionLabelFor(compact = true))
            }
            PracticeActionRoleChip(practiceActionRoleLabelFor(ReviewCompletionAction.ReturnToPath, optional = true))
            FilledTonalButton(onClick = onRepeat, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                PracticeActionButtonLabel(compactRepeatActionLabel)
            }
        }
    }
}

@Composable
private fun PracticeRepeatRequiredActionGroup(
    repeatActionLabel: String,
    compactRepeatActionLabel: String,
    reduceMotion: Boolean,
    onRepeat: () -> Unit
) {
    val animationModifier = practiceCompletionActionGroupEntranceModifier(
        key = "repeat-$repeatActionLabel",
        reduceMotion = reduceMotion
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFDFD6).copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .then(animationModifier)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            PracticeActionRoleChip(practiceActionRoleLabelFor(ReviewCompletionAction.RepeatQueue))
            Button(
                onClick = onRepeat,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.Replay, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                PracticeActionButtonLabel(compactRepeatActionLabel)
            }
        }
    }
}

@Composable
private fun PracticeActionButtonLabel(label: String) {
    Text(
        label,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PracticeActionRoleChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = practiceActionRoleColor(label)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = 112.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun practiceActionRoleColor(label: String): Color =
    when (label) {
        "Primary" -> Color(0xFFE2EEF8)
        "Optional repeat" -> MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
        "Repeat first" -> Color(0xFFFFDFD6)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    }

@Composable
private fun practiceCompletionActionGroupEntranceModifier(key: String, reduceMotion: Boolean): Modifier {
    var entered by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        entered = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "practiceActionGroupAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = tween(durationMillis = 200),
        label = "practiceActionGroupScale"
    )
    return Modifier.graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
}

@Composable
private fun PracticeCompletionSectionDivider(label: String) {
    val toneColor = practiceCompletionSectionToneColor(label)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = toneColor
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(toneColor.copy(alpha = 0.72f))
        )
    }
}

@Composable
private fun practiceCompletionSectionToneColor(label: String): Color =
    when (label) {
        "Outcome" -> Color(0xFFE2EEF8)
        "Action" -> Color(0xFFDCEBDD)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)
    }

@Composable
private fun PracticeActionRationalePanel(
    copy: PracticeActionRationaleCopy,
    action: ReviewCompletionAction,
    reduceMotion: Boolean
) {
    var entered by remember(copy.title, copy.message, action) { mutableStateOf(false) }
    LaunchedEffect(copy.title, copy.message, action) {
        entered = true
    }
    val repeatRequired = action == ReviewCompletionAction.RepeatQueue
    val icon = if (repeatRequired) Icons.Outlined.Replay else Icons.Outlined.School
    val tint = if (repeatRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val iconContainerColor = if (repeatRequired) Color(0xFFFFDFD6) else Color(0xFFDCEBDD)
    val panelAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "practiceActionRationaleAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = tween(durationMillis = 200),
        label = "practiceActionRationaleScale"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = panelAlpha, scaleX = panelScale, scaleY = panelScale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconContainerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(copy.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(copy.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PracticeOutcomeGuidancePanel(copy: PracticeOutcomeGuidanceCopy, reduceMotion: Boolean) {
    var entered by remember(copy.title, copy.message) { mutableStateOf(false) }
    LaunchedEffect(copy.title, copy.message) {
        entered = true
    }
    val panelAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "practiceOutcomeGuidanceAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = tween(durationMillis = 200),
        label = "practiceOutcomeGuidanceScale"
    )
    val needsRepeat = copy.title == "Still shaky" || copy.title == "Repair split"
    val icon = if (needsRepeat) Icons.Outlined.Replay else Icons.Outlined.CheckCircle
    val iconTint = if (needsRepeat) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = panelAlpha, scaleX = panelScale, scaleY = panelScale)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(practiceOutcomeGuidanceColor(copy.title), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(copy.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(copy.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun practiceOutcomeGuidanceColor(title: String): Color =
    when (title) {
        "Clean set" -> Color(0xFFDCEBDD)
        "Repair held" -> Color(0xFFE2EEF8)
        "Still shaky", "Repair split" -> Color(0xFFFFDFD6)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

@Composable
private fun PracticeAccuracyToneChip(copy: PracticeAccuracyToneCopy) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = practiceAccuracyToneColor(copy.label)
    ) {
        Text(
            copy.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun practiceAccuracyToneColor(label: String): Color =
    when (label) {
        "Clean" -> Color(0xFFDCEBDD)
        "Repair" -> Color(0xFFE2EEF8)
        "Repeat" -> Color(0xFFFFDFD6)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    }

@Composable
private fun PracticeCompletionMetricTile(metric: PracticeCompletionMetric, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(metric.value.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(metric.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = practiceCompletionMetricToneColor(metric.toneLabel)
            ) {
                Text(
                    metric.toneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun practiceCompletionMetricToneColor(toneLabel: String): Color =
    when (toneLabel) {
        "Stable" -> Color(0xFFDCEBDD)
        "Fixed" -> Color(0xFFE2EEF8)
        "Repeat" -> Color(0xFFFFDFD6)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    }

@Composable
private fun PracticeCompletionNextStepPanel(
    mode: PracticeMode,
    nextStep: PracticeCompletionNextStep,
    repeatRequired: Boolean,
    reduceMotion: Boolean
) {
    var entered by remember(mode, nextStep.title, nextStep.message, repeatRequired) { mutableStateOf(false) }
    LaunchedEffect(mode, nextStep.title, nextStep.message, repeatRequired) {
        entered = true
    }
    val targetContainerColor = if (repeatRequired) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.76f)
    } else {
        practiceModeColor(mode).copy(alpha = 0.76f)
    }
    val targetIconColor = if (repeatRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(targetValue = targetContainerColor, label = "practiceCompletionNextStepTone")
    val iconColor by animateColorAsState(targetValue = targetIconColor, label = "practiceCompletionNextStepIconTone")
    val panelAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "practiceCompletionNextStepAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = tween(durationMillis = 220),
        label = "practiceCompletionNextStepScale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = panelAlpha, scaleX = panelScale, scaleY = panelScale)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        nextStep.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    PracticeCompletionModeChip(mode)
                }
                Text(nextStep.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PracticeCompletionModeChip(mode: PracticeMode) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Text(
            mode.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CompletionKanaGroup(label: String, items: List<KanaItem>) {
    val previewLimit = 8
    val hiddenCount = (items.size - previewLimit).coerceAtLeast(0)
    val toneLabel = practiceCompletionGroupToneLabelFor(label)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = practiceCompletionMetricToneColor(toneLabel)
            ) {
                Text(
                    toneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            if (hiddenCount > 0) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
                ) {
                    Text(
                        "+$hiddenCount more",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            ) {
                Text(
                    "${items.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(items.take(previewLimit), key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
                ) {
                    Text(
                        item.kana,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeIntroPanel(
    intro: PracticeIntroCopy,
    goal: PracticeSessionGoal,
    previewItems: List<KanaItem>,
    previewReasons: Map<String, String>,
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
            PracticeGoalLine(goal)
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
                            Text(previewReasons[item.id].orEmpty(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
private fun PracticeGoalLine(goal: PracticeSessionGoal, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(goal.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(goal.message, style = MaterialTheme.typography.bodySmall)
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
private fun PracticeQueueIntent(state: PracticeQueueIntentState, reduceMotion: Boolean) {
    if (reduceMotion) {
        PracticeQueuePanel(
            mode = state.mode,
            sourceCue = state.sourceCue,
            queueSize = state.queueSize,
            weakCount = state.weakCount,
            dueCount = state.dueCount,
            contrastCount = state.contrastCount,
            explanation = state.explanation
        )
        return
    }

    Crossfade(
        targetState = state,
        animationSpec = tween(durationMillis = 180),
        label = "practiceQueueIntent"
    ) { intent ->
        PracticeQueuePanel(
            mode = intent.mode,
            sourceCue = intent.sourceCue,
            queueSize = intent.queueSize,
            weakCount = intent.weakCount,
            dueCount = intent.dueCount,
            contrastCount = intent.contrastCount,
            explanation = intent.explanation
        )
    }
}

@Composable
private fun PracticeQueuePanel(
    mode: PracticeMode,
    sourceCue: PracticeQueueSourceCue,
    queueSize: Int,
    weakCount: Int,
    dueCount: Int,
    contrastCount: Int,
    explanation: PracticeQueueExplanation
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
                        Text(sourceCue.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(queueSize.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PracticeQueueMetric("Queue", queueSize, Modifier.weight(1f))
                    PracticeQueueMetric("Weak", weakCount, Modifier.weight(1f))
                    PracticeQueueMetric("Due", dueCount, Modifier.weight(1f))
                    PracticeQueueMetric("Contrast", contrastCount, Modifier.weight(1f))
                }
                PracticeQueueSourcePanel(sourceCue)
                PracticeQueueExplanationPanel(explanation)
            }
        }
    }
}

@Composable
private fun PracticeQueueSourcePanel(sourceCue: PracticeQueueSourceCue) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(sourceCue.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PracticeQueueExplanationPanel(explanation: PracticeQueueExplanation) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(explanation.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(explanation.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PracticeEmptyState(explanation: PracticeQueueExplanation) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.School, contentDescription = null)
            Text(explanation.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(explanation.message, style = MaterialTheme.typography.bodyMedium)
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
private fun PracticeSessionPanel(
    stats: LessonSessionStats,
    completed: Int,
    queueSize: Int,
    goal: PracticeSessionGoal
) {
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
            PracticeGoalLine(goal)
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
private fun PracticeModeTabs(
    selectedMode: PracticeMode,
    recommendedMode: PracticeMode,
    fallbackMode: PracticeMode?,
    onModeChange: (PracticeMode) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PracticeMode.entries.forEach { mode ->
            item {
                val affordance = practiceModeTabAffordanceFor(
                    mode = mode,
                    selectedMode = selectedMode,
                    recommendedMode = recommendedMode,
                    fallbackMode = fallbackMode
                )
                AssistChip(
                    onClick = { onModeChange(mode) },
                    label = {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(affordance.label, fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Medium)
                            affordance.badge?.let { badge ->
                                Text(
                                    badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
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
