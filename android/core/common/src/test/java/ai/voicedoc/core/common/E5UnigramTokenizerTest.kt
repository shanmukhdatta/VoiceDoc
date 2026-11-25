package ai.voicedoc.core.common

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E5UnigramTokenizerTest {
    private val sampleTokenizerJson = """
        {
          "added_tokens": [
            {"id": 0, "content": "<s>"},
            {"id": 1, "content": "<pad>"},
            {"id": 2, "content": "</s>"},
            {"id": 3, "content": "<unk>"}
          ],
          "model": {
            "type": "Unigram",
            "unk_id": 3,
            "vocab": [
              ["<s>", 0.0],
              ["<pad>", 0.0],
              ["</s>", 0.0],
              ["<unk>", 0.0],
              ["▁query", -1.5],
              [":", -2.0],
              ["▁passage", -1.5],
              ["▁fever", -2.2],
              ["▁ఆరోగ్య", -2.5],
              ["▁సమాచారం", -2.5]
            ]
          }
        }
    """.trimIndent()

    @Test
    fun encodesEnglishQueryWithBosEosAndPrefix() {
        val tokenizer = E5UnigramTokenizer.fromJson(sampleTokenizerJson)
        val encoded = tokenizer.encodeQuery("fever")
        assertArrayEquals(longArrayOf(0L, 4L, 5L, 7L, 2L), encoded.inputIds)
        assertArrayEquals(longArrayOf(1L, 1L, 1L, 1L, 1L), encoded.attentionMask)
    }

    @Test
    fun encodesTeluguPassageWithPrefix() {
        val tokenizer = E5UnigramTokenizer.fromJson(sampleTokenizerJson)
        val encoded = tokenizer.encodePassage("ఆరోగ్య సమాచారం")
        assertArrayEquals(longArrayOf(0L, 6L, 5L, 8L, 9L, 2L), encoded.inputIds)
        assertEquals(encoded.inputIds.size, encoded.attentionMask.size)
    }

    @Test
    fun truncatesToMaxLengthPreservingEos() {
        val tokenizer = E5UnigramTokenizer.fromJson(sampleTokenizerJson)
        val encoded = tokenizer.encodeQuery("fever", maxLength = 4)
        assertEquals(4, encoded.inputIds.size)
        assertEquals(0L, encoded.inputIds.first())
        assertEquals(2L, encoded.inputIds.last())
        assertTrue(encoded.attentionMask.all { it == 1L })
    }
}
