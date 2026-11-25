package ai.voicedoc.core.common

import java.text.Normalizer
import kotlin.math.sqrt

/** Loss-minimizing bilingual text handling. No translation, diagnosis, or unit conversion. */
data class LanguageTrace(
    val original: String,
    val normalized: String,
    val hasTelugu: Boolean,
    val hasLatin: Boolean,
    val codeSwitched: Boolean
)

object BilingualNormalizer {
    fun analyze(text: String): LanguageTrace {
        require(text.length <= 10_000)
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
            .filter { it >= ' ' || it == '\n' || it == '\t' }
            .trim()
            .replace(Regex("\\s+"), " ")
        val telugu = normalized.any { it in '\u0C00'..'\u0C7F' }
        val latin = normalized.any { it in 'a'..'z' || it in 'A'..'Z' }
        return LanguageTrace(text, normalized, telugu, latin, telugu && latin)
    }
}

/**
 * Minimal offline PCM energy gate. A non-speech classification means only
 * low signal energy; it is NOT reliable medical voice detection.
 * Silero ONNX remains the targeted production VAD.
 */
class EnergyVoiceActivityDetector(
    private val sampleRate: Int = 16_000,
    private val frameMs: Int = 20,
    private val amplitudeThreshold: Double = 0.02,
    private val trailingFrames: Int = 3
) {
    init {
        require(sampleRate > 0 && frameMs in 10..100 &&
            amplitudeThreshold in 0.0001..1.0 && trailingFrames in 0..50)
    }

    fun detect(pcm: ShortArray): List<AudioSegment> {
        if (pcm.isEmpty()) return emptyList()
        val frameSize = (sampleRate * frameMs) / 1000
        require(frameSize > 0)
        val segments = mutableListOf<AudioSegment>()
        var start = -1
        var inactive = 0
        var sample = 0
        while (sample < pcm.size) {
            val end = minOf(sample + frameSize, pcm.size)
            var energy = 0.0
            for (i in sample until end) {
                val v = pcm[i].toDouble() / 32768.0
                energy += v * v
            }
            val active = sqrt(energy / (end - sample)) >= amplitudeThreshold
            if (active) {
                if (start < 0) start = sample
                inactive = 0
            } else if (start >= 0) {
                inactive++
                if (inactive > trailingFrames) {
                    segments.add(AudioSegment(start, maxOf(start + 1, sample - trailingFrames * frameSize)))
                    start = -1
                    inactive = 0
                }
            }
            sample = end
        }
        if (start >= 0) segments.add(AudioSegment(start, pcm.size))
        return segments
    }
}
