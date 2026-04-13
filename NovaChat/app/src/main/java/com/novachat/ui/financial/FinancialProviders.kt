package com.novachat.ui.financial

import androidx.compose.ui.graphics.Color

data class FinancialProvider(
    val name: String,
    val abbreviation: String,
    val url: String,
    val color: Color,
    val smsAddress: String
)

object FinancialProviders {
    val israeliProviders = listOf(
        FinancialProvider("American Express Israel", "AE", "https://he.americanexpress.co.il/products-services-lobby/sms/", Color(0xFF2196F3), "AMEX"),
        FinancialProvider("Isracard (Mastercard)", "IC", "https://business.isracard.co.il/digital_services/information_sms/", Color(0xFF4CAF50), "Isracard"),
        FinancialProvider("MAX", "MX", "https://www.max.co.il/cards/pages/sms", Color(0xFFFF9800), "MAX"),
        FinancialProvider("Cal", "CL", "https://www.cal-online.co.il/service-and-support/push-notifications", Color(0xFF9C27B0), "CAL"),
        FinancialProvider("Bank Discount", "BD", "https://www.discountbank.co.il/private/communication-channels/online-banking/discount-by-sms/", Color(0xFF009688), "DISCOUNT"),
        FinancialProvider("Bank Hapoalim", "BH", "https://www.bankhapoalim.co.il/", Color(0xFFF44336), "POALIM"),
        FinancialProvider("Bank Leumi", "BL", "https://www.leumi.co.il/", Color(0xFF3F51B5), "LEUMI"),
        FinancialProvider("Mizrahi Tefahot", "MT", "https://www.mizrahi-tefahot.co.il/", Color(0xFF795548), "MIZRAHI")
    )

    val usProviders = listOf(
        FinancialProvider("Chase", "CH", "https://www.chase.com/digital/alerts.html", Color(0xFF1565C0), "CHASE"),
        FinancialProvider("Bank of America", "BA", "https://www.bankofamerica.com/onlinebanking/online-banking-alerts.go", Color(0xFFC62828), "BANKOFAMERICA"),
        FinancialProvider("Wells Fargo", "WF", "https://www.wellsfargo.com/help/online-banking/alerts-faqs/", Color(0xFFD32F2F), "WELLSFARGO"),
        FinancialProvider("Citi", "CI", "https://www.citibank.com/ipb/europe/Citibank-Online-Help-Centre/my_profile/activate_alerting-text-guide.htm", Color(0xFF0277BD), "CITI"),
        FinancialProvider("Capital One", "CO", "https://www.capitalone.com/help-center/checking-savings/manage-account-alerts", Color(0xFF2E7D32), "CAPITALONE"),
        FinancialProvider("American Express US", "AE", "https://www.americanexpress.com/us/security-center/fraud-account-alerts", Color(0xFF2196F3), "AMEX"),
        FinancialProvider("Discover", "DS", "https://www.discover.com/credit-cards/card-smarts/email-and-text-alerts/", Color(0xFFFF6F00), "DISCOVER"),
        FinancialProvider("PNC", "PN", "https://www.pnc.com/en/personal-banking/banking/online-and-mobile-banking/alerts.html", Color(0xFFEF6C00), "PNC"),
        FinancialProvider("US Bank", "UB", "https://www.usbank.com/online-mobile-banking/account-alerts.html", Color(0xFF1A237E), "USBANK")
    )
}
