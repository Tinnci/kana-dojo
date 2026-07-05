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
    Reset
}

/*
 * Kana Dojo earcons are intentionally compact UI sounds, not reward jingles.
 * Keep frequent controls under 40 ms, answer/result cues under 80 ms, allow a
 * restrained two-pulse start cue, and save the only celebratory phrase for
 * completion. Spoken kana is the learning audio, so listen actions use the TTS
 * itself rather than a prep tone.
 */
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
        kanaEarconPatternFor(earcon).forEach { tone ->
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

    private companion object {
        const val EARCON_VOLUME = 38
    }
}

private data class EarconTone(
    val toneType: Int,
    val durationMs: Int,
    val delayMs: Long = 0L
)

private fun kanaEarconPatternFor(earcon: KanaEarcon): List<EarconTone> =
    when (earcon) {
        KanaEarcon.Navigate -> listOf(EarconTone(ToneGenerator.TONE_PROP_BEEP, 28))
        KanaEarcon.Select -> listOf(EarconTone(ToneGenerator.TONE_PROP_PROMPT, 30))
        KanaEarcon.Start -> listOf(
            EarconTone(ToneGenerator.TONE_PROP_PROMPT, 38),
            EarconTone(ToneGenerator.TONE_PROP_ACK, 46, delayMs = 64L)
        )

        KanaEarcon.Continue -> listOf(EarconTone(ToneGenerator.TONE_PROP_ACK, 36))
        KanaEarcon.Correct -> listOf(EarconTone(ToneGenerator.TONE_PROP_ACK, 54))

        KanaEarcon.Incorrect -> listOf(EarconTone(ToneGenerator.TONE_PROP_NACK, 72))
        KanaEarcon.Complete -> listOf(
            EarconTone(ToneGenerator.TONE_PROP_ACK, 48),
            EarconTone(ToneGenerator.TONE_PROP_ACK, 48, delayMs = 76L),
            EarconTone(ToneGenerator.TONE_PROP_PROMPT, 60, delayMs = 152L)
        )

        KanaEarcon.Review -> listOf(EarconTone(ToneGenerator.TONE_PROP_BEEP2, 58))

        KanaEarcon.Reset -> listOf(EarconTone(ToneGenerator.TONE_PROP_BEEP2, 64))
    }

internal data class KanaEarconTiming(
    val pulseCount: Int,
    val activeMs: Int,
    val longestPulseMs: Int,
    val totalMs: Int
)

internal fun kanaEarconTimingFor(earcon: KanaEarcon): KanaEarconTiming {
    val pattern = kanaEarconPatternFor(earcon)
    return KanaEarconTiming(
        pulseCount = pattern.size,
        activeMs = pattern.sumOf { it.durationMs },
        longestPulseMs = pattern.maxOf { it.durationMs },
        totalMs = pattern.maxOf { (it.delayMs + it.durationMs).toInt() }
    )
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
