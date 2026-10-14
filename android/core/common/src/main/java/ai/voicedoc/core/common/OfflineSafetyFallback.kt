package ai.voicedoc.core.common

/**
 * User-facing static message adapter for development builds.
 * Delegates to the independent safety policy; non-matches do NOT mean safe.
 * Does not generate medical diagnoses or fabricate citations.
 */
object OfflineSafetyFallback {
    fun requiresUrgencyNotice(input: String): Boolean =
        MedicalSafetyPolicy.assess(input, SupportedLanguage.ENGLISH).intent ==
            SafetyIntent.POTENTIALLY_URGENT

    fun respond(input: String, telugu: Boolean): String {
        val language = if (telugu) SupportedLanguage.TELUGU else SupportedLanguage.ENGLISH
        val assessment = MedicalSafetyPolicy.assess(input, language)
        if (assessment.notice.isNotBlank()) return assessment.notice
        return if (telugu)
            "ఈ అభివృద్ధి బిల్డ్‌లో ధృవీకరించిన వైద్య సమాచార ప్యాక్ మరియు స్థానిక AI మోడల్ లేవు. నేను మీ ప్రశ్నకు నమ్మదగిన వైద్య సమాధానం ఇవ్వలేను. అవసరమైతే వైద్య నిపుణులను సంప్రదించండి."
        else
            "This development build does not have a verified medical knowledge pack or local AI model. I cannot provide a reliable medical answer yet. Please consult a qualified professional when appropriate."
    }
}
