#!/usr/bin/env python3
"""
Train a TFLite spam classifier for NovaChat.

Generates ~3,000+ synthetic Hebrew/English spam+ham examples across 35 categories,
trains a Keras model, and exports spam_classifier.tflite + spam_vocab.txt to the
Android assets folder.

Usage:
    pip install tensorflow numpy
    python train_spam_model.py
"""

import json
import os
import random
import re
import unicodedata
from pathlib import Path

import numpy as np

ASSETS_DIR = Path("NovaChat/app/src/main/assets")
MAX_SEQ_LEN = 128
VOCAB_SIZE = 8000
PAD_TOKEN = 0
UNK_TOKEN = 1

# ── Hebrew text normalizer (mirrors HebrewTextNormalizer.kt) ─────────────

SOFIT_MAP = {
    "\u05DA": "\u05DB",  # ך → כ
    "\u05DD": "\u05DE",  # ם → מ
    "\u05DF": "\u05E0",  # ן → נ
    "\u05E3": "\u05E4",  # ף → פ
    "\u05E5": "\u05E6",  # ץ → צ
}

DIGIT_TO_HEBREW = {"0": "ו", "5": "ש", "4": "ד", "7": "ז", "1": "א"}


def normalize_hebrew(text: str) -> str:
    t = text[:2000]
    t = re.sub(r"[\u200B-\u200D\u2060\uFEFF]+", "", t)
    t = re.sub(r"[\u200E\u200F\u202A-\u202E\u2066-\u2069]+", "", t)
    t = "".join(c if ord(c) < 0xFF01 or ord(c) > 0xFF5E else chr(ord(c) - 0xFF01 + 0x21) for c in t)
    t = re.sub(
        r"[\u2600-\u26FF\u2700-\u27BF\u231A\u231B]|"
        r"[\U0001F300-\U0001F9FF]",
        " ", t,
    )
    t = "".join(c for c in t if not (0x0591 <= ord(c) <= 0x05C7))
    t = "".join(SOFIT_MAP.get(c, c) for c in t)
    for _ in range(5):
        new_t = re.sub(
            r"([\u0590-\u05FF\w])([\s\-*·_•\u2002\u2003\u2009\u00A0]+)([\u0590-\u05FF\w])",
            r"\1\3", t,
        )
        if new_t == t:
            break
        t = new_t
    if any("\u05D0" <= c <= "\u05EA" for c in t):
        for digit, heb in DIGIT_TO_HEBREW.items():
            t = t.replace(digit, heb)
    t = t.strip()
    t = re.sub(r"\s+", " ", t)
    return t


def preprocess(text: str) -> str:
    has_hebrew = any("\u0590" <= c <= "\u05FF" for c in text)
    t = text[:2000]
    if has_hebrew:
        t = normalize_hebrew(t)
    t = t.lower()
    t = re.sub(r"https?://\S+", " <URL> ", t)
    t = re.sub(r"\b\d{4,}\b", " <NUM> ", t)
    t = re.sub(r"[^\w<>\s]", " ", t)
    t = re.sub(r"\s+", " ", t).strip()
    return t


# ── Synthetic data generation ────────────────────────────────────────────

def pick(*args):
    return random.choice(args)


def rand_amount():
    return str(random.choice([500, 1000, 2000, 3000, 5000, 7500, 10000, 15000, 20000, 50000, 100000]))


def rand_percent():
    return str(random.choice([10, 15, 20, 25, 30, 40, 50, 60, 70, 80]))


BANKS = ["לאומי", "הפועלים", "דיסקונט", "מזרחי טפחות", "יהב", "מסד", "ירושלים", "פפר", "וואן זירו"]
CREDIT_CARDS = ["כאל", "ישראכרט", "מקס", "ויזה כאל", "לאומי קארד"]
DELIVERY_SERVICES = ["דואר ישראל", "DHL", "UPS", "FedEx", "Amazon", "AliExpress", "Shein", "Temu", "iHerb", "eBay"]
CARRIERS = ["Partner", "Cellcom", "HOT Mobile", "Pelephone", "We4G", "Golan Telecom"]
SERVICES = ["Wolt", "10bis", "Gett", "Yango", "Waze"]
PARTIES = ["ליכוד", "יש עתיד", "מחנה ממלכתי", "הציונות הדתית", "שס", "יהדות התורה", "העבודה", "מרצ"]
GOV_BODIES = ["רשות המיסים", "ביטוח לאומי", "משרד הבריאות", "משטרת ישראל", "פיקוד העורף", "עיריית תל אביב"]
URLS = ["bit.ly/abc123", "tinyurl.com/xyz", "t.co/aBcD", "cutt.ly/spam", "https://192.168.1.1/login"]
NAMES_HE = ["דני", "רונית", "יוסי", "מיכל", "אבי", "שירה", "עמית", "נועה", "אורי", "תמר"]
FOODS = ["פיצה", "סושי", "שווארמה", "המבורגר", "פלאפל", "חומוס"]


