package ai.voicedoc.app.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import ai.voicedoc.core.common.SupportedLanguage
import java.util.Locale

/**
 * OS-level *on-device-only* speech recognition for Android 12+.
 * Never falls back to the potentially network-backed default recognizer.
 * Language packs and actual Telugu support are dependent on the installed Android service.
 * Must only be called on the Android main thread.
 */
class OnDeviceSpeechService(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    fun available(): Boolean =
        Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun start(language: SupportedLanguage) {
        if (!available()) {
            onStatus("On-device recognition is not available on this phone.")
            return
        }
        stop()
        try {
            val engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = engine
            engine.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onStatus("Listening locally…") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onStatus("Processing on device…") }
                override fun onError(error: Int) {
                    onStatus("Offline recognition failed (code $error). Verify installed language pack.")
                }
                override fun onResults(results: Bundle?) {
                    val hypothesis = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (hypothesis.isNullOrBlank()) {
                        onStatus("No local transcript available.")
                    } else {
                        onText(hypothesis)
                        onStatus("Transcript inserted for your review. No question sent automatically.")
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val locale = if (language == SupportedLanguage.TELUGU) Locale("te", "IN") else Locale.US
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            engine.startListening(intent)
        } catch (error: Exception) {
            stop()
            onStatus("On-device recognition unavailable: ${error.javaClass.simpleName}")
        }
    }

    fun stop() {
        recognizer?.runCatching { cancel() }
        recognizer?.runCatching { destroy() }
        recognizer = null
    }
}
