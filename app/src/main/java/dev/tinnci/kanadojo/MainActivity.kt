package dev.tinnci.kanadojo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