def gen_phishing():
    templates = [
        f"לקוח יקר, חשבון הבנק שלך בבנק {pick(*BANKS)} נחסם. לחץ כאן לעדכון פרטים: {pick(*URLS)}",
        f"זוהתה פעילות חריגה בחשבונך בבנק {pick(*BANKS)}. יש לאמת את זהותך מיידית",
        f"חשבונך ב{pick(*CREDIT_CARDS)} יחסם תוך 24 שעות. עדכן פרטים: {pick(*URLS)}",
        f"הודעת אבטחה: העברה חשודה של {rand_amount()} ש\"ח מחשבונך בבנק {pick(*BANKS)}",
        f"עדכון פרטי חשבון נדרש ב{pick(*CREDIT_CARDS)} לחץ כאן: {pick(*URLS)}",
        f"כרטיס האשראי שלך ב{pick(*CREDIT_CARDS)} חסום עקב פעילות חריגה",
        f"פרטי חשבונך ב{pick(*BANKS)} פגו. לחץ כאן לאימות זהות: {pick(*URLS)}",
        f"אבטחת חשבון: חשבונך בבנק {pick(*BANKS)} דורש אימות מחדש",
        "Your account has been suspended. Click here to verify: " + pick(*URLS),
        "We detected unusual activity on your account. Verify your identity now",
        "Your password has been compromised. Reset immediately: " + pick(*URLS),
        "Update your payment details to avoid account suspension",
        f"לקוח יקר, בנק {pick(*BANKS)} מודיע על חסימת חשבונך",
        f"פעילות חריגה בכרטיס {pick(*CREDIT_CARDS)} שלך. לחץ לבדיקה: {pick(*URLS)}",
    ]
    return pick(*templates)


def gen_otp_fraud():
    templates = [
        "שלחתי לך קוד בטעות, תשלח לי אותו בבקשה",
        "קיבלת קוד אימות בן 6 ספרות? שלח לי אותו בבקשה",
        "שלח לי את הקוד שקיבלת עכשיו בהודעה",
        "קוד חד פעמי הגיע אליך בטעות. שלח לי בבקשה",
        "הזן את הקוד שקיבלת: סיסמה זמנית לאימות דו שלבי",
        "קוד זמני לאימות חשבונך. שלח לי בבקשה",
        "Please share the OTP code you just received",
        "I accidentally sent my verification code to your number, please send it back",
        "Send me the code you received, it was sent to you by mistake",
        "קוד האימות שלך הגיע בטעות אלי, שלח בבקשה",
    ]
    return pick(*templates)


def gen_prize():
    templates = [
        f"מזל טוב! זכית בפרס כספי של {rand_amount()} ש\"ח. לחץ כאן: {pick(*URLS)}",
        f"הגרלה בלעדית: נבחרת לקבל קופון מתנה בסך {rand_amount()} ש\"ח",
        f"זכית בהגרלה! שובר מתנה של {rand_amount()} ש\"ח מחכה לך",
        "Congratulations! You have won $5000. Claim your prize now: " + pick(*URLS),
        "Your number was selected for our exclusive giveaway. Free iPhone 15!",
        "Claim your reward of $1000 gift card. Click: " + pick(*URLS),
        "פרס כספי ענק מחכה לך! מזל טוב נבחרת",
        f"זכית ב{pick('שובר', 'קופון', 'כרטיס')} מתנה. לחץ כאן: {pick(*URLS)}",
    ]
    return pick(*templates)


def gen_job():
    templates = [
        f"עבודה מהבית - הכנסה נוספת של {rand_amount()} ש\"ח בחודש ללא ניסיון",
        "משרה דחופה! שכר גבוה מהבית. ללא ניסיון קודם",
        f"הרוויחו מהבית {rand_amount()} ש\"ח ביום. עבודה קלה",
        f"Work from home and earn ${rand_amount()} per week. No experience needed!",
        "Hiring now! Make $500 daily from anywhere. Apply: " + pick(*URLS),
        "Easy money online! $300 per hour data entry from home",
        f"עבודה מהבית - {rand_amount()} שקלים ביום. הצטרפו עכשיו",
        "הכנסה נוספת ללא ניסיון. הרשמו: " + pick(*URLS),
    ]
    return pick(*templates)


