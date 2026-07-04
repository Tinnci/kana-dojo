package dev.tinnci.kanadojo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KanaDojoApp()
        }
    }
}

@Composable
private fun KanaDojoApp() {
    val context = LocalContext.current
    val allItems = remember { hiraganaItems + katakanaItems }
    val progressStore = remember { ProgressStore(context) }
    val mastery = remember { mutableStateMapOf<String, Int>() }
    val mistakes = remember { mutableStateListOf<String>() }
    var selectedScript by remember { mutableStateOf(Script.Hiragana) }
    var currentTab by remember { mutableStateOf(ScreenTab.Lessons) }

    LaunchedEffect(Unit) {
        mastery.putAll(progressStore.loadMastery(allItems))
        mistakes.clear()
        mistakes.addAll(progressStore.loadMistakes())
    }

    val tts = rememberKanaSpeech()
    KanaTheme {
        Scaffold(
            topBar = {
                KanaTopBar(selectedScript = selectedScript, onScriptChange = { selectedScript = it })
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    ScreenTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        ScreenTab.Lessons -> Icons.Outlined.School
                                        ScreenTab.Chart -> Icons.Outlined.GridView
                                        ScreenTab.Mistakes -> Icons.Outlined.Replay
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = currentTab,
                label = "screen",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) { tab ->
                when (tab) {
                    ScreenTab.Lessons -> LessonPathScreen(
                        script = selectedScript,
                        mastery = mastery,
                        mistakeIds = mistakes,
                        onSpeak = tts::speak,
                        onOpenPractice = { currentTab = ScreenTab.Mistakes },
                        onResult = { items, correct ->
                            progressStore.mark(items, correct)
                            mastery.putAll(progressStore.loadMastery(allItems))
                            mistakes.clear()
                            mistakes.addAll(progressStore.loadMistakes())
                        }
                    )

                    ScreenTab.Chart -> KanaChartScreen(
                        script = selectedScript,
                        mastery = mastery,
                        onSpeak = tts::speak
                    )

                    ScreenTab.Mistakes -> MistakePracticeScreen(
                        script = selectedScript,
                        allItems = allItems,
                        mistakeIds = mistakes,
                        mastery = mastery,
                        onSpeak = tts::speak,
                        onResult = { items, correct ->
                            progressStore.mark(items, correct)
                            mastery.putAll(progressStore.loadMastery(allItems))
                            mistakes.clear()
                            mistakes.addAll(progressStore.loadMistakes())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberKanaSpeech(): KanaSpeech {
    val context = LocalContext.current
    val speech = remember { KanaSpeech(context) }
    DisposableEffect(Unit) {
        onDispose { speech.shutdown() }
    }
    return speech
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KanaTopBar(selectedScript: Script, onScriptChange: (Script) -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Column {
                Text("Kana Dojo", fontWeight = FontWeight.Black)
                Text("guided kana drills", style = MaterialTheme.typography.labelMedium)
            }
        },
        actions = {
            Script.entries.forEach { script ->
                AssistChip(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = { onScriptChange(script) },
                    label = { Text(script.label) },
                    leadingIcon = {
                        if (script == selectedScript) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun LessonPathScreen(
    script: Script,
    mastery: Map<String, Int>,
    mistakeIds: List<String>,
    onSpeak: (String) -> Unit,
    onOpenPractice: () -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val lessons = remember(script) { lessonsFor(script) }
    val scriptItems = remember(script) { itemsFor(script) }
    val snapshot = progressSnapshot(scriptItems, mastery)
    var activeLesson by remember(script) { mutableStateOf<KanaLesson?>(null) }
    var selectedStage by remember(script) { mutableStateOf<LearningStage?>(null) }
    val nextLesson = lessons.firstOrNull { lesson ->
        isLessonUnlocked(lesson, lessons, mastery) && lessonAverageMastery(lesson, mastery) < 4f
    } ?: lessons.first()
    val lessonStages = remember(lessons) { lessons.map { it.stage }.distinct() }
    val visibleLessons = remember(lessons, selectedStage) {
        selectedStage?.let { stage -> lessons.filter { it.stage == stage } } ?: lessons
    }
    val mistakeSnapshot = mistakeIds.toList()
    val masterySnapshot = mastery.toMap()
    val reviewCount = remember(scriptItems, mistakeSnapshot, masterySnapshot) {
        reviewCountFor(scriptItems, mistakeSnapshot, masterySnapshot)
    }

    if (activeLesson != null) {
        LessonRunner(
            lesson = activeLesson!!,
            allItems = itemsFor(script),
            onSpeak = onSpeak,
            onResult = onResult,
            onExit = { activeLesson = null },
            onReviewMistakes = {
                activeLesson = null
                onOpenPractice()
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
                reviewCount = reviewCount
            )
        }
        item {
            DailyFocusPanel(
                lesson = nextLesson,
                averageMastery = lessonAverageMastery(nextLesson, mastery),
                snapshot = snapshot,
                reviewCount = reviewCount,
                onStart = { activeLesson = nextLesson },
                onReview = onOpenPractice
            )
        }
        item {
            ProgressSummaryPanel(snapshot = snapshot)
        }
        item {
            StageFilterRow(
                stages = lessonStages,
                selectedStage = selectedStage,
                visibleCount = visibleLessons.size,
                totalCount = lessons.size,
                onStageChange = { selectedStage = it }
            )
        }
        items(visibleLessons, key = { it.index }) { lesson ->
            val unlocked = isLessonUnlocked(lesson, lessons, mastery)
            val learned = lesson.items.count { (mastery[it.id] ?: 0) > 0 }
            LessonNode(
                lesson = lesson,
                learned = learned,
                total = lesson.items.size,
                averageMastery = lessonAverageMastery(lesson, mastery),
                unlocked = unlocked,
                focus = lesson.index == nextLesson.index,
                onStart = { if (unlocked) activeLesson = lesson }
            )
        }
    }
}

@Composable
private fun StageFilterRow(
    stages: List<LearningStage>,
    selectedStage: LearningStage?,
    visibleCount: Int,
    totalCount: Int,
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
                Text("$visibleCount/$totalCount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun DailyFocusPanel(
    lesson: KanaLesson,
    averageMastery: Float,
    snapshot: ProgressSnapshot,
    reviewCount: Int,
    onStart: () -> Unit,
    onReview: () -> Unit
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusMetric("Review", reviewCount, Modifier.weight(1f))
                FocusMetric("Fluent", snapshot.fluent, Modifier.weight(1f))
                FocusMetric("Left", (snapshot.total - snapshot.fluent).coerceAtLeast(0), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start")
                }
                FilledTonalButton(onClick = onReview, enabled = reviewCount > 0, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Replay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Review")
                }
            }
        }
    }
}

@Composable
private fun FocusMetric(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall)
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
private fun ProgressStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = color, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PathHeroPanel(
    script: Script,
    nextLesson: KanaLesson,
    nextLessonMastery: Float,
    snapshot: ProgressSnapshot,
    reviewCount: Int
) {
    val overall by animateFloatAsState(targetValue = snapshot.overall, label = "pathHeroOverall")
    val lessonProgress by animateFloatAsState(targetValue = (nextLessonMastery / 5f).coerceIn(0f, 1f), label = "pathHeroLesson")
    val heroColor by animateColorAsState(
        targetValue = if (reviewCount > 0) Color(0xFFFFF1BC) else MaterialTheme.colorScheme.primaryContainer,
        label = "pathHeroColor"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        color = heroColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(heroColor, Color(0xFFE2EEF8))))
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
                        "Next: ${nextLesson.items.joinToString(" ") { it.kana }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            LinearProgressIndicator(progress = { overall }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HeroMetric("Lesson", "${(lessonProgress * 100).toInt()}%", Modifier.weight(1f))
                HeroMetric("Fluent", "${snapshot.fluent}/${snapshot.total}", Modifier.weight(1f))
                HeroMetric("Review", reviewCount.toString(), Modifier.weight(1f))
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
private fun HeroPanel(title: String, subtitle: String) {
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
private fun LessonNode(
    lesson: KanaLesson,
    learned: Int,
    total: Int,
    averageMastery: Float,
    unlocked: Boolean,
    focus: Boolean,
    onStart: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = (averageMastery / 5f).coerceIn(0f, 1f),
        label = "lessonProgress"
    )
    val complete = averageMastery >= 4f
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
        targetValue = if (focus && unlocked && !complete) 5.dp else 2.dp,
        label = "lessonCardElevation"
    )
    val nodeScale by animateFloatAsState(
        targetValue = if (focus && unlocked && !complete) 1.06f else 1f,
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
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
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
                Text(lesson.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                ) {
                    StageChip(lesson.stage)
                    DifficultyDots(lesson.difficulty)
                }
                Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (unlocked) "$learned/$total seen - ${masteryLabel(averageMastery)}" else "Reach recall level 2 in the previous row",
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
private fun StageChip(stage: LearningStage) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (stage) {
            LearningStage.Anchor -> Color(0xFFDCEBDD)
            LearningStage.RegularRows -> Color(0xFFFFF1BC)
            LearningStage.ShapeHeavy -> Color(0xFFFFDFD6)
            LearningStage.TailRows -> Color(0xFFE7DEFF)
            LearningStage.Voiced -> Color(0xFFE2EEF8)
            LearningStage.Combination -> Color(0xFFFFF1BC)
            LearningStage.Special -> Color(0xFFFFDFD6)
            LearningStage.Confusable -> Color(0xFFE2EEF8)
        }
    ) {
        Text(
            stage.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
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
private fun LessonRunner(
    lesson: KanaLesson,
    allItems: List<KanaItem>,
    onSpeak: (String) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit,
    onExit: () -> Unit,
    onReviewMistakes: () -> Unit
) {
    val queue = remember(lesson) { mutableStateListOf<Exercise>().apply { addAll(buildLessonExercises(lesson)) } }
    var completed by remember(lesson) { mutableIntStateOf(0) }
    var sessionStats by remember(lesson) { mutableStateOf(LessonSessionStats()) }
    var feedback by remember(lesson) { mutableStateOf<AnswerFeedback?>(null) }
    val current = queue.firstOrNull()
    val total = (completed + queue.size).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) {
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
                stats = sessionStats,
                onContinue = onExit,
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
    stats: LessonSessionStats,
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
        targetValue = if (entered) 1f else 0.88f,
        label = "completionBadgeScale"
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
        CompletionSparkRow(visible = entered, cleanRun = stats.missed == 0)
        Spacer(Modifier.height(8.dp))
        StageChip(lesson.stage)
        Spacer(Modifier.height(18.dp))
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
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
        CompletionActions(stats = stats, onContinue = onContinue, onRepeat = onRepeat, onReviewMistakes = onReviewMistakes)
    }
}

@Composable
private fun CompletionActions(
    stats: LessonSessionStats,
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

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    allItems: List<KanaItem>,
    onSpeak: (String) -> Unit,
    feedback: AnswerFeedback?,
    onAnswer: (Boolean) -> Unit,
    onContinue: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxSize()
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
                        prompt = exercise.items.first().kana,
                        promptIsKana = true,
                        options = romajiOptions(exercise.items.first(), allItems),
                        correct = exercise.items.first().romaji,
                        onSpeak = { onSpeak(exercise.items.first().kana) },
                        onAnswer = onAnswer
                    )

                    ExerciseKind.RomajiToKana -> ChoiceExercise(
                        prompt = exercise.items.first().romaji,
                        promptIsKana = false,
                        options = kanaOptions(exercise.items.first(), allItems),
                        correct = exercise.items.first().kana,
                        onSpeak = { onSpeak(exercise.items.first().kana) },
                        onAnswer = onAnswer
                    )

                    ExerciseKind.SoundToKana -> SoundChoiceExercise(
                        item = exercise.items.first(),
                        options = kanaOptions(exercise.items.first(), allItems),
                        onSpeak = onSpeak,
                        onAnswer = onAnswer
                    )

                    ExerciseKind.PairMatch -> PairMatchExercise(
                        items = exercise.items,
                        answered = feedback != null,
                        onAnswer = onAnswer
                    )

                    ExerciseKind.TraceKana -> TraceKanaExercise(
                        item = exercise.items.first(),
                        answered = feedback != null,
                        onSpeak = onSpeak,
                        onAnswer = onAnswer
                    )
                }
            }
            AnimatedVisibility(
                visible = feedback != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top) + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top) + scaleOut(targetScale = 0.96f)
            ) {
                feedback?.let {
                    FeedbackBanner(feedback = it)
                }
            }
            AnimatedVisibility(
                visible = feedback != null,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseHeader(kind: ExerciseKind) {
    val (title, subtitle) = when (kind) {
        ExerciseKind.KanaToRomaji -> "Read it" to "Choose the matching sound."
        ExerciseKind.RomajiToKana -> "Find the kana" to "Choose the symbol for this sound."
        ExerciseKind.SoundToKana -> "Hear it" to "Choose the kana you heard."
        ExerciseKind.PairMatch -> "Match pairs" to "Pair each kana with its sound."
        ExerciseKind.TraceKana -> "Write it" to "Trace the kana and check your stroke shape."
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
                Text(if (feedback.correct) "Nice. Keep going." else "Correct answer: ${feedback.answer}", fontWeight = FontWeight.Bold)
                if (!feedback.correct) {
                    Text("This will come back soon.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ChoiceExercise(
    prompt: String,
    promptIsKana: Boolean,
    options: List<String>,
    correct: String,
    onSpeak: () -> Unit,
    onAnswer: (Boolean) -> Unit
) {
    var selectedOption by remember(prompt, correct) { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                prompt,
                fontSize = if (promptIsKana) 108.sp else 56.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            FilledTonalButton(onClick = onSpeak) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Hear it")
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
                    onClick = {
                        if (selectedOption == null) {
                            selectedOption = option
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
    onAnswer: (Boolean) -> Unit
) {
    var selectedOption by remember(item.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(item.id) {
        onSpeak(item.kana)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(
                onClick = { onSpeak(item.kana) },
                shape = CircleShape,
                modifier = Modifier.size(116.dp)
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Play sound", modifier = Modifier.size(52.dp))
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
    onClick: () -> Unit
) {
    val targetColor = when {
        answered && correct -> Color(0xFFDCEBDD)
        answered && selected -> Color(0xFFFFDFD6)
        else -> MaterialTheme.colorScheme.surface
    }
    val containerColor by animateColorAsState(targetValue = targetColor, label = "answerOptionColor")
    val scale by animateFloatAsState(
        targetValue = when {
            answered && correct -> 1.02f
            answered && selected -> 0.98f
            else -> 1f
        },
        label = "answerOptionScale"
    )

    ElevatedButton(
        onClick = onClick,
        enabled = !answered,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.elevatedButtonColors(
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
private fun PairMatchExercise(items: List<KanaItem>, answered: Boolean, onAnswer: (Boolean) -> Unit) {
    var selectedKana by remember { mutableStateOf<KanaItem?>(null) }
    var selectedRomaji by remember { mutableStateOf<KanaItem?>(null) }
    val matched = remember { mutableStateListOf<String>() }
    val kanaColumn = remember(items) { items.shuffled(Random(lessonSeed(items))) }
    val romajiColumn = remember(items) { items.shuffled(Random(lessonSeed(items) + 9)) }

    LaunchedEffect(selectedKana, selectedRomaji) {
        val kana = selectedKana
        val romaji = selectedRomaji
        if (kana != null && romaji != null) {
            if (kana.id == romaji.id) matched.add(kana.id)
            selectedKana = null
            selectedRomaji = null
            if (matched.size == items.size && !answered) onAnswer(true)
        }
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MatchColumn(
            entries = kanaColumn,
            matched = matched,
            selected = selectedKana,
            label = { it.kana },
            onSelect = { if (!answered) selectedKana = it },
            modifier = Modifier.weight(1f)
        )
        MatchColumn(
            entries = romajiColumn,
            matched = matched,
            selected = selectedRomaji,
            label = { it.romaji },
            onSelect = { if (!answered) selectedRomaji = it },
            modifier = Modifier.weight(1f)
        )
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

@Composable
private fun KanaChartScreen(script: Script, mastery: Map<String, Int>, onSpeak: (String) -> Unit) {
    val items = remember(script) { itemsFor(script) }
    val rows = remember(items) { items.map { it.row }.distinct() }
    var selectedRow by remember(script) { mutableStateOf<String?>(null) }
    val visibleItems = remember(items, selectedRow) {
        selectedRow?.let { row -> items.filter { it.row == row } } ?: items
    }
    val snapshot = progressSnapshot(items, mastery)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 86.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChartHeader(script = script, snapshot = snapshot)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChartRowFilters(rows = rows, selectedRow = selectedRow, onRowChange = { selectedRow = it })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            MasteryLegend()
        }
        items(visibleItems) { item ->
            val level = mastery[item.id] ?: 0
            Card(
                onClick = { onSpeak(item.kana) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = chartTileColor(level, item.confusable.isNotEmpty()))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (item.confusable.isNotEmpty()) 1.dp else 0.dp,
                            color = if (item.confusable.isNotEmpty()) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.kana, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text(item.romaji, style = MaterialTheme.typography.labelLarge)
                    Text(item.row, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (item.confusable.isNotEmpty()) {
                        Text("contrast", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                    MasteryPips(level = level)
                }
            }
        }
    }
}

@Composable
private fun ChartHeader(script: Script, snapshot: ProgressSnapshot) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${script.label} chart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Tap any kana to hear it. Contrast marks show lookalikes.", style = MaterialTheme.typography.bodyMedium)
            }
            Text("${snapshot.fluent}/${snapshot.total}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ChartRowFilters(rows: List<String>, selectedRow: String?, onRowChange: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        item {
            AssistChip(
                onClick = { onRowChange(null) },
                label = { Text("All") },
                leadingIcon = {
                    if (selectedRow == null) Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
        items(rows) { row ->
            AssistChip(
                onClick = { onRowChange(row) },
                label = { Text(row) },
                leadingIcon = {
                    if (selectedRow == row) Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

@Composable
private fun MasteryLegend() {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item { LegendSwatch("new", MaterialTheme.colorScheme.surface) }
            item { LegendSwatch("learning", Color(0xFFFFF1BC)) }
            item { LegendSwatch("fluent", Color(0xFFDCEBDD)) }
            item { LegendSwatch("contrast", Color(0xFFE7DEFF)) }
        }
    }
}

@Composable
private fun LegendSwatch(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun chartTileColor(level: Int, confusable: Boolean): Color =
    when {
        level >= 4 -> Color(0xFFDCEBDD)
        level >= 2 -> Color(0xFFFFF1BC)
        confusable -> Color(0xFFE7DEFF)
        else -> MaterialTheme.colorScheme.surface
    }

@Composable
private fun MasteryPips(level: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (index < level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun MistakePracticeScreen(
    script: Script,
    allItems: List<KanaItem>,
    mistakeIds: List<String>,
    mastery: Map<String, Int>,
    onSpeak: (String) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val scriptItems = remember(script) { itemsFor(script) }
    var selectedMode by remember(script) { mutableStateOf(PracticeMode.Weak) }
    val mistakeSnapshot = mistakeIds.toList()
    val masterySnapshot = mastery.toMap()
    val practiceItems = remember(script, selectedMode, mistakeSnapshot, masterySnapshot) {
        practiceItemsFor(
            mode = selectedMode,
            scriptItems = scriptItems,
            mistakeIds = mistakeSnapshot,
            allItems = allItems,
            mastery = masterySnapshot
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
        HeroPanel(selectedMode.title, selectedMode.subtitle)
        PracticeModeTabs(selectedMode = selectedMode, onModeChange = { selectedMode = it })
        PracticeQueuePanel(
            mode = selectedMode,
            queueLabel = queueLabel,
            queueSize = practiceItems.size,
            weakCount = weakCount,
            contrastCount = contrastCount
        )
        PracticeSessionPanel(
            stats = sessionStats,
            completed = currentIndex,
            queueSize = practiceItems.size
        )
        ExerciseCard(
            exercise = exercise,
            allItems = optionItems,
            onSpeak = onSpeak,
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
private fun PracticeQueuePanel(
    mode: PracticeMode,
    queueLabel: String,
    queueSize: Int,
    weakCount: Int,
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
