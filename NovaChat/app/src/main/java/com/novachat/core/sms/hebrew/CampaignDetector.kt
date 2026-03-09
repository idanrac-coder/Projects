package com.novachat.core.sms.hebrew

import kotlin.math.sqrt

/**
 * Detects spam campaigns: if new message is very similar to recent messages (cosine > 0.9),
 * add campaign bonus to score. Bounded buffer; no network.
 */
class CampaignDetector(private val bufferSize: Int = 50) {

    private val buffer = ArrayDeque<String>(bufferSize)

    fun addMessage(normalizedBody: String) {
        if (normalizedBody.length < 10) return
        synchronized(buffer) {
            while (buffer.size >= bufferSize) buffer.removeFirst()
            buffer.addLast(normalizedBody)
        }
    }

    fun getCampaignBonus(normalizedBody: String): Float {
        if (normalizedBody.length < 10) return 0f
        val vec = toCountVector(normalizedBody)
        val buffered = synchronized(buffer) { buffer.toList() }
        for (other in buffered) {
            val otherVec = toCountVector(other)
            if (cosineSimilarity(vec, otherVec) > 0.9f) return 0.15f
        }
        return 0f
    }

    private fun toCountVector(text: String): Map<String, Int> {
        val tokens = HebrewTextNormalizer.tokenize(text)
        val ngrams = (3..5).flatMap { n ->
            if (text.length >= n) (0..text.length - n).map { text.substring(it, it + n) } else emptyList()
        }
        return (tokens + ngrams).groupingBy { it }.eachCount()
    }

    private fun cosineSimilarity(a: Map<String, Int>, b: Map<String, Int>): Float {
        val keys = (a.keys + b.keys).toSet()
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (k in keys) {
            val va = (a[k] ?: 0).toDouble()
            val vb = (b[k] ?: 0).toDouble()
            dot += va * vb
            na += va * va
            nb += vb * vb
        }
        return if (na * nb == 0.0) 0f else (dot / (sqrt(na) * sqrt(nb))).toFloat()
    }
}
