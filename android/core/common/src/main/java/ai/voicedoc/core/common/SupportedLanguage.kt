package ai.voicedoc.core.common
import java.util.Locale

/** Only English and Telugu are supported for this release. */
enum class SupportedLanguage(val tag: String) {
    ENGLISH("en"), TELUGU("te");
    companion object {
        fun fromTag(tag: String): SupportedLanguage? {
            val code = tag.trim().replace('_', '-').substringBefore('-').lowercase(Locale.ROOT)
            return entries.firstOrNull { it.tag == code }
        }
    }
}
