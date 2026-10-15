package ai.voicedoc.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class OfflinePipelineTest {
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) { result = value }
        })
        return requireNotNull(result).getOrThrow()
    }

    private val notUrgent = object : UrgencyGate {
        override fun needsUrgentCare(text: String, language: SupportedLanguage) = false
    }
    private fun retriever(vararg evidence: Evidence) = object : OfflineRetriever {
        override suspend fun retrieve(query: String, language: SupportedLanguage, limit: Int) = evidence.toList()
    }
    private val verified = Evidence("kb1", "Verified guide", "https://example.org/guide",
        SupportedLanguage.ENGLISH, "General reference text.", true, "CC BY 4.0")

    @Test fun refusesWhenEvidenceMissing() {
        val result = runSuspend { OfflinePipeline(retriever(), null, notUrgent).ask("What is a cold?", SupportedLanguage.ENGLISH) }
        assertEquals(Status.INSUFFICIENT_EVIDENCE, result.status)
    }

    @Test fun unverifiedEvidenceIsNeverPassedToModel() {
        val bad = verified.copy(verified = false)
        val model = object : OfflineGenerator {
            override suspend fun generate(prompt: String, language: SupportedLanguage): String {
                error("Must not invoke generator")
            }
        }
        val result = runSuspend { OfflinePipeline(retriever(bad), model, notUrgent).ask("question", SupportedLanguage.ENGLISH) }
        assertEquals(Status.INSUFFICIENT_EVIDENCE, result.status)
    }

    @Test fun urgentCaseBypassesRetrievalAndGenerator() {
        val urgent = object : UrgencyGate {
            override fun needsUrgentCare(text: String, language: SupportedLanguage) = true
        }
        val result = runSuspend { OfflinePipeline(retriever(verified), null, urgent).ask("help", SupportedLanguage.ENGLISH) }
        assertEquals(Status.URGENT, result.status)
        assertTrue(result.evidence.isEmpty())
    }

    @Test fun modelMissingIsClearlyReported() {
        val result = runSuspend { OfflinePipeline(retriever(verified), null, notUrgent).ask("question", SupportedLanguage.ENGLISH) }
        assertEquals(Status.MODEL_UNAVAILABLE, result.status)
    }

    @Test fun answeredResponseRetainsProvenance() {
        val model = object : OfflineGenerator {
            override suspend fun generate(prompt: String, language: SupportedLanguage): String {
                assertTrue(prompt.contains("Verified guide"))
                return "General information only."
            }
        }
        val result = runSuspend { OfflinePipeline(retriever(verified), model, notUrgent).ask("question", SupportedLanguage.ENGLISH) }
        assertEquals(Status.ANSWERED, result.status)
        assertEquals("kb1", result.evidence.single().id)
    }
}
