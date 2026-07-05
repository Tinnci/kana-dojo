package dev.tinnci.kanadojo

import android.os.SystemClock
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MistakePracticeScreen(
    script: Script,
    layoutMode: KanaLayoutMode,
    initialMode: PracticeMode,
    allItems: List<KanaItem>,
    mistakeIds: List<String>,
    mastery: Map<String, Int>,
    reviewDueEpochDays: Map<String, Long>,
    currentEpochDay: Long,
    onSpeak: (String) -> Unit,
    onEarcon: (KanaEarcon) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    onShellNavigationHiddenChange: (Boolean) -> Unit = {},
    onResult: (List<KanaItem>, Boolean) -> Unit,
    onReturnToPath: () -> Unit
) {
    val scriptItems = remember(script) { itemsFor(script) }
    var selectedMode by rememberSaveable(script, initialMode) { mutableStateOf(initialMode) }
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
    var currentIndex by rememberSaveable(selectedMode, queueSignature) { mutableIntStateOf(0) }
    var feedbackCorrect by rememberSaveable(selectedMode, queueSignature, currentIndex) { mutableStateOf<Boolean?>(null) }
    var feedbackAnswer by rememberSaveable(selectedMode, queueSignature, currentIndex) { mutableStateOf<String?>(null) }
    var feedbackSlow by rememberSaveable(selectedMode, queueSignature, currentIndex) { mutableStateOf(false) }
    var exerciseStartedAtMillis by rememberSaveable(selectedMode, queueSignature, currentIndex) {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }
    var correctTotal by rememberSaveable(selectedMode, queueSignature) { mutableIntStateOf(0) }
    var missedTotal by rememberSaveable(selectedMode, queueSignature) { mutableIntStateOf(0) }
    var correctCountTokens by rememberSaveable(selectedMode, queueSignature) { mutableStateOf(emptyList<String>()) }
    var missCountTokens by rememberSaveable(selectedMode, queueSignature) { mutableStateOf(emptyList<String>()) }
    val feedback = feedbackCorrect?.let { correct ->
        AnswerFeedback(correct = correct, answer = feedbackAnswer.orEmpty(), slow = feedbackSlow)
    }
    val sessionStats = LessonSessionStats(correct = correctTotal, missed = missedTotal)
    val correctCounts = remember(correctCountTokens) { countMapFromSnapshotTokens(correctCountTokens) }
    val missCounts = remember(missCountTokens) { countMapFromSnapshotTokens(missCountTokens) }
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
    var showIntro by rememberSaveable(selectedMode, queueSignature) { mutableStateOf(true) }
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
    val outerPadding = if (layoutMode == KanaLayoutMode.Expanded) 24.dp else 20.dp
    val sessionInProgress = shouldHideShellNavigationForPractice(
        showIntro = showIntro,
        queueComplete = queueComplete,
        hasCurrentExercise = current != null && exercise != null
    )

    LaunchedEffect(sessionInProgress) {
        onShellNavigationHiddenChange(sessionInProgress)
    }
    DisposableEffect(Unit) {
        onDispose { onShellNavigationHiddenChange(false) }
    }

    if (practiceItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(outerPadding), contentAlignment = Alignment.Center) {
            PracticeEmptyState(explanation = queueExplanation)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(outerPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!sessionInProgress) {
            PracticeHeroPanel(
                title = localizedPracticeModeTitle(selectedMode),
                subtitle = localizedPracticeModeSubtitle(selectedMode)
            )
            PracticeModeTabs(
                selectedMode = selectedMode,
                recommendedMode = initialMode,
                fallbackMode = fallbackMode,
                onModeChange = {
                    onEarcon(KanaEarcon.Select)
                    onTaptic(KanaTaptic.Select)
                    selectedMode = it
                }
            )
            if (!showIntro) {
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
            }
        }
        if (showIntro) {
            PracticeIntroPanel(
                intro = intro,
                goal = sessionGoal,
                previewItems = practiceItems.take(8),
                previewReasons = previewReasons,
                onStart = {
                    onEarcon(KanaEarcon.Start)
                    onTaptic(KanaTaptic.Start)
                    exerciseStartedAtMillis = SystemClock.elapsedRealtime()
                    showIntro = false
                }
            )
            return@Column
        }
        if (queueComplete) {
            LaunchedEffect(selectedMode, queueSignature) {
                onEarcon(KanaEarcon.Complete)
                onTaptic(KanaTaptic.Complete)
            }
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
                    onEarcon(KanaEarcon.Reset)
                    onTaptic(KanaTaptic.Reset)
                    currentIndex = 0
                    feedbackCorrect = null
                    feedbackAnswer = null
                    correctTotal = 0
                    missedTotal = 0
                    correctCountTokens = emptyList()
                    missCountTokens = emptyList()
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
            goal = sessionGoal,
            mode = selectedMode,
            reduceMotion = reduceMotion
        )
        val exerciseCardHeight = if (exercise.kind == ExerciseKind.TraceKana) 640.dp else 560.dp
        ExerciseCard(
            exercise = exercise,
            allItems = optionItems,
            onSpeak = onSpeak,
            onEarcon = onEarcon,
            onTaptic = onTaptic,
            reduceMotion = reduceMotion,
            feedback = feedback,
            onAnswer = { correct ->
                if (feedback == null) {
                    val speedOutcome = if (selectedMode == PracticeMode.Speed) {
                        speedAnswerOutcomeFor(
                            correct = correct,
                            elapsedMillis = SystemClock.elapsedRealtime() - exerciseStartedAtMillis
                        )
                    } else {
                        SpeedAnswerOutcome(correct = correct, slow = false)
                    }
                    feedbackCorrect = speedOutcome.correct
                    feedbackAnswer = correctAnswerFor(exercise)
                    feedbackSlow = speedOutcome.slow
                    if (speedOutcome.correct) {
                        correctTotal += 1
                        correctCountTokens = incrementCountSnapshotToken(correctCountTokens, current.id)
                    } else {
                        missedTotal += 1
                        missCountTokens = incrementCountSnapshotToken(missCountTokens, current.id)
                    }
                    onResult(listOf(current), speedOutcome.correct)
                }
            },
            onContinue = {
                onEarcon(KanaEarcon.Continue)
                onTaptic(KanaTaptic.Continue)
                currentIndex += 1
                feedbackCorrect = null
                feedbackAnswer = null
                feedbackSlow = false
                exerciseStartedAtMillis = SystemClock.elapsedRealtime()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(exerciseCardHeight)
        )
    }
}

internal fun shouldHideShellNavigationForPractice(
    showIntro: Boolean,
    queueComplete: Boolean,
    hasCurrentExercise: Boolean
): Boolean =
    !showIntro && !queueComplete && hasCurrentExercise

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
    val accuracy by animateFloatAsState(
        targetValue = stats.accuracy,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceCompletionAccuracy"
    )
    val action = reviewCompletionActionFor(stats)
    val stable = action == ReviewCompletionAction.ReturnToPath
    val nextStep = practiceCompletionNextStepFor(mode, stats)
    val repeatActionLabel = practiceRepeatActionLabelFor(mode)
    val compactRepeatActionLabel = practiceRepeatActionLabelFor(mode, compact = true)
    val completionMetrics = practiceCompletionMetricsFor(outcomes, queueSize)
    val actionAvailability = practiceCompletionActionAvailabilityFor(action, queueSize)
    val disabledActionCopy = practiceCompletionDisabledActionCopyFor(action, actionAvailability)
    val accuracyTone = practiceAccuracyToneCopyFor(stats)
    val outcomeGuidance = practiceOutcomeGuidanceCopyFor(outcomes)
    val actionRationale = practiceActionRationaleCopyFor(action, stats)
    val showActionRationale = shouldShowPracticeActionRationale(action, stats)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = KanaElevation.Resting,
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
                            stringResource(R.string.practice_completion_title),
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
            PracticeCompletionSectionDivider(
                label = stringResource(R.string.practice_completion_section_outcome),
                toneColor = Color(0xFFE2EEF8)
            )
            PracticeOutcomeGuidancePanel(copy = outcomeGuidance, reduceMotion = reduceMotion)
            if (cleanItems.isNotEmpty()) {
                CompletionKanaGroup(
                    label = stringResource(R.string.practice_group_clean),
                    toneLabel = "Stable",
                    items = cleanItems
                )
            }
            if (repairedItems.isNotEmpty()) {
                CompletionKanaGroup(
                    label = stringResource(R.string.practice_group_repaired),
                    toneLabel = "Fixed",
                    items = repairedItems
                )
            }
            if (shakyItems.isNotEmpty()) {
                CompletionKanaGroup(
                    label = stringResource(R.string.practice_group_shaky),
                    toneLabel = "Repeat",
                    items = shakyItems
                )
            }
            PracticeCompletionNextStepPanel(
                mode = mode,
                nextStep = nextStep,
                repeatRequired = !stable,
                reduceMotion = reduceMotion
            )
            PracticeCompletionSectionDivider(
                label = stringResource(R.string.practice_completion_section_action),
                toneColor = Color(0xFFDCEBDD)
            )
            if (showActionRationale) {
                PracticeActionRationalePanel(copy = actionRationale, action = action, reduceMotion = reduceMotion)
            }
            if (stable) {
                PracticeCompletionActionGroup(
                    mode = mode,
                    repeatActionLabel = repeatActionLabel,
                    compactRepeatActionLabel = compactRepeatActionLabel,
                    actionAvailability = actionAvailability,
                    disabledActionCopy = disabledActionCopy,
                    reduceMotion = reduceMotion,
                    onReturnToPath = onReturnToPath,
                    onRepeat = onRepeat
                )
            } else {
                PracticeRepeatRequiredActionGroup(
                    mode = mode,
                    repeatActionLabel = repeatActionLabel,
                    compactRepeatActionLabel = compactRepeatActionLabel,
                    actionAvailability = actionAvailability,
                    disabledActionCopy = disabledActionCopy,
                    reduceMotion = reduceMotion,
                    onRepeat = onRepeat
                )
            }
        }
    }
}

