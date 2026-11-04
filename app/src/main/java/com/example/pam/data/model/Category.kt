package com.example.pam.data.model

enum class TransactionType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    TRANSFER("Transfer")
}

enum class CategoryType(val displayName: String) {
    EXPENSE("Expense"),
    INCOME("Income"),
    BOTH("Both")
}

data class Category(
    val id: Long = 0,
    val name: String,
    val type: CategoryType = CategoryType.EXPENSE,
    val colorHex: String = "#FF7043",
    val iconName: String = "Category",
    val isDefault: Boolean = false
) {
    fun isApplicableFor(txnType: TransactionType): Boolean {
        return when (txnType) {
            TransactionType.EXPENSE -> type == CategoryType.EXPENSE || type == CategoryType.BOTH
            TransactionType.INCOME -> type == CategoryType.INCOME || type == CategoryType.BOTH
            TransactionType.TRANSFER -> false
        }
    }
}
