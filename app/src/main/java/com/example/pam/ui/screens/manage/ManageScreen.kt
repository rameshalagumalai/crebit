package com.example.pam.ui.screens.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pam.data.model.Account
import com.example.pam.data.model.AccountType
import com.example.pam.data.model.Category
import com.example.pam.data.model.CategoryType
import com.example.pam.ui.components.AccountCard
import com.example.pam.ui.components.CategoryItemCard
import com.example.pam.ui.components.ColorPickerRow
import com.example.pam.ui.components.ConfirmDeleteDialog
import com.example.pam.ui.components.IconPickerGrid
import com.example.pam.ui.components.PAMEmptyState
import com.example.pam.ui.theme.toComposeColor
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(
    viewModel: AccountsCategoriesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Spaces",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.selectedTab == 0) viewModel.openAddCategoryDialog()
                    else viewModel.openAddAccountDialog()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (uiState.selectedTab == 0) "Add Category" else "Add Account"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Categories (${uiState.categories.size})") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Accounts (${uiState.accounts.size})") }
                )
            }

            if (uiState.selectedTab == 0) {
                // Categories Tab with Filter Chips (All, Expense, Income, Both)
                Column(modifier = Modifier.fillMaxSize()) {
                    val allCount = uiState.categories.size
                    val expenseCount = uiState.categories.count { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
                    val incomeCount = uiState.categories.count { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH }
                    val bothCount = uiState.categories.count { it.type == CategoryType.BOTH }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.categoryFilter == null,
                                onClick = { viewModel.selectCategoryFilter(null) },
                                label = { Text("All ($allCount)") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.categoryFilter == CategoryType.EXPENSE,
                                onClick = { viewModel.selectCategoryFilter(CategoryType.EXPENSE) },
                                label = { Text("Expense ($expenseCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                                    selectedLabelColor = ExpenseRed
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.categoryFilter == CategoryType.INCOME,
                                onClick = { viewModel.selectCategoryFilter(CategoryType.INCOME) },
                                label = { Text("Income ($incomeCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = IncomeGreen
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.categoryFilter == CategoryType.BOTH,
                                onClick = { viewModel.selectCategoryFilter(CategoryType.BOTH) },
                                label = { Text("Both ($bothCount)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    val filteredCategories = when (uiState.categoryFilter) {
                        null -> uiState.categories
                        CategoryType.EXPENSE -> uiState.categories.filter { it.type == CategoryType.EXPENSE || it.type == CategoryType.BOTH }
                        CategoryType.INCOME -> uiState.categories.filter { it.type == CategoryType.INCOME || it.type == CategoryType.BOTH }
                        CategoryType.BOTH -> uiState.categories.filter { it.type == CategoryType.BOTH }
                    }

                    if (filteredCategories.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PAMEmptyState(
                                emoji = "🏷️",
                                title = if (uiState.categories.isEmpty()) "No categories yet" else "No categories found",
                                description = if (uiState.categories.isEmpty()) "Start organizing your finances by adding your first category, or load our essential starter pack." else "Try selecting a different filter or tap + to create a new category.",
                                actionButtonText = "Add Category",
                                onActionClick = { viewModel.openAddCategoryDialog() }
                            )
                            if (uiState.categories.isEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.seedStarterCategories() },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Load Essential Starter Categories")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCategories, key = { it.id }) { cat ->
                                CategoryItemCard(
                                    category = cat,
                                    onEdit = { viewModel.openEditCategoryDialog(cat) },
                                    onDelete = { viewModel.confirmDeleteCategory(cat) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Accounts Tab
                if (uiState.accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        PAMEmptyState(
                            emoji = "🏦",
                            title = "No accounts added yet",
                            description = "Add your bank accounts, credit cards, or cash wallets to start tracking your net worth.",
                            actionButtonText = "Add Account",
                            onActionClick = { viewModel.openAddAccountDialog() }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.accounts, key = { it.id }) { account ->
                            AccountCard(
                                account = account,
                                onClick = { viewModel.openEditAccountDialog(account) },
                                onEdit = { viewModel.openEditAccountDialog(account) },
                                onDelete = { viewModel.confirmDeleteAccount(account) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Account Dialog
    if (uiState.isAccountDialogVisible) {
        AccountEditDialog(
            account = uiState.editingAccount,
            onDismiss = { viewModel.dismissAccountDialog() },
            onSave = { name, type, initBal, accNum, color, icon, isDef ->
                viewModel.saveAccount(name, type, initBal, accNum, color, icon, isDef)
            }
        )
    }

    // Add / Edit Category Dialog
    if (uiState.isCategoryDialogVisible) {
        CategoryEditDialog(
            category = uiState.editingCategory,
            defaultType = uiState.categoryFilter ?: CategoryType.EXPENSE,
            onDismiss = { viewModel.dismissCategoryDialog() },
            onSave = { name, type, color, icon ->
                viewModel.saveCategory(name, type, color, icon)
            }
        )
    }

    // Confirm Delete Account
    if (uiState.isDeleteAccountDialogVisible && uiState.accountToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Account?",
            message = "Are you sure you want to delete '${uiState.accountToDelete?.name}'? All transactions associated with this account will also be deleted.",
            onConfirm = { viewModel.executeDeleteAccount() },
            onDismiss = { viewModel.dismissDeleteAccountDialog() }
        )
    }

    // Confirm Delete Category
    if (uiState.isDeleteCategoryDialogVisible && uiState.categoryToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Category?",
            message = "Are you sure you want to delete category '${uiState.categoryToDelete?.name}'? Transactions under this category will be unassigned.",
            onConfirm = { viewModel.executeDeleteCategory() },
            onDismiss = { viewModel.dismissDeleteCategoryDialog() }
        )
    }
}

@Composable
fun AccountEditDialog(
    account: Account?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: AccountType,
        initialBalance: Double,
        accountNumber: String?,
        colorHex: String,
        iconName: String,
        isDefault: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var selectedType by remember { mutableStateOf(account?.type ?: AccountType.BANK) }
    var initialBalanceText by remember { mutableStateOf(account?.initialBalance?.toString() ?: "0.0") }
    var accountNumber by remember { mutableStateOf(account?.accountNumber ?: "") }
    var selectedColor by remember { mutableStateOf(account?.colorHex ?: "#1E88E5") }
    var selectedIcon by remember { mutableStateOf(account?.iconName ?: "AccountBalance") }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }

    var typeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (account == null) "New Account" else "Edit Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Type Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.clickable { typeMenuExpanded = true }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { typeMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Initial Balance (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number / Last 4 Digits (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Account Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorPickerRow(
                    selectedHex = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                Text(
                    text = "Account Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val colorParsed = selectedColor.toComposeColor(MaterialTheme.colorScheme.primary)
                IconPickerGrid(
                    selectedIconName = selectedIcon,
                    accentColor = colorParsed,
                    onIconSelected = { selectedIcon = it }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Set as default account", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bal = initialBalanceText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedType, bal, accountNumber.trim().ifBlank { null }, selectedColor, selectedIcon, isDefault)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditDialog(
    category: Category?,
    defaultType: CategoryType,
    onDismiss: () -> Unit,
    onSave: (name: String, type: CategoryType, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedType by remember { mutableStateOf(category?.type ?: defaultType) }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#FF7043") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "Category") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (category == null) "New Category" else "Edit Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category Type *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3 Options: Expense, Income, Both
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryType.values().forEach { cType ->
                        val isSelected = selectedType == cType
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedType = cType },
                            label = { Text(cType.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (cType) {
                                    CategoryType.EXPENSE -> ExpenseRed.copy(alpha = 0.2f)
                                    CategoryType.INCOME -> IncomeGreen.copy(alpha = 0.2f)
                                    CategoryType.BOTH -> MaterialTheme.colorScheme.primaryContainer
                                },
                                selectedLabelColor = when (cType) {
                                    CategoryType.EXPENSE -> ExpenseRed
                                    CategoryType.INCOME -> IncomeGreen
                                    CategoryType.BOTH -> MaterialTheme.colorScheme.primary
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = when (selectedType) {
                        CategoryType.EXPENSE -> "This category can be selected when recording expenses."
                        CategoryType.INCOME -> "This category can be selected when recording incomes."
                        CategoryType.BOTH -> "This category can be selected for BOTH expenses and incomes."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Category Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorPickerRow(
                    selectedHex = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                Text(
                    text = "Category Icon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val colorParsed = selectedColor.toComposeColor(MaterialTheme.colorScheme.primary)
                IconPickerGrid(
                    selectedIconName = selectedIcon,
                    accentColor = colorParsed,
                    onIconSelected = { selectedIcon = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedType, selectedColor, selectedIcon)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
