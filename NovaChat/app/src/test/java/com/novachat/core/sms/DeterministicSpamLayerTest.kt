package com.novachat.core.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicSpamLayerTest {

    @Test
    fun taxRefundScamIsDetected() {
        val body = "\u05D4\u05D7\u05D6\u05E8\u05D9 \u05DE\u05E1 \u05DC\u05DB\u05D5\u05DC\u05DD \u05D4\u05D9\u05D5\u05DD \u05D1\u05E9\u05E2\u05D4 5"
        val result = DeterministicSpamLayer.analyze(body)
        assertTrue("Tax refund pattern should match", result.matched)
        assertEquals("TAX_REFUND_SCAM", result.ruleType)
    }
}
