package com.novachat.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCategory(
    val id: String,
    val displayName: String,
    val colorHex: String,
    val isBuiltIn: Boolean,
    val isDeleted: Boolean = false
)

val DEFAULT_USER_CATEGORIES = listOf(
    UserCategory("BILL",         "Bill",         "#42A5F5", isBuiltIn = true),
    UserCategory("SUBSCRIPTION", "Subscription", "#6C5CE7", isBuiltIn = true),
    UserCategory("PAYMENT",      "Payment",      "#00B894", isBuiltIn = true),
    UserCategory("EXPENSE",      "Expense",      "#FDCB6E", isBuiltIn = true),
)
