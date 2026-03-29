package com.novachat.core.sms

import android.util.Log
import com.novachat.core.database.dao.SpamLearningDao
import com.novachat.core.database.dao.SpamMessageDao
import com.novachat.core.database.entity.SpamKeywordWeightEntity
import com.novachat.core.database.entity.SpamLearningEntity
import com.novachat.core.database.entity.SpamSenderReputationEntity
import com.novachat.core.sms.hebrew.HebrewTextNormalizer
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
    LEARNED_SPAM,
    // Israel-specific
    TAX_REFUND,
    PENSION_SEVERANCE,
    POLITICAL_SPAM,
    MONEY_WAITING,
    MEDICAL_DISABILITY,
    PROPAGANDA,
    INSURANCE_SPAM,
    GAMBLING_SPAM,
    HEBREW_LOTTERY,
    DEBT_COLLECTION_FRAUD,
    HEALTH_DIET_SCAM,
    REAL_ESTATE_SPAM,
    CAR_ACCIDENT_CLAIM,
    CHARITY_SCAM,
    UTILITY_FRAUD,
    CARRIER_SCAM,
    LEGAL_SCAM,
    COMMERCIAL_SPAM,
    CHAIN_MESSAGE,
    SERVICE_IMPERSONATION
}

/** Allows checking if a sender is allowlisted (e.g. by ScamDetector). Used by HebrewSpamEngine. */
interface AllowlistChecker {
    suspend fun isAllowlisted(address: String): Boolean
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
    private val learningDao: SpamLearningDao,
    private val spamMessageDao: SpamMessageDao
) : AllowlistChecker {
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
        // ── Phishing ─────────────────────────────────────────────────────
        PatternRule(Regex("(?i)your\\s+account\\s+(has been|is|was)\\s+(suspend|block|lock|compromis|restrict|deactivat|limit)"), ScamCategory.PHISHING, 0.82f, "Account suspension phishing"),
        PatternRule(Regex("(?i)verify\\s+your\\s+(identity|account|bank|card|details|information)\\s*(immediately|now|urgently|within)"), ScamCategory.PHISHING, 0.84f, "Identity verification phishing"),
        PatternRule(Regex("(?i)click\\s+(here|below|this|the link)\\s+to\\s+(verify|confirm|unlock|restore|update|secure)"), ScamCategory.PHISHING, 0.80f, "Click-to-verify phishing"),
        PatternRule(Regex("(?i)we\\s+(detected|noticed|found)\\s+(unusual|suspicious|unauthorized|unknown)\\s+(activity|access|login|transaction|sign.?in)"), ScamCategory.PHISHING, 0.83f, "Suspicious activity phishing"),
        PatternRule(Regex("(?i)(update|confirm)\\s+your\\s+(payment|billing|bank)\\s+(details|info|method|information)"), ScamCategory.PHISHING, 0.78f, "Payment update phishing"),
        PatternRule(Regex("(?i)your\\s+(password|credentials)\\s+(has|have)\\s+(expired|been\\s+(changed|reset|compromised))"), ScamCategory.PHISHING, 0.81f, "Password compromise phishing"),
        PatternRule(Regex("(?i)(apple|google|microsoft|amazon|netflix|paypal|bank).{0,20}(suspend|verify|confirm|unauthorized|security\\s+alert)"), ScamCategory.PHISHING, 0.79f, "Brand impersonation phishing"),
        // Hebrew phishing — expanded bank/credit card coverage
        PatternRule(Regex("לקוח\\s*יקר"), ScamCategory.PHISHING, 0.80f, "Hebrew: dear customer"),
        PatternRule(Regex("עדכון\\s*פרטים"), ScamCategory.PHISHING, 0.82f, "Hebrew: update details"),
        PatternRule(Regex("קוד\\s*אימות"), ScamCategory.PHISHING, 0.84f, "Hebrew: authentication code"),
        PatternRule(Regex("חשבון.*יחסמ|חשבונכ.*יחסמ"), ScamCategory.PHISHING, 0.84f, "Hebrew: account will be blocked"),
        PatternRule(Regex("זוהתה\\s*פעילות\\s*חריגה"), ScamCategory.PHISHING, 0.83f, "Hebrew: unusual activity detected"),
        PatternRule(Regex("בנק.*(יהב|מסד|ירושלים).*נחסמ"), ScamCategory.PHISHING, 0.82f, "Hebrew: Yahav/Massad/Jerusalem bank blocked"),
        PatternRule(Regex("(פפר|one\\s*zero|וואן\\s*זירו).*חשבונ"), ScamCategory.PHISHING, 0.80f, "Hebrew: Pepper/OneZero account"),
        PatternRule(Regex("(כאל|ישראכרט|מקס|ויזה\\s*כאל|לאומי\\s*קארד).*נחסמ"), ScamCategory.PHISHING, 0.82f, "Hebrew: credit card company blocked"),
        PatternRule(Regex("(כאל|ישראכרט|מקס|ויזה\\s*כאל).*פעילות\\s*חריגה"), ScamCategory.PHISHING, 0.82f, "Hebrew: credit card unusual activity"),
        PatternRule(Regex("כרטיס\\s*אשראי.*חסומ|כרטיס.*אשראי.*נחסמ"), ScamCategory.PHISHING, 0.83f, "Hebrew: credit card blocked"),
        PatternRule(Regex("העברה\\s*חשודה"), ScamCategory.PHISHING, 0.82f, "Hebrew: suspicious transfer"),
        PatternRule(Regex("אבטחת\\s*חשבונ"), ScamCategory.PHISHING, 0.78f, "Hebrew: account security"),
        PatternRule(Regex("אימות\\s*זהות.*לחצ"), ScamCategory.PHISHING, 0.82f, "Hebrew: identity verification click"),
        PatternRule(Regex("פרטי\\s*חשבונ.*עדכנ|עדכנ.*פרטי\\s*חשבונ"), ScamCategory.PHISHING, 0.80f, "Hebrew: update account details"),

        // ── OTP fraud ────────────────────────────────────────────────────
        PatternRule(Regex("(?i)(share|send|tell|give|forward)\\s+(me|us)\\s+(the|your)\\s+(otp|code|pin|password|verification)"), ScamCategory.OTP_FRAUD, 0.92f, "OTP sharing request"),
        PatternRule(Regex("(?i)i\\s+(accidentally|mistakenly|wrongly)\\s+sent\\s+(a|an|my|the)\\s+(otp|code|verification)"), ScamCategory.OTP_FRAUD, 0.90f, "Accidental OTP scam"),
        PatternRule(Regex("(?i)please\\s+(send|share|give).{0,20}(code|otp|pin|verification).{0,20}(sent|received|got)"), ScamCategory.OTP_FRAUD, 0.88f, "OTP relay scam"),
        PatternRule(Regex("שלח.*(לי\\s+)?את\\s*הקוד|תשלח.*הקוד"), ScamCategory.OTP_FRAUD, 0.90f, "Hebrew: send me the code"),
        PatternRule(Regex("קוד\\s*בנ\\s*6\\s*ספרות"), ScamCategory.OTP_FRAUD, 0.88f, "Hebrew: 6-digit code"),
        PatternRule(Regex("שלחתי.*קוד.*בטעות|בטעות.*שלחתי.*קוד"), ScamCategory.OTP_FRAUD, 0.90f, "Hebrew: I sent code by mistake"),
        PatternRule(Regex("קוד\\s*זמני|הזנ\\s*את\\s*הקוד"), ScamCategory.OTP_FRAUD, 0.86f, "Hebrew: temporary code / enter code"),
        PatternRule(Regex("קוד\\s*חד\\s*פעמי"), ScamCategory.OTP_FRAUD, 0.86f, "Hebrew: one-time code"),
        PatternRule(Regex("סיסמה\\s*זמנית"), ScamCategory.OTP_FRAUD, 0.84f, "Hebrew: temporary password"),
        PatternRule(Regex("אימות\\s*דו\\s*שלבי"), ScamCategory.OTP_FRAUD, 0.82f, "Hebrew: two-step verification"),
        PatternRule(Regex("קוד\\s*הגיע.*בטעות"), ScamCategory.OTP_FRAUD, 0.88f, "Hebrew: code arrived by mistake"),

        // ── Prize / lottery ──────────────────────────────────────────────
        PatternRule(Regex("(?i)(congratulations|congrats)!?\\s+you('ve|\\s+have)\\s+(won|been\\s+selected|been\\s+chosen|qualified)"), ScamCategory.PRIZE_SCAM, 0.87f, "Prize notification scam"),
        PatternRule(Regex("(?i)you\\s+(won|have\\s+won|are\\s+the\\s+winner)\\s+\\$?[\\d,]+"), ScamCategory.PRIZE_SCAM, 0.88f, "Lottery winning scam"),
        PatternRule(Regex("(?i)claim\\s+your\\s+(prize|reward|gift|winnings|cash|bonus|free)"), ScamCategory.PRIZE_SCAM, 0.85f, "Prize claim scam"),
        PatternRule(Regex("(?i)free\\s+(iphone|ipad|gift\\s*card|cash|money|reward|voucher|samsung|macbook)"), ScamCategory.PRIZE_SCAM, 0.83f, "Free device scam"),
        PatternRule(Regex("(?i)your\\s+(number|email|phone)\\s+(has been|was)\\s+selected\\s+(for|to|as)"), ScamCategory.PRIZE_SCAM, 0.84f, "Selection notification scam"),
        PatternRule(Regex("זכית\\s*בפרס|זכית\\s*ב"), ScamCategory.PRIZE_SCAM, 0.86f, "Hebrew: you won a prize"),
        PatternRule(Regex("הגרלה\\s*בלעדית"), ScamCategory.PRIZE_SCAM, 0.84f, "Hebrew: exclusive raffle"),
        PatternRule(Regex("קופונ\\s*מתנה|שובר\\s*מתנה"), ScamCategory.PRIZE_SCAM, 0.80f, "Hebrew: gift coupon/voucher"),
        PatternRule(Regex("פרס\\s*כספי"), ScamCategory.PRIZE_SCAM, 0.82f, "Hebrew: cash prize"),
        PatternRule(Regex("מזל\\s*טוב.*נבחרת"), ScamCategory.PRIZE_SCAM, 0.84f, "Hebrew: congratulations selected"),

        // ── Job scam ─────────────────────────────────────────────────────
        PatternRule(Regex("(?i)(work\\s+from\\s+home|earn)\\s+\\$?\\d{3,}\\s*(per|a|/|every)\\s*(day|hour|week)"), ScamCategory.JOB_SCAM, 0.78f, "Work-from-home scam"),
        PatternRule(Regex("(?i)hiring\\s+(now|immediately|urgently).{0,30}(no\\s+experience|apply\\s+now|no\\s+interview)"), ScamCategory.JOB_SCAM, 0.76f, "Urgent hiring scam"),
        PatternRule(Regex("(?i)make\\s+\\$?\\d{3,}\\s*(daily|weekly|monthly)\\s*(from|at)\\s*(home|anywhere)"), ScamCategory.JOB_SCAM, 0.77f, "Income promise scam"),
        PatternRule(Regex("(?i)(easy|simple)\\s+(money|income|cash|job).{0,30}(home|online|daily|weekly)"), ScamCategory.JOB_SCAM, 0.72f, "Easy money scam"),
        PatternRule(Regex("(?i)data\\s+entry.{0,20}\\$?\\d{2,}\\s*(per|/|an?)\\s*(hour|task|page)"), ScamCategory.JOB_SCAM, 0.70f, "Data entry scam"),
        PatternRule(Regex("עבודה\\s*מהבית"), ScamCategory.JOB_SCAM, 0.76f, "Hebrew: work from home"),
        PatternRule(Regex("הכנסה\\s*נוספת.*ללא\\s*ניסיונ|ללא\\s*ניסיונ.*הכנסה"), ScamCategory.JOB_SCAM, 0.78f, "Hebrew: extra income no experience"),
        PatternRule(Regex("משרה\\s*דחופה"), ScamCategory.JOB_SCAM, 0.74f, "Hebrew: urgent position"),
        PatternRule(Regex("שכר\\s*גבוה.*מהבית|מהבית.*שכר\\s*גבוה"), ScamCategory.JOB_SCAM, 0.76f, "Hebrew: high salary from home"),
        PatternRule(Regex("הרוויחו\\s*מהבית"), ScamCategory.JOB_SCAM, 0.76f, "Hebrew: earn from home"),
        PatternRule(Regex("עבודה\\s*קלה.*שקלימ|שקלימ.*ביומ"), ScamCategory.JOB_SCAM, 0.74f, "Hebrew: easy work shekels per day"),

        // ── Loan scam ────────────────────────────────────────────────────
        PatternRule(Regex("(?i)(pre-?approved|instant|guaranteed|emergency)\\s+(loan|credit|cash|fund)\\s*(of\\s+)?(\\$|Rs\\.?|₹|€|£)?\\d"), ScamCategory.LOAN_SCAM, 0.78f, "Pre-approved loan scam"),
        PatternRule(Regex("(?i)loan\\s+(approved|disburs|sanction).{0,30}(click|tap|link|call|download)"), ScamCategory.LOAN_SCAM, 0.77f, "Loan approval scam"),
        PatternRule(Regex("(?i)(low|zero|no)\\s+(interest|processing\\s+fee).{0,20}(loan|credit|cash)"), ScamCategory.LOAN_SCAM, 0.73f, "Low interest loan scam"),
        PatternRule(Regex("(?i)(bad\\s+credit|no\\s+credit\\s+check).{0,30}(guaranteed|approved|instant)"), ScamCategory.LOAN_SCAM, 0.75f, "Bad credit loan scam"),
        PatternRule(Regex("הלוואה\\s*מיידית|קח\\s*הלוואה|הלוואה\\s*היומ"), ScamCategory.LOAN_SCAM, 0.78f, "Hebrew: instant loan / take loan today"),
        PatternRule(Regex("זכאות\\s*להלוואה|כנסו\\s*מהר"), ScamCategory.LOAN_SCAM, 0.78f, "Hebrew: loan eligibility / enter quickly"),
        PatternRule(Regex("בתנאימ\\s*מיוחדימ|הלוואה.*אושרה|מאושר.*הלוואה"), ScamCategory.LOAN_SCAM, 0.78f, "Hebrew: special conditions / loan approved"),
        PatternRule(Regex("הלוואה\\s*מיידית.*\\d|עד\\s*\\d+.*ש[\"״]?ח"), ScamCategory.LOAN_SCAM, 0.78f, "Hebrew: loan up to X shekels"),
        PatternRule(Regex("משכנתא.*ריבית|ריבית.*משכנתא"), ScamCategory.LOAN_SCAM, 0.76f, "Hebrew: mortgage interest"),
        PatternRule(Regex("ריבית\\s*אפס|ריבית\\s*0%"), ScamCategory.LOAN_SCAM, 0.78f, "Hebrew: zero interest"),
        PatternRule(Regex("ללא\\s*ערבימ"), ScamCategory.LOAN_SCAM, 0.76f, "Hebrew: no guarantors"),
        PatternRule(Regex("הלוואה\\s*חוצ\\s*בנקאית"), ScamCategory.LOAN_SCAM, 0.78f, "Hebrew: outside bank loan"),
        PatternRule(Regex("ללא\\s*בדיקת\\s*BDI|ללא\\s*בדיקת\\s*בי\\s*די\\s*אי"), ScamCategory.LOAN_SCAM, 0.80f, "Hebrew: no BDI check"),
        PatternRule(Regex("מימונ.*מיידי|אישור.*מימונ"), ScamCategory.LOAN_SCAM, 0.76f, "Hebrew: immediate financing"),

        // ── Delivery scam ────────────────────────────────────────────────
        PatternRule(Regex("(?i)your\\s+(package|shipment|delivery|parcel)\\s+(couldn't|could\\s+not|failed|has\\s+been|was).{0,40}(click|tap|link|track)"), ScamCategory.DELIVERY_SCAM, 0.76f, "Delivery notification scam"),
        PatternRule(Regex("החבילה.*ממתינה|חבילה.*למשלוח|איסופ\\s*חבילה|משלוח\\s*ממתינ"), ScamCategory.DELIVERY_SCAM, 0.76f, "Hebrew: package waiting / pickup / delivery"),
        PatternRule(Regex("דואר\\s*ישראל.*חבילה"), ScamCategory.DELIVERY_SCAM, 0.78f, "Hebrew: Israel Post package"),
        PatternRule(Regex("(?i)(FedEx|Amazon|AliExpress|Shein|Temu|iHerb|eBay).*חבילה"), ScamCategory.DELIVERY_SCAM, 0.76f, "Hebrew: intl delivery service package"),
        PatternRule(Regex("(?i)חבילה.*(FedEx|Amazon|AliExpress|Shein|Temu|iHerb|eBay)"), ScamCategory.DELIVERY_SCAM, 0.76f, "Hebrew: package from intl service"),
        PatternRule(Regex("מספר\\s*מעקב.*חבילה|חבילה.*מספר\\s*מעקב"), ScamCategory.DELIVERY_SCAM, 0.74f, "Hebrew: tracking number package"),
        PatternRule(Regex("אישור\\s*מכס|תשלומ\\s*מכס"), ScamCategory.DELIVERY_SCAM, 0.78f, "Hebrew: customs approval/payment"),
        PatternRule(Regex("תשלומ\\s*משלוח"), ScamCategory.DELIVERY_SCAM, 0.76f, "Hebrew: shipping payment"),
        PatternRule(Regex("שחרור\\s*חבילה"), ScamCategory.DELIVERY_SCAM, 0.76f, "Hebrew: package release"),
        PatternRule(Regex("חבילתכ\\s*בדרכ|ההזמנה\\s*שלכ\\s*בדרכ"), ScamCategory.DELIVERY_SCAM, 0.72f, "Hebrew: your package is on its way"),

        // ── Urgency tactics ──────────────────────────────────────────────
        PatternRule(Regex("(?i)(act\\s+now|respond\\s+immediately|urgent|expires?\\s+(today|soon|in\\s+\\d+\\s*(hour|min|second)))"), ScamCategory.URGENT_ACTION, 0.68f, "Urgency pressure tactic"),
        PatternRule(Regex("(?i)final\\s+(notice|warning|reminder|attempt).{0,30}(account|payment|action|respond)"), ScamCategory.URGENT_ACTION, 0.72f, "Final notice pressure"),
        PatternRule(Regex("(?i)failure\\s+to\\s+(respond|act|verify|comply).{0,30}(result|lead|cause).{0,20}(suspension|closure|legal|arrest)"), ScamCategory.URGENT_ACTION, 0.78f, "Consequence threat"),
        PatternRule(Regex("הזדמנות\\s*אחרונה"), ScamCategory.URGENT_ACTION, 0.72f, "Hebrew: last opportunity"),
        PatternRule(Regex("רק\\s*היומ"), ScamCategory.URGENT_ACTION, 0.68f, "Hebrew: today only"),
        PatternRule(Regex("נותרו\\s*שעות"), ScamCategory.URGENT_ACTION, 0.72f, "Hebrew: hours remaining"),
        PatternRule(Regex("מוגבל\\s*בזמנ"), ScamCategory.URGENT_ACTION, 0.70f, "Hebrew: limited time"),
        PatternRule(Regex("לפני\\s*שיגמר"), ScamCategory.URGENT_ACTION, 0.68f, "Hebrew: before it ends"),
        PatternRule(Regex("עד\\s*חצות"), ScamCategory.URGENT_ACTION, 0.68f, "Hebrew: until midnight"),

        // ── Tax scam ─────────────────────────────────────────────────────
        PatternRule(Regex("(?i)(irs|tax|government|police|court|doj|fbi)\\s+.{0,30}(warrant|arrest|legal\\s+action|fine|penalty|investigation)"), ScamCategory.TAX_SCAM, 0.82f, "Government impersonation scam"),

        // ── Suspicious links ─────────────────────────────────────────────
        PatternRule(Regex("(?i)(bit\\.ly|tinyurl|t\\.co|goo\\.gl|rb\\.gy|shorturl|cutt\\.ly|is\\.gd|v\\.gd|qr\\.ae|clck\\.ru)/\\S+"), ScamCategory.SUSPICIOUS_LINK, 0.62f, "Shortened URL detected"),
        PatternRule(Regex("(?i)https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}[/\\S]*"), ScamCategory.SUSPICIOUS_LINK, 0.70f, "IP-based URL detected"),
        PatternRule(Regex("(?i)https?://[a-z0-9-]+\\.(tk|ml|ga|cf|gq|xyz|top|buzz|club|icu|work|info|rest|click|link|online|site|fun)/\\S*"), ScamCategory.SUSPICIOUS_LINK, 0.65f, "Suspicious TLD link"),
        PatternRule(Regex("(?i)https?://[a-z0-9-]*?(paypal|amazon|apple|google|microsoft|netflix|bank)[a-z0-9-]*?\\.(com|org|net|info)/\\S*"), ScamCategory.PHISHING, 0.75f, "Brand-impersonation domain"),

        // ── Advance-fee fraud ────────────────────────────────────────────
        PatternRule(Regex("(?i)(processing|handling|shipping|transfer|insurance)\\s+(fee|charge|cost).{0,30}(\\$|Rs|₹|€|£)\\d"), ScamCategory.ADVANCE_FEE, 0.80f, "Advance fee request"),
        PatternRule(Regex("(?i)(prince|diplomat|barrister|attorney|officer).{0,40}(inheritance|fund|transfer|estate|million|billion)"), ScamCategory.ADVANCE_FEE, 0.85f, "Nigerian prince / inheritance scam"),
        PatternRule(Regex("(?i)(unclaimed|abandoned)\\s+(fund|money|inheritance|estate|account).{0,30}(contact|reply|call|claim)"), ScamCategory.ADVANCE_FEE, 0.82f, "Unclaimed funds scam"),

        // ── Tech support scam ────────────────────────────────────────────
        PatternRule(Regex("(?i)(virus|malware|trojan|hacked|infected).{0,30}(detected|found|your).{0,30}(call|contact|click)"), ScamCategory.TECH_SUPPORT, 0.78f, "Tech support scam"),
        PatternRule(Regex("(?i)(microsoft|apple|google|windows)\\s+(support|tech|security).{0,30}(call|contact|reach)\\s*(us|now|at)?\\s*\\d"), ScamCategory.TECH_SUPPORT, 0.80f, "Fake tech support"),

        // ── Investment / crypto scam ─────────────────────────────────────
        PatternRule(Regex("(?i)(guaranteed|risk.?free|100%).{0,20}(return|profit|income|roi)"), ScamCategory.INVESTMENT_SCAM, 0.79f, "Guaranteed returns scam"),
        PatternRule(Regex("(?i)(bitcoin|crypto|btc|eth|forex).{0,30}(invest|trade|profit|earn|double|triple)"), ScamCategory.CRYPTO_SCAM, 0.76f, "Crypto investment scam"),
        PatternRule(Regex("(?i)(double|triple|10x)\\s+your\\s+(money|investment|bitcoin|crypto)"), ScamCategory.CRYPTO_SCAM, 0.82f, "Money multiplication scam"),
        PatternRule(Regex("(?i)(whatsapp|telegram|signal).{0,20}(group|channel).{0,30}(profit|trading|invest|signal)"), ScamCategory.INVESTMENT_SCAM, 0.74f, "Trading group scam"),
        PatternRule(Regex("אתריומ|NFT.*השקעה|השקעה.*NFT"), ScamCategory.CRYPTO_SCAM, 0.76f, "Hebrew: Ethereum/NFT investment"),
        PatternRule(Regex("הכנסה\\s*פסיבית"), ScamCategory.CRYPTO_SCAM, 0.74f, "Hebrew: passive income"),
        PatternRule(Regex("(?i)טריידינג|פורקס|FOREX"), ScamCategory.CRYPTO_SCAM, 0.76f, "Hebrew: trading/forex"),
        PatternRule(Regex("תשואה\\s*מובטחת"), ScamCategory.INVESTMENT_SCAM, 0.80f, "Hebrew: guaranteed returns"),
        PatternRule(Regex("סיגנלימ.*מסחר|מסחר.*סיגנלימ"), ScamCategory.INVESTMENT_SCAM, 0.74f, "Hebrew: trading signals"),
        PatternRule(Regex("בורסה.*רווח|רווח.*בורסה"), ScamCategory.INVESTMENT_SCAM, 0.72f, "Hebrew: stock market profit"),

        // ── Subscription scam ────────────────────────────────────────────
        PatternRule(Regex("(?i)(subscription|membership|plan)\\s+(renew|charg|debit|payment).{0,30}(\\$|Rs|₹|€|£)\\d"), ScamCategory.SUBSCRIPTION_SCAM, 0.72f, "Fake subscription charge"),
        PatternRule(Regex("(?i)(auto-?renew|auto-?debit|recurring\\s+charge).{0,30}(cancel|stop|click|call)"), ScamCategory.SUBSCRIPTION_SCAM, 0.70f, "Subscription renewal scam"),

        // ── Romance scam signals ─────────────────────────────────────────
        PatternRule(Regex("(?i)(send|wire|transfer)\\s+(money|funds|gift\\s+card|bitcoin).{0,30}(emergency|hospital|stuck|stranded|arrest)"), ScamCategory.ROMANCE_SCAM, 0.80f, "Emergency money request"),

        // ── Israel-specific: tax refund ──────────────────────────────────
        PatternRule(Regex("החזר\\s*מס.*בדיקה\\s*ללא\\s*תשלומ"), ScamCategory.TAX_REFUND, 0.84f, "Hebrew: tax refund free check"),
        PatternRule(Regex("ממוצע\\s*החזר(י)?\\s*מס|החזר(י)?\\s*מס\\s*ממוצע"), ScamCategory.TAX_REFUND, 0.82f, "Hebrew: average tax refunds"),
        PatternRule(Regex("מגיע\\s*לכ\\s*החזר\\s*מס"), ScamCategory.TAX_REFUND, 0.82f, "Hebrew: you're owed tax refund"),
        PatternRule(Regex("החזר(י)?\\s*מס.*[\\d,]+\\s*ש[\"״]?ח"), ScamCategory.TAX_REFUND, 0.84f, "Hebrew: tax refund with shekel amount"),
        PatternRule(Regex("החזר(י)?\\s*מס.*לאזרחי"), ScamCategory.TAX_REFUND, 0.82f, "Hebrew: tax refund for citizens"),
        PatternRule(Regex("החזר(י)?\\s*מס.*\\d{4,}"), ScamCategory.TAX_REFUND, 0.80f, "Hebrew: tax refund with large number"),
        PatternRule(Regex("בדיקה\\s*חינמ.*מס|מס.*בדיקה\\s*חינמ"), ScamCategory.TAX_REFUND, 0.80f, "Hebrew: free tax check"),
        PatternRule(Regex("זיכוי\\s*מס"), ScamCategory.TAX_REFUND, 0.78f, "Hebrew: tax credit"),
        PatternRule(Regex("דוח\\s*שנתי.*מס|מס.*דוח\\s*שנתי"), ScamCategory.TAX_REFUND, 0.76f, "Hebrew: annual tax report"),
        PatternRule(Regex("מס\\s*הכנסה.*החזר|ניכוי\\s*מס"), ScamCategory.TAX_REFUND, 0.78f, "Hebrew: income tax refund / tax deduction"),

        // ── Israel-specific: pension / severance ─────────────────────────
        PatternRule(Regex("למשוכ\\s*פנסיה|פנסיה\\s*ופיצויימ"), ScamCategory.PENSION_SEVERANCE, 0.80f, "Hebrew: withdraw pension/severance"),
        PatternRule(Regex("פיצויימ.*ללא\\s*התפטרות"), ScamCategory.PENSION_SEVERANCE, 0.82f, "Hebrew: severance without resignation"),
        PatternRule(Regex("ביטוח\\s*לאומי.*מענק"), ScamCategory.PENSION_SEVERANCE, 0.80f, "Hebrew: Bituach Leumi grant scam"),
        PatternRule(Regex("קרנ\\s*פנסיה"), ScamCategory.PENSION_SEVERANCE, 0.78f, "Hebrew: pension fund"),
        PatternRule(Regex("ביטוח\\s*מנהלימ"), ScamCategory.PENSION_SEVERANCE, 0.78f, "Hebrew: executive insurance"),
        PatternRule(Regex("קופת\\s*גמל"), ScamCategory.PENSION_SEVERANCE, 0.78f, "Hebrew: provident fund"),
        PatternRule(Regex("כספימ\\s*תקועימ"), ScamCategory.PENSION_SEVERANCE, 0.80f, "Hebrew: stuck funds"),
        PatternRule(Regex("פנסיה\\s*מוקפאת"), ScamCategory.PENSION_SEVERANCE, 0.80f, "Hebrew: frozen pension"),
        PatternRule(Regex("משיכת\\s*כספימ.*פנסיה|פנסיה.*משיכת\\s*כספימ"), ScamCategory.PENSION_SEVERANCE, 0.80f, "Hebrew: pension fund withdrawal"),
        PatternRule(Regex("זכויות\\s*פנסיוניות"), ScamCategory.PENSION_SEVERANCE, 0.78f, "Hebrew: pension rights"),

        // ── Israel-specific: political spam ──────────────────────────────
        PatternRule(Regex("הצביעו\\s*עכשיו|להצביע\\s*עכשיו"), ScamCategory.POLITICAL_SPAM, 0.78f, "Hebrew: vote now campaign"),
        PatternRule(Regex("בבחירות.*בוחרימ|בוחרימ.*בבחירות"), ScamCategory.POLITICAL_SPAM, 0.76f, "Hebrew: in elections voting for"),
        PatternRule(Regex("בחירות.*הצביעו|הצביעו.*בחירות"), ScamCategory.POLITICAL_SPAM, 0.74f, "Hebrew: elections vote"),
        PatternRule(Regex("יומ הבחירות.*בוחרימ|בוחרימ.*יומ הבחירות"), ScamCategory.POLITICAL_SPAM, 0.76f, "Hebrew: election day voting for"),
        PatternRule(Regex("רק\\s+\\S+\\s+יציל"), ScamCategory.POLITICAL_SPAM, 0.70f, "Hebrew: only X will save"),
        PatternRule(Regex("איזו\\s*ממשלה"), ScamCategory.POLITICAL_SPAM, 0.68f, "Hebrew: which government poll"),
        PatternRule(Regex("חשיפה\\s*דרמטית"), ScamCategory.POLITICAL_SPAM, 0.66f, "Hebrew: dramatic expose"),
        PatternRule(Regex("ראש\\s*האופוזיציה|יו[\"״]ר\\s*יש\\s*עתיד"), ScamCategory.POLITICAL_SPAM, 0.66f, "Hebrew: political figures"),
        PatternRule(Regex("השתמטות.*חרד|חרד.*השתמטות|גיוס.*חרד|חרד.*גיוס"), ScamCategory.POLITICAL_SPAM, 0.68f, "Hebrew: Haredi draft political spam"),
        PatternRule(Regex("החוק\\s*שמכשיר|חוק.*מכשיר.*המונית"), ScamCategory.POLITICAL_SPAM, 0.66f, "Hebrew: law enables mass evasion clickbait"),
        PatternRule(Regex("חשיפה.*של\\s+\\S+\\s+\\S+,"), ScamCategory.POLITICAL_SPAM, 0.64f, "Hebrew: expose of named person clickbait"),
        PatternRule(Regex("ליכוד|יש\\s*עתיד|מחנה\\s*ממלכתי|הציונות\\s*הדתית"), ScamCategory.POLITICAL_SPAM, 0.66f, "Hebrew: political party names"),
        PatternRule(Regex("רפורמה.*משפטית|משפטית.*רפורמה"), ScamCategory.POLITICAL_SPAM, 0.66f, "Hebrew: judicial reform"),
        PatternRule(Regex("עצומה.*חתמ|חתמו\\s*על"), ScamCategory.POLITICAL_SPAM, 0.68f, "Hebrew: petition sign"),
        PatternRule(Regex("קואליציה|אופוזיציה"), ScamCategory.POLITICAL_SPAM, 0.64f, "Hebrew: coalition/opposition"),

        // ── Israel-specific: money waiting ───────────────────────────────
        PatternRule(Regex("מחכימ\\s*לכ.*שקלימ|אלפי\\s*שקלימ.*מחכימ"), ScamCategory.MONEY_WAITING, 0.84f, "Hebrew: money waiting for you"),
        PatternRule(Regex("הודעה\\s*דחופה.*שקלימ"), ScamCategory.MONEY_WAITING, 0.82f, "Hebrew: urgent message about money"),
        PatternRule(Regex("זכויות\\s*שלא\\s*קיבלת"), ScamCategory.MONEY_WAITING, 0.80f, "Hebrew: benefits you didn't receive"),

        // ── Israel-specific: medical / disability ────────────────────────
        PatternRule(Regex("בעיה\\s*רפואית.*תג\\s*חניה|בעיה\\s*רפואית.*תו\\s*נכה"), ScamCategory.MEDICAL_DISABILITY, 0.74f, "Hebrew: medical problem + parking tag"),
        PatternRule(Regex("תו\\s*נכה|תג\\s*(נכה|חניה\\s*לנכימ)"), ScamCategory.MEDICAL_DISABILITY, 0.72f, "Hebrew: disability parking tag"),
        PatternRule(Regex("נמאס.*להחנות|נמאס.*חניה"), ScamCategory.MEDICAL_DISABILITY, 0.72f, "Hebrew: tired of parking far"),
        PatternRule(Regex("להילחמ\\s*במערכת"), ScamCategory.MEDICAL_DISABILITY, 0.72f, "Hebrew: fighting the system"),
        PatternRule(Regex("(נמאס|בעיה\\s*רפואית).{0,40}(להילחמ|במערכת|להחנות)"), ScamCategory.MEDICAL_DISABILITY, 0.74f, "Hebrew: disability parking combo"),
        PatternRule(Regex("נכות\\s*רפואית"), ScamCategory.MEDICAL_DISABILITY, 0.72f, "Hebrew: medical disability"),
        PatternRule(Regex("ועדה\\s*רפואית"), ScamCategory.MEDICAL_DISABILITY, 0.70f, "Hebrew: medical committee"),
        PatternRule(Regex("אחוזי\\s*נכות|דרגת\\s*נכות"), ScamCategory.MEDICAL_DISABILITY, 0.72f, "Hebrew: disability percentage/level"),
        PatternRule(Regex("זכויות\\s*רפואיות"), ScamCategory.MEDICAL_DISABILITY, 0.70f, "Hebrew: medical rights"),

        // ── Propaganda / disinformation ──────────────────────────────────
        PatternRule(Regex("משקרימ.*לכמ|משקרות.*לכמ"), ScamCategory.PROPAGANDA, 0.62f, "Hebrew: authority lying to you"),
        PatternRule(Regex("תברח|אפ\\s*\\S+\\s*לא\\s*יכול\\s*לספק"), ScamCategory.PROPAGANDA, 0.65f, "Hebrew: flee / no X can provide"),
        PatternRule(Regex("הטילימ\\s*בדרכ|משמרות\\s*המהפכה"), ScamCategory.PROPAGANDA, 0.63f, "Hebrew: missiles on way / authority claim"),
        PatternRule(Regex("מקלט.*ביטחונ|ביטחונ.*מקלט"), ScamCategory.PROPAGANDA, 0.60f, "Hebrew: shelter safety framing"),
        PatternRule(Regex("תברחו|ברחו\\s*מהארצ"), ScamCategory.PROPAGANDA, 0.64f, "Hebrew: imperative flee plural"),
        PatternRule(Regex("מידע\\s*מטעה"), ScamCategory.PROPAGANDA, 0.62f, "Hebrew: misleading information"),
        PatternRule(Regex("הסתה"), ScamCategory.PROPAGANDA, 0.60f, "Hebrew: incitement"),
        PatternRule(Regex("תעמולה"), ScamCategory.PROPAGANDA, 0.60f, "Hebrew: propaganda"),
        PatternRule(Regex("מניפולציה"), ScamCategory.PROPAGANDA, 0.60f, "Hebrew: manipulation"),

        // ── Government impersonation (Hebrew) ────────────────────────────
        PatternRule(Regex("(בית\\s*משפט|הוצאה\\s*לפועל).*הודעה"), ScamCategory.IMPERSONATION, 0.80f, "Hebrew: court/execution office notice"),
        PatternRule(Regex("ביטוח\\s*לאומי.*זכאות|זכאות.*ביטוח\\s*לאומי"), ScamCategory.IMPERSONATION, 0.78f, "Hebrew: Bituach Leumi eligibility"),
        PatternRule(Regex("משרד\\s*הבריאות.*הודעה"), ScamCategory.IMPERSONATION, 0.76f, "Hebrew: Health Ministry notice"),
        PatternRule(Regex("עיריית.*חוב|חוב.*עיריית"), ScamCategory.IMPERSONATION, 0.76f, "Hebrew: municipality debt"),
        PatternRule(Regex("רשות\\s*מקרקעי"), ScamCategory.IMPERSONATION, 0.74f, "Hebrew: Israel Land Authority"),

        // ══ NEW CATEGORIES ═══════════════════════════════════════════════

        // ── Insurance spam ───────────────────────────────────────────────
        PatternRule(Regex("ביטוח\\s*רכב.*חסכ|חסכ.*ביטוח\\s*רכב"), ScamCategory.INSURANCE_SPAM, 0.74f, "Hebrew: car insurance savings"),
        PatternRule(Regex("ביטוח\\s*חיימ.*הצעה|הצעה.*ביטוח\\s*חיימ"), ScamCategory.INSURANCE_SPAM, 0.72f, "Hebrew: life insurance offer"),
        PatternRule(Regex("ביטוח\\s*דירה|ביטוח\\s*בריאות"), ScamCategory.INSURANCE_SPAM, 0.70f, "Hebrew: home/health insurance"),
        PatternRule(Regex("חסכ.*על\\s*ביטוח|השוואת\\s*ביטוחימ"), ScamCategory.INSURANCE_SPAM, 0.72f, "Hebrew: save on insurance/comparison"),
        PatternRule(Regex("פוליסה.*חידוש|חידוש\\s*ביטוח"), ScamCategory.INSURANCE_SPAM, 0.70f, "Hebrew: policy renewal"),
        PatternRule(Regex("ביטוח\\s*משתלמ|ביטוח\\s*זול"), ScamCategory.INSURANCE_SPAM, 0.70f, "Hebrew: worthwhile/cheap insurance"),

        // ── Gambling spam ────────────────────────────────────────────────
        PatternRule(Regex("קזינו.*הרוויח|הרוויח.*קזינו"), ScamCategory.GAMBLING_SPAM, 0.80f, "Hebrew: casino earnings"),
        PatternRule(Regex("הימורימ\\s*(?:ספורט|אונליינ)"), ScamCategory.GAMBLING_SPAM, 0.78f, "Hebrew: sports/online betting"),
        PatternRule(Regex("(?i)ספורטבט|sportbet|1xbet|bet365"), ScamCategory.GAMBLING_SPAM, 0.80f, "Hebrew: betting site names"),
        PatternRule(Regex("פוקר\\s*אונליינ|רולטה"), ScamCategory.GAMBLING_SPAM, 0.78f, "Hebrew: online poker/roulette"),
        PatternRule(Regex("הפקדה\\s*ראשונה.*בונוס|בונוס.*הפקדה"), ScamCategory.GAMBLING_SPAM, 0.76f, "Hebrew: first deposit bonus"),

        // ── Hebrew lottery ───────────────────────────────────────────────
        PatternRule(Regex("זכית\\s*בהגרלה"), ScamCategory.HEBREW_LOTTERY, 0.86f, "Hebrew: you won a raffle"),
        PatternRule(Regex("מספרכ\\s*נבחר"), ScamCategory.HEBREW_LOTTERY, 0.84f, "Hebrew: your number was selected"),
        PatternRule(Regex("הגרלה\\s*בלעדית"), ScamCategory.HEBREW_LOTTERY, 0.82f, "Hebrew: exclusive lottery"),
        PatternRule(Regex("פרס\\s*ראשונ"), ScamCategory.HEBREW_LOTTERY, 0.82f, "Hebrew: first prize"),
        PatternRule(Regex("(?:לוטו|גריד|מפעל\\s*הפיס|פיס).*זכ"), ScamCategory.HEBREW_LOTTERY, 0.82f, "Hebrew: Lotto/Pais win"),

        // ── Debt collection fraud ────────────────────────────────────────
        PatternRule(Regex("חוב\\s*בסכ\\s*\\d"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.80f, "Hebrew: debt amount"),
        PatternRule(Regex("עיקול.*חשבונ|חשבונ.*עיקול"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.82f, "Hebrew: account seizure"),
        PatternRule(Regex("צו\\s*הוצאה\\s*לפועל"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.84f, "Hebrew: execution order"),
        PatternRule(Regex("תשלומ\\s*מיידי.*חוב|חוב.*תשלומ\\s*מיידי"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.82f, "Hebrew: immediate debt payment"),
        PatternRule(Regex("חוב\\s*פתוח"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.78f, "Hebrew: open debt"),
        PatternRule(Regex("גביית\\s*חובות"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.78f, "Hebrew: debt collection"),
        PatternRule(Regex("הסדר\\s*חוב"), ScamCategory.DEBT_COLLECTION_FRAUD, 0.74f, "Hebrew: debt arrangement"),

        // ── Health / diet scam ───────────────────────────────────────────
        PatternRule(Regex("דיאטה\\s*מהירה"), ScamCategory.HEALTH_DIET_SCAM, 0.74f, "Hebrew: fast diet"),
        PatternRule(Regex("ירידה\\s*במשקל.*ללא|ללא.*ירידה\\s*במשקל"), ScamCategory.HEALTH_DIET_SCAM, 0.76f, "Hebrew: weight loss without"),
        PatternRule(Regex("תוספ\\s*טבעי.*הרזיה|שריפת\\s*שומנ"), ScamCategory.HEALTH_DIET_SCAM, 0.76f, "Hebrew: natural supplement/fat burn"),
        PatternRule(Regex("ירידה\\s*של\\s*\\d+\\s*קילו"), ScamCategory.HEALTH_DIET_SCAM, 0.78f, "Hebrew: lose X kilos"),
        PatternRule(Regex("סוד\\s*הרזיה|תוספ\\s*מהפכני"), ScamCategory.HEALTH_DIET_SCAM, 0.76f, "Hebrew: weight loss secret/revolutionary supplement"),
        PatternRule(Regex("בלי\\s*דיאטה.*רזה|רזה.*בלי\\s*דיאטה"), ScamCategory.HEALTH_DIET_SCAM, 0.74f, "Hebrew: thin without diet"),

        // ── Real estate spam ─────────────────────────────────────────────
        PatternRule(Regex("השקעה\\s*בנדלנ"), ScamCategory.REAL_ESTATE_SPAM, 0.72f, "Hebrew: real estate investment"),
        PatternRule(Regex("פרויקט\\s*חדש.*דירות|דירות.*פרויקט\\s*חדש"), ScamCategory.REAL_ESTATE_SPAM, 0.70f, "Hebrew: new project apartments"),
        PatternRule(Regex("מחיר\\s*למשתכנ"), ScamCategory.REAL_ESTATE_SPAM, 0.70f, "Hebrew: Mechir LaMishtaken"),
        PatternRule(Regex("דירות\\s*למכירה.*מיוחד|נדלנ\\s*להשקעה"), ScamCategory.REAL_ESTATE_SPAM, 0.70f, "Hebrew: apartments for sale/investment"),
        PatternRule(Regex("דירה\\s*מפנה"), ScamCategory.REAL_ESTATE_SPAM, 0.68f, "Hebrew: apartment vacating"),

        // ── Car accident claims ──────────────────────────────────────────
        PatternRule(Regex("תאונת\\s*דרכימ.*פיצוי|פיצוי.*תאונת\\s*דרכימ"), ScamCategory.CAR_ACCIDENT_CLAIM, 0.76f, "Hebrew: car accident compensation"),
        PatternRule(Regex("נפגעת\\s*בתאונה"), ScamCategory.CAR_ACCIDENT_CLAIM, 0.76f, "Hebrew: were you in an accident"),
        PatternRule(Regex("תביעת\\s*נזיקינ"), ScamCategory.CAR_ACCIDENT_CLAIM, 0.74f, "Hebrew: tort claim"),
        PatternRule(Regex("זכויות\\s*נפגעי\\s*תאונות"), ScamCategory.CAR_ACCIDENT_CLAIM, 0.74f, "Hebrew: accident victim rights"),
        PatternRule(Regex("עורכ\\s*דינ\\s*תאונות"), ScamCategory.CAR_ACCIDENT_CLAIM, 0.72f, "Hebrew: accident lawyer"),

        // ── Charity scam ─────────────────────────────────────────────────
        PatternRule(Regex("תרומה\\s*דחופה"), ScamCategory.CHARITY_SCAM, 0.76f, "Hebrew: urgent donation"),
        PatternRule(Regex("ארגונ\\s*צדקה.*תרמ|תרמו\\s*עכשיו"), ScamCategory.CHARITY_SCAM, 0.74f, "Hebrew: charity org donate now"),
        PatternRule(Regex("עזרה\\s*לנזקקימ"), ScamCategory.CHARITY_SCAM, 0.72f, "Hebrew: help the needy"),
        PatternRule(Regex("תרומה\\s*חד\\s*פעמית"), ScamCategory.CHARITY_SCAM, 0.70f, "Hebrew: one-time donation"),
        PatternRule(Regex("ילדימ\\s*חולימ"), ScamCategory.CHARITY_SCAM, 0.74f, "Hebrew: sick children"),

        // ── Utility fraud ────────────────────────────────────────────────
        PatternRule(Regex("חשבונ\\s*חשמל.*חוב|חוב.*חשבונ\\s*חשמל"), ScamCategory.UTILITY_FRAUD, 0.78f, "Hebrew: electricity bill debt"),
        PatternRule(Regex("חברת\\s*החשמל.*ניתוק|ניתוק\\s*חשמל"), ScamCategory.UTILITY_FRAUD, 0.80f, "Hebrew: electric company disconnection"),
        PatternRule(Regex("חוב\\s*לעירייה|ארנונה.*חוב|חוב.*ארנונה"), ScamCategory.UTILITY_FRAUD, 0.76f, "Hebrew: municipality debt/arnona"),
        PatternRule(Regex("חשבונ\\s*מימ.*חוב|מקורות.*חוב"), ScamCategory.UTILITY_FRAUD, 0.76f, "Hebrew: water bill debt"),

        // ── Carrier scam ─────────────────────────────────────────────────
        PatternRule(Regex("חבילת\\s*גלישה.*חינמ|גיגה\\s*חינמ"), ScamCategory.CARRIER_SCAM, 0.74f, "Hebrew: free data package"),
        PatternRule(Regex("שדרוג\\s*חינמ.*סלולר|סלולר.*שדרוג\\s*חינמ"), ScamCategory.CARRIER_SCAM, 0.74f, "Hebrew: free cellular upgrade"),
        PatternRule(Regex("(?i)(Partner|Cellcom|HOT\\s*Mobile|Pelephone|We4G|Golan).*חבילה\\s*בלעדית"), ScamCategory.CARRIER_SCAM, 0.76f, "Hebrew: carrier exclusive package"),
        PatternRule(Regex("(?i)חבילה\\s*בלעדית.*(Partner|Cellcom|HOT|Pelephone|We4G|Golan)"), ScamCategory.CARRIER_SCAM, 0.76f, "Hebrew: exclusive carrier package"),

        // ── Legal scam ───────────────────────────────────────────────────
        PatternRule(Regex("תביעה\\s*ייצוגית.*זכאי|זכאי.*תביעה\\s*ייצוגית"), ScamCategory.LEGAL_SCAM, 0.76f, "Hebrew: class action eligibility"),
        PatternRule(Regex("זכאות\\s*לפיצוי"), ScamCategory.LEGAL_SCAM, 0.74f, "Hebrew: compensation eligibility"),
        PatternRule(Regex("זכויות\\s*צרכנימ"), ScamCategory.LEGAL_SCAM, 0.72f, "Hebrew: consumer rights"),
        PatternRule(Regex("פיצוי\\s*כספי.*לחצ|לחצ.*פיצוי\\s*כספי"), ScamCategory.LEGAL_SCAM, 0.74f, "Hebrew: monetary compensation click"),

        // ── Commercial spam ──────────────────────────────────────────────
        PatternRule(Regex("מבצע\\s*חד\\s*פעמי"), ScamCategory.COMMERCIAL_SPAM, 0.68f, "Hebrew: one-time sale"),
        PatternRule(Regex("הנחה\\s*של\\s*\\d+%"), ScamCategory.COMMERCIAL_SPAM, 0.66f, "Hebrew: X% discount"),
        PatternRule(Regex("מכירת\\s*חיסול"), ScamCategory.COMMERCIAL_SPAM, 0.68f, "Hebrew: clearance sale"),
        PatternRule(Regex("(?i)BLACK\\s*FRIDAY"), ScamCategory.COMMERCIAL_SPAM, 0.64f, "Hebrew: Black Friday"),
        PatternRule(Regex("מבצע\\s*בלעדי"), ScamCategory.COMMERCIAL_SPAM, 0.66f, "Hebrew: exclusive sale"),
        PatternRule(Regex("קופונ\\s*הנחה"), ScamCategory.COMMERCIAL_SPAM, 0.64f, "Hebrew: discount coupon"),

        // ── Chain message ────────────────────────────────────────────────
        PatternRule(Regex("העבר\\s*ל-?\\d+\\s*אנשימ"), ScamCategory.CHAIN_MESSAGE, 0.78f, "Hebrew: forward to N people"),
        PatternRule(Regex("העבר\\s*הלאה"), ScamCategory.CHAIN_MESSAGE, 0.72f, "Hebrew: forward this"),
        PatternRule(Regex("שלח\\s*לכל\\s*אנשי\\s*הקשר"), ScamCategory.CHAIN_MESSAGE, 0.78f, "Hebrew: send to all contacts"),
        PatternRule(Regex("הודעה\\s*דחופה.*העבר|העבר.*הודעה\\s*דחופה"), ScamCategory.CHAIN_MESSAGE, 0.76f, "Hebrew: urgent message forward"),
        PatternRule(Regex("שתפ\\s*עמ\\s*חברימ"), ScamCategory.CHAIN_MESSAGE, 0.72f, "Hebrew: share with friends"),

        // ── Service impersonation ────────────────────────────────────────
        PatternRule(Regex("(?i)(Wolt|10bis|תנ\\s*ביס).*הזמנ"), ScamCategory.SERVICE_IMPERSONATION, 0.74f, "Hebrew: Wolt/10bis order"),
        PatternRule(Regex("(?i)(Gett|Yango).*נסיעה"), ScamCategory.SERVICE_IMPERSONATION, 0.74f, "Hebrew: Gett/Yango ride"),
        PatternRule(Regex("(?i)Waze.*עדכונ"), ScamCategory.SERVICE_IMPERSONATION, 0.70f, "Hebrew: Waze update"),
        PatternRule(Regex("(?i)(Wolt|10bis|Gett|Yango|Waze).*לחצ\\s*כאנ"), ScamCategory.SERVICE_IMPERSONATION, 0.78f, "Hebrew: service name + click here"),
        PatternRule(Regex("הזמנה\\s*שלכ.*משלוח\\s*בדרכ"), ScamCategory.SERVICE_IMPERSONATION, 0.70f, "Hebrew: your order delivery on way"),
    )

    // ── Analysis ─────────────────────────────────────────────────────────

    fun analyze(body: String): ScamAnalysis {
        if (body.length < 10) return ScamAnalysis(false, 0f, null, null)

        val matchBody = if (body.any { it in '\u0590'..'\u05FF' })
            HebrewTextNormalizer.normalizeForMatching(body)
        else body

        val signals = mutableListOf<String>()
        var combinedScore = 0f
        var bestCategory: ScamCategory? = null
        var bestReason: String? = null
        var bestRuleScore = 0f

        for (rule in rules) {
            if (rule.regex.containsMatchIn(matchBody)) {
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

    override suspend fun isAllowlisted(address: String): Boolean {
        return try {
            // Exact match first (fast path)
            if (learningDao.isAllowlisted(address)) return true
            // Normalized match: +972501234567 and 0501234567 should both match
            val norm = normalizeForMatching(address)
            val all = learningDao.getAllAllowlisted()
            all.any { normalizeForMatching(it.address) == norm }
        } catch (e: Exception) {
            false
        }
    }

    private fun normalizeForMatching(a: String): String =
        a.replace(Regex("[^0-9]"), "").let { d ->
            if (d.startsWith("972") && d.length >= 12) "0" + d.drop(3) else d
        }

    suspend fun addToAllowlist(address: String) {
        mutex.withLock {
            learningDao.addToAllowlist(
                com.novachat.core.database.entity.SenderAllowlistEntity(address = address)
            )
            // Clear spam entries so conversation shows in main inbox
            spamMessageDao.deleteByAddress(address)
            val norm = normalizeForMatching(address)
            if (norm.startsWith("0") && norm.length >= 9) {
                spamMessageDao.deleteByAddress("+972" + norm.drop(1))
                spamMessageDao.deleteByAddress("972" + norm.drop(1))
            } else if (norm.startsWith("972") && norm.length >= 12) {
                val local = "0" + norm.drop(3)
                spamMessageDao.deleteByAddress(local)
                spamMessageDao.deleteByAddress("+$norm")
            }
        }
    }

    suspend fun removeFromAllowlist(address: String) {
        mutex.withLock {
            learningDao.removeFromAllowlist(address)
        }
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
        if (isAllowlisted(address)) {
            return ScamAnalysis(isScam = false, confidence = 0f, reason = null, category = null)
        }

        ensureCacheLoaded()
        val base = analyze(body)
        var adjusted = base.confidence

        // Unknown sender scrutiny: boost score for numbers not in contacts
        if (!isKnownContact) {
            adjusted = (adjusted + 0.10f).coerceAtMost(0.98f)
        }

        // Alphanumeric sender scrutiny: boost for sender IDs like FREETAX, AMAZON (common in spam)
        if (isAlphanumericSender(address) && base.confidence > 0.3f) {
            adjusted = (adjusted + 0.08f).coerceAtMost(0.98f)
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

    private fun isAlphanumericSender(address: String): Boolean {
        val normalized = address.replace(Regex("[^A-Za-z0-9]"), "")
        if (normalized.length < 4) return false
        val letterCount = normalized.count { it.isLetter() }
        val digitCount = normalized.count { it.isDigit() }
        return letterCount >= 2 && digitCount < normalized.length
    }

    fun getAutoBlockThreshold(category: ScamCategory?): Float = when (category) {
        ScamCategory.OTP_FRAUD, ScamCategory.PHISHING -> 0.80f
        ScamCategory.POLITICAL_SPAM, ScamCategory.PROPAGANDA -> 0.95f
        ScamCategory.COMMERCIAL_SPAM, ScamCategory.CHAIN_MESSAGE -> 0.90f
        else -> 0.85f
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

    /** Normalize token: keep Unicode letters and digits, strip punctuation. Supports Hebrew and other non-Latin scripts. */
    private fun normalizeToken(word: String): String = word.replace(Regex("[^\\p{L}\\p{N}]"), "")

    /** Cache key for learned keywords: lowercase Latin-only tokens (backward compat), keep Hebrew/other as-is. */
    private fun tokenCacheKey(token: String): String {
        val normalized = normalizeToken(token)
        return if (normalized.all { it in 'a'..'z' || it in 'A'..'Z' || it.isDigit() }) normalized.lowercase() else normalized
    }

    private fun computeHeuristics(body: String, signals: MutableList<String>): Float {
        var score = 0f
        val words = body.split("\\s+".toRegex())

        // Line-fragmentation: propaganda often uses short, punchy lines (one claim per line)
        val lines = body.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val avgLineLen = if (lines.isNotEmpty()) body.length.toFloat() / lines.size else 0f
        if (lines.size >= 4 && avgLineLen < 60f) {
            score += 0.10f
            signals.add("Fragmented line structure (common in propaganda)")
        }

        // Low punctuation ratio: spam/disinformation often has very few punctuation marks
        val punctuationCount = body.count { it in ".,;:!?—–-\"'" }
        val punctRatio = if (body.length >= 20) punctuationCount.toFloat() / body.length else 0f
        if (punctRatio < 0.015f && lines.size >= 3) {
            score += 0.07f
            signals.add("Very low punctuation (common in propaganda)")
        }

        // Excessive exclamation marks
        val exclamationCount = body.count { it == '!' }
        if (exclamationCount > 3) {
            score += 0.1f
            signals.add("Excessive exclamation marks ($exclamationCount)")
        }

        // ALL CAPS words ratio (Hebrew has no case - only count words with Latin letters that have uppercase)
        val latinWords = words.filter { it.any { c -> c in 'a'..'z' || c in 'A'..'Z' } }
        val capsWords = latinWords.count { it.length > 3 && it == it.uppercase() && it.any { c -> c.isLetter() } }
        val capsRatio = if (latinWords.isNotEmpty()) capsWords.toFloat() / latinWords.size else 0f
        if (capsRatio > 0.3f) {
            score += 0.12f
            signals.add("High ALL-CAPS ratio (${(capsRatio * 100).toInt()}%)")
        }

        // Currency symbols + amounts (include shekel ₪ and שח)
        val currencyPattern = Regex("[\\$€£₹₪]\\s*[\\d,]+\\.?\\d*|\\d+[\\$€£₹₪]|[\\d,]+\\s*ש[\"״]?ח")
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
        var infoRequests = infoRequestWords.count { body.contains(it, ignoreCase = true) }

        // Hebrew personal-info / benefit phrases (when Hebrew present)
        val hasHebrew = body.any { it in '\u0590'..'\u05FF' }
        if (hasHebrew) {
            val hebrewInfoWords = listOf("תעודת זהות", "מספר כרטיס", "סיסמה", "קוד אימות", "פרטי חשבון", "כרטיס אשראי")
            infoRequests += hebrewInfoWords.count { body.contains(it) }
            val hebrewUrgencyWords = listOf(
                "דחוף", "מיד", "בהקדם",
                "מיידי", "מיידית", "מידי", "מידית",
                "תגיב", "אזהרה", "פג תוקף", "נדרשת פעולה", "ייחסם",
                "פעולה מיידית", "יש לך הודעה",
                "לחץ כאן", "לחץ", "לחצו", "הקלק", "הקליקו",
                "לבדיקה", "בדוק עכשיו"
            )
            if (hebrewUrgencyWords.count { body.contains(it) } >= 2) {
                score += 0.10f
                signals.add("High Hebrew urgency word density")
            }
            // Fear/catastrophe framing (authority claim + threat)
            val hebrewFearWords = listOf("השמדה", "הטילים", "בדרך", "מקלט", "ביטחון", "משמרות", "תברח", "משקרים")
            if (hebrewFearWords.count { body.contains(it) } >= 2) {
                score += 0.08f
                signals.add("Hebrew fear/catastrophe framing")
            }
        }
        if (infoRequests > 0) {
            score += 0.15f
            signals.add("Requests sensitive personal information")
        }

        return score.coerceAtMost(0.5f)
    }

    private fun computeLearnedKeywordScore(body: String): Float {
        if (keywordWeightCache.isEmpty()) return 0f
        val tokens = body.split("\\s+".toRegex())
            .filter { it.length >= 3 }
            .map { normalizeToken(it) }
            .filter { it.isNotBlank() }
        var totalWeight = 0f
        var matchCount = 0
        for (token in tokens) {
            val key = tokenCacheKey(token)
            val w = keywordWeightCache[key]
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
        val tokens = body.split("\\s+".toRegex())
            .filter { it.length >= 3 }
            .map { normalizeToken(it) }
            .filter { it.isNotBlank() }
            .distinct()

        for (token in tokens) {
            val key = tokenCacheKey(token)
            val existing = learningDao.getKeywordWeight(key)
                ?: SpamKeywordWeightEntity(keyword = key)
            val updated = if (isSpam) {
                existing.copy(spamOccurrences = existing.spamOccurrences + 1)
            } else {
                existing.copy(hamOccurrences = existing.hamOccurrences + 1)
            }
            val total = updated.spamOccurrences + updated.hamOccurrences
            val weight = if (total > 0) updated.spamOccurrences.toFloat() / total else 0f
            val final_ = updated.copy(weight = weight)
            learningDao.upsertKeywordWeight(final_)
            keywordWeightCache[key] = weight
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
