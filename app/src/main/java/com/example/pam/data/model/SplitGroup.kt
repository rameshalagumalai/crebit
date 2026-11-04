package com.example.pam.data.model

enum class SplitStatus {
    OPEN,
    SETTLED
}

data class SplitGroup(
    val id: Long = 0,
    val description: String,
    val totalAmount: Double,
    val myShare: Double,
    val receivableAmount: Double,
    val expenseTransactionId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val status: SplitStatus = SplitStatus.OPEN
)

data class SplitGroupWithDetails(
    val splitGroup: SplitGroup,
    val expenseTransaction: TransactionWithDetails?,
    val settlementTransactions: List<TransactionWithDetails>,
    val totalReceived: Double,
    val remainingAmount: Double
) {
    val isFullyCollected: Boolean
        get() = totalReceived >= splitGroup.receivableAmount - 0.001
}

data class SplitSettlementEntry(
    val amount: Double,
    val accountId: Long,
    val note: String? = null,
    val dateEpochMillis: Long = System.currentTimeMillis()
)
