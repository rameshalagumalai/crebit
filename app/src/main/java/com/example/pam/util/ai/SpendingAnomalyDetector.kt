package com.example.pam.util.ai

import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import java.util.Calendar
import java.util.Locale

enum class AnomalyType {
    DUPLICATE_TRANSACTION,
    CATEGORY_SPIKE,
    UNUSUALLY_LARGE_EXPENSE,
    DAILY_SURGE
}

enum class AnomalySeverity {
    INFO,
    WARNING,
    ALERT
}

data class DuplicateDetectionResult(
    val matchedTransaction: TransactionWithDetails,
    val isExactMatch: Boolean,
    val explanation: String,
    val timeDiffMinutes: Long
)

data class SpendingAnomaly(
    val id: String,
    val type: AnomalyType,
    val severity: AnomalySeverity,
    val title: String,
    val message: String,
    val categoryName: String? = null,
    val categoryColorHex: String? = null,
    val amount: Double? = null,
    val baselineAmount: Double? = null,
    val ratio: Float? = null,
    val relatedTransactionId: Long? = null
)

object SpendingAnomalyDetector {

    /**
     * Checks if a candidate transaction is an accidental duplicate of an existing recent transaction.
     *
     * @param candidateAmount The amount being entered.
     * @param candidateTitle The title / merchant of the transaction.
     * @param candidateType The transaction type (only expenses are checked for duplicate warnings).
     * @param candidateCategoryName Optional category name.
     * @param candidateAccountId Optional account ID.
     * @param candidateDateEpochMillis The timestamp of the candidate transaction.
     * @param recentTransactions The list of recently recorded transactions.
     * @param timeWindowHours Time window in hours to look for duplicates (default: 24h).
     * @param excludeTransactionId Optional transaction ID to exclude from comparison (e.g. current editing txn).
     */
    fun checkForDuplicate(
        candidateAmount: Double?,
        candidateTitle: String,
        candidateType: TransactionType = TransactionType.EXPENSE,
        candidateCategoryName: String? = null,
        candidateAccountId: Long? = null,
        candidateDateEpochMillis: Long = System.currentTimeMillis(),
        recentTransactions: List<TransactionWithDetails>,
        timeWindowHours: Long = 24L,
        excludeTransactionId: Long? = null
    ): DuplicateDetectionResult? {
        if (candidateType != TransactionType.EXPENSE) return null
        if (candidateAmount == null || candidateAmount <= 0.0) return null
        val normCandidate = candidateTitle.trim().lowercase(Locale.US)
        val windowMillis = timeWindowHours * 60 * 60 * 1000L

        for (item in recentTransactions) {
            val txn = item.transaction
            if (excludeTransactionId != null && txn.id == excludeTransactionId) continue
            // Only compare expenses against expenses
            if (txn.type != TransactionType.EXPENSE) continue

            val timeDiff = Math.abs(candidateDateEpochMillis - txn.dateEpochMillis)
            if (timeDiff > windowMillis) continue

            val diffMinutes = Math.max(1L, timeDiff / (60 * 1000L))
            val isSameAmount = Math.abs(txn.amount - candidateAmount) < 0.01
            val normTxnTitle = txn.title.trim().lowercase(Locale.US)

            val isExactTitle = normCandidate.isNotEmpty() && normCandidate == normTxnTitle
            val isSubstringTitle = normCandidate.length >= 3 && normTxnTitle.length >= 3 && (
                normCandidate.contains(normTxnTitle) || normTxnTitle.contains(normCandidate)
            )

            // Case 1: Exact amount + identical title within the window
            if (isSameAmount && isExactTitle) {
                val timeDesc = formatTimeAgo(diffMinutes)
                val accDesc = item.account?.name?.let { " on $it" } ?: ""
                return DuplicateDetectionResult(
                    matchedTransaction = item,
                    isExactMatch = true,
                    explanation = "Identical ₹${formatAmount(candidateAmount)} ('${txn.title}') was logged $timeDesc$accDesc.",
                    timeDiffMinutes = diffMinutes
                )
            }

            // Case 2: Exact amount + very similar title within 12 hours
            if (isSameAmount && isSubstringTitle && diffMinutes <= 12 * 60) {
                val timeDesc = formatTimeAgo(diffMinutes)
                return DuplicateDetectionResult(
                    matchedTransaction = item,
                    isExactMatch = false,
                    explanation = "Similar ₹${formatAmount(candidateAmount)} ('${txn.title}') was recorded $timeDesc.",
                    timeDiffMinutes = diffMinutes
                )
            }

            // Case 3: Same amount + same account logged within 1 hour
            if (isSameAmount && candidateAccountId != null && candidateAccountId == txn.accountId && diffMinutes <= 60) {
                val timeDesc = formatTimeAgo(diffMinutes)
                val accName = item.account?.name ?: "same account"
                return DuplicateDetectionResult(
                    matchedTransaction = item,
                    isExactMatch = false,
                    explanation = "A transaction of ₹${formatAmount(candidateAmount)} ('${txn.title}') was recorded $timeDesc on $accName.",
                    timeDiffMinutes = diffMinutes
                )
            }
        }
        return null
    }