def gen_loan():
    templates = [
        f"הלוואה מיידית עד {rand_amount()} ש\"ח. ריבית אפס. ללא ערבים",
        f"זכאות להלוואה של {rand_amount()} ש\"ח בתנאים מיוחדים! כנסו מהר",
        "הלוואה חוץ בנקאית ללא בדיקת BDI. מימון מיידי",
        f"הלוואה אושרה על סך {rand_amount()} ש\"ח. לחץ כאן: {pick(*URLS)}",
        f"משכנתא בריבית אפס! עד {rand_amount()} ש\"ח. ללא ערבים",
        "Pre-approved loan of $5000! No credit check. Apply now: " + pick(*URLS),
        "Guaranteed instant loan. Bad credit OK. Click: " + pick(*URLS),
        f"קח הלוואה היום - עד {rand_amount()} ש\"ח בתנאים מיוחדים",
    ]
    return pick(*templates)


def gen_delivery():
    service = pick(*DELIVERY_SERVICES)
    templates = [
        f"החבילה שלך מ{service} ממתינה לאישור מכס. לחץ כאן: {pick(*URLS)}",
        f"{service}: חבילתך בדרך. תשלום משלוח נדרש: {pick(*URLS)}",
        f"הודעה מ{service}: משלוח ממתין לאיסוף. מספר מעקב: {pick(*URLS)}",
        f"שחרור חבילה מ{service} דורש תשלום מכס. לחץ: {pick(*URLS)}",
        "Your package couldn't be delivered. Track it: " + pick(*URLS),
        "Your shipment from Amazon has been delayed. Update delivery: " + pick(*URLS),
        f"חבילה ממתינה ל{pick('איסוף', 'אישור', 'שחרור')} ב{service}",
        f"סטטוס הזמנה: החבילה שלך מ{service} בדרך",
    ]
    return pick(*templates)


def gen_tax_refund():
    templates = [
        f"מגיע לך החזר מס של {rand_amount()} ש\"ח! בדיקה ללא תשלום",
        f"ממוצע החזרי מס לשנת 2025: {rand_amount()} ש\"ח. בדוק עכשיו",
        f"החזרי מס לאזרחי ישראל - עד {rand_amount()} ש\"ח. לחץ: {pick(*URLS)}",
        f"זיכוי מס - מגיע לך {rand_amount()} ש\"ח. בדיקה חינם: {pick(*URLS)}",
        f"דוח שנתי: ניכוי מס בסך {rand_amount()} ש\"ח מחכה לך",
        f"מס הכנסה: החזר מס של {rand_amount()} ש\"ח אושר. לחץ כאן",
        f"בדיקה חינם לשכירים - החזר מס ממוצע {rand_amount()} ש\"ח",
    ]
    return pick(*templates)


def gen_pension():
    templates = [
        f"כספים תקועים בקרן פנסיה! משוך עד {rand_amount()} ש\"ח",
        "ביטוח מנהלים - זכויות פנסיוניות שלא קיבלת. בדוק עכשיו",
        "קופת גמל: פנסיה מוקפאת מחכה למשיכה. לחץ: " + pick(*URLS),
        f"פיצויים ללא התפטרות - עד {rand_amount()} ש\"ח. בדיקה חינם",
        "ביטוח לאומי מענק חד פעמי! בדוק זכאות: " + pick(*URLS),
        f"משיכת כספים מפנסיה: {rand_amount()} ש\"ח מחכים לך",
        "זכויות פנסיוניות שלא ידעת עליהן. בדוק עכשיו",
    ]
    return pick(*templates)


def gen_political():
    party = pick(*PARTIES)
    templates = [
        f"הצביעו עכשיו ל{party}! רק {party} יציל את המדינה",
        f"בבחירות הקרובות בוחרים {party}. יום הבחירות מתקרב",
        f"חשיפה דרמטית: ראש האופוזיציה נתפס ב...",
        f"סקר: {party} מוביל עם {random.randint(20, 40)} מנדטים",
        f"עצומה: חתמו על העצומה נגד {pick('הרפורמה', 'החוק', 'הממשלה')}",
        "קואליציה או אופוזיציה? הצביעו בסקר: " + pick(*URLS),
        f"חשיפה של פוליטיקאי בכיר, להצביע עכשיו ל{party}",
        "רפורמה משפטית - השלכות חמורות! חתמו על העצומה",
    ]
    return pick(*templates)


