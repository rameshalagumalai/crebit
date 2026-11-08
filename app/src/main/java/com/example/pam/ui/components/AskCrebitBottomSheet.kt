package com.example.pam.ui.components

import android.app.Activity
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pam.data.model.Account
import com.example.pam.data.model.Category
import com.example.pam.data.model.SplitGroupWithDetails
import com.example.pam.data.model.TransactionWithDetails
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.IncomeGreen
import com.example.pam.util.CurrencyFormatter
import com.example.pam.util.DateUtils
import com.example.pam.util.ai.AskCrebitEngine
import com.example.pam.util.ai.QueryIntentType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AskCrebitBottomSheet(
    allTransactions: List<TransactionWithDetails>,
    accounts: List<Account>,
    categories: List<Category>,
    splits: List<SplitGroupWithDetails> = emptyList(),
    initialQuery: String = "",
    startWithVoice: Boolean = false,
    onDismiss: () -> Unit,
    onEditTransaction: (Long) -> Unit = {},
    onDeleteTransaction: (Long) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    var queryText by remember { mutableStateOf(initialQuery) }
    var selectedTxnForDetails by remember { mutableStateOf<TransactionWithDetails?>(null) }

    val queryResult by remember(queryText, allTransactions, accounts, categories, splits) {
        derivedStateOf {
            AskCrebitEngine.query(
                rawQuery = queryText,
                allTransactions = allTransactions,
                accounts = accounts,
                categories = categories,
                splits = splits
            )
        }
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
                queryText = spokenText
            }
        }
    }

    val launchSpeechToText: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Crebit (e.g. 'Food spend this month')...")
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
        } else if (initialQuery.isBlank()) {
            delay(250)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val samplePrompts = listOf(
        "🍔 Food this month",
        "💸 Pending splits",
        "🏆 Biggest spend",
        "💳 Bank balances",
        "✈️ Travel this month",
        "🛒 Shopping this week",
        "💰 Total net worth",
        "📈 Income this month"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets.safeDrawing }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AskCrebitGlowingButton(showSpinningRing = false)

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Ask Crebit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% on-device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "Ask anything (e.g. 'Food this month', 'Pending splits')...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (queryText.isNotBlank()) {
                                IconButton(onClick = { queryText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = launchSpeechToText) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    singleLine = true
                )
            }

            // Quick Prompt Suggestions Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(samplePrompts) { prompt ->
                    val cleanPrompt = prompt.substringAfter(" ").trim()
                    FilterChip(
                        selected = queryText.equals(cleanPrompt, ignoreCase = true),
                        onClick = { queryText = cleanPrompt },
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            // Query Content / Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // If query is blank: render Welcome & capabilities card
                if (queryText.isBlank()) {
                    item {
                        CrebitAiWelcomeCard(onPromptClick = { queryText = it })
                    }
                } else {
                    // Result Hero Card
                    item {
                        CrebitHeroResultCard(result = queryResult)
                    }

                    // Follow-up Suggestions
                    if (queryResult.suggestedFollowUps.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "Suggested drill-downs:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    queryResult.suggestedFollowUps.forEach { followUp ->
                                        Surface(
                                            modifier = Modifier.clickable { queryText = followUp },
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = followUp,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Splits Receivables List (if any)
                    if (queryResult.matchingSplits.isNotEmpty()) {
                        item {
                            Text(
                                text = "Open Receivables (${queryResult.matchingSplits.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(queryResult.matchingSplits) { splitDetails ->
                            SplitReceivableItemCard(
                                item = splitDetails,
                                onClick = {
                                    splitDetails.expenseTransaction?.let {
                                        selectedTxnForDetails = it
                                    }
                                }
                            )
                        }
                    }

                    // Matching Transactions List (if any)
                    if (queryResult.matchingTransactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Matching Transactions (${queryResult.matchingTransactions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(queryResult.matchingTransactions) { item ->
                            TransactionItemCard(
                                item = item,
                                onClick = { selectedTxnForDetails = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Tap-to-Inspect Transaction Details Dialog
    selectedTxnForDetails?.let { txn ->
        TransactionDetailsDialog(
            item = txn,
            onDismiss = { selectedTxnForDetails = null },
            onEdit = { id ->
                selectedTxnForDetails = null
                onDismiss()
                onEditTransaction(id)
            },
            onDelete = { id ->
                selectedTxnForDetails = null
                onDeleteTransaction(id)
            }
        )
    }
}

@Composable
private fun CrebitHeroResultCard(result: com.example.pam.util.ai.AskCrebitQueryResult) {
    val primaryColor = when (result.intentType) {
        QueryIntentType.INCOME_SUMMARY -> IncomeGreen
        QueryIntentType.BIGGEST_EXPENSE, QueryIntentType.CATEGORY_SPEND -> MaterialTheme.colorScheme.primary
        QueryIntentType.SPLITS_RECEIVABLE -> MaterialTheme.colorScheme.tertiary
        QueryIntentType.NET_WORTH, QueryIntentType.ACCOUNT_BALANCE -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = primaryColor.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with Intent Icon and Intent Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (result.intentType) {
                                QueryIntentType.SPLITS_RECEIVABLE -> Icons.AutoMirrored.Filled.CallSplit
                                QueryIntentType.NET_WORTH, QueryIntentType.ACCOUNT_BALANCE -> Icons.Default.AccountBalance
                                QueryIntentType.INCOME_SUMMARY -> Icons.Default.Payments
                                else -> Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = result.intentType.name.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                // Comparison Badge (if available)
                result.comparisonText?.let { comp ->
                    val isHigher = comp.contains("higher")
                    val badgeColor = if (isHigher) ExpenseRed else IncomeGreen
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isHigher) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = badgeColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = comp,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Metric
            if (result.primaryMetric != null) {
                Text(
                    text = result.primaryMetric,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Headline
            Text(
                text = result.headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Subhead
            result.subhead?.let { sub ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CrebitAiWelcomeCard(onPromptClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Welcome to Ask Crebit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Type or speak financial queries in plain English. Everything runs 100% offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Capabilities Checklist
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CapabilityBullet(icon = Icons.Default.Security, title = "100% Private & Offline", desc = "No data is shared outside")
                CapabilityBullet(icon = Icons.AutoMirrored.Filled.TrendingUp, title = "Smart Comparisons", desc = "Automatic period & category trend deltas")
            }
        }
    }
}

@Composable
private fun CapabilityBullet(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SplitReceivableItemCard(
    item: SplitGroupWithDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.splitGroup.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Total ${CurrencyFormatter.format(item.splitGroup.totalAmount)} • ${DateUtils.formatRelativeDate(item.splitGroup.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(item.remainingAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "to receive",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

