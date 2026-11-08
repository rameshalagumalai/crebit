package com.example.pam.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pam.data.repository.PAMRepository
import com.example.pam.ui.theme.ExpenseRed
import com.example.pam.ui.theme.WarningAmberDark
import com.example.pam.util.ai.DuplicateDetectionResult
import kotlinx.coroutines.launch

@Composable
fun DuplicateWarningCard(
    warning: DuplicateDetectionResult,
    modifier: Modifier = Modifier,
    onEditTransaction: ((Long) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    var showDetailsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val repository = remember { PAMRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val accentColor = if (warning.isExactMatch) {
        if (isDark) Color(0xFFF87171) else ExpenseRed
    } else {
        if (isDark) WarningAmberDark else Color(0xFFD97706)
    }

    val containerColor = if (isDark) {
        accentColor.copy(alpha = 0.12f)
    } else {
        accentColor.copy(alpha = 0.08f)
    }

    val borderColor = accentColor.copy(alpha = if (isDark) 0.35f else 0.25f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { showDetailsDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Duplicate alert",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (warning.isExactMatch) "Possible Duplicate Expense" else "Similar Expense Notice",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = warning.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "View",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Details",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showDetailsDialog) {
        TransactionDetailsDialog(
            item = warning.matchedTransaction,
            onDismiss = { showDetailsDialog = false },
            onEdit = { id ->
                showDetailsDialog = false
                onEditTransaction?.invoke(id)
            },
            onDelete = { id ->
                showDetailsDialog = false
                coroutineScope.launch {
                    repository.deleteTransaction(id)
                }
            }
        )
    }
}
