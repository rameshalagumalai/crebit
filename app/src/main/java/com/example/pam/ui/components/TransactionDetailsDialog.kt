package com.example.pam.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.pam.data.model.Account
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.SplitSettlementEntry
import com.example.pam.data.model.SplitStatus
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.data.repository.PAMRepository
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.ui.theme.toComposeColor
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import com.example.pam.util.IconHelper
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsDialog(
    item: TransactionWithDetails,
    onDismiss: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PAMRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var splitWithDetails by remember { mutableStateOf<SplitGroupWithDetails?>(null) }
    var showAddSplitDialog by remember { mutableStateOf(false) }
    var showReceiveDialog by remember { mutableStateOf(false) }
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }

    val txn = item.transaction

    LaunchedEffect(txn.id, txn.splitGroupId) {
        accounts = repository.getAllAccounts()
        if (txn.splitGroupId != null) {
            splitWithDetails = repository.getSplitGroupWithDetailsById(txn.splitGroupId)
        } else if (txn.type == TransactionType.EXPENSE) {
            val sg = repository.getSplitGroupByExpenseId(txn.id)
            if (sg != null) {
                splitWithDetails = repository.getSplitGroupWithDetailsById(sg.id)
            }
        }
    }
    val categoryColor = item.category?.colorHex.toComposeColor(MaterialTheme.colorScheme.primary)
    val accountColor = item.account?.colorHex.toComposeColor(MaterialTheme.colorScheme.secondary)

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Transaction details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Spacer for symmetric header
                    Spacer(modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Icon (Google Pay style receipt avatar)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.category != null) categoryColor.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when {
                        item.category != null -> IconHelper.getIcon(item.category.iconName)
                        txn.type == TransactionType.TRANSFER -> IconHelper.getIcon("SwapHoriz")
                        txn.type == TransactionType.INCOME -> IconHelper.getIcon("Payments")
                        else -> IconHelper.getIcon("ShoppingBag")
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = txn.title,
                        tint = if (item.category != null) categoryColor else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title / Payee Name
                Text(
                    text = txn.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Payee UPI ID
                if (!txn.payeeUpiId.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = txn.payeeUpiId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Amount Display (Centered, Large, zero signs, green for income, normal text for expense)
                Text(
                    text = CurrencyFormatter.format(txn.amount),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (txn.type == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = DateUtils.formatDateTime(txn.dateEpochMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Breakdown Card (Google Pay Receipt Table)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Payment method / Account
                        ReceiptDetailRow(
                            label = if (txn.type == TransactionType.TRANSFER) "From Account" else "Payment method",
                            title = item.account?.name ?: "Account",
                            subtitle = item.account?.type?.displayName,
                            iconName = item.account?.iconName ?: "AccountBalance",
                            colorHex = item.account?.colorHex ?: "#27272A"
                        )

                        // If Transfer: Destination Account
                        if (txn.type == TransactionType.TRANSFER && item.toAccount != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                            ReceiptDetailRow(
                                label = "To Account",
                                title = item.toAccount.name,
                                subtitle = item.toAccount.type.displayName,
                                iconName = item.toAccount.iconName,
                                colorHex = item.toAccount.colorHex
                            )
                        } else if (item.category != null) {
                            // Category Row
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                            ReceiptDetailRow(
                                label = "Category",
                                title = item.category.name,
                                subtitle = item.category.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                iconName = item.category.iconName,
                                colorHex = item.category.colorHex
                            )
                        }

                        // UPI Reference (if present)
                        if (!txn.payeeUpiId.isNullOrBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Payee UPI ID",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = txn.payeeUpiId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Note / Memo (if present)
                        if (!txn.note.isNullOrBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = IconHelper.getIcon("Notes"),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Note",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = txn.note,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Split Information or Split Prompt Card
                if (splitWithDetails != null) {
                    val split = splitWithDetails!!
                    val isSettled = split.splitGroup.status == SplitStatus.SETTLED
                    val progress = if (split.splitGroup.receivableAmount > 0) {
                        (split.totalReceived / split.splitGroup.receivableAmount).coerceIn(0.0, 1.0).toFloat()
                    } else 1f

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (txn.type == TransactionType.EXPENSE) "Split Expense" else "Split Settlement",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSettled) IncomeGreen.copy(alpha = 0.15f) else Color(0xFFF57C00).copy(alpha = 0.15f)
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

                            if (txn.type == TransactionType.EXPENSE) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Your Share: ${CurrencyFormatter.format(split.splitGroup.myShare)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Receivable: ${CurrencyFormatter.format(split.splitGroup.receivableAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF57C00)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = if (progress >= 1f) IncomeGreen else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Collected: ${CurrencyFormatter.format(split.totalReceived)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IncomeGreen
                                    )
                                    Text(
                                        text = if (isSettled) "Completed ✓" else "Left: ${CurrencyFormatter.format(split.remainingAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!isSettled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showReceiveDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Receive Payment", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "This income is a settlement collection for split: ${split.splitGroup.description}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (txn.type == TransactionType.EXPENSE) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showAddSplitDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Split this expense with others",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons: Delete and Edit Options (as requested)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onEdit(txn.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

    // Confirm Delete Dialog
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete Transaction?",
            message = "Are you sure you want to delete this transaction? Account balance will be restored accordingly.",
            onConfirm = {
                showDeleteConfirm = false
                onDismiss()
                onDelete(txn.id)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Quick Add Split Dialog
    if (showAddSplitDialog) {
        var mode by remember { mutableStateOf(0) } // 0 = Equal, 1 = Custom
        var peopleCount by remember { mutableStateOf(2) }
        var customShareText by remember { mutableStateOf("") }

        val myShare = if (mode == 0) {
            txn.amount / peopleCount.coerceAtLeast(1)
        } else {
            customShareText.toDoubleOrNull() ?: (txn.amount / 2)
        }
        val receivable = maxOf(0.0, txn.amount - myShare)

        Dialog(onDismissRequest = { showAddSplitDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Split this Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total amount: ${CurrencyFormatter.format(txn.amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = mode == 0,
                            onClick = { mode = 0 },
                            label = { Text("Split Equally") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        FilterChip(
                            selected = mode == 1,
                            onClick = { mode = 1 },
                            label = { Text("Custom Share") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (mode == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Split ways (inc. you)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (peopleCount > 2) peopleCount-- },
                                    enabled = peopleCount > 2,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    text = "$peopleCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                FilledTonalIconButton(
                                    onClick = { peopleCount++ },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customShareText,
                            onValueChange = { customShareText = it },
                            label = { Text("Your Share (₹)") },
                            placeholder = { Text("Enter your portion") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Your Share",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(myShare),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Receivable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(receivable),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57C00)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddSplitDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val id = repository.splitExistingExpense(
                                        transactionId = txn.id,
                                        myShare = myShare,
                                        receivableAmount = receivable
                                    )
                                    splitWithDetails = repository.getSplitGroupWithDetailsById(id)
                                    showAddSplitDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Create Split")
                        }
                    }
                }
            }
        }
    }

    // Quick Receive Dialog
    if (showReceiveDialog && splitWithDetails != null) {
        val split = splitWithDetails!!
        val defaultAcc = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        var receiveAmountText by remember {
            mutableStateOf(
                if (split.remainingAmount % 1.0 == 0.0) split.remainingAmount.toLong().toString()
                else "%.2f".format(split.remainingAmount)
            )
        }
        var selectedAccountId by remember { mutableStateOf(defaultAcc?.id) }
        var receiveNoteText by remember { mutableStateOf("") }
        var isAccountMenuExpanded by remember { mutableStateOf(false) }
        var receiveDateEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }

        Dialog(onDismissRequest = { showReceiveDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Receive Split Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Remaining: ${CurrencyFormatter.format(split.remainingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = receiveAmountText,
                        onValueChange = { receiveAmountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val selectedAcc = accounts.firstOrNull { it.id == selectedAccountId }
                    ExposedDropdownMenuBox(
                        expanded = isAccountMenuExpanded,
                        onExpandedChange = { isAccountMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedAcc?.let { "${it.name} (${it.type.displayName})" } ?: "Select Deposit Account",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Deposit Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccountMenuExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = isAccountMenuExpanded,
                            onDismissRequest = { isAccountMenuExpanded = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.type.displayName})") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        isAccountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date & Time Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date
                        Box(modifier = Modifier.weight(1.2f)) {
                            OutlinedTextField(
                                value = DateUtils.formatDate(receiveDateEpochMillis),
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
                                        val cal = Calendar.getInstance().apply { timeInMillis = receiveDateEpochMillis }
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = receiveDateEpochMillis
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                }
                                                receiveDateEpochMillis = updated.timeInMillis
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
                                value = DateUtils.formatTime(receiveDateEpochMillis),
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
                                        val cal = Calendar.getInstance().apply { timeInMillis = receiveDateEpochMillis }
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                val updated = Calendar.getInstance().apply {
                                                    timeInMillis = receiveDateEpochMillis
                                                    set(Calendar.HOUR_OF_DAY, hour)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                receiveDateEpochMillis = updated.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                            false // 12-hour format (AM/PM)
                                        ).show()
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = receiveNoteText,
                        onValueChange = { receiveNoteText = it },
                        label = { Text("Note (optional)") },
                        placeholder = { Text("e.g. Cash settlement") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showReceiveDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val amt = receiveAmountText.toDoubleOrNull()
                                val accId = selectedAccountId
                                if (amt != null && amt > 0.0 && accId != null) {
                                    scope.launch {
                                        repository.recordSplitSettlementBatch(
                                            split.splitGroup.id,
                                            listOf(
                                                SplitSettlementEntry(
                                                    amount = amt,
                                                    accountId = accId,
                                                    note = receiveNoteText.trim().ifBlank { null },
                                                    dateEpochMillis = receiveDateEpochMillis
                                                )
                                            )
                                        )
                                        splitWithDetails = repository.getSplitGroupWithDetailsById(split.splitGroup.id)
                                        showReceiveDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(
    label: String,
    title: String,
    subtitle: String?,
    iconName: String,
    colorHex: String
) {
    val iconColor = colorHex.toComposeColor(MaterialTheme.colorScheme.primary)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = IconHelper.getIcon(iconName),
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
