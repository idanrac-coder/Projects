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
        Pattern.compile(
            "\\b(?:(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|" +
            "Mon|Tue|Wed|Thu|Fri|Sat|Sun)[,.]?\\s+)?" +
            "(?:January|February|March|April|May|June|July|August|September|October|November|December|" +
            "Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.?\\s+\\d{1,2}(?:st|nd|rd|th)?" +
            "(?:[,.]?\\s+\\d{4})?" +
            "(?:\\s+(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)?\\b",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}" +
            "(?:\\s+(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)?\\b",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "\\b(?:today|tomorrow|tonight|yesterday)" +
            "(?:\\s+at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)?\\b",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "\\b(?:next|this)\\s+(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|" +
            "Mon|Tue|Wed|Thu|Fri|Sat|Sun|week|month)" +
            "(?:\\s+at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?)?\\b",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "\\b(?:at|by|from|until|before|after)\\s+\\d{1,2}(?::\\d{2})\\s*(?:AM|PM|am|pm)\\b",
            Pattern.CASE_INSENSITIVE
        ),
        // Hebrew date patterns
        Pattern.compile(
            "\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}\\b"
        ),
        Pattern.compile(
            "(?:יום\\s+)?(?:ראשון|שני|שלישי|רביעי|חמישי|שישי|שבת)" +
            "(?:\\s+(?:הקרוב|הבא))?" +
            "(?:\\s+(?:ב-?)?\\s*\\d{1,2}(?::\\d{2})?)?",
            Pattern.CASE_INSENSITIVE
        ),
        Pattern.compile(
            "(?:מחר|היום|הערב|אתמול)" +
            "(?:\\s+(?:ב-?)?\\s*(?:שעה\\s+)?\\d{1,2}(?::\\d{2})?)?",
            Pattern.CASE_INSENSITIVE
        )
    )

    private val ADDRESS_PATTERNS = listOf(
        Pattern.compile(
            "\\b\\d{1,5}\\s+(?:[A-Z][a-zA-Z'-]+\\s+){1,4}" +
            "(?:Street|St|Avenue|Ave|Boulevard|Blvd|Road|Rd|Drive|Dr|Lane|Ln|" +
            "Court|Ct|Place|Pl|Way|Circle|Cir|Highway|Hwy)\\.?" +
            "(?:[,.]?\\s+(?:Apt|Suite|Ste|Unit|#)\\s*\\d+[A-Za-z]?)?" +
            "(?:[,.]?\\s+[A-Z][a-zA-Z]+(?:\\s+[A-Z][a-zA-Z]+)*)?" +
            "\\b",
            Pattern.CASE_INSENSITIVE
        ),
        // Hebrew street addresses: רחוב/רח' X 12, or שד' X 12
        Pattern.compile(
            "(?:רחוב|רח['\u2019]|שדרות|שד['\u2019])\\s+[\\u0590-\\u05FF]+(?:\\s+[\\u0590-\\u05FF]+)*\\s+\\d{1,4}",
            Pattern.CASE_INSENSITIVE
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
