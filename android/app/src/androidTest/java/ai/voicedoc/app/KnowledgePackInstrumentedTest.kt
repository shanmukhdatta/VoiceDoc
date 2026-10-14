package ai.voicedoc.app

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.voicedoc.app.data.LocalKnowledgePack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class KnowledgePackInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(
        File(context.noBackupFilesDir, "medical-reference-dev"),
        "candidate-instrumentation.sqlite"
    )
    private val pack get() = LocalKnowledgePack(context, "instrumentation")

    @Before fun prepareSyntheticKnowledge() {
        file.parentFile!!.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("""
                CREATE TABLE sources(
                  source_id TEXT PRIMARY KEY,publisher TEXT,title TEXT,
                  license TEXT,language TEXT,reviewed_by TEXT,sha256 TEXT)
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE chunks(
                  chunk_id TEXT PRIMARY KEY,source_id TEXT,language TEXT,content TEXT)
            """.trimIndent())
            db.execSQL("INSERT INTO sources VALUES(?,?,?,?,?,?,?)",
                arrayOf("fixture", "Synthetic publisher", "Test-only", "test-license",
                        "en", "SIMULATED_UNIT_TEST", "not-a-real-source"))
            db.execSQL("INSERT INTO chunks VALUES(?,?,?,?)",
                arrayOf("fixture:1", "fixture", "en", "hello synthetic test phrase"))
        }
    }

    @After fun cleanup() { file.delete() }

    @Test fun localSourcesAndTextSearchActuallyReadSQLite() = runBlocking {
        assertTrue(pack.available())
        assertEquals("Test-only", pack.listSources().single().title)
        val results = pack.search("hello", "en")
        assertEquals(1, results.size)
        assertEquals("fixture", results.first().sourceId)
        assertTrue(pack.search("hello", "te").isEmpty())
    }
}
