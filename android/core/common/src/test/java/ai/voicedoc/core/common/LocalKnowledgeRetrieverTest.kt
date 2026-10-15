package ai.voicedoc.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class LocalKnowledgeRetrieverTest {
    private fun <T> runSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { outcome = result }
        })
        return requireNotNull(outcome).getOrThrow()
    }
    private fun item(
        id: String, text: String, language: SupportedLanguage = SupportedLanguage.ENGLISH
    ) = Evidence(id, "Reviewed public health source", "https://example.org/source",
        language, text, true, "CC BY 4.0")

    @Test fun matchesRelevantContentAndPreservesSource() {
        val index = LocalKnowledgeRetriever.fromReviewedEvidence(listOf(
            item("a", "hydration water fluids"),
            item("b", "exercise strength movement")
        ))
        val results = runSuspend { index.retrieve("water fluids", SupportedLanguage.ENGLISH, 2) }
        assertEquals("a", results.first().id)
        assertTrue(results.first().sourceUrl.startsWith("https://"))
    }

    @Test fun doesNotCrossLanguageBoundary() {
        val index = LocalKnowledgeRetriever.fromReviewedEvidence(listOf(
            item("en", "water"),
            item("te", "water", SupportedLanguage.TELUGU)
        ))
        val result = runSuspend { index.retrieve("water", SupportedLanguage.TELUGU, 3) }
        assertEquals(listOf("te"), result.map { it.id })
    }

    @Test fun returnsNoEvidenceForUnrelatedQuery() {
        val index = LocalKnowledgeRetriever.fromReviewedEvidence(listOf(item("one", "blood pressure")))
        val matches = runSuspend { index.retrieve("unrelatedword", SupportedLanguage.ENGLISH, 3) }
        assertTrue(matches.isEmpty())
    }

    @Test fun refusesUnreviewedOrUnlicensedContent() {
        try {
            LocalKnowledgeRetriever.fromReviewedEvidence(listOf(item("x", "text").copy(verified = false)))
            throw AssertionError("Expected rejection")
        } catch (_: IllegalArgumentException) {}
        try {
            LocalKnowledgeRetriever.fromReviewedEvidence(listOf(item("x", "text").copy(license = "")))
            throw AssertionError("Expected rejection")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun refusesDuplicateChunkIds() {
        try {
            LocalKnowledgeRetriever.fromReviewedEvidence(listOf(item("x", "a"), item("x", "b")))
            throw AssertionError("Expected duplicate rejection")
        } catch (_: IllegalArgumentException) {}
    }

    @Test fun supportsIndicScriptTokenization() {
        val tokens = LocalKnowledgeRetriever.tokenize("తెలుగు ఆరోగ్యం Tamil தமிழ்")
        assertTrue(tokens.any { it.contains("తెలుగు") })
        assertTrue(tokens.any { it.contains("தமிழ்") })
    }
}
