package com.example.pam.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.pam.data.model.TransactionType

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Walkthrough : Screen("walkthrough", "Welcome")
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Transactions : Screen("transactions", "Spends", Icons.AutoMirrored.Filled.ReceiptLong)
    data object Splits : Screen("splits", "Splits", Icons.AutoMirrored.Filled.CallSplit)
    data object Reports : Screen("reports", "Insights", Icons.Default.BarChart)
    data object Manage : Screen("manage", "Spaces", Icons.Default.AccountBalance)

    data object AddEditTransaction : Screen(
        "add_edit_transaction?transactionId={transactionId}&scannedUpi={scannedUpi}&initialType={initialType}&enableSplit={enableSplit}",
        "Transaction"
    ) {
        fun createRoute(
            transactionId: Long? = null,
            scannedUpi: String? = null,
            initialType: TransactionType? = null,
            enableSplit: Boolean = false
        ): String {
            val idParam = transactionId?.toString() ?: ""
            val upiParam = if (scannedUpi != null) Uri.encode(scannedUpi) else ""
            val typeParam = initialType?.name ?: ""
            val splitParam = if (enableSplit) "true" else ""
            return "add_edit_transaction?transactionId=$idParam&scannedUpi=$upiParam&initialType=$typeParam&enableSplit=$splitParam"
        }
    }

    data object UpiQrScanner : Screen("upi_qr_scanner", "Scan UPI QR")

    companion object {
        val bottomNavScreens = listOf(Dashboard, Transactions, Splits, Reports, Manage)
    }
}
