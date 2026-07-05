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
    var practiceEpochDays by remember { mutableStateOf(progressStore.loadPracticeEpochDays()) }
    var selectedScript by remember { mutableStateOf(Script.Hiragana) }
    var currentTab by remember { mutableStateOf(ScreenTab.Lessons) }
    var requestedPracticeMode by remember { mutableStateOf(PracticeMode.Weak) }
    var reduceMotion by remember { mutableStateOf(progressStore.loadReduceMotion()) }
    var soundEnabled by remember { mutableStateOf(progressStore.loadSoundEnabled()) }
    var hapticsEnabled by remember { mutableStateOf(progressStore.loadHapticsEnabled()) }
    val today = currentEpochDay()

    LaunchedEffect(today) {
        mastery.putAll(progressStore.loadMastery(allItems))
        mistakes.clear()
        mistakes.addAll(progressStore.loadMistakes())
        reviewDueEpochDays.clear()
        reviewDueEpochDays.putAll(progressStore.loadReviewDueEpochDays(allItems))
        practiceEpochDays = progressStore.loadPracticeEpochDays(today)
    }

    val tts = rememberKanaSpeech()
    val earcons = rememberKanaEarcons(soundEnabled)
    val haptic = LocalHapticFeedback.current
    val playEarcon: (KanaEarcon) -> Unit = { earcon ->
        earcons.play(earcon)
    }
    val playTaptic: (KanaTaptic) -> Unit = { taptic ->
        if (hapticsEnabled) {
            performKanaTaptic(haptic, taptic)
        }
    }
    val speakKana: (String) -> Unit = { kana ->
        if (soundEnabled) {
            tts.speak(kana)
        }
    }
    val markResult: (List<KanaItem>, Boolean) -> Unit = { items, correct ->
        playEarcon(if (correct) KanaEarcon.Correct else KanaEarcon.Incorrect)
        playTaptic(if (correct) KanaTaptic.Correct else KanaTaptic.Incorrect)
        progressStore.mark(items, correct)
        mastery.putAll(progressStore.loadMastery(allItems))
        mistakes.clear()
        mistakes.addAll(progressStore.loadMistakes())
        reviewDueEpochDays.clear()
        reviewDueEpochDays.putAll(progressStore.loadReviewDueEpochDays(allItems))
        practiceEpochDays = progressStore.loadPracticeEpochDays(today)
    }
    KanaTheme {
        Scaffold(
            topBar = {
                KanaTopBar(
                    selectedScript = selectedScript,
                    reduceMotion = reduceMotion,
                    soundEnabled = soundEnabled,
                    hapticsEnabled = hapticsEnabled,
                    onSettingsOpen = {
                        playEarcon(KanaEarcon.Select)
                        playTaptic(KanaTaptic.Select)
                    },
                    onScriptChange = {
                        playEarcon(KanaEarcon.Navigate)
                        playTaptic(KanaTaptic.Navigate)
                        selectedScript = it
                    },
                    onReduceMotionChange = {
                        playEarcon(KanaEarcon.Select)
                        playTaptic(if (it) KanaTaptic.ToggleOn else KanaTaptic.ToggleOff)
                        reduceMotion = it
                        progressStore.setReduceMotion(it)
                    },
                    onSoundEnabledChange = {
                        playTaptic(if (it) KanaTaptic.ToggleOn else KanaTaptic.ToggleOff)
                        soundEnabled = it
                        progressStore.setSoundEnabled(it)
                        if (it) {
                            earcons.enabled = true
                            playEarcon(KanaEarcon.Select)
                        }
                    },
                    onHapticsEnabledChange = {
                        playEarcon(KanaEarcon.Select)
                        if (it) {
                            performKanaTaptic(haptic, KanaTaptic.ToggleOn)
                        } else {
                            playTaptic(KanaTaptic.ToggleOff)
                        }
                        hapticsEnabled = it
                        progressStore.setHapticsEnabled(it)
                    }
                )
            },
            bottomBar = {
                KanaBottomBar(
                    currentTab = currentTab,
                    onTabChange = {
                        if (currentTab != it) {
                            playEarcon(KanaEarcon.Navigate)
                            playTaptic(KanaTaptic.Navigate)
                        }
                        currentTab = it
                    }
                )
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
                        practiceEpochDays = practiceEpochDays,
                        currentEpochDay = today,
                        onSpeak = speakKana,
                        onEarcon = playEarcon,
                        onTaptic = playTaptic,
                        reduceMotion = reduceMotion,
                        onOpenPractice = { mode ->
                            playEarcon(KanaEarcon.Review)
                            playTaptic(KanaTaptic.Review)
                            requestedPracticeMode = mode
                            currentTab = ScreenTab.Mistakes
                        },
                        onResult = markResult
                    )

                    ScreenTab.Chart -> KanaChartScreen(
                        script = selectedScript,
                        mastery = mastery,
                        onSpeak = speakKana,
                        onEarcon = playEarcon,
                        onTaptic = playTaptic
                    )

                    ScreenTab.Mistakes -> MistakePracticeScreen(
                        script = selectedScript,
                        initialMode = requestedPracticeMode,
                        allItems = allItems,
                        mistakeIds = mistakes,
                        mastery = mastery,
                        reviewDueEpochDays = reviewDueEpochDays,
                        currentEpochDay = today,
                        onSpeak = speakKana,
                        onEarcon = playEarcon,
                        onTaptic = playTaptic,
                        reduceMotion = reduceMotion,
                        onResult = markResult,
                        onReturnToPath = {
                            playEarcon(KanaEarcon.Navigate)
                            playTaptic(KanaTaptic.Navigate)
                            currentTab = ScreenTab.Lessons
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
