package ai.voicedoc.app.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.sqrt

data class AudioProbeResult(
    val sampleRate: Int,
    val capturedSamples: Int,
    val rmsAmplitude: Double
)

/**
 * Diagnostic capture: reads ~1 second of 16 kHz mono PCM IN MEMORY.
 * Does not save audio, upload data, or claim to transcribe speech.
 * Caller must obtain RECORD_AUDIO permission before invoking.
 */
class LocalPcmRecorder {
    @SuppressLint("MissingPermission") // Permission gate enforced in MicrophoneTestPage.
    fun captureOneSecond(): AudioProbeResult {
        val rate = 16_000
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBytes = AudioRecord.getMinBufferSize(rate, channel, encoding)
        require(minBytes > 0) { "16 kHz PCM capture unsupported on this device" }
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            rate, channel, encoding, maxOf(minBytes, 2 * rate)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("AudioRecord could not be initialized")
        }
        val buffer = ShortArray(rate)
        var total = 0
        var sumSquares = 0.0
        try {
            recorder.startRecording()
            while (total < buffer.size) {
                val count = recorder.read(buffer, total, buffer.size - total, AudioRecord.READ_BLOCKING)
                if (count <= 0) throw IllegalStateException("AudioRecord read failed: $count")
                for (i in total until total + count) {
                    val x = buffer[i].toDouble() / Short.MAX_VALUE
                    sumSquares += x * x
                }
                total += count
            }
        } finally {
            try { recorder.stop() } catch (_: IllegalStateException) { }
            recorder.release()
            buffer.fill(0)
        }
        return AudioProbeResult(rate, total, sqrt(sumSquares / total))
    }
}
