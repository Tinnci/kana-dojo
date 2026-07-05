package dev.tinnci.kanadojo

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

enum class KanaEarcon {
    Navigate,
    Select,
    Start,
    Continue,
    Correct,
    Incorrect,
    Complete,
    Review,
    Reset,
    Speak
}

class KanaEarcons {
    private val handler = Handler(Looper.getMainLooper())
    private val toneGenerator = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, EARCON_VOLUME)
    }.getOrNull()

    var enabled: Boolean = true

    fun play(earcon: KanaEarcon) {
        if (!enabled) return
        val generator = toneGenerator ?: return
        handler.removeCallbacksAndMessages(null)
        earcon.pattern.forEach { tone ->
            handler.postDelayed(
                {
                    runCatching {
                        generator.startTone(tone.toneType, tone.durationMs)
                    }
                },
                tone.delayMs
            )
        }
    }

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        toneGenerator?.release()
    }

    private data class EarconTone(
        val toneType: Int,
        val durationMs: Int,
        val delayMs: Long = 0L
    )

    private val KanaEarcon.pattern: List<EarconTone>
        get() = when (this) {
            KanaEarcon.Navigate -> listOf(EarconTone(ToneGenerator.TONE_PROP_BEEP, 32))
            KanaEarcon.Select -> listOf(EarconTone(ToneGenerator.TONE_PROP_PROMPT, 36))
            KanaEarcon.Start -> listOf(
                EarconTone(ToneGenerator.TONE_PROP_PROMPT, 42),
                EarconTone(ToneGenerator.TONE_PROP_ACK, 52, delayMs = 70L)
            )

            KanaEarcon.Continue -> listOf(EarconTone(ToneGenerator.TONE_PROP_ACK, 42))
            KanaEarcon.Correct -> listOf(
                EarconTone(ToneGenerator.TONE_PROP_ACK, 48),
                EarconTone(ToneGenerator.TONE_PROP_ACK, 56, delayMs = 78L)
            )

            KanaEarcon.Incorrect -> listOf(EarconTone(ToneGenerator.TONE_PROP_NACK, 86))
            KanaEarcon.Complete -> listOf(
                EarconTone(ToneGenerator.TONE_PROP_ACK, 54),
                EarconTone(ToneGenerator.TONE_PROP_ACK, 54, delayMs = 82L),
                EarconTone(ToneGenerator.TONE_PROP_PROMPT, 68, delayMs = 164L)
            )

            KanaEarcon.Review -> listOf(
                EarconTone(ToneGenerator.TONE_PROP_BEEP2, 50),
                EarconTone(ToneGenerator.TONE_PROP_PROMPT, 54, delayMs = 88L)
            )

            KanaEarcon.Reset -> listOf(EarconTone(ToneGenerator.TONE_PROP_BEEP2, 80))
            KanaEarcon.Speak -> listOf(EarconTone(ToneGenerator.TONE_PROP_PROMPT, 28))
        }

    private companion object {
        const val EARCON_VOLUME = 42
    }
}

@Composable
fun rememberKanaEarcons(enabled: Boolean): KanaEarcons {
    val earcons = remember { KanaEarcons() }
    earcons.enabled = enabled
    DisposableEffect(Unit) {
        onDispose { earcons.shutdown() }
    }
    return earcons
}
