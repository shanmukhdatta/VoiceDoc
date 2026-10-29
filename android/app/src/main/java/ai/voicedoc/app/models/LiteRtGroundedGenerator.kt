package ai.voicedoc.app.models

import ai.voicedoc.core.common.BoundedEvidencePrompt
import ai.voicedoc.core.common.DraftGroundedAnswer
import ai.voicedoc.core.common.EngineUnavailableException
import ai.voicedoc.core.common.GroundedAnswerParser
import ai.voicedoc.core.common.KnowledgeHit
import ai.voicedoc.core.common.StructuredLocalGenerator
import ai.voicedoc.core.common.SupportedLanguage
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Bounded one-question LiteRT-LM adapter for the common evidence pipeline.
 * It must only be constructed after a trusted model-pack verifier has checked model and tokenizer
 * provenance. This adapter does not establish medical quality or make an unapproved pack trusted.
 */
class LiteRtGroundedGenerator(private val modelFile: File) : StructuredLocalGenerator {
    private val lock = Mutex()
    private var engine: Engine? = null
    private var closed = false

    override suspend fun generate(
        userQuestion: String,
        language: SupportedLanguage,
        evidence: List<KnowledgeHit>,
        maxNewTokens: Int
    ): DraftGroundedAnswer = lock.withLock {
        withContext(Dispatchers.IO) {
            check(!closed) { "Local model engine is closed" }
            if (!modelFile.isFile || modelFile.extension != "litertlm") {
                throw EngineUnavailableException("Compatible LiteRT-LM model")
            }
            require(maxNewTokens in 1..256)
            require(evidence.isNotEmpty() && evidence.size <= 5)

            val activeEngine = engine ?: Engine(
                EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.CPU())
            ).also { candidate ->
                try {
                    candidate.initialize()
                    engine = candidate
                } catch (error: Exception) {
                    candidate.close()
                    throw IOException("Could not initialize the local model", error)
                }
            }

            val prompt = BoundedEvidencePrompt.build(userQuestion, language, evidence)
            val response = StringBuilder()
            val conversationConfig = ConversationConfig(
                automaticToolCalling = false,
                maxOutputToken = maxNewTokens,
                thinkingConfig = ThinkingConfig(enableThinking = false)
            )
            activeEngine.createConversation(conversationConfig).use { conversation ->
                conversation.sendMessageAsync(prompt).collect { message ->
                    val delta = message.toString()
                    if (response.length + delta.length > MAX_RESPONSE_CHARS) {
                        throw IOException("Local model output exceeded the response safety limit")
                    }
                    response.append(delta)
                }
            }
            GroundedAnswerParser.parse(response.toString(), language)
        }
    }

    /** Call when the model feature is disposed; waits for an active generation to finish/cancel. */
    suspend fun shutdown() = lock.withLock {
        if (!closed) {
            closed = true
            engine?.close()
            engine = null
        }
    }

    private companion object {
        const val MAX_RESPONSE_CHARS = 8_000
    }
}
