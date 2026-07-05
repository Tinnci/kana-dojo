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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    reduceMotion: Boolean,
    onOpenPractice: (PracticeMode) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val lessons = remember(script) { lessonsFor(script) }
    val scriptItems = remember(script) { itemsFor(script) }
    val snapshot = progressSnapshot(scriptItems, mastery)
    var activeLesson by remember(script) { mutableStateOf<KanaLesson?>(null) }
    var resumeCue by remember(script) { mutableStateOf<LessonResumeCue?>(null) }
    var completedLessonResult by remember(script) { mutableStateOf<CompletedLessonResult?>(null) }
    var selectedStage by remember(script) { mutableStateOf<LearningStage?>(null) }
    val nextLesson = nextPathLesson(lessons, mastery) ?: lessons.first()
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
        val runningLesson = activeLesson!!
        LessonRunner(
            lesson = runningLesson,
            allItems = itemsFor(script),
            lessons = lessons,
            mastery = mastery,
            onSpeak = onSpeak,
            reduceMotion = reduceMotion,
            onResult = onResult,
            onExit = { cue ->
                resumeCue = cue
                activeLesson = null
            },
            onLessonComplete = { stats ->
                resumeCue = null
                completedLessonResult = CompletedLessonResult(runningLesson, stats)
                activeLesson = null
            },
            onReviewMistakes = {
                resumeCue = null
                completedLessonResult = null
                activeLesson = null
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
                                    ?.let { activeLesson = it }
                            }

                            PathFeedbackAction.OpenPractice -> {
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
                            resumeCue = null
                            completedLessonResult = null
                            activeLesson = lesson
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
                    resumeCue = null
                    completedLessonResult = null
                    activeLesson = nextLesson
                },
                onReview = {
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
                onStageChange = { selectedStage = it }
            )
        }
        stageEmptyStateCopy?.let { copy ->
            item {
                StageEmptyStatePanel(
                    copy = copy,
                    onClear = { selectedStage = null }
                )
            }
        }
        items(listedLessons, key = { it.index }) { lesson ->
            val unlocked = isLessonUnlocked(lesson, lessons, mastery)
            val lockCopy = lessonLockCopyFor(lesson, lessons, mastery)
            val learned = lesson.items.count { (mastery[it.id] ?: 0) > 0 }
            LessonNode(
                lesson = lesson,
                learned = learned,
                total = lesson.items.size,
                averageMastery = lessonAverageMastery(lesson, mastery),
                unlocked = unlocked,
                lockCopy = lockCopy,
                focus = lesson.index == nextLesson.index,
                onStart = {
                    if (unlocked) {
                        resumeCue = null
                        completedLessonResult = null
                        activeLesson = lesson
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
                Text("Journey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(progressCopy.message, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    StageFilterChip(
                        label = "All",
                        selected = selectedStage == null,
                        onClick = { onStageChange(null) }
                    )
                }
                items(stages) { stage ->
                    StageFilterChip(
                        label = stage.label,
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
                Text(copy.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(copy.message, style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = onClear, shape = RoundedCornerShape(16.dp)) {
                Text(copy.actionLabel, fontWeight = FontWeight.Bold)
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
        color = MaterialTheme.colorScheme.tertiaryContainer,
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
                    Text("Today's focus", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(lesson.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "${lesson.stage.label} - ${masteryLabel(averageMastery)} - ${lesson.items.joinToString(" ") { it.kana }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            startGuidance?.let { PathStartGuidancePanel(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusMetric("Due", dueReviewCount, Modifier.weight(1f))
                FocusMetric("Repair", reviewCount, Modifier.weight(1f))
                FocusMetric("Fluent", snapshot.fluent, Modifier.weight(1f))
            }
            DailyRhythmPanel(dailyRhythm)
            LessonPhasePreviewRow(phaseSummary)
            if (dueReviewItems.isNotEmpty()) {
                DueKanaPreviewRow(dueReviewItems.take(10))
            }
            PracticeRecommendationPanel(practiceRecommendation)
            LessonStartPreviewPanel(startPreview)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start")
                }
                FilledTonalButton(
                    onClick = { onReview(practiceRecommendation.mode) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Replay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(practiceRecommendation.actionLabel)
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
                    recommendation.mode.label.take(1),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(recommendation.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(recommendation.message, style = MaterialTheme.typography.bodySmall)
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
                Text(preview.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(preview.message, style = MaterialTheme.typography.bodySmall)
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
                Text(guidance.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(guidance.message, style = MaterialTheme.typography.bodySmall)
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
                Text(rhythm.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                Text(rhythm.message, style = MaterialTheme.typography.bodySmall)
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
        Text("Lesson mix", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                        Text(phase.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DueKanaPreviewRow(items: List<KanaItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Due today", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                    Text("Today status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Fluency grows when recall survives review.", style = MaterialTheme.typography.bodyMedium)
                }
                Text("${snapshot.fluent}/${snapshot.total}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ProgressStat("Seen", snapshot.seen, Color(0xFFE2EEF8), Modifier.weight(1f))
                ProgressStat("Recall", snapshot.recall, Color(0xFFFFF1BC), Modifier.weight(1f))
                ProgressStat("Fluent", snapshot.fluent, Color(0xFFDCEBDD), Modifier.weight(1f))
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
        hasDueReview -> "$dueReviewCount due today"
        hasRepairReview -> "$reviewCount to repair"
        else -> "Next lesson ready"
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
                    Text("${script.label} path", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(nextLesson.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        "$priorityLabel - ${nextLesson.items.joinToString(" ") { it.kana }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMetric("Due", dueReviewCount.toString(), Modifier.weight(1f))
                HeroMetric("Lesson", "${(lessonProgress * 100).toInt()}%", Modifier.weight(1f))
                HeroMetric("Fluent", "${snapshot.fluent}/${snapshot.total}", Modifier.weight(1f))
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
                Text(feedback.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(feedback.message, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAction, shape = RoundedCornerShape(16.dp)) {
                Text(feedback.actionLabel, fontWeight = FontWeight.Bold)
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
                    Text(cue.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text(cue.message, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onResume, shape = RoundedCornerShape(16.dp)) {
                    Text(cue.actionLabel, fontWeight = FontWeight.Bold)
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
                    Text(lesson.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (active) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "Next",
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
                    Text("$phaseTotal drills", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LessonNodePhaseSummary(phaseSummary)
                Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (unlocked) "$learned/$total seen - ${masteryLabel(averageMastery)}" else lockCopy?.message.orEmpty(),
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
                    Text(phase.label.take(1), style = MaterialTheme.typography.labelSmall)
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
