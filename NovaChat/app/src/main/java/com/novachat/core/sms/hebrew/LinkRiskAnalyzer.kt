package com.novachat.core.sms.hebrew

import java.net.IDN
import java.net.URL

/**
 * Analyzes URLs in message body: shorteners, punycode, suspicious TLDs, brand impersonation.
 * No network; uses java.net.URL and IDN only.
 */
object LinkRiskAnalyzer {

    private val URL_REGEX = Regex("https?://[^\\s]+")

    private val SHORTENER_DOMAINS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "rb.gy", "cutt.ly", "is.gd", "v.gd",
        "qr.ae", "clck.ru", "adf.ly", "ouo.io", "bc.vc", "tr.im", "ow.ly", "buff.ly", "j.mp",
        "tiny.cc", "short.link", "rebrand.ly", "lnk.bio", "bit-ly.com"
    )

    private val SUSPICIOUS_TLDS = setOf(
        "tk", "ml", "ga", "cf", "gq", "xyz", "top", "buzz", "club", "icu", "work", "info",
        "rest", "click", "link", "online", "site", "fun", "ru", "cc", "ws", "live", "stream"
    )

    private val ISRAELI_BRAND_DOMAINS = setOf(
        "leumi.co.il", "bankleumi.co.il", "bankhapoalim.co.il", "hapoalim.co.il",
        "discountbank.co.il", "mizrahi.co.il", "gov.il", "tax.gov.il",
        "israelpost.co.il", "dhl.co.il", "ups.com", "012.co.il", "bezeq.co.il"
    )

    private val BODY_BRAND_KEYWORDS = mapOf(
        "בנק לאומי" to "leumi", "לאומי" to "leumi",
        "הפועלים" to "hapoalim", "דיסקונט" to "discount", "מזרחי" to "mizrahi",
        "דואר ישראל" to "israelpost", "Israel Post" to "israelpost",
        "DHL" to "dhl", "רשות המיסים" to "tax", "פיקוד העורף" to "gov"
    )

    data class LinkRiskResult(
        val risks: List<String>,
        val score: Float
    )

    fun analyze(body: String, normalizedBody: String): LinkRiskResult {
        val risks = mutableListOf<String>()
        val urls = URL_REGEX.findAll(body).map { it.value }.toList()
        for (urlStr in urls) {
            try {
                val url = URL(urlStr)
                val host = url.host ?: continue
                val domain = host.lowercase().removePrefix("www.")

                if (SHORTENER_DOMAINS.any { domain.contains(it) }) {
                    risks.add("SHORTENER:$domain")
                }
                if (domain.contains("xn--")) {
                    val decoded = try { IDN.toUnicode(host) } catch (_: Exception) { host }
                    if (decoded.any { it.code in 0x400..0x4FF || it.code in 0x370..0x3FF }) {
                        risks.add("PUNYCODE_SUSPICIOUS")
                    }
                }
                val tld = domain.substringAfterLast('.', "")
                if (tld in SUSPICIOUS_TLDS) {
                    risks.add("SUSPICIOUS_TLD:$tld")
                }
                val bodyHasBrand = BODY_BRAND_KEYWORDS.any { (keyword, brand) ->
                    keyword in normalizedBody || brand in body.lowercase()
                }
                val domainIsOfficial = ISRAELI_BRAND_DOMAINS.any { domain == it || domain.endsWith(".$it") }
                if (bodyHasBrand && !domainIsOfficial) {
                    risks.add("BRAND_MISMATCH")
                }
            } catch (_: Exception) { }
        }
        val score = when {
            risks.any { it.startsWith("BRAND_MISMATCH") } -> 0.85f
            risks.any { it.startsWith("SHORTENER") } -> 0.65f
            risks.any { it == "PUNYCODE_SUSPICIOUS" } -> 0.7f
            risks.any { it.startsWith("SUSPICIOUS_TLD") } -> 0.5f
            else -> 0f
        }
        return LinkRiskResult(risks.distinct(), score)
    }

    fun extractUrls(body: String): List<String> =
        URL_REGEX.findAll(body).map { it.value }.toList()
}
