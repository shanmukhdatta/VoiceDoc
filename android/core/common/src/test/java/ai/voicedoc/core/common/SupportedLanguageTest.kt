package ai.voicedoc.core.common
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupportedLanguageTest {
    @Test fun onlyTwoLanguagesSupported() {
        assertEquals(setOf("en", "te"), SupportedLanguage.entries.map { it.tag }.toSet())
    }
    @Test fun languageVariantsSupported() {
        assertEquals(SupportedLanguage.TELUGU, SupportedLanguage.fromTag("te-IN"))
        assertEquals(SupportedLanguage.ENGLISH, SupportedLanguage.fromTag("EN_us"))
    }
    @Test fun rejectsTamilAndOthers() {
        assertNull(SupportedLanguage.fromTag("ta-IN"))
        assertNull(SupportedLanguage.fromTag("fr"))
    }
}