def gen_money_waiting():
    templates = [
        f"אלפי שקלים מחכים לך! {rand_amount()} ש\"ח. לחץ: {pick(*URLS)}",
        f"הודעה דחופה: {rand_amount()} שקלים מחכים לך",
        "זכויות שלא קיבלת - כספים מחכים למשיכה",
        f"מחכים לך {rand_amount()} שקלים שלא ידעת עליהם",
        f"כסף מחכה לך: {rand_amount()} ש\"ח. בדוק עכשיו: {pick(*URLS)}",
    ]
    return pick(*templates)


def gen_medical_disability():
    templates = [
        "בעיה רפואית? קבל תו נכה לחניה! ללא תור",
        "נמאס לך להחנות רחוק? תו נכה בקלות - להילחם במערכת",
        "נכות רפואית - בדוק זכאות לתג חניה לנכים",
        "ועדה רפואית: אחוזי נכות גבוהים. בדוק זכאות",
        "דרגת נכות? קבל זכויות רפואיות שמגיעות לך",
        "תו נכה + זכויות רפואיות. להילחם במערכת: " + pick(*URLS),
    ]
    return pick(*templates)


def gen_propaganda():
    templates = [
        "משקרים לכם! הממשלה מסתירה את האמת",
        "הטילים בדרך! חפשו מקלט ביטחון מיד",
        "תברחו מהארץ לפני שיהיה מאוחר מדי",
        "משמרות המהפכה מתקרבים. אף אחד לא יכול לספק ביטחון",
        "מידע מטעה מופץ על ידי גורמים עוינים",
        "הסתה ותעמולה ברשתות. מניפולציה של דעת הקהל",
        "מקלט ביטחון: ברחו לפני שיגמר הזמן",
    ]
    return pick(*templates)


def gen_crypto():
    templates = [
        f"קריפטו: השקעה מובטחת של {rand_amount()} ש\"ח. רווח כפול",
        "ביטקוין - הכנסה פסיבית מובטחת! הצטרף: " + pick(*URLS),
        f"אתריום NFT - תשואה מובטחת של {rand_percent()}%. טריידינג מקצועי",
        "פורקס FOREX: סיגנלים למסחר. רווח בבורסה מובטח",
        "Bitcoin investment opportunity! Double your money: " + pick(*URLS),
        "Crypto trading signals. Guaranteed 500% ROI",
        "השקעה בקריפטו - מטבעות דיגיטליים עם תשואה מובטחת",
    ]
    return pick(*templates)


def gen_impersonation():
    body = pick(*GOV_BODIES)
    templates = [
        f"{body}: הודעה חשובה - תגובה נדרשת. לחץ: {pick(*URLS)}",
        f"בית משפט: הוצאה לפועל - הודעה דחופה בעניינך",
        f"ביטוח לאומי: זכאות למענק. בדוק: {pick(*URLS)}",
        f"משרד הבריאות: הודעה חשובה בנוגע לבריאותך",
        f"עיריית תל אביב: חוב ארנונה. שלם לפני עיקול",
        f"רשות מקרקעי ישראל: הודעה חשובה בעניינך",
    ]
    return pick(*templates)


def gen_insurance():
    templates = [
        f"ביטוח רכב - חסוך {rand_percent()}%! השוואת ביטוחים חינם",
        f"ביטוח חיים - הצעה בלעדית. חסוך על ביטוח: {pick(*URLS)}",
        "ביטוח דירה + ביטוח בריאות במחיר מיוחד!",
        "חידוש ביטוח? השוואת ביטוחים ב30 שניות: " + pick(*URLS),
        "פוליסה חדשה - ביטוח משתלם וזול. חסוך אלפים",
        "ביטוח בריאות פרטי - מחירים מיוחדים לזמן מוגבל",
    ]
    return pick(*templates)


def gen_gambling():
    templates = [
        "קזינו אונליין - הרוויח עכשיו! הפקדה ראשונה + בונוס",
        "הימורים ספורט: ספורטבט - הצטרף וקבל בונוס: " + pick(*URLS),
        "פוקר אונליין - שחק רולטה. הפקדה ראשונה כפולה",
        "1xbet: הרשם וקבל בונוס הפקדה ראשונה. קזינו: " + pick(*URLS),
        "bet365 - הימורים אונליין. רולטה + פוקר. הפקדה ראשונה בונוס",
    ]
    return pick(*templates)


def gen_hebrew_lottery():
    templates = [
        f"זכית בהגרלה! פרס ראשון: {rand_amount()} ש\"ח. מספרך נבחר",
        f"הגרלה בלעדית של מפעל הפיס. זכית ב{rand_amount()} ש\"ח!",
        "לוטו: מספרך נבחר. גריד - פרס ראשון. לחץ: " + pick(*URLS),
        f"פיס: זכית בהגרלה בלעדית! {rand_amount()} ש\"ח מחכים",
        f"מפעל הפיס מודיע: זכית בפרס של {rand_amount()} ש\"ח",
    ]
    return pick(*templates)


