package dev.tinnci.kanadojo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    var reduceMotion by remember { mutableStateOf(progressStore.loadReduceMotion()) }
    var soundEnabled by remember { mutableStateOf(progressStore.loadSoundEnabled()) }
    var hapticsEnabled by remember { mutableStateOf(progressStore.loadHapticsEnabled()) }

    LaunchedEffect(Unit) {
        mastery.putAll(progressStore.loadMastery(allItems))
        mistakes.clear()
        mistakes.addAll(progressStore.loadMistakes())
    }

    val tts = rememberKanaSpeech()
    val haptic = LocalHapticFeedback.current
    val speakKana: (String) -> Unit = { kana ->
        if (soundEnabled) {
            tts.speak(kana)
        }
    }
    val markResult: (List<KanaItem>, Boolean) -> Unit = { items, correct ->
        if (hapticsEnabled) {
            haptic.performHapticFeedback(if (correct) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
        }
        progressStore.mark(items, correct)
        mastery.putAll(progressStore.loadMastery(allItems))
        mistakes.clear()
        mistakes.addAll(progressStore.loadMistakes())
    }
    KanaTheme {
        Scaffold(
            topBar = {
                KanaTopBar(
                    selectedScript = selectedScript,
                    reduceMotion = reduceMotion,
                    soundEnabled = soundEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onScriptChange = { selectedScript = it },
                    onReduceMotionChange = {
                        reduceMotion = it
                        progressStore.setReduceMotion(it)
                    },
                    onSoundEnabledChange = {
                        soundEnabled = it
                        progressStore.setSoundEnabled(it)
                    },
                    onHapticsEnabledChange = {
                        hapticsEnabled = it
                        progressStore.setHapticsEnabled(it)
                    }
                )
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
                        onSpeak = speakKana,
                        reduceMotion = reduceMotion,
                        onOpenPractice = { currentTab = ScreenTab.Mistakes },
                        onResult = markResult
                    )

                    ScreenTab.Chart -> KanaChartScreen(
                        script = selectedScript,
                        mastery = mastery,
                        onSpeak = speakKana
                    )

                    ScreenTab.Mistakes -> MistakePracticeScreen(
                        script = selectedScript,
                        allItems = allItems,
                        mistakeIds = mistakes,
                        mastery = mastery,
                        onSpeak = speakKana,
                        reduceMotion = reduceMotion,
                        onResult = markResult
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
private fun KanaTopBar(
    selectedScript: Script,
    reduceMotion: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onScriptChange: (Script) -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onHapticsEnabledChange: (Boolean) -> Unit
) {
    var settingsOpen by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
            Column {
                Text("Kana Dojo", fontWeight = FontWeight.Black)
                Text("guided kana drills", style = MaterialTheme.typography.labelMedium)
            }
        },
        actions = {
            Box {
                AssistChip(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = { settingsOpen = true },
                    label = { Text("Settings") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                DropdownMenu(expanded = settingsOpen, onDismissRequest = { settingsOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Sound") },
                        onClick = { onSoundEnabledChange(!soundEnabled) },
                        trailingIcon = {
                            Switch(checked = soundEnabled, onCheckedChange = onSoundEnabledChange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reduced motion") },
                        onClick = { onReduceMotionChange(!reduceMotion) },
                        trailingIcon = {
                            Switch(checked = reduceMotion, onCheckedChange = onReduceMotionChange)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Haptics") },
                        onClick = { onHapticsEnabledChange(!hapticsEnabled) },
                        trailingIcon = {
                            Switch(checked = hapticsEnabled, onCheckedChange = onHapticsEnabledChange)
                        }
                    )
                }
            }
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
fun FocusMetric(label: String, value: Int, modifier: Modifier = Modifier) {
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
fun ProgressStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = color, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun StageChip(stage: LearningStage) {
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
fun ExerciseCard(
    exercise: Exercise,
    allItems: List<KanaItem>,
    onSpeak: (String) -> Unit,
    reduceMotion: Boolean,
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
                        reduceMotion = reduceMotion,
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
