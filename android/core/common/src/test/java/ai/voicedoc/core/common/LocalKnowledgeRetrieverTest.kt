package ai.voicedoc.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalKnowledgeRetrieverTest {
    private val en = SupportedLanguage.ENGLISH
    private val te = SupportedLanguage.TELUGU
    private val sources = listOf(
        MedicalSourceRecord("approved", "Example checked source", "Test publisher", "MIT", true, true, en),
        MedicalSourceRecord("unreviewed", "Unreviewed source", "Test publisher", "MIT", true, false, en),
        MedicalSourceRecord("te", "తెలుగు", "Test publisher", "MIT", true, true, te)
    )
    private val chunks = listOf(
        MedicalChunkRecord("c1", "approved", en, "Information about staying healthy and accessing care.", floatArrayOf(1f, 0f)),
        MedicalChunkRecord("c2", "unreviewed", en, "Dangerous content about healthy care", floatArrayOf(1f, 0f)),
        MedicalChunkRecord("c3", "te", te, "ఆరోగ్య సమాచారం మరియు సహాయం", floatArrayOf(0f, 1f))
    )
    private val retriever = LocalKnowledgeRetriever(sources, chunks)

    @Test fun lexicalMatchRequiresRightsAndReview() {
        val hits = retriever.search("healthy care", en)
        assertEquals(1, hits.size)
        assertEquals("approved", hits.first().citation.sourceId)
        assertEquals("c1", hits.first().citation.chunkId)
    }

    @Test fun TeluguDoesNotReturnEnglishTextAsLocalMatch() {
        val hits = retriever.search("ఆరోగ్య సమాచారం", te)
        assertEquals(1, hits.size)
        assertEquals("c3", hits.first().citation.chunkId)
    }

    @Test fun exactCosineRetrievalIsIndependentOfLexical() {
        val hits = retriever.search("unrelated", en, queryEmbedding = floatArrayOf(1f, 0f))
        assertEquals(1, hits.size)
        assertEquals("c1", hits.first().citation.chunkId)
    }

    @Test fun unknownEvidenceDoesNotProduceMadeUpCitations() {
        assertTrue(retriever.search("no matching unique words", en).isEmpty())
        val prompt = BoundedEvidencePrompt.build("What is this?", en, emptyList())
        assertFalse(prompt.contains("Source fabricated"))
        assertTrue(prompt.contains("state uncertainty"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun boundsLargeRetrieval() {
        retriever.search("care", en, topK = 500)
    }
}
