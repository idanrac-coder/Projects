# Hebrew/Israeli Spam Detection Engine – Security Audit

**Audit Date**: 2025-03-09  
**Auditor Role**: Senior Security Engineer, ML Engineer, Android Architect  
**Scope**: Plan and design audit for production-grade Hebrew SMS spam detection

---

## Executive Summary

The original plan has solid foundations but contains **critical gaps** that would allow spammers to bypass detection and cause **high false positive rates** on legitimate Israeli messages. This audit identifies 47 weaknesses and proposes concrete fixes to harden the system.

---

## 1. Architecture Review

### 1.1 Weaknesses

| ID | Weakness | Severity |
|----|----------|----------|
| A1 | ** Hebrew engine is optional path**: When `body.any { it in '\u0590'..'\u05FF' }` is false, pure-Latin spam (e.g. "DHL package bit.ly/xyz") bypasses Hebrew engine entirely | Critical |
| A2 | **Pipeline ordering is brittle**: DeterministicLayer runs on raw body; obfuscated text never reaches keyword scoring | High |
| A3 | **Score combination undefined**: Plan says "combine scores" but does not specify weighted formula; ML vs heuristic conflict resolution unspecified | Medium |
| A4 | **Single point of failure**: If HebrewSpamEngine throws, fallback behavior is unspecified | Medium |
| A5 | **No defense in depth**: Hebrew engine and existing pipeline run sequentially, not in parallel—spammer can target the weaker path | Medium |
| A6 | **SemanticSpamLayer uses Dispatchers.IO**: Introduces coroutine overhead; plan targets <10ms but SemanticSpamLayer is async and may block on DB | Medium |

### 1.2 Bypass Vectors

- **Pure Latin Israeli spam**: "Leumi bank account blocked. Click bit.ly/xxx" — no Hebrew, Hebrew engine never runs.
- **Minimal Hebrew padding**: "ה" + Latin spam — Hebrew present but engine may under-weight Latin content.

### 1.3 Fixes

1. **Unified pipeline**: Always run Hebrew-aware normalization and Israeli patterns. Use Hebrew detection only to *boost* Hebrew-specific scoring, not to gate the engine.
2. **Explicit score fusion**: `finalScore = 0.6 * heuristicScore + 0.4 * mlScore` with fallback when ML unavailable. Document formula.
3. **Fail-safe**: On HebrewSpamEngine exception → fall back to existing SpamFilter pipeline (Deterministic + Heuristic + Semantic).
4. **Preprocessing first**: Run `HebrewTextNormalizer` (with deobfuscation) on *all* messages before any regex/layer. Apply layers to normalized text.
5. **Offline guarantee**: Ensure `SemanticSpamLayer.getOrCreateExtractor()` and `shortCodeWhitelistDao.isWhitelisted()` use only local data. ML Kit downloads model once; whitelist is Room. Confirm no implicit network.

---

## 2. Hebrew Language Handling

### 2.1 Niqqud Removal

**Plan**: Strip `\u0591`–`\u05C7`.

**Weakness**: Hebrew Unicode ranges:
- **Niqqud (combining)**: U+05B0–U+05C2 (shva, hiriq, tsere, etc.)
- **Cantillation**: U+0591–U+05AF
- **Dagesh, mappiq**: U+05BC, U+05BF
- **Precomposed (presentation forms)**: U+FB1D–U+FB4F (e.g. U+FB2E = אַ)

**Fix**: Use `Character.getType(c) == Character.NON_SPACING_MARK` for combining marks. Also strip **presentation forms** by normalizing to NFD (decomposed) and removing combining marks, or map common presentation forms (U+FB1D–U+FB4F) to base letters.

### 2.2 Final Letters

**Plan**: Map ך→כ, ם→מ, ן→נ, ף→פ, ץ→צ.

**Weakness**: Plan is correct, but **sofit letters must be normalized before tokenization** so "חֶשְׁבּוֹן" (with nikud) → "חשבון" after both nikud removal and sofit normalization.

### 2.3 RTL Punctuation

**Plan**: "Normalize mirror chars ( vs )".

**Weakness**: Plan is vague. RTL text can have punctuation in wrong logical order. Unicode has explicit RTL marks (U+200F RLM, U+202E RLO) that can be exploited.