@Composable
private fun PracticeCompletionActionGroup(
    mode: PracticeMode,
    repeatActionLabel: String,
    compactRepeatActionLabel: String,
    actionAvailability: PracticeCompletionActionAvailability,
    disabledActionCopy: PracticeCompletionDisabledActionCopy?,
    reduceMotion: Boolean,
    onReturnToPath: () -> Unit,
    onRepeat: () -> Unit
) {
    val animationModifier = practiceCompletionActionGroupEntranceModifier(
        key = "clean-$repeatActionLabel",
        reduceMotion = reduceMotion
    )
    val actionButtons = practiceCompletionActionButtonMetadataInDisplayOrder(
        completionAction = ReviewCompletionAction.ReturnToPath,
        mode = mode
    )
    val returnActionButton = actionButtons[0]
    val repeatActionButton = actionButtons[1]
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .practiceCompletionActionGroupSummary(ReviewCompletionAction.ReturnToPath)
            .then(animationModifier)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            PracticeActionRoleChip(returnActionButton)
            Button(
                onClick = onReturnToPath,
                enabled = actionAvailability.returnToPathEnabled,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.practiceCompletionActionButtonTouchTarget(
                    semantics = practiceCompletionActionButtonSemanticsFor(
                        actionButton = returnActionButton,
                        enabled = actionAvailability.returnToPathEnabled,
                        disabledCopy = disabledActionCopy
                    )
                )
            ) {
                Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                PracticeActionButtonLabel(practiceReturnActionLabelFor(compact = true))
            }
            PracticeActionRoleChip(repeatActionButton)
            FilledTonalButton(
                onClick = onRepeat,
                enabled = actionAvailability.repeatEnabled,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.practiceCompletionActionButtonTouchTarget(
                    semantics = practiceCompletionActionButtonSemanticsFor(
                        actionButton = repeatActionButton,
                        enabled = actionAvailability.repeatEnabled,
                        disabledCopy = disabledActionCopy
                    )
                )
            ) {
                Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                PracticeActionButtonLabel(compactRepeatActionLabel)
            }
            disabledActionCopy?.let { PracticeDisabledActionCopy(it) }
        }
    }
}

