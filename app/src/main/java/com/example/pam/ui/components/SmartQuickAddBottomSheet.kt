package com.example.pam.ui.components

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pam.data.model.Account
import com.example.pam.data.model.Category
import com.example.pam.data.model.Transaction
import com.example.pam.data.model.TransactionType
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.ui.theme.toComposeColor
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import com.example.pam.util.IconHelper
import com.example.pam.util.ai.NaturalLanguageTransactionParser
import com.example.pam.util.ai.ParsedTransactionDraft
import com.example.pam.util.ai.SpendingAnomalyDetector
import java.util.Calendar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartQuickAddBottomSheet(
    accounts: List<Account>,
    categories: List<Category>,
    recentTransactions: List<TransactionWithDetails> = emptyList(),
    startWithVoice: Boolean = false,
    onSaveTransaction: (Transaction, isSplit: Boolean, splitPeopleCount: Int) -> Unit,
    onOpenFullEditor: (ParsedTransactionDraft) -> Unit,
    onScanReceipt: () -> Unit = {},
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    val parsedDraft by remember(inputText, accounts, categories) {
        derivedStateOf {
            NaturalLanguageTransactionParser.parse(inputText, accounts, categories)
        }
    }

    var customTitle by remember { mutableStateOf(parsedDraft.title) }
    var userSelectedType by remember { mutableStateOf(parsedDraft.type) }
    var userSelectedCategory by remember { mutableStateOf(parsedDraft.matchedCategory) }
    var userSelectedAccount by remember { mutableStateOf(parsedDraft.matchedAccount) }
    var userSelectedToAccount by remember { mutableStateOf(parsedDraft.matchedToAccount) }
    var userSelectedDateMillis by remember { mutableStateOf(parsedDraft.dateEpochMillis) }
    var userIsSplit by remember { mutableStateOf(parsedDraft.isSplit) }
    var userSplitPeopleCount by remember { mutableStateOf(parsedDraft.splitPeopleCount) }

    var lastRawInput by remember { mutableStateOf(parsedDraft.rawInput) }
    var isSaving by remember { mutableStateOf(false) }

    val duplicateAlert by remember(
        isSaving,
        parsedDraft.amount,
        customTitle,
        userSelectedCategory,
        userSelectedAccount,
        userSelectedDateMillis,
        userSelectedType,
        recentTransactions
    ) {
        derivedStateOf {
            if (!isSaving && userSelectedType == TransactionType.EXPENSE && parsedDraft.amount != null && parsedDraft.amount!! > 0) {
                val titleToCheck = customTitle.ifBlank { parsedDraft.title }
                SpendingAnomalyDetector.checkForDuplicate(
                    candidateAmount = parsedDraft.amount,
                    candidateTitle = titleToCheck,
                    candidateType = userSelectedType,
                    candidateCategoryName = userSelectedCategory?.name,
                    candidateAccountId = userSelectedAccount?.id,
                    candidateDateEpochMillis = userSelectedDateMillis,
                    recentTransactions = recentTransactions
                )
            } else null
        }
    }

    // Synchronize parsed entity state when input text or speech recognition changes
    LaunchedEffect(parsedDraft.rawInput) {
        if (parsedDraft.rawInput != lastRawInput) {
            lastRawInput = parsedDraft.rawInput
            customTitle = parsedDraft.title
            userSelectedType = parsedDraft.type
            userSelectedCategory = parsedDraft.matchedCategory
            userSelectedAccount = parsedDraft.matchedAccount
            userSelectedToAccount = parsedDraft.matchedToAccount
            userSelectedDateMillis = parsedDraft.dateEpochMillis
            userIsSplit = parsedDraft.isSplit
            userSplitPeopleCount = parsedDraft.splitPeopleCount
        }
    }

    // Date & Time Picker Dialogs
    val datePickerDialog = remember(context, userSelectedDateMillis) {
        val c = Calendar.getInstance().apply { timeInMillis = userSelectedDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = userSelectedDateMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                userSelectedDateMillis = updated.timeInMillis
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember(context, userSelectedDateMillis) {
        val c = Calendar.getInstance().apply { timeInMillis = userSelectedDateMillis }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = userSelectedDateMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                userSelectedDateMillis = updated.timeInMillis
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            false // 12-hour format (AM/PM)
        )
    }

    // Speech-to-Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    val launchSpeechToText: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your expense (e.g. 'Coffee 180 cash')...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        if (startWithVoice) {
            delay(350)
            launchSpeechToText()
        } else {
            delay(250)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

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
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                            contentDescription = "AI",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Smart Quick Log",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% on-device AI • Tap any chip to adjust",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Natural Language Input Box
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "e.g. \"Coffee 180 cash\" or \"Dinner 1200 split 3 ways\"",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(onClick = onScanReceipt) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Scan Receipt",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(onClick = { launchSpeechToText() }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Parsed Entity Preview Card
            AnimatedVisibility(
                visible = inputText.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (parsedDraft.amount != null) {
                                Text(
                                    text = CurrencyFormatter.format(parsedDraft.amount!!),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (userSelectedType) {
                                        TransactionType.INCOME -> IncomeGreen
                                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
                                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Title / Description") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Selectable Entity Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Type Selector Chip
                            Box {
                                var showTypeMenu by remember { mutableStateOf(false) }
                                val typeTint = when (userSelectedType) {
                                    TransactionType.INCOME -> IncomeGreen
                                    TransactionType.EXPENSE -> ExpenseRed
                                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                                }

                                SelectableParsedChip(
                                    label = userSelectedType.displayName,
                                    icon = when (userSelectedType) {
                                        TransactionType.TRANSFER -> IconHelper.getIcon("SwapHoriz")
                                        else -> Icons.Default.Payments
                                    },
                                    tint = typeTint,
                                    onClick = { showTypeMenu = true }
                                )

                                DropdownMenu(
                                    expanded = showTypeMenu,
                                    onDismissRequest = { showTypeMenu = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    TransactionType.entries.forEach { type ->
                                        val isSelected = type == userSelectedType
                                        val itemTint = when (type) {
                                            TransactionType.INCOME -> IncomeGreen
                                            TransactionType.EXPENSE -> ExpenseRed
                                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = type.displayName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) itemTint else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (type) {
                                                        TransactionType.TRANSFER -> IconHelper.getIcon("SwapHoriz")
                                                        else -> Icons.Default.Payments
                                                    },
                                                    contentDescription = null,
                                                    tint = itemTint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            trailingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = itemTint,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                userSelectedType = type
                                                if (type == TransactionType.TRANSFER) {
                                                    userSelectedCategory = null
                                                    if (userSelectedToAccount == null) {
                                                        userSelectedToAccount = accounts.firstOrNull { it.id != userSelectedAccount?.id }
                                                    }
                                                } else {
                                                    if (userSelectedCategory == null || !userSelectedCategory!!.isApplicableFor(type)) {
                                                        userSelectedCategory = categories.firstOrNull { it.isApplicableFor(type) }
                                                    }
                                                }
                                                showTypeMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Category Selector Chip (Only for Expense & Income)
                            if (userSelectedType != TransactionType.TRANSFER) {
                                Box {
                                    var showCategoryMenu by remember { mutableStateOf(false) }
                                    val applicableCategories = categories.filter { it.isApplicableFor(userSelectedType) }
                                    val currentCat = userSelectedCategory
                                    val catTint = currentCat?.colorHex?.toComposeColor(MaterialTheme.colorScheme.primary)
                                        ?: MaterialTheme.colorScheme.primary
                                    val catIcon = currentCat?.let { IconHelper.getIcon(it.iconName) }
                                        ?: IconHelper.getIcon("Category")

                                    SelectableParsedChip(
                                        label = currentCat?.name ?: "Select Category",
                                        icon = catIcon,
                                        tint = catTint,
                                        onClick = { showCategoryMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showCategoryMenu,
                                        onDismissRequest = { showCategoryMenu = false },
                                        modifier = Modifier
                                            .heightIn(max = 300.dp)
                                            .widthIn(min = 200.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        applicableCategories.forEach { cat ->
                                            val isSelected = cat.id == currentCat?.id
                                            val itemColor = cat.colorHex.toComposeColor(MaterialTheme.colorScheme.primary)
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = cat.name,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = IconHelper.getIcon(cat.iconName),
                                                        contentDescription = null,
                                                        tint = itemColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    userSelectedCategory = cat
                                                    showCategoryMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Account Selector Chip(s)
                            if (userSelectedType == TransactionType.TRANSFER) {
                                // From Account
                                Box {
                                    var showFromAccountMenu by remember { mutableStateOf(false) }
                                    val currentAcc = userSelectedAccount

                                    SelectableParsedChip(
                                        label = currentAcc?.let { "From: ${it.name}" } ?: "Select From Account",
                                        icon = Icons.Default.AccountBalance,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        onClick = { showFromAccountMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showFromAccountMenu,
                                        onDismissRequest = { showFromAccountMenu = false },
                                        modifier = Modifier
                                            .heightIn(max = 280.dp)
                                            .widthIn(min = 220.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        accounts.filter { it.id != userSelectedToAccount?.id }.forEach { acc ->
                                            val isSelected = acc.id == currentAcc?.id
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = acc.name,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "${acc.type.displayName} • ${CurrencyFormatter.format(acc.currentBalance)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountBalance,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    userSelectedAccount = acc
                                                    showFromAccountMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // To Destination Account
                                Box {
                                    var showToAccountMenu by remember { mutableStateOf(false) }
                                    val currentToAcc = userSelectedToAccount

                                    SelectableParsedChip(
                                        label = currentToAcc?.let { "To: ${it.name}" } ?: "Select Destination",
                                        icon = Icons.Default.AccountBalance,
                                        tint = MaterialTheme.colorScheme.primary,
                                        onClick = { showToAccountMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showToAccountMenu,
                                        onDismissRequest = { showToAccountMenu = false },
                                        modifier = Modifier
                                            .heightIn(max = 280.dp)
                                            .widthIn(min = 220.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        accounts.filter { it.id != userSelectedAccount?.id }.forEach { acc ->
                                            val isSelected = acc.id == currentToAcc?.id
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = acc.name,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "${acc.type.displayName} • ${CurrencyFormatter.format(acc.currentBalance)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountBalance,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    userSelectedToAccount = acc
                                                    showToAccountMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Standard Single Account Selector Chip
                                Box {
                                    var showAccountMenu by remember { mutableStateOf(false) }
                                    val currentAcc = userSelectedAccount

                                    SelectableParsedChip(
                                        label = currentAcc?.name ?: "Select Account",
                                        icon = Icons.Default.AccountBalance,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        onClick = { showAccountMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showAccountMenu,
                                        onDismissRequest = { showAccountMenu = false },
                                        modifier = Modifier
                                            .heightIn(max = 280.dp)
                                            .widthIn(min = 220.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        accounts.forEach { acc ->
                                            val isSelected = acc.id == currentAcc?.id
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            text = acc.name,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "${acc.type.displayName} • ${CurrencyFormatter.format(acc.currentBalance)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountBalance,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    userSelectedAccount = acc
                                                    showAccountMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Date & Time Selector Chip
                            Box {
                                var showDateMenu by remember { mutableStateOf(false) }

                                val formattedDateText = remember(userSelectedDateMillis) {
                                    val rel = DateUtils.formatRelativeDate(userSelectedDateMillis)
                                    val time = DateUtils.formatTime(userSelectedDateMillis)
                                    "$rel, $time"
                                }

                                SelectableParsedChip(
                                    label = formattedDateText,
                                    icon = Icons.Default.CalendarToday,
                                    tint = if (parsedDraft.hasExplicitDateTime) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { showDateMenu = true }
                                )

                                DropdownMenu(
                                    expanded = showDateMenu,
                                    onDismissRequest = { showDateMenu = false },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Today (Now)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            userSelectedDateMillis = System.currentTimeMillis()
                                            showDateMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Yesterday") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            val yCal = Calendar.getInstance().apply {
                                                timeInMillis = userSelectedDateMillis
                                                add(Calendar.DAY_OF_YEAR, -1)
                                            }
                                            userSelectedDateMillis = yCal.timeInMillis
                                            showDateMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pick Date...") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showDateMenu = false
                                            datePickerDialog.show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Pick Time...") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showDateMenu = false
                                            timePickerDialog.show()
                                        }
                                    )
                                }
                            }

                            // 5. Split Chip (Clickable to adjust split count or remove/add split)
                            if (userIsSplit) {
                                Box {
                                    var showSplitMenu by remember { mutableStateOf(false) }

                                    SelectableParsedChip(
                                        label = "Split ($userSplitPeopleCount ways)",
                                        icon = Icons.AutoMirrored.Filled.CallSplit,
                                        tint = Color(0xFFF57C00),
                                        onClick = { showSplitMenu = true }
                                    )

                                    DropdownMenu(
                                        expanded = showSplitMenu,
                                        onDismissRequest = { showSplitMenu = false },
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surface)
                                            .clip(RoundedCornerShape(14.dp))
                                    ) {
                                        (2..6).forEach { count ->
                                            val isSelected = count == userSplitPeopleCount
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "Split $count ways",
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF57C00),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFFF57C00),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    userSplitPeopleCount = count
                                                    showSplitMenu = false
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = {
                                                Text("Remove split", color = ExpenseRed)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = null,
                                                    tint = ExpenseRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                userIsSplit = false
                                                showSplitMenu = false
                                            }
                                        )
                                    }
                                }
                            } else {
                                SelectableParsedChip(
                                    label = "+ Split",
                                    icon = Icons.AutoMirrored.Filled.CallSplit,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    onClick = {
                                        userIsSplit = true
                                        userSplitPeopleCount = 2
                                    },
                                    showDropdownArrow = false
                                )
                            }
                        }

                        // Duplicate Warning Banner
                        AnimatedVisibility(
                            visible = duplicateAlert != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            duplicateAlert?.let { alert ->
                                Spacer(modifier = Modifier.height(12.dp))
                                DuplicateWarningCard(warning = alert)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isSaving) return@OutlinedButton
                        isSaving = true
                        val finalTitle = customTitle.ifBlank { parsedDraft.title }
                        onClose()
                        onOpenFullEditor(
                            parsedDraft.copy(
                                title = finalTitle,
                                type = userSelectedType,
                                matchedCategory = userSelectedCategory,
                                matchedAccount = userSelectedAccount,
                                matchedToAccount = userSelectedToAccount,
                                isSplit = userIsSplit,
                                splitPeopleCount = userSplitPeopleCount,
                                dateEpochMillis = userSelectedDateMillis
                            )
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text("Edit in Form", fontWeight = FontWeight.SemiBold)
                }

                val canSave = parsedDraft.amount != null && parsedDraft.amount!! > 0 && userSelectedAccount != null &&
                    (userSelectedType != TransactionType.TRANSFER || userSelectedToAccount != null)

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val amt = parsedDraft.amount ?: return@Button
                        val accId = userSelectedAccount?.id ?: return@Button
                        isSaving = true
                        val finalTitle = customTitle.ifBlank { parsedDraft.title }
                        val txn = Transaction(
                            title = finalTitle,
                            amount = amt,
                            type = userSelectedType,
                            accountId = accId,
                            toAccountId = if (userSelectedType == TransactionType.TRANSFER) userSelectedToAccount?.id else null,
                            categoryId = if (userSelectedType == TransactionType.TRANSFER) null else userSelectedCategory?.id,
                            dateEpochMillis = userSelectedDateMillis,
                            note = parsedDraft.rawInput
                        )
                        onSaveTransaction(txn, userIsSplit, userSplitPeopleCount)
                        onClose()
                    },
                    enabled = canSave && !isSaving,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (duplicateAlert?.isExactMatch == true) {
                            "Log Anyway • ${CurrencyFormatter.format(parsedDraft.amount!!)}"
                        } else if (parsedDraft.amount != null && parsedDraft.amount!! > 0) {
                            "Log • ${CurrencyFormatter.format(parsedDraft.amount!!)}"
                        } else {
                            "Log Spend"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableParsedChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: (() -> Unit)? = null,
    showDropdownArrow: Boolean = onClick != null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f)),
        modifier = if (onClick != null) {
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
        } else {
            Modifier
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (showDropdownArrow) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Change",
                    tint = tint.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
