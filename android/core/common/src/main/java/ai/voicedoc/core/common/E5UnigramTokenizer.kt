package ai.voicedoc.core.common

import java.io.File
import java.text.Normalizer
import kotlin.math.min

data class E5TokenizedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is E5TokenizedInput) return false
        return inputIds.contentEquals(other.inputIds) && attentionMask.contentEquals(other.attentionMask)
    }

    override fun hashCode(): Int = 31 * inputIds.contentHashCode() + attentionMask.contentHashCode()
}

/**
 * Pure-Kotlin HuggingFace `tokenizer.json` (SentencePiece Unigram / XLM-RoBERTa) tokenizer
 * compatible with `intfloat/multilingual-e5-small`.
 *
 * Implements NFKC normalization, SentencePiece metaspace (`▁`) encoding, Viterbi Unigram
 * segmentation, and E5 `query: ` / `passage: ` prefixing.
 */
class E5UnigramTokenizer private constructor(
    private val vocabScores: Map<String, Pair<Int, Double>>,
    private val maxPieceLength: Int,
    private val bosId: Long,
    private val eosId: Long,
    private val unkId: Long
) {
    fun encodeQuery(text: String, maxLength: Int = DEFAULT_MAX_LENGTH): E5TokenizedInput =
        encode("query: ${text.trim()}", maxLength)

    fun encodePassage(text: String, maxLength: Int = DEFAULT_MAX_LENGTH): E5TokenizedInput =
        encode("passage: ${text.trim()}", maxLength)

    fun encode(text: String, maxLength: Int = DEFAULT_MAX_LENGTH): E5TokenizedInput {
        require(maxLength in 4..512) { "maxLength must be between 4 and 512" }
        val normalized = normalizeForMetaspace(text)
        val pieceIds = if (normalized.isEmpty()) {
            emptyList()
        } else {
            viterbiSegment(normalized)
        }
        val maxContentTokens = maxLength - 2
        val truncated = if (pieceIds.size > maxContentTokens) pieceIds.subList(0, maxContentTokens) else pieceIds
        val ids = LongArray(truncated.size + 2)
        ids[0] = bosId
        for (i in truncated.indices) {
            ids[i + 1] = truncated[i].toLong()
        }
        ids[ids.lastIndex] = eosId
        val mask = LongArray(ids.size) { 1L }
        return E5TokenizedInput(ids, mask)
    }

    private fun normalizeForMetaspace(text: String): String {
        val nfkc = Normalizer.normalize(text.trim(), Normalizer.Form.NFKC)
        if (nfkc.isEmpty()) return ""
        val collapsed = nfkc.replace(WHITESPACE, " ")
        return METASPACE + collapsed.replace(' ', METASPACE)
    }

    private fun viterbiSegment(text: String): List<Int> {
        val n = text.length
        val bestScore = DoubleArray(n + 1) { Double.NEGATIVE_INFINITY }
        val bestLen = IntArray(n + 1) { 1 }
        val bestId = IntArray(n + 1) { unkId.toInt() }
        bestScore[0] = 0.0

        for (i in 0 until n) {
            val base = bestScore[i]
            if (base == Double.NEGATIVE_INFINITY) continue
            val limit = min(n, i + maxPieceLength)
            var matched = false
            for (j in (i + 1)..limit) {
                val sub = text.substring(i, j)
                val entry = vocabScores[sub]
                if (entry != null) {
                    matched = true
                    val candidate = base + entry.second
                    if (candidate > bestScore[j]) {
                        bestScore[j] = candidate
                        bestLen[j] = j - i
                        bestId[j] = entry.first
                    }
                }
            }
            if (!matched) {
                // Unknown single-character fallback (advance by surrogate pair if needed).
                val charLen = Character.charCount(text.codePointAt(i))
                val j = min(n, i + charLen)
                val candidate = base + UNK_PENALTY
                if (candidate > bestScore[j]) {
                    bestScore[j] = candidate
                    bestLen[j] = j - i
                    bestId[j] = unkId.toInt()
                }
            }
        }

        val reversed = ArrayList<Int>(n / 2 + 1)
        var pos = n
        while (pos > 0) {
            val len = bestLen[pos].coerceAtLeast(1)
            reversed.add(bestId[pos])
            pos -= len
        }
        reversed.reverse()
        return reversed
    }

    companion object {
        private const val METASPACE = '\u2581'
        private const val DEFAULT_MAX_LENGTH = 512
        private const val UNK_PENALTY = -100.0
        private val WHITESPACE = Regex("""\s+""")

        fun fromFile(tokenizerJsonFile: File): E5UnigramTokenizer {
            require(tokenizerJsonFile.isFile) { "Tokenizer file not found: ${tokenizerJsonFile.path}" }
            return fromJson(tokenizerJsonFile.readText(Charsets.UTF_8))
        }

        @Suppress("UNCHECKED_CAST")
        fun fromJson(rawJson: String): E5UnigramTokenizer {
            val root = MinimalJsonParser(rawJson).parse() as? Map<String, Any?>
                ?: throw IllegalArgumentException("tokenizer.json root must be a JSON object")
            val model = root["model"] as? Map<String, Any?>
                ?: throw IllegalArgumentException("tokenizer.json is missing 'model'")
            val vocabRaw = model["vocab"] as? List<Any?>
                ?: throw IllegalArgumentException("tokenizer.json model is missing 'vocab'")

            val vocab = HashMap<String, Pair<Int, Double>>(vocabRaw.size * 2)
            var maxLen = 1
            for ((index, item) in vocabRaw.withIndex()) {
                val pair = item as? List<Any?> ?: continue
                if (pair.size < 2) continue
                val piece = pair[0] as? String ?: continue
                val score = (pair[1] as? Number)?.toDouble() ?: 0.0
                if (piece.isNotEmpty()) {
                    vocab[piece] = index to score
                    if (piece.length > maxLen) maxLen = piece.length
                }
            }
            require(vocab.isNotEmpty()) { "Tokenizer vocabulary cannot be empty" }

            var bosId = 0L
            var eosId = 2L
            var unkId = (model["unk_id"] as? Number)?.toLong() ?: 3L
            val added = root["added_tokens"] as? List<Any?> ?: emptyList()
            for (entry in added) {
                val obj = entry as? Map<String, Any?> ?: continue
                val content = obj["content"] as? String ?: continue
                val id = (obj["id"] as? Number)?.toLong() ?: continue
                when (content) {
                    "<s>" -> bosId = id
                    "</s>" -> eosId = id
                    "<unk>" -> unkId = id
                }
            }
            return E5UnigramTokenizer(vocab, maxLen, bosId, eosId, unkId)
        }
    }
}

