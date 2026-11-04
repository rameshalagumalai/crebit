package com.example.pam.data.model

enum class DateRangeType(val displayName: String) {
    THIS_MONTH("This Month"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

data class DateFilterRange(
    val type: DateRangeType,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val label: String
)

data class CategoryExpenseSummary(
    val category: Category,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class AccountExpenseSummary(
    val account: Account,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class FinancialOverview(
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRatePercentage: Float,
    val transactionCount: Int
)
