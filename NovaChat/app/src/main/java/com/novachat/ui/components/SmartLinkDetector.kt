package com.novachat.ui.components

import java.util.regex.Pattern

data class SmartLink(
    val start: Int,
    val end: Int,
    val type: SmartLinkType,
    val displayText: String,
    val actionData: String
)

enum class SmartLinkType {
    DATE_TIME,
    ADDRESS
}

object SmartLinkDetector {

    private val DATE_PATTERNS = listOf(
        // "January 15, 2026", "Dec 3rd", "March 12 at 3:00 PM"
        Pattern.compile(
            "\\b(?:January|February|March|April|May|June|July|August|September|October|November|December|" +
            "Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.?\\s+\\d{1,2}(?:st|nd|rd|th)?" +
            "(?:[,.]?\\s+\\d{4})?" +
            "(?:\\s+(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)?\\b",
            Pattern.CASE_INSENSITIVE
        ),
        // "12/25/2024", "3-4-26", "15.03.2026"
        Pattern.compile(
            "\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}" +
            "(?:\\s+(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)?\\b",
            Pattern.CASE_INSENSITIVE
        ),
        // "15 בינואר 2026", "3 במרץ", Hebrew month names with day number
        Pattern.compile(
            "\\b\\d{1,2}\\s+(?:ב)?(?:ינואר|פברואר|מרץ|מרס|אפריל|מאי|יוני|יולי|אוגוסט|ספטמבר|אוקטובר|נובמבר|דצמבר)" +
            "(?:\\s+\\d{4})?\\b"
        )
    )

    private val ADDRESS_PATTERNS = listOf(
        // English: "123 Main Street", "45 Oak Ave, Apt 3B"
        Pattern.compile(
            "\\b\\d{1,5}\\s+(?:[A-Z][a-zA-Z'-]+\\s+){1,4}" +
            "(?:Street|St|Avenue|Ave|Boulevard|Blvd|Road|Rd|Drive|Dr|Lane|Ln|" +
            "Court|Ct|Place|Pl|Way|Circle|Cir|Highway|Hwy)\\.?" +
            "(?:[,.]?\\s+(?:Apt|Suite|Ste|Unit|#)\\s*\\d+[A-Za-z]?)?" +
            "(?:[,.]?\\s+[A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)?" +
            "\\b",
            Pattern.CASE_INSENSITIVE
        ),
        // Hebrew with prefix: "רחוב הרצל 5", "רח' ביאליק 12", "שדרות בן גוריון 30"
        Pattern.compile(
            "(?:רחוב|רח['\u2019]|שדרות|שד['\u2019]|סמטת|סמ['\u2019]|כיכר|דרך)" +
            "\\s+[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,2}" +
            "\\s+\\d{1,4}" +
            "(?:\\s*[/\\-]\\s*\\d{1,3})?"
        ),
        // Hebrew with ב prefix: "ברחוב הרצל 5", "בשדרות בן גוריון 23"
        Pattern.compile(
            "ב(?:רחוב|רח['\u2019]|שדרות|שד['\u2019]|סמטת|סמ['\u2019]|דרך)" +
            "\\s+[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,2}" +
            "\\s+\\d{1,4}" +
            "(?:\\s*[/\\-]\\s*\\d{1,3})?"
        ),
        // Hebrew street name + apartment number (no prefix): "העצמאות 14/2", "בן גוריון 23/1"
        Pattern.compile(
            "[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,1}" +
            "\\s+\\d{1,4}\\s*[/\\-]\\s*\\d{1,3}"
        ),
        // Hebrew with city suffix + prefix: "רחוב בן גוריון 12, חיפה", "שדרות הראל 2, ראשון לציון"
        Pattern.compile(
            "(?:רחוב|רח['\u2019]|שדרות|שד['\u2019]|סמטת|דרך|כיכר)" +
            "\\s+[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,2}" +
            "\\s+\\d{1,4}" +
            "(?:\\s*[/\\-]\\s*\\d{1,3})?" +
            "\\s*,\\s*[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,2}"
        ),
        // Hebrew with city suffix, no prefix: "מבצע הראל 2, ראשון לציון", "הרצל 5, תל אביב"
        Pattern.compile(
            "[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,1}" +
            "\\s+\\d{1,4}" +
            "(?:\\s*[/\\-]\\s*\\d{1,3})?" +
            "\\s*,\\s*[\\u0590-\\u05FF]+(?:[\\s\\-][\\u0590-\\u05FF]+){0,2}"
        )
    )

    fun detect(text: String, excludeRanges: List<IntRange> = emptyList()): List<SmartLink> {
        val results = mutableListOf<SmartLink>()

        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (excludeRanges.any { start < it.last + 1 && end > it.first }) continue
                if (results.any { start < it.end && end > it.start }) continue

                val matched = matcher.group() ?: continue
                if (matched.isBlank() || matched.length < 3) continue

                results.add(
                    SmartLink(
                        start = start,
                        end = end,
                        type = SmartLinkType.DATE_TIME,
                        displayText = matched,
                        actionData = matched
                    )
                )
            }
        }

        for (pattern in ADDRESS_PATTERNS) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (excludeRanges.any { start < it.last + 1 && end > it.first }) continue
                if (results.any { start < it.end && end > it.start }) continue

                val matched = matcher.group() ?: continue
                if (matched.isBlank() || matched.length < 5) continue

                results.add(
                    SmartLink(
                        start = start,
                        end = end,
                        type = SmartLinkType.ADDRESS,
                        displayText = matched,
                        actionData = matched
                    )
                )
            }
        }

        return results.sortedBy { it.start }
    }
}