**Fix**:
- Strip bidirectional control chars: U+200E (LRM), U+200F (RLM), U+202A–U+202E (embedding overrides), U+2066–U+2069 (isolates).
- Optionally swap ( and ) when preceded by RTL text for consistent matching.

### 2.4 Mixed Hebrew + English + Numbers

**Plan**: "Keep Hebrew + Latin + digits; normalize whitespace."

**Weakness**: No handling of **mixed-direction** word boundaries. Tokenizer must not split "הקוד 483221" incorrectly.

**Fix**: Tokenize by `[\s\p{Z}\p{P}]+` and keep sequences of `[\p{L}\p{N}]+` as tokens. Hebrew and Latin can coexist in same token only if no space (e.g. "DHLחבילה" — treat as two tokens if we insert space at script boundary).

### 2.5 Obfuscated Hebrew – Critical Gap

**Plan**: Does **not** handle obfuscation.

**Evasion examples**:

| Evasion | Example | Detection |
|---------|---------|-----------|
| Hyphens between letters | ח-ש-ב-ו-ן | ❌ Bypasses |
| Spaces | ח ש ב ו ן | ❌ Bypasses |
| Asterisks | ח*ש*ב*ו*ן | ❌ Bypasses |
| Number substitution | חשב0ן (0→ו) | ❌ Bypasses |
| Latin lookalikes | ח5בון (5→ש) | ❌ Bypasses |
| Zero-width joiners | ח‍ש‍ב‍ו‍ן (U+200D) | ❌ Bypasses |

**Fix – Deobfuscation Preprocessor**:

```kotlin
// 1. Remove zero-width and joiners: U+200B, U+200C, U+200D, U+FEFF
// 2. Replace common separators between Hebrew letters: [-*\s·]+ with empty
// 3. Replace digit/letter homoglyphs: 0→ו, 5→ש, 4→פ (contextual), 7→ז
// 4. Build "normalized for matching" string before keyword/regex
```

Apply deobfuscation in `HebrewTextNormalizer.normalizeForMatching(text: String)` before keyword scoring and regex.

---

## 3. Spam Evasion Techniques

### 3.1 Unicode Tricks

| Technique | Mitigation |
|-----------|------------|
| Homoglyphs (Cyrillic а vs Latin a in URLs) | LinkAnalyzer: decode punycode; flag IDN with non-Latin in domain |
| Full-width chars (ＵＲＬ) | Normalize full-width (U+FF01–U+FF5E) to ASCII before URL regex |
| Invisible chars | Strip U+200B–U+200F, U+2028–U+202F, U+2060–U+206F |
| RTL override | Strip U+202E RLO so "moc.elpmaxe" displays as "example.com" |

### 3.2 Spaces Between Characters

**Mitigation**: Deobfuscation step: collapse `[\s\-*·]+` between consecutive Hebrew letters (or `\p{L}`) into empty string. Re-tokenize after.

### 3.3 Replacing Letters with Numbers

**Map** (Hebrew context):
- 0 ↔ ו (vav)
- 5 ↔ ש (shin) or ה (he)
- 4 ↔ ד (dalet) or פ (pe)
- 7 ↔ ז (zayin)
- 1 ↔ א (alef) or י (yod)

**Mitigation**: Before matching, try substitutions to restore likely Hebrew words. Use a small dictionary of spam terms; if `ח5בון` normalizes to `חשבון` (after 5→ש), match.

### 3.4 Emojis Between Words

**Mitigation**: Strip emoji ranges (U+1F300–U+1F9FF, U+2600–U+26FF, etc.) before keyword matching. Emojis in legitimate messages are less common in banking; in spam they add noise. Option: strip only when emoji count > 2 to reduce FP on casual chat.

### 3.5 Shortened Words

**Example**: "חשבן" instead of "חשבון", "אימות" → "אימ'.

**Mitigation**: Character n-grams in ML layer (see Section 8) help. For rules, add common typo/abbrev variants: "חשבן", "חשבונך", "חשבון שלך".

### 3.6 Random Characters

**Example**: "חשבוןxxxנחסם".

**Mitigation**: Run regex on **deobfuscated** text where we collapse repeated non-Hebrew between Hebrew runs. E.g. `ח[\W\d]{0,3}ש[\W\d]{0,3}ב` → `חשב`.

### 3.7 Image MMS

**Plan**: MMS body not parsed.

