package com.novachat.core.sms

/**
 * Layer 1: High-performance deterministic regex engine for common phishing patterns.
 * Catches shortened URLs, suspicious TLDs, urgent keywords, OTP/Verify patterns,
 * Hebrew tax refund scams (החזרי/החזר מס), survey/political spam (להסרה: השיבו),
 * and political/election campaign spam (סקר, מנדטים, בחירות, הצביעו, קלפי).
 * Runs first before heuristic/semantic layers.
 */
object DeterministicSpamLayer {

    private val shortenedUrlRegex = Regex(
        "(?i)(bit\\.ly|tinyurl|t\\.co|goo\\.gl|rb\\.gy|shorturl|cutt\\.ly|is\\.gd|v\\.gd|qr\\.ae|clck\\.ru)/\\S+"
    )
    private val suspiciousTldRegex = Regex(
        "(?i)https?://[a-z0-9-]+\\.(tk|ml|ga|cf|gq|xyz|top|buzz|club|icu|work|info|rest|click|link|online|site|fun)/\\S*"
    )
    private val urgentKeywordsRegex = Regex(
        "(?i)(act\\s+now|respond\\s+immediately|urgent|expires?\\s+(today|soon|in\\s+\\d+\\s*(hour|min|second))|verify\\s+now|confirm\\s+immediately|דחוף|מיד|בהקדם|פעולה מיידית|הזדמנות אחרונה|רק היומ|נותרו שעות|מוגבל בזמנ|לפני שיגמר|עד חצות)"
    )
    private val otpVerifyEnglishRegex = Regex(
        "(?i)(OTP|verify|verification code)"
    )
    private val otpVerifyHebrewRegex = Regex(
        "קוד\\s*אימות|הזנ\\s*את\\s*הקוד|קוד\\s*זמני|שלח.*הקוד|קוד\\s*בנ\\s*6\\s*ספרות|קוד\\s*חד\\s*פעמי|סיסמה\\s*זמנית|אימות\\s*דו\\s*שלבי|קוד\\s*הגיע.*בטעות"
    )
    private val ipUrlRegex = Regex(
        "(?i)https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}[/\\S]*"
    )
    private val taxRefundScamRegex = Regex("החזרי?\\s*מס|זיכוי\\s*מס|דוח\\s*שנתי.*מס|ניכוי\\s*מס")
    private val surveyUnsubscribeRegex = Regex("להסרה:\\s*השיבו")
    private val politicalPollRegex = Regex("סקר|מנדטימ|בבחירות.*בוחרימ|בוחרימ ב\\S+!|הצביעו ל|להצביע ל|יומ הבחירות|קלפי|ליכוד|יש\\s*עתיד|מחנה\\s*ממלכתי|קואליציה|אופוזיציה|עצומה.*חתמ")
    private val israeliBankRegex = Regex("בנק.*(נחסמ|ננעל)|כרטיס.*נחסמ|חשבנ.*נחסמ|לאומי|הפועלימ|דיסקונט|מזרחי|יהב|מסד|פפר|(?i)one\\s*zero|כאל|ישראכרט|מקס|ויזה\\s*כאל|לאומי\\s*קארד|העברה\\s*חשודה|פעילות\\s*חריגה|כרטיס\\s*אשראי.*חסומ")
    private val israeliDeliveryRegex = Regex("חבילה.*ממתינה|דואר.*ישראל|משלוח.*מחכה|חבילתכ|(?i)(FedEx|Amazon|AliExpress|Shein|Temu|iHerb|eBay).*חבילה|מספר\\s*מעקב|אישור\\s*מכס|תשלומ\\s*משלוח|שחרור\\s*חבילה")
    private val israeliGovRegex = Regex("רשות המיסימ|משטרה|פיקוד העורפ|מבקר המדינה|בית\\s*משפט|הוצאה\\s*לפועל|ביטוח\\s*לאומי|משרד\\s*הבריאות|עיריית|רשות\\s*מקרקעי")
    private val israeliPanicRegex = Regex("מתקפת טילימ|חפש מקלט|אזהרה.*טילימ|אזעקה")
    private val israeliCryptoRegex = Regex("קריפטו|השקעה מובטחת|ביטקוינ|אתריומ|(?i)NFT|הכנסה\\s*פסיבית|(?i)(טריידינג|פורקס|FOREX)|תשואה\\s*מובטחת|סיגנלימ.*מסחר|בורסה.*רווח")

