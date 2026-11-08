package com.example.pam.util.ai

import com.example.pam.data.model.Category
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class NarrativeInsightType {
    PERIOD_TREND,
    BURN_RATE_FORECAST,
    CATEGORY_HIGHLIGHT,
    WEEKEND_SKEW,
    SPLIT_RECEIVABLE,
    MILESTONE_SAVINGS
}

data class NarrativeInsight(
    val id: String,
    val iconEmoji: String,
    val title: String,
    val narrative: String,
    val type: NarrativeInsightType,
    val metricHighlight: String? = null,
    val relatedCategoryId: Long? = null,
    val relatedCategoryName: String? = null,
    val filterQuery: String? = null
)

data class SmartNarrativeDigest(
    val periodLabel: String,
    val periodWord: String,
    val insights: List<NarrativeInsight>,
    val projectedSpend: Double? = null,
    val dailyBurnRate: Double? = null,
    val projectedSavings: Double? = null,
    val periodDeltaPercent: Float? = null,
    val isPositiveDelta: Boolean? = null
)

/**
 * 100% Offline, on-device AI narrative digest generator (Feature 6).
 * Analyzes period-over-period spending trends, daily burn rate projections,
 * category concentrations, weekend skews, and split receivables into actionable summaries.
 */
object SmartNarrativeDigestEngine {

