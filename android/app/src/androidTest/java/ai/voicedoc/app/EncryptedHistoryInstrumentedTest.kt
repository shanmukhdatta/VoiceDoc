package ai.voicedoc.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.voicedoc.app.data.EncryptedHistoryStore
import ai.voicedoc.app.data.SavedConversationTurn
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedHistoryInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var store: EncryptedHistoryStore

    @Before fun setup() {
        store = EncryptedHistoryStore(context, namespace = "instrumentation-only")
        store.erase()
    }

    @After fun cleanup() { store.erase() }

    @Test fun encryptedHistoryRoundTripAndDelete() {
        val turns = listOf(
            SavedConversationTurn(true, "Synthetic fixture private test text"),
            SavedConversationTurn(false, "No model is installed")
        )
        store.write(turns)
        assertEquals(turns, store.read())
        val raw = store.ciphertextPathForTests().readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(raw.contains("Synthetic fixture private test text"))
        store.erase()
        assertTrue(store.read().isEmpty())
        assertFalse(store.ciphertextPathForTests().exists())
    }

    @Test(expected = IllegalArgumentException::class)
    fun excessiveMessageIsRejected() {
        store.write(listOf(SavedConversationTurn(true, "x".repeat(1001))))
    }
}
