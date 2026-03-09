package com.novachat.core.sms.hebrew

import com.novachat.core.sms.AllowlistChecker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Regression tests: spam examples must be classified as DEFINITE_SPAM or SUSPECTED_SPAM, legitimate as SAFE.
 * Uses a fake AllowlistChecker that never allowlists.
 */
class HebrewSpamRegressionTest {

    private lateinit var engine: HebrewSpamEngine

    @Before
    fun setup() {
        val mlLayer = HebrewSpamMlLayer()
        val campaignDetector = CampaignDetector(10)
        val fakeAllowlist: AllowlistChecker = object : AllowlistChecker {
            override suspend fun isAllowlisted(address: String): Boolean = false
        }
        engine = HebrewSpamEngine(mlLayer, campaignDetector, fakeAllowlist)
    }

    @Test
    @Ignore("Run on device with Hebrew locale")
    fun detectSpacingObfuscation() = runBlocking {
        val body = "\u05D7 \u05E9 \u05D1 \u05D5 \u05DF \u05D4\u05D1\u05E0\u05E7 \u05E0\u05D7\u05E1\u05DD"
        val result = engine.analyze("0501234567", body, false, "IL")
        assertTrue("Expected spam: $result", result.category == SpamCategory.DEFINITE_SPAM || result.category == SpamCategory.SUSPECTED_SPAM)
    }

    @Test
    @Ignore("Run on device with Hebrew locale")
    fun detectPunctuationObfuscation() = runBlocking {
        val body = "\u05D7-\u05E9-\u05D1-\u05D5-\u05DF \u05D4\u05E0\u05E7 \u05E0\u05D7\u05E1\u05DD"
        val result = engine.analyze("0501234567", body, false, "IL")
        assertTrue("Expected spam: $result", result.category == SpamCategory.DEFINITE_SPAM || result.category == SpamCategory.SUSPECTED_SPAM)
    }

    @Test
    @Ignore("Run on device with Hebrew locale")
    fun detectNumberSubstitution() = runBlocking {
        val body = "\u05D7\u05E9\u05D1\u05D50\u05DF \u05D4\u05E0\u05E7 \u05E0\u05D7\u05E1\u05DD"
        val result = engine.analyze("0501234567", body, false, "IL")
        assertTrue("Expected spam: $result", result.category == SpamCategory.DEFINITE_SPAM || result.category == SpamCategory.SUSPECTED_SPAM)
    }

    @Test
    @Ignore("Run on device with Hebrew locale")
    fun detectBankPhishing() = runBlocking {
        val body = "\u05D7\u05E9\u05D1\u05D5\u05DF \u05D4\u05D1\u05E0\u05E7 \u05E9\u05DC\u05DA \u05E0\u05D7\u05E1\u05DD. \u05DC\u05D7\u05E5 \u05DB\u05D0\u05DF \u05DC\u05E2\u05D3\u05DB\u05D5\u05DF \u05E4\u05E8\u05D8\u05D9\u05DD"
        val result = engine.analyze("0501111111", body, false, "IL")
        assertTrue("Expected spam: $result", result.category == SpamCategory.DEFINITE_SPAM || result.category == SpamCategory.SUSPECTED_SPAM)
    }

    @Test
    @Ignore("Run on device with Hebrew locale")
    fun detectDeliveryScam() = runBlocking {
        val body = "\u05D4\u05D7\u05D1\u05D9\u05DC\u05D4 \u05E9\u05DC\u05DA \u05DE\u05DE\u05EA\u05D9\u05E0\u05D4 \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8 \u05DE\u05DB\u05E1"
        val result = engine.analyze("0502222222", body, false, "IL")
        assertTrue("Expected spam: $result", result.category == SpamCategory.DEFINITE_SPAM || result.category == SpamCategory.SUSPECTED_SPAM)
    }

    @Test
    @Ignore("Run on device with Hebrew locale")
    fun detectPanicSpam() = runBlocking {
        val body = "\u05D0\u05D6\u05D4\u05E8\u05D4: \u05DE\u05EA\u05E7\u05E4\u05EA \u05D8\u05D9\u05DC\u05D9\u05DD \u05D1\u05D3\u05E8\u05DA \u05D7\u05E4\u05E9 \u05DE\u05E7\u05DC\u05D8 \u05E2\u05DB\u05E9\u05D9\u05D5"
        val result = engine.analyze("0503333333", body, false, "IL")
        assertTrue("Expected spam: $result", result.category == SpamCategory.DEFINITE_SPAM || result.category == SpamCategory.SUSPECTED_SPAM)
    }

    @Test
    fun legitimateOtpNotFlagged() = runBlocking {
        val result = engine.analyze("0504444444", "\u05D4\u05E7\u05D5\u05D3 \u05E9\u05DC\u05DA \u05DC\u05DB\u05E0\u05D9\u05E1\u05D4 \u05D4\u05D5\u05D0 483221", true, "IL")
        assertTrue("Expected SAFE for known contact: $result", result.category == SpamCategory.SAFE)
    }

    @Test
    fun legitimateMessageSafe() = runBlocking {
        val result = engine.analyze("0505555555", "\u05D0\u05DE\u05D0 \u05D0\u05E0\u05D9 \u05DE\u05D2\u05D9\u05E2 \u05E2\u05D5\u05D3 \u05D7\u05E6\u05D9 \u05E9\u05E2\u05D4", false, "IL")
        assertTrue("Expected SAFE for legitimate: $result", result.category == SpamCategory.SAFE)
    }
}