**Risk**: Spammers send phishing as image. No text to analyze.

**Mitigation**: Document as known limitation. Future: OCR (ML Kit Text Recognition) on MMS images—expensive. For now, treat MMS from unknown senders as SUSPECTED if no other signal.

### 3.8 URL Redirect Chains

**Example**: bit.ly → intermediate → final phishing domain.

**Mitigation**: Cannot resolve redirects offline. Mitigations:
- Treat all shorteners as suspicious (plan already does).
- Add more shortener domains: adf.ly, ouo.io, bc.vc, tr.im, ow.ly.
- Flag URLs with multiple redirect hints (e.g. "redirect" in path).

---

## 4. Israeli-Specific Scam Patterns

### 4.1 Missing Patterns

| Category | Missing Patterns | Suggested Regex/Keywords |
|----------|------------------|---------------------------|
| Fake missile alerts | זיוף פיקוד העורף, התראת מסיכה, Red Alert fake | `פיקוד העורף|התראת.?מסיכה|red.?alert|אזעקה.?שקר` |
| Bank phishing | Bank names: לאומי, הפועלים, דיסקונט, מזרחי, מרכנתיל | `בנק.?לאומי|הפועלים|דיסקונט|מזרחי|מרכנתיל` + "נחסם" |
| Delivery | דואר ישראל, בראון, AM_PM, Xpress | `דואר ישראל|בראון|AM.PM|אקספרס` + "חבילה" |
| Tax refund | רשות המיסים, מאגר המס | `רשות המיסים|מאגר המס` + "החזר" |
| Police/Gov | משטרת ישראל, מבקר המדינה | `משטרת ישראל|מבקר המדינה` + urgency |
| Crypto/investment | ביטוחן, פקסי, קריפטו, מטבע דיגיטלי | `ביטוחן|פקסי|מטבע דיגיטלי` |

### 4.2 Bank Name Lists

**Add explicit whitelist of legitimate senders** (short codes) for banks. If sender is whitelisted bank short code + OTP pattern → likely SAFE. If body mentions bank but sender is random number → DEFINITE_SPAM.

**Israeli bank short codes** (example): Leumi, Hapoalim, Discount use specific numeric senders. Maintain a static list or allow user to add.

### 4.3 Fake Red Alert / Missile Alerts

High-impact: fake "Red Alert" / "חפש מקלט" causes panic. Add:
- `מתקפת טילים|חפש מקלט|פיקוד העורף|אזעקה` + negative for official Pikud Haoref number (if known).
- Consider: official alerts come from specific sender; others = DEFINITE_SPAM.

---

## 5. Link Detection

### 5.1 Shortened Links

**Plan**: bit.ly, tinyurl, t.co, rb.gy, cutt.ly, is.gd, v.gd, qr.ae, clck.ru.

**Add**: adf.ly, ouo.io, bc.vc, tr.im, ow.ly, buff.ly, j.mp, tiny.cc, short.link, rebrand.ly.

### 5.2 Punycode Domains

**Plan**: Detect `xn--` in hostname.

**Weakness**: Some implementations only check `xn--` prefix. Full punycode can be in path. Also, need to **decode** punycode to detect homoglyphs (e.g. "leumi" vs "lеumi" with Cyrillic е).

**Fix**: Use `java.net.IDN.toUnicode(host)` to decode. If decoded string contains non-ASCII (e.g. Cyrillic, Greek), flag as PUNYCODE_SUSPICIOUS.

### 5.3 Brand Impersonation

**Plan**: "Check against known Israeli brands."

**Missing**: Concrete list. Add:
- Banks: leumi.co.il, bankhapoalim.co.il, discountbank.co.il, mizrahi.co.il
- Israel Post: israelpost.co.il
- Government: gov.il, tax.gov.il
- Parcel: dhl.co.il, ups.com, 012.co.il (Bezeq)

**Logic**: If body mentions "Leumi" or "בנק לאומי" but link domain is bit.ly or xyz.tk → BRAND_MISMATCH.

### 5.4 Suspicious TLDs

**Plan**: tk, ml, ga, cf, gq, xyz, top, buzz, club, icu, work, info, rest, click, link, online, site, fun.

**Add**: ru, cc, ws, bzz, live, stream, digital, tech, space.

---

## 6. Phone Number Heuristics

### 6.1 Spoofed Sender IDs

