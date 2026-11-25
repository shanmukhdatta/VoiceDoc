package ai.voicedoc.app.data

import android.database.sqlite.SQLiteDatabase
import ai.voicedoc.core.common.Citation
import ai.voicedoc.core.common.KnowledgeHit
import ai.voicedoc.core.common.MedicalRetriever
import ai.voicedoc.core.common.QueryEmbeddingEngine
import ai.voicedoc.core.common.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Offline hybrid retriever over a verified VoiceDoc SQLite knowledge database.
 * Combines FTS5/LIKE lexical retrieval and optional E5-small dense cosine similarity via
 * Reciprocal Rank Fusion (RRF), enforcing language filtering and bounded evidence length.
 */
class SqliteHybridRetriever(
    private val dbFileProvider: () -> File?,
    private val embeddingEngineProvider: () -> QueryEmbeddingEngine? = { null }
) : MedicalRetriever {

    override suspend fun search(
        query: String,
        language: SupportedLanguage,
        topK: Int
    ): List<KnowledgeHit> = withContext(Dispatchers.IO) {
        require(topK in 1..10) { "topK must be between 1 and 10" }
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val dbFile = dbFileProvider() ?: return@withContext emptyList()
        if (!dbFile.isFile || dbFile.length() < 512) return@withContext emptyList()

        val queryVector = runCatching { embeddingEngineProvider()?.embedQuery(cleanQuery) }.getOrNull()

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            verifyIntegrity(db)

            val lexicalHits = queryLexical(db, cleanQuery, language.tag, limit = 20)
            val denseHits = if (queryVector != null && hasEmbeddingsTable(db)) {
                queryDense(db, queryVector, language.tag, limit = 20)
            } else {
                emptyList()
            }

            if (lexicalHits.isEmpty() && denseHits.isEmpty()) return@withContext emptyList()

            val rrfScores = linkedMapOf<String, Double>()
            val candidates = linkedMapOf<String, KnowledgeHit>()

            for ((rank, hit) in lexicalHits.withIndex()) {
                val id = hit.citation.chunkId
                candidates[id] = hit
                rrfScores[id] = rrfScores.getOrDefault(id, 0.0) + 1.0 / (60 + rank + 1)
            }
            for ((rank, hit) in denseHits.withIndex()) {
                val id = hit.citation.chunkId
                candidates.putIfAbsent(id, hit)
                rrfScores[id] = rrfScores.getOrDefault(id, 0.0) + 1.0 / (60 + rank + 1)
            }

            rrfScores.entries
                .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
                .take(topK)
                .map { (chunkId, score) ->
                    candidates.getValue(chunkId).copy(score = score)
                }
        }
    }

    private fun verifyIntegrity(db: SQLiteDatabase) {
        db.rawQuery("PRAGMA quick_check(1)", null).use { cursor ->
            if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                throw IOException("Knowledge SQLite database failed integrity check")
            }
        }
    }

    private fun hasEmbeddingsTable(db: SQLiteDatabase): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name='chunk_embeddings'",
            null
        ).use { it.moveToFirst() }

    private fun queryLexical(
        db: SQLiteDatabase,
        query: String,
        languageTag: String,
        limit: Int
    ): List<KnowledgeHit> {
        val terms = TOKEN_REGEX.findAll(query.lowercase()).map { it.value }.filter { it.length >= 2 }.take(8).toList()
        if (terms.isEmpty()) return emptyList()
        val ftsQuery = terms.joinToString(" OR ") { "\"${it.replace("\"", "")}\"" }
        val ftsResults = runCatching {
            db.rawQuery(
                """
                SELECT c.chunk_id, c.source_id, s.title, c.content
                FROM chunks_fts f
                INNER JOIN chunks c ON c.chunk_id = f.chunk_id
                INNER JOIN sources s ON s.source_id = c.source_id
                WHERE c.language = ? AND chunks_fts MATCH ?
                ORDER BY bm25(chunks_fts)
                LIMIT ?
                """.trimIndent(),
                arrayOf(languageTag, ftsQuery, limit.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            KnowledgeHit(
                                text = cursor.getString(3).take(MAX_CHUNK_CHARS),
                                citation = Citation(
                                    sourceId = cursor.getString(1),
                                    chunkId = cursor.getString(0),
                                    title = cursor.getString(2)
                                ),
                                score = 1.0
                            )
                        )
                    }
                }
            }
        }.getOrNull()
        if (!ftsResults.isNullOrEmpty()) return ftsResults

        // Fallback LIKE search if FTS5 query syntax fails on punctuation/Unicode edge cases.
        val cleaned = query.replace("%", "").replace("_", "").take(60)
        return db.rawQuery(
            """
            SELECT c.chunk_id, c.source_id, s.title, c.content
            FROM chunks c
            INNER JOIN sources s ON s.source_id = c.source_id
            WHERE c.language = ? AND c.content LIKE ?
            ORDER BY c.chunk_id
            LIMIT ?
            """.trimIndent(),
            arrayOf(languageTag, "%$cleaned%", limit.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        KnowledgeHit(
                            text = cursor.getString(3).take(MAX_CHUNK_CHARS),
                            citation = Citation(
                                sourceId = cursor.getString(1),
                                chunkId = cursor.getString(0),
                                title = cursor.getString(2)
                            ),
                            score = 0.5
                        )
                    )
                }
            }
        }
    }

    private fun queryDense(
        db: SQLiteDatabase,
        queryVector: FloatArray,
        languageTag: String,
        limit: Int
    ): List<KnowledgeHit> {
        val scored = ArrayList<KnowledgeHit>()
        db.rawQuery(
            """
            SELECT c.chunk_id, c.source_id, s.title, c.content, e.dim, e.embedding
            FROM chunk_embeddings e
            INNER JOIN chunks c ON c.chunk_id = e.chunk_id
            INNER JOIN sources s ON s.source_id = c.source_id
            WHERE c.language = ?
            """.trimIndent(),
            arrayOf(languageTag)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val dim = cursor.getInt(4)
                if (dim != queryVector.size) continue
                val blob = cursor.getBlob(5) ?: continue
                if (blob.size != dim * 4) continue
                val vec = decodeFloat32Le(blob, dim)
                val sim = cosineSimilarity(queryVector, vec)
                if (sim > 0.0) {
                    scored.add(
                        KnowledgeHit(
                            text = cursor.getString(3).take(MAX_CHUNK_CHARS),
                            citation = Citation(
                                sourceId = cursor.getString(1),
                                chunkId = cursor.getString(0),
                                title = cursor.getString(2)
                            ),
                            score = sim
                        )
                    )
                }
            }
        }
        return scored.sortedWith(compareByDescending<KnowledgeHit> { it.score }.thenBy { it.citation.chunkId }).take(limit)
    }

    private fun decodeFloat32Le(bytes: ByteArray, dim: Int): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(dim) { buffer.float }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var aa = 0.0
        var bb = 0.0
        for (i in a.indices) {
            val av = a[i].toDouble()
            val bv = b[i].toDouble()
            dot += av * bv
            aa += av * av
            bb += bv * bv
        }
        if (aa <= 0.0 || bb <= 0.0) return 0.0
        val value = dot / sqrt(aa * bb)
        return if (value.isFinite()) value else 0.0
    }

    private companion object {
        const val MAX_CHUNK_CHARS = 1200
        val TOKEN_REGEX = Regex("""[\p{L}\p{N}]+""")
    }
}
