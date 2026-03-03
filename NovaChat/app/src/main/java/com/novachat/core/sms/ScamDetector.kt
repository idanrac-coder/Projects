package com.novachat.core.sms

import android.util.Log
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.entity.SpamKeywordWeightEntity
import com.novachat.core.database.entity.SpamLearningEntity
import com.novachat.core.database.entity.SpamSenderReputationEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class ScamAnalysis(
    val isScam: Boolean,
    val confidence: Float,
    val reason: String?,
    val category: ScamCategory?,
    val signals: List<String> = emptyList()
)

enum class ScamCategory {
    PHISHING,
    OTP_FRAUD,
    PRIZE_SCAM,
    JOB_SCAM,
    LOAN_SCAM,
    IMPERSONATION,
    URGENT_ACTION,
    SUSPICIOUS_LINK,
    ADVANCE_FEE,
    TECH_SUPPORT,
    ROMANCE_SCAM,
    INVESTMENT_SCAM,
    DELIVERY_SCAM,
    TAX_SCAM,
    CRYPTO_SCAM,
    SUBSCRIPTION_SCAM,
    LEARNED_SPAM
}

/**
 * Learning spam/scam detection agent.
 *
 * Combines rule-based pattern matching with a lightweight Bayesian classifier
 * that trains on user feedback. Every time a user confirms or dismisses a
 * scam warning, the agent stores that signal and adjusts future scoring.
 */