**Reality**: SMS sender can be spoofed. Carrier-level validation is outside app control.

**Mitigation**:
- Alphanumeric senders (e.g. "Leumi", "DHL") are more likely legitimate for brands; numeric spoofing is common for scams.
- If sender is numeric and body claims to be from bank → BRAND_SENDER_MISMATCH.

### 6.2 International Numbers

**Plan**: Non-972 when user locale is IL → +score.

**Improvement**: Add dial codes often used in Israeli scam: 44, 49, 91, 1, 7. Weight by dial code (e.g. 91, 7 higher risk).

### 6.3 Bulk Messaging

**Plan**: Deferred.

**Suggestion**: Use existing `SpamLearningDao` / `SpamSenderReputationEntity`. If same address sent multiple messages in short window (e.g. 5 in 1 hour), boost score. Requires timestamps; can be done via Room.

### 6.4 Sender vs Link Brand Mismatch

**Plan**: "If body contains bank name but sender is numeric → mismatch."

**Improvement**: Explicit matrix:
- Body: bank/delivery/gov keyword + Link: shortener/suspicious TLD → DEFINITE_SPAM
- Body: bank + Sender: alphanumeric "Leumi" → likely SAFE if whitelisted
- Body: bank + Sender: 050-xxx-xxxx → SUSPECTED

---

## 7. False Positives

### 7.1 Legitimate Cases at Risk

| Message Type | Risk | Mitigation |
|--------------|------|------------|
| Bank OTP | "הקוד שלך 483221" | OTP from whitelisted bank short code → SAFE. OTP + "לחץ כאן" + URL → SPAM |
| Delivery notification | "חבילה בדואר" | Whitelist Israel Post, DHL, etc. Short code whitelist. No URL + whitelisted sender → SAFE |
| Doctor appointment | "פגישה נדחתה" | "פגישה" alone is benign. "פגישה" + urgency + URL → SPAM. Avoid single-keyword FP |
| School messages | "הודעה מביה\"ס" | "הודעה" is generic; don't flag "הודעה אחרונה" without URL/urgency combo |

### 7.2 Keyword Refinement

**Problem**: "קוד אימות" flags legitimate OTP.

**Fix**: **Contextual scoring**. OTP alone = low score. OTP + URL = high. OTP + "שלח לי" (send me) = OTP fraud.

**Rule**: Require **2+ spam signals** for SUSPECTED, **3+** for DEFINITE when one signal is OTP/verify.

### 7.3 Short Code Whitelist

**Critical**: Populate short code whitelist with known Israeli legitimate senders (banks, Israel Post, cellular carriers). User "Report not spam" → add to allowlist (already exists). Add **seed data** for Israeli banks and postal service.

---

## 8. ML Model Evaluation

### 8.1 Is TF-IDF Sufficient?

**Weakness**: TF-IDF on word tokens fails for:
- Obfuscation (ח5בון)
- Novel phrasing
- Character-level tricks

**Recommendation**: **Add character n-grams** (3–5 grams) as secondary feature. "חשבון" → חש,, במ, מב, בו, ון. Obfuscated "ח5בון" shares n-grams. Vocabulary size grows but stays manageable (~10K n-grams). Ensemble: `score = 0.5 * wordTfidf + 0.5 * charNgramTfidf`.

### 8.2 FastText for Hebrew

**Pros**: Subword n-grams; good for morphologically rich Hebrew.  
**Cons**: FastText on Android requires JNI/native lib or ONNX/TFLite wrapper; adds complexity and binary size.

**Recommendation**: Stay with **pure Kotlin TF-IDF + char n-grams** for v1. Revisit FastText/TFLite if accuracy is insufficient after deployment.

### 8.3 Character N-grams

**Implement**: Extract 3–5 char n-grams from normalized text. Include in vocabulary. Train logistic regression on word TF-IDF + char n-gram TF-IDF. Model size: +200–400KB. Inference: +1–2ms.

### 8.4 Model Size

Target <5MB. Word vocab 5K + char n-grams 5K + coefficients = ~500KB. Well under limit.

---

## 9. Performance Review

### 9.1 Latency Budget

