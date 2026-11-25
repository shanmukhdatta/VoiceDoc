package ai.voicedoc.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalSafetyPolicyTest {
    private val en = SupportedLanguage.ENGLISH
    private val te = SupportedLanguage.TELUGU

    @Test fun exampleUrgencyWorksWithoutAnyModel() {
        val result = MedicalSafetyPolicy.assess("I can't breathe", en)
        assertEquals(SafetyIntent.POTENTIALLY_URGENT, result.intent)
        assertTrue(result.notice.contains("emergency services"))
        assertEquals(
            SafetyIntent.POTENTIALLY_URGENT,
            MedicalSafetyPolicy.assess("నాకు ఛాతి నొప్పి", te).intent
        )
    }

    @Test fun unreadableInputAbstains() {
        assertEquals(SafetyIntent.UNCERTAIN_INPUT, MedicalSafetyPolicy.assess("?", en).intent)
        assertEquals(SafetyIntent.GENERAL_INFORMATION, MedicalSafetyPolicy.assess("Explain hydration", en).intent)
    }

    @Test fun modelTextCannotCiteInventedDocuments() {
        val evidence = listOf(KnowledgeHit(
            "A synthetic fixture; not a medical recommendation.",
            Citation("source1", "chunk1", "Synthetic source"),
            0.1
        ))
        val result = EvidenceOutputGate.check(
            DraftGroundedAnswer("Some informational statement", en, setOf("made-up-source")),
            evidence
        )
        assertEquals(OutputSafetyResult.Rejected("NO_VERIFIABLE_EVIDENCE"), result)
    }

    @Test fun unsafeCertaintyRejectedDespiteCitation() {
        val evidence = listOf(KnowledgeHit("fixture", Citation("s", "c", "Test"), 0.1))
        assertEquals(
            OutputSafetyResult.Rejected("UNSAFE_CERTAINTY"),
            EvidenceOutputGate.check(
                DraftGroundedAnswer("You definitely have the disease", en, setOf("s")),
                evidence
            )
        )
    }

    @Test fun validIdsAreOnlyMechanicalAcceptance() {
        val evidence = listOf(KnowledgeHit("fixture", Citation("s", "c", "Test"), 0.1))
        val result = EvidenceOutputGate.check(
            DraftGroundedAnswer("A bounded, non-diagnostic statement.", en, setOf("s")),
            evidence
        )
        assertTrue(result is OutputSafetyResult.Accepted)
    }
}
