package ai.voicedoc.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageAndVadTest {
    @Test fun preservesImportantNegationNumbersAndLanguage() {
        val transcript = "నాకు no pain 12 hours"
        val result = BilingualNormalizer.analyze("  $transcript  ")
        assertTrue(result.codeSwitched)
        assertTrue(result.normalized.contains("no"))
        assertTrue(result.normalized.contains("12"))
        assertEquals(transcript, result.normalized)
    }

    @Test fun englishOnlyIsNotCodeSwitch() {
        val result = BilingualNormalizer.analyze("I do not know")
        assertFalse(result.hasTelugu)
        assertFalse(result.codeSwitched)
    }

    @Test fun silenceAndStrongSignalAreSeparatedByEnergy() {
        val detector = EnergyVoiceActivityDetector()
        assertTrue(detector.detect(ShortArray(16_000)).isEmpty())
        val signal = ShortArray(16_000) { if (it in 3200..6400) 8000 else 0 }
        val intervals = detector.detect(signal)
        assertEquals(1, intervals.size)
        assertTrue(intervals[0].startSample >= 3000)
        assertTrue(intervals[0].endSample >= 6400)
    }

    @Test fun trailingSpeechIsNotDropped() {
        val pcm = ShortArray(500) { 5000 }
        val intervals = EnergyVoiceActivityDetector().detect(pcm)
        assertEquals(1, intervals.size)
        assertEquals(pcm.size, intervals[0].endSample)
    }
}
