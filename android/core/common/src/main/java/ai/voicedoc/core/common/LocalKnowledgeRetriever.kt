package ai.voicedoc.core.common

import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln

/**
 * In-memory, entirely offline and deterministic lexical retrieval baseline.
 * Source packages must have passed an independent license and medical review.
 * This is NOT semantic retrieval and does not validate clinical correctness.
 */
class LocalKnowledgeRetriever private constructor(
    private val entries: List<Evidence>,
    private val minimumScore: Double
) : OfflineRetriever {
    init { require(minimumScore > 0.0 && minimumScore.isFinite()) }

    override suspend fun retrieve(
        query: String,
        language: SupportedLanguage,
        limit: Int
    ): List<Evidence> {
        if (limit <= 0 || query.isBlank()) return emptyList()
        val terms = tokenize(query).toSet()
        if (terms.isEmpty()) return emptyList()
        val corpus = entries.filter { it.language == language }
        if (corpus.isEmpty()) return emptyList()
        val size = corpus.size
        val documentFrequency = HashMap<String, Int>()
        val indexed = corpus.map { evidence ->
            val words = tokenize(evidence.text).toSet()
            for (term in words) documentFrequency[term] = (documentFrequency[term] ?: 0) + 1
            evidence to words
        }
        return indexed.asSequence().map { (evidence, words) ->
            // Smoothed inverse-document-frequency, normalized for query length.
            val score = terms.sumOf { term ->
                if (term in words) ln(1.0 + (size - (documentFrequency[term] ?: 0) + 0.5) /
                    ((documentFrequency[term] ?: 0) + 0.5)) else 0.0
            } / terms.size
            evidence to score
        }.filter { (_, score) -> score >= minimumScore }
            .sortedWith(compareByDescending<Pair<Evidence, Double>> { it.second }
                .thenBy { it.first.id })
            .take(limit.coerceAtMost(10))
            .map { it.first }
            .toList()
    }

    companion object {
        private val wordPattern = Regex("[\\p{L}\\p{N}]+")
        fun tokenize(text: String): List<String> {
            val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
            return wordPattern.findAll(normalized).map { it.value }.toList()
        }

        /** Reject malformed, duplicate, unreviewed and incomplete source records. */
        fun fromReviewedEvidence(
            candidates: List<Evidence>,
            minimumScore: Double = 0.05
        ): LocalKnowledgeRetriever {
            require(candidates.size <= 50_000) { "Knowledge pack exceeds baseline size limit" }
            require(candidates.map { it.id }.distinct().size == candidates.size) {
                "Knowledge pack contains duplicate chunk IDs"
            }
            for (entry in candidates) {
                require(entry.verified) { "Unverified knowledge must not enter the index" }
                require(entry.id.isNotBlank() && entry.sourceTitle.isNotBlank() &&
                    entry.sourceUrl.startsWith("https://") && entry.license.isNotBlank()) {
                    "Knowledge chunks require complete, traceable provenance"
                }
                require(entry.text.isNotBlank() && entry.text.length <= 8_000) {
                    "Invalid knowledge chunk length"
                }
            }
            require(minimumScore.isFinite() && minimumScore > 0.0)
            return LocalKnowledgeRetriever(candidates.toList(), minimumScore)
        }
    }
}
