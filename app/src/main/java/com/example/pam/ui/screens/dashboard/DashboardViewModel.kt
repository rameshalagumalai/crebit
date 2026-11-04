package com.example.pam.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.data.repository.PAMRepository
import com.example.pam.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val selectedPeriod: DateRangeType = DateRangeType.THIS_MONTH,
    val periodOverview: FinancialOverview = FinancialOverview(0.0, 0.0, 0.0, 0f, 0),
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val totalReceivable: Double = 0.0,
    val activeSplitsCount: Int = 0,
    val isLoading: Boolean = true
) {
    val monthlyOverview: FinancialOverview get() = periodOverview
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _selectedPeriod = MutableStateFlow(DateRangeType.THIS_MONTH)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var overviewJob: Job? = null

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            repository.getAccountsFlow().collectLatest { accountsList ->
                val totalNetWorth = accountsList.sumOf { acc ->
                    if (acc.type == AccountType.CREDIT_CARD) {
                        -acc.currentBalance // Credit card balance is owed/debt
                    } else {
                        acc.currentBalance
                    }
                }
                _uiState.update {
                    it.copy(
                        accounts = accountsList,
                        totalBalance = totalNetWorth,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getTransactionsFlow(limit = 15).collectLatest { txns ->
                _uiState.update { it.copy(recentTransactions = txns) }
            }
        }

        viewModelScope.launch {
            repository.getSplitGroupsFlow().collectLatest { splits ->
                val active = splits.filter { it.splitGroup.status == com.example.pam.data.model.SplitStatus.OPEN }
                val receivable = active.sumOf { it.remainingAmount }
                _uiState.update {
                    it.copy(
                        totalReceivable = receivable,
                        activeSplitsCount = active.size
                    )
                }
            }
        }

        viewModelScope.launch {
            _selectedPeriod.collectLatest { period ->
                _uiState.update { it.copy(selectedPeriod = period) }
                val (startDate, endDate) = DateUtils.getDateRange(period)
                overviewJob?.cancel()
                overviewJob = viewModelScope.launch {
                    repository.getReportsFlow(startDate, endDate).collectLatest { (overview, _, _) ->
                        _uiState.update { it.copy(periodOverview = overview) }
                    }
                }
            }
        }
    }

    fun onPeriodSelected(period: DateRangeType) {
        if (period == DateRangeType.THIS_MONTH || period == DateRangeType.THIS_WEEK) {
            _selectedPeriod.value = period
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun deleteTransactions(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteTransactions(ids.toList())
        }
    }
}
