package dev.tinnci.kanadojo

import android.os.Handler
import android.os.Looper
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

enum class KanaTaptic {
    Navigate,
    Select,
    Start,
    Continue,
    Correct,
    Incorrect,
    Complete,
    Review,
    Reset,
    Speak,
    ToggleOn,
    ToggleOff
}

fun performKanaTaptic(haptic: HapticFeedback, taptic: KanaTaptic) {
    val handler = Handler(Looper.getMainLooper())
    kanaTapticPatternFor(taptic).forEach { pulse ->
        handler.postDelayed(
            { haptic.performHapticFeedback(pulse.type) },
            pulse.delayMs
        )
    }
}

private fun kanaTapticPatternFor(taptic: KanaTaptic): List<HapticPulse> =
    when (taptic) {
        KanaTaptic.Navigate -> listOf(HapticPulse(HapticFeedbackType.SegmentTick))
        KanaTaptic.Select -> listOf(HapticPulse(HapticFeedbackType.SegmentTick))
        KanaTaptic.Start -> listOf(HapticPulse(HapticFeedbackType.GestureThresholdActivate))
        KanaTaptic.Continue -> listOf(HapticPulse(HapticFeedbackType.GestureEnd))
        KanaTaptic.Correct -> listOf(HapticPulse(HapticFeedbackType.Confirm))
        KanaTaptic.Incorrect -> listOf(HapticPulse(HapticFeedbackType.Reject))
        KanaTaptic.Complete -> listOf(
            HapticPulse(HapticFeedbackType.Confirm),
            HapticPulse(HapticFeedbackType.Confirm, delayMs = 90L),
            HapticPulse(HapticFeedbackType.GestureEnd, delayMs = 180L)
        )

        KanaTaptic.Review -> listOf(HapticPulse(HapticFeedbackType.GestureThresholdActivate))
        KanaTaptic.Reset -> listOf(HapticPulse(HapticFeedbackType.LongPress))
        KanaTaptic.Speak -> listOf(HapticPulse(HapticFeedbackType.ContextClick))
        KanaTaptic.ToggleOn -> listOf(HapticPulse(HapticFeedbackType.ToggleOn))
        KanaTaptic.ToggleOff -> listOf(HapticPulse(HapticFeedbackType.ToggleOff))
    }

internal data class KanaTapticTiming(
    val pulseCount: Int,
    val lastDelayMs: Long
)

internal fun kanaTapticTimingFor(taptic: KanaTaptic): KanaTapticTiming {
    val pattern = kanaTapticPatternFor(taptic)
    return KanaTapticTiming(
        pulseCount = pattern.size,
        lastDelayMs = pattern.maxOf { it.delayMs }
    )
}

private data class HapticPulse(
    val type: HapticFeedbackType,
    val delayMs: Long = 0L
)
