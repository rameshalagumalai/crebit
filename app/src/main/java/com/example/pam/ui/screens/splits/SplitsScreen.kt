package com.example.pam.ui.screens.splits

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pam.data.model.Account
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.ui.components.ConfirmDeleteDialog
import com.example.pam.ui.components.PAMEmptyState
import com.example.pam.ui.components.TransactionDetailsDialog
import com.example.pam.ui.components.TransactionItemCard
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitsScreen(
    onNavigateToAddExpenseWithSplit: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    viewModel: SplitsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithDetails?>(null) }
    val filteredSplits = remember(uiState.splits, uiState.filter, uiState.searchQuery) {
        uiState.filteredSplits
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SplitsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Splits",
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
                onClick = onNavigateToAddExpenseWithSplit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Split Expense"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero KPI Card
            item {
                SplitsHeroCard(
                    totalReceivable = uiState.totalReceivable,
                    totalCollected = uiState.totalCollected,
                    activeSplitsCount = uiState.activeSplitsCount
                )
            }

            // Filter Chips Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SplitFilter.entries.forEach { filterOption ->
                        val isSelected = uiState.filter == filterOption
                        val count = when (filterOption) {
                            SplitFilter.ALL -> uiState.splits.size
                            SplitFilter.OPEN -> uiState.splits.count { it.splitGroup.status == SplitStatus.OPEN }
                            SplitFilter.SETTLED -> uiState.splits.count { it.splitGroup.status == SplitStatus.SETTLED }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(filterOption) },
                            label = { Text("${filterOption.displayName} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Splits List or Empty State
            if (filteredSplits.isEmpty()) {
                item {
                    EmptySplitsCard(
                        filter = uiState.filter,
                        onNewSplitClick = onNavigateToAddExpenseWithSplit
                    )
                }
            } else {
                items(filteredSplits, key = { it.splitGroup.id }) { splitWithDetails ->
                    SplitItemCard(
                        item = splitWithDetails,
                        onClick = { viewModel.openSplitDetails(splitWithDetails) },
                        onReceiveClick = { viewModel.openReceiveDialog(splitWithDetails) }
                    )
                }
            }
        }
    }

    // Batch Receive Dialog
    if (uiState.selectedSplitForReceive != null) {
        BatchReceiveBottomSheet(
            split = uiState.selectedSplitForReceive!!,
            rows = uiState.batchReceiveRows,
            accounts = uiState.accounts,
            onDismiss = { viewModel.closeReceiveDialog() },
            onAddRow = { viewModel.addReceiveRow() },
            onRemoveRow = { rowId -> viewModel.removeReceiveRow(rowId) },
            onAmountChange = { rowId, text -> viewModel.updateReceiveRowAmount(rowId, text) },
            onAccountChange = { rowId, accId -> viewModel.updateReceiveRowAccount(rowId, accId) },
            onDateChange = { rowId, dateEpochMillis -> viewModel.updateReceiveRowDate(rowId, dateEpochMillis) },
            onNoteChange = { rowId, note -> viewModel.updateReceiveRowNote(rowId, note) },
            onConfirm = { viewModel.confirmBatchReceive() }
        )
    }

    // Split Details Dialog
    if (uiState.selectedSplitForDetails != null) {
        val detailsSplit = uiState.selectedSplitForDetails!!
        SplitDetailsDialog(
            split = detailsSplit,
            onDismiss = { viewModel.closeSplitDetails() },
            onTransactionClick = { txn ->
                selectedTransactionForDetails = txn
            },
            onReceiveClick = {
                viewModel.closeSplitDetails()
                viewModel.openReceiveDialog(detailsSplit)
            },
            onDeleteClick = {
                viewModel.closeSplitDetails()
                viewModel.requestDeleteSplit(detailsSplit)
            }
        )
    }

    // Transaction Details Dialog (for expense / income opened from split details)
    selectedTransactionForDetails?.let { txnDetails ->
        TransactionDetailsDialog(
            item = txnDetails,
            onDismiss = { selectedTransactionForDetails = null },
            onEdit = { id ->
                selectedTransactionForDetails = null
                viewModel.closeSplitDetails()
                onNavigateToTransactionDetail(id)
            },
            onDelete = { id ->
                selectedTransactionForDetails = null
                viewModel.deleteTransaction(id)
            }
        )
    }

    // Delete Split Confirm Dialog
    if (uiState.splitToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Split?",
            message = "This will remove the split tracking. Linked expense and income transactions will remain intact in your history.",
            onConfirm = { viewModel.confirmDeleteSplit() },
            onDismiss = { viewModel.dismissDeleteSplit() }
        )
    }
}

