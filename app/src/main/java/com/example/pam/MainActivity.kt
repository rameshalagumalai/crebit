package com.example.pam

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pam.data.model.TransactionType
import com.example.pam.ui.components.PAMBottomBar
import com.example.pam.ui.navigation.PAMNavGraph
import com.example.pam.ui.navigation.Screen
import com.example.pam.ui.theme.PAMTheme
import com.example.pam.ui.theme.ThemeManager
import com.example.pam.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_QUICK_ACTION = "com.example.pam.EXTRA_QUICK_ACTION"
        const val ACTION_SCAN = "SCAN"
        const val ACTION_SPEND = "SPEND"
        const val ACTION_RECEIVE = "RECEIVE"
        const val ACTION_MOVE = "MOVE"
    }

    private val quickActionFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        enableEdgeToEdge()
        handleQuickAction(intent)
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PAMTheme(darkTheme = isDark) {
                PAMApp(quickActionFlow)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickAction(intent)
    }

    private fun handleQuickAction(intent: Intent?) {
        val action = intent?.getStringExtra(EXTRA_QUICK_ACTION)
        if (action != null) {
            quickActionFlow.value = action
        }
    }
}

@Composable
fun PAMApp(quickActionFlow: StateFlow<String?>) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val quickAction by quickActionFlow.collectAsState()
    LaunchedEffect(quickAction) {
        quickAction?.let { action ->
            when (action) {
                MainActivity.ACTION_SCAN -> {
                    navController.navigate(Screen.UpiQrScanner.route)
                }
                MainActivity.ACTION_SPEND -> {
                    navController.navigate(
                        Screen.AddEditTransaction.createRoute(initialType = TransactionType.EXPENSE)
                    )
                }
                MainActivity.ACTION_RECEIVE -> {
                    navController.navigate(
                        Screen.AddEditTransaction.createRoute(initialType = TransactionType.INCOME)
                    )
                }
                MainActivity.ACTION_MOVE -> {
                    navController.navigate(
                        Screen.AddEditTransaction.createRoute(initialType = TransactionType.TRANSFER)
                    )
                }
            }
            (quickActionFlow as? MutableStateFlow)?.value = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            PAMBottomBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { innerPadding ->
        PAMNavGraph(
            navController = navController,
            innerPadding = innerPadding
        )
    }
}