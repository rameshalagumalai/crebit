package com.example.pam

import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.util.ai.AnomalyType
import com.example.pam.util.ai.SpendingAnomalyDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpendingAnomalyDetectorTest {

    private val defaultAccount = Account(id = 1, name = "HDFC Bank", type = AccountType.BANK, currentBalance = 10000.0)
    private val cashAccount = Account(id = 2, name = "Cash", type = AccountType.CASH, currentBalance = 2000.0)

    private val foodCategory = Category(id = 1, name = "Food & Dining", type = CategoryType.EXPENSE)
    private val shoppingCategory = Category(id = 2, name = "Shopping", type = CategoryType.EXPENSE)
    private val travelCategory = Category(id = 3, name = "Travel", type = CategoryType.EXPENSE)

    @Test
    fun testExactDuplicateDetection_Within24Hours() {
        val now = System.currentTimeMillis()
        val recentTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 101,
                title = "Starbucks Coffee",
                amount = 350.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = now - (45 * 60 * 1000L) // 45 minutes ago
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val result = SpendingAnomalyDetector.checkForDuplicate(
            candidateAmount = 350.0,
            candidateTitle = "Starbucks Coffee",
            candidateType = TransactionType.EXPENSE,
            candidateAccountId = defaultAccount.id,
            candidateDateEpochMillis = now,
            recentTransactions = listOf(recentTxn)
        )

        assertNotNull(result)
        assertTrue(result!!.isExactMatch)
        assertEquals(101L, result.matchedTransaction.transaction.id)
        assertTrue(result.explanation.contains("Starbucks Coffee"))
    }

    @Test
    fun testFuzzyDuplicateDetection_SimilarTitleWithin12Hours() {
        val now = System.currentTimeMillis()
        val recentTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 102,
                title = "Swiggy Food Delivery",
                amount = 450.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = now - (2 * 60 * 60 * 1000L) // 2 hours ago
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val result = SpendingAnomalyDetector.checkForDuplicate(
            candidateAmount = 450.0,
            candidateTitle = "Swiggy",
            candidateType = TransactionType.EXPENSE,
            candidateAccountId = defaultAccount.id,
            candidateDateEpochMillis = now,
            recentTransactions = listOf(recentTxn)
        )

        assertNotNull(result)
        // Substring match should be fuzzy (isExactMatch == false)
        assertEquals(false, result!!.isExactMatch)
        assertEquals(102L, result.matchedTransaction.transaction.id)
    }

    @Test
    fun testSameAccountAndAmount_Within1Hour() {
        val now = System.currentTimeMillis()
        val recentTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 103,
                title = "Store Payment",
                amount = 999.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = shoppingCategory.id,
                dateEpochMillis = now - (20 * 60 * 1000L) // 20 minutes ago
            ),
            account = defaultAccount,
            category = shoppingCategory
        )

        // Candidate has a different title, but identical amount and account within 20 mins
        val result = SpendingAnomalyDetector.checkForDuplicate(
            candidateAmount = 999.0,
            candidateTitle = "Merchant X",
            candidateType = TransactionType.EXPENSE,
            candidateAccountId = defaultAccount.id,
            candidateDateEpochMillis = now,
            recentTransactions = listOf(recentTxn)
        )

        assertNotNull(result)
        assertEquals(103L, result!!.matchedTransaction.transaction.id)
        assertTrue(result.explanation.contains("HDFC Bank"))
    }

    @Test
    fun testDifferentAmount_NotDuplicate() {
        val now = System.currentTimeMillis()
        val recentTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 104,
                title = "Coffee",
                amount = 150.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = now - (15 * 60 * 1000L)
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val result = SpendingAnomalyDetector.checkForDuplicate(
            candidateAmount = 250.0, // Different amount
            candidateTitle = "Coffee",
            candidateType = TransactionType.EXPENSE,
            candidateAccountId = defaultAccount.id,
            candidateDateEpochMillis = now,
            recentTransactions = listOf(recentTxn)
        )

        assertNull(result)
    }

    @Test
    fun testExcludeTransactionId_NotDuplicateWhenEditingSelf() {
        val now = System.currentTimeMillis()
        val recentTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 105,
                title = "Groceries",
                amount = 1200.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = now
            ),
            account = defaultAccount,
            category = foodCategory
        )

        // When editing txn 105, it must exclude itself
        val result = SpendingAnomalyDetector.checkForDuplicate(
            candidateAmount = 1200.0,
            candidateTitle = "Groceries",
            candidateType = TransactionType.EXPENSE,
            candidateAccountId = defaultAccount.id,
            candidateDateEpochMillis = now,
            recentTransactions = listOf(recentTxn),
            excludeTransactionId = 105L
        )

        assertNull(result)
    }

    @Test
    fun testIncomeAndTransfers_NotFlaggedAsDuplicateExpense() {
        val now = System.currentTimeMillis()
        val recentTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 106,
                title = "Salary",
                amount = 50000.0,
                type = TransactionType.INCOME,
                accountId = defaultAccount.id,
                categoryId = null,
                dateEpochMillis = now - 3600000L
            ),
            account = defaultAccount,
            category = null
        )

        val result = SpendingAnomalyDetector.checkForDuplicate(
            candidateAmount = 50000.0,
            candidateTitle = "Salary",
            candidateType = TransactionType.INCOME, // Income
            recentTransactions = listOf(recentTxn)
        )

        assertNull(result)
    }

    @Test
    fun testCategorySpikeDetection() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val txns = mutableListOf<TransactionWithDetails>()

        // Historical baseline (10 to 30 days ago): Food avg ~₹700 / week
        for (day in 10..30 step 5) {
            txns.add(
                TransactionWithDetails(
                    transaction = Transaction(
                        id = day.toLong(),
                        title = "Food baseline",
                        amount = 500.0,
                        type = TransactionType.EXPENSE,
                        accountId = 1L,
                        categoryId = foodCategory.id,
                        dateEpochMillis = now - (day * oneDay)
                    ),
                    account = defaultAccount,
                    category = foodCategory
                )
            )
        }

        // Current week: Spend is ₹4,500 (much higher than baseline)
        txns.add(
            TransactionWithDetails(
                transaction = Transaction(
                    id = 201,
                    title = "Fancy Dinner",
                    amount = 4500.0,
                    type = TransactionType.EXPENSE,
                    accountId = 1L,
                    categoryId = foodCategory.id,
                    dateEpochMillis = now - (2 * oneDay)
                ),
                account = defaultAccount,
                category = foodCategory
            )
        )

        val anomalies = SpendingAnomalyDetector.detectSpendingAnomalies(txns, referenceTimeMillis = now)
        val spike = anomalies.firstOrNull { it.type == AnomalyType.CATEGORY_SPIKE }

        assertNotNull(spike)
        assertEquals("Food & Dining", spike!!.categoryName)
        assertTrue(spike.ratio != null && spike.ratio!! >= 1.8f)
    }

    @Test
    fun testSmallAmountDoesNotTriggerSpikeFalsePositive() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val txns = mutableListOf<TransactionWithDetails>()

        // Historical baseline: Travel ₹50 / week
        for (day in 10..30 step 5) {
            txns.add(
                TransactionWithDetails(
                    transaction = Transaction(
                        id = day.toLong(),
                        title = "Bus ticket",
                        amount = 35.0,
                        type = TransactionType.EXPENSE,
                        accountId = 1L,
                        categoryId = travelCategory.id,
                        dateEpochMillis = now - (day * oneDay)
                    ),
                    account = defaultAccount,
                    category = travelCategory
                )
            )
        }

        // Current week: Spend ₹100 (2.0x ratio, but diff is only ₹50, which is < ₹500 delta threshold)
        txns.add(
            TransactionWithDetails(
                transaction = Transaction(
                    id = 301,
                    title = "Metro ticket",
                    amount = 100.0,
                    type = TransactionType.EXPENSE,
                    accountId = 1L,
                    categoryId = travelCategory.id,
                    dateEpochMillis = now - (2 * oneDay)
                ),
                account = defaultAccount,
                category = travelCategory
            )
        )

        val anomalies = SpendingAnomalyDetector.detectSpendingAnomalies(txns, referenceTimeMillis = now)
        val travelSpike = anomalies.firstOrNull { it.categoryName == "Travel" && it.type == AnomalyType.CATEGORY_SPIKE }

        // Must NOT trigger because delta < ₹500
        assertNull(travelSpike)
    }

    @Test
    fun testUnusuallyLargeExpenseDetection() {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val txns = mutableListOf<TransactionWithDetails>()

        // 3 historical shopping expenses averaging ~₹600
        txns.add(createTxn(401, "Shirt", 600.0, shoppingCategory, now - 15 * oneDay))
        txns.add(createTxn(402, "Jeans", 700.0, shoppingCategory, now - 12 * oneDay))
        txns.add(createTxn(403, "Shoes", 500.0, shoppingCategory, now - 9 * oneDay))

        // Recent expense of ₹8,000 (>= 3.0x category avg and >= avg + 1000)
        txns.add(createTxn(404, "Laptop Bag & Gear", 8000.0, shoppingCategory, now - 1 * oneDay))

        val anomalies = SpendingAnomalyDetector.detectSpendingAnomalies(txns, referenceTimeMillis = now)
        val largeTxn = anomalies.firstOrNull { it.type == AnomalyType.UNUSUALLY_LARGE_EXPENSE }

        assertNotNull(largeTxn)
        assertEquals("Shopping", largeTxn!!.categoryName)
        assertEquals(8000.0, largeTxn.amount!!, 0.01)
        assertEquals(404L, largeTxn.relatedTransactionId)
    }

    @Test
    fun testDailySurgeDetection() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val oneDay = 24 * 60 * 60 * 1000L
        val txns = mutableListOf<TransactionWithDetails>()

        // 10 days of past expenses averaging ₹500 / day
        for (day in 1..10) {
            txns.add(createTxn(500L + day, "Daily spend", 500.0, foodCategory, now - (day * oneDay)))
        }

        // Today's total spend is ₹3,500 (Ratio 7x vs daily average, diff >= ₹1500)
        txns.add(createTxn(601, "Major Electronics", 3500.0, shoppingCategory, now - (30 * 60 * 1000L)))

        val anomalies = SpendingAnomalyDetector.detectSpendingAnomalies(txns, referenceTimeMillis = now)
        val surge = anomalies.firstOrNull { it.type == AnomalyType.DAILY_SURGE }

        assertNotNull(surge)
        assertEquals(3500.0, surge!!.amount!!, 0.01)
        assertTrue(surge.ratio != null && surge.ratio!! >= 2.5f)
    }

    private fun createTxn(
        id: Long,
        title: String,
        amount: Double,
        category: Category,
        timeMillis: Long
    ): TransactionWithDetails {
        return TransactionWithDetails(
            transaction = Transaction(
                id = id,
                title = title,
                amount = amount,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = category.id,
                dateEpochMillis = timeMillis
            ),
            account = defaultAccount,
            category = category
        )
    }
}