private class MinimalJsonParser(private val src: String) {
    private var idx = 0

    fun parse(): Any? {
        skipWs()
        val value = parseValue()
        skipWs()
        return value
    }

    private fun parseValue(): Any? {
        skipWs()
        if (idx >= src.length) throw IllegalArgumentException("Unexpected end of JSON")
        return when (val ch = src[idx]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected JSON char '$ch' at $idx")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        idx++ // '{'
        val map = LinkedHashMap<String, Any?>()
        skipWs()
        if (idx < src.length && src[idx] == '}') {
            idx++
            return map
        }
        while (true) {
            skipWs()
            val key = parseString()
            skipWs()
            if (idx >= src.length || src[idx] != ':') throw IllegalArgumentException("Expected ':' at $idx")
            idx++
            val value = parseValue()
            map[key] = value
            skipWs()
            if (idx >= src.length) throw IllegalArgumentException("Unterminated JSON object")
            when (src[idx]) {
                ',' -> idx++
                '}' -> {
                    idx++
                    return map
                }
                else -> throw IllegalArgumentException("Expected ',' or '}' at $idx")
            }
        }
    }

    private fun parseArray(): List<Any?> {
        idx++ // '['
        val list = ArrayList<Any?>()
        skipWs()
        if (idx < src.length && src[idx] == ']') {
            idx++
            return list
        }
        while (true) {
            list.add(parseValue())
            skipWs()
            if (idx >= src.length) throw IllegalArgumentException("Unterminated JSON array")
            when (src[idx]) {
                ',' -> idx++
                ']' -> {
                    idx++
                    return list
                }
                else -> throw IllegalArgumentException("Expected ',' or ']' at $idx")
            }
        }
    }

    private fun parseString(): String {
        if (src[idx] != '"') throw IllegalArgumentException("Expected '\"' at $idx")
        idx++
        val sb = StringBuilder()
        while (idx < src.length) {
            val ch = src[idx++]
            when (ch) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (idx >= src.length) throw IllegalArgumentException("Unterminated escape")
                    when (val esc = src[idx++]) {
                        '"', '\\', '/' -> sb.append(esc)
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (idx + 4 > src.length) throw IllegalArgumentException("Invalid unicode escape")
                            sb.append(src.substring(idx, idx + 4).toInt(16).toChar())
                            idx += 4
                        }
                        else -> throw IllegalArgumentException("Invalid escape '\\$esc'")
                    }
                }
                else -> sb.append(ch)
            }
        }
        throw IllegalArgumentException("Unterminated string")
    }

    private fun parseNumber(): Number {
        val start = idx
        if (src[idx] == '-') idx++
        while (idx < src.length && src[idx] in '0'..'9') idx++
        var isFloat = false
        if (idx < src.length && src[idx] == '.') {
            isFloat = true
            idx++
            while (idx < src.length && src[idx] in '0'..'9') idx++
        }
        if (idx < src.length && (src[idx] == 'e' || src[idx] == 'E')) {
            isFloat = true
            idx++
            if (idx < src.length && (src[idx] == '+' || src[idx] == '-')) idx++
            while (idx < src.length && src[idx] in '0'..'9') idx++
        }
        val raw = src.substring(start, idx)
        return if (isFloat) raw.toDouble() else raw.toLong()
    }

    private fun parseBoolean(): Boolean {
        return if (src.startsWith("true", idx)) {
            idx += 4
            true
        } else if (src.startsWith("false", idx)) {
            idx += 5
            false
        } else {
            throw IllegalArgumentException("Invalid boolean at $idx")
        }
    }

    private fun parseNull(): Any? {
        if (!src.startsWith("null", idx)) throw IllegalArgumentException("Invalid null at $idx")
        idx += 4
        return null
    }

    private fun skipWs() {
        while (idx < src.length && src[idx] in charArrayOf(' ', '\t', '\r', '\n')) idx++
    }
}
