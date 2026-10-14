package ai.voicedoc.core.common

/**
 * Defense-in-depth policy mechanics, NOT a clinically validated screening tool.
 * A non-match never guarantees safety. Emergency messaging must work without an LLM.
 */
enum class SafetyIntent { POTENTIALLY_URGENT, GENERAL_INFORMATION, UNCERTAIN_INPUT }
data class SafetyAssessment(val intent: SafetyIntent, val notice: String, val language: SupportedLanguage)

object MedicalSafetyPolicy {
    private val urgentExamples = listOf(
        "chest pain", "can't breathe", "cannot breathe", "trouble breathing",
        "unconscious", "severe bleeding", "stroke symptoms", "heart attack",
        "ఛాతి నొప్పి", "శ్వాస తీసుకోలేక", "శ్వాస ఇబ్బంది", "స్పృహ కోల్పోయి",
        "తీవ్ర రక్తస్రావం"
    )
    private const val EN_URGENT = "This could require urgent attention. Contact local emergency services or a qualified clinician promptly. This app cannot reliably assess or rule out emergencies."
    private const val TE_URGENT = "ఇది అత్యవసర సమస్య కావచ్చు. వెంటనే స్థానిక అత్యవసర సేవలను లేదా అర్హత కలిగిన వైద్య నిపుణులను సంప్రదించండి. ఈ యాప్ అత్యవసర పరిస్థితులను తోసిపుచ్చలేదు."
    private const val EN_UNKNOWN = "I cannot safely interpret this question. Please rephrase or seek advice from a qualified health professional."
    private const val TE_UNKNOWN = "మీ ప్రశ్నను సురక్షితంగా అర్థం చేసుకోలేకపోయాను. దయచేసి స్పష్టంగా మళ్లీ చెప్పండి లేదా వైద్య నిపుణులను సంప్రదించండి."

    fun assess(input: String, language: SupportedLanguage): SafetyAssessment {
        val cleaned = input.trim().lowercase()
        val urgent = urgentExamples.any { cleaned.contains(it) }
        val intent = when {
            urgent -> SafetyIntent.POTENTIALLY_URGENT
            cleaned.length < 3 || cleaned.length > 1000 || cleaned.any { it == '\u0000' } ->
                SafetyIntent.UNCERTAIN_INPUT
            else -> SafetyIntent.GENERAL_INFORMATION
        }
        val te = language == SupportedLanguage.TELUGU
        val notice = when (intent) {
            SafetyIntent.POTENTIALLY_URGENT -> if (te) TE_URGENT else EN_URGENT
            SafetyIntent.UNCERTAIN_INPUT -> if (te) TE_UNKNOWN else EN_UNKNOWN
            SafetyIntent.GENERAL_INFORMATION -> ""
        }
        return SafetyAssessment(intent, notice, language)
    }
}

data class DraftGroundedAnswer(
    val text: String,
    val language: SupportedLanguage,
    val citedSourceIds: Set<String>
)
sealed interface OutputSafetyResult {
    data class Accepted(val text: String, val citations: List<Citation>) : OutputSafetyResult
    data class Rejected(val reasonCode: String) : OutputSafetyResult
}

/**
 * Mechanical validation ONLY. A matching citation ID cannot guarantee claim-level support.
 * True medical groundedness requires clinician-reviewed text entailment and safety evaluation.
 */
object EvidenceOutputGate {
    private val knownUnsafeClaims = listOf(
        "you definitely have", "you certainly have", "no need to see a doctor",
        "guaranteed cure", "it is definitely not an emergency",
        "ఖచ్చితంగా ఈ వ్యాధి", "డాక్టర్ అవసరం లేదు"
    )

    fun check(draft: DraftGroundedAnswer, evidence: List<KnowledgeHit>): OutputSafetyResult {
        if (draft.text.isBlank() || draft.text.length > 2000) {
            return OutputSafetyResult.Rejected("INVALID_LENGTH")
        }
        if (draft.text.any { it == '\u0000' }) return OutputSafetyResult.Rejected("INVALID_TEXT")
        if (knownUnsafeClaims.any { draft.text.contains(it, ignoreCase = true) }) {
            return OutputSafetyResult.Rejected("UNSAFE_CERTAINTY")
        }
        val cited = evidence.filter { it.citation.sourceId in draft.citedSourceIds }
        if (evidence.isEmpty() || draft.citedSourceIds.isEmpty() ||
            cited.map { it.citation.sourceId }.toSet() != draft.citedSourceIds
        ) {
            return OutputSafetyResult.Rejected("NO_VERIFIABLE_EVIDENCE")
        }
        if (cited.size > 5) return OutputSafetyResult.Rejected("TOO_MANY_CITATIONS")
        return OutputSafetyResult.Accepted(draft.text, cited.map { it.citation })
    }
}
