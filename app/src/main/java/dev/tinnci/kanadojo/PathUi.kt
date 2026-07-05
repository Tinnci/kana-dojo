package dev.tinnci.kanadojo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LessonPathScreen(
    script: Script,
    mastery: Map<String, Int>,
    mistakeIds: List<String>,
    reviewDueEpochDays: Map<String, Long>,
    practiceEpochDays: Set<Long>,
    currentEpochDay: Long,
    onSpeak: (String) -> Unit,
    onEarcon: (KanaEarcon) -> Unit,
    onTaptic: (KanaTaptic) -> Unit,
    reduceMotion: Boolean,
    onOpenPractice: (PracticeMode) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val lessons = remember(script) { lessonsFor(script) }
    val scriptItems = remember(script) { itemsFor(script) }
    val snapshot = progressSnapshot(scriptItems, mastery)
    var activeLessonIndex by rememberSaveable(script) { mutableStateOf<Int?>(null) }
    var resumeCue by remember(script) { mutableStateOf<LessonResumeCue?>(null) }
    var completedLessonResult by remember(script) { mutableStateOf<CompletedLessonResult?>(null) }
    val nextLesson = nextPathLesson(lessons, mastery) ?: lessons.first()
    val activeLesson = activeLessonIndex?.let { index -> lessons.firstOrNull { it.index == index } }
    var selectedStage by rememberSaveable(script) { mutableStateOf<LearningStage?>(null) }
    val lessonStages = remember(lessons) { lessons.map { it.stage }.distinct() }
    val visibleLessons = remember(lessons, selectedStage) {
        selectedStage?.let { stage -> lessons.filter { it.stage == stage } } ?: lessons
    }
    val stageProgressCopy = pathStageProgressCopyFor(selectedStage, lessons, mastery)
    val stageEmptyStateCopy = pathStageEmptyStateCopyFor(selectedStage, lessons, mastery)
    val listedLessons = if (stageEmptyStateCopy == null) visibleLessons else emptyList()
    val mistakeSnapshot = mistakeIds.toList()
    val masterySnapshot = mastery.toMap()
    val reviewCount = remember(scriptItems, mistakeSnapshot, masterySnapshot) {
        reviewCountFor(scriptItems, mistakeSnapshot, masterySnapshot)
    }
    val dueSnapshot = reviewDueEpochDays.toMap()
    val dueReviewItems = remember(scriptItems, dueSnapshot, currentEpochDay, masterySnapshot) {
        dueReviewItemsFor(scriptItems, dueSnapshot, currentEpochDay, masterySnapshot)
    }
    val dueReviewCount = dueReviewItems.size
    val dailyRhythm = remember(practiceEpochDays, currentEpochDay) {
        dailyRhythmFor(practiceEpochDays, currentEpochDay)
    }
    val startGuidance = remember(snapshot.seen, nextLesson.stage) {
        pathStartGuidanceFor(snapshot.seen, nextLesson.stage)
    }
    val practiceRecommendation = remember(dueReviewCount, reviewCount, nextLesson.stage) {
        pathPracticeRecommendationFor(
            dueReviewCount = dueReviewCount,
            weakCount = reviewCount,
            stage = nextLesson.stage
        )
    }
    val completionFeedback = remember(completedLessonResult, nextLesson, practiceRecommendation) {
        completedLessonResult?.let { result ->
            pathCompletionFeedbackFor(
                completedLesson = result.lesson,
                stats = result.stats,
                nextLesson = nextLesson,
                practiceRecommendation = practiceRecommendation
            )
        }
    }

    if (activeLesson != null) {
        val runningLesson = activeLesson
        LessonRunner(
            lesson = runningLesson,
            allItems = itemsFor(script),
            lessons = lessons,
            mastery = mastery,
            onSpeak = onSpeak,
            onEarcon = onEarcon,
            onTaptic = onTaptic,
            reduceMotion = reduceMotion,
            onResult = onResult,
            onExit = { cue ->
                onEarcon(KanaEarcon.Navigate)
                onTaptic(KanaTaptic.Navigate)
                resumeCue = cue
                activeLessonIndex = null
            },
            onLessonComplete = { stats ->
                onEarcon(KanaEarcon.Continue)
                onTaptic(KanaTaptic.Continue)
                resumeCue = null
                completedLessonResult = CompletedLessonResult(runningLesson, stats)
                activeLessonIndex = null
            },
            onReviewMistakes = {
                onEarcon(KanaEarcon.Review)
                onTaptic(KanaTaptic.Review)
                resumeCue = null
                completedLessonResult = null
                activeLessonIndex = null
                onOpenPractice(PracticeMode.Weak)
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PathHeroPanel(
                script = script,
                nextLesson = nextLesson,
                nextLessonMastery = lessonAverageMastery(nextLesson, mastery),
                snapshot = snapshot,
                reviewCount = reviewCount,
                dueReviewCount = dueReviewCount
            )
        }
        completionFeedback?.let { feedback ->
            item {
                PathCompletionFeedbackPanel(
                    feedback = feedback,
                    onAction = {
                        completedLessonResult = null
                        when (feedback.action) {
                            PathFeedbackAction.StartLesson -> {
                                feedback.targetLessonIndex
                                    ?.let { index -> lessons.firstOrNull { it.index == index } }
                                    ?.let {
                                        onEarcon(KanaEarcon.Start)
                                        onTaptic(KanaTaptic.Start)
                                        activeLessonIndex = it.index
                                    }
                            }

                            PathFeedbackAction.OpenPractice -> {
                                onEarcon(KanaEarcon.Review)
                                onTaptic(KanaTaptic.Review)
                                onOpenPractice(feedback.practiceMode ?: PracticeMode.Weak)
                            }
                        }
                    }
                )
            }
        }
        resumeCue?.let { cue ->
            item {
                PathResumeCuePanel(
                    cue = cue,
                    onResume = {
                        lessons.firstOrNull { it.index == cue.lessonIndex }?.let { lesson ->
                            onEarcon(KanaEarcon.Start)
                            onTaptic(KanaTaptic.Start)
                            resumeCue = null
                            completedLessonResult = null
                            activeLessonIndex = lesson.index
                        }
                    }
                )
            }
        }
        item {
            DailyFocusPanel(
                lesson = nextLesson,
                averageMastery = lessonAverageMastery(nextLesson, mastery),
                snapshot = snapshot,
                reviewCount = reviewCount,
                dueReviewCount = dueReviewCount,
                dueReviewItems = dueReviewItems,
                dailyRhythm = dailyRhythm,
                startGuidance = startGuidance,
                practiceRecommendation = practiceRecommendation,
                onStart = {
                    onEarcon(KanaEarcon.Start)
                    onTaptic(KanaTaptic.Start)
                    resumeCue = null
                    completedLessonResult = null
                    activeLessonIndex = nextLesson.index
                },
                onReview = {
                    onEarcon(KanaEarcon.Review)
                    onTaptic(KanaTaptic.Review)
                    completedLessonResult = null
                    onOpenPractice(it)
                }
            )
        }
        item {
            ProgressSummaryPanel(snapshot = snapshot)
        }
        item {
            StageFilterRow(
                stages = lessonStages,
                selectedStage = selectedStage,
                progressCopy = stageProgressCopy,
                onStageChange = {
                    onEarcon(KanaEarcon.Select)
                    onTaptic(KanaTaptic.Select)
                    selectedStage = it
                }
            )
        }
        stageEmptyStateCopy?.let { copy ->
            item {
                StageEmptyStatePanel(
                    copy = copy,
                    onClear = {
                        onEarcon(KanaEarcon.Reset)
                        onTaptic(KanaTaptic.Reset)
                        selectedStage = null
                    }
                )
            }
        }
        items(listedLessons, key = { it.index }) { lesson ->
            val unlocked = isLessonUnlocked(lesson, lessons, mastery)
            val lockCopy = lessonLockCopyFor(lesson, lessons, mastery)
            val learned = lesson.items.count { (mastery[it.id] ?: 0) > 0 }
            val focusedLesson = lesson.index == nextLesson.index
            LessonNode(
                lesson = lesson,
                learned = learned,
                total = lesson.items.size,
                averageMastery = lessonAverageMastery(lesson, mastery),
                unlocked = unlocked,
                lockCopy = lockCopy,
                focus = focusedLesson,
                onStart = {
                    if (unlocked) {
                        onEarcon(if (focusedLesson) KanaEarcon.Start else KanaEarcon.Select)
                        onTaptic(if (focusedLesson) KanaTaptic.Start else KanaTaptic.Select)
                        resumeCue = null
                        completedLessonResult = null
                        activeLessonIndex = lesson.index
                    }
                }
            )
        }
    }
}

private data class CompletedLessonResult(
    val lesson: KanaLesson,
    val stats: LessonSessionStats
)

@Composable
private fun StageFilterRow(
    stages: List<LearningStage>,
    selectedStage: LearningStage?,
    progressCopy: StageProgressCopy,
    onStageChange: (LearningStage?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.path_journey_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(localizedStageProgressMessage(progressCopy.message), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    StageFilterChip(
                        label = stringResource(R.string.filter_all),
                        selected = selectedStage == null,
                        onClick = { onStageChange(null) }
                    )
                }
                items(stages) { stage ->
                    StageFilterChip(
                        label = localizedLearningStageLabel(stage),
                        selected = selectedStage == stage,
                        onClick = { onStageChange(stage) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StageFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    )
}

@Composable
private fun StageEmptyStatePanel(copy: StageEmptyStateCopy, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(localizedStageEmptyTitle(copy.title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(localizedStageEmptyMessage(copy.message), style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = onClear, shape = RoundedCornerShape(16.dp)) {
                Text(localizedStageEmptyAction(copy.actionLabel), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DailyFocusPanel(
    lesson: KanaLesson,
    averageMastery: Float,
    snapshot: ProgressSnapshot,
    reviewCount: Int,
    dueReviewCount: Int,
    dueReviewItems: List<KanaItem>,
    dailyRhythm: DailyRhythm,
    startGuidance: PathStartGuidance?,
    practiceRecommendation: PracticeRecommendation,
    onStart: () -> Unit,
    onReview: (PracticeMode) -> Unit
) {
    val phaseSummary = remember(lesson) { lessonPhaseSummaryFor(lesson) }
    val startPreview = remember(lesson) { lessonStartPreviewFor(lesson) }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lesson.items.firstOrNull()?.kana.orEmpty(), fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(localizedLessonTitle(lesson.title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        lesson.items.joinToString(" ") { it.kana },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
            }
            Text(
                stringResource(
                    R.string.path_focus_meta,
                    localizedLessonTitle(lesson.title),
                    localizedLearningStageLabel(lesson.stage),
                    localizedMasteryLabel(averageMastery)
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            startGuidance?.let { PathStartGuidancePanel(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusMetric(stringResource(R.string.metric_due), dueReviewCount, Modifier.weight(1f))
                FocusMetric(stringResource(R.string.metric_repair), reviewCount, Modifier.weight(1f))
                FocusMetric(stringResource(R.string.metric_fluent), snapshot.fluent, Modifier.weight(1f))
            }
            DailyRhythmPanel(dailyRhythm)
            LessonPhasePreviewRow(phaseSummary)
            if (dueReviewItems.isNotEmpty()) {
                DueKanaPreviewRow(dueReviewItems.take(10))
            }
            PracticeRecommendationPanel(practiceRecommendation)
            LessonStartPreviewPanel(startPreview)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_start_lesson), fontWeight = FontWeight.Black)
                }
                FilledTonalButton(
                    onClick = { onReview(practiceRecommendation.mode) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Outlined.Replay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(localizedPathRecommendationAction(practiceRecommendation), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PracticeRecommendationPanel(recommendation: PracticeRecommendation) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = practiceRecommendationColor(recommendation.mode)) {
                Text(
                    localizedPracticeModeInitial(recommendation.mode),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(localizedPathRecommendationTitle(recommendation), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedPathRecommendationMessage(recommendation), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LessonStartPreviewPanel(preview: LessonStartPreview) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(localizedLessonStartPreviewTitle(preview), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedLessonStartPreviewMessage(preview), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${preview.drillCount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PathStartGuidancePanel(guidance: PathStartGuidance) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Text(
                    "1",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(localizedPathStartGuidanceTitle(guidance), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedPathStartGuidanceMessage(guidance), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DailyRhythmPanel(rhythm: DailyRhythm) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(localizedDailyRhythmTitle(rhythm), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(localizedDailyRhythmMessage(rhythm), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${rhythm.activeDays}/7", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    rhythm.days.forEach { day ->
                        val dotColor = when {
                            day.active -> MaterialTheme.colorScheme.primary
                            day.isToday -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Box(
                            modifier = Modifier
                                .size(if (day.isToday) 10.dp else 8.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

private fun practiceRecommendationColor(mode: PracticeMode): Color =
    when (mode) {
        PracticeMode.Weak -> Color(0xFFFFDFD6)
        PracticeMode.Contrast -> Color(0xFFE7DEFF)
        PracticeMode.Sound -> Color(0xFFE2EEF8)
        PracticeMode.Writing -> Color(0xFFDCEBDD)
        PracticeMode.Speed -> Color(0xFFFFF1BC)
        PracticeMode.Cross -> Color(0xFFE2EEF8)
        PracticeMode.Mixed -> Color(0xFFDCEBDD)
    }

@Composable
private fun LessonPhasePreviewRow(phases: List<LessonPhaseCount>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.path_lesson_mix_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(phases) { phase ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(phase.count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                        Text(localizedLessonPhaseLabel(phase.label), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DueKanaPreviewRow(items: List<KanaItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.path_due_today_title), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(items, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
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
    }
}

@Composable
private fun ProgressSummaryPanel(snapshot: ProgressSnapshot) {
    val overall by animateFloatAsState(targetValue = snapshot.overall, label = "overallProgress")
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.path_today_status_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.path_today_status_message), style = MaterialTheme.typography.bodyMedium)
                }
                Text("${snapshot.fluent}/${snapshot.total}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ProgressStat(stringResource(R.string.metric_seen), snapshot.seen, Color(0xFFE2EEF8), Modifier.weight(1f))
                ProgressStat(stringResource(R.string.metric_recall), snapshot.recall, Color(0xFFFFF1BC), Modifier.weight(1f))
                ProgressStat(stringResource(R.string.metric_fluent), snapshot.fluent, Color(0xFFDCEBDD), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PathHeroPanel(
    script: Script,
    nextLesson: KanaLesson,
    nextLessonMastery: Float,
    snapshot: ProgressSnapshot,
    reviewCount: Int,
    dueReviewCount: Int
) {
    val overall by animateFloatAsState(targetValue = snapshot.overall, label = "pathHeroOverall")
    val lessonProgress by animateFloatAsState(targetValue = (nextLessonMastery / 5f).coerceIn(0f, 1f), label = "pathHeroLesson")
    val hasDueReview = dueReviewCount > 0
    val hasRepairReview = reviewCount > 0
    val heroColor by animateColorAsState(
        targetValue = when {
            hasDueReview -> Color(0xFFFFF1BC)
            hasRepairReview -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "pathHeroColor"
    )
    val priorityLabel = when {
        hasDueReview -> stringResource(R.string.path_priority_due, dueReviewCount)
        hasRepairReview -> stringResource(R.string.path_priority_repair, reviewCount)
        else -> stringResource(R.string.path_priority_next_ready)
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        color = heroColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(nextLesson.items.firstOrNull()?.kana.orEmpty(), fontSize = 38.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.path_header_title, stringResource(script.displayNameResId)),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(localizedLessonTitle(nextLesson.title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        "$priorityLabel - ${nextLesson.items.joinToString(" ") { it.kana }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMetric(stringResource(R.string.metric_due), dueReviewCount.toString(), Modifier.weight(1f))
                HeroMetric(stringResource(R.string.metric_lesson), "${(lessonProgress * 100).toInt()}%", Modifier.weight(1f))
                HeroMetric(stringResource(R.string.metric_fluent), "${snapshot.fluent}/${snapshot.total}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PathCompletionFeedbackPanel(feedback: PathCompletionFeedback, onAction: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = pathActionToneColor(feedback.tone),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(7.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(localizedPathCompletionTitle(feedback), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(localizedPathCompletionMessage(feedback), style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAction, shape = RoundedCornerShape(16.dp)) {
                Text(localizedPathCompletionAction(feedback), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun pathActionToneColor(tone: PathActionTone): Color =
    when (tone) {
        PathActionTone.Advance -> MaterialTheme.colorScheme.primaryContainer
        PathActionTone.Review -> Color(0xFFFFF1BC)
        PathActionTone.Repair -> Color(0xFFFFDFD6)
    }

@Composable
private fun PathResumeCuePanel(cue: LessonResumeCue, onResume: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(localizedPathResumeTitle(cue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(localizedPathResumeMessage(cue), style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onResume, shape = RoundedCornerShape(16.dp)) {
                    Text(localizedPathResumeAction(cue.actionLabel), fontWeight = FontWeight.Bold)
                }
            }
            LinearProgressIndicator(progress = { cue.progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LessonNode(
    lesson: KanaLesson,
    learned: Int,
    total: Int,
    averageMastery: Float,
    unlocked: Boolean,
    lockCopy: LessonLockCopy?,
    focus: Boolean,
    onStart: () -> Unit
) {
    val phaseSummary = remember(lesson) { lessonPhaseSummaryFor(lesson) }
    val phaseTotal = phaseSummary.sumOf { it.count }
    val progress by animateFloatAsState(
        targetValue = (averageMastery / 5f).coerceIn(0f, 1f),
        label = "lessonProgress"
    )
    val complete = averageMastery >= 4f
    val active = focus && unlocked && !complete
    val nodeColor by animateColorAsState(
        targetValue = when {
            complete -> MaterialTheme.colorScheme.primary
            !unlocked -> MaterialTheme.colorScheme.surfaceVariant
            focus -> MaterialTheme.colorScheme.primaryContainer
            averageMastery >= 2f -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        label = "lessonNodeColor"
    )
    val cardColor by animateColorAsState(
        targetValue = when {
            !unlocked -> MaterialTheme.colorScheme.surfaceVariant
            focus -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "lessonCardColor"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (active) 4.dp else 0.dp,
        label = "lessonCardElevation"
    )
    val nodeScale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        label = "lessonNodeScale"
    )
    val nodeContentColor = when {
        complete -> MaterialTheme.colorScheme.onPrimary
        !unlocked -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onStart,
        enabled = unlocked,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer(scaleX = nodeScale, scaleY = nodeScale)
                    .background(nodeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                when {
                    complete -> Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = nodeContentColor)
                    !unlocked -> Icon(Icons.Outlined.Lock, contentDescription = null, tint = nodeContentColor)
                    else -> Text(lesson.index.toString(), color = nodeContentColor, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localizedLessonTitle(lesson.title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (active) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                stringResource(R.string.path_next_badge),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                ) {
                    StageChip(lesson.stage)
                    DifficultyDots(lesson.difficulty)
                    Text(stringResource(R.string.path_drill_count, phaseTotal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LessonNodePhaseSummary(phaseSummary)
                Text(localizedLessonSubtitle(lesson.subtitle), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (unlocked) {
                        stringResource(R.string.path_seen_mastery_status, learned, total, localizedMasteryLabel(averageMastery))
                    } else {
                        lockCopy?.let { localizedLessonLockMessage(it.message) }.orEmpty()
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(16.dp))
            Text(
                lesson.items.joinToString(" ") { it.kana },
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LessonNodePhaseSummary(phases: List<LessonPhaseCount>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        items(phases) { phase ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(phase.count.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Text(localizedLessonPhaseShortLabel(phase.label), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun DifficultyDots(difficulty: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        if (index < difficulty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun localizedStageProgressMessage(message: String): String {
    if (!message.endsWith(" fluent")) return message
    val labelAndCount = message.removeSuffix(" fluent")
    val count = labelAndCount.substringAfterLast(" ", missingDelimiterValue = labelAndCount)
    val label = labelAndCount.substringBeforeLast(" ", missingDelimiterValue = "")
    if ("/" !in count) return message
    return if (label.isBlank()) {
        stringResource(R.string.path_stage_progress_all, count)
    } else {
        stringResource(R.string.path_stage_progress_stage, localizedLearningStageLabelText(label), count)
    }
}

@Composable
private fun localizedStageEmptyTitle(title: String): String =
    when {
        title.endsWith(" fluent") -> {
            val label = title.removeSuffix(" fluent")
            stringResource(R.string.path_stage_empty_fluent_title, localizedLearningStageLabelText(label))
        }

        title.startsWith("No open ") && title.endsWith(" lessons") -> {
            val label = title.removePrefix("No open ").removeSuffix(" lessons")
            stringResource(R.string.path_stage_empty_locked_title, localizedLearningStageLabelLower(label))
        }

        else -> title
    }

@Composable
private fun localizedStageEmptyMessage(message: String): String =
    when (message) {
        "This stage is complete. Clear the filter to continue the path." -> stringResource(R.string.path_stage_empty_complete_message)
        "Earlier rows still control this stage. Clear the filter to return to the next open lesson." -> stringResource(R.string.path_stage_empty_locked_message)
        else -> message
    }

@Composable
private fun localizedStageEmptyAction(actionLabel: String): String =
    when (actionLabel) {
        "Show all" -> stringResource(R.string.path_action_show_all)
        else -> actionLabel
    }

@Composable
private fun localizedMasteryLabel(averageMastery: Float): String =
    when {
        averageMastery >= 4f -> stringResource(R.string.path_mastery_fluent)
        averageMastery >= 3f -> stringResource(R.string.path_mastery_contrast)
        averageMastery >= 2f -> stringResource(R.string.path_mastery_recall)
        averageMastery >= 1f -> stringResource(R.string.path_mastery_familiar)
        else -> stringResource(R.string.path_mastery_new)
    }

@Composable
private fun localizedDailyRhythmTitle(rhythm: DailyRhythm): String =
    when (rhythm.title) {
        "Today touched" -> stringResource(R.string.path_daily_today_title)
        "Steady rhythm" -> stringResource(R.string.path_daily_steady_title)
        "Warm start" -> stringResource(R.string.path_daily_warm_title)
        "Fresh start" -> stringResource(R.string.path_daily_fresh_title)
        else -> rhythm.title
    }

@Composable
private fun localizedDailyRhythmMessage(rhythm: DailyRhythm): String =
    when (rhythm.message) {
        "Enough for today; review more only if it feels light." -> stringResource(R.string.path_daily_today_message)
        "${rhythm.activeDays} of 7 days active without chasing a streak." -> stringResource(R.string.path_daily_steady_message, rhythm.activeDays)
        "One short lesson keeps yesterday's recall warm." -> stringResource(R.string.path_daily_warm_message)
        "Start with a tiny queue; consistency can stay low-pressure." -> stringResource(R.string.path_daily_fresh_message)
        else -> rhythm.message
    }

@Composable
private fun localizedPathStartGuidanceTitle(guidance: PathStartGuidance): String =
    when (guidance.title) {
        "Start with sound anchors" -> stringResource(R.string.path_start_guidance_anchor_title)
        "Start at the first open node" -> stringResource(R.string.path_start_guidance_node_title)
        else -> guidance.title
    }

@Composable
private fun localizedPathStartGuidanceMessage(guidance: PathStartGuidance): String =
    when (guidance.message) {
        "Vowels make every row easier because each kana maps to one clear Japanese sound." -> stringResource(R.string.path_start_guidance_anchor_message)
        "The path keeps new kana small, then brings mistakes back before they settle." -> stringResource(R.string.path_start_guidance_node_message)
        else -> guidance.message
    }

@Composable
private fun localizedPathRecommendationTitle(recommendation: PracticeRecommendation): String =
    when (recommendation.title) {
        "Due review" -> stringResource(R.string.path_recommendation_due_title)
        "Repair weak kana" -> stringResource(R.string.path_recommendation_repair_title)
        "Contrast drill" -> stringResource(R.string.path_recommendation_contrast_title)
        "Writing reps" -> stringResource(R.string.path_recommendation_writing_title)
        "Sound recall" -> stringResource(R.string.path_recommendation_sound_title)
        else -> recommendation.title
    }

@Composable
private fun localizedPathRecommendationMessage(recommendation: PracticeRecommendation): String =
    when {
        recommendation.message.endsWith(" kana are ready for spaced recall.") -> {
            val count = recommendation.message.substringBefore(" kana").toIntOrNull() ?: return recommendation.message
            stringResource(R.string.path_recommendation_due_message, count)
        }

        recommendation.message.endsWith(" kana need a quick repair pass.") -> {
            val count = recommendation.message.substringBefore(" kana").toIntOrNull() ?: return recommendation.message
            stringResource(R.string.path_recommendation_repair_message, count)
        }

        recommendation.message == "Separate lookalike kana before the next lesson." -> stringResource(R.string.path_recommendation_contrast_message)
        recommendation.message == "Trace the shapes until the stroke pattern feels stable." -> stringResource(R.string.path_recommendation_writing_message)
        recommendation.message == "Listen first so kana map directly to Japanese sound." -> stringResource(R.string.path_recommendation_sound_message)
        else -> recommendation.message
    }

@Composable
private fun localizedPathRecommendationAction(recommendation: PracticeRecommendation): String =
    when (recommendation.actionLabel) {
        "Review due" -> stringResource(R.string.path_recommendation_due_action)
        "Repair" -> stringResource(R.string.path_recommendation_repair_action)
        "Contrast" -> stringResource(R.string.path_recommendation_contrast_action)
        "Write" -> stringResource(R.string.path_recommendation_writing_action)
        "Listen" -> stringResource(R.string.path_recommendation_sound_action)
        else -> recommendation.actionLabel
    }

@Composable
private fun localizedPracticeModeInitial(mode: PracticeMode): String =
    when (mode) {
        PracticeMode.Weak -> stringResource(R.string.practice_mode_weak_label)
        PracticeMode.Contrast -> stringResource(R.string.practice_mode_contrast_label)
        PracticeMode.Sound -> stringResource(R.string.practice_mode_sound_label)
        PracticeMode.Writing -> stringResource(R.string.practice_mode_writing_label)
        PracticeMode.Speed -> stringResource(R.string.practice_mode_speed_label)
        PracticeMode.Cross -> stringResource(R.string.practice_mode_cross_label)
        PracticeMode.Mixed -> stringResource(R.string.practice_mode_mixed_label)
    }.take(1)

@Composable
private fun localizedLessonStartPreviewTitle(preview: LessonStartPreview): String =
    when {
        preview.title.startsWith("First: ") -> stringResource(R.string.path_lesson_preview_title, localizedLessonStartLabel(preview.firstExerciseLabel))
        else -> preview.title
    }

@Composable
private fun localizedLessonStartPreviewMessage(preview: LessonStartPreview): String {
    val prefix = "Begin with "
    val delimiter = ", then continue through "
    val suffix = " short drills."
    if (!preview.message.startsWith(prefix) || !preview.message.endsWith(suffix) || delimiter !in preview.message) {
        return preview.message
    }
    val prompt = preview.message.removePrefix(prefix).substringBefore(delimiter)
    return stringResource(
        R.string.path_lesson_preview_message,
        localizedLessonStartPrompt(prompt),
        preview.drillCount
    )
}

@Composable
private fun localizedLessonStartLabel(label: String): String =
    when (label) {
        "Read it" -> stringResource(R.string.exercise_kana_to_romaji_title)
        "Find the kana" -> stringResource(R.string.exercise_romaji_to_kana_title)
        "Hear it" -> stringResource(R.string.exercise_sound_to_kana_title)
        "Match pairs" -> stringResource(R.string.exercise_pair_match_title)
        "Write it" -> stringResource(R.string.exercise_trace_title)
        else -> label
    }

@Composable
private fun localizedLessonStartPrompt(prompt: String): String =
    when {
        prompt.startsWith("choose ") && " for " in prompt -> {
            val kana = prompt.removePrefix("choose ").substringBefore(" for ")
            val romaji = prompt.substringAfter(" for ")
            stringResource(R.string.path_lesson_preview_choose, kana, romaji)
        }

        prompt.startsWith("read ") && " as " in prompt -> {
            val kana = prompt.removePrefix("read ").substringBefore(" as ")
            val romaji = prompt.substringAfter(" as ")
            stringResource(R.string.path_lesson_preview_read, kana, romaji)
        }

        prompt.startsWith("listen for ") -> stringResource(R.string.path_lesson_preview_listen, prompt.removePrefix("listen for "))
        prompt.startsWith("match ") && prompt.endsWith(" kana pairs") -> {
            val count = prompt.removePrefix("match ").removeSuffix(" kana pairs").toIntOrNull() ?: return prompt
            stringResource(R.string.path_lesson_preview_match, count)
        }

        prompt.startsWith("trace ") -> stringResource(R.string.path_lesson_preview_trace, prompt.removePrefix("trace "))
        prompt == "start the lesson" -> stringResource(R.string.path_lesson_preview_start)
        else -> prompt
    }

@Composable
private fun localizedLessonPhaseLabel(label: String): String =
    when (label) {
        "Read" -> stringResource(R.string.path_phase_read)
        "Hear" -> stringResource(R.string.path_phase_hear)
        "Match" -> stringResource(R.string.path_phase_match)
        "Write" -> stringResource(R.string.path_phase_write)
        "Contrast" -> stringResource(R.string.path_phase_contrast)
        else -> label
    }

@Composable
private fun localizedLessonPhaseShortLabel(label: String): String =
    when (label) {
        "Read" -> stringResource(R.string.path_phase_read_short)
        "Hear" -> stringResource(R.string.path_phase_hear_short)
        "Match" -> stringResource(R.string.path_phase_match_short)
        "Write" -> stringResource(R.string.path_phase_write_short)
        "Contrast" -> stringResource(R.string.path_phase_contrast_short)
        else -> label.take(1)
    }

@Composable
private fun localizedLessonTitle(title: String): String =
    when {
        title == "Vowels" -> stringResource(R.string.path_lesson_title_vowels)
        title == "Special marks" -> stringResource(R.string.path_lesson_title_special_marks)
        title.startsWith("Marks: ") && title.endsWith(" row") -> {
            val row = title.removePrefix("Marks: ").removeSuffix(" row")
            stringResource(R.string.path_lesson_title_marks, localizedLessonRowLabel(row))
        }

        title.startsWith("Blends: ") -> {
            val label = title.removePrefix("Blends: ")
            stringResource(R.string.path_lesson_title_blends, label)
        }

        title.endsWith(" row") -> {
            val row = title.removeSuffix(" row")
            stringResource(R.string.path_lesson_title_row, localizedLessonRowLabel(row))
        }

        else -> title
    }

@Composable
private fun localizedLessonRowLabel(label: String): String =
    when (label) {
        "Vowel" -> stringResource(R.string.path_lesson_row_vowel)
        "Special" -> stringResource(R.string.path_lesson_row_special)
        else -> label
    }

@Composable
private fun localizedLessonSubtitle(subtitle: String): String {
    val delimiter = ": "
    if (delimiter !in subtitle) return subtitle
    val description = subtitle.substringBefore(delimiter)
    val kanaSounds = subtitle.substringAfter(delimiter)
    return stringResource(R.string.path_lesson_subtitle, localizedStageDescription(description), kanaSounds)
}

@Composable
private fun localizedStageDescription(description: String): String =
    when (description) {
        "sound anchors" -> stringResource(R.string.path_stage_desc_anchor)
        "regular row rhythm" -> stringResource(R.string.path_stage_desc_rows)
        "stroke-heavy symbols" -> stringResource(R.string.path_stage_desc_shapes)
        "remaining base kana" -> stringResource(R.string.path_stage_desc_tail)
        "dakuten and handakuten" -> stringResource(R.string.path_stage_desc_marks)
        "small ya yu yo combinations" -> stringResource(R.string.path_stage_desc_blend)
        "small kana and length marks" -> stringResource(R.string.path_stage_desc_special)
        "lookalike separation" -> stringResource(R.string.path_stage_desc_contrast)
        else -> description
    }

@Composable
private fun localizedPathCompletionTitle(feedback: PathCompletionFeedback): String =
    when (feedback.title) {
        "Misses queued" -> stringResource(R.string.path_completion_misses_title)
        "Next lesson ready" -> stringResource(R.string.path_completion_next_title)
        else -> localizedPathRecommendationTitleText(feedback.title)
    }

@Composable
private fun localizedPathCompletionMessage(feedback: PathCompletionFeedback): String =
    when {
        feedback.message.contains(" misses changed the next action to ") -> {
            val count = feedback.message.substringBefore(" misses").toIntOrNull() ?: return feedback.message
            val actionTitle = feedback.message
                .substringAfter(" misses changed the next action to ")
                .removeSuffix(".")
            stringResource(
                R.string.path_completion_misses_message,
                count,
                localizedPathRecommendationTitleText(actionTitle)
            )
        }

        feedback.message == "Review moved ahead of the path so recall stays stable." -> stringResource(R.string.path_completion_review_message)
        feedback.message.contains(" is warm. The path now points to ") -> {
            val completedTitle = feedback.message.substringBefore(" is warm. The path now points to ")
            val nextTitle = feedback.message.substringAfter(" is warm. The path now points to ").removeSuffix(".")
            stringResource(
                R.string.path_completion_next_message,
                localizedLessonTitle(completedTitle),
                localizedLessonTitle(nextTitle)
            )
        }

        else -> feedback.message
    }

@Composable
private fun localizedPathCompletionAction(feedback: PathCompletionFeedback): String =
    when (feedback.actionLabel) {
        "Start next" -> stringResource(R.string.path_completion_start_next)
        "Review due" -> stringResource(R.string.path_recommendation_due_action)
        "Repair" -> stringResource(R.string.path_recommendation_repair_action)
        "Contrast" -> stringResource(R.string.path_recommendation_contrast_action)
        "Write" -> stringResource(R.string.path_recommendation_writing_action)
        "Listen" -> stringResource(R.string.path_recommendation_sound_action)
        else -> feedback.actionLabel
    }

@Composable
private fun localizedPathRecommendationTitleText(title: String): String =
    when (title) {
        "Due review", "due review" -> stringResource(R.string.path_recommendation_due_title)
        "Repair weak kana", "repair weak kana" -> stringResource(R.string.path_recommendation_repair_title)
        "Contrast drill", "contrast drill" -> stringResource(R.string.path_recommendation_contrast_title)
        "Writing reps", "writing reps" -> stringResource(R.string.path_recommendation_writing_title)
        "Sound recall", "sound recall" -> stringResource(R.string.path_recommendation_sound_title)
        else -> title
    }

@Composable
private fun localizedPathResumeTitle(cue: LessonResumeCue): String {
    val title = cue.title.removePrefix("Return to ")
    if (title == cue.title) return cue.title
    return stringResource(R.string.path_resume_title, localizedLessonTitle(title))
}

@Composable
private fun localizedPathResumeMessage(cue: LessonResumeCue): String {
    val prefix = "You left after "
    val delimiter = " of "
    val suffix = " drills. Return while the kana are still warm."
    if (!cue.message.startsWith(prefix) || !cue.message.endsWith(suffix) || delimiter !in cue.message) {
        return cue.message
    }
    val counts = cue.message.removePrefix(prefix).removeSuffix(suffix)
    val completed = counts.substringBefore(delimiter).toIntOrNull() ?: return cue.message
    val total = counts.substringAfter(delimiter).toIntOrNull() ?: return cue.message
    return stringResource(R.string.path_resume_message, completed, total)
}

@Composable
private fun localizedPathResumeAction(actionLabel: String): String =
    when (actionLabel) {
        "Return" -> stringResource(R.string.path_resume_action)
        else -> actionLabel
    }

@Composable
private fun localizedLessonLockMessage(message: String): String {
    val prefix = "Need "
    val suffix = " recall 2"
    if (!message.startsWith(prefix) || !message.endsWith(suffix)) return message
    val lessonTitle = message.removePrefix(prefix).removeSuffix(suffix)
    return stringResource(R.string.path_lock_message, localizedLessonTitle(lessonTitle))
}