@Singleton
class ScamDetector @Inject constructor(
    private val learningDao: SpamLearningDao
) {
    private val mutex = Mutex()

    // Cached learned data — refreshed from DB lazily.
    private var senderReputationCache = mutableMapOf<String, SpamSenderReputationEntity>()
    private var keywordWeightCache = mutableMapOf<String, Float>()
    private var cacheInitialized = false

    // ── Rule-based patterns (expanded) ───────────────────────────────────

    private data class PatternRule(
        val regex: Regex,
        val category: ScamCategory,
        val baseScore: Float,
        val description: String
    )

    private val rules: List<PatternRule> = listOf(
        // Phishing
        PatternRule(Regex("(?i)your\\s+account\\s+(has been|is|was)\\s+(suspend|block|lock|compromis|restrict|deactivat|limit)"), ScamCategory.PHISHING, 0.82f, "Account suspension phishing"),
        PatternRule(Regex("(?i)verify\\s+your\\s+(identity|account|bank|card|details|information)\\s*(immediately|now|urgently|within)"), ScamCategory.PHISHING, 0.84f, "Identity verification phishing"),
        PatternRule(Regex("(?i)click\\s+(here|below|this|the link)\\s+to\\s+(verify|confirm|unlock|restore|update|secure)"), ScamCategory.PHISHING, 0.80f, "Click-to-verify phishing"),
        PatternRule(Regex("(?i)we\\s+(detected|noticed|found)\\s+(unusual|suspicious|unauthorized|unknown)\\s+(activity|access|login|transaction|sign.?in)"), ScamCategory.PHISHING, 0.83f, "Suspicious activity phishing"),
        PatternRule(Regex("(?i)(update|confirm)\\s+your\\s+(payment|billing|bank)\\s+(details|info|method|information)"), ScamCategory.PHISHING, 0.78f, "Payment update phishing"),
        PatternRule(Regex("(?i)your\\s+(password|credentials)\\s+(has|have)\\s+(expired|been\\s+(changed|reset|compromised))"), ScamCategory.PHISHING, 0.81f, "Password compromise phishing"),
        PatternRule(Regex("(?i)(apple|google|microsoft|amazon|netflix|paypal|bank).{0,20}(suspend|verify|confirm|unauthorized|security\\s+alert)"), ScamCategory.PHISHING, 0.79f, "Brand impersonation phishing"),

        // OTP fraud
        PatternRule(Regex("(?i)(share|send|tell|give|forward)\\s+(me|us)\\s+(the|your)\\s+(otp|code|pin|password|verification)"), ScamCategory.OTP_FRAUD, 0.92f, "OTP sharing request"),
        PatternRule(Regex("(?i)i\\s+(accidentally|mistakenly|wrongly)\\s+sent\\s+(a|an|my|the)\\s+(otp|code|verification)"), ScamCategory.OTP_FRAUD, 0.90f, "Accidental OTP scam"),
        PatternRule(Regex("(?i)please\\s+(send|share|give).{0,20}(code|otp|pin|verification).{0,20}(sent|received|got)"), ScamCategory.OTP_FRAUD, 0.88f, "OTP relay scam"),

        // Prize / lottery
        PatternRule(Regex("(?i)(congratulations|congrats)!?\\s+you('ve|\\s+have)\\s+(won|been\\s+selected|been\\s+chosen|qualified)"), ScamCategory.PRIZE_SCAM, 0.87f, "Prize notification scam"),
        PatternRule(Regex("(?i)you\\s+(won|have\\s+won|are\\s+the\\s+winner)\\s+\\$?[\\d,]+"), ScamCategory.PRIZE_SCAM, 0.88f, "Lottery winning scam"),
        PatternRule(Regex("(?i)claim\\s+your\\s+(prize|reward|gift|winnings|cash|bonus|free)"), ScamCategory.PRIZE_SCAM, 0.85f, "Prize claim scam"),
        PatternRule(Regex("(?i)free\\s+(iphone|ipad|gift\\s*card|cash|money|reward|voucher|samsung|macbook)"), ScamCategory.PRIZE_SCAM, 0.83f, "Free device scam"),
        PatternRule(Regex("(?i)your\\s+(number|email|phone)\\s+(has been|was)\\s+selected\\s+(for|to|as)"), ScamCategory.PRIZE_SCAM, 0.84f, "Selection notification scam"),

        // Job scam
        PatternRule(Regex("(?i)(work\\s+from\\s+home|earn)\\s+\\$?\\d{3,}\\s*(per|a|/|every)\\s*(day|hour|week)"), ScamCategory.JOB_SCAM, 0.78f, "Work-from-home scam"),
        PatternRule(Regex("(?i)hiring\\s+(now|immediately|urgently).{0,30}(no\\s+experience|apply\\s+now|no\\s+interview)"), ScamCategory.JOB_SCAM, 0.76f, "Urgent hiring scam"),
        PatternRule(Regex("(?i)make\\s+\\$?\\d{3,}\\s*(daily|weekly|monthly)\\s*(from|at)\\s*(home|anywhere)"), ScamCategory.JOB_SCAM, 0.77f, "Income promise scam"),
        PatternRule(Regex("(?i)(easy|simple)\\s+(money|income|cash|job).{0,30}(home|online|daily|weekly)"), ScamCategory.JOB_SCAM, 0.72f, "Easy money scam"),
        PatternRule(Regex("(?i)data\\s+entry.{0,20}\\$?\\d{2,}\\s*(per|/|an?)\\s*(hour|task|page)"), ScamCategory.JOB_SCAM, 0.70f, "Data entry scam"),

        // Loan scam
        PatternRule(Regex("(?i)(pre-?approved|instant|guaranteed|emergency)\\s+(loan|credit|cash|fund)\\s*(of\\s+)?(\\$|Rs\\.?|₹|€|£)?\\d"), ScamCategory.LOAN_SCAM, 0.78f, "Pre-approved loan scam"),
        PatternRule(Regex("(?i)loan\\s+(approved|disburs|sanction).{0,30}(click|tap|link|call|download)"), ScamCategory.LOAN_SCAM, 0.77f, "Loan approval scam"),
        PatternRule(Regex("(?i)(low|zero|no)\\s+(interest|processing\\s+fee).{0,20}(loan|credit|cash)"), ScamCategory.LOAN_SCAM, 0.73f, "Low interest loan scam"),
        PatternRule(Regex("(?i)(bad\\s+credit|no\\s+credit\\s+check).{0,30}(guaranteed|approved|instant)"), ScamCategory.LOAN_SCAM, 0.75f, "Bad credit loan scam"),

        // Urgency tactics
        PatternRule(Regex("(?i)(act\\s+now|respond\\s+immediately|urgent|expires?\\s+(today|soon|in\\s+\\d+\\s*(hour|min|second)))"), ScamCategory.URGENT_ACTION, 0.68f, "Urgency pressure tactic"),
        PatternRule(Regex("(?i)your\\s+(package|shipment|delivery|parcel)\\s+(couldn't|could\\s+not|failed|has\\s+been|was).{0,40}(click|tap|link|track)"), ScamCategory.DELIVERY_SCAM, 0.76f, "Delivery notification scam"),
        PatternRule(Regex("(?i)(irs|tax|government|police|court|doj|fbi)\\s+.{0,30}(warrant|arrest|legal\\s+action|fine|penalty|investigation)"), ScamCategory.TAX_SCAM, 0.82f, "Government impersonation scam"),
        PatternRule(Regex("(?i)final\\s+(notice|warning|reminder|attempt).{0,30}(account|payment|action|respond)"), ScamCategory.URGENT_ACTION, 0.72f, "Final notice pressure"),
        PatternRule(Regex("(?i)failure\\s+to\\s+(respond|act|verify|comply).{0,30}(result|lead|cause).{0,20}(suspension|closure|legal|arrest)"), ScamCategory.URGENT_ACTION, 0.78f, "Consequence threat"),

        // Suspicious links
        PatternRule(Regex("(?i)(bit\\.ly|tinyurl|t\\.co|goo\\.gl|rb\\.gy|shorturl|cutt\\.ly|is\\.gd|v\\.gd|qr\\.ae|clck\\.ru)/\\S+"), ScamCategory.SUSPICIOUS_LINK, 0.62f, "Shortened URL detected"),
        PatternRule(Regex("(?i)https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}[/\\S]*"), ScamCategory.SUSPICIOUS_LINK, 0.70f, "IP-based URL detected"),
        PatternRule(Regex("(?i)https?://[a-z0-9-]+\\.(tk|ml|ga|cf|gq|xyz|top|buzz|club|icu|work|info|rest|click|link|online|site|fun)/\\S*"), ScamCategory.SUSPICIOUS_LINK, 0.65f, "Suspicious TLD link"),
        PatternRule(Regex("(?i)https?://[a-z0-9-]*?(paypal|amazon|apple|google|microsoft|netflix|bank)[a-z0-9-]*?\\.(com|org|net|info)/\\S*"), ScamCategory.PHISHING, 0.75f, "Brand-impersonation domain"),

        // Advance-fee fraud
        PatternRule(Regex("(?i)(processing|handling|shipping|transfer|insurance)\\s+(fee|charge|cost).{0,30}(\\$|Rs|₹|€|£)\\d"), ScamCategory.ADVANCE_FEE, 0.80f, "Advance fee request"),
        PatternRule(Regex("(?i)(prince|diplomat|barrister|attorney|officer).{0,40}(inheritance|fund|transfer|estate|million|billion)"), ScamCategory.ADVANCE_FEE, 0.85f, "Nigerian prince / inheritance scam"),
        PatternRule(Regex("(?i)(unclaimed|abandoned)\\s+(fund|money|inheritance|estate|account).{0,30}(contact|reply|call|claim)"), ScamCategory.ADVANCE_FEE, 0.82f, "Unclaimed funds scam"),

        // Tech support scam
        PatternRule(Regex("(?i)(virus|malware|trojan|hacked|infected).{0,30}(detected|found|your).{0,30}(call|contact|click)"), ScamCategory.TECH_SUPPORT, 0.78f, "Tech support scam"),
        PatternRule(Regex("(?i)(microsoft|apple|google|windows)\\s+(support|tech|security).{0,30}(call|contact|reach)\\s*(us|now|at)?\\s*\\d"), ScamCategory.TECH_SUPPORT, 0.80f, "Fake tech support"),

        // Investment / crypto scam
        PatternRule(Regex("(?i)(guaranteed|risk.?free|100%).{0,20}(return|profit|income|roi)"), ScamCategory.INVESTMENT_SCAM, 0.79f, "Guaranteed returns scam"),
        PatternRule(Regex("(?i)(bitcoin|crypto|btc|eth|forex).{0,30}(invest|trade|profit|earn|double|triple)"), ScamCategory.CRYPTO_SCAM, 0.76f, "Crypto investment scam"),
        PatternRule(Regex("(?i)(double|triple|10x)\\s+your\\s+(money|investment|bitcoin|crypto)"), ScamCategory.CRYPTO_SCAM, 0.82f, "Money multiplication scam"),
        PatternRule(Regex("(?i)(whatsapp|telegram|signal).{0,20}(group|channel).{0,30}(profit|trading|invest|signal)"), ScamCategory.INVESTMENT_SCAM, 0.74f, "Trading group scam"),

        // Subscription scam
        PatternRule(Regex("(?i)(subscription|membership|plan)\\s+(renew|charg|debit|payment).{0,30}(\\$|Rs|₹|€|£)\\d"), ScamCategory.SUBSCRIPTION_SCAM, 0.72f, "Fake subscription charge"),
        PatternRule(Regex("(?i)(auto-?renew|auto-?debit|recurring\\s+charge).{0,30}(cancel|stop|click|call)"), ScamCategory.SUBSCRIPTION_SCAM, 0.70f, "Subscription renewal scam"),

        // Romance scam signals
        PatternRule(Regex("(?i)(send|wire|transfer)\\s+(money|funds|gift\\s+card|bitcoin).{0,30}(emergency|hospital|stuck|stranded|arrest)"), ScamCategory.ROMANCE_SCAM, 0.80f, "Emergency money request"),
    )

    // ── Analysis ─────────────────────────────────────────────────────────

    fun analyze(body: String): ScamAnalysis {
        if (body.length < 10) return ScamAnalysis(false, 0f, null, null)

        val signals = mutableListOf<String>()
        var combinedScore = 0f
        var bestCategory: ScamCategory? = null
        var bestReason: String? = null
        var bestRuleScore = 0f

        // 1. Rule-based pattern matching
        for (rule in rules) {
            if (rule.regex.containsMatchIn(body)) {
                signals.add(rule.description)
                if (rule.baseScore > bestRuleScore) {
                    bestRuleScore = rule.baseScore
                    bestCategory = rule.category
                    bestReason = rule.description
                }
            }
        }
        combinedScore = bestRuleScore

        // 2. Text heuristics
        val heuristicScore = computeHeuristics(body, signals)
        combinedScore = maxOf(combinedScore, combinedScore + heuristicScore * 0.15f)

        // 3. Learned keyword boost (from cache)
        val keywordScore = computeLearnedKeywordScore(body)
        if (keywordScore > 0.1f) {
            combinedScore = maxOf(combinedScore, combinedScore + keywordScore * 0.2f)
            if (keywordScore > 0.5f) {
                signals.add("Learned spam keywords detected")
                if (bestCategory == null) {
                    bestCategory = ScamCategory.LEARNED_SPAM
                    bestReason = "Matches learned spam patterns"
                }
            }
        }

        combinedScore = combinedScore.coerceIn(0f, 0.98f)
        val isScam = combinedScore >= 0.55f

        return ScamAnalysis(
            isScam = isScam,
            confidence = combinedScore,
            reason = if (isScam) bestReason else null,
            category = if (isScam) bestCategory else null,
            signals = if (isScam) signals else emptyList()
        )
    }

    /**
     * Extended analysis that also considers sender reputation from the DB.
     * Call this from a coroutine context (e.g. ViewModel).
     */
    suspend fun analyzeWithReputation(
        body: String,
        address: String,
        isKnownContact: Boolean = false
    ): ScamAnalysis {
        ensureCacheLoaded()
        val base = analyze(body)
        var adjusted = base.confidence

        // Unknown sender scrutiny: boost score for numbers not in contacts
        if (!isKnownContact) {
            adjusted = (adjusted + 0.10f).coerceAtMost(0.98f)
        }

        val rep = senderReputationCache[address]
        if (rep != null) {
            val total = rep.spamCount + rep.hamCount
            if (total > 0) {
                val spamRatio = rep.spamCount.toFloat() / total
                adjusted = when {
                    spamRatio > 0.7f -> {
                        (adjusted + 0.15f).coerceAtMost(0.98f)
                    }
                    spamRatio < 0.2f && rep.hamCount >= 3 -> {
                        (adjusted - 0.20f).coerceAtLeast(0f)
                    }
                    else -> adjusted
                }
            }
        }

        val isScam = adjusted >= 0.55f
        return base.copy(
            isScam = isScam,
            confidence = adjusted,
            reason = if (isScam) base.reason else null,
            category = if (isScam) base.category else null,
            signals = if (isScam) base.signals else emptyList()
        )
    }

    suspend fun getSenderReputation(address: String): SpamSenderReputationEntity? {
        ensureCacheLoaded()
        return senderReputationCache[address]
    }

    // ── Learning / feedback ──────────────────────────────────────────────

    /**
     * Record that the user confirmed a message is spam.
     */
    suspend fun reportSpam(address: String, body: String, category: ScamCategory?) {
        mutex.withLock {
            learningDao.insertLearningEntry(
                SpamLearningEntity(
                    address = address,
                    body = body,
                    isSpam = true,
                    detectedCategory = category?.name,
                    userFeedback = "CONFIRMED_SPAM",
                    confidence = 1f
                )
            )
            updateSenderReputation(address, isSpam = true)
            updateKeywordWeights(body, isSpam = true)
        }
    }

    /**
     * Record that the user dismissed the scam warning (false positive).
     */
    suspend fun reportNotSpam(address: String, body: String) {
        mutex.withLock {
            learningDao.insertLearningEntry(
                SpamLearningEntity(
                    address = address,
                    body = body,
                    isSpam = false,
                    detectedCategory = null,
                    userFeedback = "DISMISSED_WARNING",
                    confidence = 0f
                )
            )
            updateSenderReputation(address, isSpam = false)
            updateKeywordWeights(body, isSpam = false)
        }
    }

    /**
     * Returns simple stats about the learning model for display in settings.
     */
    suspend fun getLearningStats(): SpamAgentStats {
        val spamCount = learningDao.getSpamTrainingCount()
        val hamCount = learningDao.getHamTrainingCount()
        val topKeywords = learningDao.getSpamKeywords().take(10).map { it.keyword }
        val knownSpammers = learningDao.getKnownSpamSenders().size
        return SpamAgentStats(
            totalSpamSamples = spamCount,
            totalHamSamples = hamCount,
            knownSpamSenders = knownSpammers,
            topSpamKeywords = topKeywords
        )
    }

    // ── Internals ────────────────────────────────────────────────────────

    private fun computeHeuristics(body: String, signals: MutableList<String>): Float {
        var score = 0f
        val words = body.split("\\s+".toRegex())

        // Excessive exclamation marks
        val exclamationCount = body.count { it == '!' }
        if (exclamationCount > 3) {
            score += 0.1f
            signals.add("Excessive exclamation marks ($exclamationCount)")
        }

        // ALL CAPS words ratio
        val capsWords = words.count { it.length > 3 && it == it.uppercase() && it.any { c -> c.isLetter() } }
        val capsRatio = if (words.isNotEmpty()) capsWords.toFloat() / words.size else 0f
        if (capsRatio > 0.3f) {
            score += 0.12f
            signals.add("High ALL-CAPS ratio (${(capsRatio * 100).toInt()}%)")
        }

        // Currency symbols + amounts
        val currencyPattern = Regex("[\\$€£₹]\\s*[\\d,]+\\.?\\d*|\\d+[\\$€£₹]")
        val currencyMatches = currencyPattern.findAll(body).count()
        if (currencyMatches >= 2) {
            score += 0.08f
            signals.add("Multiple currency amounts ($currencyMatches)")
        }

        // Multiple URLs
        val urlCount = Regex("https?://\\S+").findAll(body).count()
        if (urlCount >= 2) {
            score += 0.05f
            signals.add("Multiple URLs ($urlCount)")
        }

        // Unusual character entropy — obfuscation detection
        val specialCharRatio = body.count { !it.isLetterOrDigit() && !it.isWhitespace() }.toFloat() / body.length.coerceAtLeast(1)
        if (specialCharRatio > 0.15f) {
            score += 0.06f
            signals.add("High special character ratio")
        }

        // Unicode look-alikes / zero-width chars (common in phishing)
        val suspiciousUnicode = body.any { it.code in 0x200B..0x200F || it.code in 0xFE00..0xFE0F || it.code in 0x0400..0x04FF }
        if (suspiciousUnicode && body.any { it.isLetter() }) {
            score += 0.08f
            signals.add("Suspicious Unicode characters detected")
        }

        // Urgency words density
        val urgencyWords = listOf("urgent", "immediately", "now", "fast", "hurry", "asap", "quickly", "limited", "expire", "deadline", "today only")
        val urgencyCount = urgencyWords.count { body.contains(it, ignoreCase = true) }
        if (urgencyCount >= 3) {
            score += 0.10f
            signals.add("High urgency word density ($urgencyCount)")
        }

        // Personal info requests
        val infoRequestWords = listOf("ssn", "social security", "credit card", "bank account", "routing number", "date of birth", "mother's maiden")
        val infoRequests = infoRequestWords.count { body.contains(it, ignoreCase = true) }
        if (infoRequests > 0) {
            score += 0.15f
            signals.add("Requests sensitive personal information")
        }

        return score.coerceAtMost(0.5f)
    }

    private fun computeLearnedKeywordScore(body: String): Float {
        if (keywordWeightCache.isEmpty()) return 0f
        val bodyLower = body.lowercase()
        val tokens = bodyLower.split("\\s+".toRegex()).filter { it.length >= 3 }
        var totalWeight = 0f
        var matchCount = 0
        for (token in tokens) {
            val w = keywordWeightCache[token]
            if (w != null && w > 0.3f) {
                totalWeight += w
                matchCount++
            }
        }
        return if (matchCount > 0) (totalWeight / matchCount).coerceAtMost(1f) else 0f
    }

    private suspend fun updateSenderReputation(address: String, isSpam: Boolean) {
        val existing = learningDao.getSenderReputation(address)
            ?: SpamSenderReputationEntity(address = address)
        val updated = if (isSpam) {
            existing.copy(spamCount = existing.spamCount + 1, lastSpamTimestamp = System.currentTimeMillis())
        } else {
            existing.copy(hamCount = existing.hamCount + 1, lastHamTimestamp = System.currentTimeMillis())
        }
        learningDao.upsertSenderReputation(updated)
        senderReputationCache[address] = updated
    }

    private suspend fun updateKeywordWeights(body: String, isSpam: Boolean) {
        val tokens = body.lowercase()
            .split("\\s+".toRegex())
            .filter { it.length >= 3 }
            .map { it.replace(Regex("[^a-z0-9]"), "") }
            .filter { it.isNotBlank() }
            .distinct()

        for (token in tokens) {
            val existing = learningDao.getKeywordWeight(token)
                ?: SpamKeywordWeightEntity(keyword = token)
            val updated = if (isSpam) {
                existing.copy(spamOccurrences = existing.spamOccurrences + 1)
            } else {
                existing.copy(hamOccurrences = existing.hamOccurrences + 1)
            }
            val total = updated.spamOccurrences + updated.hamOccurrences
            val weight = if (total > 0) updated.spamOccurrences.toFloat() / total else 0f
            val final_ = updated.copy(weight = weight)
            learningDao.upsertKeywordWeight(final_)
            keywordWeightCache[token] = weight
        }
    }

    private suspend fun ensureCacheLoaded() {
        if (cacheInitialized) return
        mutex.withLock {
            if (cacheInitialized) return
            try {
                val senders = learningDao.getKnownSpamSenders()
                senders.forEach { senderReputationCache[it.address] = it }
                val keywords = learningDao.getAllKeywordWeights()
                keywords.forEach { keywordWeightCache[it.keyword] = it.weight }
                cacheInitialized = true
            } catch (e: Exception) {
                Log.w("ScamDetector", "Failed to load learning cache", e)
            }
        }
    }

    suspend fun refreshCache() {
        cacheInitialized = false
        ensureCacheLoaded()
    }
}

data class SpamAgentStats(
    val totalSpamSamples: Int,
    val totalHamSamples: Int,
    val knownSpamSenders: Int,
    val topSpamKeywords: List<String>
)
