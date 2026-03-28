package com.novachat.core.sms.ml

import com.novachat.core.sms.hebrew.HebrewTextNormalizer

/**
 * Prepares raw SMS text for the TFLite spam classifier.
 * Pipeline: normalize -> lowercase -> tokenize -> map to vocab indices -> pad/truncate to fixed length.
 * Supports Hebrew + English multilingual input.
 */
object SpamTextPreprocessor {

    const val MAX_SEQUENCE_LENGTH = 128
    const val VOCAB_SIZE = 8000
    private const val PAD_TOKEN = 0
    private const val UNK_TOKEN = 1

    @Volatile
    private var vocabMap: Map<String, Int> = emptyMap()

    fun loadVocabulary(vocab: Map<String, Int>) {
        vocabMap = vocab
    }

    fun isVocabLoaded(): Boolean = vocabMap.isNotEmpty()

    fun preprocess(body: String): FloatArray {
        val normalized = normalize(body)
        val tokens = tokenize(normalized)
        val indices = tokensToIndices(tokens)
        return padOrTruncate(indices)
    }

    fun normalize(text: String): String {
        var t = text.take(2000)
        val hasHebrew = t.any { it in '\u0590'..'\u05FF' }
        if (hasHebrew) {
            t = HebrewTextNormalizer.normalizeForMatching(t)
        }
        t = t.lowercase()
        t = t.replace(Regex("https?://\\S+"), " <URL> ")
        t = t.replace(Regex("\\b\\d{4,}\\b"), " <NUM> ")
        t = t.replace(Regex("[^\\p{L}\\p{N}<>\\s]"), " ")
        t = t.replace(Regex("\\s+"), " ").trim()
        return t
    }

    fun tokenize(text: String): List<String> {
        return text.split(" ").filter { it.isNotEmpty() }
    }

    private fun tokensToIndices(tokens: List<String>): List<Int> {
        if (vocabMap.isEmpty()) {
            return tokens.map { simpleHash(it) }
        }
        return tokens.map { vocabMap[it] ?: UNK_TOKEN }
    }

    /**
     * When no vocab is loaded, use a deterministic hash to bucket index.
     * This gives consistent (though not optimal) results until a real vocab is shipped.
     */
    private fun simpleHash(token: String): Int {
        val h = token.hashCode().and(0x7FFFFFFF)
        return (h % (VOCAB_SIZE - 2)) + 2
    }

    private fun padOrTruncate(indices: List<Int>): FloatArray {
        val result = FloatArray(MAX_SEQUENCE_LENGTH) { PAD_TOKEN.toFloat() }
        val len = minOf(indices.size, MAX_SEQUENCE_LENGTH)
        for (i in 0 until len) {
            result[i] = indices[i].toFloat()
        }
        return result
    }
}
