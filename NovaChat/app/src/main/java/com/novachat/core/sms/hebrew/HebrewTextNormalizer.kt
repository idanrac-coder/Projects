package com.novachat.core.sms.hebrew

/**
 * Production-grade Hebrew text normalizer for spam detection.
 * Handles obfuscation: bidi/zero-width, homoglyphs, full-width, emoji,
 * niqqud, final letters, spacing/symbols between letters, number-letter substitution.
 * Pipeline: body -> normalizeForMatching() -> clean text for keyword/regex.
 */
object HebrewTextNormalizer {

    private const val MAX_INPUT_LENGTH = 2000

    private val ZERO_WIDTH_AND_JOINERS = Regex("[\u200B-\u200D\u2060\uFEFF]+")
    private val BIDI_CONTROL = Regex("[\u200E\u200F\u202A-\u202E\u2066-\u2069]+")
    private val FULL_WIDTH_ASCII = Regex("[\uFF01-\uFF5E]")
    private val EMOJI_RANGES = Regex(
        "[\u2600-\u26FF\u2700-\u27BF]|" +
        "[\u231A-\u231B\u23E9-\u23F3\u23F8-\u23FA\u25AA-\u25AB\u25B6\u25C0\u25FB-\u25FE]|" +
        "[\u2B05-\u2B07\u2B1B-\u2B1C\u2B50\u2B55\u3030\u303D\u3297\u3299]|" +
        "(?:[\uD83C-\uDBFF][\uDC00-\uDFFF])"
    )
    private val SEPARATORS_BETWEEN_LETTERS = Regex("([\\p{L}\\u0590-\\u05FF])([\\s\\-*·_•\u2002\u2003\u2009\u00A0]+)([\\p{L}\\u0590-\\u05FF])")

    private val SOFIT_MAP = mapOf(
        '\u05DA' to '\u05DB',
        '\u05DD' to '\u05DE',
        '\u05DF' to '\u05E0',
        '\u05E3' to '\u05E4',
        '\u05E5' to '\u05E6'
    )

    private val DIGIT_TO_HEBREW = mapOf(
        '0' to 'ו', '5' to 'ש', '4' to 'ד', '7' to 'ז', '1' to 'א'
    )

    private val ARABIC_INDIC_TO_ASCII = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )

    private val CYRILLIC_TO_LATIN = mapOf(
        'а' to 'a', 'е' to 'e', 'і' to 'i', 'о' to 'o', 'с' to 'c',
        'А' to 'A', 'Е' to 'E', 'І' to 'I', 'О' to 'O', 'С' to 'C',
        '\u0430' to 'a', '\u0435' to 'e', '\u0456' to 'i', '\u043E' to 'o', '\u0441' to 'c'
    )

    private val NIQQUD_RANGE = '\u0591'..'\u05C7'
    private val PRESENTATION_FORMS = '\uFB1D'..'\uFB4F'

    fun normalizeForMatching(text: String): String {
        if (text.isEmpty()) return text
        var t = text.take(MAX_INPUT_LENGTH)
        t = removeBidiAndZeroWidth(t)
        t = normalizeHomoglyphs(t)
        t = normalizeFullWidth(t)
        t = removeEmojis(t)
        t = removeNiqqud(t)
        t = normalizeFinalLetters(t)
        t = collapseSeparators(t)
        t = numberLetterSubstitutionFix(t)
        t = normalizeSpacing(t)
        return t
    }

    fun removeBidiAndZeroWidth(text: String): String =
        text.replace(ZERO_WIDTH_AND_JOINERS, "").replace(BIDI_CONTROL, "")

    fun normalizeHomoglyphs(text: String): String =
        text.map { c -> CYRILLIC_TO_LATIN[c] ?: c }.joinToString("")

    fun normalizeFullWidth(text: String): String =
        FULL_WIDTH_ASCII.replace(text) { match ->
            val c = match.value[0]
            (c.code - 0xFF01 + 0x21).toChar().toString()
        }

    fun removeEmojis(text: String): String = text.replace(EMOJI_RANGES, " ")

    fun removeNiqqud(text: String): String = text.filter { c ->
        when {
            Character.getType(c) == 6 -> false  // Character.NON_SPACING_MARK
            c in NIQQUD_RANGE -> false
            c == '\u05C1' -> false
            c in PRESENTATION_FORMS -> true
            else -> true
        }
    }.map { c ->
        if (c in PRESENTATION_FORMS) presentationFormToBase(c) else c
    }.joinToString("")

    private fun presentationFormToBase(c: Char): Char = when (c) {
        '\uFB2E', '\uFB2F' -> '\u05D0'
        '\uFB31', '\uFB32' -> '\u05D1'
        '\uFB34' -> '\u05D3'
        '\uFB35', '\uFB36' -> '\u05D4'
        '\uFB38', '\uFB39' -> '\u05D5'
        '\uFB3B' -> '\u05D6'
        '\uFB3C' -> '\u05D7'
        '\uFB3E' -> '\u05D8'
        '\uFB40', '\uFB41' -> '\u05D9'
        '\uFB44' -> '\u05DB'
        '\uFB46' -> '\u05DC'
        '\uFB4A', '\uFB4B' -> '\u05DE'
        '\uFB4C' -> '\u05E0'
        '\uFB4E' -> '\u05E1'
        '\uFB4F' -> '\u05E2'
        else -> c
    }

    fun normalizeFinalLetters(text: String): String =
        text.map { SOFIT_MAP[it] ?: it }.joinToString("")

    fun collapseSeparators(text: String): String {
        var result = text
        var prev: String
        do {
            prev = result
            result = SEPARATORS_BETWEEN_LETTERS.replace(result) {
                it.groupValues[1] + it.groupValues[3]
            }
        } while (result != prev)
        return result
    }

    fun numberLetterSubstitutionFix(text: String): String {
        var t = text.map { ARABIC_INDIC_TO_ASCII[it] ?: it }.joinToString("")
        if (!t.any { it in '\u05D0'..'\u05EA' }) return t
        for ((digit, hebrew) in DIGIT_TO_HEBREW) {
            t = t.replace(digit.toString(), hebrew.toString())
        }
        return t
    }

    fun normalizeSpacing(text: String): String =
        text.trim().replace(Regex("\\s+"), " ")

    fun tokenize(text: String): List<String> =
        Regex("[\\s\\p{Z}\\p{P}]+").split(normalizeForMatching(text))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
