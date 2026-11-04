package com.example.pam.ui.screens.transaction_entry

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.Category
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.repository.PAMRepository
import com.example.pam.util.UpiLaunchResult
import com.example.pam.util.UpiParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class SplitMode {
    EQUAL,
    CUSTOM
}

data class AddEditTransactionUiState(
    val isEditMode: Boolean = false,
    val transactionId: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val title: String = "",
    val selectedAccountId: Long? = null,
    val selectedToAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val payeeUpiId: String = "",
    val payeeName: String = "",
    val upiRefId: String = "",
    val isUpiScanned: Boolean = false,
    val rawUpiUri: String? = null,
    val rawUpiParams: Map<String, String> = emptyMap(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isDeleteDialogVisible: Boolean = false,
    val errorMessage: String? = null,
    val isSplitEnabled: Boolean = false,
    val splitMode: SplitMode = SplitMode.EQUAL,
    val splitPeopleCount: Int = 2,
    val customMyShareText: String = "",
    val existingSplitGroupId: Long? = null
) {
    val computedMyShare: Double
        get() {
            val total = amountText.toDoubleOrNull() ?: 0.0
            return when (splitMode) {
                SplitMode.EQUAL -> {
                    val count = splitPeopleCount.coerceAtLeast(1)
                    total / count
                }
                SplitMode.CUSTOM -> {
                    val customVal = customMyShareText.toDoubleOrNull()
                    customVal ?: (total / 2)
                }
            }
        }

    val computedReceivable: Double
        get() {
            val total = amountText.toDoubleOrNull() ?: 0.0
            return maxOf(0.0, total - computedMyShare)
        }
}

sealed class TransactionEntryEvent {
    data object Saved : TransactionEntryEvent()
    data object Deleted : TransactionEntryEvent()
    data class ShowSnackbar(val message: String) : TransactionEntryEvent()
}

class AddEditTransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<TransactionEntryEvent>()
    val eventFlow: SharedFlow<TransactionEntryEvent> = _eventFlow.asSharedFlow()

    init {
        loadMetadata()
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            repository.getAccountsFlow().collectLatest { accounts ->
                val currentAccId = _uiState.value.selectedAccountId
                val defaultAcc = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    selectedAccountId = currentAccId ?: defaultAcc?.id
                )
            }
        }
        viewModelScope.launch {
            repository.getCategoriesFlow().collectLatest { categories ->
                val currentCatId = _uiState.value.selectedCategoryId
                val matchingCat = categories.firstOrNull { it.isApplicableFor(_uiState.value.type) }
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    selectedCategoryId = currentCatId ?: matchingCat?.id
                )
            }
        }
    }

    fun initData(transactionId: Long?, scannedUpiString: String?, initialType: TransactionType?, enableSplit: Boolean = false) {
        if (transactionId != null && transactionId > 0) {
            viewModelScope.launch {
                val txnWithDetails = repository.getTransactionWithDetails(transactionId)
                if (txnWithDetails != null) {
                    val txn = txnWithDetails.transaction
                    val splitDetails = if (txn.splitGroupId != null) {
                        repository.getSplitGroupWithDetailsById(txn.splitGroupId)
                    } else {
                        val sg = repository.getSplitGroupByExpenseId(txn.id)
                        if (sg != null) repository.getSplitGroupWithDetailsById(sg.id) else null
                    }
                    val existingSplit = splitDetails?.splitGroup
                    val splitCatId = repository.getOrCreateSplitCategory()

                    val resolvedCatId = txn.categoryId

                    val resolvedAmount = existingSplit?.totalAmount ?: txn.amount
                    val resolvedTitle = existingSplit?.description ?: txn.title

                    _uiState.value = _uiState.value.copy(
                        isEditMode = true,
                        transactionId = txn.id,
                        type = txn.type,
                        amountText = if (resolvedAmount % 1.0 == 0.0) resolvedAmount.toLong().toString() else resolvedAmount.toString(),
                        title = resolvedTitle,
                        selectedAccountId = txn.accountId,
                        selectedToAccountId = txn.toAccountId,
                        selectedCategoryId = resolvedCatId,
                        dateEpochMillis = txn.dateEpochMillis,
                        note = txn.note ?: "",
                        payeeUpiId = txn.payeeUpiId ?: "",
                        payeeName = txn.payeeName ?: "",
                        upiRefId = txn.upiRefId ?: "",
                        isUpiScanned = !txn.payeeUpiId.isNullOrBlank(),
                        isSplitEnabled = existingSplit != null,
                        existingSplitGroupId = existingSplit?.id,
                        splitMode = SplitMode.CUSTOM,
                        customMyShareText = existingSplit?.let {
                            if (it.myShare % 1.0 == 0.0) it.myShare.toLong().toString() else it.myShare.toString()
                        } ?: ""
                    )
                }
            }
        } else if (!scannedUpiString.isNullOrBlank()) {
            val parsedUpi = UpiParser.parse(scannedUpiString)
            if (parsedUpi != null) {
                val upiNote = parsedUpi.note?.trim()
                val isNoteUserName = !upiNote.isNullOrBlank() && (
                    upiNote.equals(parsedUpi.payeeName?.trim(), ignoreCase = true) ||
                    upiNote.equals(parsedUpi.upiId.trim(), ignoreCase = true)
                )
                _uiState.value = _uiState.value.copy(
                    isEditMode = false,
                    type = TransactionType.EXPENSE,
                    payeeUpiId = parsedUpi.upiId,
                    payeeName = parsedUpi.payeeName ?: "",
                    amountText = parsedUpi.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
                    title = "",
                    note = if (isNoteUserName) "" else (parsedUpi.note ?: ""),
                    upiRefId = parsedUpi.refId ?: "",
                    isUpiScanned = true,
                    rawUpiUri = parsedUpi.rawUri,
                    rawUpiParams = parsedUpi.rawParams,
                    isSplitEnabled = enableSplit
                )
            }
        } else {
            if (initialType != null) {
                _uiState.value = _uiState.value.copy(type = initialType)
            }
            if (enableSplit) {
                _uiState.value = _uiState.value.copy(
                    type = TransactionType.EXPENSE,
                    isSplitEnabled = true
                )
            }
        }
    }

    fun onSplitEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSplitEnabled = enabled)
    }

    fun onSplitModeChanged(mode: SplitMode) {
        _uiState.value = _uiState.value.copy(splitMode = mode)
    }

    fun onSplitPeopleCountChanged(count: Int) {
        if (count >= 2) {
            _uiState.value = _uiState.value.copy(splitPeopleCount = count)
        }
    }

    fun onCustomMyShareChanged(text: String) {
        _uiState.value = _uiState.value.copy(customMyShareText = text)
    }

    fun onTypeChanged(newType: TransactionType) {
        val firstMatchingCat = _uiState.value.categories.firstOrNull { it.isApplicableFor(newType) }
        _uiState.value = _uiState.value.copy(
            type = newType,
            selectedCategoryId = firstMatchingCat?.id
        )
    }

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountText = amount)
    }

    fun onTitleChanged(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun onAccountSelected(accountId: Long) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
    }

    fun onToAccountSelected(accountId: Long) {
        _uiState.value = _uiState.value.copy(selectedToAccountId = accountId)
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onDateChanged(epochMillis: Long) {
        _uiState.value = _uiState.value.copy(dateEpochMillis = epochMillis)
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onPayeeUpiIdChanged(upiId: String) {
        _uiState.value = _uiState.value.copy(payeeUpiId = upiId)
    }

    fun onPayeeNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(payeeName = name)
    }

    fun addQuickAmount(delta: Double) {
        val current = _uiState.value.amountText.toDoubleOrNull() ?: 0.0
        val newAmt = current + delta
        _uiState.value = _uiState.value.copy(
            amountText = if (newAmt % 1.0 == 0.0) newAmt.toLong().toString() else String.format(java.util.Locale.US, "%.2f", newAmt)
        )
    }

    fun payViaUpi(context: Context) {
        val upiId = _uiState.value.payeeUpiId.trim()
        if (upiId.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("Please enter a valid UPI ID"))
            }
            return
        }

        val amt = _uiState.value.amountText.toDoubleOrNull()
        val payeeName = _uiState.value.payeeName.trim().ifBlank { null }
        val note = _uiState.value.note.trim().ifBlank { _uiState.value.title.trim().ifBlank { null } }

        val upiUri = UpiParser.getPaymentUri(
            rawScannedUri = _uiState.value.rawUpiUri,
            upiId = upiId,
            payeeName = payeeName,
            amount = amt,
            note = note,
            refId = _uiState.value.upiRefId.ifBlank { null },
            rawParams = _uiState.value.rawUpiParams
        )

        val result = UpiParser.launchUpiPayment(context, upiUri)
        viewModelScope.launch {
            when (result) {
                is UpiLaunchResult.Success -> {
                    _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("Opening UPI app..."))
                }
                is UpiLaunchResult.NoAppFound -> {
                    _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("No UPI payment app (GPay, PhonePe, Paytm, etc.) found on device"))
                }
                is UpiLaunchResult.Error -> {
                    _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("Error opening UPI app: ${result.message}"))
                }
            }
        }
    }

    fun saveTransaction() {
        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            viewModelScope.launch {
                _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("Please enter a valid amount"))
            }
            return
        }

        val title = state.title.trim().ifBlank {
            if (state.type == TransactionType.TRANSFER) "Transfer"
            else if (!state.payeeName.isNullOrBlank()) state.payeeName
            else if (!state.payeeUpiId.isNullOrBlank()) "UPI: ${state.payeeUpiId}"
            else state.type.displayName
        }

        val accountId = state.selectedAccountId
        if (accountId == null) {
            viewModelScope.launch {
                _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("Please select an account"))
            }
            return
        }

        if (state.type == TransactionType.TRANSFER) {
            val toAccountId = state.selectedToAccountId
            if (toAccountId == null || toAccountId == accountId) {
                viewModelScope.launch {
                    _eventFlow.emit(TransactionEntryEvent.ShowSnackbar("Please select a different destination account"))
                }
                return
            }
        }

        viewModelScope.launch {
            if (state.type == TransactionType.EXPENSE && state.isSplitEnabled) {
                val myShare = state.computedMyShare
                val splitCatId = repository.getOrCreateSplitCategory()
                val selectedCatId = state.selectedCategoryId ?: splitCatId

                if (state.isEditMode && state.existingSplitGroupId != null) {
                    repository.updateSplitExpense(
                        splitGroupId = state.existingSplitGroupId,
                        title = title,
                        totalAmount = amount,
                        myShare = myShare,
                        accountId = accountId,
                        categoryId = selectedCatId,
                        dateEpochMillis = state.dateEpochMillis,
                        note = state.note.trim().ifBlank { null }
                    )
                } else if (state.isEditMode && state.transactionId != null && state.existingSplitGroupId == null) {
                    val receivable = maxOf(0.0, amount - myShare)
                    repository.splitExistingExpense(
                        transactionId = state.transactionId,
                        myShare = myShare,
                        receivableAmount = receivable
                    )
                } else {
                    repository.createSplitExpense(
                        title = title,
                        totalAmount = amount,
                        myShare = myShare,
                        accountId = accountId,
                        categoryId = selectedCatId,
                        dateEpochMillis = state.dateEpochMillis,
                        note = state.note.trim().ifBlank { null },
                        payeeUpiId = state.payeeUpiId.trim().ifBlank { null },
                        payeeName = state.payeeName.trim().ifBlank { null },
                        upiRefId = state.upiRefId.trim().ifBlank { null },
                        rawUpiUri = state.rawUpiUri
                    )
                }
            } else {
                val txn = Transaction(
                    id = state.transactionId ?: 0,
                    title = title,
                    amount = amount,
                    type = state.type,
                    accountId = accountId,
                    toAccountId = if (state.type == TransactionType.TRANSFER) state.selectedToAccountId else null,
                    categoryId = if (state.type != TransactionType.TRANSFER) state.selectedCategoryId else null,
                    dateEpochMillis = state.dateEpochMillis,
                    note = state.note.trim().ifBlank { null },
                    payeeUpiId = state.payeeUpiId.trim().ifBlank { null },
                    payeeName = state.payeeName.trim().ifBlank { null },
                    upiRefId = state.upiRefId.trim().ifBlank { null },
                    splitGroupId = null
                )

                if (state.isEditMode && state.transactionId != null) {
                    if (state.existingSplitGroupId != null) {
                        repository.removeSplitFromExpense(
                            splitGroupId = state.existingSplitGroupId,
                            keepTransactionId = state.transactionId,
                            totalAmount = amount
                        )
                        repository.updateTransaction(txn.copy(splitGroupId = null))
                    } else {
                        repository.updateTransaction(txn)
                    }
                } else {
                    repository.addTransaction(txn)
                }
            }

            _eventFlow.emit(TransactionEntryEvent.Saved)
        }
    }

    fun openDeleteDialog() {
        _uiState.value = _uiState.value.copy(isDeleteDialogVisible = true)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(isDeleteDialogVisible = false)
    }

    fun deleteTransaction() {
        val id = _uiState.value.transactionId ?: return
        viewModelScope.launch {
            repository.deleteTransaction(id)
            dismissDeleteDialog()
            _eventFlow.emit(TransactionEntryEvent.Deleted)
        }
    }
}
