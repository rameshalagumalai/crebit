package com.example.pam.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pam.R
import com.example.pam.data.model.DateRangeType
import com.example.pam.data.model.FinancialOverview
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.ui.components.AskCrebitBottomSheet
import com.example.pam.ui.components.AskCrebitGlowingButton
import com.example.pam.ui.components.ConfirmDeleteDialog
import com.example.pam.ui.components.DateDividerChip
import com.example.pam.ui.components.PAMEmptyState
import com.example.pam.ui.components.SmartNarrativeDigestCard
import com.example.pam.ui.components.SmartQuickAddBottomSheet
import com.example.pam.util.ai.AnomalySeverity
import com.example.pam.util.ai.AnomalyType
import com.example.pam.util.ai.SpendingAnomaly
import com.example.pam.ui.components.TransactionDetailsDialog
import com.example.pam.ui.components.TransactionItemCard
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.ui.theme.ThemeManager
import com.example.pam.ui.theme.ThemeMode
import com.example.pam.ui.theme.TransferBlue
import com.example.pam.ui.theme.WarningAmberDark
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToScanUpi: () -> Unit,
    onNavigateToAddTransaction: (type: TransactionType?) -> Unit,
    onNavigateToTransactionDetail: (id: Long) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSplits: () -> Unit = {},
    onNavigateToScanReceipt: () -> Unit = onNavigateToScanUpi,
    onNavigateToAddTransactionWithPrefill: (amount: Double?, title: String?, categoryId: Long?, accountId: Long?, initialType: TransactionType?, note: String?, dateMillis: Long?, isSplit: Boolean, splitCount: Int?) -> Unit = { _, _, _, _, type, _, _, isSplit, _ -> onNavigateToAddTransaction(type) },
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    var showThemeMenu by remember { mutableStateOf(false) }
    var showSmartQuickAdd by remember { mutableStateOf(false) }
    var smartQuickAddStartVoice by remember { mutableStateOf(false) }
    var showAskCrebitSheet by remember { mutableStateOf(false) }
    var askCrebitStartVoice by remember { mutableStateOf(false) }
    var askCrebitQuery by remember { mutableStateOf("") }
    var selectedTransactionForDetails by remember { mutableStateOf<TransactionWithDetails?>(null) }
    var selectedTransactionIds by remember { mutableStateOf(emptySet<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    val isSelectionMode = selectedTransactionIds.isNotEmpty()
    val context = LocalContext.current

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = remember(currentHour) {
        when (currentHour) {
            in 5..11 -> "Morning, superstar! ☕"
            in 12..16 -> "Hey there! ⚡"
            in 17..21 -> "Evening check-in! 🌆"
            else -> "Late night vibes 🌙"
        }
    }

    val groupedRecentTransactions = remember(uiState.recentTransactions) {
        uiState.recentTransactions.groupBy { DateUtils.formatRelativeDate(it.transaction.dateEpochMillis) }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedTransactionIds = emptySet()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_crebit_logo),
                            contentDescription = "Crebit Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        Surface(
                            modifier = Modifier
                                .clickable { showThemeMenu = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (currentThemeMode) {
                                        ThemeMode.LIGHT -> Icons.Default.LightMode
                                        ThemeMode.DARK -> Icons.Default.DarkMode
                                        ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = "Color Mode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (currentThemeMode) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "System"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Light")
                                        if (currentThemeMode == ThemeMode.LIGHT) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = "Light mode",
                                        tint = if (currentThemeMode == ThemeMode.LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    ThemeManager.setThemeMode(ThemeMode.LIGHT, context)
                                    showThemeMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Dark")
                                        if (currentThemeMode == ThemeMode.DARK) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = "Dark mode",
                                        tint = if (currentThemeMode == ThemeMode.DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    ThemeManager.setThemeMode(ThemeMode.DARK, context)
                                    showThemeMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Device default")
                                        if (currentThemeMode == ThemeMode.SYSTEM) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.BrightnessAuto,
                                        contentDescription = "Device default",
                                        tint = if (currentThemeMode == ThemeMode.SYSTEM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    ThemeManager.setThemeMode(ThemeMode.SYSTEM, context)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Financial Overview Dashboard Card (Month/Week toggle)
            item {
                DashboardOverviewCard(
                    overview = uiState.periodOverview,
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { viewModel.onPeriodSelected(it) },
                    onAskCrebitClick = {
                        askCrebitStartVoice = false
                        showAskCrebitSheet = true
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Smart Natural Language Quick Log Trigger Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            smartQuickAddStartVoice = false
                            showSmartQuickAdd = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFF8B5CF6)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

//                        Icon(
//                            imageVector = Icons.Default.AutoAwesome,
//                            contentDescription = null,
//                            tint = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier.size(20.dp)
//                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Log with AI: \"Coffee 180 cash\"...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedIconButton(
                                onClick = onNavigateToScanReceipt,
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = "Scan Receipt",
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            OutlinedIconButton(
                                onClick = {
                                    smartQuickAddStartVoice = true
                                    showSmartQuickAdd = true
                                },
                                modifier = Modifier.size(34.dp),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions 2x2 Neo-Fintech Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            title = "Scan",
                            icon = Icons.Default.QrCodeScanner,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onClick = onNavigateToScanUpi
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            title = "Spend",
                            icon = Icons.Default.ArrowUpward,
                            accentColor = ExpenseRed,
                            onClick = { onNavigateToAddTransaction(TransactionType.EXPENSE) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            title = "Receive",
                            icon = Icons.Default.ArrowDownward,
                            accentColor = IncomeGreen,
                            onClick = { onNavigateToAddTransaction(TransactionType.INCOME) }
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            title = "Move",
                            icon = Icons.Default.SyncAlt,
                            accentColor = TransferBlue,
                            onClick = { onNavigateToAddTransaction(TransactionType.TRANSFER) }
                        )
                    }
                }
            }

            // Active Splits Summary Banner
            if (uiState.activeSplitsCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onNavigateToSplits() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                        contentDescription = "Splits",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${uiState.activeSplitsCount} Active Split${if (uiState.activeSplitsCount > 1) "s" else ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "To receive: ${CurrencyFormatter.format(uiState.totalReceivable)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go to splits",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // AI Spending Insights & Anomalies Card
            if (uiState.activeAnomalies.isNotEmpty()) {
                item {
                    AISpendingInsightsSection(
                        anomalies = uiState.activeAnomalies,
                        onDismissAnomaly = { viewModel.dismissAnomaly(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
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
                            }
                        }
                    )
                }
            }

            // "The Receipts" Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 22.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text(
                            text = "See All ➔",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Recent Transactions List / Empty State
            if (uiState.recentTransactions.isEmpty()) {
                item {
                    PAMEmptyState(
                        emoji = "🏖️",
                        title = "Your wallet is chilling!",
                        description = "No spends logged yet. Either you're an elite saver or you forgot to track that iced latte! 😉",
                        actionButtonText = "Log a Spend 💸",
                        onActionClick = { onNavigateToAddTransaction(TransactionType.EXPENSE) }
                    )
                }
            } else {
                groupedRecentTransactions.forEach { (dateLabel, txnsForDate) ->
                    item(key = "dashboard_date_$dateLabel") {
                        DateDividerChip(
                            dateLabel = dateLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(txnsForDate, key = { it.transaction.id }) { txnItem ->
                        val txnId = txnItem.transaction.id
                        val isSelected = selectedTransactionIds.contains(txnId)
                        TransactionItemCard(
                            item = txnItem,
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
                                    selectedTransactionForDetails = txnItem
                                }
                            },
                            onLongClick = {
                                selectedTransactionIds = if (isSelected) {
                                    selectedTransactionIds - txnId
                                } else {
                                    selectedTransactionIds + txnId
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                        )
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

    if (showSmartQuickAdd) {
        SmartQuickAddBottomSheet(
            accounts = uiState.accounts,
            categories = uiState.categories,
            recentTransactions = uiState.recentTransactions,
            startWithVoice = smartQuickAddStartVoice,
            onSaveTransaction = { txn, isSplit, splitCount ->
                viewModel.saveQuickTransaction(txn, isSplit, splitCount)
            },
            onOpenFullEditor = { draft ->
                showSmartQuickAdd = false
                smartQuickAddStartVoice = false
                onNavigateToAddTransactionWithPrefill(
                    draft.amount,
                    draft.title,
                    draft.matchedCategory?.id,
                    draft.matchedAccount?.id,
                    draft.type,
                    draft.rawInput,
                    draft.dateEpochMillis,
                    draft.isSplit,
                    draft.splitPeopleCount
                )
            },
            onScanReceipt = {
                showSmartQuickAdd = false
                smartQuickAddStartVoice = false
                onNavigateToScanReceipt()
            },
            onClose = {
                showSmartQuickAdd = false
                smartQuickAddStartVoice = false
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
            startWithVoice = askCrebitStartVoice,
            onDismiss = {
                showAskCrebitSheet = false
                askCrebitStartVoice = false
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

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DashboardOverviewCard(
    overview: FinancialOverview,
    selectedPeriod: DateRangeType,
    onPeriodSelected: (DateRangeType) -> Unit,
    onAskCrebitClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Section label & Month/Week segmented toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Period Switcher (This Month / This Week)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PeriodToggleChip(
                        text = "Month",
                        isSelected = selectedPeriod == DateRangeType.THIS_MONTH,
                        onClick = { onPeriodSelected(DateRangeType.THIS_MONTH) }
                    )
                    PeriodToggleChip(
                        text = "Week",
                        isSelected = selectedPeriod == DateRangeType.THIS_WEEK,
                        onClick = { onPeriodSelected(DateRangeType.THIS_WEEK) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Net Amount Display with Infinite Animated Glowing Circular Ask Crebit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Net Amount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = CurrencyFormatter.format(overview.netSavings),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (overview.totalIncome > 0.0 || overview.totalExpense > 0.0) {
                            val isPositive = overview.netSavings >= 0
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPositive) IncomeGreen.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isPositive) "Positive" else "Deficit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Glowing Animated Ask Crebit AI Button
                AskCrebitGlowingButton(
                    onClick = onAskCrebitClick,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Incomes and Expenses Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Income Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IncomeGreen.copy(alpha = 0.08f))
                        .border(
                            BorderStroke(1.dp, IncomeGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(IncomeGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income",
                                tint = IncomeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Income",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Expense",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = CurrencyFormatter.format(overview.totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodToggleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AISpendingInsightsSection(
    anomalies: List<SpendingAnomaly>,
    onDismissAnomaly: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Insights",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "AI Spending Insights",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val hasCritical = anomalies.any { it.severity == AnomalySeverity.ALERT }
                val badgeColor = if (hasCritical) {
                    if (isDark) Color(0xFFF87171) else ExpenseRed
                } else {
                    if (isDark) WarningAmberDark else Color(0xFFD97706)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${anomalies.size} ${if (anomalies.size == 1) "Insight" else "Insights"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Anomaly item list
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                anomalies.forEach { anomaly ->
                    val accentColor = when (anomaly.severity) {
                        AnomalySeverity.ALERT -> if (isDark) Color(0xFFF87171) else ExpenseRed
                        AnomalySeverity.WARNING -> if (isDark) WarningAmberDark else Color(0xFFD97706)
                        AnomalySeverity.INFO -> if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
                    }

                    val icon = when (anomaly.type) {
                        AnomalyType.CATEGORY_SPIKE -> Icons.AutoMirrored.Filled.TrendingUp
                        AnomalyType.UNUSUALLY_LARGE_EXPENSE -> Icons.Default.WarningAmber
                        AnomalyType.DAILY_SURGE -> Icons.Default.Bolt
                        AnomalyType.DUPLICATE_TRANSACTION -> Icons.Default.WarningAmber
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.32f else 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = anomaly.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (anomaly.ratio != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = accentColor.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "${String.format(java.util.Locale.US, "%.1f", anomaly.ratio)}×",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = anomaly.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            IconButton(
                                onClick = { onDismissAnomaly(anomaly.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