    // New category regexes
    private val insuranceSpamRegex = Regex("ביטוח\\s*(רכב|חיימ|דירה|בריאות).*(?:חסכ|הצעה|השווא)|השוואת\\s*ביטוחימ|חידוש\\s*ביטוח|פוליסה.*חידוש")
    private val gamblingRegex = Regex("קזינו|הימורימ|(?i)(ספורטבט|sportbet|1xbet|bet365)|פוקר\\s*אונליינ|רולטה|הפקדה\\s*ראשונה.*בונוס")
    private val hebrewLotteryRegex = Regex("זכית\\s*בהגרלה|מספרכ\\s*נבחר|הגרלה\\s*בלעדית|(?:לוטו|גריד|מפעל\\s*הפיס|פיס).*זכ")
    private val debtCollectionRegex = Regex("עיקול.*חשבונ|צו\\s*הוצאה\\s*לפועל|חוב\\s*בסכ\\s*\\d|גביית\\s*חובות|חוב\\s*פתוח")
    private val healthDietRegex = Regex("דיאטה\\s*מהירה|שריפת\\s*שומנ|ירידה\\s*של\\s*\\d+\\s*קילו|סוד\\s*הרזיה|תוספ\\s*מהפכני")
    private val realEstateRegex = Regex("השקעה\\s*בנדלנ|מחיר\\s*למשתכנ|נדלנ\\s*להשקעה|פרויקט\\s*חדש.*דירות")
    private val carAccidentRegex = Regex("תאונת\\s*דרכימ.*פיצוי|נפגעת\\s*בתאונה|תביעת\\s*נזיקינ|עורכ\\s*דינ\\s*תאונות")
    private val charityScamRegex = Regex("תרומה\\s*דחופה|תרמו\\s*עכשיו|ילדימ\\s*חולימ.*תרמ")
    private val utilityFraudRegex = Regex("חברת\\s*החשמל.*ניתוק|ניתוק\\s*חשמל|חוב\\s*לעירייה|ארנונה.*חוב")
    private val carrierScamRegex = Regex("חבילת\\s*גלישה.*חינמ|גיגה\\s*חינמ|שדרוג\\s*חינמ.*סלולר|(?i)(Partner|Cellcom|HOT\\s*Mobile|Pelephone|We4G|Golan).*חבילה\\s*בלעדית")
    private val legalScamRegex = Regex("תביעה\\s*ייצוגית|זכאות\\s*לפיצוי|זכויות\\s*צרכנימ")
    private val commercialSpamRegex = Regex("מבצע\\s*חד\\s*פעמי|מכירת\\s*חיסול|מבצע\\s*בלעדי|הנחה\\s*של\\s*\\d+%")
    private val chainMessageRegex = Regex("העבר\\s*ל-?\\d+\\s*אנשימ|שלח\\s*לכל\\s*אנשי\\s*הקשר|העבר\\s*הלאה|שתפ\\s*\$|שלח\\s*הלאה|שתפ\\s*עמ\\s*חברימ")
    private val serviceImpersonationRegex = Regex("(?i)(Wolt|10bis|Gett|Yango|Waze).*לחצ\\s*כאנ|(?i)(Wolt|10bis|תנ\\s*ביס).*הזמנ|כביש\\s*(6|שש|ששה)|יתרה\\s*לא\\s*סולקה|הליכימ\\s*משפטיימ|(כביש|אגרה|אגרת)\\s*(דרכ|נתיבי)|להסדיר\\s*את\\s*התשלומ")
    private val pensionRegex = Regex("קרנ\\s*פנסיה|ביטוח\\s*מנהלימ|קופת\\s*גמל|כספימ\\s*תקועימ|פנסיה\\s*מוקפאת|פיצויימ.*ללא\\s*התפטרות")
    private val loanHebrewRegex = Regex("הלוואה\\s*מיידית|הלוואה\\s*חוצ\\s*בנקאית|ריבית\\s*אפס|ללא\\s*ערבימ|ללא\\s*בדיקת\\s*BDI|משכנתא.*ריבית")
    private val medicalDisabilityRegex = Regex("תו\\s*נכה|נכות\\s*רפואית|ועדה\\s*רפואית|אחוזי\\s*נכות")
    private val prizeHebrewRegex = Regex("זכית\\s*בפרס|הגרלה\\s*בלעדית|פרס\\s*כספי|קופונ\\s*מתנה|שובר\\s*מתנה")
    private val jobHebrewRegex = Regex("עבודה\\s*מהבית|הכנסה\\s*נוספת|הרוויחו\\s*מהבית|משרה\\s*דחופה")