@Composable
private fun PracticeRepeatRequiredActionGroup(
    mode: PracticeMode,
    repeatActionLabel: String,
    compactRepeatActionLabel: String,
    actionAvailability: PracticeCompletionActionAvailability,
    disabledActionCopy: PracticeCompletionDisabledActionCopy?,
    reduceMotion: Boolean,
    onRepeat: () -> Unit
) {
    val animationModifier = practiceCompletionActionGroupEntranceModifier(
        key = "repeat-$repeatActionLabel",
        reduceMotion = reduceMotion
    )
    val repeatActionButton = practiceCompletionActionButtonMetadataInDisplayOrder(
        completionAction = ReviewCompletionAction.RepeatQueue,
        mode = mode
    ).single()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .practiceCompletionActionGroupSummary(ReviewCompletionAction.RepeatQueue)
            .then(animationModifier)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            PracticeActionRoleChip(repeatActionButton)
            Button(
                onClick = onRepeat,
                enabled = actionAvailability.repeatEnabled,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.practiceCompletionActionButtonTouchTarget(
                    semantics = practiceCompletionActionButtonSemanticsFor(
                        actionButton = repeatActionButton,
                        enabled = actionAvailability.repeatEnabled,
                        disabledCopy = disabledActionCopy
                    )
                )
            ) {
                Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                PracticeActionButtonLabel(compactRepeatActionLabel)
            }
            disabledActionCopy?.let { PracticeDisabledActionCopy(it) }
        }
    }
}

