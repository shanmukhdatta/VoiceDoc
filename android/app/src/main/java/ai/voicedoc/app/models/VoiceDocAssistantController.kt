package ai.voicedoc.app.models

import android.content.Context
import android.os.Build
import ai.voicedoc.app.data.LocalKnowledgePack
import ai.voicedoc.app.data.SqliteHybridRetriever
import ai.voicedoc.core.common.Citation
import ai.voicedoc.core.common.OfflineTextPipeline
import ai.voicedoc.core.common.SupportedLanguage
import ai.voicedoc.core.common.TextPipelineOutcome
import ai.voicedoc.core.common.UnavailableStructuredGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.File

data class AssistantReadiness(
    val bundlePackId: String?,
    val bundleVersion: String?,
    val llmModelReady: Boolean,
    val knowledgeReady: Boolean,
    val semanticEmbeddingsReady: Boolean,
    val arm64Supported: Boolean
) {
    val fullyReady: Boolean get() = llmModelReady && knowledgeReady && arm64Supported
}

sealed interface AssistantReply {
    data class Evidenced(val text: String, val citations: List<Citation>) : AssistantReply
    data class Escalation(val text: String) : AssistantReply
    data class Unavailable(val text: String) : AssistantReply
}

/**
 * Coordinates installed VoiceDoc model/knowledge packs, E5 ONNX semantic retrieval,
 * LiteRT-LM Qwen3 generation, and safety/evidence gating.
 *
 * Maintains strict fail-closed behaviour when verified assets or ARM64 runtime are absent.
 */
class VoiceDocAssistantController(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val bundleImporter = LocalModelPackImporter(
        appContext,
        runtimeName = "litertlm",
        runtimeVersion = "0.18.0",
        expectedEmbeddingDimension = 384,
        expectedKind = "bundle"
    )
    private val generationImporter = LocalModelPackImporter(
        appContext,
        runtimeName = "litertlm",
        runtimeVersion = "0.18.0",
        expectedKind = "generation"
    )
    private val standaloneKnowledge = LocalKnowledgePack(appContext)

    private var cachedPackKey: String? = null
    private var generator: LiteRtGroundedGenerator? = null
    private var embeddingEngine: E5QueryEmbeddingEngine? = null

    suspend fun readiness(): AssistantReadiness = withContext(Dispatchers.IO) {
        val bundle = bundleImporter.installed().firstOrNull()
        val genPack = if (bundle == null) generationImporter.installed().firstOrNull() else null
        val llmFile = bundle?.asset("model") ?: genPack?.asset("model")
        val dbFile = bundle?.asset("index") ?: standaloneKnowledgeDbFile()
        val e5Model = bundle?.asset("embedding_model")
        val e5Tok = bundle?.asset("embedding_tokenizer")
        AssistantReadiness(
            bundlePackId = bundle?.id ?: genPack?.id,
            bundleVersion = bundle?.version ?: genPack?.version,
            llmModelReady = llmFile?.isFile == true,
            knowledgeReady = dbFile?.isFile == true,
            semanticEmbeddingsReady = e5Model?.isFile == true && e5Tok?.isFile == true,
            arm64Supported = Build.SUPPORTED_ABIS.contains("arm64-v8a")
        )
    }

    suspend fun ask(question: String, language: SupportedLanguage): AssistantReply {
        val bundle = bundleImporter.installed().firstOrNull()
        val genPack = if (bundle == null) generationImporter.installed().firstOrNull() else null
        val activePack = bundle ?: genPack

        ensureEnginesForPack(activePack)

        val retriever = SqliteHybridRetriever(
            dbFileProvider = { bundle?.asset("index") ?: standaloneKnowledgeDbFile() },
            embeddingEngineProvider = { embeddingEngine }
        )
        val activeGenerator = if (Build.SUPPORTED_ABIS.contains("arm64-v8a") && generator != null) {
            generator!!
        } else {
            UnavailableStructuredGenerator
        }
        val pipeline = OfflineTextPipeline(
            retriever = retriever,
            generator = activeGenerator,
            maxNewTokens = 160
        )

        return try {
            val outcome = withTimeout(INFERENCE_TIMEOUT_MS) {
                pipeline.answer(question, language)
            }
            when (outcome) {
                is TextPipelineOutcome.Escalation -> AssistantReply.Escalation(outcome.message)
                is TextPipelineOutcome.EvidencedAnswer -> AssistantReply.Evidenced(outcome.text, outcome.citations)
                is TextPipelineOutcome.Unavailable -> AssistantReply.Unavailable(outcome.message)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            val te = language == SupportedLanguage.TELUGU
            AssistantReply.Unavailable(
                if (te) "స్థానిక AI ప్రతిస్పందన సమయం ముగిసింది లేదా విఫలమైంది."
                else "Offline response timed out or could not be generated safely."
            )
        }
    }

    @Synchronized
    private fun ensureEnginesForPack(pack: InstalledModelPack?) {
        val key = pack?.let { "${it.id}:${it.version}:${it.manifestSha256}" }
        if (key == cachedPackKey) return

        runBlocking { generator?.shutdown() }
        generator = null
        embeddingEngine?.close()
        embeddingEngine = null
        cachedPackKey = key

        if (pack == null) return
        val llmFile = pack.asset("model")
        if (llmFile?.isFile == true && llmFile.extension == "litertlm") {
            generator = LiteRtGroundedGenerator(llmFile)
        }
        val e5Model = pack.asset("embedding_model")
        val e5Tok = pack.asset("embedding_tokenizer")
        if (e5Model?.isFile == true && e5Tok?.isFile == true) {
            embeddingEngine = runCatching { E5QueryEmbeddingEngine(e5Model, e5Tok) }.getOrNull()
        }
    }

    private fun standaloneKnowledgeDbFile(): File? {
        if (!standaloneKnowledge.available()) return null
        val file = File(appContext.noBackupFilesDir, "medical-reference-dev/candidate-default.sqlite")
        return if (file.isFile) file else null
    }

    override fun close() {
        runBlocking { generator?.shutdown() }
        generator = null
        embeddingEngine?.close()
        embeddingEngine = null
        cachedPackKey = null
    }

    private companion object {
        const val INFERENCE_TIMEOUT_MS = 45_000L
    }
}
