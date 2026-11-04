package com.example.pam.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountExpenseSummary
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryExpenseSummary
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.data.repository.PAMRepository
import com.example.pam.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class SelectedBreakdownTarget {
    data class CategoryTarget(
        val category: Category,
        val totalAmount: Double,
        val count: Int
    ) : SelectedBreakdownTarget()

    data class AccountTarget(
        val account: Account,
        val totalAmount: Double,
        val count: Int
    ) : SelectedBreakdownTarget()
}

data class ReportsUiState(
    val selectedRange: DateRangeType = DateRangeType.THIS_MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val overview: FinancialOverview = FinancialOverview(0.0, 0.0, 0.0, 0f, 0),
    val categoryBreakdown: List<CategoryExpenseSummary> = emptyList(),
    val accountBreakdown: List<AccountExpenseSummary> = emptyList(),
    val isLoading: Boolean = false,
    val selectedBreakdownTarget: SelectedBreakdownTarget? = null,
    val breakdownTransactions: List<TransactionWithDetails> = emptyList(),
    val isBreakdownLoading: Boolean = false
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private var reportsJob: Job? = null
    private var breakdownJob: Job? = null

    init {
        loadReportsForRange(DateRangeType.THIS_MONTH)
    }

    fun onRangeSelected(rangeType: DateRangeType, customStart: Long? = null, customEnd: Long? = null) {
        _uiState.value = _uiState.value.copy(
            selectedRange = rangeType,
            customStartDate = customStart,
            customEndDate = customEnd,
            selectedBreakdownTarget = null,
            breakdownTransactions = emptyList(),
            isBreakdownLoading = false,
            isLoading = true
        )
        loadReportsForRange(rangeType, customStart, customEnd)
    }

    private fun loadReportsForRange(rangeType: DateRangeType, customStart: Long? = null, customEnd: Long? = null) {
        reportsJob?.cancel()
        val (start, end) = DateUtils.getDateRange(rangeType, customStart, customEnd)
        reportsJob = viewModelScope.launch {
            repository.getReportsFlow(start, end).collectLatest { (overview, categories, accounts) ->
                _uiState.value = _uiState.value.copy(
                    overview = overview,
                    categoryBreakdown = categories,
                    accountBreakdown = accounts,
                    isLoading = false
                )
            }
        }
    }

    fun onCategoryClicked(summary: CategoryExpenseSummary) {
        _uiState.value = _uiState.value.copy(
            selectedBreakdownTarget = SelectedBreakdownTarget.CategoryTarget(
                category = summary.category,
                totalAmount = summary.totalAmount,
                count = summary.count
            ),
            isBreakdownLoading = true
        )
        loadBreakdownTransactions(categoryId = summary.category.id, accountId = null)
    }

    fun onAccountClicked(summary: AccountExpenseSummary) {
        _uiState.value = _uiState.value.copy(
            selectedBreakdownTarget = SelectedBreakdownTarget.AccountTarget(
                account = summary.account,
                totalAmount = summary.totalAmount,
                count = summary.count
            ),
            isBreakdownLoading = true
        )
        loadBreakdownTransactions(categoryId = null, accountId = summary.account.id)
    }

    fun onDismissBreakdown() {
        breakdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            selectedBreakdownTarget = null,
            breakdownTransactions = emptyList(),
            isBreakdownLoading = false
        )
    }

    private fun loadBreakdownTransactions(categoryId: Long?, accountId: Long?) {
        breakdownJob?.cancel()
        val (start, end) = DateUtils.getDateRange(
            _uiState.value.selectedRange,
            _uiState.value.customStartDate,
            _uiState.value.customEndDate
        )
        breakdownJob = viewModelScope.launch {
            repository.getTransactionsFlow(
                type = TransactionType.EXPENSE,
                accountId = accountId,
                categoryId = categoryId,
                startDate = start,
                endDate = end,
                limit = 500
            ).collectLatest { txns ->
                _uiState.value = _uiState.value.copy(
                    breakdownTransactions = txns,
                    isBreakdownLoading = false
                )
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }
}
