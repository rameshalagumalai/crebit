package com.example.pam.data.model

enum class AccountType(val displayName: String) {
    BANK("Bank Account"),
    CASH("Cash"),
    CREDIT_CARD("Credit Card"),
    WALLET("Digital Wallet"),
    SAVINGS("Savings"),
    INVESTMENT("Investment"),
    OTHER("Other")
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val accountNumber: String? = null,
    val colorHex: String = "#1E88E5",
    val iconName: String = "AccountBalance",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
