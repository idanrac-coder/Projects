package com.novachat.core.sms.hebrew

/**
 * Weighted keyword/phrase rules for Israeli spam. Run on normalized body.
 */
object HebrewKeywordScorer {

    data class Rule(
        val name: String,
        val pattern: Regex,
        val score: Float,
        val description: String
    )

    // Patterns use normalized forms: ם→מ, ן→נ, etc. (run on normalized body)
    private val RULES = listOf(
        // ── Existing categories (expanded) ───────────────────────────────
        Rule("bank_phishing", Regex("חשבנ.*נחסמ|כרטיס.*נחסמ|בנק.*נחסמ|בנק.*(לאומי|הפועלימ|דיסקונט|מזרחי|יהב|מסד|ירושלימ).*נחסמ"), 0.75f, "Bank account blocked"),
        Rule("bank_credit_card", Regex("(כאל|ישראכרט|מקס|ויזה\\s*כאל|לאומי\\s*קארד).*(נחסמ|חסומ|פעילות\\s*חריגה)"), 0.75f, "Credit card company alert"),
        Rule("bank_digital", Regex("(פפר|(?i)one\\s*zero|וואנ\\s*זירו).*(חשבנ|נחסמ|פעילות)"), 0.72f, "Digital bank alert"),
        Rule("bank_suspicious", Regex("העברה\\s*חשודה|פעילות\\s*חריגה.*בחשבונ|אבטחת\\s*חשבונ|אימות\\s*זהות"), 0.70f, "Suspicious bank activity"),
        Rule("delivery_scam", Regex("חבילה.*ממתינה|דואר.*ישראל.*חבילה|חבילתכ|(?i)(DHL|UPS|FedEx|Amazon|AliExpress|Shein|Temu|iHerb|eBay).*חבילה"), 0.65f, "Delivery package scam"),
        Rule("delivery_customs", Regex("אישור\\s*מכס|תשלומ\\s*מכס|שחרור\\s*חבילה|תשלומ\\s*משלוח|מספר\\s*מעקב"), 0.60f, "Delivery customs/tracking"),
        Rule("tax_refund_scam", Regex("החזר.*מס|רשות.*המיסימ|זיכוי\\s*מס|ניכוי\\s*מס"), 0.70f, "Tax refund / gov"),
        Rule("tax_refund_check", Regex("בדיקה\\s*חינמ.*מס|דוח\\s*שנתי.*מס|מס\\s*הכנסה.*החזר"), 0.65f, "Tax refund free check"),
        Rule("crypto_scam", Regex("קריפטו|השקעה.*מובטחת|ביטקוינ|מטבעות.*דיגיטליימ|אתריומ|(?i)NFT"), 0.65f, "Crypto investment"),
        Rule("crypto_trading", Regex("הכנסה\\s*פסיבית|(?i)(טריידינג|פורקס|FOREX)|תשואה\\s*מובטחת|סיגנלימ.*מסחר|בורסה.*רווח"), 0.65f, "Crypto trading/forex"),
        Rule("panic_disinformation", Regex("מתקפת.*טילימ|חפש.*מקלט|אזהרה.*טילימ|(?i)Red.?Alert|אזעקה"), 0.80f, "Panic / war misinformation"),
        Rule("government_impersonation", Regex("(משטרה|רשות.*המיסימ|פיקוד.*העורפ).*(הודעה.*חשובה|תגובה.*נדרשת)"), 0.65f, "Government impersonation"),
        Rule("gov_institutions", Regex("(בית\\s*משפט|הוצאה\\s*לפועל|ביטוח\\s*לאומי|משרד\\s*הבריאות|עיריית|רשות\\s*מקרקעי).*הודעה"), 0.60f, "Government institution notice"),
        Rule("otp_phishing", Regex("שלח.*הקוד|קוד.*אימות.*לחצ|הזנ.*הקוד|קוד\\s*חד\\s*פעמי|סיסמה\\s*זמנית|קוד\\s*הגיע.*בטעות"), 0.85f, "OTP phishing"),
        Rule("political_campaign", Regex("בבחירות.*בוחרימ|בוחרימ ב|הצביעו ל|בחירות.*הקרובות|קלפי|ליכוד|יש\\s*עתיד|מחנה\\s*ממלכתי"), 0.70f, "Political campaign spam"),
        Rule("political_petition", Regex("עצומה.*חתמ|חתמו\\s*על|קואליציה|אופוזיציה|רפורמה.*משפטית"), 0.60f, "Political petition/reform"),
        Rule("soft_urgency", Regex("חשוב.*מאוד|תוקפ.*פג|פעולה.*נדרשת|הודעה.*קריטית|הזדמנות\\s*אחרונה|נותרו\\s*שעות|מוגבל\\s*בזמנ"), 0.55f, "Soft urgency"),
        Rule("click_here", Regex("לחצ.*כאנ|עדכנ.*פרטימ"), 0.70f, "Click here / update details"),
        Rule("loan_hebrew", Regex("הלוואה\\s*מיידית|הלוואה\\s*חוצ\\s*בנקאית|ריבית\\s*אפס|ללא\\s*ערבימ|משכנתא.*ריבית|מימונ.*מיידי"), 0.65f, "Hebrew loan scam"),
        Rule("pension_severance", Regex("קרנ\\s*פנסיה|ביטוח\\s*מנהלימ|קופת\\s*גמל|כספימ\\s*תקועימ|פנסיה\\s*מוקפאת|זכויות\\s*פנסיוניות"), 0.65f, "Pension/severance"),
        Rule("prize_hebrew", Regex("זכית\\s*בפרס|הגרלה\\s*בלעדית|פרס\\s*כספי|קופונ\\s*מתנה|שובר\\s*מתנה|מזל\\s*טוב.*נבחרת"), 0.70f, "Hebrew prize/lottery"),
        Rule("job_hebrew", Regex("עבודה\\s*מהבית|הכנסה\\s*נוספת|הרוויחו\\s*מהבית|משרה\\s*דחופה|שכר\\s*גבוה.*מהבית"), 0.65f, "Hebrew job scam"),
        Rule("medical_disability", Regex("תו\\s*נכה|נכות\\s*רפואית|ועדה\\s*רפואית|אחוזי\\s*נכות|דרגת\\s*נכות|זכויות\\s*רפואיות"), 0.60f, "Medical disability"),
        Rule("propaganda", Regex("מידע\\s*מטעה|הסתה|תעמולה|מניפולציה|משקרימ.*לכמ"), 0.55f, "Propaganda / disinformation"),

        // ── New categories ───────────────────────────────────────────────
        Rule("insurance_spam", Regex("ביטוח\\s*(רכב|חיימ|דירה|בריאות)|השוואת\\s*ביטוחימ|חידוש\\s*ביטוח|פוליסה.*חידוש|ביטוח\\s*משתלמ"), 0.55f, "Insurance spam"),
        Rule("gambling_spam", Regex("קזינו|הימורימ|(?i)(ספורטבט|sportbet|1xbet|bet365)|פוקר\\s*אונליינ|רולטה|הפקדה\\s*ראשונה.*בונוס"), 0.70f, "Gambling spam"),
        Rule("hebrew_lottery", Regex("זכית\\s*בהגרלה|מספרכ\\s*נבחר|הגרלה\\s*בלעדית|(?:לוטו|גריד|מפעל\\s*הפיס|פיס).*זכ"), 0.70f, "Hebrew lottery scam"),
        Rule("debt_collection", Regex("עיקול.*חשבונ|צו\\s*הוצאה\\s*לפועל|חוב\\s*בסכ\\s*\\d|גביית\\s*חובות|חוב\\s*פתוח|הסדר\\s*חוב"), 0.65f, "Debt collection fraud"),
        Rule("health_diet", Regex("דיאטה\\s*מהירה|שריפת\\s*שומנ|ירידה\\s*של\\s*\\d+\\s*קילו|סוד\\s*הרזיה|תוספ\\s*מהפכני|ירידה\\s*במשקל.*ללא"), 0.60f, "Health/diet scam"),
        Rule("real_estate", Regex("השקעה\\s*בנדלנ|מחיר\\s*למשתכנ|נדלנ\\s*להשקעה|פרויקט\\s*חדש.*דירות|דירה\\s*מפנה"), 0.50f, "Real estate spam"),
        Rule("car_accident", Regex("תאונת\\s*דרכימ.*פיצוי|נפגעת\\s*בתאונה|תביעת\\s*נזיקינ|זכויות\\s*נפגעי\\s*תאונות|עורכ\\s*דינ\\s*תאונות"), 0.60f, "Car accident claim"),
        Rule("charity_scam", Regex("תרומה\\s*דחופה|תרמו\\s*עכשיו|ילדימ\\s*חולימ|עזרה\\s*לנזקקימ|תרומה\\s*חד\\s*פעמית"), 0.55f, "Charity scam"),
        Rule("utility_fraud", Regex("חברת\\s*החשמל.*ניתוק|ניתוק\\s*חשמל|חוב\\s*לעירייה|ארנונה.*חוב|חשבונ\\s*מימ.*חוב|מקורות.*חוב"), 0.60f, "Utility fraud"),
        Rule("carrier_scam", Regex("חבילת\\s*גלישה.*חינמ|גיגה\\s*חינמ|שדרוג\\s*חינמ.*סלולר|(?i)(Partner|Cellcom|HOT|Pelephone|We4G|Golan).*חבילה\\s*בלעדית"), 0.55f, "Carrier scam"),
        Rule("legal_scam", Regex("תביעה\\s*ייצוגית|זכאות\\s*לפיצוי|זכויות\\s*צרכנימ|פיצוי\\s*כספי.*לחצ"), 0.55f, "Legal scam"),
        Rule("commercial_spam", Regex("מבצע\\s*חד\\s*פעמי|מכירת\\s*חיסול|מבצע\\s*בלעדי|הנחה\\s*של\\s*\\d+%|קופונ\\s*הנחה"), 0.45f, "Commercial spam"),
        Rule("chain_message", Regex("העבר\\s*ל-?\\d+\\s*אנשימ|שלח\\s*לכל\\s*אנשי\\s*הקשר|העבר\\s*הלאה|שתפ\\s*עמ\\s*חברימ"), 0.60f, "Chain message"),
        Rule("service_impersonation", Regex("(?i)(Wolt|10bis|תנ\\s*ביס).*הזמנ|(?i)(Gett|Yango).*נסיעה|(?i)(Wolt|10bis|Gett|Yango|Waze).*לחצ\\s*כאנ"), 0.60f, "Service impersonation")
    )

    data class ScoreResult(
        val score: Float,
        val reasons: List<String>,
        val triggeredRules: List<TriggeredRule>
    )

    fun score(normalizedBody: String, hasUrl: Boolean): ScoreResult {
        if (normalizedBody.length < 5) return ScoreResult(0f, emptyList(), emptyList())
        val reasons = mutableListOf<String>()
        val triggered = mutableListOf<TriggeredRule>()
        var totalScore = 0f
        for (rule in RULES) {
            val match = rule.pattern.find(normalizedBody) ?: continue
            totalScore += rule.score
            reasons.add(rule.description)
            triggered.add(TriggeredRule(rule.name, "KEYWORD", match.value, rule.score))
        }
        if (totalScore > 0f && hasUrl && normalizedBody.contains("קוד")) {
            val otpUrlBonus = 0.1f
            totalScore += otpUrlBonus
            reasons.add("OTP with URL")
        }
        return ScoreResult(totalScore.coerceAtMost(1f), reasons, triggered)
    }
}
