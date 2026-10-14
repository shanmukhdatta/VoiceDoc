package ai.voicedoc.core.common

/**
 * Testable pipeline boundary: every stage is offline and replaceable.
 * A production generator cannot be enabled without model + rights + safety validation.
 */
interface StructuredLocalGenerator {
    suspend fun generate(
        userQuestion: String,
        language: SupportedLanguage,
        evidence: List<KnowledgeHit>
    ): DraftGroundedAnswer
}

sealed interface TextPipelineOutcome {
    data class Escalation(val message: String) : TextPipelineOutcome
    data class Unavailable(val message: String) : TextPipelineOutcome
    data class EvidencedAnswer(val text: String, val citations: List<Citation>) : TextPipelineOutcome
}

class OfflineTextPipeline(
    private val retriever: MedicalRetriever,
    private val generator: StructuredLocalGenerator
) {
    suspend fun answer(question: String, language: SupportedLanguage): TextPipelineOutcome {
        val safety = MedicalSafetyPolicy.assess(question, language)
        if (safety.intent == SafetyIntent.POTENTIALLY_URGENT) {
            return TextPipelineOutcome.Escalation(safety.notice)
        }
        if (safety.intent == SafetyIntent.UNCERTAIN_INPUT) {
            return TextPipelineOutcome.Unavailable(safety.notice)
        }
        val hits = try {
            retriever.search(question, language, topK = 3)
        } catch (_: Exception) {
            return unavailable(language)
        }
        if (hits.isEmpty()) return unavailable(language)

        // Never call an untrusted model without bounded evidence and validated available assets.
        val draft = try {
            generator.generate(question, language, hits)
        } catch (_: Exception) {
            return unavailable(language)
        }
        if (draft.language != language) return unavailable(language)
        return when (val checked = EvidenceOutputGate.check(draft, hits)) {
            is OutputSafetyResult.Accepted ->
                TextPipelineOutcome.EvidencedAnswer(checked.text, checked.citations)
            is OutputSafetyResult.Rejected -> unavailable(language)
        }
    }

    private fun unavailable(language: SupportedLanguage): TextPipelineOutcome.Unavailable {
        val te = language == SupportedLanguage.TELUGU
        return TextPipelineOutcome.Unavailable(
            if (te)
                "ధృవీకరించిన వైద్య ఆధారాలు లేదా ఆఫ్‌లైన్ AI మోడల్ ప్రస్తుతం అందుబాటులో లేదు. నేను నమ్మదగిన వైద్య సమాధానం ఇవ్వలేను."
            else
                "Verified medical evidence or the local AI model is unavailable. I cannot give a reliable medical answer."
        )
    }
}

/** The ONLY production default until approved model/runtime is installed. */
object UnavailableStructuredGenerator : StructuredLocalGenerator {
    override suspend fun generate(
        userQuestion: String,
        language: SupportedLanguage,
        evidence: List<KnowledgeHit>
    ): DraftGroundedAnswer {
        throw EngineUnavailableException("Local SLM")
    }
}