private fun Modifier.practiceCompletionActionButtonTouchTarget(
    semantics: PracticeCompletionActionButtonSemantics
): Modifier =
    fillMaxWidth()
        .heightIn(min = 48.dp)
        .semantics(mergeDescendants = semantics.mergeDescendants) {
            contentDescription = semantics.contentDescription
            role = semantics.role.toComposeRole()
            this.stateDescription = semantics.stateDescription
            traversalIndex = semantics.traversalIndex
            onClick(label = semantics.clickLabel, action = null)
        }

private fun PracticeAccessibilityRole.toComposeRole(): Role =
    when (this) {
        PracticeAccessibilityRole.Button -> Role.Button
    }

private fun Modifier.practiceCompletionActionGroupSummary(action: ReviewCompletionAction): Modifier =
    semantics {
        paneTitle = practiceCompletionActionGroupSummaryFor(action)
        isTraversalGroup = true
    }

@Composable
private fun PracticeDisabledActionCopy(copy: PracticeCompletionDisabledActionCopy) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clearAndSetSemantics {}
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            copy.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            practiceCompletionDisabledActionVisibleMessageFor(copy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun PracticeActionRoleChip(actionButton: PracticeCompletionActionButtonMetadata) {
    PracticeActionRoleChip(
        label = actionButton.actionRoleLabel,
        tone = practiceActionRoleToneFor(actionButton),
        excludeFromAccessibility = shouldExcludePracticeActionRoleChipFromAccessibility(actionButton)
    )
}

@Composable
private fun PracticeActionRoleChip(
    label: String,
    tone: PracticeActionRoleTone = practiceActionRoleToneFor(label),
    excludeFromAccessibility: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = practiceActionRoleColor(tone),
        modifier = Modifier.practiceActionRoleChipSemantics(excludeFromAccessibility)
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

private fun Modifier.practiceActionRoleChipSemantics(excludeFromAccessibility: Boolean): Modifier =
    if (excludeFromAccessibility) {
        clearAndSetSemantics {}
    } else {
        this
    }

@Composable
private fun practiceActionRoleColor(tone: PracticeActionRoleTone): Color =
    when (tone) {
        PracticeActionRoleTone.Primary -> Color(0xFFE2EEF8)
        PracticeActionRoleTone.Optional -> MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
        PracticeActionRoleTone.RepeatRequired -> Color(0xFFFFDFD6)
        PracticeActionRoleTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    }

@Composable
private fun practiceCompletionActionGroupEntranceModifier(key: String, reduceMotion: Boolean): Modifier {
    var entered by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        entered = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceActionGroupAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceActionGroupScale"
    )
    return Modifier.graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
}

@Composable
private fun PracticeCompletionSectionDivider(label: String, toneColor: Color) {
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
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceActionRationaleAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = kanaMotionSpec(reduceMotion),
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
                Text(localizedPracticeActionRationaleTitle(copy.title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedPracticeActionRationaleMessage(copy.message), style = MaterialTheme.typography.bodySmall)
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
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceOutcomeGuidanceAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = kanaMotionSpec(reduceMotion),
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
                Text(localizedPracticeOutcomeGuidanceTitle(copy.title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedPracticeOutcomeGuidanceMessage(copy.message), style = MaterialTheme.typography.bodySmall)
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
            localizedPracticeAccuracyLabel(copy.label),
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
private fun localizedPracticeAccuracyLabel(label: String): String =
    when (label) {
        "No reps" -> stringResource(R.string.practice_tone_no_reps)
        "Clean" -> stringResource(R.string.practice_group_clean)
        "Repair" -> stringResource(R.string.practice_tone_repair)
        "Repeat" -> stringResource(R.string.practice_tone_repeat)
        else -> label
    }

@Composable
private fun localizedPracticeCompletionMetricLabel(label: String): String =
    when (label) {
        "Clean" -> stringResource(R.string.practice_group_clean)
        "Repaired" -> stringResource(R.string.practice_group_repaired)
        "Shaky" -> stringResource(R.string.practice_group_shaky_short)
        "Queue" -> stringResource(R.string.metric_queue)
        else -> label
    }

@Composable
private fun localizedPracticeCompletionToneLabel(toneLabel: String): String =
    when (toneLabel) {
        "Stable" -> stringResource(R.string.practice_tone_stable)
        "Fixed" -> stringResource(R.string.practice_tone_fixed)
        "Repeat" -> stringResource(R.string.practice_tone_repeat)
        "Total" -> stringResource(R.string.practice_tone_total)
        "Review" -> stringResource(R.string.practice_tone_review)
        else -> toneLabel
    }

@Composable
private fun localizedPracticeOutcomeGuidanceTitle(title: String): String =
    when (title) {
        "No outcome yet" -> stringResource(R.string.practice_outcome_no_outcome_title)
        "Repair split" -> stringResource(R.string.practice_outcome_repair_split_title)
        "Still shaky" -> stringResource(R.string.practice_outcome_still_shaky_title)
        "Repair held" -> stringResource(R.string.practice_outcome_repair_held_title)
        "Clean set" -> stringResource(R.string.practice_outcome_clean_set_title)
        else -> title
    }

@Composable
private fun localizedPracticeOutcomeGuidanceMessage(message: String): String =
    when (message) {
        "Finish one pass so this summary can separate clean, repaired, and shaky kana." -> stringResource(R.string.practice_outcome_no_outcome_message)
        "Repaired kana improved in this pass; shaky kana still need one more repeat." -> stringResource(R.string.practice_outcome_repair_split_message)
        "These kana missed without a clean answer. Repeat before adding new material." -> stringResource(R.string.practice_outcome_still_shaky_message)
        "Missed kana came back correctly. Review them later instead of drilling now." -> stringResource(R.string.practice_outcome_repair_held_message)
        "No repair needed. Keep momentum with the path or a mixed queue." -> stringResource(R.string.practice_outcome_clean_set_message)
        else -> message
    }

@Composable
private fun localizedPracticeActionRationaleTitle(title: String): String =
    when (title) {
        "Path is ready" -> stringResource(R.string.practice_action_path_ready_title)
        "Measure first" -> stringResource(R.string.practice_action_measure_first_title)
        "Repeat first" -> stringResource(R.string.practice_action_repeat_first_title)
        else -> title
    }

@Composable
private fun localizedPracticeActionRationaleMessage(message: String): String =
    when (message) {
        "A clean queue means new prompts can build on this recall; repeat is optional." -> stringResource(R.string.practice_action_path_ready_message)
        "Run the queue once so the app can separate clean and shaky kana." -> stringResource(R.string.practice_action_measure_first_message)
        "Missed kana are still fresh; one more pass protects the next lesson." -> stringResource(R.string.practice_action_repeat_first_message)
        else -> message
    }

@Composable
private fun localizedPracticeModeLabel(mode: PracticeMode): String =
    when (mode) {
        PracticeMode.Weak -> stringResource(R.string.practice_mode_weak_label)
        PracticeMode.Contrast -> stringResource(R.string.practice_mode_contrast_label)
        PracticeMode.Sound -> stringResource(R.string.practice_mode_sound_label)
        PracticeMode.Writing -> stringResource(R.string.practice_mode_writing_label)
        PracticeMode.Speed -> stringResource(R.string.practice_mode_speed_label)
        PracticeMode.Cross -> stringResource(R.string.practice_mode_cross_label)
        PracticeMode.Mixed -> stringResource(R.string.practice_mode_mixed_label)
    }

@Composable
private fun localizedPracticeModeTitle(mode: PracticeMode): String =
    when (mode) {
        PracticeMode.Weak -> stringResource(R.string.practice_mode_weak_title)
        PracticeMode.Contrast -> stringResource(R.string.practice_mode_contrast_title)
        PracticeMode.Sound -> stringResource(R.string.practice_mode_sound_title)
        PracticeMode.Writing -> stringResource(R.string.practice_mode_writing_title)
        PracticeMode.Speed -> stringResource(R.string.practice_mode_speed_title)
        PracticeMode.Cross -> stringResource(R.string.practice_mode_cross_title)
        PracticeMode.Mixed -> stringResource(R.string.practice_mode_mixed_title)
    }

@Composable
private fun localizedPracticeModeSubtitle(mode: PracticeMode): String =
    when (mode) {
        PracticeMode.Weak -> stringResource(R.string.practice_mode_weak_subtitle)
        PracticeMode.Contrast -> stringResource(R.string.practice_mode_contrast_subtitle)
        PracticeMode.Sound -> stringResource(R.string.practice_mode_sound_subtitle)
        PracticeMode.Writing -> stringResource(R.string.practice_mode_writing_subtitle)
        PracticeMode.Speed -> stringResource(R.string.practice_mode_speed_subtitle)
        PracticeMode.Cross -> stringResource(R.string.practice_mode_cross_subtitle)
        PracticeMode.Mixed -> stringResource(R.string.practice_mode_mixed_subtitle)
    }

@Composable
private fun localizedPracticeModeBadge(badge: String): String =
    when (badge) {
        "Now" -> stringResource(R.string.practice_badge_now)
        "Rec" -> stringResource(R.string.practice_badge_recommended)
        "Fallback" -> stringResource(R.string.practice_badge_fallback)
        else -> badge
    }

@Composable
private fun localizedPracticeQueueSourceTitle(sourceCue: PracticeQueueSourceCue): String =
    when (sourceCue.title) {
        "Both scripts" -> stringResource(R.string.practice_queue_source_cross_title)
        "Hiragana recommended" -> stringResource(
            R.string.practice_queue_source_recommended_title,
            stringResource(R.string.script_hiragana)
        )
        "Katakana recommended" -> stringResource(
            R.string.practice_queue_source_recommended_title,
            stringResource(R.string.script_katakana)
        )
        "Hiragana" -> stringResource(R.string.script_hiragana)
        "Katakana" -> stringResource(R.string.script_katakana)
        else -> sourceCue.title
    }

@Composable
private fun localizedPracticeQueueSourceMessage(sourceCue: PracticeQueueSourceCue): String =
    when (sourceCue.message) {
        "This queue can pull hiragana and katakana together." -> stringResource(R.string.practice_queue_source_cross_message)
        "This queue follows the path recommendation for the selected script." -> stringResource(R.string.practice_queue_source_recommended_message)
        "This queue uses the currently selected script." -> stringResource(R.string.practice_queue_source_script_message)
        else -> sourceCue.message
    }

@Composable
private fun localizedPracticeQueueExplanationTitle(explanation: PracticeQueueExplanation): String =
    when (explanation.title) {
        "Nothing queued" -> stringResource(R.string.practice_queue_empty_title)
        "Due recall first" -> stringResource(R.string.practice_queue_due_title)
        "Mistake repair" -> stringResource(R.string.practice_queue_mistake_title)
        "Low-mastery fallback" -> stringResource(R.string.practice_queue_low_mastery_title)
        "Seen kana audio" -> stringResource(R.string.practice_queue_sound_seen_title)
        "Sound-safe fallback" -> stringResource(R.string.practice_queue_sound_fallback_title)
        "Lookalike focus" -> stringResource(R.string.practice_queue_lookalike_title)
        "Contrast fallback" -> stringResource(R.string.practice_queue_contrast_fallback_title)
        "Writing priority" -> stringResource(R.string.practice_queue_writing_title)
        "Fast recall" -> stringResource(R.string.practice_queue_speed_title)
        "Both scripts" -> stringResource(R.string.practice_queue_cross_title)
        "Mixed recall" -> stringResource(R.string.practice_queue_mixed_title)
        else -> explanation.title
    }

@Composable
private fun localizedPracticeQueueExplanationMessage(explanation: PracticeQueueExplanation): String =
    when (explanation.message) {
        "Start or finish a lesson first, then practice will have kana to work with." -> stringResource(R.string.practice_queue_empty_message)
        "This queue mixes due spaced-review kana with any recent mistakes." -> stringResource(R.string.practice_queue_due_message)
        "This queue starts from recent misses, then keeps low-mastery kana warm." -> stringResource(R.string.practice_queue_mistake_message)
        "No due or missed kana, so this queue uses the lowest-mastery symbols." -> stringResource(R.string.practice_queue_low_mastery_message)
        "Sound recall uses kana you have already seen at least once." -> stringResource(R.string.practice_queue_sound_seen_message)
        "No seen kana yet, so this queue previews kana that support audio prompts." -> stringResource(R.string.practice_queue_sound_fallback_message)
        "This queue targets kana with known visual confusions." -> stringResource(R.string.practice_queue_lookalike_message)
        "No lookalikes are available here, so this queue uses low-mastery kana." -> stringResource(R.string.practice_queue_contrast_fallback_message)
        "Trace practice starts with the least stable kana first." -> stringResource(R.string.practice_queue_writing_message)
        "Speed rounds prefer fluent kana, with early kana as fallback." -> stringResource(R.string.practice_queue_speed_message)
        "Cross-script practice mixes recall-ready hiragana and katakana." -> stringResource(R.string.practice_queue_cross_message)
        "Mixed practice rotates reading, sound, and writing prompts." -> stringResource(R.string.practice_queue_mixed_message)
        else -> explanation.message
    }

@Composable
private fun localizedPracticeGoalTitle(goal: PracticeSessionGoal): String =
    when (goal.title) {
        "Build a queue" -> stringResource(R.string.practice_goal_build_title)
        "Clear due recall" -> stringResource(R.string.practice_goal_due_title)
        "Repair misses" -> stringResource(R.string.practice_goal_repair_title)
        "Lift low mastery" -> stringResource(R.string.practice_goal_low_mastery_title)
        "Separate lookalikes" -> stringResource(R.string.practice_goal_lookalike_title)
        "Sharpen contrast" -> stringResource(R.string.practice_goal_contrast_title)
        "Hear before reading" -> stringResource(R.string.practice_goal_sound_title)
        "Stabilize shapes" -> stringResource(R.string.practice_goal_writing_title)
        "Keep recall fast" -> stringResource(R.string.practice_goal_speed_title)
        "Switch scripts cleanly" -> stringResource(R.string.practice_goal_cross_title)
        "Stay flexible" -> stringResource(R.string.practice_goal_mixed_title)
        else -> goal.title
    }

@Composable
private fun localizedPracticeGoalMessage(goal: PracticeSessionGoal): String =
    when (goal.message) {
        "Complete a lesson first so practice has kana to measure." -> stringResource(R.string.practice_goal_build_message)
        "Turn shaky kana into clean or repaired by the end." -> stringResource(R.string.practice_goal_repair_message)
        "Move the least stable kana toward recall." -> stringResource(R.string.practice_goal_low_mastery_message)
        "Keep confusable kana distinct through every prompt." -> stringResource(R.string.practice_goal_lookalike_message)
        "Use the fallback queue to find the next visual weakness." -> stringResource(R.string.practice_goal_contrast_message)
        "Choose kana from sound before relying on written hints." -> stringResource(R.string.practice_goal_sound_message)
        "Trace each kana only after the stroke path feels deliberate." -> stringResource(R.string.practice_goal_writing_message)
        "Answer familiar kana without slowing down for labels." -> stringResource(R.string.practice_goal_speed_message)
        "Read hiragana and katakana in one rhythm." -> stringResource(R.string.practice_goal_cross_message)
        "Rotate reading, sound, and writing without losing accuracy." -> stringResource(R.string.practice_goal_mixed_message)
        else -> {
            val reps = goal.message
                .takeIf { it.startsWith("Finish ") && it.endsWith(" reps with no missed due kana.") }
                ?.removePrefix("Finish ")
                ?.removeSuffix(" reps with no missed due kana.")
                ?.toIntOrNull()
            if (reps != null) {
                stringResource(R.string.practice_goal_due_message, reps)
            } else {
                goal.message
            }
        }
    }

@Composable
private fun localizedPracticeIntroTitle(intro: PracticeIntroCopy): String =
    when (intro.title) {
        "Due recall" -> stringResource(R.string.practice_intro_due_title)
        "Mistake repair" -> stringResource(R.string.practice_intro_mistake_title)
        "Low-mastery repair" -> stringResource(R.string.practice_intro_low_mastery_title)
        "Weak repair" -> stringResource(R.string.practice_mode_weak_title)
        "Lookalike contrast" -> stringResource(R.string.practice_mode_contrast_title)
        "Sound recall" -> stringResource(R.string.practice_mode_sound_title)
        "Writing reps" -> stringResource(R.string.practice_mode_writing_title)
        "Speed round" -> stringResource(R.string.practice_mode_speed_title)
        "Both scripts" -> stringResource(R.string.practice_mode_cross_title)
        "Mixed recall" -> stringResource(R.string.practice_mode_mixed_title)
        else -> intro.title
    }

@Composable
private fun localizedPracticeIntroSubtitle(intro: PracticeIntroCopy): String =
    when (intro.subtitle) {
        "Start with kana whose spacing has matured today." -> stringResource(R.string.practice_intro_due_subtitle)
        "Replay missed kana before they settle into the wrong shape." -> stringResource(R.string.practice_intro_mistake_subtitle)
        "Build shaky kana toward stable recall." -> stringResource(R.string.practice_intro_low_mastery_subtitle)
        "Replay misses and low-mastery kana." -> stringResource(R.string.practice_mode_weak_subtitle)
        "Separate symbols that are easy to confuse." -> stringResource(R.string.practice_mode_contrast_subtitle)
        "Choose kana from Japanese audio first." -> stringResource(R.string.practice_mode_sound_subtitle)
        "Trace symbols until the shape feels familiar." -> stringResource(R.string.practice_mode_writing_subtitle)
        "Fast recognition with familiar kana." -> stringResource(R.string.practice_mode_speed_subtitle)
        "Read hiragana and katakana in one queue." -> stringResource(R.string.practice_mode_cross_subtitle)
        "Keep familiar kana fast and automatic." -> stringResource(R.string.practice_mode_mixed_subtitle)
        else -> intro.subtitle
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
                Text(localizedPracticeCompletionMetricLabel(metric.label), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = practiceCompletionMetricToneColor(metric.toneLabel)
            ) {
                Text(
                    localizedPracticeCompletionToneLabel(metric.toneLabel),
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
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceCompletionNextStepTone"
    )
    val iconColor by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceCompletionNextStepIconTone"
    )
    val panelAlpha by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0f,
        animationSpec = kanaMotionSpec(reduceMotion, durationMillis = 220),
        label = "practiceCompletionNextStepAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (reduceMotion || entered) 1f else 0.98f,
        animationSpec = kanaMotionSpec(reduceMotion, durationMillis = 220),
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
            localizedPracticeModeLabel(mode),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CompletionKanaGroup(label: String, toneLabel: String, items: List<KanaItem>) {
    val previewLimit = 8
    val hiddenCount = (items.size - previewLimit).coerceAtLeast(0)
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
                    localizedPracticeCompletionToneLabel(toneLabel),
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
                        stringResource(R.string.practice_hidden_more, hiddenCount),
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
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = KanaElevation.Resting,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(localizedPracticeIntroTitle(intro), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(localizedPracticeIntroSubtitle(intro), style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onStart,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
            ) {
                Text(stringResource(intro.action.labelResId), fontWeight = FontWeight.Black)
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
        }
    }
}

private val PracticeIntroAction.labelResId: Int
    get() = when (this) {
        PracticeIntroAction.ReviewDue -> R.string.action_review_due
        PracticeIntroAction.RepairMistakes -> R.string.action_repair_mistakes
        PracticeIntroAction.StartRepair -> R.string.action_start_repair
        PracticeIntroAction.StartPractice -> R.string.action_start_practice
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
                Text(localizedPracticeGoalTitle(goal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedPracticeGoalMessage(goal), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PracticeHeroPanel(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = KanaElevation.Resting,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
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
            explanation = state.explanation,
            reduceMotion = reduceMotion
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
            explanation = intent.explanation,
            reduceMotion = reduceMotion
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
    explanation: PracticeQueueExplanation,
    reduceMotion: Boolean
) {
    val accentColor = practiceModeColor(mode)
    val containerColor by animateColorAsState(
        targetValue = accentColor.copy(alpha = 0.28f),
        animationSpec = kanaMotionSpec(reduceMotion),
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
                        Text(localizedPracticeModeTitle(mode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(localizedPracticeQueueSourceTitle(sourceCue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(queueSize.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PracticeQueueMetric(stringResource(R.string.metric_queue), queueSize, Modifier.weight(1f))
                    PracticeQueueMetric(stringResource(R.string.metric_weak), weakCount, Modifier.weight(1f))
                    PracticeQueueMetric(stringResource(R.string.metric_due), dueCount, Modifier.weight(1f))
                    PracticeQueueMetric(stringResource(R.string.metric_contrast), contrastCount, Modifier.weight(1f))
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
            Text(localizedPracticeQueueSourceMessage(sourceCue), style = MaterialTheme.typography.bodySmall)
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
            Text(localizedPracticeQueueExplanationTitle(explanation), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(localizedPracticeQueueExplanationMessage(explanation), style = MaterialTheme.typography.bodySmall)
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
            Text(localizedPracticeQueueExplanationTitle(explanation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(localizedPracticeQueueExplanationMessage(explanation), style = MaterialTheme.typography.bodyMedium)
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
    goal: PracticeSessionGoal,
    mode: PracticeMode,
    reduceMotion: Boolean
) {
    val accuracy by animateFloatAsState(
        targetValue = stats.accuracy,
        animationSpec = kanaMotionSpec(reduceMotion),
        label = "practiceAccuracy"
    )
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
                Text(stringResource(R.string.practice_session_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${completed % queueSize + 1}/$queueSize",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PracticeGoalLine(goal)
            if (mode == PracticeMode.Speed) {
                SpeedPaceTargetRow()
            }
            LinearProgressIndicator(progress = { accuracy }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusMetric(stringResource(R.string.metric_correct), stats.correct, Modifier.weight(1f))
                FocusMetric(stringResource(R.string.metric_missed), stats.missed, Modifier.weight(1f))
                FocusMetric(stringResource(R.string.metric_accuracy_short), (accuracy * 100).toInt(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpeedPaceTargetRow() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = practiceModeColor(PracticeMode.Speed).copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.practice_speed_target_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                stringResource(R.string.practice_speed_target_message, (SpeedPracticeTargetMillis / 1000L).toInt()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            Text(localizedPracticeModeLabel(mode), fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Medium)
                            affordance.badge?.let { badge ->
                                Text(
                                    localizedPracticeModeBadge(badge),
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
