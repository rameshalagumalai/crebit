package com.example.pam

import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.ui.screens.dashboard.DashboardUiState
import com.example.pam.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTest {

    @Test
    fun testDashboardUiStateDefaults() {
        val defaultState = DashboardUiState()
        // Month must be the default period
        assertEquals(DateRangeType.THIS_MONTH, defaultState.selectedPeriod)
        assertEquals(0.0, defaultState.periodOverview.totalIncome, 0.001)
        assertEquals(0.0, defaultState.periodOverview.totalExpense, 0.001)
        assertEquals(0.0, defaultState.periodOverview.netSavings, 0.001)
        assertEquals(defaultState.periodOverview, defaultState.monthlyOverview)
    }

    @Test
    fun testPeriodSwitchingRanges() {
        val (monthStart, monthEnd) = DateUtils.getDateRange(DateRangeType.THIS_MONTH)
        assertTrue(monthEnd > monthStart)

        val (weekStart, weekEnd) = DateUtils.getDateRange(DateRangeType.THIS_WEEK)
        assertTrue(weekEnd > weekStart)
        // Week range is approximately 7 days (in milliseconds)
        val weekDurationMillis = weekEnd - weekStart
        assertTrue(weekDurationMillis in (6L * 86400000)..(8L * 86400000))
    }

    @Test
    fun testFinancialOverviewNetCalculation() {
        val overview = FinancialOverview(
            totalIncome = 50000.0,
            totalExpense = 15000.0,
            netSavings = 35000.0,
            savingsRatePercentage = 70f,
            transactionCount = 12
        )
        val state = DashboardUiState(
            selectedPeriod = DateRangeType.THIS_WEEK,
            periodOverview = overview
        )
        assertEquals(DateRangeType.THIS_WEEK, state.selectedPeriod)
        assertEquals(50000.0, state.periodOverview.totalIncome, 0.001)
        assertEquals(15000.0, state.periodOverview.totalExpense, 0.001)
        assertEquals(35000.0, state.periodOverview.netSavings, 0.001)
    }
}