| Stage | Budget | Notes |
|-------|--------|-------|
| Normalize + deobfuscate | 1ms | Single pass |
| Deterministic regex | 0.5ms | Compiled regex, short-circuit |
| Keyword scorer | 1ms | HashSet lookup |
| Link analyzer | 0.5ms | Regex + domain list |
| Phone heuristics | 0.2ms | Simple checks |
| ML inference | 3ms | Map lookups, no allocation |
| **Total** | **~6ms** | Under 10ms |

### 9.2 Memory

- Avoid loading full model into memory twice. Singleton HebrewSpamMlLayer.
- Lazy-init ML model on first Hebrew message.
- Regex: compile once (object/companion).

### 9.3 Low-End Devices

- Run on `Dispatchers.Default` (CPU-bound). Avoid `Dispatchers.IO` for pure compute.
- SemanticSpamLayer uses IO for Room/ML Kit; ensure DB query is indexed and fast.
- Consider skipping ML layer on very low RAM (<2GB); use heuristic-only.

---

## 10. Output Improvements

### 10.1 Enhanced SpamResult

```kotlin
data class SpamResult(
    val score: Float,
    val confidence: Float,           // 0-1, based on signal count
    val category: SpamCategory,
    val reasons: List<String>,
    val triggeredRules: List<TriggeredRule>,
    val modelVersion: String? = null  // for debugging/telemetry
)

data class TriggeredRule(
    val ruleId: String,
    val ruleType: String,  // DETERMINISTIC, KEYWORD, LINK, PHONE, ML
    val matchedValue: String?,
    val scoreContribution: Float
)

enum class SpamCategory {
    SAFE,
    SUSPECTED_SPAM,
    DEFINITE_SPAM
}
```

**Confidence**: `min(1f, 0.3f + 0.1f * triggeredRules.size)` when rules > 2. Fewer rules = lower confidence for same score.

---

## 11. Security Hardening

### 11.1 Adversarial Text Normalization

- Apply deobfuscation before any matching.
- Limit input length (e.g. 2000 chars) to prevent ReDoS via regex.
- Use `Regex.find()` with timeout if supported, or cap input.

### 11.2 Anti-Obfuscation

See Section 2.5 and 3. Implement `HebrewTextNormalizer.deobfuscate()`.

### 11.3 Domain Reputation Caching

- Cache "known good" domains (leumi.co.il, gov.il) in memory.
- Cache "known bad" shorteners in static set.
- No network; all from bundled config.

### 11.4 Sender Reputation

- Already have `SpamSenderReputationEntity`. Use in HebrewSpamEngine.
- High spam count → boost score. High ham count → reduce.
- Integrate with `ScamDetector.getSenderReputation()`.

---

## 12. Final Deliverables

### 12.1 All Discovered Weaknesses (47 total)

1. Hebrew engine gated by Hebrew presence; Latin-only bypass
2. No deobfuscation (hyphens, spaces, numbers, homoglyphs)
3. Niqqud range incomplete; precomposed forms not handled
4. RTL/bidi control chars not stripped
5. No obfuscation-resistant keyword matching
6. Punycode not decoded for homoglyph check
7. Brand list for Israeli entities missing
8. Shortener list incomplete
9. Bank short code whitelist not seeded
10. OTP false positives (legitimate bank codes)
11. No contextual scoring (OTP + URL)
12. TF-IDF only; no char n-grams
13. Score fusion formula undefined
14. Pipeline order: raw text to regex (pre-deobfuscation)
15. MMS image spam not addressed
16. SemanticSpamLayer IO may affect latency
17. No fail-safe on Hebrew engine exception
18. Spoofed sender detection limited
19. Bulk-send heuristic deferred
20. "הודעה" too generic (school messages)
21. Medical/parking tag rules may FP on legitimate disability services
22. TLD list incomplete
23. URL path punycode not checked
24. Full-width Unicode in URLs
25. Zero-width chars in body
26. RTL override in URLs
27. Emoji stuffing evasion
28. Shortened word variants
29. Redirect chains (document as limitation)
30. Fake Red Alert specific patterns incomplete
31. Bank names: לאומי, הפועלים, דיסקונט, מזרחי not explicit
32. Phone heuristic: dial code weighting missing
33. SpamResult lacks confidence
34. SpamResult lacks triggered rules
35. Model version not tracked
36. Input length unbounded (ReDoS risk)
37. No low-memory fallback
38. Presentation form handling missing
39. Mixed-script tokenization edge cases
40. Dagesh/mappiq not stripped
41. "פגישה" in school context FP risk
42. "מענק" can be legitimate (Bituach Leumi)
43. Delivery senders (בראון, AM_PM) not whitelisted
44. Crypto keywords may FP on legitimate
45. No explicit Israeli government domain list
46. Char n-gram vocab size not specified
47. Confidence formula not defined

