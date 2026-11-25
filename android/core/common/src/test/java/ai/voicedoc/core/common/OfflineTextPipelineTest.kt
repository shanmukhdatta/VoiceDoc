package ai.voicedoc.core.common

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineTextPipelineTest {
    private val en = SupportedLanguage.ENGLISH
    private val evidence = listOf(KnowledgeHit(
        "Synthetic reviewed fixture for test only.",
        Citation("synthetic", "c1", "Synthetic"), 1.0
    ))
    private fun retrieval(hits: List<KnowledgeHit>): MedicalRetriever = object : MedicalRetriever {
        override suspend fun search(query: String, language: SupportedLanguage, topK: Int) = hits
    }
    private fun fake(answer: DraftGroundedAnswer): StructuredLocalGenerator =
        object : StructuredLocalGenerator {
            override suspend fun generate(
                userQuestion: String, language: SupportedLanguage, evidence: List<KnowledgeHit>,
                maxNewTokens: Int
            ) = answer
        }

    @Test fun potentialUrgencyBypassesRetrieverAndGenerator() = runBlocking {
        val pipeline = OfflineTextPipeline(
            retrieval(evidence), fake(DraftGroundedAnswer("Not needed", en, setOf("synthetic")))
        )
        assertTrue(pipeline.answer("I can't breathe", en) is TextPipelineOutcome.Escalation)
    }

    @Test fun emptyKnowledgeFailsClosed() = runBlocking {
        val pipeline = OfflineTextPipeline(
            retrieval(emptyList()), fake(DraftGroundedAnswer("Not needed", en, setOf("synthetic")))
        )
        assertTrue(pipeline.answer("Explain a concept", en) is TextPipelineOutcome.Unavailable)
    }

    @Test fun missingLlmFailsClosed() = runBlocking {
        val pipeline = OfflineTextPipeline(retrieval(evidence), UnavailableStructuredGenerator)
        assertTrue(pipeline.answer("Explain a concept", en) is TextPipelineOutcome.Unavailable)
    }

    @Test fun inventedCitationRejected() = runBlocking {
        val pipeline = OfflineTextPipeline(
            retrieval(evidence), fake(DraftGroundedAnswer("Some text", en, setOf("fake")))
        )
        assertTrue(pipeline.answer("Explain a concept", en) is TextPipelineOutcome.Unavailable)
    }

    @Test fun syntheticValidatedPathReturnsCitation() = runBlocking {
        val pipeline = OfflineTextPipeline(
            retrieval(evidence), fake(DraftGroundedAnswer("Synthetic nonclinical text", en, setOf("synthetic")))
        )
        val result = pipeline.answer("Explain a concept", en)
        assertTrue(result is TextPipelineOutcome.EvidencedAnswer)
        result as TextPipelineOutcome.EvidencedAnswer
        assertEquals("synthetic", result.citations.first().sourceId)
    }

    @Test fun generationBudgetIsPassedToTheGenerator() = runBlocking {
        var received = 0
        val generator = object : StructuredLocalGenerator {
            override suspend fun generate(
                userQuestion: String, language: SupportedLanguage, evidence: List<KnowledgeHit>,
                maxNewTokens: Int
            ): DraftGroundedAnswer {
                received = maxNewTokens
                return DraftGroundedAnswer("Synthetic answer", language, setOf("synthetic"))
            }
        }
        OfflineTextPipeline(retrieval(evidence), generator, maxNewTokens = 72)
            .answer("Explain a concept", en)
        assertEquals(72, received)
    }

    @Test fun groundedParserExtractsOnlyExplicitMarkersAndLeavesUnknownIdsForGate() {
        val parsed = GroundedAnswerParser.parse(
            "A cautious answer. [source:approved-1] [source:invented]", en
        )
        assertEquals("A cautious answer.", parsed.text)
        assertEquals(setOf("approved-1", "invented"), parsed.citedSourceIds)
        assertTrue(GroundedAnswerParser.parse("No source marker", en).citedSourceIds.isEmpty())
        assertTrue(EvidenceOutputGate.check(parsed, evidence).let { it is OutputSafetyResult.Rejected })
        val known = GroundedAnswerParser.parse("A cautious answer. [source:synthetic]", en)
        assertTrue(EvidenceOutputGate.check(known, evidence) is OutputSafetyResult.Accepted)
    }
}