// ==========================================
// HERO SUMMARY CARD
// ==========================================

@Composable
private fun SplitsHeroCard(
    totalReceivable: Double,
    totalCollected: Double,
    activeSplitsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Receivables Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$activeSplitsCount Active",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "To Collect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalReceivable),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalReceivable > 0) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Collected So Far",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalCollected),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomeGreen
                    )
                }
            }
        }
    }
}

// ==========================================
// SPLIT ITEM CARD
// ==========================================

@Composable
private fun SplitItemCard(
    item: SplitGroupWithDetails,
    onClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    val split = item.splitGroup
    val isSettled = split.status == SplitStatus.SETTLED
    val progress = if (split.receivableAmount > 0) {
        (item.totalReceived / split.receivableAmount).coerceIn(0.0, 1.0).toFloat()
    } else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title, Date, Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSettled) IncomeGreen.copy(alpha = 0.15f)
                                else Color(0xFFF57C00).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = null,
                            tint = if (isSettled) IncomeGreen else Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = split.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = DateUtils.formatRelativeDate(split.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSettled) IncomeGreen.copy(alpha = 0.12f) else Color(0xFFF57C00).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isSettled) "Settled" else "Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) IncomeGreen else Color(0xFFF57C00),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Row: Total, You, Receivable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(split.totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = "Your Share",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(split.myShare),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Receivable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(split.receivableAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar & Received / Left labels
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Collected: ${CurrencyFormatter.format(item.totalReceived)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                    Text(
                        text = if (isSettled) "✓ Completed" else "Remaining: ${CurrencyFormatter.format(item.remainingAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSettled) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (progress >= 1f) IncomeGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Only show Receive button on the card if active
            if (!isSettled) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onReceiveClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Receive",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==========================================
// BATCH RECEIVE BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchReceiveBottomSheet(
    split: SplitGroupWithDetails,
    rows: List<BatchReceiveRowState>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAddRow: () -> Unit,
    onRemoveRow: (String) -> Unit,
    onAmountChange: (String, String) -> Unit,
    onAccountChange: (String, Long) -> Unit,
    onDateChange: (String, Long) -> Unit,
    onNoteChange: (String, String) -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val totalBatchAmount = rows.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    val remainingAfterBatch = maxOf(0.0, split.remainingAmount - totalBatchAmount)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Receive Split Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "For: ${split.splitGroup.description} • Remaining: ${CurrencyFormatter.format(split.remainingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Informational tip about batch collection
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add multiple payment entries if repayments were received via different accounts or at different times.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Rows
            rows.forEachIndexed { index, row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment #${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (rows.size > 1) {
                                IconButton(
                                    onClick = { onRemoveRow(row.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove payment",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Amount input
                        OutlinedTextField(
                            value = row.amountText,
                            onValueChange = { onAmountChange(row.id, it) },
                            label = { Text("Amount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Account selection dropdown
                        var isAccountDropdownExpanded by remember { mutableStateOf(false) }
                        val selectedAccount = accounts.firstOrNull { it.id == row.selectedAccountId }

                        ExposedDropdownMenuBox(
                            expanded = isAccountDropdownExpanded,
                            onExpandedChange = { isAccountDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedAccount?.let { "${it.name} (${it.type.displayName})" } ?: "Select Deposit Account",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Deposited To") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = isAccountDropdownExpanded,
                                onDismissRequest = { isAccountDropdownExpanded = false }
                            ) {
                                accounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${acc.type.displayName})") },
                                        onClick = {
                                            onAccountChange(row.id, acc.id)
                                            isAccountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date & Time Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedTextField(
                                    value = DateUtils.formatDate(row.dateEpochMillis),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Date") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = "Date",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = row.dateEpochMillis }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    val updated = Calendar.getInstance().apply {
                                                        timeInMillis = row.dateEpochMillis
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, day)
                                                    }
                                                    onDateChange(row.id, updated.timeInMillis)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                )
                            }

                            // Time
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = DateUtils.formatTime(row.dateEpochMillis),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Time") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Time",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = row.dateEpochMillis }
                                            TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    val updated = Calendar.getInstance().apply {
                                                        timeInMillis = row.dateEpochMillis
                                                        set(Calendar.HOUR_OF_DAY, hour)
                                                        set(Calendar.MINUTE, minute)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }
                                                    onDateChange(row.id, updated.timeInMillis)
                                                },
                                                cal.get(Calendar.HOUR_OF_DAY),
                                                cal.get(Calendar.MINUTE),
                                                false // 12-hour format (AM/PM)
                                            ).show()
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Optional Note / Reference
                        OutlinedTextField(
                            value = row.note,
                            onValueChange = { onNoteChange(row.id, it) },
                            label = { Text("Note (optional)") },
                            placeholder = { Text("e.g. Partial repayment via Cash") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Button to add another payment row
            OutlinedButton(
                onClick = onAddRow,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add another payment")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Batch summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Batch Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(totalBatchAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Remaining after batch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(remainingAfterBatch),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingAfterBatch <= 0) IncomeGreen else Color(0xFFF57C00)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Button
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = totalBatchAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm & Record ${CurrencyFormatter.format(totalBatchAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ==========================================
// SPLIT DETAILS & HISTORY DIALOG
// ==========================================

@Composable
private fun SplitDetailsDialog(
    split: SplitGroupWithDetails,
    onDismiss: () -> Unit,
    onTransactionClick: (TransactionWithDetails) -> Unit,
    onReceiveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isSettled = split.splitGroup.status == SplitStatus.SETTLED
    val progress = if (split.splitGroup.receivableAmount > 0) {
        (split.totalReceived / split.splitGroup.receivableAmount).coerceIn(0.0, 1.0).toFloat()
    } else 1f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = split.splitGroup.description,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSettled) IncomeGreen.copy(alpha = 0.12f) else Color(0xFFF57C00).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isSettled) "Settled" else "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSettled) IncomeGreen else Color(0xFFF57C00),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Split created ${DateUtils.formatRelativeDate(split.splitGroup.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown Row: Total, You, Receivable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(split.splitGroup.totalAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = "Your Share",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(split.splitGroup.myShare),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Receivable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.format(split.splitGroup.receivableAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57C00)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar & Received / Left labels
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Collected: ${CurrencyFormatter.format(split.totalReceived)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                        Text(
                            text = if (isSettled) "✓ Completed" else "Remaining: ${CurrencyFormatter.format(split.remainingAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSettled) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (progress >= 1f) IncomeGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Original Expense Card
                Text(
                    text = "Original Expense",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val exp = split.expenseTransaction
                if (exp != null) {
                    TransactionItemCard(
                        item = exp,
                        onClick = { onTransactionClick(exp) },
                        showSplitBadge = false
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Collections History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collection History (${split.settlementTransactions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total: ${CurrencyFormatter.format(split.totalReceived)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (split.settlementTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No payments collected yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    split.settlementTransactions.forEach { settleTxn ->
                        TransactionItemCard(
                            item = settleTxn,
                            onClick = { onTransactionClick(settleTxn) },
                            modifier = Modifier.padding(bottom = 8.dp),
                            showSplitBadge = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                if (!isSettled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onReceiveClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Receive", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Split")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// EMPTY STATE CARD
// ==========================================

@Composable
private fun EmptySplitsCard(
    filter: SplitFilter,
    onNewSplitClick: () -> Unit
) {
    PAMEmptyState(
        emoji = "🤝",
        title = when (filter) {
            SplitFilter.ALL -> "No Split Expenses Yet"
            SplitFilter.OPEN -> "No Active Splits"
            SplitFilter.SETTLED -> "No Settled Splits"
        },
        description = "When you pay for a group expense, split it to keep track of receivable amounts and batch settlements.",
        actionButtonText = "Split an Expense",
        onActionClick = onNewSplitClick
    )
}