### 12.2 Concrete Fixes – Priority Order

**P0 (Critical)**:
- Add deobfuscation preprocessor.
- Run Hebrew engine path for all messages (or at least Israeli-pattern path).
- Add OTP contextual scoring (OTP alone vs OTP+URL).
- Seed short code whitelist with Israeli banks/post.

**P1 (High)**:
- Extend niqqud/stripping to presentation forms and NFD.
- Add punycode decode + homoglyph check in LinkAnalyzer.
- Add Israeli bank names + brand-domain list.
- Define and implement score fusion formula.
- Add fail-safe fallback.

**P2 (Medium)**:
- Add character n-grams to ML.
- Extend shortener and TLD lists.
- Implement enhanced SpamResult with confidence and triggered rules.
- Add input length cap.
- Strip bidi/zero-width chars.

### 12.3 Improved Architecture

```
[Raw Body] → [Input Sanitizer: length cap, strip bidi/zw] 
          → [HebrewTextNormalizer: normalize + deobfuscate]
          → [Normalized Body]
                    ↓
    ┌───────────────┼───────────────┐
    ↓               ↓               ↓
[Deterministic] [KeywordScorer] [LinkAnalyzer]
    ↓               ↓               ↓
    └───────────────┼───────────────┘
                    ↓
            [PhoneHeuristics]
                    ↓
            [HebrewSpamMlLayer] (word + char n-gram)
                    ↓
            [Score Fusion + Category]
                    ↓
            [SpamResult]
                    ↓
    (optional) [Sender Reputation adjustment]
```

### 12.4 Improved Detection Rules (Additions)

**DeterministicSpamLayer additions**:
```kotlin
// Fake missile/Red Alert
"מתקפת טילים|חפש מקלט|פיקוד העורף|אזעקה|red.?alert"
// Bank names
"בנק.?לאומי|הפועלים|דיסקונט|מזרחי|מרכנתיל"
// Israeli Post
"דואר ישראל|Israel.?Post"
// Obfuscation-resistant (after deobfuscation)
"ח[-*\\s]*ש[-*\\s]*ב[-*\\s]*ו[-*\\s]*ן"  // חשבון with separators
```

**Keyword additions** (HebrewKeywordScorer):
- לאומי, הפועלים, דיסקונט, מזרחי (bank names)
- פיקוד העורף, התראת מסיכה
- ביטוחן, פקסי (investment scams)

### 12.5 Improved Kotlin Code (Key Snippets)

**Deobfuscation**:
```kotlin
fun deobfuscate(text: String): String {
    var t = text
    // Remove ZWJ, ZWNJ, BOM, etc.
    t = t.replace(Regex("[\\u200B-\\u200D\\uFEFF\\u2060]"), "")
    // Collapse separators between Hebrew letters
    t = t.replace(Regex("([\\p{IsHebrew}])([\\s\\-*·]+)([\\p{IsHebrew}])")) { 
        it.groupValues[1] + it.groupValues[3] 
    }
    // Digit→letter (conservative)
    t = t.replace("0", "ו").replace("5", "ש")  // context-dependent
    return t
}
```

**Punycode check**:
```kotlin
fun isSuspiciousPunycode(host: String): Boolean {
    val decoded = try { java.net.IDN.toUnicode(host) } catch (_: Exception) { host }
    return decoded.any { it.code in 0x400..0x4FF }  // Cyrillic
        || decoded.any { it.code in 0x370..0x3FF }  // Greek
}
```

---

## Conclusion

The plan is a good foundation but requires the **critical hardening** above to reach production quality. The highest-impact improvements are:

1. **Deobfuscation preprocessing** – blocks most evasion.
2. **Unified pipeline** – no Latin-only bypass.
3. **Contextual OTP scoring** – reduces false positives.
4. **Israeli brand/whitelist data** – improves precision.
5. **Character n-grams in ML** – improves recall on obfuscation.

Implementing these will significantly improve detection and reduce false positives for Hebrew and Israeli SMS.
