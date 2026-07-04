package dev.tinnci.kanadojo

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class KanaSpeech(context: Context) : TextToSpeech.OnInitListener {
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
