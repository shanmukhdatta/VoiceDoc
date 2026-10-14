package ai.voicedoc.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

data class ImportedKnowledgePack(val size: Long, val sha256: String, val sourceCount: Int, val chunkCount: Int)
data class KnowledgeSourceSummary(
    val id: String,
    val title: String,
    val publisher: String,
    val license: String,
    val language: String,
    val reviewedBy: String
)
data class KnowledgeSearchResult(
    val sourceId: String,
    val chunkId: String,
    val title: String,
    val content: String
)

/**
 * Local import/search for a separately constructed SQLite corpus (rag/ingestion/build_pack.py).
 *
 * IMPORTANT: a schema and checksum do NOT prove a corpus is clinically reviewed or licensed.
 * This viewer is deliberately separate from medical answer generation.
 */
class LocalKnowledgePack(private val context: Context) {
    private val directory get() = File(context.noBackupFilesDir, "medical-reference-dev")
    private val target get() = File(directory, "candidate.sqlite")
    private val byteLimit = 250_000_000L

    fun available(): Boolean = target.isFile && target.length() > 0

    suspend fun installFromDocument(uri: Uri): ImportedKnowledgePack = withContext(Dispatchers.IO) {
        if (available()) throw IOException("Remove the existing pack before importing another.")
        directory.mkdirs()
        val temp = File(directory, "importing.tmp")
        val hash = MessageDigest.getInstance("SHA-256")
        var bytes = 0L
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open selected knowledge file")
            stream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        bytes += read
                        if (bytes > byteLimit) throw IOException("Knowledge pack exceeds the 250 MB limit")
                        hash.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            }
            if (bytes < 512) throw IOException("SQLite file is too small")
            val inspected = validateSchema(temp)
            if (!temp.renameTo(target)) throw IOException("Unable to finish corpus import")
            ImportedKnowledgePack(
                bytes, hash.digest().joinToString("") { "%02x".format(it) },
                inspected.first, inspected.second
            )
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    private fun validateSchema(file: File): Pair<Int, Int> {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            database.rawQuery(
                "SELECT source_id,title,publisher,license,language,reviewed_by FROM sources LIMIT 1", null
            ).use { it.moveToFirst() }
            database.rawQuery(
                "SELECT chunk_id,source_id,language,content FROM chunks LIMIT 1", null
            ).use { it.moveToFirst() }
            fun count(table: String): Int =
                database.rawQuery("SELECT COUNT(*) FROM $table", null).use {
                    it.moveToFirst()
                    it.getInt(0)
                }
            val sources = count("sources")
            val chunks = count("chunks")
            if (sources == 0 || chunks == 0 || sources > 100_000 || chunks > 500_000) {
                throw IOException("Knowledge pack has invalid source/chunk counts")
            }
            database.rawQuery(
                "SELECT COUNT(*) FROM sources WHERE title='' OR publisher='' OR license='' OR reviewed_by='' OR language NOT IN ('en','te')",
                null
            ).use { cursor ->
                cursor.moveToFirst()
                if (cursor.getInt(0) != 0) throw IOException("Missing provenance metadata")
            }
            return sources to chunks
        }
    }

    suspend fun listSources(): List<KnowledgeSourceSummary> = withContext(Dispatchers.IO) {
        if (!available()) return@withContext emptyList()
        SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            database.rawQuery(
                "SELECT source_id,title,publisher,license,language,reviewed_by FROM sources ORDER BY title LIMIT 100",
                null
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(KnowledgeSourceSummary(
                        cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5)
                    ))
                }
            }
        }
    }

    suspend fun search(query: String, language: String): List<KnowledgeSearchResult> =
        withContext(Dispatchers.IO) {
            if (!available() || language !in setOf("en", "te")) return@withContext emptyList()
            val cleaned = query.trim().replace("%", "").replace("_", "").take(80)
            if (cleaned.length < 2) return@withContext emptyList()
            SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery(
                    """
                    SELECT c.source_id,c.chunk_id,s.title,c.content
                    FROM chunks c INNER JOIN sources s ON s.source_id=c.source_id
                    WHERE c.language=? AND c.content LIKE ? ORDER BY c.chunk_id LIMIT 10
                    """.trimIndent(),
                    arrayOf(language, "%$cleaned%")
                ).use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) add(KnowledgeSearchResult(
                            cursor.getString(0), cursor.getString(1),
                            cursor.getString(2), cursor.getString(3).take(1000)
                        ))
                    }
                }
            }
        }

    fun remove() {
        if (target.exists() && !target.delete()) throw IOException("Unable to remove local pack")
    }
}