def gen_debt_collection():
    templates = [
        f"חוב בסך {rand_amount()} ש\"ח. עיקול חשבון תוך 48 שעות",
        "צו הוצאה לפועל: תשלום מיידי נדרש. חוב פתוח",
        f"גביית חובות: חוב פתוח של {rand_amount()} ש\"ח. שלם עכשיו",
        f"הסדר חוב: שלם {rand_amount()} ש\"ח ומנע עיקול. לחץ: {pick(*URLS)}",
        "עיקול חשבון בנק בתוך 24 שעות. חוב פתוח. תשלום מיידי",
    ]
    return pick(*templates)


def gen_health_diet():
    kg = random.randint(5, 20)
    templates = [
        f"דיאטה מהירה - ירידה של {kg} קילו בשבועיים!",
        "שריפת שומן מהירה! תוסף טבעי מהפכני: " + pick(*URLS),
        f"ירידה במשקל ללא דיאטה. סוד הרזיה: {kg} קילו בחודש",
        f"תוסף מהפכני לירידה של {kg} קילו. בלי דיאטה רזה: {pick(*URLS)}",
        "ירידה במשקל מהירה - שריפת שומן טבעית. ללא תופעות לוואי",
    ]
    return pick(*templates)


def gen_real_estate():
    templates = [
        f"השקעה בנדלן - דירות למכירה מ{rand_amount()} ש\"ח",
        "פרויקט חדש: דירות מחיר למשתכן. הירשמו: " + pick(*URLS),
        "נדלן להשקעה - תשואה שנתית מובטחת. דירה מפנה",
        f"דירות למכירה בפרויקט חדש - מ{rand_amount()} ש\"ח",
        "השקעה בנדלן: פרויקט חדש דירות למכירה. מחיר למשתכן",
    ]
    return pick(*templates)


def gen_car_accident():
    templates = [
        f"תאונת דרכים? פיצוי מביטוח של עד {rand_amount()} ש\"ח",
        "נפגעת בתאונה? תביעת נזיקין - עורך דין תאונות",
        f"זכויות נפגעי תאונות דרכים: פיצוי של {rand_amount()} ש\"ח",
        "עורך דין תאונות דרכים - פיצוי מביטוח: " + pick(*URLS),
        "נפגעת? תביעת נזיקין מול הביטוח. ייעוץ חינם",
    ]
    return pick(*templates)


def gen_charity():
    templates = [
        "תרומה דחופה: ילדים חולים זקוקים לעזרתך. תרמו עכשיו",
        f"ארגון צדקה - עזרה לנזקקים. תרומה חד פעמית של {random.choice([18, 36, 50, 100, 180])} ש\"ח",
        "תרמו עכשיו! ילדים חולים מחכים לעזרה: " + pick(*URLS),
        "תרומה דחופה לארגון צדקה. עזרה לנזקקים",
        "ילדים חולים - תרומה חד פעמית. תרמו: " + pick(*URLS),
    ]
    return pick(*templates)


def gen_utility_fraud():
    templates = [
        f"חברת החשמל: ניתוק חשמל תוך 24 שעות. חוב: {rand_amount()} ש\"ח",
        f"חוב לעירייה - ארנונה בסך {rand_amount()} ש\"ח. שלם או עיקול",
        f"חשבון מים: חוב של {rand_amount()} ש\"ח. מקורות: {pick(*URLS)}",
        "ניתוק חשמל! שלם את חשבון החשמל: " + pick(*URLS),
        f"ארנונה: חוב של {rand_amount()} ש\"ח לעירייה. שלם מיד",
    ]
    return pick(*templates)


def gen_carrier():
    carrier = pick(*CARRIERS)
    templates = [
        f"{carrier}: חבילת גלישה חינם! גיגה חינם ל30 יום",
        f"שדרוג חינם סלולר ב{carrier}. חבילה בלעדית: {pick(*URLS)}",
        f"{carrier}: חבילה בלעדית - גיגה חינם + שיחות ללא הגבלה",
        f"הצעה מ{carrier}: שדרוג חינם לחבילת גלישה. לחץ: {pick(*URLS)}",
    ]
    return pick(*templates)


