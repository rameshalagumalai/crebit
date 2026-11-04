package com.example.pam.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pam.ui.screens.dashboard.DashboardScreen
import com.example.pam.ui.screens.manage.ManageScreen
import com.example.pam.ui.screens.reports.ReportsScreen
import com.example.pam.ui.screens.scanner.UpiQrScannerScreen
import com.example.pam.ui.screens.splits.SplitsScreen
import com.example.pam.ui.screens.transaction_entry.AddEditTransactionScreen
import com.example.pam.ui.screens.transactions.TransactionsScreen
import com.example.pam.ui.screens.walkthrough.WalkthroughScreen
import com.example.pam.util.OnboardingManager

@Composable
fun PAMNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startDestination = remember {
        if (OnboardingManager.isWalkthroughCompleted(context)) {
            Screen.Dashboard.route
        } else {
            Screen.Walkthrough.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.padding(innerPadding)
    ) {
        composable(Screen.Walkthrough.route) {
            WalkthroughScreen(
                onFinishWalkthrough = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Walkthrough.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToScanUpi = {
                    navController.navigate(Screen.UpiQrScanner.route)
                },
                onNavigateToAddTransaction = { type ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(initialType = type))
                },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(transactionId = id))
                },
                onNavigateToTransactions = {
                    navController.navigate(Screen.Transactions.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAccounts = {
                    navController.navigate(Screen.Manage.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSplits = {
                    navController.navigate(Screen.Splits.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Transactions.route) {
            TransactionsScreen(
                onNavigateToAddTransaction = {
                    navController.navigate(Screen.AddEditTransaction.createRoute())
                },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(transactionId = id))
                }
            )
        }

        composable(Screen.Splits.route) {
            SplitsScreen(
                onNavigateToAddExpenseWithSplit = {
                    navController.navigate(
                        Screen.AddEditTransaction.createRoute(
                            initialType = com.example.pam.data.model.TransactionType.EXPENSE,
                            enableSplit = true
                        )
                    )
                },
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(transactionId = id))
                }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateToTransactionDetail = { id ->
                    navController.navigate(Screen.AddEditTransaction.createRoute(transactionId = id))
                }
            )
        }

        composable(Screen.Manage.route) {
            ManageScreen()
        }

        composable(
            route = Screen.AddEditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("scannedUpi") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("initialType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("enableSplit") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val idParam = backStackEntry.arguments?.getString("transactionId")?.takeIf { it.isNotBlank() }
            val upiParam = backStackEntry.arguments?.getString("scannedUpi")?.takeIf { it.isNotBlank() }
            val typeParam = backStackEntry.arguments?.getString("initialType")?.takeIf { it.isNotBlank() }
            val enableSplitParam = backStackEntry.arguments?.getString("enableSplit") == "true"
            val resolvedType = typeParam?.let {
                try {
                    com.example.pam.data.model.TransactionType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }

            AddEditTransactionScreen(
                transactionId = idParam?.toLongOrNull(),
                scannedUpi = upiParam,
                initialType = resolvedType,
                enableSplit = enableSplitParam,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.UpiQrScanner.route) {
            UpiQrScannerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onQrCodeDetected = { scannedContent ->
                    navController.popBackStack()
                    navController.navigate(Screen.AddEditTransaction.createRoute(scannedUpi = scannedContent))
                }
            )
        }
    }
}
