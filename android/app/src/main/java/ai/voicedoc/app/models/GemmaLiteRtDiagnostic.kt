package ai.voicedoc.app.models

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Actual LiteRT-LM CPU test adapter for a user-supplied, valid .litertlm artifact.
 * Only a benign fixed test prompt is permitted in this diagnostic.
 * Not connected to medical chat or trusted knowledge / safety evaluation.
 */
class GemmaLiteRtDiagnostic {
    suspend fun runFixedBenchmark(candidate: File): String = withContext(Dispatchers.IO) {
        require(candidate.isFile && candidate.extension.equals("litertlm", ignoreCase = true))
        val started = System.nanoTime()
        Engine(EngineConfig(modelPath = candidate.absolutePath, backend = Backend.CPU())).use { engine ->
            engine.initialize()
            val loaded = System.nanoTime()
            val response = engine.createConversation().use { conversation ->
                conversation.sendMessage("Reply with exactly one short greeting in English.").toString()
            }
            val finished = System.nanoTime()
            "Loaded in ${(loaded-started)/1_000_000} ms, generated in ${(finished-loaded)/1_000_000} ms. Raw diagnostic response: ${response.take(250)}"
        }
    }
}