    fun generateDigest(
        transactions: List<TransactionWithDetails>,
        overview: FinancialOverview? = null,
        dateRangeType: DateRangeType = DateRangeType.THIS_MONTH,
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        splits: List<SplitGroupWithDetails> = emptyList(),
        categories: List<Category> = emptyList(),
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): SmartNarrativeDigest {
        val (currentStart, currentEnd) = DateUtils.getDateRange(dateRangeType, customStartDate, customEndDate)

        val currentTxns = transactions.filter { it.transaction.dateEpochMillis in currentStart..currentEnd }
        val currentExpenses = currentTxns.filter { it.transaction.type == TransactionType.EXPENSE }
        val currentIncomes = currentTxns.filter { it.transaction.type == TransactionType.INCOME }

        val totalCurrentExpense = currentExpenses.sumOf { it.transaction.amount }
        val totalCurrentIncome = overview?.totalIncome ?: currentIncomes.sumOf { it.transaction.amount }

        val periodWord = when (dateRangeType) {
            DateRangeType.THIS_WEEK -> "week"
            DateRangeType.THIS_MONTH -> "month"
            DateRangeType.TODAY -> "day"
            DateRangeType.THIS_YEAR -> "year"
            else -> "period"
        }

        val periodLabel = when (dateRangeType) {
            DateRangeType.THIS_WEEK -> "Weekly"
            DateRangeType.THIS_MONTH -> "Monthly"
            DateRangeType.TODAY -> "Daily"
            DateRangeType.THIS_YEAR -> "Annual"
            else -> "Financial"
        }

        val candidateInsights = mutableListOf<NarrativeInsight>()

        // 1. Period Trend Analysis (Week-over-Week, Month-over-Month, etc.)
        var periodDeltaPercent: Float? = null
        var isPositiveDelta: Boolean? = null

        val prevRange = getPreviousPeriodRange(dateRangeType, currentStart, currentEnd, referenceTimeMillis)
        if (prevRange != null) {
            val prevTxns = transactions.filter { it.transaction.dateEpochMillis in prevRange.first..prevRange.second }
            val prevExpenses = prevTxns.filter { it.transaction.type == TransactionType.EXPENSE }
            val totalPrevExpense = prevExpenses.sumOf { it.transaction.amount }

            if (totalPrevExpense > 0.0 && totalCurrentExpense > 0.0) {
                val delta = totalCurrentExpense - totalPrevExpense
                val deltaPercent = ((delta / totalPrevExpense) * 100).toFloat()
                val isPositive = delta <= 0
                periodDeltaPercent = deltaPercent
                isPositiveDelta = isPositive

                if (delta < 0) {
                    candidateInsights.add(
                        NarrativeInsight(
                            id = "trend_down",
                            iconEmoji = "🎉",
                            title = "Spending is down",
                            narrative = "You've spent ${CurrencyFormatter.format(abs(delta))} (${abs(deltaPercent).toInt()}%) less than last $periodWord.",
                            type = NarrativeInsightType.PERIOD_TREND,
                            metricHighlight = "-${abs(deltaPercent).toInt()}%",
                            filterQuery = "Compare $periodWord vs last $periodWord"
                        )
                    )
                } else if (delta > 0) {
                    candidateInsights.add(
                        NarrativeInsight(
                            id = "trend_up",
                            iconEmoji = "📈",
                            title = "Higher spending pace",
                            narrative = "Expenses are up by ${CurrencyFormatter.format(delta)} (${deltaPercent.toInt()}%) compared to last $periodWord.",
                            type = NarrativeInsightType.PERIOD_TREND,
                            metricHighlight = "+${deltaPercent.toInt()}%",
                            filterQuery = "Expenses this $periodWord"
                        )
                    )
                }
            } else if (totalCurrentExpense > 0.0) {
                candidateInsights.add(
                    NarrativeInsight(
                        id = "trend_current",
                        iconEmoji = "📊",
                        title = "$periodLabel Outflow",
                        narrative = "Total expenses stand at ${CurrencyFormatter.format(totalCurrentExpense)} across ${currentExpenses.size} transactions.",
                        type = NarrativeInsightType.PERIOD_TREND,
                        filterQuery = "Expenses this $periodWord"
                    )
                )
            }
        }

        // 2. Burn Rate & Projection Forecast
        var dailyBurnRate: Double? = null
        var projectedSpend: Double? = null
        var projectedSavings: Double? = null

        if (totalCurrentExpense > 0.0) {
            if (dateRangeType == DateRangeType.THIS_MONTH) {
                val cal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val daysElapsed = dayOfMonth.coerceAtLeast(1)
                val daysRemaining = (maxDay - daysElapsed).coerceAtLeast(0)

                val burn = totalCurrentExpense / daysElapsed
                val proj = totalCurrentExpense + (burn * daysRemaining)
                dailyBurnRate = burn
                projectedSpend = proj

                val projSav = if (totalCurrentIncome > 0) totalCurrentIncome - proj else null
                projectedSavings = projSav

                if (projSav != null && projSav < 0) {
                    candidateInsights.add(
                        NarrativeInsight(
                            id = "forecast_deficit",
                            iconEmoji = "⚠️",
                            title = "Budget Pace Warning",
                            narrative = "At your current pace (${CurrencyFormatter.format(burn)}/day), projected expenses (${CurrencyFormatter.format(proj)}) may exceed income by ${CurrencyFormatter.format(abs(projSav))}.",
                            type = NarrativeInsightType.BURN_RATE_FORECAST,
                            metricHighlight = "Deficit risk",
                            filterQuery = "Monthly budget"
                        )
                    )
                } else if (projSav != null && projSav >= 0) {
                    candidateInsights.add(
                        NarrativeInsight(
                            id = "forecast_ontrack",
                            iconEmoji = "🔮",
                            title = "Month-end Projection",
                            narrative = "Pacing at ${CurrencyFormatter.format(burn)}/day. Projected month-end expense is ${CurrencyFormatter.format(proj)}, preserving ${CurrencyFormatter.format(projSav)} in savings.",
                            type = NarrativeInsightType.BURN_RATE_FORECAST,
                            metricHighlight = CurrencyFormatter.format(proj),
                            filterQuery = "Net worth"
                        )
                    )
                } else {
                    candidateInsights.add(
                        NarrativeInsight(
                            id = "forecast_general",
                            iconEmoji = "🔮",
                            title = "Month-end Pace",
                            narrative = "Averaging ${CurrencyFormatter.format(burn)}/day across $daysElapsed days. Projected total for the month: ${CurrencyFormatter.format(proj)}.",
                            type = NarrativeInsightType.BURN_RATE_FORECAST,
                            metricHighlight = CurrencyFormatter.format(proj),
                            filterQuery = "Biggest expense this month"
                        )
                    )
                }
            } else if (dateRangeType == DateRangeType.THIS_WEEK) {
                val cal = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    timeInMillis = referenceTimeMillis
                }
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val daysElapsed = when (dow) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    Calendar.SUNDAY -> 7
                    else -> 1
                }
                val daysRemaining = (7 - daysElapsed).coerceAtLeast(0)
                val burn = totalCurrentExpense / daysElapsed
                val proj = totalCurrentExpense + (burn * daysRemaining)
                dailyBurnRate = burn
                projectedSpend = proj

                candidateInsights.add(
                    NarrativeInsight(
                        id = "forecast_week",
                        iconEmoji = "🔮",
                        title = "Week-end Pace",
                        narrative = "Burning ${CurrencyFormatter.format(burn)}/day. Projected week-end expense: ${CurrencyFormatter.format(proj)}.",
                        type = NarrativeInsightType.BURN_RATE_FORECAST,
                        metricHighlight = CurrencyFormatter.format(proj),
                        filterQuery = "Expenses this week"
                    )
                )
            }
        }

        // 3. Category Concentration / Top Category Highlight
        if (currentExpenses.isNotEmpty() && totalCurrentExpense > 0) {
            val catGroups = currentExpenses.groupBy { it.category?.id ?: -1L }
            val topGroup = catGroups.maxByOrNull { entry -> entry.value.sumOf { it.transaction.amount } }
            if (topGroup != null) {
                val topCatTotal = topGroup.value.sumOf { it.transaction.amount }
                val topCatName = topGroup.value.firstOrNull()?.category?.name ?: "Uncategorized"
                val topCatId = topGroup.value.firstOrNull()?.category?.id
                val catPercent = ((topCatTotal / totalCurrentExpense) * 100).toInt()

                if (catPercent >= 20 && topCatTotal >= 100) {
                    candidateInsights.add(
                        NarrativeInsight(
                            id = "cat_top",
                            iconEmoji = "🏷️",
                            title = "Top Spending Category",
                            narrative = "$topCatName accounts for $catPercent% of your spending (${CurrencyFormatter.format(topCatTotal)}) across ${topGroup.value.size} transactions.",
                            type = NarrativeInsightType.CATEGORY_HIGHLIGHT,
                            metricHighlight = "$catPercent%",
                            relatedCategoryId = topCatId,
                            relatedCategoryName = topCatName,
                            filterQuery = "$topCatName this $periodWord"
                        )
                    )
                }
            }
        }

        // 4. Weekend vs Weekday Outflow Skew
        if (currentExpenses.isNotEmpty() && (dateRangeType == DateRangeType.THIS_MONTH || dateRangeType == DateRangeType.THIS_WEEK || (currentEnd - currentStart) >= 4 * 24 * 60 * 60 * 1000L)) {
            var weekendSpend = 0.0
            var weekdaySpend = 0.0
            val weekendDates = mutableSetOf<Int>()
            val weekdayDates = mutableSetOf<Int>()
            val tempCal = Calendar.getInstance()

            currentExpenses.forEach { txn ->
                tempCal.timeInMillis = txn.transaction.dateEpochMillis
                val dow = tempCal.get(Calendar.DAY_OF_WEEK)
                val dayOfYear = tempCal.get(Calendar.DAY_OF_YEAR)
                if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                    weekendSpend += txn.transaction.amount
                    weekendDates.add(dayOfYear)
                } else {
                    weekdaySpend += txn.transaction.amount
                    weekdayDates.add(dayOfYear)
                }
            }

            val weekendDayCount = weekendDates.size.coerceAtLeast(1)
            val weekdayDayCount = weekdayDates.size.coerceAtLeast(1)
            val weekendDaily = weekendSpend / weekendDayCount
            val weekdayDaily = weekdaySpend / weekdayDayCount

            if (weekendSpend >= 500 && weekdayDaily > 0 && weekendDaily >= 1.5 * weekdayDaily) {
                val ratio = (weekendDaily / weekdayDaily).toFloat()
                candidateInsights.add(
                    NarrativeInsight(
                        id = "weekend_skew",
                        iconEmoji = "☕",
                        title = "Weekend Spending Skew",
                        narrative = "Daily spending on weekends averages ${CurrencyFormatter.format(weekendDaily)}, which is ${String.format(Locale.US, "%.1f", ratio)}× higher than weekdays (${CurrencyFormatter.format(weekdayDaily)}/day).",
                        type = NarrativeInsightType.WEEKEND_SKEW,
                        metricHighlight = "${String.format(Locale.US, "%.1f", ratio)}×",
                        filterQuery = "Food this month"
                    )
                )
            }
        }

        // 5. Split Receivables & Liquidity Impact
        val openSplits = splits.filter { it.splitGroup.status == SplitStatus.OPEN && !it.isFullyCollected && it.remainingAmount > 0.01 }
        if (openSplits.isNotEmpty()) {
            val totalPending = openSplits.sumOf { it.remainingAmount }
            val recoveryPercent = if (totalCurrentExpense > 0) ((totalPending / totalCurrentExpense) * 100).toInt() else 0

            val narrative = if (recoveryPercent >= 5) {
                "You have ${CurrencyFormatter.format(totalPending)} in pending split receivables across ${openSplits.size} splits. Settling them recovers $recoveryPercent% of this $periodWord's expenses."
            } else {
                "You have ${CurrencyFormatter.format(totalPending)} pending across ${openSplits.size} active group splits."
            }

            candidateInsights.add(
                NarrativeInsight(
                    id = "splits_receivable",
                    iconEmoji = "💡",
                    title = "Pending Split Receivables",
                    narrative = narrative,
                    type = NarrativeInsightType.SPLIT_RECEIVABLE,
                    metricHighlight = CurrencyFormatter.format(totalPending),
                    filterQuery = "Pending splits"
                )
            )
        }

        // 6. Milestone Savings Rate
        val savingsRate = if (totalCurrentIncome > 0 && totalCurrentIncome > totalCurrentExpense) {
            (((totalCurrentIncome - totalCurrentExpense) / totalCurrentIncome) * 100).toFloat()
        } else overview?.savingsRatePercentage ?: 0f

        if (savingsRate >= 35f && totalCurrentIncome >= 500) {
            candidateInsights.add(
                NarrativeInsight(
                    id = "milestone_savings",
                    iconEmoji = "🏆",
                    title = "Healthy Savings Rate",
                    narrative = "You're saving ${savingsRate.toInt()}% of your income this $periodWord (${CurrencyFormatter.format(totalCurrentIncome - totalCurrentExpense)} net savings).",
                    type = NarrativeInsightType.MILESTONE_SAVINGS,
                    metricHighlight = "${savingsRate.toInt()}% saved",
                    filterQuery = "Income this $periodWord"
                )
            )
        }

        // Empty state fallback if no insights matched
        if (candidateInsights.isEmpty()) {
            candidateInsights.add(
                NarrativeInsight(
                    id = "empty_digest",
                    iconEmoji = "✨",
                    title = "Clean Slate",
                    narrative = "No expenses recorded for this $periodWord yet. You are starting with 100% of your budget intact.",
                    type = NarrativeInsightType.PERIOD_TREND,
                    filterQuery = "Total net worth"
                )
            )
        }

        // Take the top 3-4 insights for maximum clarity without clutter
        val sortedInsights = candidateInsights.distinctBy { it.id }.take(4)

        return SmartNarrativeDigest(
            periodLabel = periodLabel,
            periodWord = periodWord,
            insights = sortedInsights,
            projectedSpend = projectedSpend,
            dailyBurnRate = dailyBurnRate,
            projectedSavings = projectedSavings,
            periodDeltaPercent = periodDeltaPercent,
            isPositiveDelta = isPositiveDelta
        )
    }

    private fun getPreviousPeriodRange(
        dateRangeType: DateRangeType,
        currentStart: Long,
        currentEnd: Long,
        referenceTimeMillis: Long
    ): Pair<Long, Long>? {
        return when (dateRangeType) {
            DateRangeType.THIS_MONTH -> {
                val prevCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
                prevCal.add(Calendar.MONTH, -1)
                prevCal.set(Calendar.DAY_OF_MONTH, 1)
                prevCal.set(Calendar.HOUR_OF_DAY, 0)
                prevCal.set(Calendar.MINUTE, 0)
                prevCal.set(Calendar.SECOND, 0)
                prevCal.set(Calendar.MILLISECOND, 0)
                val pStart = prevCal.timeInMillis

                val maxDay = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                prevCal.set(Calendar.DAY_OF_MONTH, maxDay)
                prevCal.set(Calendar.HOUR_OF_DAY, 23)
                prevCal.set(Calendar.MINUTE, 59)
                prevCal.set(Calendar.SECOND, 59)
                prevCal.set(Calendar.MILLISECOND, 999)
                val pEnd = prevCal.timeInMillis
                Pair(pStart, pEnd)
            }
            DateRangeType.THIS_WEEK -> {
                val pStart = currentStart - (7 * 24 * 60 * 60 * 1000L)
                val pEnd = currentEnd - (7 * 24 * 60 * 60 * 1000L)
                Pair(pStart, pEnd)
            }
            DateRangeType.TODAY -> {
                val pStart = currentStart - (24 * 60 * 60 * 1000L)
                val pEnd = currentEnd - (24 * 60 * 60 * 1000L)
                Pair(pStart, pEnd)
            }
            DateRangeType.THIS_YEAR -> {
                val prevCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
                prevCal.add(Calendar.YEAR, -1)
                prevCal.set(Calendar.DAY_OF_YEAR, 1)
                prevCal.set(Calendar.HOUR_OF_DAY, 0)
                prevCal.set(Calendar.MINUTE, 0)
                prevCal.set(Calendar.SECOND, 0)
                prevCal.set(Calendar.MILLISECOND, 0)
                val pStart = prevCal.timeInMillis

                val maxDay = prevCal.getActualMaximum(Calendar.DAY_OF_YEAR)
                prevCal.set(Calendar.DAY_OF_YEAR, maxDay)
                prevCal.set(Calendar.HOUR_OF_DAY, 23)
                prevCal.set(Calendar.MINUTE, 59)
                prevCal.set(Calendar.SECOND, 59)
                prevCal.set(Calendar.MILLISECOND, 999)
                val pEnd = prevCal.timeInMillis
                Pair(pStart, pEnd)
            }
            DateRangeType.CUSTOM -> {
                if (currentEnd > currentStart && currentStart > 0) {
                    val span = currentEnd - currentStart
                    Pair(currentStart - span, currentStart)
                } else null
            }
            DateRangeType.ALL_TIME -> null
        }
    }
}

