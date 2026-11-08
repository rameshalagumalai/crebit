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

import com.example.pam.data.model.Category
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.util.ai.SpendingAnomaly
import com.example.pam.util.ai.SpendingAnomalyDetector
import com.example.pam.util.ai.SmartNarrativeDigest
import com.example.pam.util.ai.SmartNarrativeDigestEngine

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val selectedPeriod: DateRangeType = DateRangeType.THIS_MONTH,
    val periodOverview: FinancialOverview = FinancialOverview(0.0, 0.0, 0.0, 0f, 0),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val recentTransactions: List<TransactionWithDetails> = emptyList(),
    val allSplits: List<SplitGroupWithDetails> = emptyList(),
    val anomalies: List<SpendingAnomaly> = emptyList(),
    val dismissedAnomalyIds: Set<String> = emptySet(),
    val totalReceivable: Double = 0.0,
    val activeSplitsCount: Int = 0,
    val smartDigest: SmartNarrativeDigest? = null,
    val isLoading: Boolean = true
) {
    val monthlyOverview: FinancialOverview get() = periodOverview
    val activeAnomalies: List<SpendingAnomaly> get() = anomalies.filter { it.id !in dismissedAnomalyIds }
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
            repository.getCategoriesFlow().collectLatest { cats ->
                _uiState.update { it.copy(categories = cats) }
                recomputeDigest()
            }
        }

        viewModelScope.launch {
            repository.getTransactionsFlow(limit = 1000).collectLatest { allTxns ->
                val detected = SpendingAnomalyDetector.detectSpendingAnomalies(allTxns)
                _uiState.update {
                    it.copy(
                        allTransactions = allTxns,
                        recentTransactions = allTxns.take(15),
                        anomalies = detected
                    )
                }
                recomputeDigest()
            }
        }

        viewModelScope.launch {
            repository.getSplitGroupsFlow().collectLatest { splits ->
                val active = splits.filter { it.splitGroup.status == com.example.pam.data.model.SplitStatus.OPEN }
                val receivable = active.sumOf { it.remainingAmount }
                _uiState.update {
                    it.copy(
                        allSplits = splits,
                        totalReceivable = receivable,
                        activeSplitsCount = active.size
                    )
                }
                recomputeDigest()
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
                        recomputeDigest()
                    }
                }
            }
        }
    }

    private fun recomputeDigest() {
        val currentState = _uiState.value
        val digest = SmartNarrativeDigestEngine.generateDigest(
            transactions = currentState.allTransactions,
            overview = currentState.periodOverview,
            dateRangeType = currentState.selectedPeriod,
            splits = currentState.allSplits,
            categories = currentState.categories
        )
        _uiState.update { it.copy(smartDigest = digest) }
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

    fun dismissAnomaly(anomalyId: String) {
        _uiState.update { it.copy(dismissedAnomalyIds = it.dismissedAnomalyIds + anomalyId) }
    }

    fun saveQuickTransaction(transaction: Transaction, isSplit: Boolean, splitPeopleCount: Int) {
        viewModelScope.launch {
            if (isSplit && transaction.type == TransactionType.EXPENSE) {
                val people = maxOf(2, splitPeopleCount)
                val myShare = transaction.amount / people
                repository.createSplitExpense(
                    title = transaction.title,
                    totalAmount = transaction.amount,
                    myShare = myShare,
                    accountId = transaction.accountId,
                    categoryId = transaction.categoryId ?: 1L,
                    dateEpochMillis = transaction.dateEpochMillis,
                    note = transaction.note
                )
            } else {
                repository.addTransaction(transaction)
            }
        }
    }
}
