package ai.voicedoc.core.common

/**
 * Runtime-neutral capability boundaries. A declared interface does not imply an
 * implementation exists. Production adapters must prove ABI, model and license compatibility.
 */
interface VoiceActivityEngine {
    suspend fun detectSpeech(pcm16kMono: ShortArray): List<AudioSegment>
    fun close()
}
data class AudioSegment(val startSample: Int, val endSample: Int) {
    init { require(startSample >= 0 && endSample > startSample) }
}
data class Transcript(val text: String, val language: SupportedLanguage, val confidence: Float?)

interface OfflineAsrEngine {
    suspend fun transcribe(pcm16kMono: ShortArray, languageHint: SupportedLanguage?): Transcript
    fun close()
}
interface QueryEmbeddingEngine {
    val dimension: Int
    suspend fun embedQuery(query: String): FloatArray
    fun close()
}
data class Citation(val sourceId: String, val chunkId: String, val title: String)
data class KnowledgeHit(val text: String, val citation: Citation, val score: Double)
interface MedicalRetriever {
    suspend fun search(query: String, language: SupportedLanguage, topK: Int): List<KnowledgeHit>
}
data class GenerationRequest(
    val instruction: String,
    val evidence: List<KnowledgeHit>,
    val language: SupportedLanguage,
    val maxNewTokens: Int
) {
    init { require(maxNewTokens in 1..1024) }
}
interface LocalLanguageModelEngine {
    suspend fun generate(request: GenerationRequest, onToken: (String) -> Unit)
    suspend fun cancel()
    fun close()
}
data class LocalVoice(val id: String, val language: SupportedLanguage, val requiresNetwork: Boolean)
interface OfflineTextToSpeechEngine {
    suspend fun listAvailableVoices(): List<LocalVoice>
    suspend fun speak(text: String, voiceId: String): Boolean
    fun stop()
    fun close()
}

/** Any unavailable engine MUST fail explicitly; it must not generate fake medical answers. */
class EngineUnavailableException(component: String) :
    IllegalStateException("$component is not installed or verified for offline use")
