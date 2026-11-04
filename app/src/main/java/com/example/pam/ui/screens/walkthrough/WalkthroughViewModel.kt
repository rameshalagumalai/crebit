package com.example.pam.ui.screens.walkthrough

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.repository.PAMRepository
import com.example.pam.util.OnboardingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WalkthroughUiState(
    val accountName: String = "Main Bank",
    val accountType: AccountType = AccountType.BANK,
    val startingBalance: String = "",
    val includeStarterCategories: Boolean = false,
    val isCompleting: Boolean = false
)

class WalkthroughViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PAMRepository.getInstance(application)

    private val _uiState = MutableStateFlow(WalkthroughUiState())
    val uiState: StateFlow<WalkthroughUiState> = _uiState.asStateFlow()

    fun updateAccountName(name: String) {
        _uiState.value = _uiState.value.copy(accountName = name)
    }

    fun updateAccountType(type: AccountType) {
        _uiState.value = _uiState.value.copy(accountType = type)
    }

    fun updateStartingBalance(balance: String) {
        // Allow digits and at most one decimal point
        val sanitized = balance.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            _uiState.value = _uiState.value.copy(startingBalance = sanitized)
        }
    }

    fun toggleStarterCategories(include: Boolean) {
        _uiState.value = _uiState.value.copy(includeStarterCategories = include)
    }

    fun completeOnboarding(skipSetup: Boolean, onFinished: () -> Unit) {
        if (_uiState.value.isCompleting) return
        _uiState.value = _uiState.value.copy(isCompleting = true)

        viewModelScope.launch {
            try {
                // Ensure General category is always present regardless of what user opts
                repository.getOrCreateGeneralCategory()

                if (!skipSetup) {
                    val state = _uiState.value
                    if (state.accountName.isNotBlank()) {
                        val initBal = state.startingBalance.toDoubleOrNull() ?: 0.0
                        val (colorHex, iconName) = when (state.accountType) {
                            AccountType.BANK -> Pair("#1E88E5", "AccountBalance")
                            AccountType.CASH -> Pair("#43A047", "Payments")
                            AccountType.CREDIT_CARD -> Pair("#E53935", "CreditCard")
                            AccountType.WALLET -> Pair("#8E24AA", "AccountBalanceWallet")
                            AccountType.SAVINGS -> Pair("#00897B", "Savings")
                            AccountType.INVESTMENT -> Pair("#FB8C00", "TrendingUp")
                            AccountType.OTHER -> Pair("#546E7A", "Category")
                        }
                        val account = Account(
                            name = state.accountName.trim(),
                            type = state.accountType,
                            initialBalance = initBal,
                            currentBalance = initBal,
                            colorHex = colorHex,
                            iconName = iconName,
                            isDefault = true
                        )
                        repository.addAccount(account)
                    }

                    if (state.includeStarterCategories) {
                        repository.seedStarterCategories()
                    }
                }

                OnboardingManager.setWalkthroughCompleted(getApplication(), true)
            } finally {
                _uiState.value = _uiState.value.copy(isCompleting = false)
                onFinished()
            }
        }
    }
}

