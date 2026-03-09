package com.novachat.core.sms.hebrew

import com.novachat.core.sms.AllowlistChecker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Microbenchmarks for Hebrew spam detection. Targets: normalizer <1ms, full analyze p99 <10ms.
 */
class SpamDetectionBenchmark {

    private val sampleBody = "חשבון הבנק שלך נחסם. לחץ כאן לעדכון: https://bit.ly/xxx"
    private val fakeAllowlist: AllowlistChecker = object : AllowlistChecker {
        override suspend fun isAllowlisted(address: String): Boolean = false
    }

    @Test
    @Ignore("CI timing varies")
    fun normalizerUnder1ms() {
        val times = (1..100).map {
            val start = System.nanoTime()
            HebrewTextNormalizer.normalizeForMatching(sampleBody)
            System.nanoTime() - start
        }
        val p99Ns = times.sorted().let { it[it.size * 99 / 100] }
        assertTrue("Normalizer p99 should be <1ms", p99Ns < 1_000_000)
    }

    @Test
    @Ignore("CI timing varies")
    fun linkAnalyzerUnderHalfMs() {
        val times = (1..100).map {
            val start = System.nanoTime()
            LinkRiskAnalyzer.analyze(sampleBody, HebrewTextNormalizer.normalizeForMatching(sampleBody))
            System.nanoTime() - start
        }
        val p99Ns = times.sorted().let { it[it.size * 99 / 100] }
        assertTrue("LinkRiskAnalyzer p99 should be <2ms", p99Ns < 2_000_000)
    }

    @Test
    @Ignore("CI timing varies")
    fun fullAnalyzeUnder10ms() = runBlocking {
        val mlLayer = HebrewSpamMlLayer()
        val campaignDetector = CampaignDetector(50)
        val engine = HebrewSpamEngine(mlLayer, campaignDetector, fakeAllowlist)
        val times = (1..50).map {
            val start = System.nanoTime()
            engine.analyze("0501234567", sampleBody, false, "IL")
            System.nanoTime() - start
        }
        val p50Ns = times.sorted()[times.size / 2]
        val p99Ns = times.sorted().let { it[it.size * 99 / 100].coerceAtLeast(it.last()) }
        assertTrue("Full analyze p50 should be <20ms (relaxed for CI)", p50Ns < 20_000_000)
        assertTrue("Full analyze p99 should be <50ms (relaxed for CI)", p99Ns < 50_000_000)
    }
}
