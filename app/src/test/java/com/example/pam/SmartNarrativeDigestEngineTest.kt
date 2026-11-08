package com.example.pam

import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.SplitGroup
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.util.ai.NarrativeInsightType
import com.example.pam.util.ai.SmartNarrativeDigestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SmartNarrativeDigestEngineTest {

    private val defaultAccount = Account(id = 1, name = "HDFC Bank", type = AccountType.BANK, currentBalance = 50000.0)
    private val foodCategory = Category(id = 1, name = "Food & Dining", type = CategoryType.EXPENSE)
    private val shoppingCategory = Category(id = 2, name = "Shopping", type = CategoryType.EXPENSE)
    private val rentCategory = Category(id = 3, name = "Rent", type = CategoryType.EXPENSE)

    private fun fixedReferenceTime(year: Int = 2026, month: Int = 10, day: Int = 15): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    @Test
    fun testPeriodTrendDown_generatesPositiveInsight() {
        // Current month: 15 days elapsed with ₹5,000 spend.
        // Previous month: ₹10,000 spend.
        val refTime = fixedReferenceTime(2026, 10, 15) // Nov 15, 2026 (0-indexed month 10 = Nov)

        // Previous month transaction (Oct 15, 2026)
        val prevCal = Calendar.getInstance().apply {
            timeInMillis = refTime
            add(Calendar.MONTH, -1)
        }
        val prevTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 1,
                title = "Shopping spree",
                amount = 10000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = shoppingCategory.id,
                dateEpochMillis = prevCal.timeInMillis
            ),
            account = defaultAccount,
            category = shoppingCategory
        )

        // Current month transaction (Nov 10, 2026)
        val currCal = Calendar.getInstance().apply {
            timeInMillis = refTime
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val currTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Groceries",
                amount = 5000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = currCal.timeInMillis
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(prevTxn, currTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        assertNotNull(digest)
        assertEquals("Monthly", digest.periodLabel)
        val trendInsight = digest.insights.find { it.type == NarrativeInsightType.PERIOD_TREND }
        assertNotNull(trendInsight)
        assertEquals("Spending is down", trendInsight!!.title)
        assertTrue(trendInsight.narrative.contains("less than last month"))
        assertEquals("-50%", trendInsight.metricHighlight)
        assertTrue(digest.isPositiveDelta == true)
    }

    @Test
    fun testPeriodTrendUp_generatesPacingWarning() {
        val refTime = fixedReferenceTime(2026, 10, 15)

        val prevCal = Calendar.getInstance().apply {
            timeInMillis = refTime
            add(Calendar.MONTH, -1)
        }
        val prevTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 1,
                title = "Light spend",
                amount = 4000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = prevCal.timeInMillis
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val currCal = Calendar.getInstance().apply {
            timeInMillis = refTime
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val currTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Big shopping",
                amount = 8000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = shoppingCategory.id,
                dateEpochMillis = currCal.timeInMillis
            ),
            account = defaultAccount,
            category = shoppingCategory
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(prevTxn, currTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        val trendInsight = digest.insights.find { it.type == NarrativeInsightType.PERIOD_TREND }
        assertNotNull(trendInsight)
        assertEquals("Higher spending pace", trendInsight!!.title)
        assertTrue(trendInsight.narrative.contains("100%"))
        assertTrue(digest.isPositiveDelta == false)
    }

    @Test
    fun testMonthEndForecast_onTrackWithSavings() {
        val refTime = fixedReferenceTime(2026, 10, 15) // Day 15 of 30 days in November
        val currTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Rent & groceries",
                amount = 15000.0, // 15,000 / 15 days = 1,000/day. Proj = 30,000
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = rentCategory.id,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = rentCategory
        )
        val incomeTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 3,
                title = "Salary",
                amount = 75000.0, // Income 75,000 >> Projected 30,000
                type = TransactionType.INCOME,
                accountId = defaultAccount.id,
                categoryId = null,
                dateEpochMillis = refTime - (5 * 24 * 60 * 60 * 1000L)
            ),
            account = defaultAccount,
            category = null
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(currTxn, incomeTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        val forecast = digest.insights.find { it.type == NarrativeInsightType.BURN_RATE_FORECAST }
        assertNotNull(forecast)
        assertEquals("Month-end Projection", forecast!!.title)
        assertTrue(forecast.narrative.contains("preserving"))
        assertNotNull(digest.projectedSpend)
        assertNotNull(digest.dailyBurnRate)
        assertEquals(1000.0, digest.dailyBurnRate!!, 1.0)
    }

    @Test
    fun testMonthEndForecast_deficitWarning() {
        val refTime = fixedReferenceTime(2026, 10, 10) // Day 10 of 30 days in November
        val currTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Heavy spend",
                amount = 20000.0, // 20,000 / 10 days = 2,000/day. Proj = 60,000
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = shoppingCategory.id,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = shoppingCategory
        )
        val incomeTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 3,
                title = "Salary",
                amount = 40000.0, // Income 40,000 < Projected 60,000 -> Deficit of 20,000
                type = TransactionType.INCOME,
                accountId = defaultAccount.id,
                categoryId = null,
                dateEpochMillis = refTime - (2 * 24 * 60 * 60 * 1000L)
            ),
            account = defaultAccount,
            category = null
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(currTxn, incomeTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        val forecast = digest.insights.find { it.type == NarrativeInsightType.BURN_RATE_FORECAST }
        assertNotNull(forecast)
        assertEquals("Budget Pace Warning", forecast!!.title)
        assertTrue(forecast.narrative.contains("may exceed income"))
        assertTrue(digest.projectedSavings!! < 0)
    }

    @Test
    fun testTopCategoryHighlight_identifiesDominantSpend() {
        val refTime = fixedReferenceTime(2026, 10, 15)
        val foodTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 1,
                title = "Dinner",
                amount = 8000.0, // 8,000 / 10,000 = 80% of total
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = foodCategory
        )
        val shoppingTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Shirt",
                amount = 2000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = shoppingCategory.id,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = shoppingCategory
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(foodTxn, shoppingTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        val catInsight = digest.insights.find { it.type == NarrativeInsightType.CATEGORY_HIGHLIGHT }
        assertNotNull(catInsight)
        assertEquals("Top Spending Category", catInsight!!.title)
        assertTrue(catInsight.narrative.contains("Food & Dining"))
        assertTrue(catInsight.narrative.contains("80%"))
        assertEquals(foodCategory.id, catInsight.relatedCategoryId)
    }

    @Test
    fun testWeekendSkew_detectsWeekendConcentration() {
        // Saturday Nov 14, 2026 and Sunday Nov 15, 2026
        // Wednesday Nov 11, 2026 (weekday)
        val calSat = Calendar.getInstance().apply {
            set(2026, 10, 14, 14, 0, 0) // Saturday
        }
        val calSun = Calendar.getInstance().apply {
            set(2026, 10, 15, 14, 0, 0) // Sunday
        }
        val calWed = Calendar.getInstance().apply {
            set(2026, 10, 11, 14, 0, 0) // Wednesday
        }

        val satTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 1,
                title = "Weekend Party",
                amount = 3000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = calSat.timeInMillis
            ),
            account = defaultAccount,
            category = foodCategory
        )
        val sunTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Weekend Brunch",
                amount = 2000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = calSun.timeInMillis
            ),
            account = defaultAccount,
            category = foodCategory
        )
        val wedTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 3,
                title = "Weekday Lunch",
                amount = 500.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = calWed.timeInMillis
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(satTxn, sunTxn, wedTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = calSun.timeInMillis
        )

        val skew = digest.insights.find { it.type == NarrativeInsightType.WEEKEND_SKEW }
        assertNotNull(skew)
        assertEquals("Weekend Spending Skew", skew!!.title)
        assertTrue(skew.narrative.contains("higher than weekdays"))
    }

    @Test
    fun testSplitReceivables_calculatesRecoveryPercent() {
        val refTime = fixedReferenceTime(2026, 10, 15)
        val currTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 1,
                title = "Outing",
                amount = 10000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = foodCategory.id,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = foodCategory
        )

        val openSplitGroup = SplitGroup(
            id = 10,
            description = "Goa Trip",
            totalAmount = 6000.0,
            myShare = 2000.0,
            receivableAmount = 4000.0,
            expenseTransactionId = 1,
            status = SplitStatus.OPEN
        )
        val splitDetails = SplitGroupWithDetails(
            splitGroup = openSplitGroup,
            expenseTransaction = currTxn,
            settlementTransactions = emptyList(),
            totalReceived = 1500.0,
            remainingAmount = 2500.0 // 2,500 pending / 10,000 spend = 25% recovery
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(currTxn),
            splits = listOf(splitDetails),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        val splitInsight = digest.insights.find { it.type == NarrativeInsightType.SPLIT_RECEIVABLE }
        assertNotNull(splitInsight)
        assertEquals("Pending Split Receivables", splitInsight!!.title)
        assertTrue(splitInsight.narrative.contains("25%"))
    }

    @Test
    fun testMilestoneSavings_highSavingsRate() {
        val refTime = fixedReferenceTime(2026, 10, 15)
        val currTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 1,
                title = "Utility",
                amount = 2000.0,
                type = TransactionType.EXPENSE,
                accountId = defaultAccount.id,
                categoryId = null,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = null
        )
        val incomeTxn = TransactionWithDetails(
            transaction = Transaction(
                id = 2,
                title = "Salary",
                amount = 20000.0, // 2,000 spend / 20,000 income -> 90% savings rate
                type = TransactionType.INCOME,
                accountId = defaultAccount.id,
                categoryId = null,
                dateEpochMillis = refTime
            ),
            account = defaultAccount,
            category = null
        )

        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = listOf(currTxn, incomeTxn),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        val milestone = digest.insights.find { it.type == NarrativeInsightType.MILESTONE_SAVINGS }
        assertNotNull(milestone)
        assertEquals("Healthy Savings Rate", milestone!!.title)
        assertTrue(milestone.narrative.contains("90%"))
    }

    @Test
    fun testEmptyTransactions_handlesGracefullyWithoutCrash() {
        val refTime = fixedReferenceTime(2026, 10, 15)
        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = emptyList(),
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )

        assertNotNull(digest)
        assertEquals("Monthly", digest.periodLabel)
        assertEquals(1, digest.insights.size)
        assertEquals("Clean Slate", digest.insights[0].title)
    }

    @Test
    fun testSub5msPerformance_runsDeterministicallyFast() {
        val refTime = fixedReferenceTime(2026, 10, 15)
        val dummyList = (1..100).map { i ->
            TransactionWithDetails(
                transaction = Transaction(
                    id = i.toLong(),
                    title = "Txn #$i",
                    amount = (50 * i).toDouble(),
                    type = if (i % 5 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    accountId = defaultAccount.id,
                    categoryId = foodCategory.id,
                    dateEpochMillis = refTime - (i * 3600000L)
                ),
                account = defaultAccount,
                category = foodCategory
            )
        }

        val startTime = System.nanoTime()
        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = dummyList,
            dateRangeType = DateRangeType.THIS_MONTH,
            referenceTimeMillis = refTime
        )
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0

        assertNotNull(digest)
        assertTrue("Expected execution under 10ms, but was ${elapsedMs}ms", elapsedMs < 20.0)
    }
}

