package com.example.pam.ui.screens.manage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.data.repository.PAMRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ManageUiState(
    val selectedTab: Int = 0, // 0 for Categories, 1 for Accounts
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryFilter: CategoryType? = null, // null means "All"
    val isAccountDialogVisible: Boolean = false,
    val editingAccount: Account? = null,
    val isCategoryDialogVisible: Boolean = false,
    val editingCategory: Category? = null,
    val isDeleteAccountDialogVisible: Boolean = false,
    val accountToDelete: Account? = null,
    val isDeleteCategoryDialogVisible: Boolean = false,
    val categoryToDelete: Category? = null
)

class AccountsCategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ManageUiState())
    val uiState: StateFlow<ManageUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
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
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun selectCategoryFilter(filter: CategoryType?) {
        _uiState.value = _uiState.value.copy(categoryFilter = filter)
    }

    // ==========================================
    // ACCOUNT CRUD
    // ==========================================

    fun openAddAccountDialog() {
        _uiState.value = _uiState.value.copy(
            isAccountDialogVisible = true,
            editingAccount = null
        )
    }

    fun openEditAccountDialog(account: Account) {
        _uiState.value = _uiState.value.copy(
            isAccountDialogVisible = true,
            editingAccount = account
        )
    }

    fun dismissAccountDialog() {
        _uiState.value = _uiState.value.copy(
            isAccountDialogVisible = false,
            editingAccount = null
        )
    }

    fun saveAccount(
        name: String,
        type: AccountType,
        initialBalance: Double,
        accountNumber: String?,
        colorHex: String,
        iconName: String,
        isDefault: Boolean
    ) {
        val editing = _uiState.value.editingAccount
        viewModelScope.launch {
            if (editing != null) {
                repository.updateAccount(
                    editing.copy(
                        name = name,
                        type = type,
                        initialBalance = initialBalance,
                        accountNumber = accountNumber,
                        colorHex = colorHex,
                        iconName = iconName,
                        isDefault = isDefault
                    )
                )
            } else {
                repository.addAccount(
                    Account(
                        name = name,
                        type = type,
                        initialBalance = initialBalance,
                        currentBalance = initialBalance,
                        accountNumber = accountNumber,
                        colorHex = colorHex,
                        iconName = iconName,
                        isDefault = isDefault
                    )
                )
            }
            dismissAccountDialog()
        }
    }

    fun confirmDeleteAccount(account: Account) {
        _uiState.value = _uiState.value.copy(
            isDeleteAccountDialogVisible = true,
            accountToDelete = account
        )
    }

    fun dismissDeleteAccountDialog() {
        _uiState.value = _uiState.value.copy(
            isDeleteAccountDialogVisible = false,
            accountToDelete = null
        )
    }

    fun executeDeleteAccount() {
        val account = _uiState.value.accountToDelete ?: return
        viewModelScope.launch {
            repository.deleteAccount(account.id)
            dismissDeleteAccountDialog()
        }
    }

    fun setDefaultAccount(accountId: Long) {
        viewModelScope.launch {
            repository.setDefaultAccount(accountId)
        }
    }

    // ==========================================
    // CATEGORY CRUD
    // ==========================================

    fun openAddCategoryDialog() {
        _uiState.value = _uiState.value.copy(
            isCategoryDialogVisible = true,
            editingCategory = null
        )
    }

    fun openEditCategoryDialog(category: Category) {
        _uiState.value = _uiState.value.copy(
            isCategoryDialogVisible = true,
            editingCategory = category
        )
    }

    fun dismissCategoryDialog() {
        _uiState.value = _uiState.value.copy(
            isCategoryDialogVisible = false,
            editingCategory = null
        )
    }

    fun saveCategory(
        name: String,
        type: CategoryType,
        colorHex: String,
        iconName: String
    ) {
        val editing = _uiState.value.editingCategory
        viewModelScope.launch {
            if (editing != null) {
                repository.updateCategory(
                    editing.copy(
                        name = name,
                        type = type,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
            } else {
                repository.addCategory(
                    Category(
                        name = name,
                        type = type,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
            }
            dismissCategoryDialog()
        }
    }

    fun confirmDeleteCategory(category: Category) {
        _uiState.value = _uiState.value.copy(
            isDeleteCategoryDialogVisible = true,
            categoryToDelete = category
        )
    }

    fun dismissDeleteCategoryDialog() {
        _uiState.value = _uiState.value.copy(
            isDeleteCategoryDialogVisible = false,
            categoryToDelete = null
        )
    }

    fun executeDeleteCategory() {
        val category = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch {
            repository.deleteCategory(category.id)
            dismissDeleteCategoryDialog()
        }
    }

    fun seedStarterCategories() {
        viewModelScope.launch {
            repository.seedStarterCategories()
        }
    }
}
