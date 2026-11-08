package com.example.pam.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pam.data.model.Account
import com.example.pam.data.model.Category
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.ui.theme.toComposeColor
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import com.example.pam.util.IconHelper
import com.example.pam.util.ai.ParsedReceipt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanResultBottomSheet(
    parsedReceipt: ParsedReceipt,
    accounts: List<Account>,
    categories: List<Category>,
    onSaveTransaction: (Transaction) -> Unit,
    onReviewInFullEditor: (amount: Double, merchant: String, categoryId: Long?, accountId: Long, dateMillis: Long, note: String) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val expenseCategories = remember(categories) {
        categories.filter { it.isApplicableFor(TransactionType.EXPENSE) }
    }

    var merchantName by remember {
        mutableStateOf(parsedReceipt.merchantName ?: "Store Receipt")
    }

    var amountText by remember {
        mutableStateOf(
            parsedReceipt.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else String.format(java.util.Locale.US, "%.2f", it)
            } ?: ""
        )
    }

    var selectedCategoryId by remember {
        mutableStateOf(
            parsedReceipt.suggestedCategory?.id
                ?: expenseCategories.firstOrNull { it.isDefault }?.id
                ?: expenseCategories.firstOrNull()?.id
        )
    }

    var selectedAccountId by remember {
        mutableStateOf(
            accounts.firstOrNull { it.isDefault }?.id
                ?: accounts.firstOrNull()?.id
                ?: 0L
        )
    }

    var showItemsExpanded by remember { mutableStateOf(parsedReceipt.items.isNotEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        IncomeGreen,
                                        Color(0xFF0D9488)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Receipt Scanned",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IncomeGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "On-Device AI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Extracted locally with zero internet access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Detected Amount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.width(180.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant Name & Date
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it },
                label = { Text("Merchant / Store") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            if (parsedReceipt.dateEpochMillis != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Receipt Date: ${DateUtils.formatDate(parsedReceipt.dateEpochMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector Chips
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (cat in expenseCategories) {
                    val isSelected = cat.id == selectedCategoryId
                    val catColor = cat.colorHex.toComposeColor()
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryId = cat.id },
                        label = {
                            Text(cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = IconHelper.getIcon(cat.iconName),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else catColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Selector Chips
            Text(
                text = "Paid From",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (acc in accounts) {
                    val isSelected = acc.id == selectedAccountId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAccountId = acc.id },
                        label = {
                            Text(acc.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Line Items Card (Collapsible)
            if (parsedReceipt.items.isNotEmpty() || (parsedReceipt.taxAmount != null && parsedReceipt.taxAmount > 0)) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showItemsExpanded = !showItemsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Line Items (${parsedReceipt.items.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (showItemsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showItemsExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                for (item in parsedReceipt.items) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.name + (item.quantity?.let { " (x$it)" } ?: ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (item.price != null) {
                                            Text(
                                                text = CurrencyFormatter.format(item.price),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (parsedReceipt.taxAmount != null && parsedReceipt.taxAmount > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Taxes & GST",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = CurrencyFormatter.format(parsedReceipt.taxAmount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Review & Edit or 1-Tap Save
            val parsedAmount = amountText.toDoubleOrNull()
            val canSave = parsedAmount != null && parsedAmount > 0.0 && selectedAccountId != 0L

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val amt = parsedAmount ?: 0.0
                        val dateMillis = parsedReceipt.dateEpochMillis ?: System.currentTimeMillis()
                        val note = parsedReceipt.buildSummaryNote()
                        onReviewInFullEditor(amt, merchantName, selectedCategoryId, selectedAccountId, dateMillis, note)
                        onClose()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text("Review & Edit", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val amt = parsedAmount ?: return@Button
                        val dateMillis = parsedReceipt.dateEpochMillis ?: System.currentTimeMillis()
                        val note = parsedReceipt.buildSummaryNote()
                        val txn = Transaction(
                            title = merchantName.ifBlank { "Receipt Spend" },
                            amount = amt,
                            type = TransactionType.EXPENSE,
                            accountId = selectedAccountId,
                            categoryId = selectedCategoryId,
                            dateEpochMillis = dateMillis,
                            note = note.ifBlank { null }
                        )
                        onSaveTransaction(txn)
                        onClose()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (parsedAmount != null && parsedAmount > 0) {
                            "Log • ${CurrencyFormatter.format(parsedAmount)}"
                        } else {
                            "Log Expense"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