def gen_legal():
    templates = [
        f"תביעה ייצוגית: זכאי לפיצוי של {rand_amount()} ש\"ח. בדוק: {pick(*URLS)}",
        "זכאות לפיצוי - זכויות צרכנים שלא ידעת. לחץ כאן",
        f"פיצוי כספי של {rand_amount()} ש\"ח מחכה לך. תביעה ייצוגית",
        "זכויות צרכנים: האם קיבלת פיצוי? לחץ כאן: " + pick(*URLS),
    ]
    return pick(*templates)


def gen_commercial():
    templates = [
        f"מבצע חד פעמי! הנחה של {rand_percent()}% רק היום",
        "מכירת חיסול - מבצע בלעדי. קופון הנחה: " + pick(*URLS),
        "BLACK FRIDAY: הנחה של 50% על הכל! מוגבל בזמן",
        f"מבצע בלעדי: הנחה של {rand_percent()}%. קופון הנחה בפנים",
        "מכירת חיסול! מבצע חד פעמי - רק היום. לחץ: " + pick(*URLS),
    ]
    return pick(*templates)


def gen_chain():
    templates = [
        "הודעה דחופה! העבר ל-10 אנשים. שלח לכל אנשי הקשר",
        "העבר הלאה! הודעה דחופה - שתף עם חברים",
        "שלח לכל אנשי הקשר שלך. הודעה חשובה!",
        "העבר ל-5 אנשים ותקבל מתנה. שתף עם חברים",
        "הודעה דחופה מהממשלה. העבר הלאה לכל אנשי הקשר!",
    ]
    return pick(*templates)


def gen_service_impersonation():
    service = pick(*SERVICES)
    templates = [
        f"{service}: ההזמנה שלך בדרך. משלוח בדרך. לחץ כאן: {pick(*URLS)}",
        f"הזמנה מ{service} מחכה לאישור. לחץ כאן: {pick(*URLS)}",
        f"{service}: עדכון חשוב בנוגע להזמנתך. לחץ: {pick(*URLS)}",
        f"10bis: ההזמנה שלך מוכנה. אשר: {pick(*URLS)}",
        f"Gett: נסיעה מחכה לך. אשר: {pick(*URLS)}",
    ]
    return pick(*templates)


def gen_advance_fee():
    templates = [
        "You have inherited $5,000,000 from a distant relative. Pay processing fee: " + pick(*URLS),
        "Unclaimed funds of $2,000,000 in your name. Contact us to claim",
        "A diplomat has a package worth $1M for you. Pay shipping fee",
        "Processing fee of $50 required to release your inheritance",
    ]
    return pick(*templates)


def gen_tech_support():
    templates = [
        "Virus detected on your device! Call Microsoft support now: 1-800-XXX",
        "Your computer has been hacked. Contact Apple support immediately",
        "Malware found! Click here to clean: " + pick(*URLS),
        "Windows security alert: trojan detected. Call now",
    ]
    return pick(*templates)


def gen_investment():
    templates = [
        "Guaranteed 100% return on investment. Risk free! Join now",
        "Trading signals group on Telegram. Guaranteed profits",
        "WhatsApp group for investment signals. 500% ROI: " + pick(*URLS),
        f"תשואה מובטחת של {rand_percent()}% על השקעה. סיגנלים למסחר",
    ]
    return pick(*templates)


def gen_subscription():
    templates = [
        "Your Netflix subscription will be charged $49.99. Cancel: " + pick(*URLS),
        "Auto-renewal of $99/month. Cancel now to avoid charges",
        "Subscription payment of $199 pending. Stop: " + pick(*URLS),
    ]
    return pick(*templates)


def gen_romance():
    templates = [
        "I'm stuck at the airport and need money urgently. Please wire $500",
        "Emergency: send gift card codes. I'm stranded in hospital",
        "Please transfer Bitcoin. Emergency - I'm stuck abroad",
    ]
    return pick(*templates)


def gen_tax_scam():
    templates = [
        "IRS notice: legal action pending. Pay your fine: " + pick(*URLS),
        "Tax warrant issued. Pay immediately or face arrest",
        "Government fine: failure to comply will result in legal action",
    ]
    return pick(*templates)


def gen_urgent():
    templates = [
        "Act now! This offer expires today. הזדמנות אחרונה",
        "Respond immediately or your account will be closed",
        f"Final warning: {rand_amount()} ש\"ח - נותרו שעות. מוגבל בזמן",
        "לפני שיגמר! עד חצות בלבד. פעולה מיידית נדרשת",
    ]
    return pick(*templates)


# ── Legitimate (ham) messages ────────────────────────────────────────────

