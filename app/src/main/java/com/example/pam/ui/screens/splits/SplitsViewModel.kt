package com.example.pam.ui.screens.splits

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitSettlementEntry
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.repository.PAMRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

enum class SplitFilter(val displayName: String) {
    ALL("All"),
    OPEN("Active"),
    SETTLED("Settled")
}

data class BatchReceiveRowState(
    val id: String = UUID.randomUUID().toString(),
    val amountText: String = "",
    val selectedAccountId: Long? = null,
    val note: String = "",
    val dateEpochMillis: Long = System.currentTimeMillis()
)

data class SplitsUiState(
    val splits: List<SplitGroupWithDetails> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val filter: SplitFilter = SplitFilter.ALL,
    val searchQuery: String = "",
    val selectedSplitForReceive: SplitGroupWithDetails? = null,
    val batchReceiveRows: List<BatchReceiveRowState> = emptyList(),
    val selectedSplitForDetails: SplitGroupWithDetails? = null,
    val splitToDelete: SplitGroupWithDetails? = null,
    val isLoading: Boolean = false
) {
    val filteredSplits: List<SplitGroupWithDetails>
        get() {
            var list = splits
            // Filter by status
            list = when (filter) {
                SplitFilter.ALL -> list
                SplitFilter.OPEN -> list.filter { it.splitGroup.status == SplitStatus.OPEN }
                SplitFilter.SETTLED -> list.filter { it.splitGroup.status == SplitStatus.SETTLED }
            }
            // Filter by search query
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.splitGroup.description.lowercase().contains(q) ||
                    (it.expenseTransaction?.account?.name?.lowercase()?.contains(q) == true)
                }
            }
            return list
        }

    val totalReceivable: Double
        get() = splits.filter { it.splitGroup.status == SplitStatus.OPEN }.sumOf { it.remainingAmount }

    val totalCollected: Double
        get() = splits.sumOf { it.totalReceived }

    val activeSplitsCount: Int
        get() = splits.count { it.splitGroup.status == SplitStatus.OPEN }
}

sealed class SplitsEvent {
    data class ShowSnackbar(val message: String) : SplitsEvent()
}

class SplitsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _uiState = MutableStateFlow(SplitsUiState())
    val uiState: StateFlow<SplitsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SplitsEvent>()
    val eventFlow: SharedFlow<SplitsEvent> = _eventFlow.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getSplitGroupsFlow().collectLatest { splits ->
                _uiState.value = _uiState.value.copy(splits = splits)
                // If a split details dialog is open, update its reference
                val currentDetailsId = _uiState.value.selectedSplitForDetails?.splitGroup?.id
                if (currentDetailsId != null) {
                    val updated = splits.firstOrNull { it.splitGroup.id == currentDetailsId }
                    _uiState.value = _uiState.value.copy(selectedSplitForDetails = updated)
                }
            }
        }
        viewModelScope.launch {
            repository.getAccountsFlow().collectLatest { accounts ->
                _uiState.value = _uiState.value.copy(accounts = accounts)
            }
        }
    }

    fun setFilter(filter: SplitFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun openSplitDetails(split: SplitGroupWithDetails) {
        _uiState.value = _uiState.value.copy(selectedSplitForDetails = split)
    }

    fun closeSplitDetails() {
        _uiState.value = _uiState.value.copy(selectedSplitForDetails = null)
    }

    // ==========================================
    // BATCH RECEIVE PAYMENTS
    // ==========================================

    fun openReceiveDialog(split: SplitGroupWithDetails) {
        val remaining = split.remainingAmount
        val defaultAccountId = _uiState.value.accounts.firstOrNull { it.isDefault }?.id
            ?: _uiState.value.accounts.firstOrNull()?.id

        val initialRow = BatchReceiveRowState(
            amountText = if (remaining > 0.0) {
                if (remaining % 1.0 == 0.0) remaining.toLong().toString() else "%.2f".format(remaining)
            } else "",
            selectedAccountId = defaultAccountId,
            note = ""
        )

        _uiState.value = _uiState.value.copy(
            selectedSplitForReceive = split,
            batchReceiveRows = listOf(initialRow)
        )
    }

    fun closeReceiveDialog() {
        _uiState.value = _uiState.value.copy(
            selectedSplitForReceive = null,
            batchReceiveRows = emptyList()
        )
    }

    fun addReceiveRow() {
        val defaultAccountId = _uiState.value.accounts.firstOrNull { it.isDefault }?.id
            ?: _uiState.value.accounts.firstOrNull()?.id
        val newRow = BatchReceiveRowState(
            amountText = "",
            selectedAccountId = defaultAccountId,
            note = ""
        )
        _uiState.value = _uiState.value.copy(
            batchReceiveRows = _uiState.value.batchReceiveRows + newRow
        )
    }

    fun removeReceiveRow(rowId: String) {
        if (_uiState.value.batchReceiveRows.size <= 1) return
        _uiState.value = _uiState.value.copy(
            batchReceiveRows = _uiState.value.batchReceiveRows.filter { it.id != rowId }
        )
    }

    fun updateReceiveRowAmount(rowId: String, amountText: String) {
        _uiState.value = _uiState.value.copy(
            batchReceiveRows = _uiState.value.batchReceiveRows.map {
                if (it.id == rowId) it.copy(amountText = amountText) else it
            }
        )
    }

    fun updateReceiveRowAccount(rowId: String, accountId: Long) {
        _uiState.value = _uiState.value.copy(
            batchReceiveRows = _uiState.value.batchReceiveRows.map {
                if (it.id == rowId) it.copy(selectedAccountId = accountId) else it
            }
        )
    }

    fun updateReceiveRowNote(rowId: String, note: String) {
        _uiState.value = _uiState.value.copy(
            batchReceiveRows = _uiState.value.batchReceiveRows.map {
                if (it.id == rowId) it.copy(note = note) else it
            }
        )
    }

    fun updateReceiveRowDate(rowId: String, dateEpochMillis: Long) {
        _uiState.value = _uiState.value.copy(
            batchReceiveRows = _uiState.value.batchReceiveRows.map {
                if (it.id == rowId) it.copy(dateEpochMillis = dateEpochMillis) else it
            }
        )
    }

    fun confirmBatchReceive() {
        val split = _uiState.value.selectedSplitForReceive ?: return
        val rows = _uiState.value.batchReceiveRows

        val entries = mutableListOf<SplitSettlementEntry>()
        for ((index, row) in rows.withIndex()) {
            val amt = row.amountText.toDoubleOrNull()
            if (amt == null || amt <= 0.0) {
                viewModelScope.launch {
                    _eventFlow.emit(SplitsEvent.ShowSnackbar("Please enter a valid amount for entry #${index + 1}"))
                }
                return
            }
            val accId = row.selectedAccountId
            if (accId == null) {
                viewModelScope.launch {
                    _eventFlow.emit(SplitsEvent.ShowSnackbar("Please select a deposit account for entry #${index + 1}"))
                }
                return
            }
            entries.add(
                SplitSettlementEntry(
                    amount = amt,
                    accountId = accId,
                    note = row.note.trim().ifBlank { null },
                    dateEpochMillis = row.dateEpochMillis
                )
            )
        }

        viewModelScope.launch {
            val success = repository.recordSplitSettlementBatch(split.splitGroup.id, entries)
            if (success) {
                val totalCollectedInBatch = entries.sumOf { it.amount }
                closeReceiveDialog()
                val countText = if (entries.size == 1) "payment" else "${entries.size} payments"
                _eventFlow.emit(
                    SplitsEvent.ShowSnackbar("Recorded ₹${if (totalCollectedInBatch % 1.0 == 0.0) totalCollectedInBatch.toLong() else totalCollectedInBatch} across $countText! 🎉")
                )
            } else {
                _eventFlow.emit(SplitsEvent.ShowSnackbar("Failed to record split settlement"))
            }
        }
    }

    // ==========================================
    // DELETE
    // ==========================================

    fun requestDeleteSplit(split: SplitGroupWithDetails) {
        _uiState.value = _uiState.value.copy(splitToDelete = split)
    }

    fun dismissDeleteSplit() {
        _uiState.value = _uiState.value.copy(splitToDelete = null)
    }

    fun confirmDeleteSplit() {
        val split = _uiState.value.splitToDelete ?: return
        viewModelScope.launch {
            repository.deleteSplitGroup(split.splitGroup.id)
            dismissDeleteSplit()
            closeSplitDetails()
            _eventFlow.emit(SplitsEvent.ShowSnackbar("Split removed"))
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            val success = repository.deleteTransaction(transactionId)
            if (success) {
                _eventFlow.emit(SplitsEvent.ShowSnackbar("Transaction deleted"))
            } else {
                _eventFlow.emit(SplitsEvent.ShowSnackbar("Failed to delete transaction"))
            }
        }
    }
}
