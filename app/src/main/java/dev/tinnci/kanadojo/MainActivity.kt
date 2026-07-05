package dev.tinnci.kanadojo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var selectedScript by rememberSaveable { mutableStateOf(Script.Hiragana) }
    var currentTab by rememberSaveable { mutableStateOf(ScreenTab.Lessons) }
    var requestedPracticeMode by rememberSaveable { mutableStateOf(PracticeMode.Weak) }
    var reduceMotion by rememberSaveable { mutableStateOf(progressStore.loadReduceMotion()) }
    var soundEnabled by rememberSaveable { mutableStateOf(progressStore.loadSoundEnabled()) }
    var hapticsEnabled by rememberSaveable { mutableStateOf(progressStore.loadHapticsEnabled()) }
    var shellNavigationHidden by rememberSaveable { mutableStateOf(false) }
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val layoutMode = kanaLayoutModeFor(maxWidth, maxHeight)
            val onTabChange: (ScreenTab) -> Unit = { tab ->
                if (currentTab != tab) {
                    playEarcon(KanaEarcon.Navigate)
                    playTaptic(KanaTaptic.Navigate)
                }
                currentTab = tab
                shellNavigationHidden = false
            }

            Scaffold(
                topBar = {
                    if (!shellNavigationHidden) {
                        KanaTopBar(
                            layoutMode = layoutMode,
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
                    }
                },
                bottomBar = {
                    if (layoutMode == KanaLayoutMode.Compact && !shellNavigationHidden) {
                        KanaBottomBar(currentTab = currentTab, onTabChange = onTabChange)
                    }
                }
            ) { padding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (layoutMode != KanaLayoutMode.Compact && !shellNavigationHidden) {
                        KanaNavigationRail(currentTab = currentTab, onTabChange = onTabChange)
                    }
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            val duration = if (reduceMotion) 0 else 160
                            fadeIn(animationSpec = tween(durationMillis = duration)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = duration))
                        },
                        label = "screen",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            ScreenTab.Lessons -> LessonPathScreen(
                                script = selectedScript,
                                layoutMode = layoutMode,
                                mastery = mastery,
                                mistakeIds = mistakes,
                                reviewDueEpochDays = reviewDueEpochDays,
                                practiceEpochDays = practiceEpochDays,
                                currentEpochDay = today,
                                onSpeak = speakKana,
                                onEarcon = playEarcon,
                                onTaptic = playTaptic,
                                reduceMotion = reduceMotion,
                                onShellNavigationHiddenChange = { shellNavigationHidden = it },
                                onOpenPractice = { mode ->
                                    playEarcon(KanaEarcon.Review)
                                    playTaptic(KanaTaptic.Review)
                                    shellNavigationHidden = false
                                    requestedPracticeMode = mode
                                    currentTab = ScreenTab.Mistakes
                                },
                                onResult = markResult
                            )

                            ScreenTab.Chart -> KanaChartScreen(
                                script = selectedScript,
                                layoutMode = layoutMode,
                                mastery = mastery,
                                onSpeak = speakKana,
                                onEarcon = playEarcon,
                                onTaptic = playTaptic
                            )

                            ScreenTab.Mistakes -> MistakePracticeScreen(
                                script = selectedScript,
                                layoutMode = layoutMode,
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
                                onShellNavigationHiddenChange = { shellNavigationHidden = it },
                                onResult = markResult,
                                onReturnToPath = {
                                    playEarcon(KanaEarcon.Navigate)
                                    playTaptic(KanaTaptic.Navigate)
                                    shellNavigationHidden = false
                                    currentTab = ScreenTab.Lessons
                                }
                            )
                        }
                    }
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