    /**
     * Evaluates a user's transaction history to detect category spending spikes,
     * unusually large single expenses, and daily surges.
     */
    fun detectSpendingAnomalies(
        transactions: List<TransactionWithDetails>,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): List<SpendingAnomaly> {
        val anomalies = mutableListOf<SpendingAnomaly>()
        if (transactions.isEmpty()) return anomalies

        val oneDayMillis = 24 * 60 * 60 * 1000L
        val sevenDaysMillis = 7 * oneDayMillis
        val thirtyDaysMillis = 30 * oneDayMillis

        val sevenDaysAgo = referenceTimeMillis - sevenDaysMillis
        val thirtySevenDaysAgo = referenceTimeMillis - (37 * oneDayMillis)

        // Only evaluate EXPENSE transactions
        val expenses = transactions.filter { it.transaction.type == TransactionType.EXPENSE }

        val current7DaysExpenses = expenses.filter { it.transaction.dateEpochMillis in sevenDaysAgo..referenceTimeMillis }
        val baselineExpenses = expenses.filter { it.transaction.dateEpochMillis in thirtySevenDaysAgo until sevenDaysAgo }

        // 1. Category Spike Anomaly
        val baselineByCategory = baselineExpenses.groupBy { it.category?.name ?: "Uncategorized" }
        val currentByCategory = current7DaysExpenses.groupBy { it.category?.name ?: "Uncategorized" }

        for ((categoryName, currentList) in currentByCategory) {
            val currentTotal = currentList.sumOf { it.transaction.amount }
            val baselineList = baselineByCategory[categoryName] ?: emptyList()

            val earliestBaselineMillis = baselineList.minOfOrNull { it.transaction.dateEpochMillis }
            val baselineSpanDays = if (earliestBaselineMillis != null) {
                ((sevenDaysAgo - earliestBaselineMillis) / (24 * 60 * 60 * 1000.0)).coerceIn(1.0, 30.0)
            } else 0.0

            // Require at least 5 days of baseline data to avoid false positive spikes
            if (baselineSpanDays >= 5.0 && baselineList.isNotEmpty()) {
                val baselineTotal = baselineList.sumOf { it.transaction.amount }
                val baselineWeeklyAvg = (baselineTotal / baselineSpanDays) * 7.0

                if (baselineWeeklyAvg > 0.0) {
                    val ratio = currentTotal / baselineWeeklyAvg
                    val diff = currentTotal - baselineWeeklyAvg

                    // Minimum meaningful spike threshold: >= 1.8x baseline AND at least ₹500 diff
                    if (ratio >= 1.8 && diff >= 500.0) {
                        val sampleCat = currentList.firstOrNull()?.category
                        anomalies.add(
                            SpendingAnomaly(
                                id = "spike_${categoryName.lowercase(Locale.US).replace(" ", "_")}",
                                type = AnomalyType.CATEGORY_SPIKE,
                                severity = if (ratio >= 2.5) AnomalySeverity.ALERT else AnomalySeverity.WARNING,
                                title = "$categoryName Spike",
                                message = "You've spent ₹${formatAmount(currentTotal)} on $categoryName this week, which is ${String.format(Locale.US, "%.1f", ratio)}× higher than your usual weekly average (₹${formatAmount(baselineWeeklyAvg)}).",
                                categoryName = categoryName,
                                categoryColorHex = sampleCat?.colorHex ?: "#FF7043",
                                amount = currentTotal,
                                baselineAmount = baselineWeeklyAvg,
                                ratio = ratio.toFloat()
                            )
                        )
                    }
                }
            }
        }

        // 2. Unusually Large Expense in the last 7 days
        for ((categoryName, catAllList) in expenses.groupBy { it.category?.name ?: "Uncategorized" }) {
            // Need at least 3 historical expenses in this category to establish baseline
            if (catAllList.size >= 3) {
                val recentCatTxns = current7DaysExpenses.filter { (it.category?.name ?: "Uncategorized") == categoryName }

                for (recent in recentCatTxns) {
                    val amt = recent.transaction.amount
                    val otherTxns = catAllList.filter { it.transaction.id != recent.transaction.id }
                    if (otherTxns.size >= 2) {
                        val avgTxnSize = otherTxns.map { it.transaction.amount }.average()
                        val ratio = if (avgTxnSize > 0) amt / avgTxnSize else 1.0

                        // Threshold: >= 3.0x category average AND >= average + ₹1,000
                        if (ratio >= 3.0 && (amt - avgTxnSize) >= 1000.0) {
                            anomalies.add(
                                SpendingAnomaly(
                                    id = "large_txn_${recent.transaction.id}",
                                    type = AnomalyType.UNUSUALLY_LARGE_EXPENSE,
                                    severity = AnomalySeverity.INFO,
                                    title = "Unusual $categoryName Expense",
                                    message = "₹${formatAmount(amt)} for '${recent.transaction.title}' is ${String.format(Locale.US, "%.1f", ratio)}× higher than your typical $categoryName expense (avg ₹${formatAmount(avgTxnSize)}).",
                                    categoryName = categoryName,
                                    categoryColorHex = recent.category?.colorHex ?: "#FF7043",
                                    amount = amt,
                                    baselineAmount = avgTxnSize,
                                    ratio = ratio.toFloat(),
                                    relatedTransactionId = recent.transaction.id
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Daily Spending Surge (Today vs 30-Day Daily Average)
        val calNow = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayExpenses = expenses.filter { it.transaction.dateEpochMillis in startOfToday..referenceTimeMillis }
        val todayTotal = todayExpenses.sumOf { it.transaction.amount }

        val past30DaysExpenses = expenses.filter { it.transaction.dateEpochMillis in (referenceTimeMillis - thirtyDaysMillis) until startOfToday }
        if (past30DaysExpenses.size >= 3) {
            val earliestMillis = past30DaysExpenses.minOfOrNull { it.transaction.dateEpochMillis } ?: (referenceTimeMillis - thirtyDaysMillis)
            val spanDays = ((startOfToday - earliestMillis) / (24 * 60 * 60 * 1000.0)).coerceIn(1.0, 30.0)
            if (spanDays >= 3.0) {
                val dailyAvg = past30DaysExpenses.sumOf { it.transaction.amount } / spanDays
                if (dailyAvg > 0.0) {
                    val ratio = todayTotal / dailyAvg
                    if (ratio >= 2.5 && (todayTotal - dailyAvg) >= 1500.0) {
                        anomalies.add(
                            SpendingAnomaly(
                                id = "daily_surge_${calNow.get(Calendar.DAY_OF_YEAR)}",
                                type = AnomalyType.DAILY_SURGE,
                                severity = AnomalySeverity.WARNING,
                                title = "High Daily Spend Surge",
                                message = "Today's spend of ₹${formatAmount(todayTotal)} is ${String.format(Locale.US, "%.1f", ratio)}× higher than your daily average of ₹${formatAmount(dailyAvg)}.",
                                amount = todayTotal,
                                baselineAmount = dailyAvg,
                                ratio = ratio.toFloat()
                            )
                        )
                    }
                }
            }
        }

        // Sort by severity (ALERT > WARNING > INFO) and return distinct by id
        return anomalies.distinctBy { it.id }.sortedByDescending { it.severity.ordinal }
    }

    private fun formatTimeAgo(diffMinutes: Long): String {
        return when {
            diffMinutes < 1 -> "just now"
            diffMinutes == 1L -> "1 minute ago"
            diffMinutes < 60 -> "$diffMinutes minutes ago"
            diffMinutes < 120 -> "1 hour ago"
            diffMinutes < 24 * 60 -> "${diffMinutes / 60} hours ago"
            else -> "${diffMinutes / (24 * 60)} days ago"
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format(Locale.US, "%,d", amount.toLong())
        } else {
            String.format(Locale.US, "%,.2f", amount)
        }
    }
}
