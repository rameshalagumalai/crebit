package com.example.pam.ui.screens.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.Category
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.SplitGroupWithDetails
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

data class TransactionsFilterState(
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val selectedDateRangeType: DateRangeType = DateRangeType.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
)

data class TransactionsUiState(
    val filterState: TransactionsFilterState = TransactionsFilterState(),
    val transactions: List<TransactionWithDetails> = emptyList(),
    val allTransactions: List<TransactionWithDetails> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val allSplits: List<SplitGroupWithDetails> = emptyList(),
    val totalFilteredIncome: Double = 0.0,
    val totalFilteredExpense: Double = 0.0,
    val isLoading: Boolean = false
)

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private var transactionsJob: Job? = null

    init {
        loadFilterMetadata()
        observeTransactions()
    }

    private fun loadFilterMetadata() {
        viewModelScope.launch {
            repository.getAccountsFlow().collectLatest { accounts ->
                _uiState.value = _uiState.value.copy(accounts = accounts)
            }
        }
        viewModelScope.launch {
            repository.getCategoriesFlow().collectLatest { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
            }
        }
        viewModelScope.launch {
            repository.getTransactionsFlow(limit = 1000).collectLatest { allTxns ->
                _uiState.value = _uiState.value.copy(allTransactions = allTxns)
            }
        }
        viewModelScope.launch {
            repository.getSplitGroupsFlow().collectLatest { splits ->
                _uiState.value = _uiState.value.copy(allSplits = splits)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(
            filterState = _uiState.value.filterState.copy(searchQuery = query)
        )
        observeTransactions()
    }

    fun onTypeSelected(type: TransactionType?) {
        _uiState.value = _uiState.value.copy(
            filterState = _uiState.value.filterState.copy(selectedType = type)
        )
        observeTransactions()
    }

    fun onAccountSelected(accountId: Long?) {
        _uiState.value = _uiState.value.copy(
            filterState = _uiState.value.filterState.copy(selectedAccountId = accountId)
        )
        observeTransactions()
    }

    fun onCategorySelected(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(
            filterState = _uiState.value.filterState.copy(selectedCategoryId = categoryId)
        )
        observeTransactions()
    }

    fun onDateRangeSelected(type: DateRangeType, customStart: Long? = null, customEnd: Long? = null) {
        _uiState.value = _uiState.value.copy(
            filterState = _uiState.value.filterState.copy(
                selectedDateRangeType = type,
                customStartDate = customStart,
                customEndDate = customEnd
            )
        )
        observeTransactions()
    }

    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            filterState = TransactionsFilterState()
        )
        observeTransactions()
    }

    private fun observeTransactions() {
        val filter = _uiState.value.filterState
        val (startDate, endDate) = if (filter.selectedDateRangeType == DateRangeType.ALL_TIME) {
            Pair(null, null)
        } else {
            val range = DateUtils.getDateRange(filter.selectedDateRangeType, filter.customStartDate, filter.customEndDate)
            Pair(range.first, range.second)
        }

        transactionsJob?.cancel()
        transactionsJob = viewModelScope.launch {
            repository.getTransactionsFlow(
                type = filter.selectedType,
                accountId = filter.selectedAccountId,
                categoryId = filter.selectedCategoryId,
                startDate = startDate,
                endDate = endDate,
                searchQuery = filter.searchQuery.ifBlank { null }
            ).collectLatest { txns ->
                var inc = 0.0
                var exp = 0.0
                txns.forEach { item ->
                    when (item.transaction.type) {
                        TransactionType.INCOME -> inc += item.transaction.amount
                        TransactionType.EXPENSE -> exp += item.transaction.amount
                        TransactionType.TRANSFER -> {}
                    }
                }
                _uiState.value = _uiState.value.copy(
                    transactions = txns,
                    totalFilteredIncome = inc,
                    totalFilteredExpense = exp
                )
            }
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
