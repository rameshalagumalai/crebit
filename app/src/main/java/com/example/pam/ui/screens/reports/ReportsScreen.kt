package com.example.pam.ui.screens.reports

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.ui.components.AskCrebitBottomSheet
import com.example.pam.ui.components.AskCrebitGlowingButton
import com.example.pam.ui.components.PAMDateRangePickerDialog
import com.example.pam.ui.components.SmartNarrativeDigestCard
import com.example.pam.ui.components.TransactionDetailsDialog
import com.example.pam.ui.components.TransactionItemCard
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.ui.theme.toComposeColor
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import com.example.pam.util.IconHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateToTransactionDetail: (Long) -> Unit = {},
    viewModel: ReportsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    var showAskCrebitSheet by remember { mutableStateOf(false) }
    var askCrebitQuery by remember { mutableStateOf("") }

    if (showCustomDatePicker) {
        PAMDateRangePickerDialog(
            initialStartDateMillis = uiState.customStartDate,
            initialEndDateMillis = uiState.customEndDate,
            onConfirm = { start, end ->
                viewModel.onRangeSelected(DateRangeType.CUSTOM, start, end)
                showCustomDatePicker = false
            },
            onDismiss = {
                showCustomDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Period Filter Chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DateRangeType.values()) { rangeType ->
                        val isSelected = uiState.selectedRange == rangeType
                        val label = if (rangeType == DateRangeType.CUSTOM && isSelected &&
                            uiState.customStartDate != null && uiState.customEndDate != null) {
                            DateUtils.formatDateRange(uiState.customStartDate!!, uiState.customEndDate!!)
                        } else {
                            rangeType.displayName
                        }

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (rangeType == DateRangeType.CUSTOM) {
                                    showCustomDatePicker = true
                                } else {
                                    viewModel.onRangeSelected(rangeType)
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
            }

            // Summary Overview Card
            item {
                val overview = uiState.overview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Net Savings",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (overview.netSavings >= 0) IncomeGreen.copy(alpha = 0.15f)
                                                else ExpenseRed.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (overview.netSavings >= 0) "${overview.savingsRatePercentage.toInt()}% Saved" else "Deficit",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (overview.netSavings >= 0) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = CurrencyFormatter.format(overview.netSavings),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (overview.netSavings >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                                )
                            }

                            // Glowing Animated Ask Crebit AI Button (Same style as Dashboard)
                            AskCrebitGlowingButton(
                                onClick = { showAskCrebitSheet = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Income & Expense Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Income Box
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IncomeGreen.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Income",
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Total Income", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(overview.totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }

                            // Expense Box
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ExpenseRed.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Expense",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Total Expense", style = MaterialTheme.typography.labelSmall, color = ExpenseRed)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyFormatter.format(overview.totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Expense-to-Income Ratio Bar
                        if (overview.totalIncome > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val ratio = (overview.totalExpense / overview.totalIncome).toFloat().coerceIn(0f, 1f)
                            Text(
                                text = "Spend Ratio: ${(ratio * 100).toInt()}% of income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (ratio > 0.85f) ExpenseRed else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Smart Narrative Digest Card (Feature 6)
            if (uiState.smartDigest != null && uiState.smartDigest!!.insights.isNotEmpty()) {
                item {
                    SmartNarrativeDigestCard(
                        digest = uiState.smartDigest!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        onInsightClick = { insight ->
                            if (insight.filterQuery != null) {
                                askCrebitQuery = insight.filterQuery
                                showAskCrebitSheet = true
                            } else if (insight.relatedCategoryId != null) {
                                val catSummary = uiState.categoryBreakdown.find { it.category.id == insight.relatedCategoryId }
                                if (catSummary != null) {
                                    viewModel.onCategoryClicked(catSummary)
                                }
                            }
                        }
                    )
                }
            }

            // Category Breakdown Section
            item {
                Text(
                    text = "Spending by Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            if (uiState.categoryBreakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses recorded in this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.categoryBreakdown) { item ->
                    val catColor = item.category.colorHex.toComposeColor(MaterialTheme.colorScheme.primary)

                    Card(
                        onClick = { viewModel.onCategoryClicked(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(catColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconHelper.getIcon(item.category.iconName),
                                        contentDescription = item.category.name,
                                        tint = catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.category.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${item.count} transaction${if (item.count > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(item.totalAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f%%", item.percentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = catColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View transactions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = catColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // Account Spending Breakdown Section
            item {
                Text(
                    text = "Spending by Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            if (uiState.accountBreakdown.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No account transactions in this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.accountBreakdown) { item ->
                    val accColor = item.account.colorHex.toComposeColor(MaterialTheme.colorScheme.secondary)

                    Card(
                        onClick = { viewModel.onAccountClicked(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(accColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconHelper.getIcon(item.account.iconName),
                                        contentDescription = item.account.name,
                                        tint = accColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.account.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = item.account.type.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(item.totalAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f%%", item.percentage),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View transactions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { (item.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = accColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Transactions Breakdown Bottom Sheet
    if (uiState.selectedBreakdownTarget != null) {
        val target = uiState.selectedBreakdownTarget!!
        val targetName = when (target) {
            is SelectedBreakdownTarget.CategoryTarget -> target.category.name
            is SelectedBreakdownTarget.AccountTarget -> target.account.name
        }
        val targetIcon = when (target) {
            is SelectedBreakdownTarget.CategoryTarget -> target.category.iconName
            is SelectedBreakdownTarget.AccountTarget -> target.account.iconName
        }
        val targetColorHex = when (target) {
            is SelectedBreakdownTarget.CategoryTarget -> target.category.colorHex
            is SelectedBreakdownTarget.AccountTarget -> target.account.colorHex
        }
        val targetTotal = when (target) {
            is SelectedBreakdownTarget.CategoryTarget -> target.totalAmount
            is SelectedBreakdownTarget.AccountTarget -> target.totalAmount
        }
        val targetCount = when (target) {
            is SelectedBreakdownTarget.CategoryTarget -> target.count
            is SelectedBreakdownTarget.AccountTarget -> target.count
        }
        val targetColor = targetColorHex.toComposeColor(MaterialTheme.colorScheme.primary)

        ModalBottomSheet(
            onDismissRequest = { viewModel.onDismissBreakdown() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(targetColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = IconHelper.getIcon(targetIcon),
                            contentDescription = targetName,
                            tint = targetColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = targetName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val rangeLabel = if (uiState.selectedRange == DateRangeType.CUSTOM &&
                            uiState.customStartDate != null && uiState.customEndDate != null) {
                            DateUtils.formatDateRange(uiState.customStartDate!!, uiState.customEndDate!!)
                        } else {
                            uiState.selectedRange.displayName
                        }
                        Text(
                            text = "$rangeLabel • ${CurrencyFormatter.format(targetTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { viewModel.onDismissBreakdown() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                )

                if (uiState.isBreakdownLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (uiState.breakdownTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.breakdownTransactions, key = { it.transaction.id }) { item ->
                            TransactionItemCard(
                                item = item,
                                onClick = { selectedTransactionForDetails = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Transaction Details Dialog
    selectedTransactionForDetails?.let { txnDetails ->
        TransactionDetailsDialog(
            item = txnDetails,
            onDismiss = { selectedTransactionForDetails = null },
            onEdit = { id ->
                selectedTransactionForDetails = null
                viewModel.onDismissBreakdown()
                onNavigateToTransactionDetail(id)
            },
            onDelete = { id ->
                selectedTransactionForDetails = null
                viewModel.deleteTransaction(id)
            }
        )
    }

    if (showAskCrebitSheet) {
        AskCrebitBottomSheet(
            allTransactions = uiState.allTransactions,
            accounts = uiState.accounts,
            categories = uiState.categories,
            splits = uiState.allSplits,
            initialQuery = askCrebitQuery,
            onDismiss = {
                showAskCrebitSheet = false
                askCrebitQuery = ""
            },
            onEditTransaction = { id ->
                showAskCrebitSheet = false
                askCrebitQuery = ""
                onNavigateToTransactionDetail(id)
            },
            onDeleteTransaction = { id ->
                viewModel.deleteTransaction(id)
            }
        )
    }
}