    private data class Rule(val regex: Regex, val type: String, val score: Int, val priority: Int)

    private val rules = listOf(
        Rule(taxRefundScamRegex, "TAX_REFUND_SCAM", 25, 100),
        Rule(israeliPanicRegex, "ISRAELI_PANIC", 30, 95),
        Rule(surveyUnsubscribeRegex, "SURVEY_UNSUBSCRIBE", 35, 90),
        Rule(politicalPollRegex, "POLITICAL_POLL", 35, 90),
        Rule(israeliBankRegex, "ISRAELI_BANK", 25, 85),
        Rule(israeliGovRegex, "ISRAELI_GOV", 25, 85),
        Rule(israeliDeliveryRegex, "ISRAELI_DELIVERY", 25, 80),
        Rule(israeliCryptoRegex, "ISRAELI_CRYPTO", 25, 80),
        Rule(debtCollectionRegex, "DEBT_COLLECTION", 25, 82),
        Rule(hebrewLotteryRegex, "HEBREW_LOTTERY", 25, 80),
        Rule(gamblingRegex, "GAMBLING", 30, 80),
        Rule(utilityFraudRegex, "UTILITY_FRAUD", 25, 80),
        Rule(carAccidentRegex, "CAR_ACCIDENT_CLAIM", 25, 78),
        Rule(charityScamRegex, "CHARITY_SCAM", 20, 75),
        Rule(insuranceSpamRegex, "INSURANCE_SPAM", 20, 75),
        Rule(healthDietRegex, "HEALTH_DIET_SCAM", 20, 75),
        Rule(carrierScamRegex, "CARRIER_SCAM", 20, 75),
        Rule(serviceImpersonationRegex, "SERVICE_IMPERSONATION", 25, 75),
        Rule(legalScamRegex, "LEGAL_SCAM", 20, 75),
        Rule(realEstateRegex, "REAL_ESTATE_SPAM", 15, 70),
        Rule(commercialSpamRegex, "COMMERCIAL_SPAM", 15, 70),
        Rule(chainMessageRegex, "CHAIN_MESSAGE", 25, 70),
        Rule(pensionRegex, "PENSION_SEVERANCE", 25, 78),
        Rule(loanHebrewRegex, "LOAN_HEBREW", 25, 78),
        Rule(medicalDisabilityRegex, "MEDICAL_DISABILITY", 20, 75),
        Rule(prizeHebrewRegex, "PRIZE_HEBREW", 25, 78),
        Rule(jobHebrewRegex, "JOB_HEBREW", 20, 75),
        Rule(ipUrlRegex, "IP_URL", 25, 70),
        Rule(suspiciousTldRegex, "SUSPICIOUS_TLD", 25, 65),
        Rule(shortenedUrlRegex, "SHORTENED_URL", 25, 60),
        Rule(urgentKeywordsRegex, "URGENT_KEYWORDS", 20, 50),
        Rule(otpVerifyEnglishRegex, "OTP_VERIFY_EN", 25, 40),
        Rule(otpVerifyHebrewRegex, "OTP_VERIFY_HE", 25, 40)
    )

    data class MatchResult(
        val matched: Boolean,
        val ruleType: String?,
        val contributesToScore: Int,
        val matchedRuleTypes: List<String> = emptyList()
    )

    /**
     * Runs all deterministic pattern checks on message body.
     * Returns the highest-priority match as the primary ruleType,
     * with contributesToScore accumulated from all matched patterns (capped at 60).
     */
    fun analyze(body: String): MatchResult {
        if (body.length < 10) return MatchResult(false, null, 0)

        var bestRule: Rule? = null
        var totalScore = 0
        val matchedTypes = mutableListOf<String>()

        for (rule in rules) {
            if (rule.regex.containsMatchIn(body)) {
                matchedTypes.add(rule.type)
                totalScore += rule.score
                if (bestRule == null || rule.priority > bestRule.priority) {
                    bestRule = rule
                }
            }
        }

        if (bestRule == null) return MatchResult(false, null, 0)

        return MatchResult(
            matched = true,
            ruleType = bestRule.type,
            contributesToScore = totalScore.coerceAtMost(60),
            matchedRuleTypes = matchedTypes
        )
    }
}
