package ai.voicedoc.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import ai.voicedoc.core.common.SupportedLanguage

/**
 * Real Android platform TTS, strictly restricted to installed voices that
 * report no network requirement. A voice listing is not proof of offline
 * synth success: confirm in airplane mode before accepting product readiness.
 */
class DeviceOfflineTts(context: Context, private val onInitialized: (Boolean) -> Unit) {
    private val synthesizer = TextToSpeech(context.applicationContext) { status ->
        onInitialized(status == TextToSpeech.SUCCESS)
    }

    fun available(language: SupportedLanguage): List<Voice> =
        synthesizer.voices.orEmpty()
            .filter { it.locale.language == language.tag && !it.isNetworkConnectionRequired }
            .sortedBy { it.name }

    fun speak(text: String, voice: Voice): Boolean {
        if (voice.isNetworkConnectionRequired || text.isBlank() || text.length > 300) return false
        if (synthesizer.setVoice(voice) != TextToSpeech.SUCCESS) return false
        return synthesizer.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice-test") ==
            TextToSpeech.SUCCESS
    }

    fun stop() { synthesizer.stop() }
    fun close() { synthesizer.shutdown() }
}
