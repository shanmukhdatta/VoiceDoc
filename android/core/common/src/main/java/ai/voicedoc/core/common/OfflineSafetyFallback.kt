package ai.voicedoc.core.common

/**
 * Independent *conservative* fallback. NOT a medically validated emergency classifier.
 * Input keywords are merely examples: missing keywords never means a situation is safe.
 * Never presents diagnoses, prescriptions, or invented references.
 */
object OfflineSafetyFallback {
    private val warningTerms = listOf(
        "chest pain", "can't breathe", "cannot breathe", "unconscious", "severe bleeding",
        "trouble breathing", "difficulty breathing", "heart attack", "stroke",
        "ఛాతి నొప్పి", "శ్వాస తీసుకోలేక", "శ్వాస కష్టం", "స్పృహ కోల్పోయి", "తీవ్ర రక్తస్రావం"
    )

    fun requiresUrgencyNotice(input: String): Boolean {
        val lower = input.lowercase()
        return warningTerms.any { lower.contains(it) }
    }

    fun respond(input: String, telugu: Boolean): String {
        if (requiresUrgencyNotice(input)) {
            return if (telugu)
                "ఇది అత్యవసర సమస్య కావచ్చు. వెంటనే స్థానిక అత్యవసర సేవలు లేదా వైద్య నిపుణులను సంప్రదించండి. ఈ యాప్ అత్యవసర పరిస్థితులను నిర్ధారించలేదు లేదా తోసిపుచ్చలేదు."
            else
                "This could require urgent medical attention. Contact local emergency services or a qualified clinician promptly. This app cannot assess or rule out emergencies."
        }
        return if (telugu)
            "ఈ అభివృద్ధి బిల్డ్‌లో ధృవీకరించిన వైద్య సమాచార ప్యాక్ మరియు స్థానిక AI మోడల్ లేవు. నేను మీ ప్రశ్నకు నమ్మదగిన వైద్య సమాధానం ఇవ్వలేను. అవసరమైతే వైద్య నిపుణులను సంప్రదించండి."
        else
            "This development build does not have a verified medical knowledge pack or local AI model. I cannot provide a reliable medical answer yet. Please consult a qualified professional when appropriate."
    }
}