def gen_ham():
    name = pick(*NAMES_HE)
    food = pick(*FOODS)
    templates = [
        f"אמא אני מגיע עוד חצי שעה",
        f"היי {name}, מה נשמע? רוצה להיפגש היום?",
        f"הקוד שלך לכניסה הוא {random.randint(100000, 999999)}",
        "פגישה נדחתה למחר בשעה 10",
        f"אני בדרך, אהיה שם בעוד 15 דקות",
        f"תזכורת: פגישה עם {name} מחר בשעה {random.randint(8, 18)}",
        f"ההזמנה שלך מ{food} מוכנה! בואו לאסוף",
        f"היי, קניתי {food} לארוחת ערב. בואו",
        f"שבת שלום {name}! נתראה בשבוע הבא",
        f"תודה על ההודעה, אחזור אליך מאוחר יותר",
        "בדקתי ואפשר להיפגש ביום שלישי",
        f"אבא, תוכל לאסוף אותי מבית הספר ב-{random.randint(13, 16)}?",
        "ילדים, שיעורי בית עד מחר. אל תשכחו!",
        f"הזמנתי שולחן לארוחת ערב בשעה {random.randint(18, 21)}",
        f"נולד לנו בן! {random.choice([3.2, 3.5, 3.8, 4.0])} ק\"ג",
        f"Hey {name}, are you coming to the meeting at 3pm?",
        "Running late, be there in 10 minutes",
        "Can you pick up milk on the way home?",
        "Happy birthday! Hope you have a great day!",
        f"Your appointment is confirmed for March {random.randint(1, 31)}",
        f"Your order #{random.randint(10000, 99999)} has been shipped",
        "Thanks for the help yesterday. Really appreciate it!",
        "See you at the gym tomorrow morning",
        f"מחר בבוקר יש לנו פגישת צוות בשעה {random.randint(8, 11)}",
        f"שכחתי להגיד, {name} מגיע/ה אלינו לאכול",
        "בוקר טוב! יום טוב",
        "מישהו רוצה קפה? אני הולך למכונה",
        f"הרופא שלך רשם תרופה חדשה. {name}, תיקח בבוקר",
        f"מזל טוב ליום הולדת! {name} אוהב/ת אותך",
        f"חזרנו מהטיול! היה מדהים. תמונות בדרך",
        "שלום, אני מ{} לגבי הפגישה שלנו".format(pick("המשרד", "העבודה", "הגן")),
        f"הילד חולה, לא יהיה בגן מחר",
        f"עברנו דירה! הכתובת החדשה: רחוב הרצל {random.randint(1, 200)}",
        "חשבון החשמל השנה ירד. כל הכבוד",
        f"Your verification code is {random.randint(100000, 999999)}",
        "Delivery confirmed. Thank you for your order!",
        "Meeting rescheduled to 4pm tomorrow",
        "Great news! Your application has been approved",
        "Reminder: dentist appointment tomorrow at 2:30",
        f"Hey, want to grab {pick('lunch', 'coffee', 'dinner')} later?",
    ]
    return pick(*templates)


# ── Build dataset ────────────────────────────────────────────────────────

SPAM_GENERATORS = [
    ("phishing", gen_phishing, 80),
    ("otp_fraud", gen_otp_fraud, 60),
    ("prize_scam", gen_prize, 60),
    ("job_scam", gen_job, 60),
    ("loan_scam", gen_loan, 60),
    ("delivery_scam", gen_delivery, 60),
    ("tax_refund", gen_tax_refund, 60),
    ("pension_severance", gen_pension, 55),
    ("political_spam", gen_political, 60),
    ("money_waiting", gen_money_waiting, 50),
    ("medical_disability", gen_medical_disability, 50),
    ("propaganda", gen_propaganda, 55),
    ("crypto_scam", gen_crypto, 55),
    ("impersonation", gen_impersonation, 50),
    ("insurance_spam", gen_insurance, 50),
    ("gambling_spam", gen_gambling, 50),
    ("hebrew_lottery", gen_hebrew_lottery, 50),
    ("debt_collection", gen_debt_collection, 50),
    ("health_diet", gen_health_diet, 50),
    ("real_estate", gen_real_estate, 45),
    ("car_accident", gen_car_accident, 45),
    ("charity_scam", gen_charity, 45),
    ("utility_fraud", gen_utility_fraud, 50),
    ("carrier_scam", gen_carrier, 45),
    ("legal_scam", gen_legal, 45),
    ("commercial_spam", gen_commercial, 50),
    ("chain_message", gen_chain, 45),
    ("service_impersonation", gen_service_impersonation, 45),
    ("advance_fee", gen_advance_fee, 40),
    ("tech_support", gen_tech_support, 40),
    ("investment_scam", gen_investment, 45),
    ("subscription_scam", gen_subscription, 35),
    ("romance_scam", gen_romance, 35),
    ("tax_scam", gen_tax_scam, 35),
    ("urgent_action", gen_urgent, 40),
]

