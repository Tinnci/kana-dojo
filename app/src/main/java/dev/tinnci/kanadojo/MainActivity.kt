package dev.tinnci.kanadojo

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.hypot
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KanaDojoApp()
        }
    }
}

private enum class Script(val label: String) {
    Hiragana("Hiragana"),
    Katakana("Katakana")
}

private enum class ScreenTab(val label: String) {
    Lessons("Lessons"),
    Chart("Chart"),
    Mistakes("Mistakes")
}

private enum class ExerciseKind {
    KanaToRomaji,
    RomajiToKana,
    PairMatch,
    TraceKana
}

private data class KanaItem(
    val id: String,
    val script: Script,
    val kana: String,
    val romaji: String,
    val row: String,
    val lesson: Int,
    val confusable: List<String> = emptyList()
)

private data class KanaLesson(
    val index: Int,
    val title: String,
    val subtitle: String,
    val items: List<KanaItem>
)

private data class Exercise(
    val kind: ExerciseKind,
    val items: List<KanaItem>
)

private class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("kana_progress", Context.MODE_PRIVATE)

    fun loadMastery(items: List<KanaItem>): Map<String, Int> =
        items.associate { it.id to prefs.getInt("mastery:${it.id}", 0) }

    fun loadMistakes(): Set<String> =
        prefs.getStringSet("mistakes", emptySet()).orEmpty()

    fun mark(items: List<KanaItem>, correct: Boolean) {
        val editor = prefs.edit()
        val currentMistakes = loadMistakes().toMutableSet()
        items.forEach { item ->
            val key = "mastery:${item.id}"
            val next = (prefs.getInt(key, 0) + if (correct) 1 else -1).coerceIn(0, 5)
            editor.putInt(key, next)
            if (correct && next >= 2) currentMistakes.remove(item.id) else if (!correct) currentMistakes.add(item.id)
        }
        editor.putStringSet("mistakes", currentMistakes).apply()
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
                        onSpeak = tts::speak,
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

private class KanaSpeech(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale.JAPANESE
            tts.setSpeechRate(0.75f)
        }
    }

    fun speak(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kana-$text")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
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
    onSpeak: (String) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val lessons = remember(script) { lessonsFor(script) }
    var activeLesson by remember(script) { mutableStateOf<KanaLesson?>(null) }

    if (activeLesson != null) {
        LessonRunner(
            lesson = activeLesson!!,
            allItems = itemsFor(script),
            onSpeak = onSpeak,
            onResult = onResult,
            onExit = { activeLesson = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroPanel(
                title = "${script.label} path",
                subtitle = "Tiny lessons. Quick drills. Writing practice before moving on."
            )
        }
        items(lessons) { lesson ->
            val learned = lesson.items.count { (mastery[it.id] ?: 0) > 0 }
            LessonNode(
                lesson = lesson,
                learned = learned,
                total = lesson.items.size,
                onStart = { activeLesson = lesson }
            )
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
private fun LessonNode(lesson: KanaLesson, learned: Int, total: Int, onStart: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else learned / total.toFloat(),
        label = "lessonProgress"
    )
    Card(
        onClick = onStart,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(lesson.index.toString(), fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(lesson.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(16.dp))
            Text(lesson.items.joinToString(" ") { it.kana }, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LessonRunner(
    lesson: KanaLesson,
    allItems: List<KanaItem>,
    onSpeak: (String) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit,
    onExit: () -> Unit
) {
    val queue = remember(lesson) { mutableStateListOf<Exercise>().apply { addAll(buildLessonExercises(lesson)) } }
    var completed by remember(lesson) { mutableIntStateOf(0) }
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
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
        }

        if (current == null) {
            LessonComplete(lesson = lesson, onContinue = onExit)
        } else {
            ExerciseCard(
                exercise = current,
                allItems = allItems,
                onSpeak = onSpeak,
                feedback = lastCorrect,
                onAnswer = { correct ->
                    lastCorrect = correct
                    onResult(current.items, correct)
                    queue.removeAt(0)
                    completed += 1
                    if (!correct) {
                        queue.add(buildMistakeExercise(current.items.first()))
                    }
                }
            )
        }
    }
}

@Composable
private fun LessonComplete(lesson: KanaLesson, onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("済", fontSize = 44.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(20.dp))
        Text("Lesson complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(lesson.items.joinToString("  ") { "${it.kana} ${it.romaji}" }, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onContinue) {
            Text("Continue")
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    allItems: List<KanaItem>,
    onSpeak: (String) -> Unit,
    feedback: Boolean?,
    onAnswer: (Boolean) -> Unit
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
            AnimatedVisibility(visible = feedback != null) {
                FeedbackBanner(correct = feedback == true)
            }
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

                ExerciseKind.PairMatch -> PairMatchExercise(items = exercise.items, onAnswer = onAnswer)
                ExerciseKind.TraceKana -> TraceKanaExercise(item = exercise.items.first(), onSpeak = onSpeak, onAnswer = onAnswer)
            }
        }
    }
}

@Composable
private fun ExerciseHeader(kind: ExerciseKind) {
    val (title, subtitle) = when (kind) {
        ExerciseKind.KanaToRomaji -> "Read it" to "Choose the matching sound."
        ExerciseKind.RomajiToKana -> "Find the kana" to "Choose the symbol for this sound."
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
private fun FeedbackBanner(correct: Boolean) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (correct) Color(0xFFDCEBDD) else Color(0xFFFFDFD6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (correct) Icons.Outlined.CheckCircle else Icons.Outlined.Replay,
                contentDescription = null,
                tint = if (correct) Color(0xFF2F5D50) else Color(0xFF9B2D20)
            )
            Spacer(Modifier.width(10.dp))
            Text(if (correct) "Nice. Keep going." else "Try this one again soon.", fontWeight = FontWeight.Bold)
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
                ElevatedButton(
                    onClick = { onAnswer(option == correct) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                ) {
                    Text(option, fontSize = if (option.length == 1) 34.sp else 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PairMatchExercise(items: List<KanaItem>, onAnswer: (Boolean) -> Unit) {
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
            if (matched.size == items.size) onAnswer(true)
        }
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MatchColumn(
            entries = kanaColumn,
            matched = matched,
            selected = selectedKana,
            label = { it.kana },
            onSelect = { selectedKana = it },
            modifier = Modifier.weight(1f)
        )
        MatchColumn(
            entries = romajiColumn,
            matched = matched,
            selected = selectedRomaji,
            label = { it.romaji },
            onSelect = { selectedRomaji = it },
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
private fun TraceKanaExercise(item: KanaItem, onSpeak: (String) -> Unit, onAnswer: (Boolean) -> Unit) {
    var points by remember(item.id) { mutableStateOf<List<Offset>>(emptyList()) }
    val pathLength = remember(points) { points.zipWithNext().sumOf { (a, b) -> hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()) }.toFloat() }
    val hasShape = points.size > 12 && pathLength > 220f

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.romaji, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onSpeak(item.kana) }) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = "Hear")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFFFFBF3), RoundedCornerShape(26.dp))
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
                .pointerInput(item.id) {
                    detectDragGestures(
                        onDragStart = { points = points + it },
                        onDrag = { change, _ -> points = points + change.position }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(item.kana, fontSize = 190.sp, color = Color(0x242F5D50), fontWeight = FontWeight.Black)
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0x22A66A5A),
                    start = Offset(size.width * 0.18f, size.height * 0.5f),
                    end = Offset(size.width * 0.82f, size.height * 0.5f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0x22A66A5A),
                    start = Offset(size.width * 0.5f, size.height * 0.18f),
                    end = Offset(size.width * 0.5f, size.height * 0.82f),
                    strokeWidth = 2.dp.toPx()
                )
                val path = Path()
                points.firstOrNull()?.let { path.moveTo(it.x, it.y) }
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                drawPath(
                    path = path,
                    color = Color(0xFF2F5D50),
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        Text(
            if (hasShape) "Looks ready to check." else "Trace over the ghost kana. Fill enough of the shape before checking.",
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { points = emptyList() }, modifier = Modifier.weight(1f)) {
                Text("Clear")
            }
            Button(onClick = { onAnswer(hasShape) }, modifier = Modifier.weight(1f)) {
                Text("Check")
            }
        }
    }
}

@Composable
private fun KanaChartScreen(script: Script, mastery: Map<String, Int>, onSpeak: (String) -> Unit) {
    val items = remember(script) { itemsFor(script) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 86.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            val level = mastery[item.id] ?: 0
            Card(
                onClick = { onSpeak(item.kana) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (level > 0) Color(0xFFDCEBDD) else MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(item.kana, fontSize = 42.sp, fontWeight = FontWeight.Black)
                    Text(item.romaji, style = MaterialTheme.typography.labelLarge)
                    Text(item.row, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MistakePracticeScreen(
    allItems: List<KanaItem>,
    mistakeIds: List<String>,
    mastery: Map<String, Int>,
    onSpeak: (String) -> Unit,
    onResult: (List<KanaItem>, Boolean) -> Unit
) {
    val weakItems = remember(mistakeIds, mastery) {
        val byId = allItems.associateBy { it.id }
        mistakeIds.mapNotNull { byId[it] }.ifEmpty {
            allItems.sortedBy { mastery[it.id] ?: 0 }.take(6)
        }
    }
    var currentIndex by remember(weakItems) { mutableIntStateOf(0) }
    val current = weakItems.getOrNull(currentIndex % weakItems.size)

    if (current == null) {
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
        HeroPanel("Mistake replay", "Short, targeted review for weak kana.")
        ExerciseCard(
            exercise = buildMistakeExercise(current),
            allItems = itemsFor(current.script),
            onSpeak = onSpeak,
            feedback = null,
            onAnswer = { correct ->
                onResult(listOf(current), correct)
                currentIndex += 1
            }
        )
    }
}

@Composable
private fun KanaTheme(content: @Composable () -> Unit) {
    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = Color(0xFF2F5D50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCEBDD),
        onPrimaryContainer = Color(0xFF0E2B23),
        secondary = Color(0xFFA66A5A),
        secondaryContainer = Color(0xFFFFDFD6),
        tertiary = Color(0xFF6B5CA5),
        tertiaryContainer = Color(0xFFE7DEFF),
        background = Color(0xFFFFF8F0),
        surface = Color(0xFFFFFCF7),
        surfaceVariant = Color(0xFFF1E6D6),
        outlineVariant = Color(0xFFD8C8B7)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

private fun buildLessonExercises(lesson: KanaLesson): List<Exercise> {
    val intro = lesson.items.flatMap {
        listOf(
            Exercise(ExerciseKind.RomajiToKana, listOf(it)),
            Exercise(ExerciseKind.KanaToRomaji, listOf(it))
        )
    }
    val pairs = lesson.items.chunked(4).map { Exercise(ExerciseKind.PairMatch, it) }
    val writing = lesson.items.take(3).map { Exercise(ExerciseKind.TraceKana, listOf(it)) }
    return (intro + pairs + writing).shuffled(Random(lesson.index))
}

private fun buildMistakeExercise(item: KanaItem): Exercise =
    Exercise(
        kind = listOf(ExerciseKind.RomajiToKana, ExerciseKind.KanaToRomaji, ExerciseKind.TraceKana).random(Random(item.id.hashCode())),
        items = listOf(item)
    )

private fun romajiOptions(target: KanaItem, allItems: List<KanaItem>): List<String> =
    (listOf(target.romaji) + allItems.filter { it.row == target.row && it.id != target.id }.map { it.romaji }.take(3) +
        allItems.filter { it.id != target.id }.shuffled(Random(target.id.hashCode())).map { it.romaji }.take(3))
        .distinct()
        .take(4)
        .shuffled(Random(target.romaji.hashCode()))

private fun kanaOptions(target: KanaItem, allItems: List<KanaItem>): List<String> =
    (listOf(target.kana) + target.confusable + allItems.filter { it.row == target.row && it.id != target.id }.map { it.kana }.take(3) +
        allItems.filter { it.id != target.id }.shuffled(Random(target.id.hashCode())).map { it.kana }.take(3))
        .distinct()
        .take(4)
        .shuffled(Random(target.kana.hashCode()))

private fun lessonsFor(script: Script): List<KanaLesson> =
    itemsFor(script).groupBy { it.lesson }.toSortedMap().map { (index, items) ->
        KanaLesson(
            index = index,
            title = "Lesson $index",
            subtitle = "${items.first().row} row: ${items.joinToString(" ") { it.romaji }}",
            items = items
        )
    }

private fun itemsFor(script: Script): List<KanaItem> =
    if (script == Script.Hiragana) hiraganaItems else katakanaItems

private fun lessonSeed(items: List<KanaItem>): Int =
    items.joinToString("") { it.id }.hashCode()

private fun kana(
    script: Script,
    kana: String,
    romaji: String,
    row: String,
    lesson: Int,
    confusable: List<String> = emptyList()
) = KanaItem("${script.name.lowercase()}-$romaji", script, kana, romaji, row, lesson, confusable)

private val hiraganaItems = listOf(
    kana(Script.Hiragana, "あ", "a", "vowels", 1),
    kana(Script.Hiragana, "い", "i", "vowels", 1),
    kana(Script.Hiragana, "う", "u", "vowels", 1),
    kana(Script.Hiragana, "え", "e", "vowels", 1),
    kana(Script.Hiragana, "お", "o", "vowels", 1),
    kana(Script.Hiragana, "か", "ka", "k", 2),
    kana(Script.Hiragana, "き", "ki", "k", 2),
    kana(Script.Hiragana, "く", "ku", "k", 2),
    kana(Script.Hiragana, "け", "ke", "k", 2),
    kana(Script.Hiragana, "こ", "ko", "k", 2),
    kana(Script.Hiragana, "さ", "sa", "s", 3, listOf("ち")),
    kana(Script.Hiragana, "し", "shi", "s", 3),
    kana(Script.Hiragana, "す", "su", "s", 3),
    kana(Script.Hiragana, "せ", "se", "s", 3),
    kana(Script.Hiragana, "そ", "so", "s", 3),
    kana(Script.Hiragana, "た", "ta", "t", 4),
    kana(Script.Hiragana, "ち", "chi", "t", 4, listOf("さ")),
    kana(Script.Hiragana, "つ", "tsu", "t", 4),
    kana(Script.Hiragana, "て", "te", "t", 4),
    kana(Script.Hiragana, "と", "to", "t", 4),
    kana(Script.Hiragana, "な", "na", "n", 5),
    kana(Script.Hiragana, "に", "ni", "n", 5),
    kana(Script.Hiragana, "ぬ", "nu", "n", 5, listOf("め")),
    kana(Script.Hiragana, "ね", "ne", "n", 5),
    kana(Script.Hiragana, "の", "no", "n", 5),
    kana(Script.Hiragana, "は", "ha", "h", 6),
    kana(Script.Hiragana, "ひ", "hi", "h", 6),
    kana(Script.Hiragana, "ふ", "fu", "h", 6),
    kana(Script.Hiragana, "へ", "he", "h", 6),
    kana(Script.Hiragana, "ほ", "ho", "h", 6),
    kana(Script.Hiragana, "ま", "ma", "m", 7),
    kana(Script.Hiragana, "み", "mi", "m", 7),
    kana(Script.Hiragana, "む", "mu", "m", 7),
    kana(Script.Hiragana, "め", "me", "m", 7, listOf("ぬ")),
    kana(Script.Hiragana, "も", "mo", "m", 7),
    kana(Script.Hiragana, "や", "ya", "y", 8),
    kana(Script.Hiragana, "ゆ", "yu", "y", 8),
    kana(Script.Hiragana, "よ", "yo", "y", 8),
    kana(Script.Hiragana, "ら", "ra", "r", 9),
    kana(Script.Hiragana, "り", "ri", "r", 9),
    kana(Script.Hiragana, "る", "ru", "r", 9),
    kana(Script.Hiragana, "れ", "re", "r", 9),
    kana(Script.Hiragana, "ろ", "ro", "r", 9),
    kana(Script.Hiragana, "わ", "wa", "w", 10),
    kana(Script.Hiragana, "を", "wo", "w", 10),
    kana(Script.Hiragana, "ん", "n", "w", 10)
)

private val katakanaItems = listOf(
    kana(Script.Katakana, "ア", "a", "vowels", 1),
    kana(Script.Katakana, "イ", "i", "vowels", 1),
    kana(Script.Katakana, "ウ", "u", "vowels", 1),
    kana(Script.Katakana, "エ", "e", "vowels", 1),
    kana(Script.Katakana, "オ", "o", "vowels", 1),
    kana(Script.Katakana, "カ", "ka", "k", 2),
    kana(Script.Katakana, "キ", "ki", "k", 2),
    kana(Script.Katakana, "ク", "ku", "k", 2),
    kana(Script.Katakana, "ケ", "ke", "k", 2),
    kana(Script.Katakana, "コ", "ko", "k", 2),
    kana(Script.Katakana, "サ", "sa", "s", 3),
    kana(Script.Katakana, "シ", "shi", "s", 3, listOf("ツ", "ン")),
    kana(Script.Katakana, "ス", "su", "s", 3),
    kana(Script.Katakana, "セ", "se", "s", 3),
    kana(Script.Katakana, "ソ", "so", "s", 3, listOf("ン")),
    kana(Script.Katakana, "タ", "ta", "t", 4),
    kana(Script.Katakana, "チ", "chi", "t", 4),
    kana(Script.Katakana, "ツ", "tsu", "t", 4, listOf("シ")),
    kana(Script.Katakana, "テ", "te", "t", 4),
    kana(Script.Katakana, "ト", "to", "t", 4),
    kana(Script.Katakana, "ナ", "na", "n", 5),
    kana(Script.Katakana, "ニ", "ni", "n", 5),
    kana(Script.Katakana, "ヌ", "nu", "n", 5),
    kana(Script.Katakana, "ネ", "ne", "n", 5),
    kana(Script.Katakana, "ノ", "no", "n", 5),
    kana(Script.Katakana, "ハ", "ha", "h", 6),
    kana(Script.Katakana, "ヒ", "hi", "h", 6),
    kana(Script.Katakana, "フ", "fu", "h", 6),
    kana(Script.Katakana, "ヘ", "he", "h", 6),
    kana(Script.Katakana, "ホ", "ho", "h", 6),
    kana(Script.Katakana, "マ", "ma", "m", 7),
    kana(Script.Katakana, "ミ", "mi", "m", 7),
    kana(Script.Katakana, "ム", "mu", "m", 7),
    kana(Script.Katakana, "メ", "me", "m", 7),
    kana(Script.Katakana, "モ", "mo", "m", 7),
    kana(Script.Katakana, "ヤ", "ya", "y", 8),
    kana(Script.Katakana, "ユ", "yu", "y", 8),
    kana(Script.Katakana, "ヨ", "yo", "y", 8),
    kana(Script.Katakana, "ラ", "ra", "r", 9),
    kana(Script.Katakana, "リ", "ri", "r", 9),
    kana(Script.Katakana, "ル", "ru", "r", 9),
    kana(Script.Katakana, "レ", "re", "r", 9),
    kana(Script.Katakana, "ロ", "ro", "r", 9),
    kana(Script.Katakana, "ワ", "wa", "w", 10),
    kana(Script.Katakana, "ヲ", "wo", "w", 10),
    kana(Script.Katakana, "ン", "n", "w", 10, listOf("ソ", "シ"))
)
