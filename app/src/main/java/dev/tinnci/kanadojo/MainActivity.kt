package dev.tinnci.kanadojo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

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
    val reviewDueEpochDays = remember { mutableStateMapOf<String, Long>() }
    var selectedScript by remember { mutableStateOf(Script.Hiragana) }
    var currentTab by remember { mutableStateOf(ScreenTab.Lessons) }
    var requestedPracticeMode by remember { mutableStateOf(PracticeMode.Weak) }
    var reduceMotion by remember { mutableStateOf(progressStore.loadReduceMotion()) }
    var soundEnabled by remember { mutableStateOf(progressStore.loadSoundEnabled()) }
    var hapticsEnabled by remember { mutableStateOf(progressStore.loadHapticsEnabled()) }

    LaunchedEffect(Unit) {
        mastery.putAll(progressStore.loadMastery(allItems))
        mistakes.clear()
        mistakes.addAll(progressStore.loadMistakes())
        reviewDueEpochDays.clear()
        reviewDueEpochDays.putAll(progressStore.loadReviewDueEpochDays(allItems))
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
        reviewDueEpochDays.clear()
        reviewDueEpochDays.putAll(progressStore.loadReviewDueEpochDays(allItems))
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
                KanaBottomBar(currentTab = currentTab, onTabChange = { currentTab = it })
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
                        reviewDueEpochDays = reviewDueEpochDays,
                        currentEpochDay = currentEpochDay(),
                        onSpeak = speakKana,
                        reduceMotion = reduceMotion,
                        onOpenPractice = { mode ->
                            requestedPracticeMode = mode
                            currentTab = ScreenTab.Mistakes
                        },
                        onResult = markResult
                    )

                    ScreenTab.Chart -> KanaChartScreen(
                        script = selectedScript,
                        mastery = mastery,
                        onSpeak = speakKana
                    )

                    ScreenTab.Mistakes -> MistakePracticeScreen(
                        script = selectedScript,
                        initialMode = requestedPracticeMode,
                        allItems = allItems,
                        mistakeIds = mistakes,
                        mastery = mastery,
                        reviewDueEpochDays = reviewDueEpochDays,
                        currentEpochDay = currentEpochDay(),
                        onSpeak = speakKana,
                        reduceMotion = reduceMotion,
                        onResult = markResult,
                        onReturnToPath = { currentTab = ScreenTab.Lessons }
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
