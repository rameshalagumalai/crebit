package com.example.pam

import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.SplitGroup
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.util.ai.AskCrebitEngine
import com.example.pam.util.ai.QueryIntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AskCrebitEngineTest {

    private val bankAccount = Account(id = 1, name = "HDFC Bank", type = AccountType.BANK, currentBalance = 25000.0)
    private val cashAccount = Account(id = 2, name = "Cash", type = AccountType.CASH, currentBalance = 3500.0)
    private val creditCard = Account(id = 3, name = "ICICI Credit Card", type = AccountType.CREDIT_CARD, currentBalance = 5000.0)
    private val testAccounts = listOf(bankAccount, cashAccount, creditCard)

    private val foodCategory = Category(id = 1, name = "Food & Dining", type = CategoryType.EXPENSE)
    private val travelCategory = Category(id = 2, name = "Travel", type = CategoryType.EXPENSE)
    private val shoppingCategory = Category(id = 3, name = "Shopping", type = CategoryType.EXPENSE)
    private val salaryCategory = Category(id = 4, name = "Salary", type = CategoryType.INCOME)
    private val testCategories = listOf(foodCategory, travelCategory, shoppingCategory, salaryCategory)

    private val fixedReferenceTime: Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun createTxn(
        id: Long,
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category?,
        account: Account,
        dateEpochMillis: Long
    ): TransactionWithDetails {
        return TransactionWithDetails(
            transaction = Transaction(
                id = id,
                title = title,
                amount = amount,
                type = type,
                accountId = account.id,
                categoryId = category?.id,
                dateEpochMillis = dateEpochMillis
            ),
            account = account,
            category = category
        )
    }

    @Test
    fun testBlankQuery_ReturnsWelcome() {
        val res = AskCrebitEngine.query("", emptyList(), testAccounts, testCategories)
        assertEquals(QueryIntentType.PERIOD_SUMMARY, res.intentType)
        assertTrue(res.headline.contains("Ask Crebit Anything"))
        assertTrue(res.suggestedFollowUps.isNotEmpty())
    }

    @Test
    fun testCategorySpendQuery_WithComparison() {
        // Current month (Nov 2026) food expenses: 500 + 300 = 800
        val nov10 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 10, 10, 0)
        }.timeInMillis

        // Previous month (Oct 2026) food expenses: 1000
        val oct15 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 10, 0)
        }.timeInMillis

        val txns = listOf(
            createTxn(1, "Restaurant Lunch", 500.0, TransactionType.EXPENSE, foodCategory, bankAccount, nov10),
            createTxn(2, "Cafe Dinner", 300.0, TransactionType.EXPENSE, foodCategory, bankAccount, nov10),
            createTxn(3, "Supermarket Snacks", 1000.0, TransactionType.EXPENSE, foodCategory, bankAccount, oct15)
        )

        val res = AskCrebitEngine.query(
            "How much did I spend on food this month?",
            txns,
            testAccounts,
            testCategories,
            referenceTimeMillis = fixedReferenceTime
        )

        assertEquals(QueryIntentType.CATEGORY_SPEND, res.intentType)
        assertTrue(res.headline.contains("800"))
        assertEquals(2, res.matchingTransactions.size)
        assertNotNull(res.comparisonText)
        assertTrue(res.comparisonText!!.contains("lower than last month"))
    }

    @Test
    fun testSplitsReceivableQuery() {
        val splitGroup1 = SplitGroup(
            id = 10,
            description = "Team Dinner",
            totalAmount = 3000.0,
            myShare = 1000.0,
            receivableAmount = 2000.0,
            expenseTransactionId = 1,
            status = SplitStatus.OPEN
        )
        val splitDetails1 = SplitGroupWithDetails(
            splitGroup = splitGroup1,
            expenseTransaction = null,
            settlementTransactions = emptyList(),
            totalReceived = 500.0,
            remainingAmount = 1500.0
        )

        val res = AskCrebitEngine.query(
            "Pending splits",
            emptyList(),
            testAccounts,
            testCategories,
            splits = listOf(splitDetails1)
        )

        assertEquals(QueryIntentType.SPLITS_RECEIVABLE, res.intentType)
        assertTrue(res.headline.contains("1,500"))
        assertEquals(1, res.matchingSplits.size)
    }

    @Test
    fun testNetWorthQuery() {
        // Bank (25,000) + Cash (3,500) - Credit Debt (5,000) = 23,500
        val res = AskCrebitEngine.query(
            "What is my net worth?",
            emptyList(),
            testAccounts,
            testCategories
        )

        assertEquals(QueryIntentType.NET_WORTH, res.intentType)
        assertTrue(res.headline.contains("23,500"))
    }

    @Test
    fun testAccountBalanceQuery() {
        val resHdfc = AskCrebitEngine.query(
            "What is my balance in HDFC?",
            emptyList(),
            testAccounts,
            testCategories
        )
        assertEquals(QueryIntentType.ACCOUNT_BALANCE, resHdfc.intentType)
        assertTrue(resHdfc.headline.contains("25,000"))
        assertEquals(bankAccount.id, resHdfc.matchingAccount?.id)

        val resCash = AskCrebitEngine.query(
            "How much cash do I have?",
            emptyList(),
            testAccounts,
            testCategories
        )
        assertEquals(QueryIntentType.ACCOUNT_BALANCE, resCash.intentType)
        assertTrue(resCash.headline.contains("3,500"))
    }

    @Test
    fun testExtremumMaxExpenseQuery() {
        val now = fixedReferenceTime
        val txns = listOf(
            createTxn(1, "Flight to Delhi", 4500.0, TransactionType.EXPENSE, travelCategory, bankAccount, now),
            createTxn(2, "Coffee", 150.0, TransactionType.EXPENSE, foodCategory, cashAccount, now),
            createTxn(3, "Sneakers", 3200.0, TransactionType.EXPENSE, shoppingCategory, creditCard, now)
        )

        val res = AskCrebitEngine.query(
            "What was my biggest expense this month?",
            txns,
            testAccounts,
            testCategories,
            referenceTimeMillis = fixedReferenceTime
        )

        assertEquals(QueryIntentType.BIGGEST_EXPENSE, res.intentType)
        assertTrue(res.headline.contains("4,500"))
        assertTrue(res.subhead?.contains("Flight to Delhi") == true)
    }

    @Test
    fun testExtremumLowestExpenseQuery() {
        val now = fixedReferenceTime
        val txns = listOf(
            createTxn(1, "Flight to Delhi", 4500.0, TransactionType.EXPENSE, travelCategory, bankAccount, now),
            createTxn(2, "Chai", 20.0, TransactionType.EXPENSE, foodCategory, cashAccount, now),
            createTxn(3, "Sneakers", 3200.0, TransactionType.EXPENSE, shoppingCategory, creditCard, now)
        )

        val res = AskCrebitEngine.query(
            "What is my lowest expense this month?",
            txns,
            testAccounts,
            testCategories,
            referenceTimeMillis = fixedReferenceTime
        )

        assertEquals(QueryIntentType.LOWEST_EXPENSE, res.intentType)
        assertTrue(res.headline.contains("20"))
        assertTrue(res.subhead?.contains("Chai") == true)
    }

    @Test
    fun testIncomeSummaryQuery() {
        val nov1 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 1, 9, 0) }.timeInMillis
        val txns = listOf(
            createTxn(1, "Monthly Salary", 75000.0, TransactionType.INCOME, salaryCategory, bankAccount, nov1),
            createTxn(2, "Freelance Gig", 12000.0, TransactionType.INCOME, salaryCategory, bankAccount, nov1)
        )

        val res = AskCrebitEngine.query(
            "Total income this month",
            txns,
            testAccounts,
            testCategories,
            referenceTimeMillis = fixedReferenceTime
        )

        assertEquals(QueryIntentType.INCOME_SUMMARY, res.intentType)
        assertTrue(res.headline.contains("87,000"))
        assertEquals(2, res.matchingTransactions.size)
    }

    @Test
    fun testMerchantSearch_WithDateFilter() {
        val nov5 = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 5, 14, 0) }.timeInMillis
        val oct20 = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 14, 0) }.timeInMillis

        val txns = listOf(
            createTxn(1, "Uber Ride to Airport", 650.0, TransactionType.EXPENSE, travelCategory, bankAccount, nov5),
            createTxn(2, "Uber Office Commute", 220.0, TransactionType.EXPENSE, travelCategory, bankAccount, nov5),
            createTxn(3, "Uber Weekend", 300.0, TransactionType.EXPENSE, travelCategory, bankAccount, oct20)
        )

        val res = AskCrebitEngine.query(
            "Show all Uber spends this month",
            txns,
            testAccounts,
            testCategories,
            referenceTimeMillis = fixedReferenceTime
        )

        assertEquals(QueryIntentType.MERCHANT_SEARCH, res.intentType)
        assertTrue(res.headline.contains("870"))
        assertEquals(2, res.matchingTransactions.size)
    }

    @Test
    fun testDateRangeParsing() {
        val todayRange = AskCrebitEngine.resolveDateRange("how much spent today", fixedReferenceTime)
        assertEquals("Today", todayRange.label)

        val yesterdayRange = AskCrebitEngine.resolveDateRange("yesterday's expenses", fixedReferenceTime)
        assertEquals("Yesterday", yesterdayRange.label)

        val thisWeekRange = AskCrebitEngine.resolveDateRange("this week spends", fixedReferenceTime)
        assertEquals("This Week", thisWeekRange.label)

        val octRange = AskCrebitEngine.resolveDateRange("spends in october", fixedReferenceTime)
        assertEquals("October", octRange.label)
    }

    @Test
    fun testPerformance_Sub5msExecution() {
        val txns = (1..500).map { i ->
            createTxn(
                id = i.toLong(),
                title = "Expense #$i",
                amount = (i * 10).toDouble(),
                type = TransactionType.EXPENSE,
                category = foodCategory,
                account = bankAccount,
                dateEpochMillis = fixedReferenceTime - (i * 3600000L)
            )
        }

        val start = System.nanoTime()
        val res = AskCrebitEngine.query(
            "How much did I spend on food this month?",
            txns,
            testAccounts,
            testCategories,
            referenceTimeMillis = fixedReferenceTime
        )
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000.0

        assertNotNull(res)
        assertTrue("Expected execution under 10ms, but was ${elapsedMillis}ms", elapsedMillis < 10.0)
    }
}

