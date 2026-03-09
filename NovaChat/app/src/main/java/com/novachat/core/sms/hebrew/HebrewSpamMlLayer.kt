package com.novachat.core.sms.hebrew

import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Lightweight ML layer: word TF-IDF + character n-grams. Model in assets/hebrew_spam_model.json.
 * If no model loaded, returns 0f (fallback to heuristic-only). Model size <5MB.
 */
class HebrewSpamMlLayer {

    private var intercept: Float = 0f
    private var wordCoefficients: Map<String, Float> = emptyMap()
    private var charNgramCoefficients: Map<String, Float> = emptyMap()
    private var idf: Map<String, Float> = emptyMap()
    private var initialized: Boolean = false

    fun setModel(intercept: Float, wordCoef: Map<String, Float>, charNgramCoef: Map<String, Float>, idf: Map<String, Float>) {
        this.intercept = intercept
        this.wordCoefficients = wordCoef
        this.charNgramCoefficients = charNgramCoef
        this.idf = idf
        this.initialized = true
    }

    fun score(normalizedText: String): Float {
        if (!initialized || (wordCoefficients.isEmpty() && charNgramCoefficients.isEmpty())) return 0f
        val tokens = HebrewTextNormalizer.tokenize(normalizedText)
        val wordFeat = tokens.associateWith { 1f }.mapValues { (k, v) ->
            v * (idf[k] ?: 1f)
        }
        val ngrams = extractCharNgrams(normalizedText, 3) + extractCharNgrams(normalizedText, 4) + extractCharNgrams(normalizedText, 5)
        val ngramFeat = ngrams.groupingBy { it }.eachCount().mapValues { (k, cnt) ->
            cnt.toFloat() * (idf[k] ?: 1f)
        }
        var dot = intercept
        for ((k, v) in wordFeat) {
            dot += (wordCoefficients[k] ?: 0f) * v
        }
        for ((k, v) in ngramFeat) {
            dot += (charNgramCoefficients[k] ?: 0f) * v
        }
        return sigmoid(dot)
    }

    private fun extractCharNgrams(text: String, n: Int): List<String> {
        if (text.length < n) return emptyList()
        return (0..text.length - n).map { text.substring(it, it + n) }
    }

    private fun sigmoid(x: Float): Float {
        return (1f / (1f + exp(-x.coerceIn(-20f, 20f)))).toFloat()
    }
}
