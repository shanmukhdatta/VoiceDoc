package ai.voicedoc.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSafetyFallbackTest {
    @Test fun recognizesExampleWarnings() {
        assertTrue(OfflineSafetyFallback.requiresUrgencyNotice("I have chest pain"))
        assertTrue(OfflineSafetyFallback.requiresUrgencyNotice("నాకు ఛాతి నొప్పి"))
    }
    @Test fun negativeIsNotMedicalReassurance() {
        assertFalse(OfflineSafetyFallback.requiresUrgencyNotice("What is wellness?"))
        assertTrue(OfflineSafetyFallback.respond("What is wellness?", false).contains("cannot provide"))
    }
    @Test fun noDiagnosesOrFabricatedSources() {
        val response = OfflineSafetyFallback.respond("What disease is this?", false)
        assertFalse(response.contains("diagnosed", ignoreCase = true))
        assertFalse(response.contains("http", ignoreCase = true))
    }
}