HAM_COUNT = 1100


def build_dataset():
    random.seed(42)
    examples = []

    for cat_name, gen_fn, count in SPAM_GENERATORS:
        for _ in range(count):
            text = gen_fn()
            examples.append((text, 1, cat_name))

    for _ in range(HAM_COUNT):
        text = gen_ham()
        examples.append((text, 0, "legitimate"))

    random.shuffle(examples)
    print(f"Generated {len(examples)} examples ({sum(1 for _, l, _ in examples if l == 1)} spam, "
          f"{sum(1 for _, l, _ in examples if l == 0)} ham)")
    return examples


# ── Vocabulary building ──────────────────────────────────────────────────

def build_vocab(texts, max_vocab=VOCAB_SIZE - 2):
    word_freq = {}
    for text in texts:
        processed = preprocess(text)
        for token in processed.split():
            if token:
                word_freq[token] = word_freq.get(token, 0) + 1

    sorted_words = sorted(word_freq.items(), key=lambda x: -x[1])
    vocab = {}
    for i, (word, _) in enumerate(sorted_words[:max_vocab]):
        vocab[word] = i + 2  # 0=PAD, 1=UNK
    print(f"Vocabulary size: {len(vocab) + 2} (including PAD and UNK)")
    return vocab


def text_to_indices(text, vocab):
    processed = preprocess(text)
    tokens = processed.split()
    indices = []
    for token in tokens:
        indices.append(vocab.get(token, UNK_TOKEN))
    result = np.zeros(MAX_SEQ_LEN, dtype=np.float32)
    length = min(len(indices), MAX_SEQ_LEN)
    for i in range(length):
        result[i] = indices[i]
    return result


# ── Model training ───────────────────────────────────────────────────────

def train_model(examples):
    import tensorflow as tf

    texts = [e[0] for e in examples]
    labels = np.array([e[1] for e in examples], dtype=np.float32)

    vocab = build_vocab(texts)

    X = np.array([text_to_indices(t, vocab) for t in texts])
    y = labels

    split = int(len(X) * 0.85)
    indices = np.arange(len(X))
    np.random.seed(42)
    np.random.shuffle(indices)
    X = X[indices]
    y = y[indices]

    X_train, X_val = X[:split], X[split:]
    y_train, y_val = y[:split], y[split:]

    print(f"Training: {len(X_train)} examples, Validation: {len(X_val)} examples")

    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(MAX_SEQ_LEN,)),
        tf.keras.layers.Embedding(VOCAB_SIZE, 32, input_length=MAX_SEQ_LEN),
        tf.keras.layers.GlobalAveragePooling1D(),
        tf.keras.layers.Dense(64, activation="relu"),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(32, activation="relu"),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(1, activation="sigmoid"),
    ])

    model.compile(
        optimizer="adam",
        loss="binary_crossentropy",
        metrics=["accuracy"],
    )

    model.summary()

    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor="val_loss", patience=5, restore_best_weights=True
    )

    model.fit(
        X_train, y_train,
        epochs=30,
        batch_size=32,
        validation_data=(X_val, y_val),
        callbacks=[early_stop],
        verbose=1,
    )

    val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
    print(f"\nValidation accuracy: {val_acc:.4f}, loss: {val_loss:.4f}")

    return model, vocab


def export_tflite(model, vocab):
    import tensorflow as tf

    os.makedirs(ASSETS_DIR, exist_ok=True)

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()

    tflite_path = ASSETS_DIR / "spam_classifier.tflite"
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"Saved TFLite model: {tflite_path} ({len(tflite_model) / 1024:.1f} KB)")

    sorted_vocab = sorted(vocab.items(), key=lambda x: x[1])
    vocab_path = ASSETS_DIR / "spam_vocab.txt"
    with open(vocab_path, "w", encoding="utf-8") as f:
        for word, _ in sorted_vocab:
            f.write(word + "\n")
    print(f"Saved vocabulary: {vocab_path} ({len(sorted_vocab)} tokens)")


def main():
    print("=" * 60)
    print("NovaChat Spam TFLite Model Trainer")
    print("=" * 60)

    examples = build_dataset()
    model, vocab = train_model(examples)
    export_tflite(model, vocab)

    print("\nDone! Files exported to:", ASSETS_DIR)
    print("  - spam_classifier.tflite")
    print("  - spam_vocab.txt")


if __name__ == "__main__":
    main()
