package com.example.pam.ui.screens.transactions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.ui.components.ConfirmDeleteDialog
import com.example.pam.ui.components.DateDividerChip
import com.example.pam.ui.components.PAMDateRangePickerDialog
import com.example.pam.ui.components.PAMEmptyState
import com.example.pam.ui.components.TransactionDetailsDialog
import com.example.pam.ui.components.TransactionItemCard
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactionDetail: (id: Long) -> Unit,
    viewModel: TransactionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var selectedTransactionIds by remember { mutableStateOf(emptySet<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    val isSelectionMode = selectedTransactionIds.isNotEmpty()

    val groupedTransactions = remember(uiState.transactions) {
        uiState.transactions.groupBy { DateUtils.formatRelativeDate(it.transaction.dateEpochMillis) }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedTransactionIds = emptySet()
    }

    if (showCustomDatePicker) {
        PAMDateRangePickerDialog(
            initialStartDateMillis = uiState.filterState.customStartDate,
            initialEndDateMillis = uiState.filterState.customEndDate,
            onConfirm = { start, end ->
                viewModel.onDateRangeSelected(DateRangeType.CUSTOM, start, end)
                showCustomDatePicker = false
            },
            onDismiss = {
                showCustomDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedTransactionIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel selection"
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "${selectedTransactionIds.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        val allSelected = uiState.transactions.isNotEmpty() && selectedTransactionIds.size == uiState.transactions.size
                        IconButton(
                            onClick = {
                                selectedTransactionIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    uiState.transactions.map { it.transaction.id }.toSet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All"
                            )
                        }

                        IconButton(onClick = { showBatchDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "All Spends & Moves",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            val hasActiveFilters = uiState.filterState.selectedAccountId != null ||
                                    uiState.filterState.selectedCategoryId != null ||
                                    uiState.filterState.selectedDateRangeType != DateRangeType.ALL_TIME

                            Box {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasActiveFilters) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = onNavigateToAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.filterState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Search...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.filterState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Quick Type Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.filterState.selectedType == null,
                        onClick = { viewModel.onTypeSelected(null) },
                        label = { Text("All Moves") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterState.selectedType == TransactionType.EXPENSE,
                        onClick = { viewModel.onTypeSelected(TransactionType.EXPENSE) },
                        label = { Text("Spent") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.2f),
                            selectedLabelColor = ExpenseRed
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterState.selectedType == TransactionType.INCOME,
                        onClick = { viewModel.onTypeSelected(TransactionType.INCOME) },
                        label = { Text("Received") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = 0.2f),
                            selectedLabelColor = IncomeGreen
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterState.selectedType == TransactionType.TRANSFER,
                        onClick = { viewModel.onTypeSelected(TransactionType.TRANSFER) },
                        label = { Text("Moved") }
                    )
                }
            }

            // Active Filters Row (if date/account/category filter is active)
            val hasActiveDateFilter = uiState.filterState.selectedDateRangeType != DateRangeType.ALL_TIME
            val hasActiveAccountFilter = uiState.filterState.selectedAccountId != null
            val hasActiveCategoryFilter = uiState.filterState.selectedCategoryId != null

            if (hasActiveDateFilter || hasActiveAccountFilter || hasActiveCategoryFilter) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasActiveDateFilter) {
                        item {
                            val dateLabel = if (uiState.filterState.selectedDateRangeType == DateRangeType.CUSTOM &&
                                uiState.filterState.customStartDate != null && uiState.filterState.customEndDate != null) {
                                DateUtils.formatDateRange(uiState.filterState.customStartDate!!, uiState.filterState.customEndDate!!)
                            } else {
                                uiState.filterState.selectedDateRangeType.displayName
                            }
                            FilterChip(
                                selected = true,
                                onClick = {
                                    if (uiState.filterState.selectedDateRangeType == DateRangeType.CUSTOM) {
                                        showCustomDatePicker = true
                                    } else {
                                        showFilterSheet = true
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear date filter",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.onDateRangeSelected(DateRangeType.ALL_TIME) }
                                    )
                                },
                                label = { Text(dateLabel) }
                            )
                        }
                    }

                    if (hasActiveAccountFilter) {
                        item {
                            val accountName = uiState.accounts.find { it.id == uiState.filterState.selectedAccountId }?.name ?: "Account"
                            FilterChip(
                                selected = true,
                                onClick = { showFilterSheet = true },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear account filter",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.onAccountSelected(null) }
                                    )
                                },
                                label = { Text(accountName) }
                            )
                        }
                    }

                    if (hasActiveCategoryFilter) {
                        item {
                            val categoryName = uiState.categories.find { it.id == uiState.filterState.selectedCategoryId }?.name ?: "Category"
                            FilterChip(
                                selected = true,
                                onClick = { showFilterSheet = true },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear category filter",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.onCategorySelected(null) }
                                    )
                                },
                                label = { Text(categoryName) }
                            )
                        }
                    }

                    item {
                        TextButton(
                            onClick = { viewModel.resetFilters() },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Summary Info Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.transactions.size} transactions found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (uiState.totalFilteredIncome > 0) {
                            Text(
                                text = CurrencyFormatter.formatCompact(uiState.totalFilteredIncome),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                        if (uiState.totalFilteredExpense > 0) {
                            Text(
                                text = CurrencyFormatter.formatCompact(uiState.totalFilteredExpense),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Transaction List
            if (uiState.transactions.isEmpty()) {
                val hasAnyFilter = hasActiveDateFilter || hasActiveAccountFilter || hasActiveCategoryFilter ||
                        uiState.filterState.selectedType != null || uiState.filterState.searchQuery.isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    PAMEmptyState(
                        emoji = "🕵️‍♂️",
                        title = "Nothing found here!",
                        description = "Try switching filters or typing a different search query.",
                        actionButtonText = if (hasAnyFilter) "Reset Filters" else null,
                        onActionClick = { viewModel.resetFilters() }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedTransactions.forEach { (dateLabel, txnsForDate) ->
                        item(key = "txns_date_$dateLabel") {
                            DateDividerChip(
                                dateLabel = dateLabel,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(txnsForDate, key = { it.transaction.id }) { item ->
                            val txnId = item.transaction.id
                            val isSelected = selectedTransactionIds.contains(txnId)
                            TransactionItemCard(
                                item = item,
                                isSelected = isSelected,
                                isInSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedTransactionIds = if (isSelected) {
                                            selectedTransactionIds - txnId
                                        } else {
                                            selectedTransactionIds + txnId
                                        }
                                    } else {
                                        selectedTransactionForDetails = item
                                    }
                                },
                                onLongClick = {
                                    selectedTransactionIds = if (isSelected) {
                                        selectedTransactionIds - txnId
                                    } else {
                                        selectedTransactionIds + txnId
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedTransactionForDetails?.let { txnDetails ->
        TransactionDetailsDialog(
            item = txnDetails,
            onDismiss = { selectedTransactionForDetails = null },
            onEdit = { id ->
                selectedTransactionForDetails = null
                onNavigateToTransactionDetail(id)
            },
            onDelete = { id ->
                selectedTransactionForDetails = null
                viewModel.deleteTransaction(id)
            }
        )
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteConfirm && selectedTransactionIds.isNotEmpty()) {
        val count = selectedTransactionIds.size
        ConfirmDeleteDialog(
            title = if (count == 1) "Delete Transaction?" else "Delete $count Transactions?",
            message = if (count == 1)
                "Are you sure you want to delete this transaction? Account balance will be restored accordingly."
            else
                "Are you sure you want to delete these $count transactions? Account balances will be restored accordingly.",
            onConfirm = {
                val toDelete = selectedTransactionIds
                selectedTransactionIds = emptySet()
                showBatchDeleteConfirm = false
                viewModel.deleteTransactions(toDelete)
            },
            onDismiss = { showBatchDeleteConfirm = false }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.resetFilters() }) {
                        Text("Reset All")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Range Filter
                Text(
                    text = "Time Period",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DateRangeType.values()) { rangeType ->
                        val isSelected = uiState.filterState.selectedDateRangeType == rangeType
                        val label = if (rangeType == DateRangeType.CUSTOM && isSelected &&
                            uiState.filterState.customStartDate != null && uiState.filterState.customEndDate != null) {
                            DateUtils.formatDateRange(uiState.filterState.customStartDate!!, uiState.filterState.customEndDate!!)
                        } else {
                            rangeType.displayName
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (rangeType == DateRangeType.CUSTOM) {
                                    showCustomDatePicker = true
                                } else {
                                    viewModel.onDateRangeSelected(rangeType)
                                }
                            },
                            leadingIcon = if (rangeType == DateRangeType.CUSTOM) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Filter
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.filterState.selectedAccountId == null,
                            onClick = { viewModel.onAccountSelected(null) },
                            label = { Text("All Accounts") }
                        )
                    }
                    items(uiState.accounts) { acc ->
                        FilterChip(
                            selected = uiState.filterState.selectedAccountId == acc.id,
                            onClick = { viewModel.onAccountSelected(acc.id) },
                            label = { Text(acc.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Filter
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.filterState.selectedCategoryId == null,
                            onClick = { viewModel.onCategorySelected(null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(uiState.categories) { cat ->
                        FilterChip(
                            selected = uiState.filterState.selectedCategoryId == cat.id,
                            onClick = { viewModel.onCategorySelected(cat.id) },
                            label = { Text(cat.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = { showFilterSheet = false },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Apply Filters",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
