package com.example.pam.data.model

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val note: String? = null,
    val payeeUpiId: String? = null,
    val payeeName: String? = null,
    val upiRefId: String? = null,
    val paymentStatus: String = "COMPLETED",
    val createdAt: Long = System.currentTimeMillis(),
    val splitGroupId: Long? = null
)

data class TransactionWithDetails(
    val transaction: Transaction,
    val account: Account?,
    val toAccount: Account? = null,
    val category: Category? = null
)
